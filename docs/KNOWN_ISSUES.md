# Known issues and review risks

This is a pre-release implementation inventory. Statuses below do not imply compilation or runtime verification.

## Blocking verification / release

- **Build unverified:** local `./gradlew assembleDebug testDebugUnitTest lint` cannot download Gradle 8.9 because the sandbox blocks `services.gradle.org`; see `REQ-002`. Kotlin syntax was parsed by tree-sitter only; that does not establish symbol/API correctness.
- **CI unobserved:** Android CI and API 31 emulator workflows are configured but have not run. No green workflow, APK artifact, merge, or tag exists.
- **Poco X3 Pro untested:** decoder capabilities, MIUI/HyperOS storage providers, PiP, orientation lock, brightness restoration and hardware codec behavior require an actual device pass.
- **Google ML Kit terms:** the official translation SDK is under separate Google terms, not a permissive OSS license. Resolve REQ-003 before publishing.
- **Dictionary data rights:** REQ-001 remains open. No dictionary DB, sample data, extracted model or derived content is included.

## Product limitations requiring verification or follow-up

- A single SAF document grant cannot enumerate sidecars. The Home folder picker is the supported same-directory auto-sidecar route; standalone video selection still works, but sibling lookup is best effort.
- Sidecar auto-discovery supports `.srt`, `.vtt`, `.ass`, `.ssa`; it selects one best basename/language match. To display both language tracks, load/select another track in the subtitle dialog. Providers can restrict folder enumeration.
- Persisted layer metadata is nullable for old/newly selected media and stored with Room schema v2; migration has not run on a device.
- Embedded Media3 captions are surfaced as the current cue, not as a complete indexed transcript history. Full transcript/navigation is most complete for parsed external tracks; validate what Media3 `CueGroup` exposes before claiming full embedded-track transcript support.
- `SOFTWARE` decoder prioritization works only if the device exposes a software `MediaCodec`; SubLearn does not bundle FFmpeg. Hardware and fallback mappings need device validation.
- Cleartext traffic is globally enabled so user-entered HTTP streams can play. Prefer HTTPS; review whether the network policy can be narrowed without breaking arbitrary user media URLs.
- AI requires a user API key, network access, and sends explicitly selected subtitle context to the chosen third party. Model names/provider endpoints can change; provider errors are surfaced, but terms/retention should be reviewed by the user.
- ML Kit model status/download UI and API support for EN↔FA must be confirmed against the resolved dependency on a clean device.
- `Full dictionary details` is intentionally disabled/Coming Soon; no Google Translate browser fallback is currently exposed in that card.
- App UI has English/Persian resources and locale selection; additional languages are not enabled. Validate mixed bidi token hit-testing and font scaling on real RTL devices.
- UI test sources, Room migration, player controllers, and Compose/Material3 APIs have not passed a Kotlin compiler; CI findings may reveal necessary fixes.

## Maintenance

When an issue is fixed, record the commit/check or device used, update the matching checklist row, and remove it only after verification. Never convert an unverified item to “done” based on code inspection alone.
