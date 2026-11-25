package com.aifitnesscoach.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.ui.ControlFlow
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.feature.workout.PlannedWorkoutItem

@Composable
internal fun AdaptiveTrainingPanel(
    profile: UserProfile,
    sessions: List<WorkoutSessionWithDetails>,
    isPersian: Boolean,
    onReview: (List<PlannedWorkoutItem>, Boolean) -> Unit
) {
    var focus by rememberSaveable { mutableStateOf(AdaptiveTraining.defaultFocus(profile)) }
    var minutes by rememberSaveable { mutableIntStateOf(20) }
    var dumbbells by rememberSaveable { mutableStateOf(false) }
    var bands by rememberSaveable { mutableStateOf(false) }
    var empty by rememberSaveable { mutableStateOf(false) }
    val recentlyTrained = remember(sessions) { AdaptiveTraining.recentlyTrained(sessions, System.currentTimeMillis()) }
    val lastCheck = remember(sessions) {
        sessions.filter { it.session.notes == AdaptiveTraining.CHECK_IN_TAG }
            .maxByOrNull { it.session.endedAtEpochMs }
    }
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(LayoutSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
            Text(stringResource(R.string.coach_plan_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.coach_plan_description), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.coach_focus), style = MaterialTheme.typography.labelLarge)
            ControlFlow {
                listOf(
                    TrainingFocus.FULL_BODY to R.string.plan_full_body,
                    TrainingFocus.UPPER to R.string.plan_upper,
                    TrainingFocus.LOWER to R.string.plan_lower,
                    TrainingFocus.CORE to R.string.coach_focus_core,
                    TrainingFocus.MOBILITY to R.string.coach_focus_mobility
                ).forEach { (option, res) ->
                    FilterChip(selected = focus == option, onClick = { focus = option },
                        label = { Text(stringResource(res)) })
                }
            }
            Text(stringResource(R.string.coach_duration), style = MaterialTheme.typography.labelLarge)
            ControlFlow {
                listOf(10, 20, 30, 45).forEach { value ->
                    FilterChip(selected = minutes == value, onClick = { minutes = value },
                        label = { Text(stringResource(R.string.coach_minutes, value)) })
                }
            }
            Text(stringResource(R.string.coach_equipment), style = MaterialTheme.typography.labelLarge)
            ControlFlow {
                FilterChip(selected = dumbbells, onClick = { dumbbells = !dumbbells },
                    label = { Text(stringResource(R.string.coach_dumbbells)) })
                FilterChip(selected = bands, onClick = { bands = !bands },
                    label = { Text(stringResource(R.string.coach_bands)) })
            }
            Text(stringResource(R.string.coach_recent_muscles), style = MaterialTheme.typography.labelLarge)
            Text(if (recentlyTrained.isEmpty()) stringResource(R.string.coach_no_recent)
                else recentlyTrained.joinToString(" · ") { it.label(isPersian) },
                style = MaterialTheme.typography.bodyMedium)
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                val suggestion = AdaptiveTraining.suggest(profile, TrainingOptions(focus, minutes, dumbbells, bands),
                    sessions, System.currentTimeMillis())
                empty = suggestion.isEmpty()
                if (!empty) onReview(suggestion, false)
            }) { Text(stringResource(R.string.coach_review_plan)) }
            if (empty) Text(stringResource(R.string.coach_no_match), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.check_in_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.check_in_description), style = MaterialTheme.typography.bodyMedium)
            if (lastCheck != null) {
                val reps = lastCheck.sets.filter { it.exerciseId == "bodyweight_squat" }.sumOf { it.completedReps }
                val seconds = lastCheck.sets.filter { it.exerciseId == "forearm_plank" }.sumOf { it.holdDurationMs } / 1000
                Text(stringResource(R.string.check_in_last, reps, seconds), style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onReview(AdaptiveTraining.checkIn(), true) }) {
                Text(stringResource(R.string.check_in_start))
            }
        }
    }
}
