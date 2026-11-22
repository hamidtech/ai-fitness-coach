package com.aifitnesscoach.core.exercise

import com.aifitnesscoach.core.equipment.ResistanceBandType
import com.aifitnesscoach.core.pose.PoseLandmarkIndex

enum class ExerciseCategory {
    WEIGHTED_TRAINING,
    BODYWEIGHT_TRAINING,
    CORE_TRAINING,
    RESISTANCE_BAND_TRAINING
}

enum class ExerciseMode {
    REPETITIONS,
    TIMED_HOLD,
    ALTERNATING_REPETITIONS
}

enum class RecommendedCameraView(val labelEn: String, val labelFa: String) {
    SIDE("Side View", "نمای جانبی"),
    FRONT("Front View", "نمای روبه‌رو"),
    THREE_QUARTER("Three-Quarter View", "نمای سه‌رخ")
}

enum class ImplementationStatus {
    IMPLEMENTED_NEEDS_VALIDATION,
    EXPERIMENTAL,
    COMING_SOON
}

enum class EquipmentCategoryType {
    DUMBBELL,
    BODYWEIGHT,
    MAT_OR_FLOOR,
    WALL,
    RESISTANCE_BAND
}

data class CameraSetupGuide(
    val recommendedView: RecommendedCameraView,
    val distanceEn: String,
    val distanceFa: String,
    val heightEn: String,
    val heightFa: String
)

data class MediaAssetRef(
    val thumbnailAsset: String,
    val demoVideoAsset: String,
    val demoGifAsset: String? = null,
    val availableViews: List<RecommendedCameraView> = listOf(RecommendedCameraView.SIDE)
)

data class TrackingConfiguration(
    val minConfidence: Float = 0.55f,
    val minPhaseDurationMs: Long = 160L,
    val hysteresisDegrees: Float = 8f
)

data class ExerciseDefinition(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val category: ExerciseCategory,
    val descriptionEn: String,
    val descriptionFa: String,
    val targetMusclesEn: List<String>,
    val targetMusclesFa: List<String>,
    val equipmentType: EquipmentCategoryType,
    val supportedBandTypes: List<ResistanceBandType> = emptyList(),
    val requiresAnchor: Boolean = false,
    val anchorSetupEn: String? = null,
    val anchorSetupFa: String? = null,
    val exerciseMode: ExerciseMode,
    val cameraGuide: CameraSetupGuide,
    val requiredLandmarks: List<PoseLandmarkIndex>,
    val trackingConfiguration: TrackingConfiguration = TrackingConfiguration(),
    val analyzerId: String?,
    val demoMedia: MediaAssetRef,
    val instructionsEn: List<String>,
    val instructionsFa: List<String>,
    val commonMistakesEn: List<String>,
    val commonMistakesFa: List<String>,
    val observableFormChecksEn: List<String>,
    val observableFormChecksFa: List<String>,
    val limitationsEn: List<String>,
    val limitationsFa: List<String>,
    val implementationStatus: ImplementationStatus
)
