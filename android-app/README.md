# AI Fitness Coach for Android

The production app is in `android-app/`. Use JDK 17, Android SDK 36, and the checked-in Gradle wrapper. The React preview is independent.

## Navigation

- **Home** shows training history shortcuts, illustrated recent exercises, and the app's daily quote.
- **Exercises** opens an illustrated, searchable catalog. Select a movement to configure sets, reps or hold time, and applicable equipment. Start Workout stays at the bottom. Technique, media, and camera guidance are separate actions.
- **Settings** opens pages for profile, appearance and language, workout and camera preferences, notifications, Health Connect, AI, media storage, and privacy. All controls still use the existing settings store. Only media's Wi-Fi preference and the incomplete onboarding draft add keys.
- **First run** has Welcome, Language, Identity, Optional Metrics, Goal and Experience, Privacy, and Review. Back retains inputs. DataStore retains the current step and raw draft until completion, when the profile and completion flag are saved together. The privacy page points to Health Connect settings for later opt-in. No new equipment profile fields were introduced.

## Exercise visuals and media

Offline artwork consists of 39 original Compose drawings in `core/media/ExerciseIllustration.kt`, authored for this project as part of the app source. The drawings illustrate representative positions and equipment. The source code maps them by stable exercise ID. They contain no external imagery or font assets.

Optional online WebP images are **Exercise data by RepDB**. The Free Tier License v1.0 permits in-app use with attribution. The catalog pins the [published image revision and license](https://github.com/RepDB/exercise-dataset/blob/f12da458a90f353a86c953f204304d5a8bd98275/LICENSE-DATA.md). Each supported row lists the provider, 512 × 512 size, summed file bytes, and applicable license. Some exercises have image pairs, which the existing player alternates. Six band movements use the offline illustration and local import. No provider-qualified free GIF, MP4, or alternate quality is advertised for these entries. Personal MP4, GIF, WebP, PNG, and JPEG files can be imported with the Android document picker.

WorkManager performs unique per-exercise downloads and applies the saved Wi-Fi-only/unmetered constraint. The existing Room asset record reports progress and stores the verified local paths. Transfers check bounds, expected size and format before publishing. Failed or cancelled transfers leave no playable record. Successfully downloaded or imported files are reusable offline. The document picker scopes access to the file the user selects. The app validates imports and caps individual files at 50 MiB.

## Workout camera

The previous code called `ProcessCameraProvider.get()` outside its exception guard, silently discarded the exceptions from binding, and blocked the main thread in disposal while waiting for provider initialization. A provider or bind failure could therefore produce a black preview, an exception, or a hang. Other risks included pending listeners after back navigation, a stale front/back parameter in the analyzer, unguarded frame errors, and MediaPipe GPU initialization on the UI thread. These are source-confirmed failure paths. The original device incident has no reproduced stack trace.

The screen reports permission separately from camera streaming and pose initialization. A worker owns model initialization, frame analysis, and close. CameraX uses lifecycle-scoped use cases, and late callbacks are ignored after disposal. The CPU delegate avoids GPU/EGL initialization on startup. Unsupported preview or model paths show a retry action, and manual-only movements can run without camera permission. A native crash such as an ABI-specific signal still requires physical-device diagnosis.

For diagnostics, use `adb logcat -v threadtime -s WorkoutCamera AndroidRuntime CameraX`. The app records permission, startup, lens, streaming, pose and teardown events without recording camera frames, landmarks, profile values, or health data.

## Home-screen widgets

- **Quick Workout & Activity** (recommended 3 × 2) shows today's training and opens the exercise picker.
- **Workout & Daily Motivation** (recommended 4 × 3 or larger) also shows the same quote as Home. Quotes retain the existing English text and attribution in the Persian app.
- Tapping activity opens History, and tapping the quote opens Home. Counts include saved sessions with at least one recorded set, based on the local calendar day of the session end. Duration is the saved session duration rounded up for display.
- Widgets read Room and DataStore directly using WorkManager. They refresh on saves, relevant settings or clock changes, launcher lifecycle events, and an approximate next-midnight job. WorkManager can defer the midnight update while the device is asleep. The widget never polls frequently.
- The widgets use native RemoteViews. There are no production dependency additions or Room migrations.

## Verification

From `android-app/` with JDK 17 and SDK 36:

```bash
bash ./gradlew :app:testDebugUnitTest --stacktrace
bash ./gradlew :app:assembleDebug --stacktrace
bash ./gradlew :app:lintDebug --stacktrace
bash ./gradlew :app:connectedDebugAndroidTest --stacktrace
```

The AndroidX Test runner 1.6.2 is pinned for instrumentation only. It supports the project's minSdk 26 and JDK 17 toolchain, is maintained by AndroidX, and is Apache-2.0 licensed. It contributes to the test APK only.

### Physical device checklist

1. Update an existing installation without clearing app data. Confirm the profile, history, settings, media and Health Connect integration survive.
2. On a fresh installation, switch English/Persian mid-onboarding, enter optional data, go back, rotate, and reopen the app. Check that the saved draft resumes and completing the wizard opens Home. Repeat at the device's largest font size.
3. With the camera permission unset, open a supported exercise. Deny once, deny permanently, and enable the permission in Android settings. Test one-time permission and revocation after backgrounding. Open a manual-only workout on the same device without granting camera access.
4. Start bodyweight squat, dumbbell curl, band curl, and forearm plank with both available lenses. Confirm preview, pose startup, framing, tracking, and overlay alignment. Switch lenses, rotate, leave for help while tracking, reopen the session, and repeat rapidly. Save at least one workout.
5. Test a cold start with the camera occupied by another app or disabled by Android's camera privacy switch. Check the message, retry and exit. Verify in logcat that a closed session has no subsequent bind or inference callbacks.
6. In airplane mode, browse all thumbnails and saved demonstrations. With network enabled, test a queued unmetered-only download, cancellation, retry, a blocked download, and valid/invalid personal imports.
7. Pin both widgets on a phone launcher. Check empty states, all tap destinations, a saved workout, latest quote equality with Home, light/dark themes, Persian layout, resizing, and an overnight or post-reboot update. Launchers and power management can affect widget dimensions and refresh timing.
