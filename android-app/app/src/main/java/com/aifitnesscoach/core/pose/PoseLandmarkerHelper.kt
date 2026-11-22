package com.aifitnesscoach.core.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Wraps MediaPipe Pose Landmarker in LIVE_STREAM mode.
 * Uses the CPU delegate to avoid device-specific EGL setup during camera startup.
 * Never uploads frames or landmarks off-device.
 */
class PoseLandmarkerHelper(
    private val context: Context,
    private val onResult: (PoseFrame) -> Unit,
    private val onError: (String) -> Unit
) {
    private var poseLandmarker: PoseLandmarker? = null
    @Volatile
    private var isFrontCamera: Boolean = true
    @Volatile
    private var lastFrameWidth: Int = 0
    @Volatile
    private var lastFrameHeight: Int = 0
    @Volatile
    private var lastTimestampMs: Long = 0L

    companion object {
        private const val MODEL_ASSET_PATH = "pose_landmarker_lite.task"
        private const val MIN_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_PRESENCE_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f
    }

    fun initialize(): Boolean {
        close()
        lastTimestampMs = 0L
        return tryCreateLandmarker()
    }

    private fun tryCreateLandmarker(): Boolean {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET_PATH)
                .setDelegate(Delegate.CPU)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(MIN_DETECTION_CONFIDENCE)
                .setMinPosePresenceConfidence(MIN_PRESENCE_CONFIDENCE)
                .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
                .setResultListener { result, input ->
                    try {
                        handleResult(result, input.width, input.height)
                    } catch (error: RuntimeException) {
                        Log.e("WorkoutCamera", "pose_callback_failed", error)
                        onError("pose_callback_failed")
                    }
                }
                .setErrorListener { error ->
                    Log.e("WorkoutCamera", "pose_runtime_failed", error)
                    onError("pose_runtime_failed")
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.i("WorkoutCamera", "pose_model_ready delegate=CPU")
            true
        } catch (error: RuntimeException) {
            Log.e("WorkoutCamera", "pose_model_load_failed", error)
            onError("pose_model_load_failed")
            false
        } catch (error: LinkageError) {
            Log.e("WorkoutCamera", "pose_native_library_failed", error)
            onError("pose_native_library_failed")
            false
        }
    }

    fun detectLiveStream(
        bitmap: Bitmap,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ) {
        val landmarker = poseLandmarker ?: return
        this.isFrontCamera = isFrontCamera

        val transformed = transformBitmap(bitmap, rotationDegrees, isFrontCamera)
        lastFrameWidth = transformed.width
        lastFrameHeight = transformed.height

        val mpImage = BitmapImageBuilder(transformed).build()
        val now = SystemClock.uptimeMillis()
        val frameTimeMs = if (now <= lastTimestampMs) lastTimestampMs + 1L else now
        lastTimestampMs = frameTimeMs
        landmarker.detectAsync(mpImage, frameTimeMs)
    }

    private fun transformBitmap(
        source: Bitmap,
        rotationDegrees: Int,
        mirrorHorizontally: Boolean
    ): Bitmap {
        if (rotationDegrees == 0 && !mirrorHorizontally) return source
        val matrix = Matrix().apply {
            if (rotationDegrees != 0) {
                postRotate(rotationDegrees.toFloat())
            }
            if (mirrorHorizontally) {
                // Mirror horizontally in post-rotation output coordinates;
                // Bitmap.createBitmap automatically translates the bounding rect back to (0, 0).
                postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)
    }

    private fun handleResult(
        result: PoseLandmarkerResult,
        width: Int,
        height: Int
    ) {
        val inferenceMs = (SystemClock.uptimeMillis() - result.timestampMs()).coerceAtLeast(0L)
        val firstPose = result.landmarks().firstOrNull()
        val mapped = firstPose?.map { lm ->
            NormalizedLandmark3D(
                x = lm.x(),
                y = lm.y(),
                z = lm.z(),
                visibility = lm.visibility().orElse(1f),
                presence = lm.presence().orElse(1f)
            )
        } ?: emptyList()

        onResult(
            PoseFrame(
                timestampMs = result.timestampMs(),
                landmarks = mapped,
                inferenceTimeMs = inferenceMs,
                sourceWidth = width.takeIf { it > 0 } ?: lastFrameWidth,
                sourceHeight = height.takeIf { it > 0 } ?: lastFrameHeight,
                isFrontCameraMirrored = isFrontCamera
            )
        )
    }

    fun close() {
        val landmarker = poseLandmarker
        poseLandmarker = null
        try {
            landmarker?.close()
        } catch (error: RuntimeException) {
            Log.w("WorkoutCamera", "pose_close_failed", error)
        } catch (error: LinkageError) {
            Log.w("WorkoutCamera", "pose_close_native_library_failed", error)
        }
    }
}
