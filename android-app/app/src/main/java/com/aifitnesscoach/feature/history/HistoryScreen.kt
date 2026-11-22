package com.aifitnesscoach.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.feature.exercises.categoryLabel
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    isPersian: Boolean,
    sessions: List<WorkoutSessionWithDetails>,
    onAnalyzeWorkouts: (Long?) -> Unit
) {
    var categoryFilter by remember { mutableStateOf<ExerciseCategory?>(null) }

    val filtered = remember(sessions, categoryFilter) {
        if (categoryFilter == null) sessions
        else sessions.filter { item ->
            item.sets.any { it.categoryName == categoryFilter?.name }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = { onAnalyzeWorkouts(null) }) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Text("  " + stringResource(R.string.ai_analyze_history_btn))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { categoryFilter = null },
                    label = { Text(stringResource(R.string.exercises_all_categories)) }
                )
            }
            items(ExerciseCategory.entries) { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                    label = { Text(categoryLabel(cat)) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(LayoutSpacing.pagePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.session.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isPersian) item.session.titleFa else item.session.titleEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val dateStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(item.session.startedAtEpochMs))
                            val durationMin = (item.session.totalDurationMs / 60_000L).coerceAtLeast(1L)
                            Text(
                                text = "$dateStr · ${stringResource(R.string.history_duration_min, durationMin.toInt())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))
                            item.sets.forEach { set ->
                                val ex = ExerciseCatalog.findById(set.exerciseId)
                                val exName = if (isPersian) ex?.nameFa ?: set.exerciseId else ex?.nameEn ?: set.exerciseId
                                val equipDetail = buildString {
                                    set.weightValue?.let { append(" · $it ${set.weightUnit ?: "kg"} (${set.weightLoadMode ?: ""})") }
                                    set.bandType?.let {
                                        append(" · $it")
                                        set.bandResistanceLevel?.let { lvl -> append(" [$lvl]") }
                                        set.bandColor?.takeIf { c -> c.isNotBlank() }?.let { c -> append(" ($c)") }
                                    }
                                }
                                val perfText = if (set.holdDurationMs > 0L) {
                                    stringResource(R.string.workout_hold_seconds_format, set.holdDurationMs / 1000L)
                                } else {
                                    stringResource(R.string.history_reps_ratio_format, set.completedReps, set.targetReps)
                                }
                                Text(
                                    text = stringResource(R.string.history_set_line_format, exName, set.setNumber, "$perfText$equipDetail"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (item.observations.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.history_observations),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                item.observations.take(4).forEach { obs ->
                                    Text(
                                        text = "· ${if (isPersian) obs.detailFa else obs.detailEn}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { onAnalyzeWorkouts(item.session.id) }) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                                Text("  " + stringResource(R.string.ai_analyze_this_workout))
                            }
                        }
                    }
                }
            }
        }
    }
}
