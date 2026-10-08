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
- **CI result:** All push/PR workflow pairs have reached `Verify Android project` and failed; APK upload and dependent Android 12 smoke tests were skipped. Annotations exposed compile errors in Home, Media3, Player, and App. The latest App fixes add Room runtime directly, import `WindowInsets.safeDrawing`, remove the inaccessible `weight` import, and adapt the drawer item to the actual Material3 signature with a guarded click; they await CI re-verification. See REQ-005 for exact diagnostics and run IDs.
- **Suspected cause:** Sandbox outbound access is limited to GitHub/API/codeload, npm and PyPI; it excludes Gradle's distribution host, GitHub's release-asset host, Google Maven and Maven Central. Android SDK availability is also unconfirmed.
- **Next step:** Commit/push the App-module fixes and rerun CI; use the diagnostic annotation to fix every newly exposed compiler/test/lint failure. If CI diagnostics or execution remain unavailable, use a machine with JDK 17, Gradle 8.9, Android SDK 35, and Google/Maven repository access.
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
- **Run/job IDs:** Initial push/PR runs `37822587380`/`37822600898` ran commit `b4a8ed3ec5783214a71ba90051e8438d7b988576`; first diagnostic runs `37835059008`/`37835063379` ran `c5677279362965b2272df62089dc13def851e692`; marker-focused runs `37835733412` (job `113512147455`) / `37835739889` (job `113512170607`) ran `b8440e1c55a061cd8c7939289ece73e7c8597aa5`; fix-attempt runs `37836688168` (job `113515392700`) / `37836697332` (job `113515424027`) ran `87dc8d268a18be2b8dd65df44bd0f5e8d22351fe`; next runs `37837301728` (job `113517509085`) / `37837308854` (job `113517532697`) ran `271267e2edf5fee16a3872b65846c636b03363bc`; subsequent runs `37837964458` (job `113519783016`) / `37837971337` (job `113519806624`) ran `04bcda413ce5e33d668235f968c9797c91a01e47`; latest runs `37838859936` (job `113522828167`) / `37838867243` (job `113522851865`) ran `413e72539f84e4c585a284329f20fa065e51ae3b`. All fourteen runs reached `Verify Android project` and failed; APK upload and dependent Android 12 jobs were skipped.
- **Tried/result (ten retrieval paths):**
  1. `gh run view ... --log-failed` for the initial push/PR runs failed with EOF while downloading the log ZIP from `results-receiver.actions.githubusercontent.com`.
  2. GitHub Actions jobs REST API returned step conclusions only; initial check-run annotations contained only `Process completed with exit code 1`.
  3. `gh api repos/humorheatyt/SubLearn/actions/jobs/113467188179/logs` followed the signed-log redirect and failed with EOF.
  4. Fetching the GitHub job page returned “Sign in to view logs” and truncated step output; it exposed no diagnostic lines.
  5. The initial annotation experiment (commit `c567727`) returned a bounded failure annotation through `api.github.com`, but selected only bottom stack frames and the `BUILD FAILED` summary.
  6. The marker-focused annotation on commit `b8440e1` exposed `HomeScreen.kt:228` nullable-`Uri` and `Media3PlayerController.kt:354` `Int`/`Long` compiler errors; these fixes were included in `87dc8d2`.
  7. The annotation on `87dc8d2` exposed `presentationTimeUs` as unresolved in `Media3PlayerController.kt:351` and downstream inference errors. Media3 1.5.1 source shows `CueGroup` in `androidx.media3.common.text`; that import was corrected in `271267e`.
  8. The annotation on `271267e` exposed an unused invalid `TrackSelectionParameters` import at line 18 and an `init` block referencing `listener` before its later declaration; both were fixed in `04bcda4`.
  9. The annotation on `04bcda4` exposed Player-module issues: missing Media3 common on the player module classpath (`AdViewProvider`), missing layout imports, missing Material/Foundation opt-ins, an inaccessible `weight` import, and unresolved stale `totalY`; these fixes are in `413e725`.
  10. The annotation on `413e725` exposed app errors: inaccessible `weight`, missing `WindowInsets.safeDrawing`, `NavigationDrawerItem` lacking an `enabled` parameter (causing slot/type errors), and Room's `RoomDatabase` supertype missing from the app compile classpath. These are patched locally by removing the invalid import, adding the `safeDrawing` import and Room runtime dependency, and guarding/styling/marking disabled drawer items in semantics; awaiting CI.
- **Mitigation implemented:** `.github/workflows/android.yml` selects Gradle/Kotlin error markers, adjacent context, and the final build summary (bounded to 5,000 characters). YAML parsing, `bash -n`, and simulated compiler failures verified annotation generation and non-zero exit preservation. Real CI annotations have successively exposed and enabled fixes for the source issues listed above.
- **Suspected cause:** The full Actions log endpoint redirects to `results-receiver.actions.githubusercontent.com`, a host outside this sandbox's outbound allowlist; the authenticated CLI cannot download that host, and the public HTML fetch is not authenticated for logs. The marker annotation is the accessible diagnostic route.
- **Next step:** Commit/push the App-module fixes on `arena/4386d633-sublearn`; inspect the fresh check-run annotation and continue resolving any new build/test/lint failures. Keep PR #1 draft and unmerged until all checks pass.
- **Severity:** Blocking for build/test/lint acceptance and merge; the PR remains explicitly unverified.
## REQ-006 — GitHub App cannot manage Actions secrets; release signing needs owner action
- **Phase/task:** Phase 9 release; sign release APKs with a stable key.
- **Needed:** Either (a) grant the Arena GitHub App the **Secrets: read/write** repository permission and set `SUBLEARN_KEYSTORE_BASE64`, `SUBLEARN_KEYSTORE_PASSWORD`, `SUBLEARN_KEY_ALIAS`, `SUBLEARN_KEY_PASSWORD` from the workspace backup in `sublearn-signing/`, or (b) accept the two-phase staged-signing pipeline described in `DECISIONS.md` #27 for every release.
- **Tried/result:** `gh secret set` and the Actions variables API both return `403 Resource not accessible by integration` although the token has admin/push on the repository; artifact downloads redirect to a blocked Azure host; release-asset uploads go to blocked `uploads.github.com`. A persistent RSA-4096 key was generated and stored outside the repo; `apksigner` (Android build-tools 34, Apache-2.0) is available in the workspace and signs/verifies locally.
- **Suspected cause:** The Arena GitHub App token lacks the Actions Secrets scope; sandbox egress is restricted to GitHub API/git hosts, npm and PyPI.
- **Next step:** Owner: back up `sublearn-signing/` (keystore + passwords) and either add the four secrets or keep using the staging pipeline. Back up the keystore: losing it means users cannot update installed APKs in place.
- **Severity:** Medium. Releases are still possible and installable via the staging pipeline; only the signing-in-CI convenience is missing.
