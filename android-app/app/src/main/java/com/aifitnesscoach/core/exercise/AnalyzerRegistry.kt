package com.aifitnesscoach.core.exercise

import com.aifitnesscoach.core.exercise.analyzers.BicepsCurlAnalyzer
import com.aifitnesscoach.core.exercise.analyzers.BodyweightSquatAnalyzer
import com.aifitnesscoach.core.exercise.analyzers.ForearmPlankAnalyzer

/**
 * Extensible analyzer registry decoupling exercise selection from CameraX & UI code.
 * Adding a new analyzer only requires registering its factory here.
 */
object AnalyzerRegistry {

    private val factories: Map<String, () -> ExerciseAnalyzer> = mapOf(
        "bodyweight_squat" to { BodyweightSquatAnalyzer() },
        "biceps_curl" to { BicepsCurlAnalyzer(exerciseId = "dumbbell_biceps_curl") },
        "forearm_plank" to { ForearmPlankAnalyzer() },
        "band_biceps_curl" to { BicepsCurlAnalyzer(exerciseId = "band_biceps_curl") }
    )

    fun createForExercise(definition: ExerciseDefinition): ExerciseAnalyzer? {
        val analyzerId = definition.analyzerId ?: return null
        return factories[analyzerId]?.invoke()
    }

    fun isSupported(analyzerId: String?): Boolean =
        analyzerId != null && factories.containsKey(analyzerId)
}
