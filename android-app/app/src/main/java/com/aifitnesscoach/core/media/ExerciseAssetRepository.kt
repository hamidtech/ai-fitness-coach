package com.aifitnesscoach.core.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.aifitnesscoach.core.database.ExerciseAssetDao
import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class ExerciseAssetRepository(
    private val context: Context,
    private val assetDao: ExerciseAssetDao
) {
    val mediaDir: File = File(context.filesDir, "exercise_media").apply { mkdirs() }

    fun observeAllAssetItems(): Flow<List<ExerciseAssetUiItem>> {
        return assetDao.observeAll().map { entities ->
            val entityById = entities.associateBy { it.exerciseId }
            ExerciseCatalog.allExercises.map { ex ->
                val manifest = ExerciseAssetCatalog.entryFor(ex.id)
                val entity = entityById[ex.id]
                val status = resolveEffectiveStatus(manifest, entity)
                ExerciseAssetUiItem(
                    exercise = ex,
                    manifest = manifest,
                    entity = entity,
                    effectiveStatus = status,
                    bytesDownloaded = entity?.bytesDownloaded ?: 0L,
                    totalBytes = entity?.totalBytes ?: 0L,
                    errorMessage = entity?.errorMessage
                )
            }
        }
    }

    fun observeAssetByExerciseId(exerciseId: String): Flow<ExerciseAssetEntity?> =
        assetDao.observeById(exerciseId)

    suspend fun getAssetByExerciseId(exerciseId: String): ExerciseAssetEntity? =
        assetDao.getById(exerciseId)

    fun resolveEffectiveStatus(
        manifest: ExerciseMediaManifestEntry,
        entity: ExerciseAssetEntity?
    ): AssetDownloadStatus {
        if (entity == null) return manifest.defaultStatus
        val parsed = runCatching { AssetDownloadStatus.valueOf(entity.status) }
            .getOrDefault(manifest.defaultStatus)
        if (parsed == AssetDownloadStatus.DOWNLOADED) {
            val primaryExists = !entity.localPrimaryPath.isNullOrBlank() && File(entity.localPrimaryPath).exists()
            if (!primaryExists) return manifest.defaultStatus
        }
        return parsed
    }

    suspend fun reconcileOrphanedDownloads() = withContext(Dispatchers.IO) {
        val current = assetDao.getAll()
        current.forEach { entity ->
            if (entity.status == AssetDownloadStatus.DOWNLOADING.name) {
                assetDao.upsert(
                    entity.copy(
                        status = AssetDownloadStatus.PAUSED.name,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            } else if (entity.status == AssetDownloadStatus.DOWNLOADED.name) {
                val exists = !entity.localPrimaryPath.isNullOrBlank() && File(entity.localPrimaryPath).exists()
                if (!exists) {
                    assetDao.deleteById(entity.exerciseId)
                }
            }
        }
    }

    suspend fun importLocalMediaForExercise(
        exerciseId: String,
        sourceUri: Uri
    ): Result<ExerciseAssetEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(sourceUri)?.lowercase().orEmpty()
            val ext = when {
                mimeType.contains("mp4") || mimeType.startsWith("video/") -> "mp4"
                mimeType.contains("gif") -> "gif"
                mimeType.contains("png") -> "png"
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                mimeType.contains("webp") -> "webp"
                else -> MimeTypeMap.getFileExtensionFromUrl(sourceUri.toString())
                    ?.lowercase()
                    ?.takeIf { it in setOf("mp4", "gif", "webp", "png", "jpg", "jpeg") }
                    ?: error("Unsupported media type. Select MP4, GIF, WebP, PNG, or JPG.")
            }

            val mediaType = when (ext) {
                "mp4" -> AssetMediaType.MP4
                "gif" -> AssetMediaType.GIF
                else -> AssetMediaType.WEBP_SINGLE
            }

            val destFile = ExerciseAssetCatalog.resolveConfinedMediaFile(
                mediaDir,
                "${exerciseId}_user_import.$ext"
            )
            val partFile = ExerciseAssetCatalog.resolveConfinedMediaFile(
                mediaDir,
                "${exerciseId}_user_import.$ext.part"
            )

            val digest = MessageDigest.getInstance("SHA-256")
            var totalCopied = 0L
            resolver.openInputStream(sourceUri)?.use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        totalCopied += read
                    }
                }
            } ?: error("Unable to open selected file.")

            require(totalCopied > 0L) { "Selected media file is empty." }
            if (destFile.exists()) destFile.delete()
            if (!partFile.renameTo(destFile)) {
                partFile.copyTo(destFile, overwrite = true)
                partFile.delete()
            }

            val shaHex = digest.digest().joinToString("") { "%02x".format(it) }
            val entity = ExerciseAssetEntity(
                exerciseId = exerciseId,
                sourceId = AssetSourceType.USER_IMPORTED.name,
                status = AssetDownloadStatus.DOWNLOADED.name,
                version = "user-1",
                localPrimaryPath = destFile.absolutePath,
                localSecondaryPath = null,
                mediaType = mediaType.name,
                checksumSha256 = shaHex,
                bytesDownloaded = totalCopied,
                totalBytes = totalCopied,
                attribution = "User-imported local file",
                licenseName = "Local user media",
                errorMessage = null,
                updatedAtMs = System.currentTimeMillis()
            )
            assetDao.upsert(entity)
            entity
        }
    }

    suspend fun deleteAsset(exerciseId: String) = withContext(Dispatchers.IO) {
        val existing = assetDao.getById(exerciseId)
        listOfNotNull(existing?.localPrimaryPath, existing?.localSecondaryPath).forEach { path ->
            runCatching { File(path).delete() }
        }
        mediaDir.listFiles { f -> f.name.startsWith("${exerciseId}_") }?.forEach { it.delete() }
        assetDao.deleteById(exerciseId)
    }

    suspend fun deleteAllAssets() = withContext(Dispatchers.IO) {
        mediaDir.listFiles()?.forEach { it.deleteRecursively() }
        assetDao.deleteAll()
    }

    suspend fun computeStorageBytes(): Long = withContext(Dispatchers.IO) {
        mediaDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }
}
