"use strict";
import { clearSessionCookie, sendJson, originIsSameSite } from "../lib/security.js";

export default function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return sendJson(res, 405, { error: "Method not allowed." });
  }
  if (!originIsSameSite(req)) return sendJson(res, 403, { error: "Cross-site request rejected." });
  clearSessionCookie(res);
  return sendJson(res, 200, { ok: true });
}
