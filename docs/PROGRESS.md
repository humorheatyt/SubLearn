# Progress

**Last updated:** 2026-10-08. **Branch:** `arena/c5e36220-sublearn`. **CI:** green (run `37857249419` on `bb7d0cc`): `assembleDebug`, `testDebugUnitTest` (incl. 4 Robolectric Compose UI tests on API 31), `:core:domain:test` (12 tests), `:core:subtitles:test` (12 tests) and `lint` all pass. See `CHECKLIST.md` for the row-by-row evidence and `KNOWN_ISSUES.md` for what still needs a device.

## Goal

SubLearn: a free, open-source MX-Player-style Android video player for learning English from subtitles (dual subtitle layers, tap-to-translate, shadowing/repeat tools, My Words, AI help). NOW scope = `PRODUCT_SPEC.md`; plan = `PHASES.md`; decisions = `DECISIONS.md`.

## What is done

- Full multi-module Kotlin/Compose/Material 3 app: `app` + `core:{domain,subtitles,platform,design}` + `feature:{home,player,learning,settings}` (~7k lines), Koin DI, Media3 playback, Room (FTS My Words + recents), typed versioned DataStore settings with JSON export/import, Keystore-encrypted AI keys.
- Player: MX-style overlay (auto-hide, quick-actions column, dockable subtitle buttons, lock, playlist, aspect ratio, speed, PiP, decoder SW/HW/HW+ offered only when MediaCodec exposes them), configurable gestures (brightness/volume/seek/double-tap/two-finger speed), subtitle list with search/no-spoiler/tap-to-seek, per-layer delay, external+embedded tracks, same-name sidecar auto-load, charset detection (UTF-8/UTF-16/Windows-1256).
- Subtitle engine: own SRT/WebVTT/ASS parsers → `Cue`, normalization pipeline (tag/entity cleanup, fragment merge, punctuation-aware max-char split), O(log n) timeline, batch tools + SRT export — all pure-JVM unit-tested.
- Interaction: tap word/line/block + drag phrase translation (ML Kit, explicit model download UI), pause/dismiss/resume rule, My Words save/mark-known/remove (Room FTS), word styling (My Words/known styles; POS/phrase gated behind LATER analyzer), entertainment/learning popups with level fallback that never invents CEFR.
- Shadowing: repeat-once/hold-auto-repeat, count + pause formula (duration multiplier), stop-at-block-end with temporary-hold inversion.
- AI: Gemini (default)/OpenAI/Anthropic behind `AiProvider`, prompt editor, context builder (previous N + title + timestamps), loading ring, pause/resume, no keys in logs/exports/backups.
- Settings system: searchable categorized screens, per-surface/per-language fonts, themes (light/dark/AMOLED), EN + FA (RTL per text run), LATER entries disabled with Coming Soon labels.
- CI/CD: verify workflow (build + unit + Robolectric UI + lint, debug APK artifact), non-blocking API 31 emulator smoke job, tag-triggered release workflow (3 ABIs + universal APKs + checksums), signed-release publishing workflow, release signing key outside the repo.
- Docs: complete set (`AGENTS.md`, README, LICENSE Apache-2.0, THIRD_PARTY_NOTICES, CHANGELOG, PRODUCT_SPEC, REFERENCES, ARCHITECTURE, DESIGN_SYSTEM, DECISIONS, PHASES, CHECKLIST, EXTENSION_POINTS, AGENT_REQUESTS, KNOWN_ISSUES, PROGRESS) kept current.

## What is NOT done (and why)

1. **Device pass (Poco X3 Pro / API 31 hardware):** impossible from the sandbox. Playback/PiP/gesture feel/decoder quirks/SAF providers/ML Kit download need a real device. Checklist marks these `IMPLEMENTED / DEVICE PENDING` — never "done".
2. **Instrumented emulator smoke (`connectedDebugAndroidTest`):** the CI job runs but currently fails; the sandbox cannot read the log host. Test reports are uploaded as job artifacts — open the run in GitHub to see the cause. Robolectric UI tests cover the same screens in the blocking gate meanwhile.
3. **Rights decisions:** REQ-001 (dictionary data provenance — dictionary stays off), REQ-003 (ML Kit Terms of Service — dependency ships; owner should accept or replace), REQ-006 (grant the GitHub App Secrets permission to enable signing-in-CI; until then the two-stage staging-branch signing flow is used, see DECISIONS #27).
4. **LATER scope** (YouTube section, PDF/browser/image learning, dictionary import, auto level, quiz, update checker, AI re-segmentation/quote marking, speech-to-text, offline NLP, on-device AI, more languages): intentionally stubbed behind interfaces + false flags (see `EXTENSION_POINTS.md`). Stretch promotion order after NOW: (1) dictionary import+lookup, (2) My Words quiz, (3) GitHub-releases update checker.

## How to test it

```bash
./gradlew assembleDebug testDebugUnitTest :core:domain:test :core:subtitles:test lint   # the CI gate
./gradlew connectedDebugAndroidTest                                                    # API 31 emulator/device
adb install app/build/outputs/apk/debug/app-debug.apk                                  # try on a phone
```

UI smoke tests run on the JVM via Robolectric (`app/src/test/kotlin/com/sublearn/app/RobolectricUiSmokeTest.kt`). Install the release APK from GitHub Releases (arm64-v8a for Poco X3 Pro). First run: open a local video (or folder for sidecar subtitles), download the EN↔FA translation model in Settings, tap words to translate.

## Release state

See the GitHub Releases page for `v0.1.0`: `SubLearn-0.1.0-{arm64-v8a,armeabi-v7a,x86_64,universal}.apk` + `SHA256SUMS.txt`, signed with the SubLearn release key (SHA-256 cert fingerprint recorded in DECISIONS #27-era notes and `sublearn-signing/`). The key is outside the repo — back it up (REQ-006).

## Continuing checklist

1. Run the device pass and flip the `DEVICE PENDING` rows to verified with evidence.
2. Fix the emulator smoke job (reports are in the failed run's artifacts) or keep it informational.
3. Owner: resolve REQ-003/REQ-001/REQ-006.
4. Stretch LATER items in the fixed order when NOW is fully device-verified.
