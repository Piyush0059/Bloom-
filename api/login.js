"use strict";
import {
  isServerConfigured, sessionToken, setSessionCookie, sendJson, parseBody,
  originIsSameSite, rateLimit, timingSafeStringEqual
} from "../lib/security.js";

export default function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return sendJson(res, 405, { error: "Method not allowed." });
  }
  if (!originIsSameSite(req)) return sendJson(res, 403, { error: "Cross-site request rejected." });
  if (!isServerConfigured()) return sendJson(res, 503, { error: "Bloom access is not configured on the server yet." });

  const limit = rateLimit(req, "login", 8, 15 * 60 * 1000);
  if (!limit.allowed) {
    res.setHeader("Retry-After", String(limit.retryAfter));
    return sendJson(res, 429, { error: "Too many login attempts. Try again later." });
  }

  const body = parseBody(req);
  const password = body && typeof body.password === "string" ? body.password : "";
  if (!password || password.length > 512 ||
      !timingSafeStringEqual(password, process.env.BLOOM_ACCESS_PASSWORD)) {
    return sendJson(res, 401, { error: "Incorrect password." });
  }
  setSessionCookie(res, sessionToken());
  return sendJson(res, 200, { ok: true });
}
