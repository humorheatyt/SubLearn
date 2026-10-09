# Changelog

All notable changes are recorded here. The project has not reached its verified v0.1.0 release.

## Unreleased — `arena/c5e36220-sublearn`

- Establish modular Android architecture, Compose app shell, design tokens, English/Persian resources, typed settings, and disabled LATER capability boundaries.
- Add SAF video/folder and direct URL entry, recent media, Media3 player controls/gestures, independent subtitle layers, parsers/normalization, translation, My Words, learning popups, repeat tools, AI-provider settings, and subtitle tools.
- Add Room migration to persist the last subtitle URI and layer, enabling resume of external subtitles.
- Add a Gradle 8.9 wrapper JAR, GitHub Actions build/unit/lint and Android 12 smoke workflows, instrumented Compose tests, and licensing/extension documentation.
- **Not verified:** the sandbox cannot fetch the Gradle distribution. No build, unit tests, lint, device run, APK, merge, or release has been observed. See `docs/PROGRESS.md`.

## v0.1.0 — 2026-10-09

First release. Signed APKs (arm64-v8a, armeabi-v7a, x86_64, universal) on the GitHub Releases page.

- MX-style player for local videos and direct HTTP(S) streams: overlay controls, quick actions, gestures (brightness/volume/seek/double-tap/two-finger speed), lock, playlist, aspect ratio, speed, PiP, decoder SW/HW/HW+ where exposed.
- Dual subtitle layers (learning + native) with per-layer tracks/delay/fonts/position, SRT/WebVTT/ASS parsers, sidecar auto-load, charset detection, subtitle list (search/no-spoiler/tap-to-seek), batch tools + export.
- Tap word/line/block + drag phrase translation (ML Kit on-device with explicit model download), My Words (search/mark-known), word styling, learning-mode popups, shadowing repeat tools.
- AI help (Gemini/OpenAI/Anthropic) with editable prompt, context builder, Keystore-encrypted keys.
- Settings: typed versioned schema, JSON export/import, per-surface fonts, light/dark/AMOLED, EN+FA (RTL).
- CI: build + 24 JVM unit tests + 4 Robolectric API 31 UI tests + lint green; ABI-split release pipeline with checksums.

Open review items: ML Kit Terms of Service (REQ-003), dictionary data provenance (REQ-001), signing-secret convenience (REQ-006). Device pass on Poco X3 Pro recommended (see `docs/CHECKLIST.md`).
