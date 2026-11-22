package com.aifitnesscoach.core.pose

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Standard 33-point MediaPipe Pose Landmarker indices.
 */
enum class PoseLandmarkIndex(val index: Int) {
    NOSE(0),
    LEFT_EYE_INNER(1),
    LEFT_EYE(2),
    LEFT_EYE_OUTER(3),
    RIGHT_EYE_INNER(4),
    RIGHT_EYE(5),
    RIGHT_EYE_OUTER(6),
    LEFT_EAR(7),
    RIGHT_EAR(8),
    MOUTH_LEFT(9),
    MOUTH_RIGHT(10),
    LEFT_SHOULDER(11),
    RIGHT_SHOULDER(12),
    LEFT_ELBOW(13),
    RIGHT_ELBOW(14),
    LEFT_WRIST(15),
    RIGHT_WRIST(16),
    LEFT_PINKY(17),
    RIGHT_PINKY(18),
    LEFT_INDEX(19),
    RIGHT_INDEX(20),
    LEFT_THUMB(21),
    RIGHT_THUMB(22),
    LEFT_HIP(23),
    RIGHT_HIP(24),
    LEFT_KNEE(25),
    RIGHT_KNEE(26),
    LEFT_ANKLE(27),
    RIGHT_ANKLE(28),
    LEFT_HEEL(29),
    RIGHT_HEEL(30),
    LEFT_FOOT_INDEX(31),
    RIGHT_FOOT_INDEX(32)
}

data class NormalizedLandmark3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f,
    val presence: Float = 1f
) {
    val confidence: Float get() = min(visibility, presence)
}

data class PoseFrame(
    val timestampMs: Long,
    val landmarks: List<NormalizedLandmark3D>,
    val inferenceTimeMs: Long = 0L,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val isFrontCameraMirrored: Boolean = false
) {
    fun landmark(id: PoseLandmarkIndex): NormalizedLandmark3D? =
        landmarks.getOrNull(id.index)

    fun hasRequiredLandmarks(
        required: List<PoseLandmarkIndex>,
        minConfidence: Float = 0.55f
    ): Boolean {
        if (landmarks.size < 33) return false
        return required.all { idx ->
            val lm = landmarks[idx.index]
            lm.confidence >= minConfidence &&
                lm.x in -0.05f..1.05f &&
                lm.y in -0.05f..1.05f
        }
    }

    fun averageConfidence(indices: List<PoseLandmarkIndex>): Float {
        if (landmarks.size < 33 || indices.isEmpty()) return 0f
        return indices.map { landmarks[it.index].confidence }.average().toFloat()
    }
}

enum class TrackingQuality {
    GOOD,
    LOW_CONFIDENCE,
    LOST
}

object JointAngleMath {

    /**
     * Calculates the 2D planar joint angle (in degrees, 0..180) at vertex [b] formed by [a]-[b]-[c].
     */
    fun angleDegrees2D(
        a: NormalizedLandmark3D,
        b: NormalizedLandmark3D,
        c: NormalizedLandmark3D
    ): Float {
        val bax = a.x - b.x
        val bay = a.y - b.y
        val bcx = c.x - b.x
        val bcy = c.y - b.y

        val magBa = hypot(bax.toDouble(), bay.toDouble())
        val magBc = hypot(bcx.toDouble(), bcy.toDouble())
        if (magBa < 1e-5 || magBc < 1e-5) return 0f

        val dot = (bax * bcx + bay * bcy).toDouble()
        val cosTheta = max(-1.0, min(1.0, dot / (magBa * magBc)))
        return Math.toDegrees(acos(cosTheta)).toFloat()
    }

    /**
     * Vertical angle of segment [a]->[b] relative to screen vertical axis (degrees).
     * 0 deg = straight vertical, 90 deg = horizontal.
     */
    fun segmentAngleFromVerticalDegrees(
        a: NormalizedLandmark3D,
        b: NormalizedLandmark3D
    ): Float {
        val dx = abs(b.x - a.x).toDouble()
        val dy = abs(b.y - a.y).toDouble()
        val mag = hypot(dx, dy)
        if (mag < 1e-5) return 0f
        val cosTheta = max(-1.0, min(1.0, dy / mag))
        return Math.toDegrees(acos(cosTheta)).toFloat()
    }
}
