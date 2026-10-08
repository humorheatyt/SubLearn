# Reference study and license-safe decisions

Reviewed before implementation on 2026-10-08. The references inform product ideas only unless an explicit permissive source is identified below. No source files, logos, screenshots, sample media, dictionaries, wordlists or trained models from the reference repositories are shipped by SubLearn.

## `hoangkien1703/dual-sub-replay`
- **What was studied:** current README and architecture/build notes, `SubtitleMerger`, the translation-engine boundary, caption-provider separation, and Android CI workflow. Its own notice distinguishes its MIT source from historical GPL-distributed releases/dependencies; the current app includes AndroidX/Compose/Media3/ML Kit/OkHttp and has CI gates for unit tests, lint, APK and managed-device checks.
- **Adopt as ideas:** keep caption/translation capabilities behind small replaceable interfaces; normalize/merge short cue fragments with duration/gap/character bounds; cache/execute translation away from the UI; make CI a required quality gate; preserve text if a translation fails.
- **Reject:** YouTube/caption retrieval and its undocumented endpoint, language-specific Japanese logic, code/assets, and the reference's current multi-language/browser product scope. SubLearn opens local media/URLs and its own subtitle files only. The user-provided reference states MIT, but no reference code is reused, so no upstream MIT notice is needed as a copied-code notice.
- **Source/license:** https://github.com/hoangkien1703/dual-sub-replay (MIT for original repository source; historical release caveats are described in that repository's `THIRD_PARTY_NOTICES.md`).

## `kgurniak91/yall-mp`
- **What was studied:** README feature concepts for an editable subtitle timeline, listen/speak presets, contextual speed, word tokenization, and sentence mining.
- **Adopt as ideas:** cue-oriented operations and explicit practice presets as extension points; keep the initial playback loop understandable and subtitle-centric.
- **Reject:** all code, design assets and dependencies. It is a desktop Electron/Angular/mpv project under GPL-3.0; SubLearn independently implements any similar behavior in Kotlin/Compose.
- **Source/license:** https://github.com/kgurniak91/yall-mp (GPL-3.0; ideas only).

## `arianneorpilla/jidoujisho`
- **What was studied:** README-described subtitle text selection, transcript, current-line seeking, same-name sidecar loading, delay controls and popup dismissal gestures.
- **Adopt as ideas:** selection/lookup is scoped to subtitle text; transcript rows seek to their own timestamps; dismissal and playback-resume behavior are explicit; sidecar matching is a user-visible convenience, not a hidden network feature.
- **Reject:** code, assets, Flutter packages and GPL-3.0 implementation. SubLearn's RTL hit-testing and Compose interaction are written independently.
- **Source/license:** https://github.com/arianneorpilla/jidoujisho (GPL-3.0; ideas only).

## SubX Player (commercial, closed source)
- **What was studied:** only the product-level interaction ideas listed in the request: dual subtitle tracks, gestures for subtitle layout, repeat/auto-pause, seek-by-subtitle and a playlist.
- **Adopt as ideas:** configurable overlay controls and explicit repeat/list surfaces.
- **Reject:** no code or assets were available or used; avoid imitating proprietary graphics. This is UX inspiration only and is not a dependency.

## `melonityhub/proudvocab`
- **What was studied:** the owner's ProudVocab extension/desktop repository's card hierarchy and visual vocabulary: clear headword/translation separation, compact metadata, layered rounded surfaces and restrained accent treatment.
- **Adopt as ideas:** re-create a native Compose word card with independent surface tokens, strong headword emphasis, concise contextual translation and accessible bookmark action.
- **Reject:** no code/CSS, scripts, bundled media, fonts or graphics. GitHub metadata reports no declared repository license; this is not treated as an asset grant. The user's explicit permission to take the *visual idea* is honored by an independent native implementation only.
- **Source:** https://github.com/melonityhub/proudvocab (owner's project; no repository license detected during audit; no files reused).

## `melonityhub/dictionaryproject`
- **What was studied:** README's result-section concepts (meanings, examples, synonyms/antonyms, phrasal verbs, collocations, idioms, word family, CEFR and categories), and the stated import-from-user-file direction.
- **Adopt as ideas:** define a documented, user-supplied SQLite import contract and a result model that can represent optional sections without coupling the player to a dictionary implementation.
- **Reject:** **all** SQLite files, `.bipe` files, translation models, extracted source-app schema/queries, mappings, and any derived content. The README explicitly says its schema/queries were taken from third-party Android app Java source and that its model loader reverse-engineered models; these are not reproduced here. Offline dictionary import/lookup remains LATER.
- **Source/license:** https://github.com/melonityhub/dictionaryproject (GitHub metadata reports no declared repository license; rights provenance in its README is restricted/unclear). The open rights question is logged as `REQ-001` in `docs/AGENT_REQUESTS.md`.

## Resulting boundary

SubLearn's original code is licensed Apache-2.0. Direct Android/Jetpack/Kotlin/Media3/OkHttp/Room/DataStore/Hilt dependencies are independently selected permissive libraries; see `THIRD_PARTY_NOTICES.md`. Google ML Kit is called through the official Android API and downloads its own language model under Google's terms; no model file is redistributed. System fonts and Compose Material icons are used; no third-party font, image, wordlist, dictionary database, binary fixture or proprietary sample media is included.