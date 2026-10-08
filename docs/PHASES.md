# SubLearn implementation plan

**Phase convention:** work is organized by the phase IDs below on the session's fixed branch `arena/4386d633-sublearn`; phase names are planning labels, not alternate Git branches. Each phase has an explicit verification gate. Scope follows the supplied NOW/LATER boundary; LATER features receive only an interface, disabled flag/UI, and a documented `NotImplemented` implementation.

## Phase 0 — `phase/0-foundations`
- Audit the empty repository and study the five named references; record findings, license decisions, and the unresolved dictionary-data rights question.
- Establish the Kotlin/Compose/Media3 Gradle multi-module build, version catalog, Android 12-compatible baseline, CI, app license, notices, and documentation skeleton.
- Define design tokens/motion, versioned typed settings, English/Persian resources and per-run RTL support.
- Add explicit LATER capability interfaces, disabled feature flags and stubs before any consumer depends on them.
- **Gate:** Gradle project configures; license and extension-point audits are recorded; CI builds a debug APK.

## Phase 1 — `phase/1-app-shell`
- Build the navigation shell, settings/search/export/import surfaces, theme selection, and accessible reusable components.
- Implement SAF-based video/URL opening, Room-backed recent media/progress, side navigation, and disabled LATER destinations.
- **Gate:** Home and settings render with empty real data, URI handoff works, settings survive restart, and UI smoke tests cover navigation/RTL.

## Phase 2 — `phase/2-player-core`
- Add a Media3 player abstraction and real ExoPlayer implementation for local content and supported HTTP streams, audio focus, PiP, playback restoration, and playlist/recent queue.
- Build the MX-inspired overlay, auto-hide, transport/seek, track menus, decoder mapping, orientation/aspect/lock controls, and prioritized configurable gestures.
- **Gate:** FakePlayer UI tests cover controls; an emulator smoke test opens/pause/seeks a real fixture-free URI path; no inactive controls are presented.

## Phase 3 — `phase/3-subtitle-engine`
- Implement pure SRT/WebVTT/ASS parsers, charset detection, cue normalization/batch operations and O(log n) cue lookup with unit tests.
- Connect external subtitle loading, best-effort same-folder sidecars, independent multi-track layers, delay/style settings, tappable Compose text, subtitle list/no-spoiler, layout mode and dockable quick actions.
- **Gate:** parser/normalizer/property tests pass and both subtitle surfaces work through the actual player state path, including mixed RTL/LTR.

## Phase 4 — `phase/4-interaction`
- Implement ML Kit on-device translation with explicit model download/status; translate word/line/block from subtitle hit testing and correct pause/dismiss/resume behavior.
- Add Room-backed My Words, mark-known state, word cards, learning/entertainment mode settings and reusable detail surfaces.
- **Gate:** translation is a real provider call (never sample output); saved words persist; UI tests cover selected text and native RTL.

## Phase 5 — `phase/5-shadowing`
- Implement repeat-current cue, configurable count/delay/duration multiplier, temporary hold inversion, stop-at-block-end and all settings.
- **Gate:** pure formula/state-machine tests plus FakePlayer interaction tests pass.

## Phase 6 — `phase/6-learning-mode`
- Add `WordLevelProvider` with manual level and unknown-word fallback (no unlicensed list), animated contextual popup pipeline, and per-surface/per-role word styles.
- Keep POS/phrase detection behind the disabled `WordAnalyzer` capability.
- **Gate:** fallback only uses the user's own saved/known state and selected translation provider; styling settings do not bleed across surfaces.

## Phase 7 — `phase/7-ai-assistant`
- Implement prompt/context builder and Gemini/OpenAI/Anthropic `AiProvider` adapters, secure Keystore-backed key storage, cancellation/loading/pause/resume, and editor/settings UI.
- **Gate:** request serialization tests, secret-storage tests on Android, cancellation/error/offline states, and no API key in logs, exports, or backup.

## Phase 8 — `phase/8-subtitle-tools`
- Implement line-break flattening, max-character splitting, search, current-line list and no-spoiler behavior; expose deferred AI segmentation/quote marking as disabled entries only.
- **Gate:** transformed cue timing/text tests and UI tests pass; users can apply changes and export a subtitle through SAF.

## Phase 9 — `phase/9-hardening-release`
- Close accessibility, i18n, lifecycle/process-death, performance, license and test gaps; complete user/developer docs and the ID-by-ID checklist.
- Run `./gradlew assembleDebug testDebugUnitTest lint`, instrumented smoke tests where CI permits, inspect APK/release artifact, publish `v0.1.0` debug APK, and open/merge the session-branch PR if repository protections permit.
- **Gate:** every NOW ID is implemented and verified or its blocker is explicitly disclosed; every LATER ID has an interface/disabled flag/extension documentation; CI is green and release provenance is documented.

## Verification policy

Use focused module tests while implementing, followed by the full requested gate `./gradlew assembleDebug testDebugUnitTest lint`. CI additionally runs instrumentation on an Android 12/API 31 emulator for core navigation/player surfaces and packages a debug APK. If local Android tooling is absent, record that limitation, rely on CI only after observing its actual result, and never label an unrun check as passing. Reference project code and data are not copied; only permissive notices are retained when code is actually reused (none is planned).