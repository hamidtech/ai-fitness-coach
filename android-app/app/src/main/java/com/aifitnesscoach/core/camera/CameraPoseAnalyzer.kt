package com.aifitnesscoach.core.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aifitnesscoach.core.pose.PoseLandmarkerHelper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX ImageAnalysis.Analyzer that drops incoming frames whenever inference
 * is still busy, preventing UI thread stalls and queue backlog.
 */
class CameraPoseAnalyzer(
    private val landmarkerHelper: PoseLandmarkerHelper,
    private val isFrontCameraProvider: () -> Boolean,
    private val onFpsUpdated: (Float) -> Unit
) : ImageAnalysis.Analyzer {

    private val isBusy = AtomicBoolean(false)
    private var frameCounter = 0
    private var windowStartMs = SystemClock.elapsedRealtime()

    override fun analyze(imageProxy: ImageProxy) {
        if (!isBusy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            landmarkerHelper.detectLiveStream(
                bitmap = bitmap,
                rotationDegrees = rotation,
                isFrontCamera = isFrontCameraProvider()
            )

            frameCounter += 1
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - windowStartMs
            if (elapsed >= 1000L) {
                val fps = frameCounter * 1000f / elapsed.toFloat()
                onFpsUpdated(fps)
                frameCounter = 0
                windowStartMs = now
            }
        } finally {
            imageProxy.close()
            isBusy.set(false)
        }
    }
}
