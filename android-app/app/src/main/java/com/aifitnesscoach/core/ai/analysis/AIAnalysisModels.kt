package com.aifitnesscoach.core.ai.analysis

import com.aifitnesscoach.core.database.SavedInsightEntity

enum class AnalysisType {
    MANUAL_WORKOUT,
    MANUAL_RANGE,
    SCHEDULED_SUMMARY,
    QA_FOLLOWUP
}

enum class InsightStatus {
    COMPLETED,
    FALLBACK_RULE_BASED,
    FAILED
}

data class InsightIssueItem(
    val severity: String,
    val title: String,
    val detail: String
)

data class AnalyzedExerciseBreakdown(
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val setsCompleted: Int,
    val totalReps: Int,
    val totalHoldSeconds: Long,
    val equipmentNote: String? = null
)

data class AnalyzedWorkoutSummary(
    val sessionId: Long,
    val dateIso: String,
    val title: String,
    val durationMinutes: Int,
    val exercises: List<AnalyzedExerciseBreakdown>,
    val observedFormIssues: List<String>
)

data class AnalyzedHealthMetrics(
    val todaySteps: Long? = null,
    val recentSleepMinutes: Long? = null,
    val recentHeartRateAvgBpm: Long? = null
)

data class AnalyzedAthleteContext(
    val athleteDisplayName: String? = null,
    val fitnessGoal: String,
    val experienceLevel: String,
    val measurementUnit: String
)

/**
 * Versioned normalized payload sent to Local or External AI analysis.
 * Strictly built from recorded workouts, user goal settings, and optional Health Connect
 * metrics when granted — never invents data or transmits API keys.
 */
data class WorkoutAnalysisPayload(
    val schemaVersion: Int = 1,
    val generatedAtEpochMs: Long,
    val analysisType: String,
    val locale: String,
    val sourceRangeLabel: String,
    val athleteContext: AnalyzedAthleteContext,
    val workouts: List<AnalyzedWorkoutSummary>,
    val optionalHealthMetrics: AnalyzedHealthMetrics? = null,
    val userQuestion: String? = null
)

data class AnalysisRequestOutcome(
    val insight: SavedInsightEntity,
    val usedProviderName: String,
    val failedExternalOrLocalReason: String? = null,
    val canFallbackToLocalLlm: Boolean = false,
    val canFallbackToRuleBased: Boolean = false
)
