# Agent requests, rights questions and blocked work

Use this log instead of leaving an unfinished feature silent. Each entry records what is needed, attempts/results, suspected cause, severity, and a concrete next step. **Nothing here is a claim of successful build or release.**

## REQ-001 — Dictionary-project data/schema rights provenance
- **Phase/task:** Phase 0 license audit; offline dictionary is LATER.
- **Needed:** Evidence that the owner has redistribution rights to the source schema/query structure and any dictionary content/model material described by `melonityhub/dictionaryproject`, or a clean-room, independently designed import contract that does not depend on that provenance.
- **Tried/result:** Reviewed the project's README and repository license metadata before implementation. The README says the schema/queries came from third-party Android dictionary Java source and its `.bipe` model loader was reverse-engineered; no repository license was found. No DB, model, query, mapping, sample record, or derived content is present in this repository.
- **Suspected cause:** The source project does not document rights to the upstream app-derived material.
- **Next step:** Keep `DictionaryProvider` disabled and accept no bundled data. Before any future dictionary release, use a separately designed schema/importer for a file the user supplies, and obtain rights evidence for any imported format or content.
- **Severity:** High for any dictionary feature; not a blocker to unrelated NOW features while the dictionary stays absent.

## REQ-002 — Local Gradle/Android build blocked by the sandbox toolchain/network
- **Phase/task:** Phase 9 verification; exact required command is `./gradlew assembleDebug testDebugUnitTest lint`.
- **Needed:** Gradle 8.9 distribution, JDK 17+, Android SDK/platform 35 and dependency access to Google's/Maven repositories.
- **Tried/result (three distinct recovery paths):**
  1. **Environment/wrapper repair:** Confirmed `java` and system `gradle` are absent and the checked-out wrapper JAR was missing. Retrieved the official 43,504-byte Gradle 8.9 wrapper JAR from the Gradle `v8.9.0` GitHub repository via the allowed GitHub API. `./gradlew --version` then stopped with `java: not found`.
  2. **Temporary Java runtime from an allowed package host:** Installed `jdk4py==21.0.8.2` under `/tmp` (not in Git). With that Java 21 runtime, the wrapper attempted `https://services.gradle.org/distributions/gradle-8.9-bin.zip` and failed with `SSLHandshakeException` / `EOFException` before receiving the distribution.
  3. **Official distribution from GitHub releases:** Queried the Gradle 8.9 release asset through `api.github.com`; the download endpoint responded `302 Found` to `release-assets.githubusercontent.com`, which is outside the sandbox allowlist. No distribution was written to the repository. The required Gradle command was re-run both before and after the locally patched compiler errors using the temporary Java 21 runtime; each attempt failed at the same `services.gradle.org` TLS handshake before any Gradle task.
- **Additional local checks:** The latest Tree-sitter Kotlin pass parsed 34 Kotlin source/test files with no syntax-error or missing nodes; Python XML parsing passed for all 15 XML files; EN/FA string-key parity passed for app (34), core/platform (5), home (23), player (81), learning (14), and settings (141) keys; the local `R.string` reference scan found no missing keys. These are **not** substitutes for Kotlin compilation, lint, unit tests, instrumentation, or device verification.
- **CI result:** The push/PR pairs have reached `Verify Android project` and failed; APK upload and dependent Android 12 smoke tests were skipped. Check annotations successively exposed three Kotlin issues: nullable `Uri?` access in `HomeScreen.kt:228`, an `Int`/`Long` cue-time mismatch at `Media3PlayerController.kt:354`, and an incorrect `CueGroup` import that left `presentationTimeUs` unresolved. All three are patched locally (`uri?.let`; explicit `Long` time values; import `androidx.media3.common.text.CueGroup`) and await another CI run. See REQ-005 for IDs/log transport details.
- **Suspected cause:** Sandbox outbound access is limited to GitHub/API/codeload, npm and PyPI; it excludes Gradle's distribution host, GitHub's release-asset host, Google Maven and Maven Central. Android SDK availability is also unconfirmed.
- **Next step:** Rerun the CI diagnostic annotation after the two local compiler fixes, then fix every newly exposed compiler/test/lint failure. If CI diagnostics or execution remain unavailable, use a machine with JDK 17, Gradle 8.9, Android SDK 35, and Google/Maven repository access.
- **Severity:** Blocking for acceptance, release, merge, and any claim that tests/lint/build passed.

## REQ-003 — Google ML Kit Translation license terms
- **Phase/task:** Phase 4 translation; required official Google on-device API.
- **Needed:** Owner review/acceptance that the Google ML Kit Android dependency and downloaded translation models may be used in an Apache-2.0 SubLearn distribution.
- **Tried/result:** Chose the official `com.google.mlkit:translate:17.0.3` API as required by the product constraint. Public package metadata identifies ML Kit Terms of Service (not an OSI-approved permissive source license). The application does not extract, commit, or redistribute ML Kit's model files. See Google's [ML Kit terms](https://developers.google.com/ml-kit/terms) and the dependency entry in `THIRD_PARTY_NOTICES.md`.
- **Suspected cause:** The API is a Google-provided proprietary SDK/service governed by separate terms, despite source samples being Apache-2.0.
- **Next step:** Review/accept the terms before release or explicitly authorize a different translation implementation. Do not represent ML Kit as Apache/MIT.
- **Severity:** High release/legal review item because translation is a NOW capability and the dependency is distributed with the app.

## REQ-004 — Device/Android 12 verification
- **Phase/task:** Phase 2/9, Poco X3 Pro and API 31.
- **Needed:** Instrumented API 31 smoke-test result and hands-on verification on a Poco X3 Pro (or equivalent Android 12 device), including codec/stream/SAF/PiP behavior.
- **Tried/result:** Added an API 31 emulator workflow and instrumented Compose smoke tests using a test-only `FakePlayer`. No Android emulator/device is available in the local sandbox; no test has executed yet.
- **Suspected cause:** Local environment has no Android SDK/emulator and cannot fetch the Gradle distribution.
- **Next step:** The emulator job was skipped because the Gradle verify job failed. After Gradle verification succeeds, inspect the API 31 emulator workflow and then perform a device pass for OEM decoder quirks, PiP, orientation, volume/brightness, and real SAF permissions before release.
- **Severity:** High for the requested device compatibility claim; release remains unverified.

## REQ-005 — GitHub Actions Gradle failure logs inaccessible from sandbox
- **Phase/task:** Phase 9 verification; diagnose failed `Verify Android project` step on PR #1.
- **Needed:** Gradle output from the CI command to identify and repair the build/test/lint failure.
- **Run/job IDs:** Initial push/PR runs `37822587380`/`37822600898` ran commit `b4a8ed3ec5783214a71ba90051e8438d7b988576`; first diagnostic runs `37835059008`/`37835063379` ran `c5677279362965b2272df62089dc13def851e692`; marker-focused runs `37835733412` (job `113512147455`) / `37835739889` (job `113512170607`) ran `b8440e1c55a061cd8c7939289ece73e7c8597aa5`; fix-attempt runs `37836688168` (job `113515392700`) / `37836697332` (job `113515424027`) ran `87dc8d268a18be2b8dd65df44bd0f5e8d22351fe`. All eight runs reached `Verify Android project` and failed; APK upload and dependent Android 12 jobs were skipped.
- **Tried/result (seven retrieval paths):**
  1. `gh run view ... --log-failed` for the initial push/PR runs failed with EOF while downloading the log ZIP from `results-receiver.actions.githubusercontent.com`.
  2. GitHub Actions jobs REST API returned step conclusions only; initial check-run annotations contained only `Process completed with exit code 1`.
  3. `gh api repos/humorheatyt/SubLearn/actions/jobs/113467188179/logs` followed the signed-log redirect and failed with EOF.
  4. Fetching the GitHub job page returned “Sign in to view logs” and truncated step output; it exposed no diagnostic lines.
  5. The initial annotation experiment (commit `c567727`) returned a bounded failure annotation through `api.github.com`, but selected only bottom stack frames and the `BUILD FAILED` summary.
  6. The marker-focused annotation on commit `b8440e1` exposed `HomeScreen.kt:228` nullable-`Uri` and `Media3PlayerController.kt:354` `Int`/`Long` compiler errors; these fixes were included in `87dc8d2`.
  7. The annotation on `87dc8d2` then exposed `presentationTimeUs` as unresolved in `Media3PlayerController.kt:351` and downstream type-inference errors. Media3 1.5.1 source shows `CueGroup` in `androidx.media3.common.text`; the local import is corrected to that package, awaiting CI confirmation.
- **Mitigation implemented:** `.github/workflows/android.yml` selects Gradle/Kotlin error markers, adjacent context, and the final build summary (bounded to 5,000 characters). YAML parsing, `bash -n`, and simulated compiler failures verified annotation generation and non-zero exit preservation. Real CI annotations identified the three successive source issues described above.
- **Suspected cause:** The full Actions log endpoint redirects to `results-receiver.actions.githubusercontent.com`, a host outside this sandbox's outbound allowlist; the authenticated CLI cannot download that host, and the public HTML fetch is not authenticated for logs. The marker annotation is the accessible diagnostic route.
- **Next step:** Commit/push the corrected `CueGroup` import on `arena/4386d633-sublearn`; inspect the fresh check-run annotation and continue resolving any new build/test/lint failures. Keep PR #1 draft and unmerged until all checks pass.
- **Severity:** Blocking for compiler/test/lint diagnosis and merge; the PR remains explicitly unverified.
