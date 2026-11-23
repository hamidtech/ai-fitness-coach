package com.aifitnesscoach.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ui.ControlFlow
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.core.ai.external.AIProviderClient
import com.aifitnesscoach.core.ai.external.AIProviderRepository
import com.aifitnesscoach.core.ai.external.ProviderConnectionTestResult
import com.aifitnesscoach.core.ai.external.ProviderPresetType
import com.aifitnesscoach.core.database.AIProviderConfigEntity
import com.aifitnesscoach.core.database.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun AIProviderSettingsScreen(
    isPersian: Boolean,
    preferences: UserPreferences,
    repository: AIProviderRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val providers by repository.observeProviders()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var editingProviderId by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    var preset by remember { mutableStateOf(ProviderPresetType.OPENAI_CLOUD) }
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(ProviderPresetType.OPENAI_CLOUD.defaultBaseUrl) }
    var endpointPath by remember { mutableStateOf(ProviderPresetType.OPENAI_CLOUD.defaultEndpointPath) }
    var modelId by remember { mutableStateOf(ProviderPresetType.OPENAI_CLOUD.defaultModelId) }
    var apiKeyInput by remember { mutableStateOf("") }
    var clearStoredKey by remember { mutableStateOf(false) }
    var customHeadersJson by remember { mutableStateOf("{}") }
    var timeoutText by remember { mutableStateOf("30") }
    var maxTokensText by remember { mutableStateOf("700") }
    var tempText by remember { mutableStateOf("0.25") }
    var allowLanHttp by remember { mutableStateOf(false) }
    var showLanHttpWarningDialog by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(true) }
    var editorError by remember { mutableStateOf<String?>(null) }

    var testingProviderId by remember { mutableStateOf<String?>(null) }
    var testResultByProvider by remember { mutableStateOf<Map<String, ProviderConnectionTestResult>>(emptyMap()) }

    fun openEditorForNew(selectedPreset: ProviderPresetType) {
        editingProviderId = null
        preset = selectedPreset
        name = if (isPersian) selectedPreset.displayNameFa else selectedPreset.displayNameEn
        baseUrl = selectedPreset.defaultBaseUrl
        endpointPath = selectedPreset.defaultEndpointPath
        modelId = selectedPreset.defaultModelId
        apiKeyInput = ""
        clearStoredKey = false
        customHeadersJson = "{}"
        timeoutText = "30"
        maxTokensText = "700"
        tempText = "0.25"
        allowLanHttp = false
        enabled = true
        editorError = null
        showEditor = true
    }

    fun openEditorForExisting(existing: AIProviderConfigEntity) {
        editingProviderId = existing.id
        preset = runCatching { ProviderPresetType.valueOf(existing.presetType) }
            .getOrDefault(ProviderPresetType.CUSTOM_COMPATIBLE)
        name = existing.name
        baseUrl = existing.baseUrl
        endpointPath = existing.endpointPath
        modelId = existing.modelId
        apiKeyInput = ""
        clearStoredKey = false
        customHeadersJson = existing.customHeadersJson
        timeoutText = existing.timeoutSec.toString()
        maxTokensText = existing.maxTokens.toString()
        tempText = existing.temperature.toString()
        allowLanHttp = existing.allowInsecureLocalHttp
        enabled = existing.enabled
        editorError = null
        showEditor = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                text = stringResource(R.string.external_ai_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(LayoutSpacing.pagePadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.external_ai_security_note), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.external_ai_add_preset), style = MaterialTheme.typography.labelLarge)
                        ControlFlow {
                            ProviderPresetType.entries.forEach { p ->
                                OutlinedButton(onClick = { openEditorForNew(p) }) {
                                    Text(if (isPersian) p.displayNameFa else p.displayNameEn)
                                }
                            }
                        }
                    }
                }
            }

            if (providers.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Text(
                            text = stringResource(R.string.external_ai_empty),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(providers, key = { it.id }) { config ->
                    val isActive = preferences.activeExternalProviderId == config.id
                    val hasKey = repository.hasApiKey(config.id)
                    val testRes = testResultByProvider[config.id]
                    val isTesting = testingProviderId == config.id

                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = config.name + if (isActive) " ★" else "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${config.baseUrl}${config.endpointPath} · ${config.modelId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    R.string.external_ai_key_status,
                                    if (hasKey) stringResource(R.string.external_ai_key_stored_masked)
                                    else stringResource(R.string.common_none)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            testRes?.let { res ->
                                val summary = if (res.success) {
                                    stringResource(R.string.external_ai_test_ok, res.latencyMs, res.sampleResponse)
                                } else {
                                    stringResource(R.string.external_ai_test_failed, res.errorMessage ?: "Unknown error")
                                }
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (res.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            testingProviderId = config.id
                                            val result = repository.testConnection(config)
                                            testResultByProvider = testResultByProvider + (config.id to result)
                                            testingProviderId = null
                                        }
                                    },
                                    enabled = !isTesting
                                ) {
                                    Text(
                                        if (isTesting) stringResource(R.string.external_ai_testing)
                                        else stringResource(R.string.external_ai_test_btn)
                                    )
                                }
                                if (!isActive && config.enabled) {
                                    OutlinedButton(onClick = {
                                        scope.launch { repository.setActiveProvider(config.id) }
                                    }) {
                                        Text(stringResource(R.string.common_set_active))
                                    }
                                }
                                OutlinedButton(onClick = { openEditorForExisting(config) }) {
                                    Text(stringResource(R.string.common_edit))
                                }
                                TextButton(onClick = {
                                    scope.launch { repository.deleteProvider(config.id) }
                                }) {
                                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        val urlPreview = AIProviderClient.validateEndpointUrl(baseUrl, endpointPath, allowLanHttp)
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(stringResource(R.string.external_ai_editor_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.external_ai_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text(stringResource(R.string.external_ai_base_url_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = endpointPath,
                        onValueChange = { endpointPath = it },
                        label = { Text(stringResource(R.string.external_ai_path_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = modelId,
                        onValueChange = { modelId = it },
                        label = { Text(stringResource(R.string.external_ai_model_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            if (it.isNotBlank()) clearStoredKey = false
                        },
                        label = { Text(stringResource(R.string.external_ai_apikey_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timeoutText,
                            onValueChange = { timeoutText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(R.string.external_ai_timeout_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxTokensText,
                            onValueChange = { maxTokensText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(R.string.external_ai_max_tokens_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (urlPreview.isHttp && urlPreview.isLanOrLoopback) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.external_ai_allow_lan_http), modifier = Modifier.weight(1f))
                            Switch(
                                checked = allowLanHttp,
                                onCheckedChange = { checked ->
                                    if (checked) showLanHttpWarningDialog = true
                                    else allowLanHttp = false
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.external_ai_enabled_toggle))
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }

                    editorError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val res = repository.saveProvider(
                            existingId = editingProviderId,
                            name = name,
                            presetType = preset,
                            baseUrl = baseUrl,
                            endpointPath = endpointPath,
                            modelId = modelId,
                            apiKeyInput = apiKeyInput,
                            clearExistingApiKey = clearStoredKey,
                            customHeadersJson = customHeadersJson,
                            timeoutSec = timeoutText.toIntOrNull() ?: 30,
                            maxTokens = maxTokensText.toIntOrNull() ?: 700,
                            temperature = tempText.toFloatOrNull() ?: 0.25f,
                            allowInsecureLocalHttp = allowLanHttp,
                            enabled = enabled,
                            setAsActive = editingProviderId == null
                        )
                        res.fold(
                            onSuccess = { showEditor = false },
                            onFailure = { editorError = it.localizedMessage }
                        )
                    }
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showLanHttpWarningDialog) {
        AlertDialog(
            onDismissRequest = { showLanHttpWarningDialog = false },
            title = { Text(stringResource(R.string.external_ai_lan_http_warn_title)) },
            text = { Text(stringResource(R.string.external_ai_lan_http_warn_body)) },
            confirmButton = {
                TextButton(onClick = {
                    allowLanHttp = true
                    showLanHttpWarningDialog = false
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanHttpWarningDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
