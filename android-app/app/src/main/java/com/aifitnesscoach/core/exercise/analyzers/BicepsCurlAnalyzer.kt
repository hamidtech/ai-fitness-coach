package com.aifitnesscoach.core.exercise.analyzers

import com.aifitnesscoach.core.exercise.AnalysisResult
import com.aifitnesscoach.core.exercise.CoachingMessageKey
import com.aifitnesscoach.core.exercise.ExerciseAnalyzer
import com.aifitnesscoach.core.exercise.FeedbackSeverity
import com.aifitnesscoach.core.exercise.FormObservation
import com.aifitnesscoach.core.exercise.MovementPhase
import com.aifitnesscoach.core.pose.JointAngleMath
import com.aifitnesscoach.core.pose.NormalizedLandmark3D
import com.aifitnesscoach.core.pose.PoseFrame
import com.aifitnesscoach.core.pose.PoseLandmarkIndex
import com.aifitnesscoach.core.pose.TrackingQuality
import kotlin.math.max
import kotlin.math.min

/**
 * Shared deterministic curl analyzer used by both:
 * - dumbbell_biceps_curl (Weighted Training)
 * - band_biceps_curl (Resistance Band Training)
 *
 * Evaluates elbow flexion/extension, repetition counting, partial reps, and
 * observable upper-arm drift relative to the vertical torso line.
 * Never estimates weight or band resistance force.
 */
class BicepsCurlAnalyzer(
    override val exerciseId: String
) : ExerciseAnalyzer {

    private var phase: MovementPhase = MovementPhase.IDLE
    private var lastPhaseBeforeLoss: MovementPhase = MovementPhase.IDLE
    private var phaseEnteredAtMs: Long = 0L
    private var repCount: Int = 0
    private var minElbowAngleInCycle: Float = 180f
    private var maxUpperArmDriftDeg: Float = 0f
    private var smoothedElbowAngle: Float? = null
    private var activeFeedback: CoachingMessageKey = CoachingMessageKey.READY
    private val observations = mutableListOf<FormObservation>()

    companion object {
        private const val MIN_CONFIDENCE = 0.55f
        private const val EXTENDED_MIN_DEG = 148f
        private const val FLEX_ENTER_DEG = 135f
        private const val PEAK_FLEXION_MAX_DEG = 58f
        private const val EXTEND_HYSTERESIS_DEG = 70f
        private const val UPPER_ARM_DRIFT_WARN_DEG = 26f
        private const val MIN_PHASE_DURATION_MS = 130L
        private const val SMOOTHING_ALPHA = 0.45f
    }

    override fun reset() {
        phase = MovementPhase.IDLE
        lastPhaseBeforeLoss = MovementPhase.IDLE
        phaseEnteredAtMs = 0L
        repCount = 0
        minElbowAngleInCycle = 180f
        maxUpperArmDriftDeg = 0f
        smoothedElbowAngle = null
        activeFeedback = CoachingMessageKey.READY
        observations.clear()
    }

    override fun processFrame(frame: PoseFrame): AnalysisResult {
        val side = selectBestVisibleSide(frame)
        if (side == null) {
            if (phase != MovementPhase.PAUSED_TRACKING_LOST) {
                lastPhaseBeforeLoss = phase
                phase = MovementPhase.PAUSED_TRACKING_LOST
            }
            return AnalysisResult(
                exerciseId = exerciseId,
                phase = MovementPhase.PAUSED_TRACKING_LOST,
                repCount = repCount,
                holdDurationMs = 0L,
                trackingQuality = if (frame.landmarks.size < 33) TrackingQuality.LOST else TrackingQuality.LOW_CONFIDENCE,
                primaryAngleDegrees = null,
                secondaryAngleDegrees = null,
                activeFeedbackKey = CoachingMessageKey.TRACKING_PAUSED,
                observations = observations.toList(),
                uncertaintyWarningEn = "Required shoulder, elbow, and wrist landmarks are not clearly visible.",
                uncertaintyWarningFa = "نقاط شانه، آرنج و مچ دست به‌وضوح در کادر دیده نمی‌شوند."
            )
        }

        if (phase == MovementPhase.PAUSED_TRACKING_LOST) {
            phase = if (lastPhaseBeforeLoss == MovementPhase.PAUSED_TRACKING_LOST) MovementPhase.IDLE else lastPhaseBeforeLoss
            phaseEnteredAtMs = frame.timestampMs
        }

        val (shoulder, elbow, wrist) = side
        val rawElbowAngle = JointAngleMath.angleDegrees2D(shoulder, elbow, wrist)
        val upperArmDrift = JointAngleMath.segmentAngleFromVerticalDegrees(shoulder, elbow)

        val elbowAngle = smoothedElbowAngle?.let {
            SMOOTHING_ALPHA * rawElbowAngle + (1f - SMOOTHING_ALPHA) * it
        } ?: rawElbowAngle
        smoothedElbowAngle = elbowAngle

        if (phaseEnteredAtMs == 0L) phaseEnteredAtMs = frame.timestampMs
        val elapsedInPhase = frame.timestampMs - phaseEnteredAtMs

        when (phase) {
            MovementPhase.IDLE -> {
                if (elbowAngle >= EXTENDED_MIN_DEG) {
                    phase = MovementPhase.EXTENDING
                    phaseEnteredAtMs = frame.timestampMs
                    minElbowAngleInCycle = 180f
                    maxUpperArmDriftDeg = 0f
                    activeFeedback = CoachingMessageKey.READY
                }
            }

            MovementPhase.EXTENDING -> {
                if (phaseEnteredAtMs != 0L && repCount > 0 && elbowAngle >= EXTENDED_MIN_DEG && activeFeedback == CoachingMessageKey.CURL_TOP) {
                    activeFeedback = CoachingMessageKey.READY
                }
                if (elbowAngle <= FLEX_ENTER_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    phase = MovementPhase.FLEXING
                    phaseEnteredAtMs = frame.timestampMs
                    minElbowAngleInCycle = elbowAngle
                    maxUpperArmDriftDeg = upperArmDrift
                    activeFeedback = CoachingMessageKey.CURL_FLEXING
                }
            }

            MovementPhase.FLEXING -> {
                minElbowAngleInCycle = min(minElbowAngleInCycle, elbowAngle)
                maxUpperArmDriftDeg = max(maxUpperArmDriftDeg, upperArmDrift)

                if (upperArmDrift > UPPER_ARM_DRIFT_WARN_DEG) {
                    activeFeedback = CoachingMessageKey.CURL_ELBOW_DRIFT
                }

                if (elbowAngle <= PEAK_FLEXION_MAX_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    phase = MovementPhase.PEAK_FLEXION
                    phaseEnteredAtMs = frame.timestampMs
                    activeFeedback = CoachingMessageKey.CURL_TOP
                } else if (elbowAngle >= EXTENDED_MIN_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    recordObservation(
                        key = CoachingMessageKey.CURL_PARTIAL,
                        severity = FeedbackSeverity.WARNING,
                        timestampMs = frame.timestampMs,
                        detailEn = "Incomplete curl range (min elbow angle ${minElbowAngleInCycle.toInt()}°).",
                        detailFa = "تکرار ناقص (حداقل زاویه آرنج ${minElbowAngleInCycle.toInt()}°)."
                    )
                    activeFeedback = CoachingMessageKey.CURL_PARTIAL
                    phase = MovementPhase.EXTENDING
                    phaseEnteredAtMs = frame.timestampMs
                    minElbowAngleInCycle = 180f
                    maxUpperArmDriftDeg = 0f
                }
            }

            MovementPhase.PEAK_FLEXION -> {
                minElbowAngleInCycle = min(minElbowAngleInCycle, elbowAngle)
                maxUpperArmDriftDeg = max(maxUpperArmDriftDeg, upperArmDrift)
                if (elbowAngle >= EXTEND_HYSTERESIS_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    repCount += 1
                    if (maxUpperArmDriftDeg > UPPER_ARM_DRIFT_WARN_DEG) {
                        recordObservation(
                            key = CoachingMessageKey.CURL_ELBOW_DRIFT,
                            severity = FeedbackSeverity.WARNING,
                            timestampMs = frame.timestampMs,
                            detailEn = "Rep $repCount: upper arm drifted ${maxUpperArmDriftDeg.toInt()}° from vertical.",
                            detailFa = "تکرار $repCount: بازو حدود ${maxUpperArmDriftDeg.toInt()}° از خط عمود فاصله گرفت."
                        )
                    } else {
                        recordObservation(
                            key = CoachingMessageKey.CURL_TOP,
                            severity = FeedbackSeverity.INFO,
                            timestampMs = frame.timestampMs,
                            detailEn = "Rep $repCount completed (ROM to ${minElbowAngleInCycle.toInt()}°).",
                            detailFa = "تکرار $repCount تکمیل شد (حداقل زاویه ${minElbowAngleInCycle.toInt()}°)."
                        )
                    }
                    phase = MovementPhase.EXTENDING
                    phaseEnteredAtMs = frame.timestampMs
                    minElbowAngleInCycle = 180f
                    maxUpperArmDriftDeg = 0f
                }
            }

            else -> {
                phase = MovementPhase.IDLE
                phaseEnteredAtMs = frame.timestampMs
            }
        }

        return AnalysisResult(
            exerciseId = exerciseId,
            phase = phase,
            repCount = repCount,
            holdDurationMs = 0L,
            trackingQuality = TrackingQuality.GOOD,
            primaryAngleDegrees = elbowAngle,
            secondaryAngleDegrees = upperArmDrift,
            activeFeedbackKey = activeFeedback,
            observations = observations.toList()
        )
    }

    private fun recordObservation(
        key: CoachingMessageKey,
        severity: FeedbackSeverity,
        timestampMs: Long,
        detailEn: String,
        detailFa: String
    ) {
        if (observations.size >= 50) observations.removeAt(0)
        observations.add(FormObservation(key, severity, timestampMs, detailEn, detailFa))
    }

    private data class ArmChain(
        val shoulder: NormalizedLandmark3D,
        val elbow: NormalizedLandmark3D,
        val wrist: NormalizedLandmark3D
    )

    private fun selectBestVisibleSide(frame: PoseFrame): ArmChain? {
        if (frame.landmarks.size < 33) return null
        val leftIds = listOf(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_ELBOW,
            PoseLandmarkIndex.LEFT_WRIST
        )
        val rightIds = listOf(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_ELBOW,
            PoseLandmarkIndex.RIGHT_WRIST
        )

        val leftOk = frame.hasRequiredLandmarks(leftIds, MIN_CONFIDENCE)
        val rightOk = frame.hasRequiredLandmarks(rightIds, MIN_CONFIDENCE)
        if (!leftOk && !rightOk) return null

        val useLeft = when {
            leftOk && !rightOk -> true
            !leftOk && rightOk -> false
            else -> frame.averageConfidence(leftIds) >= frame.averageConfidence(rightIds)
        }
        val ids = if (useLeft) leftIds else rightIds
        return ArmChain(
            shoulder = frame.landmark(ids[0])!!,
            elbow = frame.landmark(ids[1])!!,
            wrist = frame.landmark(ids[2])!!
        )
    }
}
