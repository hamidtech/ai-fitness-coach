# AI Fitness Coach for Android

The production app is in `android-app/`. Use JDK 17, Android SDK 36, and the checked-in Gradle wrapper. The React preview is independent.

## Navigation

- **Home** shows training history shortcuts, illustrated recent exercises, and the app's daily quote.
- **Exercises** opens an illustrated, searchable catalog. Select a movement to configure sets, reps or hold time, and applicable equipment. Start Workout stays at the bottom. Technique, media, and camera guidance are separate actions.
- **Settings** opens pages for profile, appearance and language, workout and camera preferences, notifications, Health Connect, AI, media storage, and privacy. All controls still use the existing settings store. Only media's Wi-Fi preference and the incomplete onboarding draft add keys.
- **First run** has Welcome, Language, Identity, Optional Metrics, Goal and Experience, Privacy, and Review. Back retains inputs. DataStore retains the current step and raw draft until completion, when the profile and completion flag are saved together. The privacy page points to Health Connect settings for later opt-in. No new equipment profile fields were introduced.

## Exercise visuals and media

Offline artwork consists of 39 original Compose drawings in `core/media/ExerciseIllustration.kt`, authored for this project as part of the app source. The drawings illustrate representative positions and equipment. The source code maps them by stable exercise ID. They contain no external imagery or font assets.

Optional online WebP images are [Exercise data by RepDB](https://repdb.co). The Free Tier License v1.0 permits in-app use with attribution. The catalog pins the [published image revision and license](https://github.com/RepDB/exercise-dataset/blob/f12da458a90f353a86c953f204304d5a8bd98275/LICENSE-DATA.md). Each supported row lists the provider, 512 × 512 size, summed file bytes, and applicable license. Some exercises have image pairs, which the existing player alternates. Six band movements use the offline illustration and local import. No provider-qualified free GIF, MP4, or alternate quality is advertised for these entries. Personal MP4, GIF, WebP, PNG, and JPEG files can be imported with the Android document picker.

WorkManager performs unique per-exercise downloads and applies the saved Wi-Fi-only/unmetered constraint. The existing Room asset record reports progress and stores the verified local paths. Transfers check bounds, expected size and format before publishing. Failed or cancelled transfers leave no playable record. Successfully downloaded or imported files are reusable offline. The document picker scopes access to the file the user selects. The app validates imports and caps individual files at 50 MiB.

## Workout camera

The previous code called `ProcessCameraProvider.get()` outside its exception guard, silently discarded the exceptions from binding, and blocked the main thread in disposal while waiting for provider initialization. A provider or bind failure could therefore produce a black preview, an exception, or a hang. Other risks included pending listeners after back navigation, a stale front/back parameter in the analyzer, unguarded frame errors, and MediaPipe GPU initialization on the UI thread. These are source-confirmed failure paths. The original device incident has no reproduced stack trace.

The screen reports permission separately from camera streaming and pose initialization. A worker owns model initialization, frame analysis, and close. CameraX uses lifecycle-scoped use cases, and late callbacks are ignored after disposal. The CPU delegate avoids GPU/EGL initialization on startup. Unsupported preview or model paths show a retry action, and all movements can continue without camera permission. A native crash such as an ABI-specific signal still requires physical-device diagnosis.

Android Lint reports that the bundled arm64 MediaPipe Tasks Vision 0.10.14 JNI library lacks 16 KB alignment. Check the affected phone with `adb shell getconf PAGE_SIZE`. A 16 KB device can expose native-library load problems. The camera screen reports a pose error and logs the load failure when Android throws an exception. See [Android’s 16 KB compatibility guidance](https://developer.android.com/guide/practices/page-sizes).

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

1. Update an existing installation using a build signed with the same key, keeping app data in place. Confirm the profile, history, settings, media and Health Connect integration survive.
2. On a fresh installation, switch English/Persian mid-onboarding, enter optional data, go back, rotate, and reopen the app. Check that the saved draft resumes and completing the wizard opens Home. Repeat at the device's largest font size.
3. With the camera permission unset, open a supported exercise. Deny once, deny permanently, and enable the permission in Android settings. Test one-time permission and revocation after backgrounding. Open a generic tracked exercise, deny permission, and use Continue without camera.
4. Start bodyweight squat, dumbbell curl, band curl, and forearm plank with both available lenses. Confirm preview, pose startup, framing, tracking, and overlay alignment. Switch lenses, rotate, leave for help while tracking, reopen the session, and repeat rapidly. Save at least one workout.
5. Test a cold start with the camera occupied by another app or disabled by Android's camera privacy switch. Check the message, retry and exit. Verify in logcat that a closed session has no subsequent bind or inference callbacks.
6. In airplane mode, browse all thumbnails and saved demonstrations. With network enabled, test a queued unmetered-only download, cancellation, retry, a blocked download, and valid/invalid personal imports.
7. Pin both widgets on a phone launcher. Check empty states, all tap destinations, a saved workout, latest quote equality with Home, light/dark themes, Persian layout, resizing, and an overnight or post-reboot update. Launchers and power management can affect widget dimensions and refresh timing.

## Expanded offline exercise library

Exercises defaults to all available sources. The source picker shows counts for the MIT-licensed [Exercises Dataset](https://github.com/hasaneyldrm/exercises-dataset), OpenExerciseBase, and built-in exercises. The built-in library remains visible with either external source and can be selected on its own. Choose OpenExerciseBase to see the validated snapshot of its main branch. Search accepts both scripts for built-in names and filters, and tolerates a one-character typo in an exercise name. Equipment and muscle group filters intersect. The workout builder uses the same search and filters.

The combined view contains 39 built-in records, 1,324 Exercises Dataset records, and 191 professionally reviewed OpenExerciseBase records. IDs are namespaced by provider and preserved in the log. Some movements occur in more than one source. The native dedicated form analyzers cover four built-in movements. Every other entry supports live skeleton tracking, experimental calibrated counting for repetition movements and manual recording. Imported entries may retain their source-language names and instructions.

OpenExerciseBase images use [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/) and are credited to the Ludwig Boltzmann Institute for Digital Health and Prevention. The app bundles a downscaled thumbnail atlas and offers pinned original PNG downloads through the same WorkManager pipeline as RepDB. Exact, unique normalized name-and-equipment matches can also use these images. Other external records display a muscle-group diagram and support the existing file picker to add a personal image, GIF or MP4. Gym visual photos and GIFs referenced by Exercises Dataset carry separate terms and are excluded. See Settings → Licenses and credits for notices, revisions, original sources, and modification disclosures.

To regenerate the dataset, install Pillow in the development environment and run:

```bash
python tools/import_exercise_sources.py /path/to/exercises-dataset/data/exercises.json \
    /path/to/OpenExerciseBase-Database app/src/main/assets/exercise_sources
```

Use the two revisions pinned in the script. The tool removes restricted media fields, selects reviewed OpenExerciseBase records, and packages the normalized data and thumbnail atlas. It adds no APK dependency and requires no API key.

## Plan and progress

The bottom bar contains Home, Exercises, Start, History or Progress, and AI Analysis. Start stays centered. Open Plan and Settings from Home, and Health Connect from Settings. Set up an editable routine for any weekday in Plan. Start launches today's saved routine or opens the quick picker when the day is unscheduled. The workout builder preserves set, rep, duration, rest and equipment values in the saved routine. Single-exercise setup can prefill the last logged weight. The screen stays awake while a set or rest timer is active.

Progress opens with the daily training heatmap, consecutive-day streaks, seven days of completed sets by muscle group, and best estimated one-rep maxima from sets of 1–12 reps. Estimated maxima separate total and per-dumbbell loads. The log and its AI analysis remain under Progress → History. These changes use existing Room records and add DataStore keys only. See [the OpenGym feature audit](../docs/opengym-feature-audit.md) for the detailed comparison.

### Verification on a device

1. Open Exercises in English and Persian, then try dumbbell + shoulders, an impossible combination, and a misspelling. Tap the longest returned item and check wrapping at 165% font scale.
2. Open an OpenExerciseBase result offline. Confirm the thumbnail is present, then reconnect to download the original; try retry and cancel. Open a dataset entry without a verified image and import your own JPG or MP4.
3. Use a starter routine, save one weekday, restart, and edit the saved sets. Start on the configured day and save a session. Verify its history, heatmap, muscle totals, and both widgets.
4. Open Settings → Licenses and credits. Check that the source and license are available and the bundled license text opens offline.

## App features and catalog updates

Settings → App features selects Simple or Advanced and controls which tools
appear in each. Training, History and Exercises are available in both. App
feature visibility preserves saved preferences, permissions and background
schedules. Enable Plan to use either a weekly schedule or a rotation of
saved slots. The next slot advances when a workout with sets is saved.

Settings → Exercise catalog and the Exercises list can fetch a revised
offline catalog separately from app installation. The current APK keeps its
bundled fallback and downloaded media. See
[exercise catalog delivery](../catalog-content/README.md) for the data format,
publication steps and license requirements.


## Adaptive training and universal camera tracking

Open **Plan → Adaptive workout** (enable Plan under Settings → App features if needed). Choose focus, approximate duration and available equipment, then review the proposed routine. The local planner prioritizes muscle groups that were not logged in the past 48 hours, varies the routine by profile experience and goal, and changes rep targets using recent logged performance. Weight is retained from prior sets or left at zero for manual selection. The recent muscle summary reflects logged exercise activity, not a physiological recovery measurement. The fitness check-in logs a short squat and plank session in workout history and shows the last recorded counts/time. The existing AI Analysis question field supports workout coaching with the configured provider.

Camera permission and pose initialization now apply to every catalog exercise. For moves without a dedicated analyzer, capture two reference poses (three-second countdown). The tracker chooses a visible joint with a sufficiently different angle and counts a return to the first reference after reaching the second. This is an experimental estimate, requires stable framing and movement visibility, and does not assess form or safety. Static holds retain the manual timer. Recalibrate after changing the lens or angle; calibration persists across sets of the same movement. Manual correction and Continue without camera are available. No frames or reference coordinates are saved.

Catalog v3 adds four original bilingual movement records and schematic thumbnails. The Zing guides filter exposes matched existing exercises and explicit links to public reference pages, including the full web library. Zing media, proprietary SDKs and body-composition algorithms are not distributed. The app does not estimate body fat from camera images.

Device checks: calibrate a visible single-joint exercise and return to the initial pose; test slow movement, occlusion, phone rotation, lens switching, pause/resume, and a second set. Repeat for a timed hold and a denied permission. Compare estimated counts against actual manual counts before relying on them.
