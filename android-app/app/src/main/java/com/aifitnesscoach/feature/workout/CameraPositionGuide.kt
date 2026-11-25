package com.aifitnesscoach.feature.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.exercise.ExerciseDefinition
import com.aifitnesscoach.core.media.ExerciseIllustration
import com.aifitnesscoach.core.ui.LayoutSpacing

@Composable
fun CameraPositionGuide(
    exercise: ExerciseDefinition,
    isPersian: Boolean,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LayoutSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(LayoutSpacing.screen)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
            }
            Text(stringResource(R.string.camera_setup_title), style = MaterialTheme.typography.headlineSmall)
        }
        Text(if (isPersian) exercise.nameFa else exercise.nameEn, style = MaterialTheme.typography.titleLarge)
        CameraFramingExample(exercise.id, Modifier.align(Alignment.CenterHorizontally))
        CameraPositionInstructions(exercise, isPersian)
        Card {
            Column(
                Modifier.padding(LayoutSpacing.screen),
                verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
            ) {
                Text(stringResource(R.string.guide_light), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.guide_clear_view))
                Text(stringResource(R.string.guide_angle_explanation))
            }
        }
    }
}

@Composable
internal fun CameraFramingExample(exerciseId: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.layout.Box(
        modifier.widthIn(max = 280.dp).fillMaxWidth()
            .aspectRatio(if (exerciseId in floorExerciseIds) 1.6f else 1f).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        ExerciseIllustration(exerciseId, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val border = 2.dp.toPx()
            drawRect(color, size = Size(size.width, size.height), style = Stroke(border))
            val marks = listOf(0.2f, 0.8f)
            marks.forEach { x ->
                val mark = size.width * x
                drawLine(color, Offset(mark, 0f), Offset(mark, 8.dp.toPx()), border, StrokeCap.Round)
                drawLine(color, Offset(mark, size.height), Offset(mark, size.height - 8.dp.toPx()), border, StrokeCap.Round)
            }
        }
    }
}

@Composable
internal fun CameraPositionInstructions(exercise: ExerciseDefinition, isPersian: Boolean) {
    val guide = exercise.cameraGuide
    Column(verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
        Text(if (isPersian) guide.recommendedView.labelFa else guide.recommendedView.labelEn, style = MaterialTheme.typography.titleMedium)
        val distance = if (isPersian) guide.distanceFa else guide.distanceEn
        val height = if (isPersian) guide.heightFa else guide.heightEn
        if (distance.isNotBlank()) Text(stringResource(R.string.camera_setup_distance, distance))
        if (height.isNotBlank()) Text(stringResource(R.string.camera_setup_height, height))
        if (distance.isBlank() || height.isBlank()) Text(stringResource(R.string.guide_general_framing))
        Text(stringResource(if (exercise.id in floorExerciseIds) R.string.guide_landscape else R.string.guide_portrait))
        Text(stringResource(R.string.guide_frame_range))
        when (exercise.id) {
            "dumbbell_biceps_curl", "band_biceps_curl" -> Text(stringResource(R.string.guide_curl))
            "bodyweight_squat" -> Text(stringResource(R.string.guide_squat))
            "forearm_plank" -> Text(stringResource(R.string.guide_plank))
        }
    }
}

private val floorExerciseIds = setOf(
    "dumbbell_chest_press", "push_up", "glute_bridge", "superman", "forearm_plank", "side_plank",
    "dead_bug", "bird_dog", "abdominal_crunch", "reverse_crunch", "mountain_climber",
    "hollow_body_hold", "mini_band_clamshell", "band_dead_bug", "aifc-high-plank",
    "aifc-child-pose", "aifc-seated-forward-fold", "aifc-wide-push-up"
)
