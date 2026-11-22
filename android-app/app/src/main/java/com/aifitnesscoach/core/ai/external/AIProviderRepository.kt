package com.aifitnesscoach.core.ai.external

import com.aifitnesscoach.core.database.AIProviderConfigEntity
import com.aifitnesscoach.core.database.AIProviderDao
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.security.AndroidKeystoreSecretStore
import com.aifitnesscoach.core.security.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

data class AIProviderUiItem(
    val config: AIProviderConfigEntity,
    val hasApiKeyStored: Boolean,
    val isActive: Boolean
)

class AIProviderRepository(
    private val dao: AIProviderDao,
    private val secretStore: SecretStore,
    private val preferencesRepository: UserPreferencesRepository,
    val client: AIProviderClient = AIProviderClient()
) {
    fun observeProviders(): Flow<List<AIProviderConfigEntity>> = dao.observeAll()

    suspend fun getProvider(id: String): AIProviderConfigEntity? = dao.getById(id)

    fun hasApiKey(providerId: String): Boolean =
        secretStore.hasSecret(AndroidKeystoreSecretStore.providerApiKeyAlias(providerId))

    fun getApiKey(providerId: String): String? =
        secretStore.getSecret(AndroidKeystoreSecretStore.providerApiKeyAlias(providerId))

    suspend fun saveProvider(
        existingId: String?,
        name: String,
        presetType: ProviderPresetType,
        baseUrl: String,
        endpointPath: String,
        modelId: String,
        apiKeyInput: String?,
        clearExistingApiKey: Boolean,
        customHeadersJson: String,
        timeoutSec: Int,
        maxTokens: Int,
        temperature: Float,
        allowInsecureLocalHttp: Boolean,
        enabled: Boolean,
        setAsActive: Boolean
    ): Result<AIProviderConfigEntity> {
        val validation = AIProviderClient.validateEndpointUrl(baseUrl, endpointPath, allowInsecureLocalHttp)
        if (!validation.valid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid provider endpoint URL"))
        }

        val now = System.currentTimeMillis()
        val existing = existingId?.let { dao.getById(it) }
        val id = existing?.id ?: "prov_${UUID.randomUUID()}"
        val entity = AIProviderConfigEntity(
            id = id,
            name = name.trim().ifBlank { presetType.displayNameEn },
            presetType = presetType.name,
            baseUrl = baseUrl.trim().trimEnd('/'),
            endpointPath = endpointPath.trim().ifBlank { "/v1/chat/completions" },
            modelId = modelId.trim().ifBlank { presetType.defaultModelId },
            customHeadersJson = customHeadersJson.trim().ifBlank { "{}" },
            timeoutSec = timeoutSec.coerceIn(5, 180),
            maxTokens = maxTokens.coerceIn(64, 4096),
            temperature = temperature.coerceIn(0f, 1.5f),
            allowInsecureLocalHttp = allowInsecureLocalHttp,
            enabled = enabled,
            createdAtMs = existing?.createdAtMs ?: now,
            updatedAtMs = now
        )
        dao.upsert(entity)

        val keyAlias = AndroidKeystoreSecretStore.providerApiKeyAlias(id)
        if (clearExistingApiKey) {
            secretStore.removeSecret(keyAlias)
        } else if (!apiKeyInput.isNullOrBlank()) {
            secretStore.putSecret(keyAlias, apiKeyInput.trim())
        }

        if (setAsActive && enabled) {
            preferencesRepository.setAiConfiguration(activeExternalProviderId = id)
        }
        return Result.success(entity)
    }

    suspend fun testConnection(
        config: AIProviderConfigEntity,
        overrideApiKey: String? = null
    ): ProviderConnectionTestResult {
        val key = overrideApiKey?.takeIf { it.isNotBlank() } ?: getApiKey(config.id)
        return client.testConnection(config, key)
    }

    suspend fun setActiveProvider(id: String) {
        preferencesRepository.setAiConfiguration(activeExternalProviderId = id)
    }

    suspend fun deleteProvider(id: String) {
        dao.deleteById(id)
        secretStore.removeSecret(AndroidKeystoreSecretStore.providerApiKeyAlias(id))
        val activeId = preferencesRepository.preferencesFlow.first().activeExternalProviderId
        if (activeId == id) {
            preferencesRepository.setAiConfiguration(clearExternalProviderId = true)
        }
    }
}
