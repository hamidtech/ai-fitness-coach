package com.aifitnesscoach.feature.history

import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.equipment.WeightUnit
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.exercise.definitions.MuscleRegion
import com.aifitnesscoach.core.exercise.definitions.discoveryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class LiftRecord(
    val exerciseId: String,
    val mode: WeightLoadMode,
    val loadKg: Double,
    val reps: Int
) {
    val estimatedMaxKg: Double get() = if (reps == 1) loadKg else loadKg * (1 + reps / 30.0)
}

internal data class TrainingProgress(
    val minutesByDay: Map<LocalDate, Long>,
    val sessionCount: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val setsByRegion: Map<MuscleRegion, Int>,
    val records: List<LiftRecord>
)

internal fun summarizeTraining(
    sessions: List<WorkoutSessionWithDetails>,
    today: LocalDate,
    zone: ZoneId
): TrainingProgress {
    val byDay = sessions.groupBy { Instant.ofEpochMilli(it.session.startedAtEpochMs).atZone(zone).toLocalDate() }
        .filterKeys { it <= today }
    val activeDays = byDay.keys.sorted()
    var runLength = 0
    var longest = 0
    var lastDay: LocalDate? = null
    for (day in activeDays) {
        runLength = if (lastDay?.plusDays(1) == day) runLength + 1 else 1
        longest = maxOf(longest, runLength)
        lastDay = day
    }
    val activeStreak = if (lastDay == today || lastDay == today.minusDays(1)) runLength else 0
    val records = byDay.values.asSequence().flatten().flatMap { it.sets.asSequence() }
        .mapNotNull { set ->
            val load = set.weightValue?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            if (set.completedReps !in 1..12 || set.holdDurationMs > 0) return@mapNotNull null
            val unit = WeightUnit.entries.firstOrNull { it.name == set.weightUnit } ?: return@mapNotNull null
            val mode = WeightLoadMode.entries.firstOrNull { it.name == set.weightLoadMode } ?: return@mapNotNull null
            LiftRecord(set.exerciseId, mode, if (unit == WeightUnit.LB) load * 0.45359237 else load.toDouble(),
                set.completedReps)
        }
        .groupBy { it.exerciseId to it.mode }
        .values.mapNotNull { sets -> sets.maxByOrNull { it.estimatedMaxKg } }
        .sortedBy { it.exerciseId }

    val recentSets = byDay.filterKeys { it >= today.minusDays(6) }.values.asSequence()
        .flatten().flatMap { it.sets.asSequence() }
        .filter { it.completedReps > 0 || it.holdDurationMs > 0 }
        .groupingBy { it.exerciseId }.eachCount()
    val regionCounts = mutableMapOf<MuscleRegion, Int>()
    recentSets.forEach { (exerciseId, count) ->
        ExerciseCatalog.findById(exerciseId)?.let { exercise ->
            discoveryEntry(exercise).muscleRegions.forEach { region ->
                regionCounts.merge(region, count, Int::plus)
            }
        }
    }

    return TrainingProgress(
        minutesByDay = byDay.mapValues { (_, entries) ->
            entries.sumOf { it.session.totalDurationMs.coerceAtLeast(0L) }.div(60_000L).coerceAtLeast(1L)
        },
        sessionCount = byDay.values.sumOf { it.size },
        currentStreak = activeStreak,
        longestStreak = longest,
        setsByRegion = regionCounts,
        records = records
    )
}
