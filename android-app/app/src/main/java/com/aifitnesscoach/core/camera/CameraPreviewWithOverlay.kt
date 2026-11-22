package com.aifitnesscoach.core.camera

import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aifitnesscoach.core.pose.PoseFrame
import com.aifitnesscoach.core.pose.PoseLandmarkIndex
import java.util.concurrent.Executors
import kotlin.math.max

private val POSE_CONNECTIONS: List<Pair<PoseLandmarkIndex, PoseLandmarkIndex>> = listOf(
    PoseLandmarkIndex.LEFT_SHOULDER to PoseLandmarkIndex.RIGHT_SHOULDER,
    PoseLandmarkIndex.LEFT_SHOULDER to PoseLandmarkIndex.LEFT_ELBOW,
    PoseLandmarkIndex.LEFT_ELBOW to PoseLandmarkIndex.LEFT_WRIST,
    PoseLandmarkIndex.RIGHT_SHOULDER to PoseLandmarkIndex.RIGHT_ELBOW,
    PoseLandmarkIndex.RIGHT_ELBOW to PoseLandmarkIndex.RIGHT_WRIST,
    PoseLandmarkIndex.LEFT_SHOULDER to PoseLandmarkIndex.LEFT_HIP,
    PoseLandmarkIndex.RIGHT_SHOULDER to PoseLandmarkIndex.RIGHT_HIP,
    PoseLandmarkIndex.LEFT_HIP to PoseLandmarkIndex.RIGHT_HIP,
    PoseLandmarkIndex.LEFT_HIP to PoseLandmarkIndex.LEFT_KNEE,
    PoseLandmarkIndex.LEFT_KNEE to PoseLandmarkIndex.LEFT_ANKLE,
    PoseLandmarkIndex.RIGHT_HIP to PoseLandmarkIndex.RIGHT_KNEE,
    PoseLandmarkIndex.RIGHT_KNEE to PoseLandmarkIndex.RIGHT_ANKLE
)

@Composable
fun CameraPreviewWithOverlay(
    useFrontCamera: Boolean,
    showSkeletonOverlay: Boolean,
    latestPoseFrame: PoseFrame?,
    analyzer: CameraPoseAnalyzer,
    skeletonColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(useFrontCamera, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val displayRotation = previewView.display?.rotation ?: Surface.ROTATION_0
            val preview = Preview.Builder()
                .setTargetRotation(displayRotation)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetRotation(displayRotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyzer)
                }

            val selector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (
            showSkeletonOverlay &&
            latestPoseFrame != null &&
            latestPoseFrame.landmarks.size >= 33 &&
            latestPoseFrame.isFrontCameraMirrored == useFrontCamera
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val srcW = latestPoseFrame.sourceWidth.takeIf { it > 0 }?.toFloat() ?: size.width
                val srcH = latestPoseFrame.sourceHeight.takeIf { it > 0 }?.toFloat() ?: size.height

                // PreviewView.ScaleType.FILL_CENTER scaling alignment
                val scale = max(size.width / srcW, size.height / srcH)
                val scaledW = srcW * scale
                val scaledH = srcH * scale
                val offsetX = (size.width - scaledW) / 2f
                val offsetY = (size.height - scaledH) / 2f

                fun mapPoint(idx: PoseLandmarkIndex): Offset? {
                    val lm = latestPoseFrame.landmark(idx) ?: return null
                    if (lm.confidence < 0.45f) return null
                    return Offset(
                        x = lm.x * scaledW + offsetX,
                        y = lm.y * scaledH + offsetY
                    )
                }

                POSE_CONNECTIONS.forEach { (startIdx, endIdx) ->
                    val p1 = mapPoint(startIdx)
                    val p2 = mapPoint(endIdx)
                    if (p1 != null && p2 != null) {
                        drawLine(
                            color = skeletonColor,
                            start = p1,
                            end = p2,
                            strokeWidth = 7f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                PoseLandmarkIndex.entries.forEach { idx ->
                    val pt = mapPoint(idx)
                    if (pt != null) {
                        drawCircle(
                            color = Color.White,
                            radius = 7f,
                            center = pt
                        )
                        drawCircle(
                            color = skeletonColor,
                            radius = 4.5f,
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
