package com.aifitnesscoach.feature.exercises

import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.ExerciseMode

internal enum class WorkoutSetupProblem {
    SETS, REPS, HOLD, WEIGHT
}

internal fun workoutSetupProblem(
    mode: ExerciseMode,
    category: ExerciseCategory,
    sets: Int?,
    reps: Int?,
    holdSeconds: Int?,
    weightKg: Float?
): WorkoutSetupProblem? = when {
    sets == null || sets !in 1..20 -> WorkoutSetupProblem.SETS
    mode == ExerciseMode.TIMED_HOLD && (holdSeconds == null || holdSeconds !in 5..600) -> WorkoutSetupProblem.HOLD
    mode != ExerciseMode.TIMED_HOLD && (reps == null || reps !in 1..100) -> WorkoutSetupProblem.REPS
    category == ExerciseCategory.WEIGHTED_TRAINING &&
        (weightKg == null || !weightKg.isFinite() || weightKg < 0) -> WorkoutSetupProblem.WEIGHT
    else -> null
}
