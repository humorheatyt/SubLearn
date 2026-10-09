# Specification checklist

**Snapshot:** 2026-10-08, session branch `arena/c5e36220-sublearn`, CI run `37857249419` on `bb7d0cc` **GREEN** (`assembleDebug testDebugUnitTest :core:domain:test :core:subtitles:test lint` all passed). Status vocabulary: `CI-VERIFIED` = compiled and covered by a passing automated check in that run (unit/Robolectric UI/lint); `IMPLEMENTED / DEVICE PENDING` = real code path exists and compiles but the behavior needs a device/emulator pass; `LATER / DISABLED` = intentional stub with interface + false flag + disabled UI. No row claims a device run that did not happen.

## General, entry and Home

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| GEN-1 | 0, 9 | CI-VERIFIED (structure/tokens) | Design tokens in `core:design`; screens render in Robolectric UI tests. Visual polish is subjective — screenshot review on device recommended. |
| GEN-2 | 0–8 | CI-VERIFIED | Settings export/import round-trip unit-tested (`SettingsCodec`); all option groups exposed in `SettingsScreen`. |
| GEN-3 | 0, 3, 6 | CI-VERIFIED (logic) / DEVICE PENDING | `SurfaceFontSettings` per surface + per language role; map round-trip unit-tested; UI applies fonts per surface (`SubtitleOverlay`, cards). Device check for bleed-through recommended. |
| GEN-4 | 0, 3, 4 | IMPLEMENTED / DEVICE PENDING | Per-run direction via `TextDirection` + `LocalLayoutDirection`; bidi hit-testing uses `TextLayoutResult.getOffsetForPosition`. Persian string parity enforced (6 module sets). RTL emulator pass still due. |
| GEN-5 | 1, 2, 9 | IMPLEMENTED / DEVICE PENDING | SAF-only storage, PiP entry, rotation configChanges, nav-argument process restore, Room progress persistence. Needs API 31 device pass. |
| GEN-6 | 0–9 | CI-VERIFIED | `app`, `core:domain`, `core:subtitles`, `core:platform`, `core:design`, `feature:*` build independently; capability ports isolate every external system. |
| GEN-7 | 4 | IMPLEMENTED / DEVICE PENDING | ML Kit translate + explicit model download/status UI; offline-after-download is ML Kit behavior. REQ-003 terms open. |
| APP-1 | 1, 2 | IMPLEMENTED / DEVICE PENDING | Manifest VIEW/SEND filters + intent consumption in `MainActivity`/`SubLearnApp` route straight to the player. Generic filters documented (`AppLinkUrlError` suppression rationale). |
| APP-2 | 1 | LATER content / routing real | PDF intents route to Learn with Coming Soon copy; no PDF content read (flag `PDF_BROWSER_IMAGE_LEARNING=false`). |
| APP-3 | 1 | CI-VERIFIED | Nav shell (Home/Learn/My Words/Settings/Level/Folder/Player + drawer with disabled LATER entries) exercised by Robolectric smoke tests. |
| APP-4 | 1 | IMPLEMENTED / DEVICE PENDING | SAF video/folder pickers, Room recents, real empty state, URL dialog with validation (Robolectric-tested). |

## Player (`PLY`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| PLY-1 | 2 | IMPLEMENTED / DEVICE PENDING | Top bar back/title/audio/subtitle/decoder/More+PiP in `PlayerCanvas`; quick actions column present. |
| PLY-2 | 2 | IMPLEMENTED / DEVICE PENDING | Overlay auto-hide timer (`autoHideControlsMs`, default 3 s) + single-tap toggle wired in `PlayerScreen`. |
| PLY-3 | 2, 3, 5 | IMPLEMENTED / DEVICE PENDING | Seekbar + elapsed/total, prev/next subtitle block, center play + repeat-block, lock/playlist/aspect-ratio corners. |
| PLY-4 | 2 | CI-VERIFIED (bindings) / DEVICE PENDING | Gesture map is data-driven (`gestureBindings`, unit-tested round-trip); implementations: brightness/volume/seek/double-tap (remappable to seek) / two-finger speed. |
| PLY-5 | 2 | IMPLEMENTED / DEVICE PENDING | Rotation lock setting + orientation configChanges + PiP params. |
| PLY-6 | 3, 8 | IMPLEMENTED / DEVICE PENDING | Subtitle list panel (landscape side panel / portrait below), search, current-line highlight, tap-to-seek, no-spoiler mode (long-press toggle). |
| PLY-7 | 2, 3 | IMPLEMENTED / DEVICE PENDING | External (SRT/VTT/ASS) + embedded soft tracks per layer with delay; auto-sidecar matching; charset UTF-8/UTF-16/Windows-1256 (unit-tested). |

## Subtitle controls (`SUB`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| SUB-1 | 3 | IMPLEMENTED / DEVICE PENDING | Two toggle buttons with tap-toggle and hold-invert; size/alpha/position settings per button (clamped + round-tripped in unit tests). |
| SUB-2 | 3 | IMPLEMENTED / DEVICE PENDING | `DockMode` QUICK_COLUMN / BOTTOM_BAR / FLOATING / HIDDEN applied in `PlayerCanvas`. |
| SUB-3 | 3 | IMPLEMENTED / DEVICE PENDING | Layout mode drag adjusts each layer's `…SubtitlePosition` independently; subtitle text gestures otherwise reserved for translation. |
| SUB-4 | 4 | CI-VERIFIED (hit-test logic) / DEVICE PENDING | Tap-count mapping word/line/block + drag phrase in `SubtitleOverlay` (token offsets unit-tested incl. Persian); pause-on-lookup / dismiss-resume in `PlayerScreen`. |
| SUB-5 | 6 | CI-VERIFIED (styles) / POS LATER | My Words / known styles applied via `StyledWordRange` (dotted/outline drawn with layout boxes); POS/phrase styles disabled behind `WordAnalyzer` stub. |
| SUB-6 | 8 | CI-VERIFIED | `removeLineBreaks` + `splitByMaxCharacters` unit-tested (punctuation-aware cuts, duration preserved); batch dialog + SRT export; AI re-seg/quotes LATER flags. |
| SUB-7 | 3 | CI-VERIFIED | Normalizer unit tests cover tag stripping, entity decode, stray-space-before-punctuation, fragment merge, mid-sentence splits, desync-tolerant per-layer delay settings. |

## Shadowing (`SHD`) and Learning (`LRN`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| SHD-1 | 5 | CI-VERIFIED (logic) / DEVICE PENDING | `repeatBlock` one-tap = two passes, hold = auto-repeat (`RepeatState`), user action cancels session (controller logic + FakePlayer). |
| SHD-2 | 5 | CI-VERIFIED | `RepeatPlanner.delayAfterBlock` / `nextStartMs` unit-tested (base pause + duration multiplier). |
| SHD-3 | 5 | CI-VERIFIED (formula) | `shouldPauseAtBlockEnd` xor of toggle+temporary-invert unit-tested; wired to `setStopAtBlockEnd`. |
| LRN-1 | 4 | IMPLEMENTED / DEVICE PENDING | ML Kit word/line/block/phrase lookup cards (proudvocab-inspired), bookmark-to-My-Words, full-details icon gated to disabled dictionary (Coming Soon). |
| LRN-2 | 6 | IMPLEMENTED / DEVICE PENDING | `UnknownWordLevelProvider` (known-state-based, never invents CEFR; unit-tested) drives animated `LearningPopupStack` cards. |
| LRN-3 | 6 | LATER / DISABLED | `WordAnalyzer`/`NotImplementedWordAnalyzer`, `OFFLINE_NLP=false`, POS/phrase style switches disabled. |
| LRN-4 | 4 | CI-VERIFIED (persistence design) / DEVICE PENDING | Room FTS saved words + known marking; repository test is instrumented (emulator job non-blocking) — schema/DAO compile-verified. |

## AI (`AI`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| AI-1 | 7 | CI-VERIFIED (offline parts) | Provider abstraction + Gemini/OpenAI/Anthropic HTTP adapters; Keystore-encrypted keys, excluded from export; model field in settings. Live HTTPS call needs device + user key. |
| AI-2 | 7 | IMPLEMENTED / DEVICE PENDING | Long-press → prompt editor dialog; selected-or-block payload; loading ring; pause-until-answer with manual resume. |
| AI-3 | 7 | CI-VERIFIED (prompt) | Default prompt demands tone/why/synonym-difference/other uses + uncertainty (see `DEFAULT_AI_PROMPT`). |
| AI-4 | 7 | CI-VERIFIED | `AiContextBuilder` unit-tested for previous-N / film title / timestamp toggles and selection fallback. |

## Engineering, licensing, docs and release

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| ENG-1 | 0 | CI-VERIFIED | Version catalog, multi-module build, AGP 8.7.3/Kotlin 2.0.21/Gradle 8.9, minSdk 26 (justified in DECISIONS #1), compile/target 35 — green CI run `37857249419`. |
| ENG-2 | 0, 2, 4, 7 | CI-VERIFIED | Ports: `PlayerController` (+FakePlayer for tests), `SubtitleRepository`, `TranslationProvider`, `AiProvider`, `DictionaryProvider`, `WordAnalyzer`, `WordLevelProvider`, `SpeechToText`, `UpdateChecker` — all wrapped, LATER ones `NotImplemented`. |
| ENG-3 | 3 | CI-VERIFIED | `SubtitleParsingTest` (12 tests): SRT/VTT/ASS, BOM, multi-hour stamps, NOTE blocks, commas, charset UTF-16LE/BOM/Windows-1256. |
| ENG-4 | 3 | CI-VERIFIED | `CueTimelineIndex` O(log n) tests (overlap, gaps, half-open), normalizer/batch tools tests, token offsets mixed EN/FA. |
| ENG-5 | 3 | IMPLEMENTED / DEVICE PENDING | SAF tree sidecar lookup + layer persistence code paths exist; needs provider/device validation. |
| ENG-6 | 2 | IMPLEMENTED / DEVICE PENDING | Decoder modes only offered when `MediaCodecSelector` reports matching decoders (`supportedDecoderModes`); HW+/SW mapping documented (DECISIONS #7). |
| ENG-7 | 2 | IMPLEMENTED / DEVICE PENDING | Single gesture layer with priority subtitle-text > buttons > video surface in `PlayerGestures`/`SubtitleOverlay` input chain. |
| ENG-8 | 0, 1 | CI-VERIFIED | Typed `AppSettings` + `SettingsCodec` round-trip/clamp/migration tests; JSON export/import in settings UI. |
| ENG-9 | 0, 7 | IMPLEMENTED / DEVICE PENDING | `AndroidKeystoreSecretStore` (AES/Keystore, no-backup app-private file); instrumentation check pending device. |
| ENG-10 | 1, 2, 9 | IMPLEMENTED / DEVICE PENDING | Intent restore, Room progress, PiP, audio focus (`setHandleAudioBecomingNoisy`), rotation — need API 31 pass. |
| ENG-11 | 3, 9 | CI-VERIFIED (complexity) | Cue lookup binary-indexed segment tree; overlay work is Compose-idiomatic (no per-frame allocation in steady state). Frame profiling still due on device. |
| ENG-12 | 9 | CI-VERIFIED (unit/UI) | JVM: 12 domain + 12 subtitle tests + 4 Robolectric Compose UI tests (API 31) green in run `37857249419`. Instrumented API 31 job runs non-blocking (last attempt failed — see KNOWN_ISSUES). |
| ENG-13 | 0, 9 | CI-VERIFIED (resources) | EN/FA parity across all 6 resource sets (automatically re-checkable script in docs); light/dark/AMOLED themes via `ThemeMode`; font scaling supported by sp units. TalkBack pass due on device. |
| LIC-1 | 0, 9 | PARTIAL (legal review open) | Apache-2.0 app license, `THIRD_PARTY_NOTICES.md` maintained; no GPL code/data/models committed. REQ-001 (dictionary provenance) and REQ-003 (ML Kit terms) remain owner decisions. |
| DOC-1 | 0, 9 | CI-VERIFIED (present) | Full doc set maintained and cross-linked (`AGENTS.md` entry point). |
| REL-1 | 9 | **RELEASED** | `v0.1.0` published 2026-10-09: https://github.com/humorheatyt/SubLearn/releases/tag/v0.1.0 — signed APKs `SubLearn-0.1.0-{arm64-v8a,armeabi-v7a,x86_64,universal}.apk` + `SHA256SUMS.txt` (v2+v3 signatures verified with `apksigner`). Green CI on `main` (`3304340`). |

## LATER interfaces/stubs and disabled UI

| LATER ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| LAT-1 YouTube | 0 | LATER / DISABLED | `YouTubeCatalog`/`NotImplementedYouTubeCatalog`, `YOUTUBE=false`, disabled tab/menu. |
| LAT-2 PDF/browser/image learning | 0, 1 | LATER / DISABLED | `PdfLearningProvider`/stub, false flag, Learn Coming Soon; PDF intents route only. |
| LAT-3 offline dictionary/import | 0 | LATER / DISABLED | `DictionaryProvider`/stub, false flag, disabled drawer/settings entries; no content shipped (REQ-001). |
| LAT-4 automatic level | 0 | LATER / DISABLED | `AutomaticLevelDetector`/stub; manual level + known-state fallback only. |
| LAT-5 quiz | 0 | LATER / DISABLED | `QuizProvider`/stub and `QUIZ=false`. |
| LAT-6 update checker | 0 | LATER / DISABLED | `UpdateChecker`/stub and `UPDATE_CHECKER=false`. Stretch candidate (1) per scope order. |
| LAT-7 AI re-segmentation/quote marking | 0, 8 | LATER / DISABLED | `SubtitleAiTools`/stub and `AI_SUBTITLE_TOOLS=false`; local batch tools are separate NOW code. |
| LAT-8 offline speech-to-text | 0 | LATER / DISABLED | `SpeechToText`/stub and false flag. |
| LAT-9 POS/phrasal/collocation/idiom/CEFR | 0, 6 | LATER / DISABLED | `WordAnalyzer`/stub and `OFFLINE_NLP=false`. |
| LAT-10 on-device AI models | 0 | LATER / DISABLED | `OnDeviceAiModelProvider`/stub and `ON_DEVICE_AI=false`. |
| LAT-11 additional languages | 0 | LATER / DISABLED | BCP-47 ports everywhere; only EN/FA exposed/localized (`extraLanguageFeatureEnabled=false`). |

## Global completion state

- **Compiler/unit-UI/lint gate: GREEN** (run `37857249419`).
- **Instrumented emulator gate:** attempted non-blocking; failing — cause not visible from the sandbox (logs host blocked); tracked in KNOWN_ISSUES and fixable in CI via the report artifacts.
- **Device gate (Poco X3 Pro / API 31):** not performed in this environment; required before calling the device-dependent rows done-done (ENG-10, PLY gestures feel, PiP, decoder quirks).
- **Rights review:** REQ-001/REQ-003 owner decisions; REQ-006 signing-secrets convenience.
- **Release:** `v0.1.0` is published (see REL-1 row and `docs/PROGRESS.md`).
