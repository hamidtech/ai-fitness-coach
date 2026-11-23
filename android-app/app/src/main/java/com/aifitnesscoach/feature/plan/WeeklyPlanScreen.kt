package com.aifitnesscoach.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import com.aifitnesscoach.core.database.nextRotationSlot
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.exercise.definitions.discoveryEntry
import com.aifitnesscoach.core.ui.MuscleArtwork
import com.aifitnesscoach.core.ui.ControlFlow
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.feature.workout.PlannedWorkoutItem

import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
internal fun WeeklyPlanScreen(
    savedDays: Map<Int, String>,
    isPersian: Boolean,
    rotationEnabled: Boolean,
    lastRotationSlot: Int,
    onRotationEnabled: (Boolean) -> Unit,
    onEdit: (Int, List<PlannedWorkoutItem>) -> Unit,
    onStart: (List<PlannedWorkoutItem>, Int?) -> Unit
) {
    val today = LocalDate.now().dayOfWeek
    val locale = if (isPersian) Locale.forLanguageTag("fa-IR") else Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val days = remember(firstDay) { List(7) { firstDay.plus(it.toLong()) } }
    val catalogVersion by ExternalExerciseSources.version.collectAsStateWithLifecycle()
    val decoded = remember(savedDays, catalogVersion) { savedDays.mapValues { WeeklyPlan.decode(it.value) } }
    val nextSlot = nextRotationSlot(decoded.filterValues { it.isNotEmpty() }.keys, lastRotationSlot)
        .takeIf { rotationEnabled }
    var templateDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val forTemplate = templateDay
    if (forTemplate != null) {
        AlertDialog(
            onDismissRequest = { templateDay = null },
            title = { Text(stringResource(R.string.plan_choose_template)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                    StarterRoutine.entries.forEach { routine ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LayoutSpacing.touchTarget),
                            onClick = {
                                templateDay = null
                                onEdit(forTemplate, routine.exercises.mapNotNull(ExerciseCatalog::findById)
                                    .map { PlannedWorkoutItem(exercise = it, targetSets = routine.sets, targetReps = routine.reps) })
                            }
                        ) { Text(stringResource(routine.title)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { templateDay = null }) { Text(stringResource(R.string.common_back)) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LayoutSpacing.pagePadding),
        verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
    ) {
        item(key = "summary", contentType = "heading") {
            Column(verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                Text(stringResource(R.string.nav_plan), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(if (rotationEnabled) R.string.rotation_help else R.string.plan_description),
                    style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                    Text(stringResource(R.string.rotation_enable), Modifier.weight(1f))
                    Switch(checked = rotationEnabled, onCheckedChange = onRotationEnabled)
                }
            }
        }
        items(if (rotationEnabled) days.sortedBy { it.value } else days, key = { it.value }, contentType = { "weekday" }) { day ->
            val dayPlan = decoded[day.value].orEmpty()
            val isNext = if (rotationEnabled) day.value == nextSlot else day == today
            Surface(
                color = if (isNext) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(LayoutSpacing.screen),
                    verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                    val label = if (rotationEnabled) stringResource(R.string.rotation_slot, day.value)
                    else day.getDisplayName(TextStyle.FULL, locale)
                    Row(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isNext) stringResource(
                                if (rotationEnabled) R.string.rotation_next else R.string.plan_today, label
                            ) else label,
                                style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (dayPlan.isEmpty()) stringResource(R.string.plan_rest_day)
                                else stringResource(R.string.discovery_results, dayPlan.size),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (dayPlan.isNotEmpty()) {
                            val regions = remember(dayPlan) {
                                dayPlan.flatMap { discoveryEntry(it.exercise).muscleRegions }.toSet()
                            }
                            MuscleArtwork(regions, Modifier.size(52.dp))
                        }
                    }
                    ControlFlow {
                        if (dayPlan.isNotEmpty()) {
                            Button(onClick = { onStart(dayPlan, nextSlot?.takeIf { it == day.value }) }) {
                                Text(stringResource(R.string.home_start_workout))
                            }
                        }
                        OutlinedButton(onClick = { onEdit(day.value, dayPlan) }) {
                            Text(stringResource(if (dayPlan.isEmpty()) R.string.plan_add else R.string.plan_edit))
                        }
                        if (dayPlan.isEmpty()) {
                            TextButton(onClick = { templateDay = day.value }) {
                                Text(stringResource(R.string.plan_choose_template))
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class StarterRoutine(
    val title: Int,
    val exercises: List<String>,
    val sets: Int = 3,
    val reps: Int = 10
) {
    FULL_BODY(R.string.plan_full_body, listOf("goblet_squat", "push_up", "dumbbell_bent_over_row", "forearm_plank")),
    UPPER(R.string.plan_upper, listOf("dumbbell_chest_press", "dumbbell_bent_over_row", "dumbbell_shoulder_press", "dumbbell_biceps_curl")),
    LOWER(R.string.plan_lower, listOf("goblet_squat", "dumbbell_romanian_deadlift", "reverse_lunge", "standing_calf_raise")),
    PUSH(R.string.plan_push, listOf("push_up", "dumbbell_chest_press", "dumbbell_shoulder_press", "dumbbell_triceps_extension")),
    PULL(R.string.plan_pull, listOf("dumbbell_bent_over_row", "band_face_pull", "band_pull_apart", "dumbbell_biceps_curl")),
    FIVE_BY_FIVE(R.string.plan_five_by_five, listOf("goblet_squat", "dumbbell_chest_press", "dumbbell_bent_over_row"), 5, 5)
}
