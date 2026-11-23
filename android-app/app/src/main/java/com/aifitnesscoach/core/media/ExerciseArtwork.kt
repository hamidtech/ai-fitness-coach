package com.aifitnesscoach.core.media

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import com.aifitnesscoach.core.exercise.definitions.exerciseRegions
import com.aifitnesscoach.core.ui.MuscleArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private object ExerciseThumbnailAtlas {
    private val lock = Mutex()
    private var cached: Pair<Int, ImageBitmap>? = null

    suspend fun load(context: Context, version: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        lock.withLock {
            cached?.takeIf { it.first == version }?.second ?: runCatching {
                ExternalExerciseSources.openAtlas(context, version)?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()?.also { cached = version to it }
        }
    }
}

@Composable
fun ExerciseArtwork(exerciseId: String, modifier: Modifier = Modifier) {
    val version by ExternalExerciseSources.version.collectAsStateWithLifecycle()
    val imported = ExternalExerciseSources.entry(exerciseId)
    if (imported == null) {
        ExerciseIllustration(exerciseId, modifier)
        return
    }
    val photo = ExternalExerciseSources.photo(exerciseId, version)
    val context = LocalContext.current.applicationContext
    val atlas by produceState<Pair<Int, ImageBitmap>?>(null, version, photo != null) {
        value = if (photo != null) ExerciseThumbnailAtlas.load(context, version)?.let { version to it } else null
    }
    val image = atlas?.takeIf { it.first == version }?.second
    Box(modifier, contentAlignment = Alignment.Center) {
        if (image != null && photo != null) {
            Canvas(Modifier.fillMaxSize()) {
                val edge = ExternalExerciseSources.TILE_EDGE
                val side = size.minDimension.toInt()
                if (side == 0) return@Canvas
                drawImage(
                    image = image,
                    srcOffset = IntOffset(photo.tile % ExternalExerciseSources.ATLAS_COLUMNS * edge,
                        photo.tile / ExternalExerciseSources.ATLAS_COLUMNS * edge),
                    srcSize = IntSize(edge, edge),
                    dstOffset = IntOffset((size.width.toInt() - side) / 2, (size.height.toInt() - side) / 2),
                    dstSize = IntSize(side, side)
                )
            }
        } else {
            val regions = remember(imported) { exerciseRegions(imported.definition, imported.regions) }
            MuscleArtwork(regions, Modifier.fillMaxSize())
        }
    }
}
