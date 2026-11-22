# Local Assets Directory

The MediaPipe Lite model is checked in for offline inference. Exercise identification also works offline using the original drawings in `core/media/ExerciseIllustration.kt`.

1. **MediaPipe Pose Landmarker Model**:
   - The shipped model is `pose_landmarker_lite.task` from Google MediaPipe Solutions.
   - Place it at: `app/src/main/assets/pose_landmarker_lite.task`

2. **Persian Typography (Optional Vazirmatn Font)**:
   - Place `Vazirmatn-Regular.ttf` and `Vazirmatn-Bold.ttf` in `app/src/main/res/font/` as `vazirmatn_regular.ttf` and `vazirmatn_bold.ttf` (OFL-1.1 licensed).
   - `Theme.kt` automatically falls back to system sans-serif if custom font resources are absent.

3. **Exercise Demonstration Media**:
   - Place offline `.mp4`, `.gif`, or `.webp` files in `app/src/main/assets/demos/` using the naming convention defined in `ExerciseCatalog.kt`:
     - e.g., `squat_thumb.webp`, `squat_demo.mp4`
     - `dumbbell_curl_thumb.webp`, `dumbbell_curl_demo.mp4`
     - `forearm_plank_thumb.webp`, `forearm_plank_demo.mp4`
     - `band_biceps_curl_thumb.webp`, `band_biceps_curl_demo.mp4`
   - The player falls back to the drawing for the exercise ID if bundled, downloaded, or imported media is unavailable. See [media provenance and licensing](../../../../README.md#exercise-visuals-and-media).
