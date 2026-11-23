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
    private var bitmap: ImageBitmap? = null

    suspend fun load(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        lock.withLock {
            bitmap ?: runCatching {
                context.assets.open(ExternalExerciseSources.ATLAS_PATH).use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()?.also { bitmap = it }
        }
    }
}

@Composable
fun ExerciseArtwork(exerciseId: String, modifier: Modifier = Modifier) {
    val imported = ExternalExerciseSources.entry(exerciseId)
    if (imported == null) {
        ExerciseIllustration(exerciseId, modifier)
        return
    }
    val photo = ExternalExerciseSources.photo(exerciseId)
    val context = LocalContext.current.applicationContext
    val atlas by produceState<ImageBitmap?>(null, photo != null) {
        if (photo != null) value = ExerciseThumbnailAtlas.load(context)
    }
    val image = atlas
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
