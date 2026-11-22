# Android dependency compatibility audit — 2026-11-22

Scope: `android-app` on `main`. This is a source/configuration audit; **successful resolution is not proof of functional device runtime**.

## Build toolchain (verified against upstream references and CI progress)

| Component | Pinned | Assessment |
|---|---|---|
| JDK | 17 | Compatible with AGP 8.10 |
| Android Gradle Plugin | 8.10.1 | Requires Gradle 8.11.1; supports API 36 |
| Gradle wrapper | 8.11.1 | Matches AGP 8.10 |
| compileSdk | 36 | Required by Health Connect 1.1.0 |
| targetSdk | 35 | Independent of compileSdk |
| minSdk | 26 | Higher than Room 2.8.x minimum 23; assess feature runtimes individually |
| Kotlin | 2.4.21 | Compiled dependency metadata after upgrade |
| KSP | 2.3.10 | Ran successfully during the 2026-11-22 CI attempt |
| Room runtime, KTX, compiler | 2.8.5 | Single pinned version; KSP task passed |
| Health Connect client | 1.1.0 | Requires compileSdk 36 and AGP >= 8.9.1 |
| LiteRT-LM Android | 0.19.1 | Requires newer Kotlin metadata; on-device compatibility not yet established |

## Application dependency inventory

Version pins in `gradle/libs.versions.toml`:

- AndroidX core-ktx 1.13.1; appcompat 1.7.0
- Lifecycle 2.8.6; activity-compose 1.9.2
- Compose BOM 2024.09.03; Navigation Compose 2.8.1
- CameraX core/camera2/lifecycle/view 1.3.4
- MediaPipe Tasks Vision 0.10.14
- Room 2.8.5; DataStore 1.1.1
- WorkManager 2.9.1; Kotlin coroutines 1.9.0
- Health Connect 1.1.0
- Media3 exoplayer/ui 1.4.1
- Coil compose/gif 2.7.0
- OkHttp and MockWebServer 4.12.0; Gson 2.11.0
- LiteRT-LM 0.19.1; JUnit 4.13.2; coroutines-test 1.9.0

**Consistency checks:** CameraX artifacts share one version; Room runtime/KTX/compiler share one version; Coil artifacts share one version; Media3 share one version; test and runtime OkHttp share one version; Kotlin coroutines runtime/test share one version. Compose artifacts are coordinated by the BOM. No dynamic `+` versions were found in declared version pins.

## Evidence from uploaded GitHub Actions log

- Android SDK setup and `:app:checkDebugAarMetadata` succeeded.
- `:app:kspDebugKotlin` succeeded with Room 2.8.5.
- `:app:compileDebugKotlin` reached two source errors: CameraX Preview setter and a LazyRow Composable scope. Both were fixed on `main`.
- Unit tests, `assembleDebug`, APK packaging, device execution, Health Connect permission flow and LiteRT model inference were not yet verified as successful in that run.

## CI checks added

The workflow now produces two artifact reports on each run:

- `dependencies-compile.txt` for `debugCompileClasspath`
- `dependencies-runtime.txt` for `debugRuntimeClasspath`

It fails early when the report contains unresolved dependencies. Successful compilation and tests remain required for an APK artifact.

## Policy for future changes

1. Prefer preserving existing pinned versions when compatibility requirements are met.
2. Upgrade related build tools as a tested set (JDK / AGP / Gradle / Kotlin / KSP).
3. Do not upgrade a dependency merely because a newer version exists.
4. Confirm both directly declared and transitive dependency versions via CI reports.
5. Require source compile, unit tests and APK packaging to succeed before declaring build-ready.
6. Validate camera, inference, multilingual UI, Health Connect and LLM behavior on a physical device.

## Source references

- Android Gradle Plugin 8.10: https://developer.android.com/build/releases/agp-8-10-0-release-notes
- Health Connect: https://developer.android.com/health-and-fitness/health-connect/get-started
- Room: https://developer.android.com/jetpack/androidx/releases/room
- Gradle dependency locking: https://docs.gradle.org/current/userguide/dependency_locking.html
- Gradle dependency verification: https://docs.gradle.org/current/userguide/dependency_verification.html

**Unresolved risk:** This report cannot certify that every third-party library works at runtime. CI dependency reports and successful APK build are the next gates. Dependency locking and checksum verification should be introduced only after reviewing the resolved graph, to avoid generating or committing unverified lock/checksum files.
