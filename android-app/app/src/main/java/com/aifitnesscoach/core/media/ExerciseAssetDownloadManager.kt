package com.aifitnesscoach.core.media

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import java.util.UUID
import kotlin.coroutines.coroutineContext
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
            ExistingWorkPolicy.KEEP,
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
        if (existing?.status == AssetDownloadStatus.DOWNLOADED.name &&
            existing.localPrimaryPath?.let { File(it).isFile } == true &&
            (manifest.secondaryUrl == null || existing.localSecondaryPath?.let { File(it).isFile } == true ||
                existing.sourceId == AssetSourceType.USER_IMPORTED.name)
        ) return@withContext Result.success()

        val attempt = UUID.randomUUID().toString()
        var received = 0L
        val staged = mutableListOf<File>()
        val working = ExerciseAssetEntity(
            exerciseId = exerciseId,
            sourceId = manifest.sourceType.name,
            status = AssetDownloadStatus.DOWNLOADING.name,
            version = manifest.version,
            mediaType = manifest.mediaType.name,
            bytesDownloaded = 0,
            totalBytes = manifest.expectedBytes ?: 0,
            attribution = manifest.attribution,
            licenseName = manifest.licenseName,
            updatedAtMs = System.currentTimeMillis()
        )
        assetDao.upsert(working)
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        var saved = false
        try {
            suspend fun receive(url: String, role: String): Pair<File, String> {
                val file = ExerciseAssetCatalog.resolveConfinedMediaFile(
                    mediaDir, "${exerciseId}_${attempt}_$role.webp"
                )
                staged.add(file)
                val hash = downloadFile(client, url, file) { bytes ->
                    received += bytes
                    assetDao.upsert(working.copy(bytesDownloaded = received))
                }
                return file to hash
            }

            val (primary, hash) = receive(primaryUrl, "primary")
            val secondary = manifest.secondaryUrl?.let { receive(it, "secondary").first }
            require(manifest.expectedBytes == null || received == manifest.expectedBytes) {
                "Downloaded size differs from the published manifest"
            }
            coroutineContext.ensureActive()
            assetDao.upsert(working.copy(
                status = AssetDownloadStatus.DOWNLOADED.name,
                localPrimaryPath = primary.absolutePath,
                localSecondaryPath = secondary?.absolutePath,
                checksumSha256 = hash,
                bytesDownloaded = received,
                totalBytes = received
            ))
            saved = true
            listOfNotNull(existing?.localPrimaryPath, existing?.localSecondaryPath).forEach { File(it).delete() }
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                assetDao.upsert(working.copy(status = AssetDownloadStatus.PAUSED.name, bytesDownloaded = 0))
            }
            throw error
        } catch (error: Exception) {
            val cancelled = isStopped
            withContext(NonCancellable) {
                assetDao.upsert(working.copy(
                    status = if (cancelled) AssetDownloadStatus.PAUSED.name else AssetDownloadStatus.FAILED.name,
                    bytesDownloaded = 0,
                    errorMessage = if (cancelled) null else applicationContext.getString(R.string.media_status_failed)
                ))
            }
            if (!cancelled && prefs.notificationsEnabled && prefs.notifyDownloads) {
                notifications.notifyDownloadEvent(
                    notificationId = 1500 + (exerciseId.hashCode() and 0xFF),
                    title = applicationContext.getString(R.string.notif_download_failed_title),
                    body = exerciseId.replace('_', ' ')
                )
            }
            return@withContext Result.failure()
        } finally {
            if (!saved) staged.forEach { it.delete() }
        }
        if (prefs.notificationsEnabled && prefs.notifyDownloads) {
            notifications.notifyDownloadEvent(
                notificationId = 1500 + (exerciseId.hashCode() and 0xFF),
                title = applicationContext.getString(R.string.notif_download_complete_title),
                body = exerciseId.replace('_', ' ')
            )
        }
        Result.success()
    }

    private suspend fun downloadFile(
        client: OkHttpClient,
        url: String,
        destFile: File,
        onBytes: suspend (Int) -> Unit
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} downloading media" }
            val body = response.body ?: error("Empty response body")
            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= 2_000_000L) { "Media exceeds the download limit" }
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        onBytes(read)
                    }
                    require(total > 0 && (body.contentLength() < 0 || total == body.contentLength())) {
                        "Incomplete media file"
                    }
                }
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(destFile.absolutePath, options)
        require(options.outMimeType == "image/webp" && options.outWidth > 0 && options.outHeight > 0) {
            "Invalid WebP file"
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
