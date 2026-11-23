package com.aifitnesscoach.feature.exercises

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.media.AssetDownloadStatus
import com.aifitnesscoach.core.media.AssetMediaType
import com.aifitnesscoach.core.media.ExerciseMediaManifestEntry
import com.aifitnesscoach.core.media.resolveEffectiveStatus
import com.aifitnesscoach.core.ui.LayoutSpacing

internal fun hasMotionMedia(manifest: ExerciseMediaManifestEntry?, downloaded: ExerciseAssetEntity?): Boolean {
    val remote = manifest?.mediaType
    if (remote == AssetMediaType.MP4 || remote == AssetMediaType.GIF) return true
    return (downloaded?.mediaType == AssetMediaType.MP4.name || downloaded?.mediaType == AssetMediaType.GIF.name) &&
        hasUsableDownloadedMedia(downloaded)
}

@StringRes
internal fun AssetMediaType.labelResource(): Int = when (this) {
    AssetMediaType.MP4 -> R.string.exercise_media_video
    AssetMediaType.GIF -> R.string.exercise_media_gif
    else -> R.string.exercise_media_image
}

@Composable
internal fun ExerciseMediaControls(
    manifest: ExerciseMediaManifestEntry,
    downloaded: ExerciseAssetEntity?,
    pending: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onManage: () -> Unit
) {
    val status = resolveEffectiveStatus(manifest, downloaded)
    val available = manifest.primaryUrl != null
    val context = LocalContext.current
    val stored = status == AssetDownloadStatus.DOWNLOADED
    val storedType = if (stored) AssetMediaType.entries.firstOrNull { it.name == downloaded?.mediaType } else null
    val type = storedType ?: manifest.mediaType

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.padding(horizontal = LayoutSpacing.screen, vertical = LayoutSpacing.small),
            verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
        ) {
            if (available || stored) {
                val bytes = if (stored) downloaded?.bytesDownloaded else manifest.expectedBytes
                val kind = stringResource(type.labelResource())
                val details = if (bytes != null && bytes > 0) {
                    stringResource(R.string.exercise_media_format_size, kind, Formatter.formatShortFileSize(context, bytes))
                } else kind
                Text(details, style = MaterialTheme.typography.titleSmall)
            }
            when {
                pending -> {
                    val total = downloaded?.totalBytes ?: 0L
                    if (downloaded != null && downloaded.status == AssetDownloadStatus.DOWNLOADING.name && total > 0L) {
                        LinearProgressIndicator(
                            progress = { (downloaded.bytesDownloaded.toFloat() / total).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(if (downloaded?.status == AssetDownloadStatus.DOWNLOADING.name)
                        R.string.media_status_downloading else R.string.media_waiting_download))
                    TextButton(onClick = onCancelDownload) { Text(stringResource(R.string.common_cancel)) }
                }
                stored -> Text(stringResource(R.string.exercise_media_offline), style = MaterialTheme.typography.bodyMedium)
                available -> {
                    if (status == AssetDownloadStatus.FAILED) {
                        Text(stringResource(R.string.media_status_failed), color = MaterialTheme.colorScheme.error)
                    }
                    val downloadLabel = when (type) {
                        AssetMediaType.MP4 -> R.string.exercise_download_video
                        AssetMediaType.GIF -> R.string.exercise_download_gif
                        else -> R.string.exercise_download_image
                    }
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LayoutSpacing.touchTarget),
                        onClick = onDownload
                    ) {
                        Text(stringResource(if (status == AssetDownloadStatus.FAILED) R.string.common_retry else downloadLabel))
                    }
                }
                else -> Text(stringResource(R.string.exercise_media_no_download), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onManage) {
                Text(stringResource(if (available || stored) R.string.exercise_media_manage else R.string.media_import_local))
            }
        }
    }
}
