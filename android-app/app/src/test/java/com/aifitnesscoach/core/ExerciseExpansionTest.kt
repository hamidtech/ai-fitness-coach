package com.aifitnesscoach.core

import com.aifitnesscoach.core.database.ExerciseSetEntity
import com.aifitnesscoach.core.database.WorkoutSessionEntity
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.equipment.WeightEntry
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.equipment.WeightUnit
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.exercise.definitions.MuscleRegion
import com.aifitnesscoach.core.exercise.definitions.discoveryEntry
import com.aifitnesscoach.core.exercise.definitions.equipmentKey
import com.aifitnesscoach.core.exercise.definitions.searchKey
import com.aifitnesscoach.feature.history.summarizeTraining
import com.aifitnesscoach.feature.plan.WeeklyPlan
import com.aifitnesscoach.feature.workout.PlannedWorkoutItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ExerciseExpansionTest {
    @Test
    fun findsByEquipmentMuscleAndBilingualSearch() {
        val band = discoveryEntry(requireNotNull(ExerciseCatalog.findById("band_squat")))
        val resistanceBand = equipmentKey("resistance bands")
        assertTrue(band.matches(searchKey("sqaut"), resistanceBand, MuscleRegion.THIGHS))
        assertTrue(band.matches(searchKey("ران"), resistanceBand, MuscleRegion.THIGHS))
        assertFalse(band.matches("", "dumbbell", MuscleRegion.THIGHS))
        assertFalse(band.matches("", resistanceBand, MuscleRegion.CHEST))
        assertFalse(band.matches("table tennis racket", null, null))
        assertEquals(searchKey("كش مقاومتي"), searchKey("کش مقاومتی"))
    }

    @Test
    fun planRetainsSetRestAndLoadConfigurationAndSkipsBadRecords() {
        val curl = requireNotNull(ExerciseCatalog.findById("dumbbell_biceps_curl"))
        val expected = PlannedWorkoutItem(exercise = curl, targetSets = 4, targetReps = 7, restDurationSec = 90,
            weightEntry = WeightEntry(14f, WeightUnit.LB, WeightLoadMode.TOTAL_LOAD))
        val encoded = WeeklyPlan.encode(listOf(expected)).dropLast(1) + ",{}]"
        val restored = WeeklyPlan.decode(encoded).single()
        assertEquals(expected.copy(uid = restored.uid), restored)
        assertTrue(WeeklyPlan.decode("not JSON").isEmpty())
    }

    @Test
    fun activityCountsConsecutiveActualDatesAndDoesNotMixLoadBases() {
        val today = LocalDate.of(2026, 11, 23)
        val date = { day: Long -> today.plusDays(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val set = ExerciseSetEntity(
            sessionId = 0, exerciseId = "dumbbell_biceps_curl", categoryName = "WEIGHTED_TRAINING",
            setNumber = 1, targetReps = 10, completedReps = 10, holdDurationMs = 0, restDurationSec = 60,
            weightValue = 10f, weightUnit = "KG", weightLoadMode = "PER_DUMBBELL"
        )
        fun session(delta: Long, sets: List<ExerciseSetEntity>) = WorkoutSessionWithDetails(
            WorkoutSessionEntity(clientRecordId = delta.toString(), titleEn = "Workout", titleFa = "تمرین",
                startedAtEpochMs = date(delta), endedAtEpochMs = date(delta) + 3_600_000, totalDurationMs = 3_600_000),
            sets, emptyList()
        )
        val result = summarizeTraining(listOf(
            session(-2, listOf(set.copy(weightLoadMode = "TOTAL_LOAD", weightUnit = "LB", weightValue = 22f))),
            session(-1, listOf(set, set.copy(weightValue = 180f, completedReps = 13))),
            session(1, listOf(set.copy(weightValue = 200f)))
        ), today, ZoneOffset.UTC)
        assertEquals(2, result.sessionCount)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
        assertEquals(60L, result.minutesByDay.getValue(today.minusDays(1)))
        assertEquals(2, result.records.size)
        assertEquals(13.3333, result.records.single { it.mode == WeightLoadMode.PER_DUMBBELL }.estimatedMaxKg, 0.001)
        assertEquals(3, result.setsByRegion[MuscleRegion.ARMS])
    }
}
