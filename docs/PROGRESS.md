# Progress

**Last updated:** 2026-10-08. **Definition of Done: NOT MET.** This is an unverified pre-release implementation on `arena/4386d633-sublearn`. The draft PR is [#1](https://github.com/humorheatyt/SubLearn/pull/1). No successful build, unit test, lint, APK artifact, emulator/device run, merge, or `v0.1.0` tag has been observed; all six Android CI runs so far failed at Gradle verification. The marker-based annotation exposed two Kotlin compile errors; local fixes await a fresh CI result (REQ-005).

## Goal

Build SubLearn as a modular Kotlin/Compose Android 12-compatible video/subtitle learning app. NOW scope and IDs are in `PRODUCT_SPEC.md`; the phase order is in `PHASES.md`; the row-by-row work/verification map is `CHECKLIST.md`.

## Implemented in source (not yet compiler/device verified)

- Multi-module Android app with `app`, `core:domain`, `core:subtitles`, `core:platform`, `core:design`, and Home/Player/Learning/Settings features; Koin wiring, Media3, Room, DataStore, Keystore-backed key storage and ML Kit/OkHttp adapters.
- Home/recent-media screen, SAF video and folder picker, same-folder subtitle discovery, direct URL dialog, video/stream intents and a deferred PDF-to-Learn Coming Soon route.
- Player overlays, audio/subtitle menus, Media3 controller, supported-decoder capability reporting, aspect ratio/speed/PiP/playlist/lock controls, gestures/feedback, lifecycle progress, brightness restoration and repeat-state flow/UI.
- SRT/WebVTT/text ASS/SSA parser and normalizer with unit-test source; two independently styled subtitle layers, per-layer delays, external/embedded track assignment, subtitle list/search/no-spoiler and batch tools/export.
- Word/line/block/drag translation flow, explicit model download UI, Room My Words and known state, per-language word marking, manual level/unknown-word popup path, independent typography, EN/FA resources and app locale direction.
- Gemini/OpenAI/Anthropic provider abstractions, context/prompt editor/loading/pause/resume and no-backup Keystore-encrypted key storage.
- LATER interfaces/stubs/false flags, disabled Coming Soon surfaces, `AGENTS.md`, reference/phase/decision/architecture/design/spec/checklist/extension/request/issue docs, license notices, CI and test sources.
- Shared design tokens in `core:design` now cover spacing, radii, elevation, motion timings, and alpha values; Home/Learning/Player screens consume these tokens for repeated geometry, feedback timing, and translucency.
- Restored the official 43,504-byte Gradle 8.9 wrapper JAR from the matching upstream tag. Added Room schema v2 nullable subtitle-layer migration.

The above is an implementation inventory, not a claim that the app runs. Check `KNOWN_ISSUES.md` before relying on any behavior.

## Verification actually performed

- Python `xml.etree.ElementTree` parsed all 15 repository XML files (resources/manifests) with zero errors.
- Tree-sitter Kotlin grammar parsed all 34 Kotlin source/test files with zero syntax-error or missing nodes. EN/FA parity passed for app (34 keys), core/platform (5), home (23), player (81), learning (14), and settings (141); the local `R.string` reference scan found zero missing keys. These are static checks only: Tree-sitter cannot type-check Android/Compose symbols or substitute for Android resource linking.
- `./gradlew assembleDebug testDebugUnitTest lint` was attempted locally before and after the compiler fixes with a temporary Java 21 runtime; each attempt failed before configuration because the TLS handshake to `services.gradle.org` failed while downloading Gradle 8.9. Running without Java also fails immediately (`java: not found`). No local Gradle task ran.
- GitHub Actions runs `37822587380`/`37822600898` (`b4a8ed3`), `37835059008`/`37835063379` (`c567727`), and `37835733412`/`37835739889` (`b8440e1`) all completed with `Verify Android project` failing; APK upload and dependent Android 12 smoke tests were skipped. The marker-focused check annotation exposed nullable-`Uri` and `Int`/`Long` Kotlin errors, now patched locally and awaiting a fresh CI run; details are in `REQ-005`.

## Build recovery attempts and current blockers

See detailed evidence in `AGENT_REQUESTS.md`:

1. Confirmed no system Java/Gradle and restored the official wrapper JAR; a wrapper run without Java reports `java: not found`.
2. Installed a temporary Java 21 runtime under `/tmp` from PyPI (`jdk4py`) so the wrapper could start; it then failed with `SSLHandshakeException` while downloading Gradle 8.9 from `services.gradle.org`.
3. Queried the official GitHub release distribution through the allowed GitHub API; its asset redirects to the blocked `release-assets.githubusercontent.com` host. The required Gradle command still reaches no Gradle task.

No Android SDK/emulator result is available. The GitHub Actions build and API 31 smoke workflow ran on three commits, but Gradle verification failed in all push/PR pairs; the smoke job was skipped. The marker-focused annotation identified two Kotlin compile errors, which are patched locally pending CI confirmation.

## Open license/rights review

- **REQ-001:** dictionaryproject's README says its schema/queries were taken from a third-party Android dictionary source and model loader reverse-engineered; no license was found. Nothing derived is included; dictionary remains disabled.
- **REQ-003:** Google ML Kit is the requested official translation API, but its Android artifact is governed by Google ML Kit Terms of Service, not a permissive OSS license. No model is bundled; review/accept this exception before release.
- `THIRD_PARTY_NOTICES.md` lists dependencies/assets and requires a resolved Gradle/SBOM/transitive-license audit before release. The app's original code is Apache-2.0; no GPL source or unlicensed reference assets are copied.

## What remains

1. Commit and push the `HomeScreen` nullable-URI fix and explicit `Long` cue-time fix on `arena/4386d633-sublearn`; inspect the next CI result and fix every newly exposed compile/test/lint failure.
2. Keep PR #1 in draft while CI is red or inconclusive; do not mark any checklist row verified without evidence.
3. Add or strengthen tests for settings normalization/migration, repository/Room schema, SAF sidecars, all gestures/translation pause-resume, repeat cancellation, and RTL; rerun `./gradlew assembleDebug testDebugUnitTest lint` and `connectedDebugAndroidTest` in CI.
4. On an API 31 emulator and Poco X3 Pro (or equivalent), verify local and HTTP playback, hardware/software decoder availability, embedded/external subtitle behavior, folder/sidecar permissions, audio focus, PiP, lifecycle/process restore, orientation, brightness/volume, EN/FA and accessibility.
5. Resolve ML Kit terms and complete the transitive dependency/SBOM review; keep REQ-001 data excluded.
6. Update every affected checklist/doc, require green CI, produce/review debug APK + checksum, then consider merge and the `v0.1.0` release/tag. Until then, no Definition of Done claim.

## Build/test instructions

On a machine/runner with JDK 17, Android SDK platform/build tools 35, and Google/Maven repository access:

```bash
./gradlew assembleDebug testDebugUnitTest lint
./gradlew connectedDebugAndroidTest  # API 31 emulator/device required
```

Expected debug APK after a successful build: `app/build/outputs/apk/debug/app-debug.apk`. In this sandbox, the wrapper currently reaches no Gradle task because the distribution download host is blocked; do not interpret that as a source build result.

## How to resume

Read `AGENTS.md`, `KNOWN_ISSUES.md`, `CHECKLIST.md` and `AGENT_REQUESTS.md`. Stay on the fixed session branch. Build/verify through GitHub Actions after push because local network policy blocks the Gradle distribution. Update this file with actual command/check IDs/results at the end of each task.
