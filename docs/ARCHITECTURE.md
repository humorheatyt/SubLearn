# Architecture

## Module graph

```text
app
├── core:domain          typed models, settings codec, ports, LATER stubs
├── core:subtitles       pure Kotlin parsing, normalization, tokenization, timeline index
├── core:platform        Media3, Room, DataStore, Keystore, ML Kit, HTTP AI providers
├── core:design          Compose theme and design tokens
└── feature:home / player / learning / settings
```

`feature:player` depends on domain, subtitle, platform and learning surfaces. Features are UI/orchestration modules; platform implementations are created through the Koin graph. Keep feature-to-platform coupling behind domain ports except the player view bridge that must attach Media3's `Player` to `PlayerView`.

## Runtime flows

### Media and lifecycle

`MainActivity` handles `ACTION_VIEW`/share intents, delegates SAF document/folder selection, and routes video/URL requests to Home or directly to Player. SAF read grants are persisted when the provider allows it. Folder listing uses `DocumentsContract` under the selected tree grant; video requests preserve the tree-qualified document URI so the sidecar repository can enumerate siblings. Recent media stores URI/title/MIME/position/duration plus last subtitle URI/layer, not copied video bytes.

`PlayerScreen` owns an injected `PlayerController`, observes its `PlayerSnapshot` and repeat-state flow, and writes playback progress. `Media3PlayerController` owns ExoPlayer, audio focus, track discovery/selection, cue snapshots, decoder preference, session-scoped repeat, block-stop state and cleanup. `PlayerCanvas` owns only rendering/gesture presentation; `SubtitleOverlay` uses Compose `TextLayoutResult` hit-testing rather than Media3's inaccessible built-in text view.

### Subtitle pipeline

`SubtitleFileLoader` reads a user-selected or discovered URI through `ContentResolverSubtitleRepository`. The pure parser handles SRT, WebVTT, and text-only ASS/SSA, decodes UTF-8/UTF-16/Windows-1256, strips tags and normalizes whitespace/tokens. `SubtitleNormalizer` provides merge/split/line-break operations; `CueTimelineIndex` uses sorted intervals and binary search. Learning/native tracks are independent external selections; a selected embedded Media3 track is assigned to the layer selected in the subtitle dialog. Delays are applied at lookup/render time, with matching delay applied when seeking/repeating blocks.

Sidecar matching is exact basename first, then language suffixes (English/Persian), limited to subtitle text extensions. SAF cannot enumerate siblings through a single-file grant; the app therefore offers a separate folder picker.

### Translation, vocabulary and learning

`TranslationProvider` wraps the official ML Kit on-device English↔Persian API. Download is explicit in Settings; no model bytes are bundled. `SavedWordRepository` stores vocabulary and known state in Room. Subtitle marking derives token sets from saved words and known status; learning popups use `WordLevelProvider`, manual level, and words not marked known. No third-party frequency/CEFR list is shipped.

### Settings and secrets

`AppSettings` is a typed serializable schema stored as JSON within DataStore preferences. The `schemaVersion` is checked/migrated by `SettingsCodec`; all update/import paths call `normalized()`. JSON export/import excludes secrets. `AndroidKeystoreSecretStore` encrypts provider-key ciphertext using AES-GCM with an Android Keystore key and stores only ciphertext under `noBackupFilesDir`; app backup is disabled.

### AI

`AiProviderFactory` resolves Gemini, OpenAI and Anthropic `AiProvider` implementations. The user invokes AI; the app combines selected text/current subtitle, configured previous blocks, film title/timestamps, prompt and model, pauses playback and surfaces loading/errors. Requests are HTTPS calls through OkHttp. Keys are read from the secret store and never logged or included in settings JSON. AI is not offline.

## Data stores

- `DataStoreAppSettingsRepository`: typed settings payload plus stored schema integer.
- Room `recent_media`: SAF/media URI, title/MIME, last position/duration/open timestamp, last subtitle URI and optional layer. Database schema v2 adds nullable `lastSubtitleLayer` to v1.
- Room `saved_words` and FTS4 table: normalized text/language uniqueness, translation/context/media/timestamp and known flag.
- Keystore/ciphertext files: provider credentials; no plaintext key persistence.

## Capability boundaries

Domain interfaces cover `PlayerController`, `SubtitleRepository`/`SubtitleFileLoader`, `TranslationProvider`, `AiProvider`, `DictionaryProvider`, `WordAnalyzer`, `WordLevelProvider`, `SpeechToText`, `UpdateChecker`, YouTube/PDF providers, automatic-level detection and subtitle AI tools. LATER defaults fail loudly via `NotImplementedError` and are disabled by constants in `FeatureFlags`.

## Security and privacy notes

- Request only user-mediated SAF read grants; do not request broad storage permission.
- Direct HTTP is enabled to support user-entered `http://` streams. This currently uses app-wide cleartext allowance, a security-review risk; HTTPS should be preferred. Restrict this more narrowly if Media3/network configuration permits without breaking arbitrary user URLs.
- No API key in app resources, logs, exported settings, Git, or backup.
- AI sends user-selected subtitle context to an external provider only after explicit invocation; explain this in UI and docs.
- Media3 error surfaces should avoid echoing full secret-bearing URLs; current error text uses a generic code name.
- Dictionary/model provenance restrictions are hard boundaries; see `THIRD_PARTY_NOTICES.md` and REQ-001/REQ-003.

## Build baseline

Gradle 8.9 wrapper, AGP 8.7.3, Kotlin 2.0.21, Java 17 bytecode, compile/target SDK 35, min SDK 26. The wrapper JAR was restored from the official Gradle 8.9 source tag. Local verification remains blocked by network policy; only a successful CI build can establish compiler/test/lint status.
