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

/**
 * Deterministic Forearm Plank analyzer:
 * - Detects horizontal plank posture
 * - Accumulates hold time only while landmarks are valid and posture is active
 * - Observes approximate shoulder-hip-ankle alignment deviations (hip sag vs hip pike)
 * - Handles tracking interruptions cleanly without advancing hold time
 */
class ForearmPlankAnalyzer : ExerciseAnalyzer {
    override val exerciseId: String = "forearm_plank"

    private var phase: MovementPhase = MovementPhase.IDLE
    private var lastFrameTimestampMs: Long? = null
    private var accumulatedHoldMs: Long = 0L
    private var lastWarningRecordedMs: Long = 0L
    private var activeFeedback: CoachingMessageKey = CoachingMessageKey.READY
    private val observations = mutableListOf<FormObservation>()

    companion object {
        private const val MIN_CONFIDENCE = 0.55f
        private const val MIN_BODY_LINE_ANGLE_DEG = 158f
        private const val MAX_HORIZONTAL_TILT_DEG = 32f
        private const val ALIGNMENT_DEVIATION_WARN_DEG = 16f
        private const val WARNING_COOLDOWN_MS = 4000L
    }

    override fun reset() {
        phase = MovementPhase.IDLE
        lastFrameTimestampMs = null
        accumulatedHoldMs = 0L
        lastWarningRecordedMs = 0L
        activeFeedback = CoachingMessageKey.READY
        observations.clear()
    }

    override fun processFrame(frame: PoseFrame): AnalysisResult {
        val chain = selectBestVisibleSide(frame)
        if (chain == null) {
            phase = MovementPhase.PAUSED_TRACKING_LOST
            lastFrameTimestampMs = null
            return AnalysisResult(
                exerciseId = exerciseId,
                phase = MovementPhase.PAUSED_TRACKING_LOST,
                repCount = 0,
                holdDurationMs = accumulatedHoldMs,
                trackingQuality = if (frame.landmarks.size < 33) TrackingQuality.LOST else TrackingQuality.LOW_CONFIDENCE,
                primaryAngleDegrees = null,
                secondaryAngleDegrees = null,
                activeFeedbackKey = CoachingMessageKey.TRACKING_PAUSED,
                observations = observations.toList(),
                uncertaintyWarningEn = "Shoulder, hip, and ankle landmarks must be visible from the side.",
                uncertaintyWarningFa = "نقاط شانه، لگن و مچ پا باید از نمای جانبی مشخص باشند."
            )
        }

        val (shoulder, hip, ankle) = chain
        val bodyLineAngle = JointAngleMath.angleDegrees2D(shoulder, hip, ankle)
        val verticalTilt = JointAngleMath.segmentAngleFromVerticalDegrees(shoulder, ankle)
        val isHorizontalEnough = verticalTilt >= (90f - MAX_HORIZONTAL_TILT_DEG)

        if (!isHorizontalEnough) {
            phase = MovementPhase.IDLE
            lastFrameTimestampMs = frame.timestampMs
            activeFeedback = CoachingMessageKey.READY
            return AnalysisResult(
                exerciseId = exerciseId,
                phase = MovementPhase.IDLE,
                repCount = 0,
                holdDurationMs = accumulatedHoldMs,
                trackingQuality = TrackingQuality.GOOD,
                primaryAngleDegrees = bodyLineAngle,
                secondaryAngleDegrees = verticalTilt,
                activeFeedbackKey = activeFeedback,
                observations = observations.toList()
            )
        }

        // Accumulate valid plank hold delta (cap single frame delta at 500ms to guard against spikes)
        val prevTs = lastFrameTimestampMs
        if (prevTs != null && phase == MovementPhase.HOLDING) {
            val delta = (frame.timestampMs - prevTs).coerceIn(0L, 500L)
            accumulatedHoldMs += delta
        }
        lastFrameTimestampMs = frame.timestampMs
        phase = MovementPhase.HOLDING

        // Evaluate approximate shoulder-hip-ankle line (in image Y coordinates, larger Y is lower)
        val expectedMidY = (shoulder.y + ankle.y) / 2f
        val hipDeltaY = hip.y - expectedMidY

        if (bodyLineAngle < MIN_BODY_LINE_ANGLE_DEG && (180f - bodyLineAngle) > ALIGNMENT_DEVIATION_WARN_DEG) {
            val feedbackKey = if (hipDeltaY > 0.02f) {
                CoachingMessageKey.PLANK_HIP_SAG
            } else {
                CoachingMessageKey.PLANK_HIP_HIGH
            }
            activeFeedback = feedbackKey
            if (frame.timestampMs - lastWarningRecordedMs >= WARNING_COOLDOWN_MS) {
                lastWarningRecordedMs = frame.timestampMs
                recordObservation(
                    key = feedbackKey,
                    severity = FeedbackSeverity.WARNING,
                    timestampMs = frame.timestampMs,
                    detailEn = if (feedbackKey == CoachingMessageKey.PLANK_HIP_SAG) {
                        "Hips dropped below line (body angle ${bodyLineAngle.toInt()}°)."
                    } else {
                        "Hips raised above line (body angle ${bodyLineAngle.toInt()}°)."
                    },
                    detailFa = if (feedbackKey == CoachingMessageKey.PLANK_HIP_SAG) {
                        "افتادگی لگن مشاهده شد (زاویه خط بدن ${bodyLineAngle.toInt()}°)."
                    } else {
                        "بالا رفتن بیش از حد لگن مشاهده شد (زاویه خط بدن ${bodyLineAngle.toInt()}°)."
                    }
                )
            }
        } else {
            activeFeedback = CoachingMessageKey.PLANK_HOLDING
        }

        return AnalysisResult(
            exerciseId = exerciseId,
            phase = phase,
            repCount = 0,
            holdDurationMs = accumulatedHoldMs,
            trackingQuality = TrackingQuality.GOOD,
            primaryAngleDegrees = bodyLineAngle,
            secondaryAngleDegrees = verticalTilt,
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

    private data class PlankChain(
        val shoulder: NormalizedLandmark3D,
        val hip: NormalizedLandmark3D,
        val ankle: NormalizedLandmark3D
    )

    private fun selectBestVisibleSide(frame: PoseFrame): PlankChain? {
        if (frame.landmarks.size < 33) return null
        val leftIds = listOf(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_ANKLE
        )
        val rightIds = listOf(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_HIP,
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
        return PlankChain(
            shoulder = frame.landmark(ids[0])!!,
            hip = frame.landmark(ids[1])!!,
            ankle = frame.landmark(ids[2])!!
        )
    }
}
