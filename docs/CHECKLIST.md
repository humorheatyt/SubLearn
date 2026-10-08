# Specification checklist

**Snapshot:** 2026-10-08, session branch `arena/4386d633-sublearn`. Status vocabulary: `PARTIAL / UNVERIFIED` means code is present but the requested behavior has not passed the full build/device gate; `BLOCKED` means verification cannot currently run; `LATER / DISABLED` is intentional and not a NOW deliverable. “Verification method” is the required check; “current evidence” says what has actually run. XML/resource checks and tree-sitter parsing do not count as compilation.

## General, entry and Home

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| GEN-1 | 0, 9 | PARTIAL / UNVERIFIED | Review Material 3 tokens/screens and motion on phone/tablet; not device-checked. |
| GEN-2 | 0–8 | PARTIAL / UNVERIFIED | Settings export/import and feature controls; source exists, Gradle tests not run. |
| GEN-3 | 0, 3, 6 | PARTIAL / UNVERIFIED | Inspect independent per-role/per-surface font/color/weight/direction changes in Compose/RTL UI; not run. |
| GEN-4 | 0, 3, 4 | PARTIAL / UNVERIFIED | Mixed EN/FA TalkBack, bidi hit-testing and layout test; no RTL emulator run. |
| GEN-5 | 1, 2, 9 | PARTIAL / UNVERIFIED | Inspect SAF permission/lifecycle/PiP/process restore on API 31 device; no device run. |
| GEN-6 | 0–9 | PARTIAL / UNVERIFIED | Inspect module/port boundaries and build each module; Kotlin syntax parsed, no compiler run. |
| GEN-7 | 4 | PARTIAL / BLOCKED | Download EN↔FA model then translate offline; API code exists, SDK/terms/clean-device behavior unverified. |
| APP-1 | 1, 2 | PARTIAL / UNVERIFIED | `ACTION_VIEW` video/HTTP(S) URL/share opens player; manifest and route code present, no intent test/device run. |
| APP-2 | 1 | PARTIAL / UNVERIFIED; PDF content LATER | Open PDF intent should route to Learn and show disabled Coming Soon; no content read. No instrumentation result yet. |
| APP-3 | 1 | PARTIAL / UNVERIFIED | Home/Learn/side menu navigation, disabled LATER tabs; instrumented smoke source added, not executed. |
| APP-4 | 1 | PARTIAL / UNVERIFIED | SAF video/folder listing, Room recents, real empty state, URL dialog; test via API 31 SAF/device, not executed. |

## Player (`PLY`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| PLY-1 | 2 | PARTIAL / UNVERIFIED | Verify back/title/audio/subtitle/decoder/More/PiP controls against Media3; no device run. |
| PLY-2 | 2 | PARTIAL / UNVERIFIED | FakePlayer Compose test + idle timer/show-on-tap test; only a player pause smoke test is authored, not run. |
| PLY-3 | 2, 3, 5 | PARTIAL / UNVERIFIED | FakePlayer seek/buffer/progress and cue navigation tests; no executed test. |
| PLY-4 | 2 | PARTIAL / UNVERIFIED | Gesture unit/Compose tests for each direction, remapping, priority and system-edge guard; no executed test. |
| PLY-5 | 2, 9 | PARTIAL / UNVERIFIED | Rotation lock/portrait/landscape/PiP on API 31 and Poco X3 Pro; not run. |
| PLY-6 | 3, 8 | PARTIAL / UNVERIFIED | List search/highlight/auto-scroll/no-spoiler/tap-to-seek UI test both orientations; not run. |
| PLY-7 | 2, 3 | PARTIAL / UNVERIFIED | Open local/HTTP stream, select multiple external/embedded tracks and confirm independent layer rendering/delay; no build/device test. |

## Subtitle controls (`SUB`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| SUB-1 | 3 | PARTIAL / UNVERIFIED | Tap/hold/release layer visibility and persisted size/alpha/position tests; not run. |
| SUB-2 | 3 | PARTIAL / UNVERIFIED | Exercise quick column, bar, floating and hidden docking modes; not run. |
| SUB-3 | 3 | PARTIAL / UNVERIFIED | Drag learning/native layer separately and verify text/video gestures do not clash; not run. |
| SUB-4 | 4 | PARTIAL / UNVERIFIED | Tap word/line/block, drag phrase, pause/dismiss/resume and Persian bidi hit-test; not run. |
| SUB-5 | 6 | PARTIAL / UNVERIFIED; POS/phrase LATER | Validate independent My Words/known mark styles. Analyzer controls are visibly disabled and use `NotImplementedWordAnalyzer`; no UI run. |
| SUB-6 | 8 | PARTIAL / UNVERIFIED | Unit-test flatten/split boundaries and verify apply/SAF export. Parser/unit source exists, no test has run. |
| SUB-7 | 3 | PARTIAL / UNVERIFIED | Parser/normalizer edge cases for split cues, whitespace, punctuation and per-layer delay; unit source exists, not run. |

## Shadowing (`SHD`) and Learning (`LRN`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| SHD-1 | 5 | PARTIAL / UNVERIFIED | FakePlayer proves one tap = two passes, hold starts finite auto-repeat, user action cancels; no test run. |
| SHD-2 | 5 | PARTIAL / UNVERIFIED | `RepeatPlanner` formulas have JVM test source; full pass blocked by REQ-002. |
| SHD-3 | 5 | PARTIAL / UNVERIFIED | Verify toggle pauses at active delayed cue end and hold temporarily inverts; formula source test not executed. |
| LRN-1 | 4 | PARTIAL / UNVERIFIED; full details LATER | Real ML Kit word/line/block/phrase translation, pause/resume, save; full dictionary detail control is disabled/Coming Soon. No runtime test. |
| LRN-2 | 6 | PARTIAL / UNVERIFIED | Verify manual level, unknown-only popup list, real translation, animation and settings; no UI/device run. |
| LRN-3 | 6 | LATER / DISABLED | `WordAnalyzer` and `NotImplementedWordAnalyzer`, false `OFFLINE_NLP`, disabled POS/phrase styles; inspect flag/stub, no analyzer claimed. |
| LRN-4 | 4 | PARTIAL / UNVERIFIED | Room FTS save/search/mark known/remove and subtitle styling; no Room/device tests run. |

## AI (`AI`)

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| AI-1 | 7 | PARTIAL / UNVERIFIED; ML Kit license open | Verify each HTTPS provider, model choice, Keystore persistence, key exclusion from export/logs. Source exists; REQ-003 unresolved. |
| AI-2 | 7 | PARTIAL / UNVERIFIED | Fake/provider UI test for long-press editor, selected-text fallback, ring, cancellation/pause/resume; not run. |
| AI-3 | 7 | PARTIAL / UNVERIFIED | Provider contract/prompt test for requested tone/synonym/use explanation and uncertainty; not run. |
| AI-4 | 7 | PARTIAL / UNVERIFIED | `AiContextBuilder` has unit source for previous-N/title/timestamp config; no tests run. |

## Engineering, licensing, docs and release

| Spec ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| ENG-1 | 0 | PARTIAL / BLOCKED | Verify version catalog/modules/Android 12 baseline via Gradle; wrapper JAR restored, distribution fetch blocked (REQ-002). |
| ENG-2 | 0, 2, 4, 7 | PARTIAL / UNVERIFIED | Inspect domain ports and Media3/test FakePlayer; FakePlayer is in Android instrumentation sources; no compile/run. |
| ENG-3 | 3 | PARTIAL / UNVERIFIED | `SubtitleParsingTest` SRT/VTT/ASS/charset source; `testDebugUnitTest` not run. |
| ENG-4 | 3 | PARTIAL / UNVERIFIED | Unit tests cover normalization, token offsets, timeline and batch changes; not run. |
| ENG-5 | 3 | PARTIAL / UNVERIFIED | SAF tree sidecar/layer persistence, embedded cues and bidi selection device tests; no run. |
| ENG-6 | 2 | PARTIAL / UNVERIFIED | Compare exposed MediaCodec modes and playback on Poco; no device run. |
| ENG-7 | 2 | PARTIAL / UNVERIFIED | Event-priority tests for subtitle > buttons > video and edge guard; code inspection only. |
| ENG-8 | 0, 1 | PARTIAL / UNVERIFIED | Settings codec/migrations/search/JSON round-trip tests; source exists, not run. |
| ENG-9 | 0, 7 | PARTIAL / UNVERIFIED | Keystore encrypt/decrypt, no-backup and export exclusion instrumentation tests; not run. |
| ENG-10 | 1, 2, 9 | PARTIAL / UNVERIFIED | SAF, external intent, PiP/audio focus, rotation, lifecycle/process-death restore on API 31; not run. |
| ENG-11 | 3, 9 | PARTIAL / UNVERIFIED | Profile cue indexing and Compose frame behavior on a representative device; no profiling. |
| ENG-12 | 9 | PARTIAL / BLOCKED | JVM parser/domain tests and Compose smoke test source present; required Gradle task and emulator tests have not executed. |
| ENG-13 | 0, 9 | PARTIAL / UNVERIFIED | Resource parity/XML checks pass; TalkBack/font scale/light-dark/AMOLED/RTL tests not run. |
| LIC-1 | 0, 9 | PARTIAL / BLOCKED | Audit `LICENSE`/`THIRD_PARTY_NOTICES.md`; no GPL assets/data copied. ML Kit terms REQ-003 and dictionary rights REQ-001 remain open. |
| DOC-1 | 0, 9 | PARTIAL | Required docs/pointers created; needs a final cross-link/accuracy pass and updates after CI/license decisions. |
| REL-1 | 9 | BLOCKED / NOT RELEASED | CI build/API31 job and tag workflow configured; no green CI, APK, PR merge or `v0.1.0` tag observed. |

## LATER interfaces/stubs and disabled UI

| LATER ID | Phase | Status | Verification method / current evidence |
|---|---|---|---|
| LAT-1 YouTube | 0 | LATER / DISABLED | `YouTubeCatalog`/`NotImplementedYouTubeCatalog`, `YOUTUBE=false`, disabled tab/menu; verify no endpoint called. |
| LAT-2 PDF/browser/image learning | 0, 1 | LATER / DISABLED | `PdfLearningProvider`/stub, false flag, disabled Learn entry; PDF routes to Learn/Coming Soon only; instrumentation unrun. |
| LAT-3 offline dictionary/import | 0 | LATER / DISABLED | `DictionaryProvider`/stub, false flag, disabled drawer/settings; no content shipped; REQ-001 open. |
| LAT-4 automatic level | 0 | LATER / DISABLED | `AutomaticLevelDetector`/stub and false flag; manual level remains. |
| LAT-5 quiz | 0 | LATER / DISABLED | `QuizProvider`/stub and `QUIZ=false`; disabled menu entry; no quiz generator. |
| LAT-6 update checker | 0 | LATER / DISABLED | `UpdateChecker`/stub and false flag; disabled menu entry. |
| LAT-7 AI re-segmentation/quote marking | 0, 8 | LATER / DISABLED | `SubtitleAiTools`/stub and `AI_SUBTITLE_TOOLS=false`; local batch tools are separate. |
| LAT-8 offline speech-to-text | 0 | LATER / DISABLED | `SpeechToText`/stub and false flag; no model shipped. |
| LAT-9 POS/phrasal/collocation/idiom/CEFR | 0, 6 | LATER / DISABLED | `WordAnalyzer`/stub and `OFFLINE_NLP=false`; style UI disabled. |
| LAT-10 on-device AI models | 0 | LATER / DISABLED | `OnDeviceAiModelProvider`/stub and `ON_DEVICE_AI=false`; no model/runtime shipped. |
| LAT-11 additional languages | 0 | LATER / DISABLED | BCP-47 settings/provider ports and `ADDITIONAL_LANGUAGES=false`; only EN/FA are exposed/localized. |

## Global completion state

**Definition of Done: NOT MET.** All NOW rows still need compiler/test/lint/device/CI/license evidence. REQ-002 is the current local execution blocker; REQ-001/REQ-003 are rights review items. No v0.1.0 release exists.
