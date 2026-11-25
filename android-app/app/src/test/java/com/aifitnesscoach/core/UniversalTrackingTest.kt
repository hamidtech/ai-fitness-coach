package com.aifitnesscoach.core

import com.aifitnesscoach.core.database.ExerciseSetEntity
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.database.WorkoutSessionEntity
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.exercise.AnalyzerRegistry
import com.aifitnesscoach.core.exercise.analyzers.CalibratedPoseAnalyzer
import com.aifitnesscoach.core.exercise.analyzers.CalibrationStage
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.exercise.definitions.MuscleRegion
import com.aifitnesscoach.core.exercise.definitions.discoveryEntry
import com.aifitnesscoach.core.pose.NormalizedLandmark3D
import com.aifitnesscoach.core.pose.PoseFrame
import com.aifitnesscoach.core.pose.PoseLandmarkIndex
import com.aifitnesscoach.core.pose.TrackingQuality
import com.aifitnesscoach.feature.plan.AdaptiveTraining
import com.aifitnesscoach.feature.plan.TrainingFocus
import com.aifitnesscoach.feature.plan.TrainingOptions
import com.aifitnesscoach.feature.workout.PlannedWorkoutItem
import com.aifitnesscoach.feature.workout.WorkoutUiState
import org.junit.Assert.*
import org.junit.Test

class UniversalTrackingTest {
    private fun pose(time: Long, bent: Boolean): PoseFrame {
        val points = MutableList(33) { NormalizedLandmark3D(0.5f, 0.5f, visibility = 0f) }
        points[PoseLandmarkIndex.LEFT_SHOULDER.index] = NormalizedLandmark3D(0.5f, 0.3f)
        points[PoseLandmarkIndex.LEFT_ELBOW.index] = NormalizedLandmark3D(0.5f, 0.5f)
        points[PoseLandmarkIndex.LEFT_WRIST.index] = if (bent)
            NormalizedLandmark3D(0.7f, 0.5f) else NormalizedLandmark3D(0.5f, 0.7f)
        return PoseFrame(time, points)
    }

    @Test
    fun allBuiltInExercisesHaveATrackerAndDedicatedAnalyzersRemain() {
        ExerciseCatalog.allExercises.forEach { exercise ->
            val analyzer = AnalyzerRegistry.createForExercise(exercise)
            assertEquals(exercise.id, analyzer.exerciseId)
            assertEquals(!AnalyzerRegistry.isSupported(exercise.analyzerId), analyzer is CalibratedPoseAnalyzer)
        }
    }

    @Test
    fun calibrationRequiresVisibleDistinctPosesAndCountsCompleteCycles() {
        val tracker = CalibratedPoseAnalyzer("generic")
        assertFalse(tracker.captureReference(PoseFrame(0, emptyList())))
        assertTrue(tracker.captureReference(pose(100, false)))
        assertEquals(CalibrationStage.END, tracker.calibrationStage)
        assertFalse(tracker.captureReference(pose(200, false)))
        assertTrue(tracker.captureReference(pose(300, true)))
        assertEquals(CalibrationStage.READY, tracker.calibrationStage)
        tracker.processFrame(pose(400, false))
        tracker.processFrame(pose(600, false))
        tracker.processFrame(pose(800, true))
        tracker.processFrame(pose(1000, true))
        tracker.processFrame(pose(1200, false))
        val completed = tracker.processFrame(pose(1400, false))
        assertEquals(1, completed.repCount)
        assertTrue(completed.observations.isEmpty())
        assertEquals(1, tracker.processFrame(pose(1600, false)).repCount)
        tracker.reset()
        assertEquals(CalibrationStage.READY, tracker.calibrationStage)
        assertEquals(0, tracker.processFrame(pose(1800, false)).repCount)
    }

    @Test
    fun lostTrackingAndPauseDoNotCompleteAPartialCycle() {
        val tracker = CalibratedPoseAnalyzer("generic")
        tracker.captureReference(pose(100, false))
        tracker.captureReference(pose(200, true))
        tracker.processFrame(pose(300, false))
        tracker.processFrame(pose(500, false))
        tracker.processFrame(pose(700, true))
        tracker.processFrame(pose(900, true))
        assertEquals(TrackingQuality.LOST, tracker.processFrame(PoseFrame(1000, emptyList())).trackingQuality)
        tracker.processFrame(pose(1100, false))
        assertEquals(0, tracker.processFrame(pose(1300, false)).repCount)
        tracker.interrupt()
        tracker.clearCalibration()
        assertEquals(CalibrationStage.START, tracker.calibrationStage)
        assertEquals(TrackingQuality.GOOD, tracker.processFrame(pose(1500, true)).trackingQuality)
    }

    @Test
    fun specializedHoldCanContinueManuallyWhenCameraIsUnavailable() {
        val plank = requireNotNull(ExerciseCatalog.findById("forearm_plank"))
        val state = WorkoutUiState(plannedItems = listOf(PlannedWorkoutItem(exercise = plank)))
        assertFalse(state.isManualHold)
        assertTrue(state.copy(cameraEnabled = false).isManualHold)
    }

    @Test
    fun adaptivePlanRespectsEquipmentAndDoesNotIncreaseUnsuccessfulTargets() {
        val profile = UserProfile()
        val now = 500_000_000L
        val plan = AdaptiveTraining.suggest(profile, TrainingOptions(TrainingFocus.LOWER, 20), emptyList(), now)
        assertTrue(plan.isNotEmpty())
        assertTrue(plan.all { item ->
            discoveryEntry(item.exercise).equipment.all { it in setOf("body weight", "yoga mat", "wall") }
                && item.targetSets == 2 && item.weightEntry == null
        })
        val item = plan.first { it.exercise.analyzerId != "forearm_plank" }
        val past = WorkoutSessionWithDetails(
            WorkoutSessionEntity(clientRecordId = "test", titleEn = "", titleFa = "", startedAtEpochMs = now - 10000L,
                endedAtEpochMs = now - 1000L, totalDurationMs = 9000L),
            listOf(ExerciseSetEntity(sessionId = 0, exerciseId = item.exercise.id,
                categoryName = item.exercise.category.name, setNumber = 1, targetReps = 9,
                completedReps = 6, holdDurationMs = 0L, restDurationSec = 60)),
            emptyList()
        )
        assertTrue(AdaptiveTraining.recentlyTrained(listOf(past), now).isNotEmpty())
        assertTrue(AdaptiveTraining.recentlyTrained(listOf(past), now + 49L * 60 * 60 * 1000).isEmpty())
        val withHistory = AdaptiveTraining.suggest(profile, TrainingOptions(TrainingFocus.LOWER, 45), listOf(past), now)
        assertEquals(8, withHistory.first { it.exercise.id == item.exercise.id }.targetReps)
    }

    @Test
    fun checkInIsLowVolumeAndUsesDedicatedAnalyzers() {
        val check = AdaptiveTraining.checkIn()
        assertEquals(listOf("bodyweight_squat", "forearm_plank"), check.map { it.exercise.id })
        assertTrue(check.all { it.targetSets == 1 && AnalyzerRegistry.isSupported(it.exercise.analyzerId) })
    }
}
