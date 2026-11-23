package com.aifitnesscoach.core.media

import com.aifitnesscoach.core.database.ExerciseAssetEntity
import java.io.File
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
    OPEN_EXERCISE_BASE,
    CATALOG_FEED,
    EXERCISE_DB_RESTRICTED,
    USER_IMPORTED,
    NONE
}

enum class AssetMediaType {
    NONE,
    WEBP_IMAGE_PAIR,
    WEBP_SINGLE,
    PNG_SINGLE,
    JPEG_SINGLE,
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
    val licenseUrl: String? = null,
    val expectedSha256: String? = null
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


internal fun resolveEffectiveStatus(
    manifest: ExerciseMediaManifestEntry,
    entity: ExerciseAssetEntity?,
    isUsable: (String) -> Boolean = { path -> File(path).let { it.isFile && it.length() > 0 } }
): AssetDownloadStatus {
    val status = AssetDownloadStatus.entries.firstOrNull { it.name == entity?.status } ?: manifest.defaultStatus
    if (entity == null || status != AssetDownloadStatus.DOWNLOADED) return status
    if (entity.sourceId != AssetSourceType.USER_IMPORTED.name && manifest.primaryUrl != null &&
        (entity.version != manifest.version || entity.sourceId != manifest.sourceType.name)
    ) return manifest.defaultStatus
    return if (hasUsableDownloadedMedia(entity, isUsable)) status else manifest.defaultStatus
}

internal fun hasUsableDownloadedMedia(
    entity: ExerciseAssetEntity?,
    isUsable: (String) -> Boolean = { path -> File(path).let { it.isFile && it.length() > 0 } }
): Boolean {
    if (entity == null || entity.status != AssetDownloadStatus.DOWNLOADED.name) return false
    return entity.localPrimaryPath?.let(isUsable) == true &&
        (entity.mediaType != AssetMediaType.WEBP_IMAGE_PAIR.name ||
            entity.localSecondaryPath?.let(isUsable) == true)
}
