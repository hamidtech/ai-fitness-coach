package com.aifitnesscoach.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_assets")
data class ExerciseAssetEntity(
    @PrimaryKey val exerciseId: String,
    val sourceId: String,
    val status: String,
    val version: String,
    val localPrimaryPath: String? = null,
    val localSecondaryPath: String? = null,
    val mediaType: String = "NONE",
    val checksumSha256: String? = null,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val attribution: String = "",
    val licenseName: String = "",
    val errorMessage: String? = null,
    val updatedAtMs: Long = 0L
)

@Entity(tableName = "installed_local_models")
data class InstalledLocalModelEntity(
    @PrimaryKey val modelId: String,
    val localPath: String = "",
    val fileSizeBytes: Long = 0L,
    val sha256: String = "",
    val checksumVerified: Boolean = false,
    val isVerifiedCompatible: Boolean = false,
    val lastTestLoadSuccess: Boolean? = null,
    val lastTestInitMs: Long? = null,
    val lastTestLatencyMs: Long? = null,
    val lastTestMemoryMb: Long? = null,
    val lastTestError: String? = null,
    val status: String = "NOT_DOWNLOADED",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val updatedAtMs: Long = 0L
)

/**
 * External AI provider metadata. API keys are NEVER stored in Room; they live strictly in
 * Android Keystore encrypted SecureCredentialStore keyed by [id].
 */
@Entity(tableName = "ai_provider_configs")
data class AIProviderConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val presetType: String,
    val baseUrl: String,
    val endpointPath: String,
    val modelId: String,
    val customHeadersJson: String = "{}",
    val timeoutSec: Int = 30,
    val maxTokens: Int = 700,
    val temperature: Float = 0.25f,
    val allowInsecureLocalHttp: Boolean = false,
    val enabled: Boolean = true,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

@Entity(tableName = "saved_insights")
data class SavedInsightEntity(
    @PrimaryKey val id: String,
    val timestampMs: Long,
    val analysisType: String,
    val providerType: String,
    val modelIdentifier: String,
    val relatedWorkoutIdsCsv: String = "",
    val sourceRangeLabel: String = "",
    val status: String,
    val summaryTitle: String,
    val bodyText: String,
    val issuesJson: String = "[]",
    val errorMessage: String? = null
)
