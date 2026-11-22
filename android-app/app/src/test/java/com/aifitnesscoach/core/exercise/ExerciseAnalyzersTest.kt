package com.aifitnesscoach.core.exercise

import com.aifitnesscoach.core.exercise.analyzers.BicepsCurlAnalyzer
import com.aifitnesscoach.core.exercise.analyzers.BodyweightSquatAnalyzer
import com.aifitnesscoach.core.exercise.analyzers.ForearmPlankAnalyzer
import com.aifitnesscoach.core.pose.JointAngleMath
import com.aifitnesscoach.core.pose.NormalizedLandmark3D
import com.aifitnesscoach.core.pose.PoseFrame
import com.aifitnesscoach.core.pose.PoseLandmarkIndex
import com.aifitnesscoach.core.pose.TrackingQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseAnalyzersTest {

    private fun blankLandmarks(confidence: Float = 0.9f): MutableList<NormalizedLandmark3D> =
        MutableList(33) { NormalizedLandmark3D(0.5f, 0.5f, 0f, confidence, confidence) }

    private fun squatFrame(timestampMs: Long, kneeYOffset: Float, ankleY: Float = 0.9f, hipY: Float): PoseFrame {
        val list = blankLandmarks()
        list[PoseLandmarkIndex.LEFT_SHOULDER.index] = NormalizedLandmark3D(0.5f, hipY - 0.25f)
        list[PoseLandmarkIndex.LEFT_HIP.index] = NormalizedLandmark3D(0.45f, hipY)
        // The standing pose must have an extended knee (approximately 166 degrees).
        // Keep the bent-knee X coordinate for descending/bottom positions.
        val kneeX = if (kneeYOffset >= 0.19f) 0.50f else 0.58f
        list[PoseLandmarkIndex.LEFT_KNEE.index] = NormalizedLandmark3D(kneeX, hipY + kneeYOffset)
        list[PoseLandmarkIndex.LEFT_ANKLE.index] = NormalizedLandmark3D(0.5f, ankleY)
        return PoseFrame(timestampMs = timestampMs, landmarks = list)
    }

    private fun curlFrame(
        timestampMs: Long,
        wristX: Float,
        wristY: Float,
        elbowX: Float = 0.4f,
        elbowY: Float = 0.55f
    ): PoseFrame {
        val list = blankLandmarks()
        list[PoseLandmarkIndex.LEFT_SHOULDER.index] = NormalizedLandmark3D(0.4f, 0.3f)
        list[PoseLandmarkIndex.LEFT_ELBOW.index] = NormalizedLandmark3D(elbowX, elbowY)
        list[PoseLandmarkIndex.LEFT_WRIST.index] = NormalizedLandmark3D(wristX, wristY)
        return PoseFrame(timestampMs = timestampMs, landmarks = list)
    }

    private fun plankFrame(timestampMs: Long, hipY: Float): PoseFrame {
        val list = blankLandmarks()
        list[PoseLandmarkIndex.LEFT_SHOULDER.index] = NormalizedLandmark3D(0.2f, 0.6f)
        list[PoseLandmarkIndex.LEFT_HIP.index] = NormalizedLandmark3D(0.5f, hipY)
        list[PoseLandmarkIndex.LEFT_ANKLE.index] = NormalizedLandmark3D(0.8f, 0.6f)
        return PoseFrame(timestampMs = timestampMs, landmarks = list)
    }

    @Test
    fun `joint angle math returns 90 degrees for perpendicular vectors`() {
        val a = NormalizedLandmark3D(0f, 1f)
        val b = NormalizedLandmark3D(0f, 0f)
        val c = NormalizedLandmark3D(1f, 0f)
        val angle = JointAngleMath.angleDegrees2D(a, b, c)
        assertEquals(90f, angle, 0.5f)
    }

    @Test
    fun `bodyweight squat counts complete rep and recovers cleanly from tracking loss`() {
        val analyzer = BodyweightSquatAnalyzer()
        val sequence = listOf(
            squatFrame(0L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(200L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(400L, kneeYOffset = 0.14f, hipY = 0.62f),
            squatFrame(600L, kneeYOffset = 0.02f, hipY = 0.78f),
            squatFrame(850L, kneeYOffset = 0.02f, hipY = 0.78f),
            squatFrame(1050L, kneeYOffset = 0.14f, hipY = 0.62f),
            squatFrame(1250L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(1450L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(1650L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(1850L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(2050L, kneeYOffset = 0.20f, hipY = 0.50f)
        )
        var lastResult = analyzer.processFrame(sequence.first())
        sequence.drop(1).forEach { lastResult = analyzer.processFrame(it) }
        assertEquals(1, lastResult.repCount)

        // Tracking loss pauses cleanly without incrementing reps
        val lostFrame = PoseFrame(2250L, blankLandmarks(confidence = 0.1f))
        val lostResult = analyzer.processFrame(lostFrame)
        assertEquals(TrackingQuality.LOW_CONFIDENCE, lostResult.trackingQuality)
        assertEquals(MovementPhase.PAUSED_TRACKING_LOST, lostResult.phase)
        assertEquals(1, lostResult.repCount)

        // Recovery returns to valid tracking without phantom rep increment
        val recovered = analyzer.processFrame(squatFrame(2500L, kneeYOffset = 0.20f, hipY = 0.50f))
        assertEquals(TrackingQuality.GOOD, recovered.trackingQuality)
        assertEquals(1, recovered.repCount)
    }

    @Test
    fun `bodyweight squat warns on shallow depth without counting a rep`() {
        val analyzer = BodyweightSquatAnalyzer()
        val sequence = listOf(
            squatFrame(0L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(200L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(400L, kneeYOffset = 0.14f, hipY = 0.62f),
            squatFrame(600L, kneeYOffset = 0.14f, hipY = 0.62f),
            squatFrame(800L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(1000L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(1200L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(1400L, kneeYOffset = 0.20f, hipY = 0.50f),
            squatFrame(1600L, kneeYOffset = 0.20f, hipY = 0.50f)
        )
        var result = analyzer.processFrame(sequence.first())
        sequence.drop(1).forEach { result = analyzer.processFrame(it) }

        assertEquals(0, result.repCount)
        assertEquals(CoachingMessageKey.SQUAT_SHALLOW, result.activeFeedbackKey)
        assertTrue(result.observations.any { it.key == CoachingMessageKey.SQUAT_SHALLOW })
    }

    @Test
    fun `dumbbell and resistance band biceps curl analyzers count full rep`() {
        listOf("dumbbell_biceps_curl", "band_biceps_curl").forEach { id ->
            val analyzer = BicepsCurlAnalyzer(id)
            val seq = listOf(
                curlFrame(0L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(200L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(400L, wristX = 0.58f, wristY = 0.55f),
                curlFrame(600L, wristX = 0.46f, wristY = 0.34f),
                curlFrame(800L, wristX = 0.46f, wristY = 0.34f),
                curlFrame(1000L, wristX = 0.58f, wristY = 0.55f),
                curlFrame(1200L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(1400L, wristX = 0.42f, wristY = 0.80f)
            )
            var result = analyzer.processFrame(seq.first())
            seq.drop(1).forEach { result = analyzer.processFrame(it) }
            assertEquals("Failed for $id", 1, result.repCount)
        }
    }

    @Test
    fun `biceps curl detects partial rep and upper arm drift for both dumbbell and band`() {
        listOf("dumbbell_biceps_curl", "band_biceps_curl").forEach { id ->
            val partialAnalyzer = BicepsCurlAnalyzer(id)
            val partialSeq = listOf(
                curlFrame(0L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(200L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(400L, wristX = 0.58f, wristY = 0.55f),
                curlFrame(600L, wristX = 0.58f, wristY = 0.55f),
                curlFrame(800L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(1000L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(1200L, wristX = 0.42f, wristY = 0.80f)
            )
            var partialRes = partialAnalyzer.processFrame(partialSeq.first())
            partialSeq.drop(1).forEach { partialRes = partialAnalyzer.processFrame(it) }
            assertEquals("Partial curl should not count for $id", 0, partialRes.repCount)
            assertEquals(CoachingMessageKey.CURL_PARTIAL, partialRes.activeFeedbackKey)
            assertTrue(partialRes.observations.any { it.key == CoachingMessageKey.CURL_PARTIAL })

            val driftAnalyzer = BicepsCurlAnalyzer(id)
            val driftSeq = listOf(
                curlFrame(0L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(200L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(400L, wristX = 0.64f, wristY = 0.52f, elbowX = 0.56f, elbowY = 0.52f),
                curlFrame(600L, wristX = 0.44f, wristY = 0.34f, elbowX = 0.58f, elbowY = 0.50f),
                curlFrame(800L, wristX = 0.44f, wristY = 0.34f, elbowX = 0.58f, elbowY = 0.50f),
                curlFrame(1000L, wristX = 0.62f, wristY = 0.54f),
                curlFrame(1200L, wristX = 0.42f, wristY = 0.80f),
                curlFrame(1400L, wristX = 0.42f, wristY = 0.80f)
            )
            var driftRes = driftAnalyzer.processFrame(driftSeq.first())
            driftSeq.drop(1).forEach { driftRes = driftAnalyzer.processFrame(it) }
            assertTrue(
                "Expected elbow drift observation for $id",
                driftRes.observations.any { it.key == CoachingMessageKey.CURL_ELBOW_DRIFT }
            )
        }
    }

    @Test
    fun `forearm plank accumulates hold time, detects hip sag and high, and freezes timer on tracking loss`() {
        val analyzer = ForearmPlankAnalyzer()
        analyzer.processFrame(plankFrame(0L, hipY = 0.60f))
        val held = analyzer.processFrame(plankFrame(400L, hipY = 0.60f))
        assertEquals(MovementPhase.HOLDING, held.phase)
        assertEquals(400L, held.holdDurationMs)

        val sag = analyzer.processFrame(plankFrame(800L, hipY = 0.74f))
        assertEquals(CoachingMessageKey.PLANK_HIP_SAG, sag.activeFeedbackKey)
        assertTrue(sag.observations.any { it.key == CoachingMessageKey.PLANK_HIP_SAG })

        // Hip pike / high hips after cooldown window
        val high = analyzer.processFrame(plankFrame(5000L, hipY = 0.42f))
        assertEquals(CoachingMessageKey.PLANK_HIP_HIGH, high.activeFeedbackKey)
        assertTrue(high.observations.any { it.key == CoachingMessageKey.PLANK_HIP_HIGH })

        val holdBeforeLoss = high.holdDurationMs
        val lost = analyzer.processFrame(PoseFrame(5400L, blankLandmarks(confidence = 0.1f)))
        assertEquals(MovementPhase.PAUSED_TRACKING_LOST, lost.phase)
        assertEquals(holdBeforeLoss, lost.holdDurationMs)

        // Resuming 4 seconds later must not credit offline time
        val resumedFirst = analyzer.processFrame(plankFrame(9400L, hipY = 0.60f))
        assertEquals(holdBeforeLoss, resumedFirst.holdDurationMs)
        val resumedNext = analyzer.processFrame(plankFrame(9700L, hipY = 0.60f))
        assertEquals(holdBeforeLoss + 300L, resumedNext.holdDurationMs)
    }
}
