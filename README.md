# AI Fitness Coach

An offline-first Android workout tracker and camera-based form coach with English and Persian interfaces. This repository also includes a React web preview for exploring the interface and exercise logic.

## Install and start

1. Open the latest successful [Android APK build](https://github.com/hamidtech/ai-fitness-coach/actions/workflows/android-apk.yml) in GitHub Actions.
2. Download the `ai-fitness-coach-debug-apk` artifact, extract the ZIP, and install the APK on Android 8.0 or later. GitHub may require sign-in to download the artifact.
3. Finish the first-run profile setup. You can skip optional body metrics and connect Health Connect later.

## What you can do

| Feature | Where to find it |
| --- | --- |
| Start and track a workout | **Start** is in the center of the bottom bar. Log sets, reps, weights or hold times. Supported exercises offer on-device camera tracking and a positioning guide. |
| Find exercises | **Exercises → Source** shows the available catalogs and exercise counts. Search by name and combine equipment and muscle group filters. |
| Download or import exercise media | Open an exercise and tap its media action. Download available licensed demos or import a personal photo, GIF, or MP4. Download options depend on the exercise and its source. |
| Update the exercise catalog | In **Exercises**, use **Check for catalog updates**. The same action is under **Settings → Exercise catalog**. The app keeps the last valid catalog for offline use. |
| Make a routine | Open **Plan** from Home after enabling it in **Settings → App features**. Build a weekly schedule or run your saved routines in rotation. |
| Inspect workout history and progress | Open **History** in Simple mode. Enable progress charts to see activity, streaks, muscle groups, and estimated strength records under **Progress**. |
| Analyze workouts with AI | Open **AI Analysis** from the bottom bar. Use rule-based feedback or configure a supported on-device model or external provider in Settings. External analysis requires consent to share the workout summary. |
| Customize the interface | **Home → Settings → App features** switches between Simple and Advanced layouts and controls which optional sections appear. Settings also includes language, appearance, Health Connect, and licenses. |
| Pin Android widgets | Add **Quick Workout & Daily Activity** or **Workout & Daily Motivation** from your launcher’s widget picker. They read saved workouts and the app’s daily quote. |

The built-in library, offline catalog, manual tracking, and workout history work without an account. Catalog updates and optional media downloads use the network. Camera processing for supported movements runs on the phone. Review source attribution and media terms under **Settings → Licenses and credits**.

## Project layout

| Path | Contents |
| --- | --- |
| [android-app/](android-app/) | Kotlin and Jetpack Compose app, CameraX and MediaPipe tracking, Room and DataStore, tests, and native widgets |
| [catalog-content/](catalog-content/) | Versioned public exercise feed, packaging tool, and offline-safe update contract |
| [src/](src/) | React and Vite preview with the browser workspace and analyzer lab |
| [docs/](docs/) | Feature and design notes, including the OpenGym comparison |

## Build and test

Use JDK 17, Android SDK Platform 36, and the checked-in Gradle wrapper.

```bash
cd android-app
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# With an emulator or Android device connected:
bash ./gradlew :app:connectedDebugAndroidTest
```

The APK is written to `android-app/app/build/outputs/apk/debug/app-debug.apk`. See the [Android guide](android-app/README.md) for camera troubleshooting and the device test checklist.

Run the browser preview from the repository root with a Node.js version supported by the pinned Vite version.

```bash
npm install
npm run dev
```

## Publish exercise updates

Increment the catalog version, validate the package, and publish it under `catalog-content/` on `main`. Android reads the pack from that branch. Catalog-only changes go through the [Exercise Catalog workflow](.github/workflows/exercise-catalog.yml) without rebuilding the APK. The [catalog publishing guide](catalog-content/README.md) describes stable IDs, thumbnails, and license and checksum requirements for media.
