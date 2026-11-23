package com.aifitnesscoach.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.aifitnesscoach.core.exercise.definitions.MuscleRegion

@Composable
internal fun MuscleArtwork(regions: Set<MuscleRegion>, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val edge = size.minDimension
        val x = (size.width - edge) / 2
        val y = (size.height - edge) / 2
        fun at(horizontal: Float, vertical: Float) = Offset(x + horizontal * edge, y + vertical * edge)
        fun segment(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, region: MuscleRegion) {
            drawLine(if (region in regions || MuscleRegion.FULL_BODY in regions) accent else outline,
                at(x1, y1), at(x2, y2), width * edge, StrokeCap.Round)
        }

        drawCircle(outline, edge * 0.09f, at(0.5f, 0.16f))
        segment(0.5f, 0.24f, 0.5f, 0.31f, 0.08f, MuscleRegion.NECK)
        segment(0.38f, 0.34f, 0.62f, 0.34f, 0.1f, MuscleRegion.SHOULDERS)
        val upper = when {
            MuscleRegion.CHEST in regions -> MuscleRegion.CHEST
            MuscleRegion.CARDIO in regions -> MuscleRegion.CARDIO
            else -> MuscleRegion.BACK
        }
        segment(0.5f, 0.37f, 0.5f, 0.43f, 0.22f, upper)
        segment(0.5f, 0.46f, 0.5f, 0.54f, 0.17f, MuscleRegion.CORE)
        segment(0.44f, 0.57f, 0.56f, 0.57f, 0.12f, MuscleRegion.HIPS)
        segment(0.34f, 0.38f, 0.27f, 0.57f, 0.06f, MuscleRegion.ARMS)
        segment(0.66f, 0.38f, 0.73f, 0.57f, 0.06f, MuscleRegion.ARMS)
        segment(0.44f, 0.62f, 0.41f, 0.74f, 0.08f, MuscleRegion.THIGHS)
        segment(0.56f, 0.62f, 0.59f, 0.74f, 0.08f, MuscleRegion.THIGHS)
        segment(0.41f, 0.77f, 0.39f, 0.9f, 0.065f, MuscleRegion.LOWER_LEGS)
        segment(0.59f, 0.77f, 0.61f, 0.9f, 0.065f, MuscleRegion.LOWER_LEGS)
    }
}
