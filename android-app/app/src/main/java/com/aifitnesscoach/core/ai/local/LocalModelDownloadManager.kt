package com.aifitnesscoach.core.ai.local

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
import com.aifitnesscoach.core.database.InstalledLocalModelEntity
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.notifications.AppNotificationManager
import com.aifitnesscoach.core.security.AndroidKeystoreSecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class LocalModelDownloadManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun enqueueDownload(modelId: String, wifiOnly: Boolean = true) {
        val spec = LocalModelCatalog.findById(modelId) ?: return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<LocalModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(KEY_MODEL_ID, modelId).build())
            .addTag(TAG_MODEL_DOWNLOAD)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(spec.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun pauseDownload(modelId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(modelId))
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val TAG_MODEL_DOWNLOAD = "aifc_local_model_dl"
        fun uniqueWorkName(modelId: String) = "aifc_model_dl_$modelId"
    }
}

class LocalModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(LocalModelDownloadManager.KEY_MODEL_ID)
            ?: return@withContext Result.failure()
        val spec = LocalModelCatalog.findById(modelId) ?: return@withContext Result.failure()

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.localModelDao()
        val modelsDir = File(applicationContext.filesDir, "local_models").apply { mkdirs() }
        val secretStore = AndroidKeystoreSecretStore(applicationContext)
        val notifications = AppNotificationManager(applicationContext)
        val prefs = UserPreferencesRepository(applicationContext).preferencesFlow.first()

        val destFile = LocalModelCatalog.resolveConfinedModelFile(modelsDir, spec.fileName)
        val partFile = LocalModelCatalog.resolveConfinedModelFile(modelsDir, "${spec.fileName}.part")
        var resumeFromBytes = if (partFile.exists()) partFile.length() else 0L

        dao.upsert(
            InstalledLocalModelEntity(
                modelId = spec.id,
                localPath = destFile.absolutePath,
                fileSizeBytes = spec.expectedSizeBytes,
                sha256 = spec.sha256,
                checksumVerified = false,
                isVerifiedCompatible = false,
                status = LocalModelDownloadStatus.DOWNLOADING.name,
                downloadedBytes = resumeFromBytes,
                totalBytes = spec.expectedSizeBytes,
                updatedAtMs = System.currentTimeMillis()
            )
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val outcome = runCatching {
            val reqBuilder = Request.Builder().url(spec.downloadUrl).get()
            if (resumeFromBytes > 0L && resumeFromBytes < spec.expectedSizeBytes) {
                reqBuilder.addHeader("Range", "bytes=$resumeFromBytes-")
            } else if (resumeFromBytes >= spec.expectedSizeBytes) {
                partFile.delete()
                resumeFromBytes = 0L
            }

            if (spec.requiresHfToken) {
                val hfToken = secretStore.getSecret(AndroidKeystoreSecretStore.HUGGING_FACE_TOKEN_ALIAS)
                if (!hfToken.isNullOrBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $hfToken")
                }
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    error("Access denied (HTTP ${response.code}). Accept the Gemma license on Hugging Face and enter your Hugging Face token.")
                }
                if (!response.isSuccessful && response.code != 206) {
                    error("HTTP ${response.code} downloading model")
                }
                val isAppending = response.code == 206 && resumeFromBytes > 0L
                if (!isAppending && resumeFromBytes > 0L) {
                    partFile.delete()
                    resumeFromBytes = 0L
                }

                val body = response.body ?: error("Empty model response body")
                var totalDownloaded = resumeFromBytes
                var lastPersistBytes = totalDownloaded

                FileOutputStream(partFile, isAppending).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (isStopped) error("Paused by user")
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            totalDownloaded += read
                            if (totalDownloaded - lastPersistBytes >= 512 * 1024) {
                                lastPersistBytes = totalDownloaded
                                dao.upsert(
                                    InstalledLocalModelEntity(
                                        modelId = spec.id,
                                        localPath = destFile.absolutePath,
                                        fileSizeBytes = spec.expectedSizeBytes,
                                        sha256 = spec.sha256,
                                        status = LocalModelDownloadStatus.DOWNLOADING.name,
                                        downloadedBytes = totalDownloaded,
                                        totalBytes = spec.expectedSizeBytes,
                                        updatedAtMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // SHA-256 verification before completing
            dao.upsert(
                InstalledLocalModelEntity(
                    modelId = spec.id,
                    localPath = destFile.absolutePath,
                    fileSizeBytes = partFile.length(),
                    sha256 = spec.sha256,
                    status = LocalModelDownloadStatus.VERIFYING.name,
                    downloadedBytes = partFile.length(),
                    totalBytes = spec.expectedSizeBytes,
                    updatedAtMs = System.currentTimeMillis()
                )
            )

            val shaOk = LocalModelCatalog.verifySha256(partFile, spec.sha256)
            if (!shaOk) {
                partFile.delete()
                error("SHA-256 checksum mismatch. File was deleted for safety.")
            }

            if (destFile.exists()) destFile.delete()
            if (!partFile.renameTo(destFile)) {
                partFile.copyTo(destFile, overwrite = true)
                partFile.delete()
            }

            dao.upsert(
                InstalledLocalModelEntity(
                    modelId = spec.id,
                    localPath = destFile.absolutePath,
                    fileSizeBytes = destFile.length(),
                    sha256 = spec.sha256,
                    checksumVerified = true,
                    isVerifiedCompatible = false,
                    status = LocalModelDownloadStatus.DOWNLOADED.name,
                    downloadedBytes = destFile.length(),
                    totalBytes = spec.expectedSizeBytes,
                    updatedAtMs = System.currentTimeMillis()
                )
            )

            if (prefs.notificationsEnabled && prefs.notifyDownloads) {
                notifications.notifyDownloadEvent(
                    notificationId = 1600 + (spec.id.hashCode() and 0xFF),
                    title = applicationContext.getString(R.string.notif_download_complete_title),
                    body = "${spec.displayName} — ${applicationContext.getString(R.string.local_model_run_test_prompt)}"
                )
            }
        }

        return@withContext outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { err ->
                val paused = isStopped
                dao.upsert(
                    InstalledLocalModelEntity(
                        modelId = spec.id,
                        localPath = destFile.absolutePath,
                        fileSizeBytes = spec.expectedSizeBytes,
                        sha256 = spec.sha256,
                        status = if (paused) LocalModelDownloadStatus.PAUSED.name else LocalModelDownloadStatus.FAILED.name,
                        downloadedBytes = if (partFile.exists()) partFile.length() else 0L,
                        totalBytes = spec.expectedSizeBytes,
                        lastTestError = if (paused) null else (err.localizedMessage ?: "Download failed"),
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
                if (!paused && prefs.notificationsEnabled && prefs.notifyDownloads) {
                    notifications.notifyDownloadEvent(
                        notificationId = 1600 + (spec.id.hashCode() and 0xFF),
                        title = applicationContext.getString(R.string.notif_download_failed_title),
                        body = err.localizedMessage ?: spec.displayName
                    )
                }
                if (paused) Result.success() else Result.failure()
            }
        )
    }
}
