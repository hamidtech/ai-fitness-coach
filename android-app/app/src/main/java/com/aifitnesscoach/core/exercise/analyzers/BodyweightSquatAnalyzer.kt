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
import kotlin.math.min

/**
 * Deterministic finite-state analyzer for Bodyweight Squat.
 * Evaluates knee & hip flexion with hysteresis and minimum valid phase durations.
 */
class BodyweightSquatAnalyzer : ExerciseAnalyzer {
    override val exerciseId: String = "bodyweight_squat"

    private var phase: MovementPhase = MovementPhase.IDLE
    private var lastPhaseBeforeLoss: MovementPhase = MovementPhase.IDLE
    private var phaseEnteredAtMs: Long = 0L
    private var repCount: Int = 0
    private var minKneeAngleInCycle: Float = 180f
    private var smoothedKneeAngle: Float? = null
    private var activeFeedback: CoachingMessageKey = CoachingMessageKey.READY
    private val observations = mutableListOf<FormObservation>()

    companion object {
        private const val MIN_CONFIDENCE = 0.55f
        private const val STANDING_KNEE_MIN_DEG = 160f
        private const val DESCEND_ENTER_DEG = 148f
        private const val GOOD_DEPTH_KNEE_MAX_DEG = 105f
        private const val ASCEND_HYSTERESIS_DEG = 114f
        private const val MIN_PHASE_DURATION_MS = 140L
        private const val SMOOTHING_ALPHA = 0.45f
    }

    override fun reset() {
        phase = MovementPhase.IDLE
        lastPhaseBeforeLoss = MovementPhase.IDLE
        phaseEnteredAtMs = 0L
        repCount = 0
        minKneeAngleInCycle = 180f
        smoothedKneeAngle = null
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
                uncertaintyWarningEn = "Required hip, knee, and ankle landmarks are not clearly visible.",
                uncertaintyWarningFa = "نقاط لگن، زانو و مچ پا به‌وضوح در کادر دیده نمی‌شوند."
            )
        }

        if (phase == MovementPhase.PAUSED_TRACKING_LOST) {
            phase = if (lastPhaseBeforeLoss == MovementPhase.PAUSED_TRACKING_LOST) MovementPhase.IDLE else lastPhaseBeforeLoss
            phaseEnteredAtMs = frame.timestampMs
        }

        val (shoulder, hip, knee, ankle) = side
        val rawKneeAngle = JointAngleMath.angleDegrees2D(hip, knee, ankle)
        val hipAngle = JointAngleMath.angleDegrees2D(shoulder, hip, knee)
        val kneeAngle = smoothedKneeAngle?.let {
            SMOOTHING_ALPHA * rawKneeAngle + (1f - SMOOTHING_ALPHA) * it
        } ?: rawKneeAngle
        smoothedKneeAngle = kneeAngle

        if (phaseEnteredAtMs == 0L) phaseEnteredAtMs = frame.timestampMs
        val elapsedInPhase = frame.timestampMs - phaseEnteredAtMs

        when (phase) {
            MovementPhase.IDLE, MovementPhase.STANDING -> {
                if (kneeAngle >= STANDING_KNEE_MIN_DEG) {
                    if (phase != MovementPhase.STANDING) {
                        phase = MovementPhase.STANDING
                        phaseEnteredAtMs = frame.timestampMs
                        minKneeAngleInCycle = 180f
                        activeFeedback = CoachingMessageKey.READY
                    }
                } else if (kneeAngle < DESCEND_ENTER_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    phase = MovementPhase.DESCENDING
                    phaseEnteredAtMs = frame.timestampMs
                    minKneeAngleInCycle = kneeAngle
                    activeFeedback = CoachingMessageKey.SQUAT_DESCENDING
                }
            }

            MovementPhase.DESCENDING -> {
                minKneeAngleInCycle = min(minKneeAngleInCycle, kneeAngle)
                if (kneeAngle <= GOOD_DEPTH_KNEE_MAX_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    phase = MovementPhase.BOTTOM
                    phaseEnteredAtMs = frame.timestampMs
                    activeFeedback = CoachingMessageKey.SQUAT_GOOD_DEPTH
                } else if (kneeAngle >= STANDING_KNEE_MIN_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    // User stood back up without reaching target squat depth
                    recordObservation(
                        key = CoachingMessageKey.SQUAT_SHALLOW,
                        severity = FeedbackSeverity.WARNING,
                        timestampMs = frame.timestampMs,
                        detailEn = "Incomplete squat depth (min knee angle ${minKneeAngleInCycle.toInt()}°).",
                        detailFa = "عمق اسکوات کافی نبود (حداقل زاویه زانو ${minKneeAngleInCycle.toInt()}°)."
                    )
                    activeFeedback = CoachingMessageKey.SQUAT_SHALLOW
                    phase = MovementPhase.STANDING
                    phaseEnteredAtMs = frame.timestampMs
                    minKneeAngleInCycle = 180f
                }
            }

            MovementPhase.BOTTOM -> {
                minKneeAngleInCycle = min(minKneeAngleInCycle, kneeAngle)
                if (kneeAngle >= ASCEND_HYSTERESIS_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    phase = MovementPhase.ASCENDING
                    phaseEnteredAtMs = frame.timestampMs
                }
            }

            MovementPhase.ASCENDING -> {
                if (kneeAngle >= STANDING_KNEE_MIN_DEG && elapsedInPhase >= MIN_PHASE_DURATION_MS) {
                    repCount += 1
                    recordObservation(
                        key = CoachingMessageKey.SQUAT_GOOD_DEPTH,
                        severity = FeedbackSeverity.INFO,
                        timestampMs = frame.timestampMs,
                        detailEn = "Rep $repCount completed (knee angle ${minKneeAngleInCycle.toInt()}°).",
                        detailFa = "تکرار $repCount تکمیل شد (زاویه زانو ${minKneeAngleInCycle.toInt()}°)."
                    )
                    phase = MovementPhase.STANDING
                    phaseEnteredAtMs = frame.timestampMs
                    minKneeAngleInCycle = 180f
                    activeFeedback = CoachingMessageKey.READY
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
            primaryAngleDegrees = kneeAngle,
            secondaryAngleDegrees = hipAngle,
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

    private data class SideChain(
        val shoulder: NormalizedLandmark3D,
        val hip: NormalizedLandmark3D,
        val knee: NormalizedLandmark3D,
        val ankle: NormalizedLandmark3D
    )

    private fun selectBestVisibleSide(frame: PoseFrame): SideChain? {
        if (frame.landmarks.size < 33) return null
        val leftIds = listOf(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE,
            PoseLandmarkIndex.LEFT_ANKLE
        )
        val rightIds = listOf(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_KNEE,
            PoseLandmarkIndex.RIGHT_ANKLE
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
        return SideChain(
            shoulder = frame.landmark(ids[0])!!,
            hip = frame.landmark(ids[1])!!,
            knee = frame.landmark(ids[2])!!,
            ankle = frame.landmark(ids[3])!!
        )
    }
}
