package com.aifitnesscoach.core.media

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.exercise.MediaAssetRef
import kotlinx.coroutines.delay
import java.io.File

private fun assetExists(context: Context, assetPath: String): Boolean {
    return runCatching {
        context.assets.open(assetPath).use { true }
    }.getOrDefault(false)
}

/**
 * Offline demonstration media renderer:
 * 1. Renders verified locally downloaded files (MP4 via Media3, GIF via Coil, or animated Start<->Peak WebP posture pair)
 * 2. Otherwise plays bundled app assets if present
 * 3. Otherwise displays an explicit labeled placeholder without substituting unrelated equipment
 */
@Composable
fun ExerciseDemoMediaView(
    mediaRef: MediaAssetRef,
    isPersian: Boolean,
    downloadedAsset: ExerciseAssetEntity? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryFile = downloadedAsset?.localPrimaryPath?.let(::File)?.takeIf { it.exists() }
    val secondaryFile = downloadedAsset?.localSecondaryPath?.let(::File)?.takeIf { it.exists() }
    val isDownloaded = downloadedAsset?.status == AssetDownloadStatus.DOWNLOADED.name && primaryFile != null

    val hasBundledMp4 = remember(mediaRef.demoVideoAsset) { assetExists(context, mediaRef.demoVideoAsset) }
    val hasBundledGif = remember(mediaRef.demoGifAsset) {
        mediaRef.demoGifAsset?.let { assetExists(context, it) } ?: false
    }
    val hasBundledThumb = remember(mediaRef.thumbnailAsset) { assetExists(context, mediaRef.thumbnailAsset) }

    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val viewLabel = mediaRef.availableViews.firstOrNull()?.let {
        if (isPersian) it.labelFa else it.labelEn
    } ?: ""

    var showPeakPhase by remember(primaryFile?.absolutePath, secondaryFile?.absolutePath) {
        mutableStateOf(false)
    }

    if (isDownloaded && secondaryFile != null) {
        LaunchedEffect(primaryFile.absolutePath, secondaryFile.absolutePath) {
            while (true) {
                delay(1600L)
                showPeakPhase = !showPeakPhase
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloaded && downloadedAsset?.mediaType == AssetMediaType.MP4.name && primaryFile != null -> {
                    val exoPlayer = remember(primaryFile.absolutePath) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(primaryFile.toURI().toString()))
                            repeatMode = Player.REPEAT_MODE_ALL
                            volume = 0f
                            prepare()
                            playWhenReady = true
                        }
                    }
                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = exoPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                isDownloaded && primaryFile != null -> {
                    val activeFile = if (showPeakPhase && secondaryFile != null) secondaryFile else primaryFile
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = secondaryFile != null) {
                                showPeakPhase = !showPeakPhase
                            }
                    ) {
                        AsyncImage(
                            model = activeFile,
                            imageLoader = gifImageLoader,
                            contentDescription = downloadedAsset?.exerciseId ?: mediaRef.thumbnailAsset,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                        if (secondaryFile != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (showPeakPhase) {
                                            stringResource(R.string.media_phase_peak)
                                        } else {
                                            stringResource(R.string.media_phase_start)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                hasBundledMp4 -> {
                    val exoPlayer = remember(mediaRef.demoVideoAsset) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri("file:///android_asset/${mediaRef.demoVideoAsset}"))
                            repeatMode = Player.REPEAT_MODE_ALL
                            volume = 0f
                            prepare()
                            playWhenReady = true
                        }
                    }
                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = exoPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                hasBundledGif || hasBundledThumb -> {
                    val path = if (hasBundledGif) mediaRef.demoGifAsset!! else mediaRef.thumbnailAsset
                    AsyncImage(
                        model = "file:///android_asset/$path",
                        imageLoader = gifImageLoader,
                        contentDescription = mediaRef.thumbnailAsset,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OndemandVideo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = stringResource(R.string.exercise_demo_placeholder_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(
                                R.string.exercise_demo_placeholder_desc,
                                mediaRef.demoVideoAsset,
                                viewLabel
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (isDownloaded && !downloadedAsset?.attribution.isNullOrBlank()) {
            Text(
                text = downloadedAsset!!.attribution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
