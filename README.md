# Bloom — Android app + private web companion

Bloom is a multilingual AI companion for reflective conversations. This repository contains **two interfaces**: an Android app (Jetpack Compose) and a private web preview (static UI plus Node/Vercel API routes). Both now enforce **Text Only** until a consented Gemini replicated voice ID is configured.

## Current status

- Android source changes and web backend are committed to `main`.
- Node/web checks run through GitHub Actions.
- An Android CI workflow now runs unit tests and builds a debug APK; check its run before treating the Android build as verified.
- The web API requires a Vercel deployment and server-side environment values. GitHub Pages cannot run the protected `/api/*` functions.
- The supplied voice attachment is about 8 seconds long. A working replicated voice ID has **not** been created from it. No real API keys or voice credentials have been committed.

## What is implemented

### Android app

- Jetpack Compose UI with animated intelligence orb.
- Language options: English, Korean, and Hinglish.
- Gemini text chat, local guest mode, Firebase Auth/Firestore hooks, history, and export functions.
- Gemini TTS uses an explicit replicated voice ID, not the prebuilt Aoede/Puck voices.
- Android/system TextToSpeech fallback has been removed. If the configured friend voice is absent or fails, Bloom reports an error and does not substitute another voice.
- TTS audio playback is saved as WAV, stopped/released safely on interruption, and temporary cache files are cleaned up.
- Voice choice defaults to Text Only; Settings validates and saves the custom replica ID.
- Automatic Android backup is disabled by default for private conversation data.

### Web app

- Responsive mobile-first dark interface with layered animated orb, English/Korean/Hinglish UI and reduced-motion support.
- Protected same-origin API routes for login, logout, session status, Gemini chat and optional Gemini replicated speech.
- API keys remain in Vercel environment variables and are never collected in the page UI.
- Password-protected session uses a signed, HTTP-only, SameSite cookie.
- Input size checks, origin checks, method checks, and best-effort per-instance rate limiting.
- Browser-local conversations and memory with export, delete-all, history switching, and opt-in memory.
- Browser speech recognition is input-only where available. Browser/system text-to-speech is not used.

## Friend voice: required real-world step

Google Gemini Voice Replication requires **two recordings from the same adult speaker**:

1. A clean, natural **10–30 second reference recording**.
2. A separate recording of the same speaker reading Google's mandatory consent statement in a supported language.

The clip uploaded for this chat is approximately 8 seconds and cannot meet the reference-duration requirement by itself. Bloom does not create a clone simply because a file was uploaded, and the file has not been added to the public repository. Ask your friend to explicitly agree to this specific use. The friend should record the consent phrase themselves and complete the Google AI Studio Voice Replication flow.

- [Official Gemini Voice Replication guide](https://ai.google.dev/gemini-api/docs/voice-replication)
- [Official Gemini TTS guide](https://ai.google.dev/gemini-api/docs/speech-generation)

When Voice Replication returns a permitted ID beginning with `voice_` or `voicekey_`, configure it only in a private setting. Bloom accepts only those replica-ID formats; a name like `Aoede` is not a replicated voice ID.

## Android: run and test

Prerequisites: Android Studio with a compatible Android SDK, JDK 17 or the JDK required by the installed Android Gradle Plugin, and network access to download Gradle dependencies.

1. Open this repository in Android Studio and allow Gradle sync.
2. For the already-installed APK, open **Settings → AI Chat Setup**, paste your own Gemini API key and tap **Save API Key**. The key is encrypted locally with Android Keystore and is not synced to Firebase. This is convenient for personal testing but does not make a distributed APK safe from reverse engineering.
3. If you build the APK yourself, you can also copy `.env.example` to a **local, untracked** `.env` and set `GEMINI_API_KEY`; the committed file contains placeholders, not a valid API key. A key saved in app Settings takes precedence over the build-time key.
4. If you use Firebase Google sign-in/cloud history, add the `google-services.json` for your own Firebase project to the expected app location and enable the Firebase products used by the app. This private configuration is not committed here. Guest mode can still be used without Firebase sign-in.
5. For friend-voice output, complete the Google AI Studio Voice Replication flow and enter the resulting `voice_...` or `voicekey_...` ID in the app's Settings. Text Only remains the safe default until the ID is configured.
6. Run **testDebugUnitTest** and **assembleDebug** from the Gradle tool window, or run:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
6. Install the debug APK on a real Android device and test text chat, each language, history, no network, invalid API key, microphone denial, valid/invalid replica ID, audio playback and interruption.

The mobile app currently uses its Gemini key through the native client configuration. Android APKs can be reverse-engineered, so restrict the key to the extent supported by the Google project, monitor quotas, and use a trusted backend for any public release. The web backend keeps the key server-side; this does not automatically secure the independent Android client.

## Web: deploy privately on Vercel

Vercel is required because the web version has protected API functions. A successful GitHub commit or static check is **not** a live deployment.

1. Open [Vercel New Project](https://vercel.com/new) and import `Piyush0059/Bloom-`.
2. Use repository root, Framework Preset **Other**, no frontend build command.
3. In **Project → Settings → Environment Variables**, configure the required values below.
4. Deploy, open the assigned Vercel URL, sign in, and test `/api/session` plus chat. Re-deploy after changing environment values.

| Environment variable | Required | Purpose |
|---|---|---|
| `GEMINI_API_KEY` | Yes | Server-only Gemini API key |
| `GEMINI_MODEL` | No | Defaults to `gemini-3.8-flash`; override for a model enabled for your account |
| `BLOOM_ACCESS_PASSWORD` | Yes | Unique private-access password, at least 12 characters |
| `BLOOM_SESSION_SECRET` | Yes | Random signing secret, at least 32 characters |
| `BLOOM_CUSTOM_VOICE_ID` | Voice only | Approved Gemini replica ID: `voice_...` or `voicekey_...` |
| `BLOOM_VOICE_CONSENT_CONFIRMED` | Voice only | Exact value `true` after consent and provider-policy checks; otherwise keep `false` |

Generate a session secret in a trusted terminal with `openssl rand -hex 32`. Never put real secrets in this repository, source code, screenshots, or chat messages. Use separate values for the password and signing secret.

### Web security limits

The web app is a personal private beta, not multi-user SaaS auth. Its in-memory rate limiter is best-effort and not shared across serverless instances; it does not replace provider quota/budget controls. Conversations are local to the browser and do not sync across devices. When memory is explicitly enabled, the selected excerpts and memory note are sent to Gemini as prompt context.

## Automated checks

- Web/API: `npm test` (Node.js 22 or newer). This checks JavaScript syntax, referenced HTML IDs, no client-side API key field, WAV header handling, session/login/logout, access protection and consent-gated voice readiness.
- Android: GitHub Actions workflow **Android build and unit tests** runs `./gradlew testDebugUnitTest assembleDebug` and uploads a debug APK artifact when successful. This repository check is not a substitute for testing the resulting APK on your phone.
- External live tests: actual Gemini responses and voice playback require your own valid API key, allowed model, and (for speech) completed voice replication. Do not mark those paths “working” until you test them with credentials configured outside source control.

## Repository map

```text
Bloom-/
├── app/                       # Native Android app (Jetpack Compose)
├── index.html                 # Web UI
├── api/                       # Vercel session/login/chat functions
├── lib/security.js            # Web session and request checks
├── tests/smoke.js             # Web/API smoke tests
├── .github/workflows/ci.yml   # Node/web checks
├── .github/workflows/android-ci.yml
├── .env.example               # Safe placeholders only
├── firestore.rules            # User-scoped Firestore rules
├── package.json
└── vercel.json
```

## Safety note

Bloom is an AI companion, not a therapist, medical device, or emergency service. Emotional support and crisis behaviour need human review and specific end-to-end tests before public release.
