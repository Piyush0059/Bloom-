"use strict";
import {
  isServerConfigured, voiceIsConfigured, isAuthenticated, sendJson, parseBody,
  originIsSameSite, rateLimit, getConfiguredVoiceId
} from "../lib/security.js";

const LANGUAGE_NAMES = {
  en: "English",
  ko: "Korean",
  hinglish: "natural Hinglish (Hindi written in Latin script with familiar English words where natural)"
};

function makeSystemPrompt(language, memory) {
  let prompt = "You are Bloom, a calm, thoughtful AI companion. Respond in " +
    (LANGUAGE_NAMES[language] || LANGUAGE_NAMES.hinglish) +
    " unless the user explicitly asks for another language. Sound warm, specific, and respectful without pretending to be human, claiming feelings, or encouraging dependence. Listen before advising. Reflect the user's words tentatively; do not diagnose, mind-read, or overstate what they feel. Ask at most one thoughtful follow-up question at a time. Avoid generic positivity, empty praise, forced therapy language, and long lists. Offer practical next steps only when useful or requested. Respect boundaries and corrections. You are not a therapist, doctor, or emergency service. Do not claim confidentiality beyond the product's disclosed data handling. If the user expresses imminent danger, suicidal intent, or immediate risk of self-harm, respond calmly and directly: encourage them to contact local emergency services or a crisis service now, reach a trusted person who can stay with them, and move away from anything they could use to harm themselves. Do not rely on this AI conversation alone for crisis support. If you are unsure, ask a brief safety question. Bloom's speaking mode uses only the voice configured by the owner; never imply that audio has played when it has not.";
  if (memory) prompt += "\n\nUser-approved memory context (provided because the user enabled Memory):\n" +
    memory + "\nTreat this as fallible, possibly outdated context. Use it only when relevant, and ask rather than assuming.";
  return prompt;
}

export function pcmToWav(pcm, sampleRate = 24000, channels = 1, bitsPerSample = 16) {
  if (pcm.length >= 12 && pcm.toString("ascii", 0, 4) === "RIFF" && pcm.toString("ascii", 8, 12) === "WAVE") {
    return pcm;
  }
  const blockAlign = channels * bitsPerSample / 8;
  const byteRate = sampleRate * blockAlign;
  const header = Buffer.alloc(44);
  header.write("RIFF", 0);
  header.writeUInt32LE(36 + pcm.length, 4);
  header.write("WAVE", 8);
  header.write("fmt ", 12);
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(channels, 22);
  header.writeUInt32LE(sampleRate, 24);
  header.writeUInt32LE(byteRate, 28);
  header.writeUInt16LE(blockAlign, 32);
  header.writeUInt16LE(bitsPerSample, 34);
  header.write("data", 36);
  header.writeUInt32LE(pcm.length, 40);
  return Buffer.concat([header, pcm]);
}

function cleanMessages(value) {
  if (!Array.isArray(value) || value.length < 1 || value.length > 40) return null;
  const messages = [];
  let total = 0;
  for (const item of value) {
    if (!item || !["user", "assistant"].includes(item.role) || typeof item.text !== "string") return null;
    const text = item.text.trim();
    if (!text || text.length > 5000) return null;
    total += text.length;
    if (total > 18000) return null;
    const role = item.role === "assistant" ? "model" : "user";
    if (!messages.length && role !== "user") continue;
    if (messages.length && messages[messages.length - 1].role === role) {
      messages[messages.length - 1].parts[0].text += "\n\n" + text;
    } else {
      messages.push({ role, parts: [{ text }] });
    }
  }
  while (messages.length && messages[0].role !== "user") messages.shift();
  if (!messages.length || messages[messages.length - 1].role !== "user") return null;
  return messages.slice(-24);
}

export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return sendJson(res, 405, { error: "Method not allowed." });
  }
  if (!originIsSameSite(req)) return sendJson(res, 403, { error: "Cross-site request rejected." });
  if (!isServerConfigured()) return sendJson(res, 503, { error: "Bloom server setup is incomplete. Configure the required Vercel environment variables." });
  if (!isAuthenticated(req)) return sendJson(res, 401, { error: "Your Bloom session has expired. Please sign in again." });

  const limit = rateLimit(req, "chat", 30, 10 * 60 * 1000);
  if (!limit.allowed) {
    res.setHeader("Retry-After", String(limit.retryAfter));
    return sendJson(res, 429, { error: "Too many messages in a short period. Please pause and try again." });
  }

  const body = parseBody(req);
  if (!body) return sendJson(res, 400, { error: "Invalid JSON request." });
  const messages = cleanMessages(body.messages);
  if (!messages) return sendJson(res, 400, { error: "The conversation is invalid or too large. Please shorten the message and try again." });

  const language = Object.prototype.hasOwnProperty.call(LANGUAGE_NAMES, body.language) ? body.language : "hinglish";
  const memoryContext = typeof body.memoryContext === "string" ? body.memoryContext.trim().slice(0, 3500) : "";
  const model = process.env.GEMINI_MODEL || "gemini-3.8-flash";

  let answer;
  try {
    const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models/" +
      encodeURIComponent(model) + ":generateContent", {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": process.env.GEMINI_API_KEY },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: makeSystemPrompt(language, memoryContext) }] },
        contents: messages,
        generationConfig: { temperature: 0.72, maxOutputTokens: 900 }
      }),
      signal: AbortSignal.timeout(25000)
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const providerMessage = data && data.error && typeof data.error.message === "string" ? data.error.message : "";
      console.error("Gemini API request failed", response.status, providerMessage.slice(0, 220));
      return sendJson(res, response.status === 429 ? 429 : 502, {
        error: response.status === 429
          ? "Bloom's AI usage limit was reached. Please try again later."
          : "Bloom could not get an AI response. Check the server model and Gemini API configuration."
      });
    }
    answer = ((data.candidates && data.candidates[0] && data.candidates[0].content && data.candidates[0].content.parts) || [])
      .map((part) => typeof part.text === "string" ? part.text : "")
      .join("").trim();
    if (!answer) return sendJson(res, 502, { error: "Gemini returned no text. Try a different message." });
  } catch (error) {
    console.error("Gemini request failed before a response:", error && error.name ? error.name : "Error");
    return sendJson(res, 502, { error: "Bloom could not reach its AI service. Check the connection or try again." });
  }

  let audioBase64 = null;
  let voiceError = false;
  const voiceReady = voiceIsConfigured();
  if (body.speak === true && voiceReady) {
    try {
      const voiceId = getConfiguredVoiceId();
      const tts = await fetch("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-tts:generateContent", {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": process.env.GEMINI_API_KEY },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: answer.slice(0, 1800) }] }],
          generationConfig: {
            responseModalities: ["AUDIO"],
            speechConfig: {
              voiceConfig: { voice: voiceId },
              languageCode: language === "ko" ? "ko-KR" : language === "hinglish" ? "en-IN" : "en-US"
            }
          }
        }),
        signal: AbortSignal.timeout(25000)
      });
      const data = await tts.json().catch(() => ({}));
      if (!tts.ok) {
        const providerMessage = data && data.error && typeof data.error.message === "string" ? data.error.message : "";
        console.error("Gemini voice generation failed", tts.status, providerMessage.slice(0, 220));
        voiceError = true;
      } else {
        const parts = (data.candidates && data.candidates[0] && data.candidates[0].content && data.candidates[0].content.parts) || [];
        const audioPart = parts.find((part) => part.inlineData && typeof part.inlineData.data === "string");
        if (!audioPart) {
          voiceError = true;
          console.error("Gemini voice response contained no inline audio data.");
        } else {
          const pcm = Buffer.from(audioPart.inlineData.data, "base64");
          if (pcm.length > 5 * 1024 * 1024) {
            voiceError = true;
            console.error("Gemini voice response exceeded the safe size limit.");
          } else {
            audioBase64 = pcmToWav(pcm).toString("base64");
          }
        }
      }
    } catch (error) {
      console.error("Gemini voice request failed:", error && error.name ? error.name : "Error");
      voiceError = true;
    }
  }

  return sendJson(res, 200, { text: answer, audioBase64, voiceReady, voiceError });
}
