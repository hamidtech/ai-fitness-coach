package com.aifitnesscoach.core.camera

enum class WorkoutCameraState {
    INITIALIZING_CAMERA,
    INITIALIZING_POSE,
    CAMERA_READY,
    WORKOUT_READY,
    CAMERA_UNAVAILABLE,
    PREVIEW_ERROR,
    POSE_ERROR
}

fun workoutCameraState(
    streaming: Boolean,
    needsPose: Boolean,
    poseReady: Boolean,
    failure: WorkoutCameraState?
): WorkoutCameraState = when {
    failure != null -> failure
    !streaming -> WorkoutCameraState.INITIALIZING_CAMERA
    !needsPose -> WorkoutCameraState.CAMERA_READY
    !poseReady -> WorkoutCameraState.INITIALIZING_POSE
    else -> WorkoutCameraState.WORKOUT_READY
}

enum class CameraPermissionState {
    REQUIRED, DENIED, SETTINGS_REQUIRED, GRANTED
}

fun cameraPermissionState(granted: Boolean, requested: Boolean, showRationale: Boolean): CameraPermissionState = when {
    granted -> CameraPermissionState.GRANTED
    !requested -> CameraPermissionState.REQUIRED
    showRationale -> CameraPermissionState.DENIED
    else -> CameraPermissionState.SETTINGS_REQUIRED
}
