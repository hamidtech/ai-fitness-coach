package com.aifitnesscoach.feature.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalUriHandler
import android.text.format.Formatter
import com.aifitnesscoach.core.media.ExerciseIllustration
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.ui.LayoutSpacing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifitnesscoach.R
import com.aifitnesscoach.core.media.AssetDownloadStatus
import com.aifitnesscoach.core.media.ExerciseAssetCatalog
import com.aifitnesscoach.core.media.ExerciseAssetDownloadManager
import com.aifitnesscoach.core.media.ExerciseAssetRepository
import com.aifitnesscoach.feature.exercises.categoryLabel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseMediaLibraryScreen(
    isPersian: Boolean,
    assetRepository: ExerciseAssetRepository,
    downloadManager: ExerciseAssetDownloadManager,
    preferencesRepository: UserPreferencesRepository,
    wifiOnly: Boolean,
    exerciseId: String? = null,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val items by assetRepository.observeAllAssetItems()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var statusFilter by remember { mutableStateOf<AssetDownloadStatus?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = LocalUriHandler.current
    var storageBytes by remember { mutableLongStateOf(0L) }
    var pendingImportExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var importFeedback by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val exId = pendingImportExerciseId
        pendingImportExerciseId = null
        if (uri != null && exId != null) {
            scope.launch {
                val res = assetRepository.importLocalMediaForExercise(exId, uri)
                importFeedback = res.fold(
                    onSuccess = { null },
                    onFailure = { it.localizedMessage }
                )
                storageBytes = assetRepository.computeStorageBytes()
            }
        }
    }

    LaunchedEffect(items) {
        storageBytes = assetRepository.computeStorageBytes()
    }

    val downloadableIds = remember(items) {
        items.filter {
            it.effectiveStatus == AssetDownloadStatus.AVAILABLE ||
                it.effectiveStatus == AssetDownloadStatus.FAILED ||
                it.effectiveStatus == AssetDownloadStatus.PAUSED
        }.map { it.exercise.id }
    }
    val downloadedCount = items.count { it.effectiveStatus == AssetDownloadStatus.DOWNLOADED }

    var showAll by rememberSaveable(exerciseId) { mutableStateOf(exerciseId == null) }
    val filtered = items.filter { (showAll || it.exercise.id == exerciseId) &&
        (statusFilter == null || it.effectiveStatus == statusFilter) }

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
                text = stringResource(R.string.media_library_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(LayoutSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(
                                R.string.media_library_summary,
                                downloadedCount,
                                items.size,
                                String.format(Locale.US, "%.1f MB", storageBytes / (1024.0 * 1024.0))
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = ExerciseAssetCatalog.REPDB_ATTRIBUTION + " — " + stringResource(R.string.media_library_repdb_note),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.common_wifi_only), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = wifiOnly, onCheckedChange = { value ->
                                scope.launch { preferencesRepository.setMediaWifiOnly(value) }
                            })
                        }
                        if (!showAll) {
                            TextButton(onClick = { showAll = true }) {
                                Text(stringResource(R.string.media_show_library))
                            }
                        }
                        if (showAll) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { downloadManager.enqueueBatchDownload(downloadableIds, wifiOnly = wifiOnly) },
                                enabled = downloadableIds.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.media_download_all))
                            }
                            OutlinedButton(
                                onClick = {
                                    downloadManager.cancelAllDownloads()
                                    scope.launch {
                                        assetRepository.deleteAllAssets()
                                        storageBytes = assetRepository.computeStorageBytes()
                                    }
                                },
                                enabled = downloadedCount > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.media_delete_all))
                            }
                        }
                        importFeedback?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (showAll) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { statusFilter = null },
                            label = { Text(stringResource(R.string.exercises_all_categories)) }
                        )
                    }
                    items(
                        listOf(
                            AssetDownloadStatus.AVAILABLE to R.string.media_status_available,
                            AssetDownloadStatus.DOWNLOADED to R.string.media_status_downloaded,
                            AssetDownloadStatus.NOT_AVAILABLE to R.string.media_status_not_available,
                            AssetDownloadStatus.FAILED to R.string.media_status_failed
                        )
                    ) { (st, labelRes) ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }

            items(filtered, key = { it.exercise.id }) { item ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExerciseIllustration(item.exercise.id, Modifier.size(72.dp))
                        Text(
                            text = if (isPersian) item.exercise.nameFa else item.exercise.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${categoryLabel(item.exercise.category)} · ${statusDisplay(item.effectiveStatus)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (item.effectiveStatus == AssetDownloadStatus.NOT_AVAILABLE) {
                            val reason = if (isPersian) item.manifest.unavailableReasonFa else item.manifest.unavailableReasonEn
                            if (!reason.isNullOrBlank()) {
                                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (item.effectiveStatus == AssetDownloadStatus.DOWNLOADED) {
                            Text(stringResource(R.string.media_on_device, Formatter.formatShortFileSize(context, item.totalBytes)))
                            item.entity?.licenseName?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                        if (item.manifest.primaryUrl != null) {
                            Text(stringResource(
                                R.string.media_webp_source,
                                item.manifest.resolution.orEmpty(),
                                Formatter.formatShortFileSize(context, item.manifest.expectedBytes ?: 0)
                            ))
                            item.manifest.licenseUrl?.let { url ->
                                TextButton(onClick = { uriHandler.openUri(url) }) {
                                    Text(item.manifest.licenseName)
                                }
                            }
                        }
                        if (item.effectiveStatus == AssetDownloadStatus.DOWNLOADING) {
                            if (item.totalBytes > 0) {
                                LinearProgressIndicator(
                                    progress = { (item.bytesDownloaded.toFloat() / item.totalBytes).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                        }

                        item.errorMessage?.let { err ->
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                            when (item.effectiveStatus) {
                                AssetDownloadStatus.AVAILABLE,
                                AssetDownloadStatus.PAUSED,
                                AssetDownloadStatus.FAILED -> {
                                    Button(onClick = { downloadManager.enqueueDownload(item.exercise.id, wifiOnly) }) {
                                        Text(
                                            if (item.effectiveStatus == AssetDownloadStatus.FAILED) {
                                                stringResource(R.string.common_retry)
                                            } else {
                                                stringResource(R.string.common_download)
                                            }
                                        )
                                    }
                                }
                                AssetDownloadStatus.DOWNLOADING -> {
                                    OutlinedButton(onClick = { downloadManager.pauseOrCancel(item.exercise.id) }) {
                                        Text(stringResource(R.string.common_cancel))
                                    }
                                }
                                AssetDownloadStatus.DOWNLOADED -> {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            assetRepository.deleteAsset(item.exercise.id)
                                            storageBytes = assetRepository.computeStorageBytes()
                                        }
                                    }) {
                                        Text(stringResource(R.string.common_delete))
                                    }
                                }
                                AssetDownloadStatus.NOT_AVAILABLE,
                                AssetDownloadStatus.LICENSE_RESTRICTED -> Unit
                            }

                            TextButton(enabled = item.effectiveStatus != AssetDownloadStatus.DOWNLOADING, onClick = {
                                pendingImportExerciseId = item.exercise.id
                                importLauncher.launch(arrayOf("video/mp4", "image/*"))
                            }) {
                                Text(stringResource(R.string.media_import_local))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusDisplay(status: AssetDownloadStatus): String = when (status) {
    AssetDownloadStatus.AVAILABLE -> stringResource(R.string.media_status_available)
    AssetDownloadStatus.DOWNLOADING -> stringResource(R.string.media_status_downloading)
    AssetDownloadStatus.DOWNLOADED -> stringResource(R.string.media_status_downloaded)
    AssetDownloadStatus.PAUSED -> stringResource(R.string.media_status_paused)
    AssetDownloadStatus.FAILED -> stringResource(R.string.media_status_failed)
    AssetDownloadStatus.NOT_AVAILABLE -> stringResource(R.string.media_status_not_available)
    AssetDownloadStatus.LICENSE_RESTRICTED -> stringResource(R.string.media_status_license_restricted)
}
