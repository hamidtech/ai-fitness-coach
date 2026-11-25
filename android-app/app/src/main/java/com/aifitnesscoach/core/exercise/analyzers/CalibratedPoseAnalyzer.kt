package com.aifitnesscoach.core.exercise.analyzers

import com.aifitnesscoach.core.exercise.AnalysisResult
import com.aifitnesscoach.core.exercise.ExerciseAnalyzer
import com.aifitnesscoach.core.exercise.MovementPhase
import com.aifitnesscoach.core.pose.JointAngleMath
import com.aifitnesscoach.core.pose.PoseFrame
import com.aifitnesscoach.core.pose.PoseLandmarkIndex
import com.aifitnesscoach.core.pose.PoseLandmarkIndex.*
import com.aifitnesscoach.core.pose.TrackingQuality
import kotlin.math.abs

enum class CalibrationStage { START, END, READY }

/**
 * General-purpose pose tracking. Two user-demonstrated poses define a visible joint's range.
 * Counts are estimates of repeated movement, with no exercise-specific form judgement.
 */
class CalibratedPoseAnalyzer(override val exerciseId: String) : ExerciseAnalyzer {
    private var startAngles: Map<List<PoseLandmarkIndex>, Float> = emptyMap()
    private var joint: List<PoseLandmarkIndex>? = null
    private var startAngle = 0f
    private var endAngle = 0f
    private var reps = 0
    private var lastTimestamp: Long? = null
    private var phaseSince: Long? = null
    private var phase: MovementPhase = MovementPhase.IDLE
    private var reachedStart = false
    private var reachedEnd = false

    val calibrationStage: CalibrationStage
        get() = when {
            joint != null -> CalibrationStage.READY
            startAngles.isNotEmpty() -> CalibrationStage.END
            else -> CalibrationStage.START
        }

    fun captureReference(frame: PoseFrame): Boolean {
        val angles = joints.mapNotNull { key -> angle(frame, key)?.let { key to it } }.toMap()
        if (angles.isEmpty()) return false
        if (calibrationStage == CalibrationStage.START) {
            startAngles = angles
            return true
        }
        val candidate = startAngles.keys.filter { it in angles }
            .maxByOrNull { abs(angles.getValue(it) - startAngles.getValue(it)) } ?: return false
        val first = startAngles.getValue(candidate)
        val second = angles.getValue(candidate)
        if (abs(second - first) < 25f) return false
        joint = candidate
        startAngle = first
        endAngle = second
        interrupt()
        return true
    }

    fun clearCalibration() {
        joint = null
        startAngles = emptyMap()
        interrupt()
    }

    fun interrupt() {
        phaseSince = null
        reachedStart = false
        reachedEnd = false
        phase = MovementPhase.IDLE
        lastTimestamp = null
    }

    override fun reset() {
        reps = 0
        interrupt()
    }

    override fun processFrame(frame: PoseFrame): AnalysisResult {
        val chosen = joint
        val value = chosen?.let { angle(frame, it) }
        val quality = when {
            frame.landmarks.size < 33 -> TrackingQuality.LOST
            chosen != null && value != null -> TrackingQuality.GOOD
            chosen == null && joints.any { angle(frame, it) != null } -> TrackingQuality.GOOD
            else -> TrackingQuality.LOW_CONFIDENCE
        }
        val timestamp = frame.timestampMs
        if (quality != TrackingQuality.GOOD ||
            lastTimestamp?.let { timestamp <= it || timestamp - it > 500 } == true
        ) interrupt()
        lastTimestamp = timestamp
        if (quality == TrackingQuality.GOOD && value != null) {
            val progress = (value - startAngle) / (endAngle - startAngle)
            val next = when {
                abs(progress) <= 0.25f -> MovementPhase.REFERENCE_START
                abs(progress - 1f) <= 0.25f -> MovementPhase.REFERENCE_END
                else -> MovementPhase.BETWEEN_POSES
            }
            if (next != phase) {
                phase = next
                phaseSince = timestamp
            }
            if (timestamp - (phaseSince ?: timestamp) >= 180L) {
                when (phase) {
                    MovementPhase.REFERENCE_START -> {
                        if (reachedStart && reachedEnd) reps++
                        reachedStart = true
                        reachedEnd = false
                    }
                    MovementPhase.REFERENCE_END -> if (reachedStart) reachedEnd = true
                    else -> Unit
                }
            }
        }
        return AnalysisResult(
            exerciseId = exerciseId,
            phase = if (quality == TrackingQuality.GOOD) phase else MovementPhase.PAUSED_TRACKING_LOST,
            repCount = reps,
            holdDurationMs = 0L,
            trackingQuality = quality,
            primaryAngleDegrees = value,
            secondaryAngleDegrees = null,
            activeFeedbackKey = null,
            observations = emptyList()
        )
    }

    private fun angle(frame: PoseFrame, points: List<PoseLandmarkIndex>): Float? {
        if (!frame.hasRequiredLandmarks(points)) return null
        val aspect = if (frame.sourceWidth > 0 && frame.sourceHeight > 0)
            frame.sourceWidth.toFloat() / frame.sourceHeight else 1f
        val (a, b, c) = points.map { id ->
            requireNotNull(frame.landmark(id)).let { it.copy(x = it.x * aspect) }
        }
        // Coincident landmarks cannot define a meaningful angle.
        if (kotlin.math.hypot(a.x - b.x, a.y - b.y) < 0.015f ||
            kotlin.math.hypot(c.x - b.x, c.y - b.y) < 0.015f) return null
        return JointAngleMath.angleDegrees2D(a, b, c).takeIf(Float::isFinite)
    }

    private companion object {
        val joints = listOf(
            listOf(LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST),
            listOf(RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST),
            listOf(LEFT_HIP, LEFT_SHOULDER, LEFT_ELBOW),
            listOf(RIGHT_HIP, RIGHT_SHOULDER, RIGHT_ELBOW),
            listOf(LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE),
            listOf(RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE),
            listOf(LEFT_HIP, LEFT_KNEE, LEFT_ANKLE),
            listOf(RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE),
            listOf(LEFT_KNEE, LEFT_ANKLE, LEFT_FOOT_INDEX),
            listOf(RIGHT_KNEE, RIGHT_ANKLE, RIGHT_FOOT_INDEX)
        )
    }
}
