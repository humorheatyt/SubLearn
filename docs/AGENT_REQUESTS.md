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
  3. **Official distribution from GitHub releases:** Queried the Gradle 8.9 release asset through `api.github.com`; the download endpoint responded `302 Found` to `release-assets.githubusercontent.com`, which is outside the sandbox allowlist. No distribution was written to the repository. Re-ran the required Gradle command with the temporary JDK; it failed at the same distribution download.
- **Additional local checks:** The latest Tree-sitter Kotlin pass parsed 34 Kotlin source/test files with no syntax-error or missing nodes; Python XML parsing passed for all 15 XML files; EN/FA string-key parity passed for app (34), core/platform (5), home (23), player (81), learning (14), and settings (141) keys; the local `R.string` reference scan found no missing keys. These are **not** substitutes for Kotlin compilation, lint, unit tests, instrumentation, or device verification.
- **CI result:** Both push and pull-request runs reached the `Verify Android project` step and failed; APK upload and dependent Android 12 smoke tests were skipped. The error details could not be retrieved; see REQ-005.
- **Suspected cause:** Sandbox outbound access is limited to GitHub/API/codeload, npm and PyPI; it excludes Gradle's distribution host, GitHub's release-asset host, Google Maven and Maven Central. Android SDK availability is also unconfirmed.
- **Next step:** Use the CI diagnostic annotation added to `.github/workflows/android.yml` on the next run to expose the bounded Gradle failure tail, then fix every compiler/test/lint failure. If the workflow cannot provide diagnostics, use a machine with JDK 17, Gradle 8.9, Android SDK 35, and Google/Maven repository access.
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
- **Run/job IDs:** Initial push run `37822587380`, job `113467143062`, and PR run `37822600898`, job `113467188179`, ran commit `b4a8ed3ec5783214a71ba90051e8438d7b988576`. Diagnostic-workflow push run `37835059008` and PR run `37835063379`, job/check-run `113509864538`, ran commit `c5677279362965b2272df62089dc13def851e692`. All four runs reached `Verify Android project` and failed; APK upload and dependent Android 12 jobs were skipped.
- **Tried/result (five retrieval paths):**
  1. `gh run view ... --log-failed` for the initial push/PR runs failed with EOF while downloading the log ZIP from `results-receiver.actions.githubusercontent.com`.
  2. GitHub Actions jobs REST API returned step conclusions only; initial check-run annotations contained only `Process completed with exit code 1`.
  3. `gh api repos/humorheatyt/SubLearn/actions/jobs/113467188179/logs` followed the signed-log redirect and failed with EOF.
  4. Fetching the GitHub job page returned “Sign in to view logs” and truncated step output; it exposed no diagnostic lines.
  5. The first CI annotation experiment (commit `c567727`) successfully returned a bounded failure annotation through `api.github.com`, but selected only the bottom stack frames and `BUILD FAILED` summary, not the underlying compiler/task cause.
- **Mitigation implemented:** `.github/workflows/android.yml` was improved to select Gradle/Kotlin error markers, adjacent context, and the final build summary (bounded to 5,000 characters), rather than only the last lines. YAML parsing, `bash -n`, and a simulated compiler failure verified that its diagnostic appears in a check annotation and the step preserves a non-zero exit code. The improved filter is not yet committed or exercised by GitHub Actions.
- **Suspected cause:** The Actions log endpoint redirects to `results-receiver.actions.githubusercontent.com`, a host outside this sandbox's outbound allowlist; the authenticated CLI cannot download that host, and the public HTML fetch is not authenticated for logs. The Gradle build failure itself remains unknown.
- **Next step:** Commit/push the improved marker-based annotation on `arena/4386d633-sublearn`; retrieve the next check-run annotation through `api.github.com`, diagnose/fix the actual Gradle error, and rerun the workflow. Keep PR #1 draft and unmerged until all checks pass.
- **Severity:** Blocking for compiler/test/lint diagnosis and merge; the PR remains explicitly unverified.
