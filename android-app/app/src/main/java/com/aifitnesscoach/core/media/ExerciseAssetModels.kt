package com.aifitnesscoach.core.media

import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.exercise.ExerciseDefinition

enum class AssetDownloadStatus {
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    PAUSED,
    FAILED,
    NOT_AVAILABLE,
    LICENSE_RESTRICTED
}

enum class AssetSourceType {
    REPDB_FREE_TIER,
    EXERCISE_DB_RESTRICTED,
    USER_IMPORTED,
    NONE
}

enum class AssetMediaType {
    NONE,
    WEBP_IMAGE_PAIR,
    WEBP_SINGLE,
    GIF,
    MP4
}

data class ExerciseMediaManifestEntry(
    val exerciseId: String,
    val sourceType: AssetSourceType,
    val sourceSlug: String?,
    val mediaType: AssetMediaType,
    val primaryUrl: String?,
    val secondaryUrl: String? = null,
    val version: String = "repdb-1.0",
    val licenseName: String = "",
    val attribution: String = "",
    val defaultStatus: AssetDownloadStatus,
    val unavailableReasonEn: String? = null,
    val unavailableReasonFa: String? = null,
    val expectedBytes: Long? = null,
    val resolution: String? = null,
    val licenseUrl: String? = null
)

data class ExerciseAssetUiItem(
    val exercise: ExerciseDefinition,
    val manifest: ExerciseMediaManifestEntry,
    val entity: ExerciseAssetEntity?,
    val effectiveStatus: AssetDownloadStatus,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val errorMessage: String?
)
