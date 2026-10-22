"use strict";
const { clearSessionCookie, sendJson, originIsSameSite } = require("../lib/security");

module.exports = function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return sendJson(res, 405, { error: "Method not allowed." });
  }
  if (!originIsSameSite(req)) return sendJson(res, 403, { error: "Cross-site request rejected." });
  clearSessionCookie(res);
  return sendJson(res, 200, { ok: true });
};
