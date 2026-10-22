# Bloom — AI companion web preview

Bloom is a responsive, single-file web MVP for a calm multilingual AI companion. The repository is intentionally static so it can be hosted with GitHub Pages without a build step.

## Included in this preview

- Responsive dark interface with a layered animated intelligence sphere.
- English, Korean, and Hinglish interface/prompt selection.
- Gemini text chat using the official `generateContent` REST endpoint.
- A session-only API-key input (the key is not committed to the repository or stored in local chat history).
- Local browser conversation history, new conversations, export-to-JSON, deletion, and an optional user-authored memory note.
- Optional recent-conversation excerpts in prompts when the user explicitly enables the Memory setting.
- Browser speech-recognition input where available; no speech is played back.
- Keyboard navigation, responsive mobile layout, reduced-motion support, and accessible labels.
- A GitHub Actions workflow that checks the entry point and deploys the static files to GitHub Pages.

## Important: what is not connected

This is a **web MVP**, not the complete production version of Bloom.

- **Friend's voice:** speech replies are deliberately disabled. No generic/system voice is used. A voice provider, authorized voice asset, clear consent, and backend integration still need to be connected and tested.
- **Production security:** the browser calls Gemini directly. The API key is entered by the user and retained only in `sessionStorage`; however, a browser-based key is still visible to that browser session and can be exposed by malicious extensions or device access. Do not distribute a shared key or offer this as a public multi-user service. A production release should route requests through an authenticated server or serverless function, store secrets in environment variables, apply rate limits, and enforce per-user quotas.
- **Persistent cross-device memory:** chat history is stored in that browser's local storage. It does not sync across devices or have user accounts/backups. Export the JSON file to keep a backup.
- **No verified therapeutic benefit:** Bloom is an AI companion, not a mental-health professional or an emergency service. Crisis behavior must receive dedicated testing and appropriate expert review before a public launch.

## Run locally

Open `index.html` in a modern browser. For browser features that require a secure context, use HTTPS or `localhost`.

1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Open Bloom and choose **Set up AI**.
3. Paste your own key, select a model that is enabled for your account, and save.
4. Start a conversation. Do not commit keys to source control.

The app currently uses the Gemini REST endpoint `v1beta/models/{model}:generateContent`. Models and account availability can change; see Google's [current models list](https://ai.google.dev/gemini-api/docs/models).

## Publish using GitHub Pages

A GitHub Actions workflow is included at `.github/workflows/pages.yml`.

1. Push the repository to the `main` branch.
2. Open **Settings → Pages** for this repository.
3. In **Build and deployment → Source**, select **GitHub Actions** and save. This repository setting has to be enabled by an account with admin access.
4. Open **Actions** and wait for the **Deploy Bloom to GitHub Pages** workflow to finish successfully.
5. Open the Pages URL GitHub displays under **Settings → Pages** and test the page on mobile and desktop.

Typical URL format: `https://piyush0059.github.io/Bloom-/` (the actual URL only becomes available after Pages is enabled and the workflow succeeds).

If the workflow fails, check its run log first. Do not treat the site as deployed unless the Actions deployment job succeeds and the live URL loads.

## Smoke-test checklist

- [ ] Open on a phone and desktop; check responsive layout and keyboard focus.
- [ ] Switch between Hinglish, English, and Korean; verify interface text changes.
- [ ] Without an API key, try sending a message; Bloom should explain how to configure AI.
- [ ] With a valid key and available model, verify multi-turn replies; test invalid key, unavailable model, quota exhaustion, and offline failure.
- [ ] Create two conversations, switch via History, refresh, and confirm local history remains.
- [ ] Export JSON; inspect that messages exist and that no API key is included.
- [ ] Delete all history; confirm it is gone after refresh.
- [ ] Toggle Memory off and on; check whether prior chat excerpts are included only when enabled.
- [ ] Test microphone denial and unsupported-browser fallback.
- [ ] Verify there is **no speech playback** and no generic voice.
- [ ] Test reduced motion, narrow 320-pixel screens, and browser zoom.
- [ ] Review API usage/quota and privacy notice before public use.

## Repository structure

```text
Bloom-/
├── index.html
├── favicon.svg
├── README.md
└── .github/
    └── workflows/
        └── pages.yml
```

## License

No license is declared yet. Add one before inviting external contributions or permitting reuse.
