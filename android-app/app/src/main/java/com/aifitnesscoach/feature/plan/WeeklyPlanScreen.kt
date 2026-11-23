package com.aifitnesscoach.feature.plan

import androidx.compose.foundation.layout.Arrangement
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
    onEdit: (Int, List<PlannedWorkoutItem>) -> Unit,
    onStart: (List<PlannedWorkoutItem>) -> Unit
) {
    val today = LocalDate.now().dayOfWeek
    val locale = if (isPersian) Locale.forLanguageTag("fa-IR") else Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val days = remember(firstDay) { List(7) { firstDay.plus(it.toLong()) } }
    val decoded = remember(savedDays) { savedDays.mapValues { WeeklyPlan.decode(it.value) } }
    var templateDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val forTemplate = templateDay
    if (forTemplate != null) {
        AlertDialog(
            onDismissRequest = { templateDay = null },
            title = { Text(stringResource(R.string.plan_choose_template)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                    StarterRoutine.entries.forEach { routine ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LayoutSpacing.touchTarget),
                            onClick = {
                                templateDay = null
                                onEdit(forTemplate, routine.exercises.mapNotNull(ExerciseCatalog::findById)
                                    .map { PlannedWorkoutItem(exercise = it) })
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
                Text(stringResource(R.string.plan_description), style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(days, key = { it.value }, contentType = { "weekday" }) { day ->
            val dayPlan = decoded[day.value].orEmpty()
            Surface(
                color = if (day == today) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(LayoutSpacing.screen),
                    verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                    val label = day.getDisplayName(TextStyle.FULL, locale)
                    Row(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (day == today) stringResource(R.string.plan_today, label) else label,
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
                            Button(onClick = { onStart(dayPlan) }) {
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

private enum class StarterRoutine(val title: Int, val exercises: List<String>) {
    FULL_BODY(R.string.plan_full_body, listOf("goblet_squat", "push_up", "dumbbell_bent_over_row", "forearm_plank")),
    UPPER(R.string.plan_upper, listOf("dumbbell_chest_press", "dumbbell_bent_over_row", "dumbbell_shoulder_press", "dumbbell_biceps_curl")),
    LOWER(R.string.plan_lower, listOf("goblet_squat", "dumbbell_romanian_deadlift", "reverse_lunge", "standing_calf_raise"))
}
