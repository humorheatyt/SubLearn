# Known issues and review risks

**CI state (2026-10-08):** the blocking gate (`assembleDebug testDebugUnitTest :core:domain:test :core:subtitles:test lint`) is green, including Robolectric Compose UI tests on API 31. Remaining risks below are honest gaps, not hidden failures.

## Open items

- **Device verification not performed (REQ-004):** no Poco X3 Pro or API 31 hardware pass has run in this environment. Playback, PiP, brightness/volume gestures, rotation lock, MIUI/HyperOS SAF providers, decoder quirks (HW/HW+/SW on the SD860/Adreno 640), ML Kit model download and AI HTTPS calls still need hands-on checks. Checklist rows say `IMPLEMENTED / DEVICE PENDING`.
- **Instrumented emulator smoke job is failing (non-blocking):** `connectedDebugAndroidTest` on the API 31 emulator job fails within the job's runtime; the sandbox cannot fetch the log host to see why. The job uploads `sublearn-androidTest-reports-*` artifacts — open the failed run on GitHub to read the reports. Possible causes: emulator boot flakiness on hosted runners, or genuine test issues on device (Room/Compose). The same screens are covered by passing Robolectric UI tests in the blocking gate.
- **REQ-003 — Google ML Kit Terms of Service:** the official translation SDK is a required dependency but is not permissively licensed. No model binaries are bundled. Owner must accept the terms or choose a different translation backend before broad distribution.
- **REQ-001 — dictionary data provenance:** the offline dictionary stays disabled; nothing derived from `dictionaryproject`'s upstream material is committed. Any future import must use the documented user-supplied schema.
- **REQ-006 — signing-in-CI convenience missing:** the GitHub App token cannot set Actions secrets, so releases use the staged signing flow (DECISIONS #27). The release keystore lives outside the repo in the workspace `sublearn-signing/` — **back it up**; losing it breaks in-place updates for installed APKs.

## Product limitations (by design or pending polish)

- A single SAF document grant cannot enumerate sidecars; the **Browse a folder** route is the reliable same-directory auto-sidecar path. Standalone video selection still works, sibling lookup is best effort per provider.
- Sidecar auto-discovery covers `.srt`, `.vtt`, `.ass`, `.ssa` and picks one best basename/language match; load another track from the subtitle dialog for the second layer.
- Embedded Media3 captions are surfaced as the current cue (with timestamped block), not a full indexed transcript; external parsed tracks give the complete list/search experience. Validate against `CueGroup` on device before claiming full embedded transcript support.
- `SOFTWARE` decoder mode works only if the device exposes a software `MediaCodec`; no FFmpeg extension is bundled (DECISIONS #7).
- Cleartext traffic is enabled globally so user-entered `http://` streams can play; prefer HTTPS.
- AI sends the selected subtitle/context to the chosen third party only on explicit request; providers may change model IDs/endpoints.
- `Full dictionary details` is disabled/Coming Soon (LATER); the lookup card offers translation + bookmarking now.
- Mixed-bidi token hit-testing and font scaling are unit/UI-verified only at the logic level; a Persian-content device pass is still recommended.

## Maintenance rules

When an item is resolved, record the commit/check/device used, update the matching `CHECKLIST.md` row, and only then remove it here. Never upgrade an unverified item to "done" on code inspection alone.
