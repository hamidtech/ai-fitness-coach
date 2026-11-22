package com.aifitnesscoach.core.ai.local

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.aifitnesscoach.core.database.InstalledLocalModelEntity
import com.aifitnesscoach.core.database.LocalModelDao
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.security.SecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class LocalModelRepository(
    private val context: Context,
    private val localModelDao: LocalModelDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val secretStore: SecretStore,
    private val inferenceEngine: LocalLlmRuntime
) {
    val modelsDir: File = File(context.filesDir, "local_models").apply { mkdirs() }

    fun getDeviceResources(): DeviceResourceSummary {
        val stat = StatFs(modelsDir.absolutePath)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).toInt()
        val availRamMb = (memInfo.availMem / (1024L * 1024L)).toInt()
        return DeviceResourceSummary(
            availableStorageBytes = freeBytes,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty()
        )
    }

    fun observeModelStates(): Flow<List<LocalModelUiState>> {
        return combine(
            localModelDao.observeAll(),
            preferencesRepository.preferencesFlow
        ) { installedList, prefs ->
            val installedMap = installedList.associateBy { it.modelId }
            val resources = getDeviceResources()
            LocalModelCatalog.models.map { spec ->
                val entity = installedMap[spec.id]
                val status = resolveStatus(spec, entity)
                LocalModelUiState(
                    spec = spec,
                    entity = entity,
                    status = status,
                    isActiveSelection = prefs.activeLocalModelId == spec.id,
                    hasEnoughStorage = status == LocalModelDownloadStatus.DOWNLOADED ||
                        status == LocalModelDownloadStatus.TEST_PASSED ||
                        resources.availableStorageBytes >= spec.requiredFreeStorageBytes,
                    hasRecommendedRam = resources.totalRamMb == 0 || resources.totalRamMb >= spec.recommendedRamMb
                )
            }
        }
    }

    fun resolveStatus(
        spec: LocalModelSpec,
        entity: InstalledLocalModelEntity?
    ): LocalModelDownloadStatus {
        if (entity == null) {
            val partFile = File(modelsDir, "${spec.fileName}.part")
            return if (partFile.exists() && partFile.length() > 0L) {
                LocalModelDownloadStatus.PAUSED
            } else {
                LocalModelDownloadStatus.NOT_DOWNLOADED
            }
        }
        val parsed = runCatching { LocalModelDownloadStatus.valueOf(entity.status) }
            .getOrDefault(LocalModelDownloadStatus.NOT_DOWNLOADED)

        if (parsed in setOf(
                LocalModelDownloadStatus.DOWNLOADED,
                LocalModelDownloadStatus.TEST_PASSED,
                LocalModelDownloadStatus.TEST_FAILED
            )
        ) {
            val exists = entity.localPath.isNotBlank() && File(entity.localPath).exists()
            if (!exists) return LocalModelDownloadStatus.NOT_DOWNLOADED
        }
        return parsed
    }

    suspend fun reconcileOnStartup() = withContext(Dispatchers.IO) {
        localModelDao.getAll().forEach { entity ->
            if (entity.status == LocalModelDownloadStatus.DOWNLOADING.name ||
                entity.status == LocalModelDownloadStatus.VERIFYING.name
            ) {
                localModelDao.upsert(
                    entity.copy(
                        status = LocalModelDownloadStatus.PAUSED.name,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            } else if (entity.localPath.isNotBlank() && !File(entity.localPath).exists() &&
                entity.status in setOf(
                    LocalModelDownloadStatus.DOWNLOADED.name,
                    LocalModelDownloadStatus.TEST_PASSED.name,
                    LocalModelDownloadStatus.TEST_FAILED.name
                )
            ) {
                localModelDao.deleteById(entity.modelId)
            }
        }
    }

    suspend fun getVerifiedExecutableModel(modelId: String?): InstalledLocalModelEntity? {
        if (modelId.isNullOrBlank()) return null
        val entity = localModelDao.getById(modelId) ?: return null
        if (!entity.checksumVerified || !entity.isVerifiedCompatible) return null
        if (entity.localPath.isBlank() || !File(entity.localPath).exists()) return null
        return entity
    }

    suspend fun runCompatibilityTest(modelId: String): LocalModelTestReport {
        val spec = LocalModelCatalog.findById(modelId)
            ?: return LocalModelTestReport(false, 0, 0, 0, "CPU", "", "Unknown model")
        val entity = localModelDao.getById(modelId)
            ?: return LocalModelTestReport(false, 0, 0, 0, "CPU", "", "Model is not downloaded")
        if (!entity.checksumVerified) {
            return LocalModelTestReport(false, 0, 0, 0, "CPU", "", "SHA-256 checksum verification has not passed")
        }

        val preferGpu = spec.backendHint.contains("GPU", ignoreCase = true)
        val report = inferenceEngine.testModel(entity.localPath, preferGpu = preferGpu)
        localModelDao.upsert(
            entity.copy(
                isVerifiedCompatible = report.loadSuccess,
                lastTestLoadSuccess = report.loadSuccess,
                lastTestInitMs = report.initTimeMs,
                lastTestLatencyMs = report.latencyMs,
                lastTestMemoryMb = report.memoryDeltaMb,
                lastTestError = report.errorMessage,
                status = if (report.loadSuccess) {
                    LocalModelDownloadStatus.TEST_PASSED.name
                } else {
                    LocalModelDownloadStatus.TEST_FAILED.name
                },
                updatedAtMs = System.currentTimeMillis()
            )
        )
        return report
    }

    suspend fun selectActiveModel(modelId: String): Result<Unit> {
        val entity = localModelDao.getById(modelId)
            ?: return Result.failure(IllegalStateException("Download the model first."))
        if (!entity.checksumVerified || !entity.isVerifiedCompatible) {
            return Result.failure(IllegalStateException("Run 'Test Model' and pass compatibility before activating."))
        }
        preferencesRepository.setAiConfiguration(activeLocalModelId = modelId)
        return Result.success(Unit)
    }

    suspend fun deleteModel(modelId: String) = withContext(Dispatchers.IO) {
        val spec = LocalModelCatalog.findById(modelId)
        val entity = localModelDao.getById(modelId)
        if (entity != null && entity.localPath.isNotBlank()) {
            runCatching { File(entity.localPath).delete() }
        }
        if (spec != null) {
            runCatching { LocalModelCatalog.resolveConfinedModelFile(modelsDir, spec.fileName).delete() }
            runCatching { LocalModelCatalog.resolveConfinedModelFile(modelsDir, "${spec.fileName}.part").delete() }
        }
        localModelDao.deleteById(modelId)
        val activeId = preferencesRepository.preferencesFlow.first().activeLocalModelId
        if (activeId == modelId) {
            preferencesRepository.setAiConfiguration(clearLocalModelId = true)
        }
    }

    suspend fun computeModelsStorageBytes(): Long = withContext(Dispatchers.IO) {
        modelsDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    fun getHuggingFaceToken(): String? =
        secretStore.getSecret(com.aifitnesscoach.core.security.AndroidKeystoreSecretStore.HUGGING_FACE_TOKEN_ALIAS)

    fun saveHuggingFaceToken(token: String?) {
        secretStore.putSecret(
            com.aifitnesscoach.core.security.AndroidKeystoreSecretStore.HUGGING_FACE_TOKEN_ALIAS,
            token
        )
    }
}
