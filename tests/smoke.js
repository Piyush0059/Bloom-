import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { Script } from "node:vm";
import session from "../api/session.js";
import login from "../api/login.js";
import logout from "../api/logout.js";
import chat, { pcmToWav } from "../api/chat.js";

const html = await readFile(new URL("../index.html", import.meta.url), "utf8");
const idSet = new Set(Array.from(html.matchAll(/\bid="([^"]+)"/g), (match) => match[1]));
const scriptMatches = Array.from(html.matchAll(/<script>([\s\S]*?)<\/script>/g));
assert.equal(scriptMatches.length, 1, "expected one self-contained inline app script");
const inlineScript = scriptMatches[0][1];
new Script(inlineScript, { filename: "index.html:inline-script" });
const directlyReferencedIds = new Set(Array.from(inlineScript.matchAll(/\$\("([^"]+)"\)/g), (match) => match[1]));
const missingIds = Array.from(directlyReferencedIds).filter((id) => !idSet.has(id));
assert.deepEqual(missingIds, [], "UI script references non-existent element IDs");
assert.equal(html.includes('id="apiKeyInput"'), false, "Gemini API key must not be collected in browser UI");
assert.equal(inlineScript.includes("speechSynthesis"), false, "must never use generic browser speech playback");
assert.equal(html.includes('id="voiceToggleButton"'), true, "voice toggle must exist");
assert.equal(inlineScript.includes('new Blob([bytes],{type:"audio/wav"})'), true, "browser voice playback must use WAV");
const chatSource = await readFile(new URL("../api/chat.js", import.meta.url), "utf8");
assert.equal(chatSource.includes("elevenlabs.io"), false, "voice output must use Gemini, not a second provider");
assert.equal(chatSource.includes("gemini-3.8-flash-tts"), true, "Gemini TTS model must be configured");
assert.equal(chatSource.includes('https://generativelanguage.googleapis.com/v1beta/interactions'), true, "replicated voice must use the official Interactions endpoint");
assert.equal(chatSource.includes("models/gemini-3.8-flash-tts:generateContent"), false, "replicated voice must not use the generic generateContent route");
const pcm = Buffer.from([0, 0, 1, 0, 255, 127, 0, 0]);
const wav = pcmToWav(pcm);
assert.equal(wav.toString("ascii", 0, 4), "RIFF");
assert.equal(wav.toString("ascii", 8, 12), "WAVE");
assert.equal(wav.readUInt32LE(24), 24000, "PCM must be wrapped as 24 kHz WAV");
assert.equal(wav.length, 44 + pcm.length, "WAV header must include the original PCM payload");
assert.equal(pcmToWav(wav), wav, "already wrapped WAV data must not get a second header");

function mockResponse() {
  return {
    headers: {},
    statusCode: 200,
    payload: undefined,
    setHeader(name, value) { this.headers[name] = value; },
    status(code) { this.statusCode = code; return this; },
    json(value) { this.payload = value; return this; }
  };
}

const env = {
  GEMINI_API_KEY: process.env.GEMINI_API_KEY,
  BLOOM_ACCESS_PASSWORD: process.env.BLOOM_ACCESS_PASSWORD,
  BLOOM_SESSION_SECRET: process.env.BLOOM_SESSION_SECRET,
  BLOOM_CUSTOM_VOICE_ID: process.env.BLOOM_CUSTOM_VOICE_ID,
  BLOOM_VOICE_CONSENT_CONFIRMED: process.env.BLOOM_VOICE_CONSENT_CONFIRMED
};

try {
  process.env.GEMINI_API_KEY = "test-gemini-key-not-real";
  process.env.BLOOM_ACCESS_PASSWORD = "bloom-test-password-long";
  process.env.BLOOM_SESSION_SECRET = "test-secret-at-least-thirty-two-characters-long";
  delete process.env.BLOOM_CUSTOM_VOICE_ID;
  delete process.env.BLOOM_VOICE_CONSENT_CONFIRMED;

  const headers = { host: "bloom.test", origin: "https://bloom.test" };
  let res = mockResponse();
  await session({ method: "GET", headers }, res);
  assert.equal(res.statusCode, 200);
  assert.equal(res.payload.configured, true);
  assert.equal(res.payload.authenticated, false);
  assert.equal(res.payload.voiceReady, false);
  assert.equal(JSON.stringify(res.payload).includes("test-gemini-key-not-real"), false);

  res = mockResponse();
  await login({ method: "POST", headers, body: { password: "wrong-password" } }, res);
  assert.equal(res.statusCode, 401, "bad passwords must be rejected");

  res = mockResponse();
  await login({ method: "POST", headers, body: { password: process.env.BLOOM_ACCESS_PASSWORD } }, res);
  assert.equal(res.statusCode, 200);
  assert.match(res.headers["Set-Cookie"], /HttpOnly/);
  assert.match(res.headers["Set-Cookie"], /SameSite=Strict/);
  const cookie = res.headers["Set-Cookie"].split(";")[0];

  res = mockResponse();
  await session({ method: "GET", headers: { ...headers, cookie } }, res);
  assert.equal(res.payload.authenticated, true);

  res = mockResponse();
  await chat({ method: "POST", headers: { ...headers, cookie }, body: { messages: [] } }, res);
  assert.equal(res.statusCode, 400, "invalid chat payload should be rejected before provider use");

  res = mockResponse();
  await chat({ method: "POST", headers, body: { messages: [] } }, res);
  assert.equal(res.statusCode, 401, "chat endpoint must require a signed session");

  process.env.BLOOM_CUSTOM_VOICE_ID = "voice_abc12345";
  res = mockResponse();
  await session({ method: "GET", headers: { ...headers, cookie } }, res);
  assert.equal(res.payload.voiceReady, false, "voice remains disabled without explicit consent flag");
  process.env.BLOOM_VOICE_CONSENT_CONFIRMED = "true";
  res = mockResponse();
  await session({ method: "GET", headers: { ...headers, cookie } }, res);
  assert.equal(res.payload.voiceReady, true, "voice only turns on with Gemini key, replica voice ID, and explicit consent flag");
  process.env.BLOOM_CUSTOM_VOICE_ID = "Aoede";
  res = mockResponse();
  await session({ method: "GET", headers: { ...headers, cookie } }, res);
  assert.equal(res.payload.voiceReady, false, "prebuilt voice names must not be accepted as replica voice IDs");

  res = mockResponse();
  await logout({ method: "POST", headers, body: {} }, res);
  assert.equal(res.statusCode, 200);
  assert.match(res.headers["Set-Cookie"], /Max-Age=0/);

  console.log("Bloom smoke tests passed: session security, login rejection, signed cookie, protected chat, consent-gated voice, and logout.");
} finally {
  for (const [key, value] of Object.entries(env)) {
    if (value === undefined) delete process.env[key];
    else process.env[key] = value;
  }
}
