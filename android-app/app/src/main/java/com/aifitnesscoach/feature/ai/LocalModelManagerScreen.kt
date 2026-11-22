package com.aifitnesscoach.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ai.local.LocalModelDownloadManager
import com.aifitnesscoach.core.ai.local.LocalModelDownloadStatus
import com.aifitnesscoach.core.ai.local.LocalModelRepository
import com.aifitnesscoach.core.ai.local.LocalModelTestReport
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LocalModelManagerScreen(
    isPersian: Boolean,
    repository: LocalModelRepository,
    downloadManager: LocalModelDownloadManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val modelStates by repository.observeModelStates()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val resources = remember(modelStates) { repository.getDeviceResources() }

    var wifiOnly by remember { mutableStateOf(true) }
    var hfTokenInput by remember { mutableStateOf("") }
    var hasStoredHfToken by remember { mutableStateOf(!repository.getHuggingFaceToken().isNullOrBlank()) }
    var testingModelId by remember { mutableStateOf<String?>(null) }
    var lastTestReport by remember { mutableStateOf<Pair<String, LocalModelTestReport>?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
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
                text = stringResource(R.string.local_models_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(
                                R.string.local_models_device_resources,
                                resources.totalRamMb,
                                String.format(Locale.US, "%.2f GB", resources.availableStorageBytes / (1024.0 * 1024.0 * 1024.0)),
                                resources.supportedAbis.firstOrNull() ?: "unknown"
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.local_models_runtime_note),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.common_wifi_only), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
                        }
                    }
                }
            }

            // Optional user-entered Hugging Face token for gated Gemma 3 models
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.local_models_hf_token_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (hasStoredHfToken) {
                                stringResource(R.string.local_models_hf_token_saved)
                            } else {
                                stringResource(R.string.local_models_hf_token_hint)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = hfTokenInput,
                            onValueChange = { hfTokenInput = it },
                            label = { Text(stringResource(R.string.local_models_hf_token_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                repository.saveHuggingFaceToken(hfTokenInput)
                                hasStoredHfToken = !repository.getHuggingFaceToken().isNullOrBlank()
                                hfTokenInput = ""
                            }) {
                                Text(stringResource(R.string.common_save))
                            }
                            if (hasStoredHfToken) {
                                TextButton(onClick = {
                                    repository.saveHuggingFaceToken(null)
                                    hasStoredHfToken = false
                                }) {
                                    Text(stringResource(R.string.common_clear))
                                }
                            }
                        }
                    }
                }
            }

            actionFeedback?.let { msg ->
                item {
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            items(modelStates, key = { it.spec.id }) { state ->
                val spec = state.spec
                val entity = state.entity
                val sizeLabel = String.format(Locale.US, "%.0f MB", spec.expectedSizeBytes / (1024.0 * 1024.0))

                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = spec.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${spec.familyName} · ${spec.quantizationLabel} · $sizeLabel · RAM ≥ ${spec.recommendedRamMb} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isPersian) spec.summaryFa else spec.summaryEn,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${spec.licenseLabel}${if (spec.requiresHfToken) " · HF Token" else " · Open"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!state.hasEnoughStorage) {
                            Text(
                                text = stringResource(R.string.local_models_low_storage_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (!state.hasRecommendedRam) {
                            Text(
                                text = stringResource(R.string.local_models_low_ram_warning, spec.recommendedRamMb),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (state.status == LocalModelDownloadStatus.DOWNLOADING && entity != null && entity.totalBytes > 0L) {
                            val pct = (entity.downloadedBytes.toFloat() / entity.totalBytes.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                            Text(
                                text = "${(pct * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        val statusLabel = when (state.status) {
                            LocalModelDownloadStatus.NOT_DOWNLOADED -> stringResource(R.string.local_model_status_not_downloaded)
                            LocalModelDownloadStatus.DOWNLOADING -> stringResource(R.string.local_model_status_downloading)
                            LocalModelDownloadStatus.PAUSED -> stringResource(R.string.local_model_status_paused)
                            LocalModelDownloadStatus.VERIFYING -> stringResource(R.string.local_model_status_verifying)
                            LocalModelDownloadStatus.DOWNLOADED -> stringResource(R.string.local_model_status_downloaded_untested)
                            LocalModelDownloadStatus.TEST_PASSED -> stringResource(R.string.local_model_status_verified)
                            LocalModelDownloadStatus.TEST_FAILED -> stringResource(R.string.local_model_status_test_failed)
                            LocalModelDownloadStatus.FAILED -> stringResource(R.string.local_model_status_failed)
                        }
                        Text(
                            text = statusLabel + if (state.isActiveSelection) " · ★ ${stringResource(R.string.common_active_badge)}" else "",
                            style = MaterialTheme.typography.labelLarge
                        )

                        entity?.lastTestError?.let { err ->
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (entity?.lastTestLoadSuccess == true) {
                            Text(
                                text = stringResource(
                                    R.string.local_model_benchmark_line,
                                    entity.lastTestInitMs ?: 0L,
                                    entity.lastTestLatencyMs ?: 0L,
                                    entity.lastTestMemoryMb ?: 0L
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (lastTestReport?.first == spec.id && lastTestReport?.second?.sampleOutput?.isNotBlank() == true) {
                            Text(
                                text = lastTestReport!!.second.sampleOutput,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (state.status) {
                                LocalModelDownloadStatus.NOT_DOWNLOADED,
                                LocalModelDownloadStatus.PAUSED,
                                LocalModelDownloadStatus.FAILED -> {
                                    Button(
                                        onClick = { downloadManager.enqueueDownload(spec.id, wifiOnly) },
                                        enabled = state.hasEnoughStorage
                                    ) {
                                        Text(
                                            when (state.status) {
                                                LocalModelDownloadStatus.PAUSED -> stringResource(R.string.common_resume)
                                                LocalModelDownloadStatus.FAILED -> stringResource(R.string.common_retry)
                                                else -> stringResource(R.string.common_download)
                                            }
                                        )
                                    }
                                    if (state.status == LocalModelDownloadStatus.PAUSED || state.status == LocalModelDownloadStatus.FAILED) {
                                        OutlinedButton(onClick = { scope.launch { repository.deleteModel(spec.id) } }) {
                                            Text(stringResource(R.string.common_delete))
                                        }
                                    }
                                }

                                LocalModelDownloadStatus.DOWNLOADING -> {
                                    OutlinedButton(onClick = { downloadManager.pauseDownload(spec.id) }) {
                                        Text(stringResource(R.string.workout_pause))
                                    }
                                }

                                LocalModelDownloadStatus.VERIFYING -> {
                                    Text(stringResource(R.string.local_model_status_verifying), style = MaterialTheme.typography.bodyMedium)
                                }

                                LocalModelDownloadStatus.DOWNLOADED,
                                LocalModelDownloadStatus.TEST_PASSED,
                                LocalModelDownloadStatus.TEST_FAILED -> {
                                    val isTesting = testingModelId == spec.id
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                testingModelId = spec.id
                                                val report = repository.runCompatibilityTest(spec.id)
                                                lastTestReport = spec.id to report
                                                testingModelId = null
                                            }
                                        },
                                        enabled = !isTesting
                                    ) {
                                        Text(
                                            if (isTesting) stringResource(R.string.local_model_testing)
                                            else stringResource(R.string.local_model_test_btn)
                                        )
                                    }

                                    if (state.status == LocalModelDownloadStatus.TEST_PASSED && !state.isActiveSelection) {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                repository.selectActiveModel(spec.id)
                                                    .onSuccess { actionFeedback = "${spec.displayName} activated." }
                                                    .onFailure { actionFeedback = it.localizedMessage }
                                            }
                                        }) {
                                            Text(stringResource(R.string.common_set_active))
                                        }
                                    }

                                    TextButton(onClick = { scope.launch { repository.deleteModel(spec.id) } }) {
                                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
