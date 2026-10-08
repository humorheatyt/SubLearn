# Third-party notices and license audit

Audit date: 2026-10-08. SubLearn's original application code is licensed under Apache-2.0 (see `LICENSE`). No GPL code, reference-project source files, dictionary database, extracted translation model, external font, sample media, or third-party image is distributed in this tree.

## Runtime dependencies

| Component | Version/source | License/terms | Use and notice |
|---|---|---|---|
| AndroidX Core, Activity, Lifecycle, Navigation, Compose UI/Foundation/Material 3/Material Icons, DataStore, Room, SQLite, AndroidX Test | AndroidX; pinned in `gradle/libs.versions.toml` | Apache-2.0 | Android platform/UI/storage/test libraries. Material icons are from the Apache-2.0 Material Icons library; no icon files were copied into the repository. |
| AndroidX Media3 ExoPlayer/UI/Common | `androidx.media3` 1.5.1 | Apache-2.0 | Playback and embedded-track discovery. No FFmpeg or other codec extension is bundled. |
| Kotlin / Kotlin serialization / kotlinx.coroutines | Kotlin 2.0.21, serialization 1.7.3, coroutines 1.9.0 | Apache-2.0 | Language, JSON settings, asynchronous flows and Google Task integration. |
| Koin | 4.0.2 | Apache-2.0 | Dependency injection. |
| OkHttp | 4.12.0 | Apache-2.0 and MIT components; see upstream notices | HTTPS AI-provider transport. OkHttp's bundled license/notice obligations remain applicable. |
| Google ML Kit on-device Translation | `com.google.mlkit:translate:17.0.3` | **Google ML Kit Terms of Service; not a permissive OSS license** | Required official translation API. ML Kit downloads/manages its own language models. No model binary is committed or separately redistributed. This is an explicit license exception/open review item tracked as `REQ-003`; do not describe it as Apache-2.0. |
| Google Play Services Tasks | version resolved transitively for ML Kit | Apache-2.0 (Google Play Services open-source notices) | Coroutine `Task.await()` bridge. |

## Build/test-only dependencies

| Component | Version/source | License/terms | Use and notice |
|---|---|---|---|
| Android Gradle Plugin, Gradle wrapper, Kotlin Gradle plugins/KSP | AGP 8.7.3, Gradle 8.9 wrapper, Kotlin/KSP plugin versions in the version catalog | Apache-2.0 | Build tooling only. `gradle/wrapper/gradle-wrapper.jar` is the official wrapper from the Gradle `v8.9.0` source tag. It is not application code. |
| JUnit 4 | 4.13.2 | EPL-1.0 | JVM unit tests only; not packaged in the app. |
| AndroidX Test Runner/Rules/JUnit and Compose UI Test | pinned in the version catalog | Apache-2.0 | Instrumented smoke tests only; not packaged in the app. |
| Robolectric | 4.14.1 | Apache-2.0 | JVM-hosted Compose UI tests (API 31) in CI; not packaged in the app. |
| Android SDK Build-Tools `apksigner` | build-tools 34.0.0 (`lib/apksigner.jar`, SHA-256 `eefdd6ae…2123a`) | Apache-2.0 (Android SDK tools) | Release-signing tool used outside the repository to sign CI-built APKs; not packaged in the app. Retrieved from the npm package `@postar/apktool-node` (which redistributes the official jar) because the Android SDK host is unreachable from the build sandbox. |

Dependency versions are pinned in `gradle/libs.versions.toml`; the Android Gradle dependency graph still needs to be reviewed from a successful CI build/SBOM before release. Transitive notices from Google Play Services, AndroidX, and OkHttp remain applicable. No binary third-party dependency source is copied into this repository.

## Assets and reference projects

- **Fonts:** no font binaries are included; system sans/serif/monospace families are selected through Android/Compose.
- **Icons:** Compose Material Icons Extended at runtime; Apache-2.0. `app/src/main/res/drawable/ic_sublearn.xml` is an original vector mark authored for this project.
- **Word lists/dictionaries/media:** none. Word popups use the user's own known-word state/manual level; there is no bundled frequency list or sample subtitle/video.
- **DualSub Replay:** reviewed as an MIT-licensed reference; no source code or asset reused, so no upstream source notice is needed. Historical bundled-component caveats are documented in `docs/REFERENCES.md`.
- **yall-mp and jidoujisho:** GPL-3.0; ideas only. No code or assets copied.
- **SubX Player:** closed-source commercial UX inspiration only; no code/assets.
- **ProudVocab:** the owner explicitly allowed taking the visual idea; no license was found and no code/assets copied.
- **dictionaryproject:** no license found and README provenance is restricted/unclear. No schema, query, database, model, example, or derived content copied; rights question REQ-001 remains open.

## Release review

Before publishing `v0.1.0`, generate and inspect an SBOM/dependency-license report from CI, check the license files/notices of all resolved transitive Android/OkHttp components, and resolve REQ-003. If a new asset or dependency is added, update this table and preserve the applicable copyright/license text in `THIRD_PARTY_NOTICES.md` before shipping it.
