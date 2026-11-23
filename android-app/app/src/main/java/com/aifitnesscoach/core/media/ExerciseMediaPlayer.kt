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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.exercise.MediaAssetRef
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import kotlinx.coroutines.delay
import java.io.File

private fun assetExists(context: Context, assetPath: String): Boolean {
    if (assetPath.isBlank()) return false
    return runCatching {
        context.assets.open(assetPath).use { true }
    }.getOrDefault(false)
}

/**
 * Offline demonstration media renderer:
 * 1. Renders verified locally downloaded files (MP4 via Media3, GIF via Coil, or animated Start<->Peak WebP posture pair)
 * 2. Otherwise plays bundled app assets if present
 * 3. Shows the exercise-specific offline illustration when a file is missing or unreadable
 */
@Composable
fun ExerciseDemoMediaView(
    mediaRef: MediaAssetRef,
    exerciseId: String,
    downloadedAsset: ExerciseAssetEntity? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryFile = downloadedAsset?.localPrimaryPath?.let(::File)?.takeIf { it.exists() }
    val secondaryFile = downloadedAsset?.localSecondaryPath?.let(::File)?.takeIf { it.exists() }
    val isDownloaded = hasUsableDownloadedMedia(downloadedAsset)

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

    var mediaFailed by remember(primaryFile?.absolutePath, downloadedAsset?.updatedAtMs, exerciseId) {
        mutableStateOf(false)
    }
    var showPeakPhase by remember(primaryFile?.absolutePath, secondaryFile?.absolutePath) {
        mutableStateOf(false)
    }

    if (isDownloaded && secondaryFile != null && !mediaFailed) {
        LaunchedEffect(primaryFile?.absolutePath, secondaryFile.absolutePath) {
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
                mediaFailed -> ExerciseArtwork(exerciseId, Modifier.fillMaxSize())
                isDownloaded && downloadedAsset?.mediaType == AssetMediaType.MP4.name && primaryFile != null -> {
                    val exoPlayer = remember(primaryFile.absolutePath) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(primaryFile.toURI().toString()))
                            repeatMode = Player.REPEAT_MODE_ALL
                            volume = 0f
                            prepare()
                            playWhenReady = false
                        }
                    }
                    DisposableEffect(exoPlayer) {
                        val listener = object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) { mediaFailed = true }
                        }
                        exoPlayer.addListener(listener)
                        if (exoPlayer.playerError != null) mediaFailed = true
                        onDispose {
                            exoPlayer.removeListener(listener)
                            exoPlayer.release()
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = true
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
                            contentDescription = null,
                            onError = { mediaFailed = true },
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
                            playWhenReady = false
                        }
                    }
                    DisposableEffect(exoPlayer) {
                        val listener = object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) { mediaFailed = true }
                        }
                        exoPlayer.addListener(listener)
                        if (exoPlayer.playerError != null) mediaFailed = true
                        onDispose {
                            exoPlayer.removeListener(listener)
                            exoPlayer.release()
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = true
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
                        contentDescription = null,
                        onError = { mediaFailed = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    ExerciseArtwork(exerciseId, Modifier.fillMaxSize())
                }
            }
        }

        if (mediaFailed) {
            Text(stringResource(R.string.exercise_media_unreadable), style = MaterialTheme.typography.labelMedium)
        }
        val attribution = when {
            !mediaFailed && isDownloaded -> downloadedAsset?.attribution
            else -> ExternalExerciseSources.photo(exerciseId)?.attribution
        }
        attribution?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
