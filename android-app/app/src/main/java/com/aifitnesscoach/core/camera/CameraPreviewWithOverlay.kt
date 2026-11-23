package com.aifitnesscoach.core.camera

import android.view.Surface
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraState
import androidx.lifecycle.Observer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.aifitnesscoach.core.pose.PoseLandmarkerHelper
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
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

private const val CAMERA_TAG = "WorkoutCamera"

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
    needsPose: Boolean,
    onPose: (PoseFrame) -> Unit,
    onFps: (Float) -> Unit,
    onState: (WorkoutCameraState) -> Unit,
    skeletonColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val poseCallback by rememberUpdatedState(onPose)
    val fpsCallback by rememberUpdatedState(onFps)
    val stateCallback by rememberUpdatedState(onState)
    var streaming by remember(useFrontCamera, needsPose, lifecycleOwner) { mutableStateOf(false) }
    var poseReady by remember(useFrontCamera, needsPose, lifecycleOwner) { mutableStateOf(false) }
    var failure by remember(useFrontCamera, needsPose, lifecycleOwner) {
        mutableStateOf<WorkoutCameraState?>(null)
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(streaming, needsPose, poseReady, failure) {
        stateCallback(workoutCameraState(streaming, needsPose, poseReady, failure))
    }

    LaunchedEffect(lifecycleOwner, streaming, needsPose, poseReady, failure) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (failure == null && (!streaming || (needsPose && !poseReady))) {
                delay(10_000)
                Log.w(CAMERA_TAG, "startup_timeout streaming=$streaming")
                failure = if (streaming) WorkoutCameraState.POSE_ERROR else WorkoutCameraState.PREVIEW_ERROR
            }
        }
    }

    DisposableEffect(useFrontCamera, needsPose, lifecycleOwner) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val worker = Executors.newSingleThreadExecutor()
        val closed = AtomicBoolean(false)
        val analysisFailed = AtomicBoolean(false)
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var analysis: ImageAnalysis? = null
        var cameraState: androidx.lifecycle.LiveData<CameraState>? = null
        val streamObserver = Observer<PreviewView.StreamState> { state ->
            streaming = state == PreviewView.StreamState.STREAMING
            Log.i(CAMERA_TAG, "preview_streaming=$streaming")
        }
        val cameraObserver = Observer<CameraState> { state ->
            state.error?.let {
                Log.w(CAMERA_TAG, "camera_state_error code=${it.code}")
                failure = WorkoutCameraState.PREVIEW_ERROR
            }
        }

        fun poseFailed() {
            analysisFailed.set(true)
            mainExecutor.execute {
                if (!closed.get()) {
                    analysis?.clearAnalyzer()
                    failure = WorkoutCameraState.POSE_ERROR
                }
            }
        }
        val helper = if (needsPose) PoseLandmarkerHelper(
            context = context.applicationContext,
            onResult = { frame ->
                mainExecutor.execute {
                    if (!closed.get() && !analysisFailed.get()) {
                        try {
                            poseCallback(frame)
                        } catch (error: RuntimeException) {
                            Log.e(CAMERA_TAG, "pose_result_failed", error)
                            poseFailed()
                        }
                    }
                }
            },
            onError = { poseFailed() }
        ) else null

        Log.i(CAMERA_TAG, "session_start front=$useFrontCamera pose=$needsPose")
        try {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!closed.get()) {
                    try {
                        val cameraProvider = future.get()
                        provider = cameraProvider
                        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                            else CameraSelector.DEFAULT_BACK_CAMERA
                        if (!cameraProvider.hasCamera(selector)) {
                            Log.w(CAMERA_TAG, "lens_unavailable front=$useFrontCamera")
                            failure = WorkoutCameraState.CAMERA_UNAVAILABLE
                        } else {
                            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                            val previewUseCase = Preview.Builder().setTargetRotation(rotation).build()
                            preview = previewUseCase
                            previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
                            previewView.previewStreamState.observe(lifecycleOwner, streamObserver)

                            val camera = if (helper == null) {
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, previewUseCase)
                            } else {
                                val frames = ImageAnalysis.Builder()
                                    .setTargetRotation(rotation)
                                    .setTargetResolution(Size(640, 480))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .build()
                                analysis = frames
                                try {
                                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, previewUseCase, frames)
                                } catch (error: IllegalArgumentException) {
                                    Log.w(CAMERA_TAG, "analysis_binding_unsupported", error)
                                    cameraProvider.unbind(previewUseCase, frames)
                                    analysis = null
                                    failure = WorkoutCameraState.POSE_ERROR
                                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, previewUseCase)
                                }
                            }
                            cameraState = camera.cameraInfo.cameraState
                            cameraState?.observe(lifecycleOwner, cameraObserver)
                            Log.i(CAMERA_TAG, "preview_bound")

                            val frames = analysis
                            if (helper != null && frames != null) {
                                worker.execute {
                                    if (!closed.get() && helper.initialize()) {
                                        mainExecutor.execute {
                                            if (!closed.get() && failure == null) {
                                                frames.setAnalyzer(worker, CameraPoseAnalyzer(
                                                    landmarkerHelper = helper,
                                                    isFrontCamera = useFrontCamera,
                                                    onFpsUpdated = { fps ->
                                                        mainExecutor.execute {
                                                            if (!closed.get()) fpsCallback(fps)
                                                        }
                                                    },
                                                    onFailure = ::poseFailed
                                                ))
                                                poseReady = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (error: Exception) {
                        Log.e(CAMERA_TAG, "provider_or_binding_failed", error)
                        failure = WorkoutCameraState.PREVIEW_ERROR
                    }
                }
            }, mainExecutor)
        } catch (error: RuntimeException) {
            Log.e(CAMERA_TAG, "provider_request_failed", error)
            failure = WorkoutCameraState.PREVIEW_ERROR
        }

        onDispose {
            closed.set(true)
            previewView.previewStreamState.removeObserver(streamObserver)
            cameraState?.removeObserver(cameraObserver)
            analysis?.clearAnalyzer()
            val useCases = listOfNotNull(preview, analysis)
            try {
                if (useCases.isNotEmpty()) provider?.unbind(*useCases.toTypedArray())
            } catch (error: RuntimeException) {
                Log.w(CAMERA_TAG, "unbind_failed", error)
            }
            worker.execute { helper?.close() }
            worker.shutdown()
            Log.i(CAMERA_TAG, "session_stop")
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
