"use strict";

const crypto = require("crypto");
const COOKIE_NAME = "bloom_session";
const SESSION_TTL_SECONDS = 60 * 60 * 24 * 7;

function isServerConfigured() {
  return Boolean(
    process.env.GEMINI_API_KEY &&
    process.env.BLOOM_ACCESS_PASSWORD &&
    process.env.BLOOM_ACCESS_PASSWORD.length >= 12 &&
    process.env.BLOOM_SESSION_SECRET &&
    process.env.BLOOM_SESSION_SECRET.length >= 32
  );
}

function voiceIsConfigured() {
  return Boolean(
    process.env.ELEVENLABS_API_KEY &&
    process.env.ELEVENLABS_VOICE_ID &&
    process.env.BLOOM_VOICE_CONSENT_CONFIRMED === "true"
  );
}

function signExpiry(expiry, secret) {
  return crypto
    .createHmac("sha256", secret)
    .update("bloom:v1:" + expiry)
    .digest("hex");
}

function timingSafeStringEqual(left, right) {
  const a = crypto.createHash("sha256").update(String(left)).digest();
  const b = crypto.createHash("sha256").update(String(right)).digest();
  return crypto.timingSafeEqual(a, b);
}

function parseCookies(header) {
  const result = {};
  String(header || "").split(";").forEach((part) => {
    const i = part.indexOf("=");
    if (i < 1) return;
    const name = part.slice(0, i).trim();
    const value = part.slice(i + 1).trim();
    try { result[name] = decodeURIComponent(value); } catch (_) { result[name] = value; }
  });
  return result;
}

function isAuthenticated(req) {
  if (!isServerConfigured()) return false;
  const token = parseCookies(req.headers && req.headers.cookie)[COOKIE_NAME];
  if (!token) return false;
  const split = token.split(".");
  if (split.length !== 2) return false;
  const expiry = Number(split[0]);
  if (!Number.isSafeInteger(expiry) || expiry <= Math.floor(Date.now() / 1000)) return false;
  return timingSafeStringEqual(split[1], signExpiry(String(expiry), process.env.BLOOM_SESSION_SECRET));
}

function sessionToken() {
  const expiry = String(Math.floor(Date.now() / 1000) + SESSION_TTL_SECONDS);
  return expiry + "." + signExpiry(expiry, process.env.BLOOM_SESSION_SECRET);
}

function setSessionCookie(res, token) {
  const secure = process.env.NODE_ENV === "production" ? "; Secure" : "";
  res.setHeader(
    "Set-Cookie",
    COOKIE_NAME + "=" + encodeURIComponent(token) +
    "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + SESSION_TTL_SECONDS + secure
  );
}

function clearSessionCookie(res) {
  const secure = process.env.NODE_ENV === "production" ? "; Secure" : "";
  res.setHeader("Set-Cookie", COOKIE_NAME + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0" + secure);
}

function baseHeaders(res) {
  res.setHeader("Cache-Control", "no-store, max-age=0");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
  res.setHeader("X-Frame-Options", "DENY");
}

function sendJson(res, status, payload) {
  baseHeaders(res);
  res.status(status).json(payload);
}

function parseBody(req) {
  if (req.body && typeof req.body === "object") return req.body;
  if (typeof req.body === "string") {
    try { return JSON.parse(req.body); } catch (_) { return null; }
  }
  return null;
}

function originIsSameSite(req) {
  const origin = req.headers && req.headers.origin;
  if (!origin) return true;
  const host = String((req.headers && (req.headers["x-forwarded-host"] || req.headers.host)) || "").split(",")[0].trim().toLowerCase();
  try { return new URL(origin).host.toLowerCase() === host; } catch (_) { return false; }
}

const attempts = new Map();
function rateLimit(req, bucket, limit, windowMs) {
  const forwarded = String((req.headers && req.headers["x-forwarded-for"]) || "");
  const ip = forwarded.split(",")[0].trim() || String((req.headers && req.headers["x-real-ip"]) || "unknown");
  const key = bucket + ":" + ip;
  const now = Date.now();
  let entry = attempts.get(key);
  if (!entry || now - entry.start >= windowMs) entry = { start: now, count: 0 };
  entry.count += 1;
  attempts.set(key, entry);
  if (attempts.size > 1000) {
    for (const [k, v] of attempts) if (now - v.start > windowMs * 2) attempts.delete(k);
  }
  return { allowed: entry.count <= limit, retryAfter: Math.max(1, Math.ceil((entry.start + windowMs - now) / 1000)) };
}

module.exports = {
  isServerConfigured,
  voiceIsConfigured,
  isAuthenticated,
  sessionToken,
  setSessionCookie,
  clearSessionCookie,
  sendJson,
  parseBody,
  originIsSameSite,
  rateLimit,
  timingSafeStringEqual
};
