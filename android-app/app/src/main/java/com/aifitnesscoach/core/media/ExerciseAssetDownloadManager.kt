package com.aifitnesscoach.core.media

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.AppDatabase
import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.notifications.AppNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ExerciseAssetDownloadManager(
    private val context: Context,
    private val assetRepository: ExerciseAssetRepository
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueueDownload(exerciseId: String, wifiOnly: Boolean = false) {
        val manifest = ExerciseAssetCatalog.entryFor(exerciseId)
        if (manifest.defaultStatus != AssetDownloadStatus.AVAILABLE || manifest.primaryUrl == null) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<ExerciseAssetDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(KEY_EXERCISE_ID, exerciseId).build())
            .addTag(TAG_ASSET_DOWNLOAD)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(exerciseId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueueBatchDownload(exerciseIds: List<String>, wifiOnly: Boolean = false) {
        exerciseIds.forEach { enqueueDownload(it, wifiOnly) }
    }

    fun pauseOrCancel(exerciseId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(exerciseId))
    }

    fun cancelAllDownloads() {
        workManager.cancelAllWorkByTag(TAG_ASSET_DOWNLOAD)
    }

    companion object {
        const val KEY_EXERCISE_ID = "exercise_id"
        const val TAG_ASSET_DOWNLOAD = "aifc_asset_download"
        fun uniqueWorkName(exerciseId: String) = "aifc_asset_dl_$exerciseId"
    }
}

class ExerciseAssetDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val exerciseId = inputData.getString(ExerciseAssetDownloadManager.KEY_EXERCISE_ID)
            ?: return@withContext Result.failure()

        val manifest = ExerciseAssetCatalog.entryFor(exerciseId)
        val primaryUrl = manifest.primaryUrl
        if (manifest.defaultStatus != AssetDownloadStatus.AVAILABLE || primaryUrl == null) {
            return@withContext Result.failure()
        }

        val db = AppDatabase.getInstance(applicationContext)
        val assetDao = db.exerciseAssetDao()
        val mediaDir = File(applicationContext.filesDir, "exercise_media").apply { mkdirs() }
        val notifications = AppNotificationManager(applicationContext)
        val prefs = UserPreferencesRepository(applicationContext).preferencesFlow.first()

        val existing = assetDao.getById(exerciseId)
        assetDao.upsert(
            ExerciseAssetEntity(
                exerciseId = exerciseId,
                sourceId = manifest.sourceType.name,
                status = AssetDownloadStatus.DOWNLOADING.name,
                version = manifest.version,
                localPrimaryPath = existing?.localPrimaryPath,
                localSecondaryPath = existing?.localSecondaryPath,
                mediaType = manifest.mediaType.name,
                checksumSha256 = existing?.checksumSha256,
                bytesDownloaded = existing?.bytesDownloaded ?: 0L,
                totalBytes = existing?.totalBytes ?: 0L,
                attribution = manifest.attribution,
                licenseName = manifest.licenseName,
                errorMessage = null,
                updatedAtMs = System.currentTimeMillis()
            )
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

        val result = runCatching {
            val ext = when (manifest.mediaType) {
                AssetMediaType.MP4 -> "mp4"
                AssetMediaType.GIF -> "gif"
                else -> "webp"
            }

            val primaryDest = ExerciseAssetCatalog.resolveConfinedMediaFile(mediaDir, "${exerciseId}_primary.$ext")
            val primaryHash = downloadFile(client, primaryUrl, primaryDest)

            val secondaryDest = manifest.secondaryUrl?.let { secUrl ->
                val secFile = ExerciseAssetCatalog.resolveConfinedMediaFile(mediaDir, "${exerciseId}_secondary.$ext")
                downloadFile(client, secUrl, secFile)
                secFile
            }

            val totalBytes = primaryDest.length() + (secondaryDest?.length() ?: 0L)
            val entity = ExerciseAssetEntity(
                exerciseId = exerciseId,
                sourceId = manifest.sourceType.name,
                status = AssetDownloadStatus.DOWNLOADED.name,
                version = manifest.version,
                localPrimaryPath = primaryDest.absolutePath,
                localSecondaryPath = secondaryDest?.absolutePath,
                mediaType = manifest.mediaType.name,
                checksumSha256 = primaryHash,
                bytesDownloaded = totalBytes,
                totalBytes = totalBytes,
                attribution = manifest.attribution,
                licenseName = manifest.licenseName,
                errorMessage = null,
                updatedAtMs = System.currentTimeMillis()
            )
            assetDao.upsert(entity)
            if (prefs.notificationsEnabled && prefs.notifyDownloads) {
                notifications.notifyDownloadEvent(
                    notificationId = 1500 + (exerciseId.hashCode() and 0xFF),
                    title = applicationContext.getString(R.string.notif_download_complete_title),
                    body = exerciseId.replace('_', ' ')
                )
            }
        }

        return@withContext result.fold(
            onSuccess = { Result.success() },
            onFailure = { err ->
                val wasStopped = isStopped
                assetDao.upsert(
                    ExerciseAssetEntity(
                        exerciseId = exerciseId,
                        sourceId = manifest.sourceType.name,
                        status = if (wasStopped) AssetDownloadStatus.PAUSED.name else AssetDownloadStatus.FAILED.name,
                        version = manifest.version,
                        localPrimaryPath = null,
                        localSecondaryPath = null,
                        mediaType = manifest.mediaType.name,
                        bytesDownloaded = 0L,
                        totalBytes = 0L,
                        attribution = manifest.attribution,
                        licenseName = manifest.licenseName,
                        errorMessage = if (wasStopped) null else (err.localizedMessage ?: "Download failed"),
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
                if (!wasStopped && prefs.notificationsEnabled && prefs.notifyDownloads) {
                    notifications.notifyDownloadEvent(
                        notificationId = 1500 + (exerciseId.hashCode() and 0xFF),
                        title = applicationContext.getString(R.string.notif_download_failed_title),
                        body = err.localizedMessage ?: exerciseId
                    )
                }
                if (wasStopped) Result.success() else Result.failure()
            }
        )
    }

    private fun downloadFile(client: OkHttpClient, url: String, destFile: File): String {
        val partFile = File(destFile.parentFile, "${destFile.name}.part")
        val request = Request.Builder().url(url).get().build()
        val digest = MessageDigest.getInstance("SHA-256")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} downloading media")
            }
            val body = response.body ?: error("Empty response body")
            body.byteStream().use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        if (isStopped) error("Cancelled")
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        total += read
                    }
                    require(total > 0L) { "Empty media file received" }
                }
            }
        }
        if (destFile.exists()) destFile.delete()
        if (!partFile.renameTo(destFile)) {
            partFile.copyTo(destFile, overwrite = true)
            partFile.delete()
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
