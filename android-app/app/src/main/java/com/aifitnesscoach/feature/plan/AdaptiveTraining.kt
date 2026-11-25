package com.aifitnesscoach.feature.plan

import com.aifitnesscoach.core.database.ExperienceLevel
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.equipment.WeightEntry
import com.aifitnesscoach.core.equipment.WeightUnit
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.exercise.ExerciseMode
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.exercise.definitions.MuscleRegion
import com.aifitnesscoach.core.exercise.definitions.discoveryEntry
import com.aifitnesscoach.feature.workout.PlannedWorkoutItem

internal enum class TrainingFocus { FULL_BODY, UPPER, LOWER, CORE, MOBILITY }

internal data class TrainingOptions(
    val focus: TrainingFocus,
    val minutes: Int,
    val dumbbells: Boolean = false,
    val bands: Boolean = false
)

internal object AdaptiveTraining {
    const val CHECK_IN_TAG = "fitness_check_in_v1"
    private const val RECENT_MS = 48L * 60 * 60 * 1000
    private val upper = setOf(MuscleRegion.CHEST, MuscleRegion.BACK, MuscleRegion.SHOULDERS, MuscleRegion.ARMS)
    private val lower = setOf(MuscleRegion.HIPS, MuscleRegion.THIGHS, MuscleRegion.LOWER_LEGS)

    private val candidates = listOf(
        "bodyweight_squat", "push_up", "glute_bridge", "bird_dog", "forearm_plank",
        "standing_calf_raise", "reverse_lunge", "dead_bug", "wall_sit",
        "EX-knee-push-ups-01KZ3V3DYR43D2FTV12ZVQHG11",
        "EX-wall-push-up-4142F3V3M07GQC9F8SH26B4VKX",
        "dumbbell_bent_over_row", "goblet_squat", "dumbbell_biceps_curl",
        "dumbbell_shoulder_press", "dumbbell_romanian_deadlift", "band_pull_apart",
        "band_biceps_curl", "band_squat"
    )
    private val mobility = listOf("bird_dog", "dead_bug", "aifc-child-pose", "aifc-seated-forward-fold", "EX-triceps-stretch-01KZ3V3DYMQ0JFE1ZDQWY74XF5",
        "EX-upper-back-stretch-01KZ3V3DYY2J67YNXR955N44K1",
        "EX-standing-quadriceps-stretch-0NMW4RJFR5VXQVF5E1T2Z4PMN7")

    fun defaultFocus(profile: UserProfile): TrainingFocus = when (profile.fitnessGoal) {
        FitnessGoal.MOBILITY -> TrainingFocus.MOBILITY
        FitnessGoal.CORE_STABILITY -> TrainingFocus.CORE
        else -> TrainingFocus.FULL_BODY
    }

    fun recentlyTrained(sessions: List<WorkoutSessionWithDetails>, nowMs: Long): Set<MuscleRegion> =
        sessions.asSequence().filter { it.session.endedAtEpochMs in (nowMs - RECENT_MS)..nowMs }
            .flatMap { it.sets.asSequence() }
            .filter { it.completedReps > 0 || it.holdDurationMs > 0 }
            .mapNotNull { ExerciseCatalog.findById(it.exerciseId) }
            .flatMap { discoveryEntry(it).muscleRegions.asSequence() }.toSet()

    fun suggest(
        profile: UserProfile,
        options: TrainingOptions,
        sessions: List<WorkoutSessionWithDetails>,
        nowMs: Long
    ): List<PlannedWorkoutItem> {
        val equipment = buildSet {
            addAll(listOf("body weight", "yoga mat", "wall"))
            if (options.dumbbells) add("dumbbell")
            if (options.bands) add("resistance band")
        }
        val eligible = (if (options.focus == TrainingFocus.MOBILITY) mobility else candidates)
            .mapNotNull(ExerciseCatalog::findById).map(::discoveryEntry)
            .filter { exercise -> exercise.equipment.all { it in equipment } && when (options.focus) {
                TrainingFocus.UPPER -> exercise.muscleRegions.any { it in upper }
                TrainingFocus.LOWER -> exercise.muscleRegions.any { it in lower }
                TrainingFocus.CORE -> MuscleRegion.CORE in exercise.muscleRegions
                else -> true
            } }
        val recent = recentlyTrained(sessions, nowMs)
        val past = sessions.filter { it.session.notes != CHECK_IN_TAG }.sortedByDescending { it.session.endedAtEpochMs }
        val chosen = mutableListOf<com.aifitnesscoach.core.exercise.definitions.DiscoverableExercise>()
        val seen = mutableSetOf<MuscleRegion>()
        val available = eligible.toMutableList()
        val baselineSets = if (profile.experienceLevel == ExperienceLevel.BEGINNER || options.minutes <= 15) 2 else 3
        // Reserve approximately 2 minutes per set, including rest and setup.
        val maxItems = (options.minutes.coerceIn(10, 45) / (baselineSets * 2)).coerceIn(2, 6)
        repeat(minOf(maxItems, available.size)) {
            val next = available.minByOrNull { entry ->
                entry.muscleRegions.count { it in recent } * 2 +
                    entry.muscleRegions.count { it in seen } * 3 +
                    if (profile.experienceLevel == ExperienceLevel.BEGINNER && entry.exercise.id == "push_up") 2 else 0
            } ?: return@repeat
            chosen.add(next)
            seen.addAll(next.muscleRegions)
            available.remove(next)
        }
        return chosen.map { entry ->
            val exercise = entry.exercise
            val prior = past.map { workout -> workout.sets.filter { it.exerciseId == exercise.id } }
                .filter { it.isNotEmpty() }.take(2)
            val fallbackReps = when (profile.fitnessGoal) {
                FitnessGoal.STRENGTH -> 6
                FitnessGoal.MUSCLE_BUILDING -> 10
                else -> 8
            }
            val latest = prior.firstOrNull()
            val latestTarget = latest?.firstOrNull()?.targetReps?.coerceIn(4, 15) ?: fallbackReps
            val reps = when {
                latest != null && latest.any { it.completedReps < it.targetReps } -> (latestTarget - 1).coerceAtLeast(4)
                prior.size == 2 && prior.all { sets -> sets.all { it.targetReps > 0 && it.completedReps >= it.targetReps } } ->
                    (latestTarget + 1).coerceAtMost(15)
                else -> latestTarget
            }
            val item = PlannedWorkoutItem(exercise = exercise, targetSets = baselineSets,
                targetReps = reps, targetHoldSec = 20, restDurationSec = 60)
            val load = latest?.firstOrNull()?.let { set ->
                val value = set.weightValue?.takeIf { it.isFinite() && it >= 0f } ?: return@let null
                val unit = WeightUnit.entries.firstOrNull { it.name == set.weightUnit } ?: return@let null
                val mode = WeightLoadMode.entries.firstOrNull { it.name == set.weightLoadMode } ?: return@let null
                WeightEntry(value, unit, mode)
            }
            item.copy(weightEntry = item.weightEntry?.let { load ?: it.copy(value = 0f) },
                targetReps = if (exercise.exerciseMode == ExerciseMode.TIMED_HOLD) 1 else reps)
        }
    }

    fun checkIn(): List<PlannedWorkoutItem> =
        listOf("bodyweight_squat", "forearm_plank").mapNotNull(ExerciseCatalog::findById)
            .map { PlannedWorkoutItem(exercise = it, targetSets = 1, targetReps = 5,
                targetHoldSec = 15, restDurationSec = 60) }
}
