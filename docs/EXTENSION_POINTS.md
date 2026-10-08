# Extension points

LATER features are intentionally not implemented. Keep them behind the ports/flags below; do not replace a disabled state with sample output or a network shortcut.

| Capability | Domain interface / location | Flag/UI | Current behavior | Safe extension path |
|---|---|---|---|---|
| YouTube catalogue/player | `YouTubeCatalog` in `core/domain` | `FeatureFlags.YOUTUBE=false`; disabled tab/menu | `NotImplementedYouTubeCatalog` throws if called | Add a provider only after endpoint/API policy and provider terms are documented; isolate all undocumented endpoints behind this port; do not scrape/download. |
| PDF/browser/image learning | `PdfLearningProvider` | `PDF_BROWSER_IMAGE_LEARNING=false`; Learn entry disabled | `NotImplementedPdfLearningProvider`; external PDF opens Learn and shows Coming Soon | Add import/view lifecycle, document permissions, text extraction/OCR and accessibility as a separate feature; keep video player untouched. |
| Offline dictionary/import | `DictionaryProvider` (`search`, `importUserDatabase`) | `OFFLINE_DICTIONARY=false`; drawer/settings Coming Soon | `NotImplementedDictionaryProvider`; no DB/content shipped | User selects a file via SAF. Define and publish an independent SQLite schema, validate version/indexes/size/encoding, sandbox import, and audit every record/source license. Never use `fastdic_plain.sqlite`, derived content or `.bipe` models. Resolve REQ-001 first. |
| Automatic level detection | `AutomaticLevelDetector` | `AUTOMATIC_LEVEL=false` | `NotImplementedAutomaticLevelDetector`; manual A1–C2 setting is real | Add a licensed, privacy-safe analysis source. Manual setting and `WordLevelProvider` must continue to work if detection is absent. |
| Quiz | `QuizProvider` | `QUIZ=false`; disabled side-menu item | `NotImplementedQuizProvider` throws if called | Build question generation and study state as a new feature; no change to My Words persistence contract. |
| Release update checker | `UpdateChecker` | `UPDATE_CHECKER=false`; disabled side-menu item | `NotImplementedUpdateChecker` | Query GitHub Releases only through a replaceable implementation; verify HTTPS, rate limits, release signatures/checksums, and do not download/install silently. |
| AI subtitle re-segmentation / quote marking | `SubtitleAiTools` | `AI_SUBTITLE_TOOLS=false` | `NotImplementedSubtitleAiTools` | Add explicit user confirmation, undo/preview and timing-safe transformed cues. Existing local batch tools remain independent. |
| Offline speech-to-text | `SpeechToText` | `OFFLINE_SPEECH_TO_TEXT=false` | `NotImplementedSpeechToText` | Select a model/runtime only after license, size, ABI, privacy and Android 12 performance review. Never check in model weights. |
| POS/phrasal/collocation analysis | `WordAnalyzer` | `OFFLINE_NLP=false`; style controls disabled with Coming Soon | `NotImplementedWordAnalyzer` | Add a verified permissively licensed analyzer/list behind the port; preserve independent My Words styling and no-list fallback. |
| On-device AI models | `OnDeviceAiModelProvider` | `ON_DEVICE_AI=false`; no visible provider choice | `NotImplementedOnDeviceAiModelProvider` throws if called; remote Gemini/OpenAI/Anthropic only | Add only after model licensing, footprint, device capability, battery/privacy and cancellation review. |
| Additional languages | BCP-47 `AppSettings` fields and provider ports | `ADDITIONAL_LANGUAGES=false` | EN learning + FA native localized now | Add complete resources, RTL/script test cases, ML Kit support checks, cue tokenization tests and per-role fonts before exposing a language. |
| Part-of-speech and phrase marks | `WordAnalyzer` + `WordStyleSettings` | Separate style settings disabled | Controls show Coming Soon; no analyzer is called | Keep user-word and known-word styles independent; add source attribution for any analysis data. |

## Contribution rules

1. Add/adjust a domain interface before platform implementation; keep API endpoints replaceable.
2. Add a disabled `FeatureFlags` constant until the real path is complete and tested.
3. Use explicit `NotImplemented` behavior and disabled localized UI rather than plausible fake output.
4. Add the feature ID/phase/status/verification to `docs/CHECKLIST.md`, update this table, decisions, notices, and known issues.
5. Add unit tests for pure logic and UI/instrumented tests for the user-visible path; run the requested full Gradle gate.
