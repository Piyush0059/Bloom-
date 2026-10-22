"use strict";
const { isServerConfigured, voiceIsConfigured, isAuthenticated, sendJson } = require("../lib/security");

module.exports = function handler(req, res) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return sendJson(res, 405, { error: "Method not allowed." });
  }
  const configured = isServerConfigured();
  return sendJson(res, 200, {
    configured,
    authenticated: configured && isAuthenticated(req),
    voiceReady: configured && voiceIsConfigured(),
    model: process.env.GEMINI_MODEL || "gemini-3.8-flash"
  });
};
