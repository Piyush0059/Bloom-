# Bloom — private AI companion

Bloom is a mobile-first, multilingual AI companion with a calm animated interface. This repository now targets **Vercel**, not GitHub Pages, because the app uses server-side secrets, private-session authentication, and protected API routes.

## What is implemented

- Responsive dark UI with an animated orbital intelligence sphere and reduced-motion support.
- Interface/prompt language modes: Hinglish, English, and Korean.
- Gemini text chat through a same-origin server function; the Gemini API key is never sent to browser JavaScript.
- Password-protected private access using an HTTP-only, SameSite cookie signed with a server secret.
- Server-side checks for payload sizes, request origin, methods, authentication and best-effort per-instance rate limits.
- Browser-local conversation history, chat switching, JSON export, delete-all and optional user-controlled memory.
- Browser speech recognition as input only where supported. Bloom does not use browser/system text-to-speech.
- Optional ElevenLabs text-to-speech that activates only when the owner's approved voice ID, provider API key and explicit consent flag are configured server-side. There is no voice fallback.
- Baseline smoke tests for login, signed sessions, protected chat, consent-gated voice and logout.
- Security response headers, content-security-policy, no-cache APIs and deployment configuration.

## Important voice status

The voice file shared for this project is an **8.04-second AAC clip**. It is not included in the public GitHub repository. This is too short to promise a high-fidelity voice clone. ElevenLabs recommends roughly **1–2 minutes of clear, consistent audio** for Instant Voice Cloning; source quality, accent and delivery affect the result. See the [official voice-cloning guide](https://elevenlabs.io/docs/eleven-creative/voices/voice-cloning/instant-voice-cloning).

Only continue with the friend's voice if she explicitly agrees to this specific use. The recommended path is for the friend/voice owner to create or authorize an eligible Instant Voice Clone in the provider account, complete any consent/verification prompts themselves, and privately share the permitted voice ID. Professional Voice Cloning is restricted to the voice owner's own voice by ElevenLabs policy; do not bypass this. See [ElevenLabs voice-cloning policy and methods](https://elevenlabs.io/docs/eleven-api/concepts/voice-cloning).

The app's voice toggle stays disabled until these environment variables are present and the consent flag is explicitly enabled:

- `ELEVENLABS_API_KEY`
- `ELEVENLABS_VOICE_ID`
- `BLOOM_VOICE_CONSENT_CONFIRMED=true`

Do not set the consent flag unless the voice owner has clearly agreed and you are permitted to use that voice. The audio file itself is never sent by this app to ElevenLabs; the server only sends the generated reply text to the voice already configured by the owner.

## Set up and deploy on Vercel

### 1. Import the repository

1. Open [Vercel](https://vercel.com/new) and sign in.
2. Import the GitHub repository `Piyush0059/Bloom-`.
3. Use the repository root as the project root. Framework preset: **Other**. No frontend build command is required.
4. Add the environment variables below before the first production deployment. Redeploy after changing environment values.

### 2. Required server variables

Set these in **Vercel → Project → Settings → Environment Variables** for the environment you intend to use (Production, and Preview if needed):

| Variable | Required | Purpose |
|---|---|---|
| `GEMINI_API_KEY` | Yes | Server-only Google Gemini API key |
| `GEMINI_MODEL` | No | Defaults to `gemini-3.8-flash`; override if that model isn't enabled for your account |
| `BLOOM_ACCESS_PASSWORD` | Yes | Private sign-in password; use a unique value at least 12 characters long |
| `BLOOM_SESSION_SECRET` | Yes | Random secret of at least 32 characters used to sign session cookies |
| `ELEVENLABS_API_KEY` | Voice only | Server-only voice provider key |
| `ELEVENLABS_VOICE_ID` | Voice only | ID of the eligible, consented voice already in the provider account |
| `BLOOM_VOICE_CONSENT_CONFIRMED` | Voice only | Set to literal `true` only after explicit voice-owner consent and provider policy checks |

Generate a session secret in a trusted terminal with `openssl rand -hex 32`. Do not put secret values in source code, GitHub commits, screenshots, or this chat. Do not use the same secret as the access password.

Google's official model list: [Gemini API models](https://ai.google.dev/gemini-api/docs/models). ElevenLabs' official synchronous speech endpoint: [Create speech](https://elevenlabs.io/docs/api-reference/text-to-speech/convert).

### 3. Sign in and test

After Vercel finishes deploying, open the Production URL:

1. Sign in with `BLOOM_ACCESS_PASSWORD`.
2. Send a short chat message and confirm that a reply appears.
3. Refresh and switch chats; confirm history remains in the same browser.
4. Export the conversation JSON and confirm that it contains no API keys.
5. Sign out; verify the API returns to a locked state.
6. To test audio, first configure an authorized voice ID plus the three voice environment variables, redeploy, then enable **Voice on · friend's voice**. If voice configuration is incomplete, no speech plays.

For a first deployment, visit `/api/session` on the Vercel domain. It reports only whether setup/login/voice are ready; it does not return keys. A correctly configured site should report `configured: true`. Sign-in is required before chat requests are accepted.

## Security and known limits

- This is a personal, password-protected MVP, not a multi-user SaaS authentication system.
- The rate limiter is per serverless instance and best-effort; it does not replace provider-level spending limits, quotas or centralized rate limiting.
- Conversation history and memory are stored in the current browser's local storage. They do not sync between devices. When Memory is enabled, selected recent excerpts and the user-entered note are sent to Gemini as context.
- Gemini and voice calls are made by server functions. Provider responses can still fail because of invalid credentials, quota, model availability, network or provider policy.
- Voice audio is not guaranteed to sound identical to the source speaker; synthesis can change accent, prosody and pronunciation.
- Browser dictation availability and its processing vary by browser. It is input-only, not a spoken-response fallback.
- Bloom is not a therapist or emergency service. Crisis responses need independent safety review before wider public release.

## Local checks

Requires Node.js 22 or later:

```bash
npm test
```

This runs JavaScript syntax checks plus small local tests of authentication and consent gating. It does not call paid external APIs; live Gemini and voice-provider checks must be performed after secrets are configured in Vercel.

## Repository layout

```text
Bloom-/
├── index.html
├── api/
│   ├── chat.js
│   ├── login.js
│   ├── logout.js
│   └── session.js
├── lib/
│   └── security.js
├── tests/
│   └── smoke.js
├── .github/workflows/ci.yml
├── package.json
└── vercel.json
```

## Deployment reality

GitHub Pages cannot run these API functions. Deploy on Vercel (or another Node.js serverless host configured for these routes) so keys remain server-side. A successful GitHub commit or local test is not evidence of a live deployment: use Vercel's completed deployment URL and test the protected API in that live environment.
