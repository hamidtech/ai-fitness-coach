package com.aifitnesscoach.core.database

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import java.io.File

data class ExportedWorkoutBundle(
    val schemaVersion: Int = 1,
    val exportedAtEpochMs: Long,
    val athleteSummary: ExportedAthleteSummary,
    val sessions: List<ExportedSessionRecord>
)

data class ExportedAthleteSummary(
    val displayName: String,
    val gender: String,
    val fitnessGoal: String,
    val experienceLevel: String,
    val measurementUnit: String
)

data class ExportedSessionRecord(
    val id: Long,
    val titleEn: String,
    val titleFa: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val totalDurationMs: Long,
    val notes: String,
    val sets: List<ExerciseSetEntity>,
    val observations: List<FormObservationEntity>
)

/**
 * Exports local workout history to JSON without ever reading or including API keys,
 * tokens, or external credential material.
 */
object WorkoutExportManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun buildExportJson(
        preferences: UserPreferences,
        sessions: List<WorkoutSessionWithDetails>,
        exportedAtMs: Long = System.currentTimeMillis()
    ): String {
        val bundle = ExportedWorkoutBundle(
            exportedAtEpochMs = exportedAtMs,
            athleteSummary = ExportedAthleteSummary(
                displayName = preferences.profile.name,
                gender = preferences.profile.gender.name,
                fitnessGoal = preferences.profile.fitnessGoal.name,
                experienceLevel = preferences.profile.experienceLevel.name,
                measurementUnit = preferences.measurementUnit.name
            ),
            sessions = sessions.map { item ->
                ExportedSessionRecord(
                    id = item.session.id,
                    titleEn = item.session.titleEn,
                    titleFa = item.session.titleFa,
                    startedAtEpochMs = item.session.startedAtEpochMs,
                    endedAtEpochMs = item.session.endedAtEpochMs,
                    totalDurationMs = item.session.totalDurationMs,
                    notes = item.session.notes,
                    sets = item.sets,
                    observations = item.observations
                )
            }
        )
        return gson.toJson(bundle)
    }

    fun writeAndCreateShareIntent(
        context: Context,
        preferences: UserPreferences,
        sessions: List<WorkoutSessionWithDetails>
    ): Intent {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "ai_fitness_coach_workouts_${System.currentTimeMillis()}.json")
        file.writeText(buildExportJson(preferences, sessions), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AI Fitness Coach Workout History")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
