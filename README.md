# SubLearn

**SubLearn** is an open-source Android video player and English-learning companion.

**[Download v0.1.0](https://github.com/humorheatyt/SubLearn/releases/tag/v0.1.0)** — pick `arm64-v8a` for most phones (including the Poco X3 Pro), `x86_64` for emulators, or `universal` if unsure. Verify with `SHA256SUMS.txt`. It puts two independently styled subtitle layers, tap-to-translate, repeat/shadowing controls, a searchable transcript, My Words and opt-in contextual AI around local videos and direct video streams.

> **Verification status (2026-10-08):** CI is green on `arena/c5e36220-sublearn` — the full gate `assembleDebug testDebugUnitTest :core:domain:test :core:subtitles:test lint` passes (24 JVM unit tests + 4 Robolectric Compose UI tests on API 31 + lint). The release pipeline publishes signed `v0.1.0` APKs (arm64-v8a / armeabi-v7a / x86_64 / universal + SHA256SUMS) on the GitHub Releases page. A physical-device pass (Poco X3 Pro) is still recommended before relying on playback/PiP/decoder specifics; see [docs/CHECKLIST.md](docs/CHECKLIST.md) and [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md).

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
./gradlew assembleDebug testDebugUnitTest :core:domain:test :core:subtitles:test lint   # the CI gate
./gradlew connectedDebugAndroidTest                                                    # emulator/device smoke
```

Install the local debug APK with Android Studio or `adb install app/build/outputs/apk/debug/app-debug.apk`. UI smoke tests run on the JVM (Robolectric, API 31) inside `testDebugUnitTest`; instrumented tests need an emulator or device.

GitHub Actions runs the gate above plus a non-blocking Android 12 emulator smoke job. Pushing a `v*` tag builds release APKs for **arm64-v8a, armeabi-v7a, x86_64 and universal** with checksums; the `publish-release` workflow verifies signatures and attaches them to the GitHub Release (signing details in `docs/DECISIONS.md` #27).

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
