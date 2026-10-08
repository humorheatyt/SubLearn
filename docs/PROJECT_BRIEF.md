# Project brief

## Purpose

SubLearn is a privacy-conscious Android video player that helps English learners understand real subtitle context. It combines local/direct-stream playback with independent learning and translation subtitle layers, tap/drag translation, shadowing/repeat, user-owned vocabulary and an opt-in remote AI explanation path.

## Audience and defaults

- Primary audience: English learners whose native/translation language is Persian.
- Default language roles: learning `en`, native/translation `fa`, UI `en`.
- Platform: Android 12/API 31 is a priority target (including Poco X3 Pro); `minSdk 26`, `compileSdk/targetSdk 35`.
- Cost/privacy: free and ad-free; subtitle/media files remain local; translation is on-device after the official ML Kit language model is downloaded; AI calls are explicit and remote.

## Product boundaries

The NOW release contains Home/recent media, Media3 playback of SAF-selected media and direct streams, subtitle track/layer handling, subtitle translation and editing, My Words, manual-level/unknown-word popups, repeat/shadowing controls, configurable settings/themes, and opt-in AI providers. LATER capabilities remain disabled and have explicit interfaces, feature flags, failure stubs, Coming Soon UI where exposed, and extension instructions.

No scraped captions, downloader, hidden media endpoint, ads, dictionary data, extracted translation model, bundled word list or demo media is part of the plan. The offline dictionary must eventually be an explicitly user-supplied file and pass a rights/schema review.

## Design direction

Use a calm cinematic player surface and a clear learning-card hierarchy: dark player stage, amber learning emphasis, indigo interaction accent, restrained rounded cards, readable large targets, and high-contrast light/dark/AMOLED variants. User-selected system fonts avoid unverified font licensing. See `DESIGN_SYSTEM.md`.

## Architecture

Use a modular Kotlin/Compose/Material 3 application with Media3, Coroutines/Flow, Room/FTS, DataStore, Koin, OkHttp and a Gradle version catalog. Pure subtitle/domain logic is separated from Android platform capabilities. See `ARCHITECTURE.md` and `EXTENSION_POINTS.md`.

## Delivery status

The repository is pre-release and currently unverified: the sandbox blocks the Gradle distribution host, and no local build/tests/lint/emulator run have completed. CI workflows and test sources are present but have not yet run. See `CHECKLIST.md`, `AGENT_REQUESTS.md`, and `PROGRESS.md`; do not claim v0.1.0 Definition of Done until those gates pass.
