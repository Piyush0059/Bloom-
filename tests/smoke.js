import assert from "node:assert/strict";
import session from "../api/session.js";
import login from "../api/login.js";
import logout from "../api/logout.js";
import chat from "../api/chat.js";

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
  ELEVENLABS_API_KEY: process.env.ELEVENLABS_API_KEY,
  ELEVENLABS_VOICE_ID: process.env.ELEVENLABS_VOICE_ID,
  BLOOM_VOICE_CONSENT_CONFIRMED: process.env.BLOOM_VOICE_CONSENT_CONFIRMED
};

try {
  process.env.GEMINI_API_KEY = "test-gemini-key-not-real";
  process.env.BLOOM_ACCESS_PASSWORD = "bloom-test-password-long";
  process.env.BLOOM_SESSION_SECRET = "test-secret-at-least-thirty-two-characters-long";
  delete process.env.ELEVENLABS_API_KEY;
  delete process.env.ELEVENLABS_VOICE_ID;
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

  process.env.ELEVENLABS_API_KEY = "test-voice-key-not-real";
  process.env.ELEVENLABS_VOICE_ID = "test-voice-id";
  res = mockResponse();
  await session({ method: "GET", headers: { ...headers, cookie } }, res);
  assert.equal(res.payload.voiceReady, false, "voice remains disabled without explicit consent flag");
  process.env.BLOOM_VOICE_CONSENT_CONFIRMED = "true";
  res = mockResponse();
  await session({ method: "GET", headers: { ...headers, cookie } }, res);
  assert.equal(res.payload.voiceReady, true, "voice only turns on with provider credentials and consent flag");

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
