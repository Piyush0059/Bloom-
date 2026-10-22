"use strict";
import {
  isServerConfigured, voiceIsConfigured, isAuthenticated, sendJson, parseBody,
  originIsSameSite, rateLimit
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
      const tts = await fetch("https://api.elevenlabs.io/v1/text-to-speech/" +
        encodeURIComponent(process.env.ELEVENLABS_VOICE_ID) + "?output_format=mp3_44100_128", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "xi-api-key": process.env.ELEVENLABS_API_KEY,
          "Accept": "audio/mpeg"
        },
        body: JSON.stringify({
          text: answer.slice(0, 2000),
          model_id: "eleven_multilingual_v2",
          voice_settings: { stability: 0.52, similarity_boost: 0.8, style: 0.12, use_speaker_boost: true }
        }),
        signal: AbortSignal.timeout(25000)
      });
      if (!tts.ok) {
        const detail = (await tts.text().catch(() => "")).slice(0, 180);
        console.error("Voice provider request failed", tts.status, detail.replace(/\s+/g, " "));
        voiceError = true;
      } else {
        const audio = Buffer.from(await tts.arrayBuffer());
        if (audio.length > 6 * 1024 * 1024) {
          console.error("Generated voice response exceeded safe size.");
          voiceError = true;
        } else {
          audioBase64 = audio.toString("base64");
        }
      }
    } catch (error) {
      console.error("Voice generation failed:", error && error.name ? error.name : "Error");
      voiceError = true;
    }
  }

  return sendJson(res, 200, { text: answer, audioBase64, voiceReady, voiceError });
}
