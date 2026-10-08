# SubLearn

**SubLearn** is an open-source Android video player and English-learning companion. It puts two independently styled subtitle layers, tap-to-translate, repeat/shadowing controls, a searchable transcript, My Words and opt-in contextual AI around local videos and direct video streams.

> **Verification status:** This repository is being assembled on the Arena session branch. The Gradle wrapper is present, but the sandbox cannot download Gradle 8.9; no Android build, unit test, lint, emulator run, APK, or release has yet been verified. See [docs/PROGRESS.md](docs/PROGRESS.md) and [docs/AGENT_REQUESTS.md](docs/AGENT_REQUESTS.md). Do not treat this pre-release tree as a finished v0.1.0.

## Product defaults

- UI: English; learning language: English; translation/native language: Persian.
- Material 3 light, dark, and AMOLED themes; per-surface/per-language typography and RTL direction.
- No ads, monetization, bundled videos, sample subtitles, dictionary database, word list, or extracted translation model.
- Android 12 / API 31 is a first-class target (including the Poco X3 Pro); minimum SDK is 26.

## Current implementation inventory

The modular app source includes a Room-backed recent-media/My Words store, SAF file/folder pickers, direct HTTP(S) player entry, Media3 playback, dual Compose subtitle overlays, SRT/WebVTT/text ASS/SSA parsing, subtitle batch editing/export, on-device ML Kit translation, encrypted provider-key storage, Gemini/OpenAI/Anthropic adapters, shadowing controls, settings JSON import/export, and explicit disabled LATER interfaces/flags. These paths have **not yet been compiler- or device-verified**; see the ID-by-ID [checklist](docs/CHECKLIST.md) for exact status and test method.

Matching subtitle sidecars can be discovered when the user selects a SAF folder (which grants sibling access). A single `Choose a video` document grant alone cannot reliably enumerate sibling files on every provider.

## Build and test

Prerequisites:

- JDK 17 (Android Gradle Plugin 8.7.3 requirement)
- Android SDK platform/build tools 35
- Gradle wrapper (Gradle 8.9; use the checked-in `gradlew`)
- Google/Maven repository access for Android and Kotlin dependencies

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint
./gradlew assembleDebug testDebugUnitTest lint
```

Install the local debug APK with Android Studio or `adb install app/build/outputs/apk/debug/app-debug.apk` after the build succeeds. API 31 emulator smoke tests are configured as `connectedDebugAndroidTest`; a supported emulator is required:

```bash
./gradlew connectedDebugAndroidTest
```

GitHub Actions runs the build/unit/lint gate and an Android 12 Compose smoke job. On a tag, the release workflow builds/verifies and attaches the debug APK plus SHA-256 checksum. No tag or release is created until the full NOW scope and legal/test gates are satisfied.

## First run

1. Open a local video through **Choose a video**, or select a directory through **Browse a folder** to list its videos and allow matching sidecar subtitles to be found.
2. Paste a direct `http://`/`https://` stream URL from Home, or open a video/URL from another app.
3. Download the English↔Persian ML Kit translation models from Settings before using translation offline. Translation is on-device after model download; the official Google ML Kit dependency has separate Terms of Service, tracked in [REQ-003](docs/AGENT_REQUESTS.md).
4. Add an AI provider key in Settings only if you want remote AI explanations. Keys are encrypted using Android Keystore and are excluded from JSON settings export; an explicit AI request sends the selected subtitle/context to the selected provider.

The future offline dictionary is deliberately not bundled. Never add `fastdic_plain.sqlite`, derived dictionary content, `.bipe` files, or extracted models. See [license notes](THIRD_PARTY_NOTICES.md) and [extension points](docs/EXTENSION_POINTS.md).

## Modules

- `app`: Android entry, SAF/intent integration, navigation, Koin wiring.
- `core:domain`: typed models, capability interfaces, settings codec, repeat/context logic and LATER stubs.
- `core:subtitles`: pure Kotlin SRT/WebVTT/ASS parsing, normalization, tokenization and timeline lookup.
- `core:platform`: Media3, Room, DataStore, Keystore secrets, ML Kit and HTTP AI adapters.
- `core:design`: Material 3 theme, spacing/color/shape/motion/typography tokens.
- `feature:home`, `feature:player`, `feature:learning`, `feature:settings`: isolated Compose features.

## Project documentation

Start with [`AGENTS.md`](AGENTS.md). Product and architecture decisions are in `docs/PRODUCT_SPEC.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`, `docs/DECISIONS.md`, and `docs/PHASES.md`. Current limitations and verification are in `docs/CHECKLIST.md`, `docs/KNOWN_ISSUES.md`, `docs/AGENT_REQUESTS.md`, and `docs/PROGRESS.md`.

## License

Original SubLearn code is Apache-2.0; third-party dependencies retain their own terms. See [`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). Google ML Kit's Terms of Service and the dictionary data provenance review are open items; SubLearn does not claim those are permissively licensed.
