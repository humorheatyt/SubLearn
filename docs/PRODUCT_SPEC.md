# SubLearn product and engineering specification

**Status:** source of truth for this pre-release branch. Rows are not complete merely because a code path exists; see `docs/CHECKLIST.md` for the current implementation status and verification evidence. Defaults: UI English, learning English (`en`), native/translation Persian (`fa`), Android 12/Poco X3 Pro priority, Kotlin/Compose/Media3, no ads or monetization.

## General

- **GEN-1 — Product quality/design.** Deliver a modern, polished, animated UI. Design direction: cinematic player stage, amber learning accent, indigo interactive cards, readable Material 3 surfaces, shared tokens; see `DESIGN_SYSTEM.md`.
- **GEN-2 — Customization.** Expose useful alternatives as settings instead of hard-coded feature choices.
- **GEN-3 — Typography isolation.** Configure font family, size, color, weight and direction per language role (learning/native) *and* per surface (app menu, subtitles, popups, word cards, AI answer); surfaces must not bleed.
- **GEN-4 — RTL.** Persian direction must be correct per text run/surface, including mixed English/Persian lines and token hit-testing.
- **GEN-5 — Android behavior.** Use permissions only when needed, SAF/scoped storage, lifecycle cleanup, PiP, rotation, process-death restore and accessible back behavior.
- **GEN-6 — Modularity.** A new feature plugs behind a module/port without breaking existing paths.
- **GEN-7 — Offline.** NOW playback/learning paths work offline once the selected on-device translation model has been downloaded; explicit AI/network paths explain that they are online.

## App entry and navigation

- **APP-1 — External video/URL entry.** A video/stream intent opens directly in Player; HTTP(S) URL/share is validated and routed.
- **APP-2 — Deferred document entry.** PDF belongs to the Learn section but PDF/browser/image learning is LATER; route an external PDF to Learn and show a disabled Coming Soon explanation without reading it.
- **APP-3 — Normal entry.** Show modern Home/YouTube/Learn/Dictionary navigation and a side menu for Level, My Words, Quiz, updates and Settings. LATER destinations remain disabled, never demo-filled.
- **APP-4 — Home/recent.** Home lists actual recent local videos/media from Room, supports user-selected videos/SAF folders and direct URL entry, and has truthful empty states. Never ship sample content.

## Player (`PLY`)

- **PLY-1 — Top bar.** Transparent overlay: back/title, audio/subtitle track actions, decoder preference, More/options including PiP.
- **PLY-2 — Overlay and idle.** Controls overlay video and hide after the configured idle period (default 3s); a single tap shows controls.
- **PLY-3 — Transport.** Center play/pause/repeat-current subtitle block; bottom seekbar with buffer/elapsed/duration and previous/play/next subtitle navigation; corners expose lock, list, playlist, ratio and modes.
- **PLY-4 — Gestures.** Left vertical brightness, right vertical volume, horizontal direction-aware seek, configurable double tap except buttons/subtitle, and two-finger up speed shortcut. Actions are remappable and avoid system edges.
- **PLY-5 — Orientation.** Portrait/landscape playback and orientation lock.
- **PLY-6 — Subtitle List.** Landscape right panel / portrait below player, active cue highlight/scroll, tap-to-seek, search; long-press toggle activates no-spoiler filtering.
- **PLY-7 — Media3 and tracks.** Local and supported HTTP(S) sources; independent learning/translation layers and multiple external/embedded subtitle tracks, with per-layer options.

## Subtitle controls (`SUB`)

- **SUB-1 — Layer toggles.** Learning and translation buttons default to Quick Actions; tap toggles visibility, hold temporarily inverts visibility; each button has size/alpha/position settings.
- **SUB-2 — Docking.** Quick column, bottom bar, free-floating or hidden modes.
- **SUB-3 — Layout mode.** Independently adjust each layer's vertical position; normal subtitle text remains reserved for lookup rather than unrelated video gesture handling.
- **SUB-4 — Lookup.** Learning text: one tap word, two taps visual line, three taps block, drag selects a phrase. Pause during result; tapping elsewhere dismisses and resumes only if previously playing.
- **SUB-5 — Word marks.** Independent style settings for My Words and known words; POS and phrasal/collocation style settings exist but remain disabled until the LATER analyzer is real. Supported visual styles include underline, dotted underline, box/outline, background, bold, color.
- **SUB-6 — Batch tools.** Remove line breaks within blocks and split by max characters. AI re-segmentation and AI quote marking are LATER.
- **SUB-7 — Cue quality.** Normalize fragment boundaries, whitespace/newlines, punctuation and layer delays/positions. Avoid inventing text or timestamps.

## Shadowing (`SHD`)

- **SHD-1 — Repeat.** Repeat button tap gives exactly one additional current-cue pass; press/hold activates finite automatic repeat, canceled by user playback/navigation actions.
- **SHD-2 — Count/delay.** Configure repeat count and pause; delay formula depends on cue duration with adjustable multiplier.
- **SHD-3 — Stop at block.** Toggle pauses at cue end; hold temporarily inverts the persistent state.

## Learning/vocabulary (`LRN`)

- **LRN-1 — Entertainment.** Tap word/line/block and drag-select phrase for a real translation; save/bookmark; card separates source, target and context. Full offline dictionary detail is LATER, shown disabled/Coming Soon; never fabricate a definition.
- **LRN-2 — Learning mode.** Popup cards show current-block words not marked known, with real translation, manual A1–C2 level and configurable count/opacity. No unlicensed CEFR/frequency data or invented level labels.
- **LRN-3 — NLP.** English-only offline POS/phrasal verbs/collocations/idiom/word level analysis is LATER behind `WordAnalyzer`; no analysis is claimed until implemented.
- **LRN-4 — My Words.** Persist, search/list, mark known/unmark and remove vocabulary in Room; highlight saved/known tokens with independent styles.

## AI assistant (`AI`)

- **AI-1 — Providers/settings.** Gemini default plus OpenAI/Anthropic adapters, provider/model selection, editable prompt and encrypted API-key settings; keys are never exported or logged.
- **AI-2 — Interaction.** Button has loading ring; long press edits prompt; send selected text if available else full cue; pause until result and allow explicit resume/cancel.
- **AI-3 — Answer.** Explain tone, contextual use, differences from near-synonyms and other usage, concisely and with uncertainty where appropriate.
- **AI-4 — Context.** Previous N blocks (default 10), film title and timestamps are configurable. Clearly disclose that AI sends context to the selected provider and is online-only.

## Engineering, safety and delivery (`ENG`, `LIC`, `DOC`, `REL`)

- **ENG-1 — Stack/build.** Kotlin, Compose + Material 3, Media3, Coroutines/Flow, Room/FTS, DataStore, Koin, OkHttp/ML Kit, version catalog and `app`/`core:*`/`feature:*` modules; Android 12 first-class and min SDK justified in `DECISIONS.md`.
- **ENG-2 — Ports.** Wrap player, subtitle parser/repository, translation, AI, dictionary, analyzer, word level, speech-to-text and update-check capabilities behind interfaces; provide Media3 and deterministic FakePlayer implementations.
- **ENG-3 — Parsers.** Pure SRT, WebVTT, text ASS/SSA to one Cue model: id/start/end/text/tokens/track ID.
- **ENG-4 — Normalization.** Test markup removal, whitespace, merge/split, punctuation-aware cuts, max chars, charset detection (UTF-8, UTF-16, Windows-1256) and O(log n) cue lookup.
- **ENG-5 — Subtitle tracks.** Own Compose overlay with bidi-correct text hit testing; embedded/external tracks, sidecar matching, per-layer timing; preserve source cues.
- **ENG-6 — Decoder mapping.** `AUTO` delegates; HW/SW prioritize exposed `MediaCodec` capability; HW+ enables fallback. No FFmpeg until ABI/build/license are reviewed; unsupported modes hidden or reported.
- **ENG-7 — Gesture priority.** Single layer with subtitle/button priority over video, system-edge guard, remappable actions.
- **ENG-8 — Settings.** Typed, versioned, searchable settings grouped by Player/Subtitles/Fonts/Gestures/Shadowing/Learning/AI/Dictionary/Appearance/About, JSON export/import.
- **ENG-9 — Secrets.** Keystore-backed encryption; no key in logs, JSON, backup, repo or artifacts.
- **ENG-10 — Android.** SAF, intent filters, PiP/audio focus/process-death progress and track/layout restore, edge-to-edge, predictive back and large-screen/rotation behavior.
- **ENG-11 — Performance.** No blocking work on main; cue lookup O(log n); responsive Compose transitions.
- **ENG-12 — Testing.** Parser/normalizer/repeat/settings/repository unit tests; Compose key-screen tests using FakePlayer; RTL checks; API 31 instrumented smoke where feasible.
- **ENG-13 — Accessibility/i18n.** EN+FA resources, content descriptions, targets/font scale, light/dark/AMOLED themes, shared tokens; verify TalkBack/RTL.
- **LIC-1 — License safety.** Apache-2.0 or MIT original code; no GPL code; audit all dependencies/assets; never commit restricted dictionary/model data; user-supplied dictionary import only.
- **DOC-1 — Documentation.** `AGENTS.md` is the single agent entry point; required references, product, architecture, design, decisions, phases, checklist, extension, requests, issues, progress and notices remain current.
- **REL-1 — Release gate.** CI builds APK and runs tests/lint; release workflow attaches APK/checksum. Do not tag/claim v0.1.0 until all NOW rows are implemented+verified, CI green, rights reviewed and artifact exists.

## NOW/LATER scope

NOW: GEN-1..7, APP-1/3/4, PLY-1..7, SUB-1..7, SHD-1..3, LRN-1/2/4, AI-1..4, ENG-1..13, LIC-1, DOC-1, REL-1.

LATER only: APP-2 implementation beyond a Coming Soon route; YouTube, PDF/browser/image learning, dictionary import/lookup, automatic level detection, quiz, update checker, AI subtitle re-segmentation/quote marking, offline speech-to-text, POS/phrasal/collocation/idiom/CEFR analyzer, local AI models and additional languages. See `EXTENSION_POINTS.md`.
