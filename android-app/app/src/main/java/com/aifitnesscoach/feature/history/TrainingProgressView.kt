package com.aifitnesscoach.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.core.ui.MuscleArtwork
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TrainingProgressView(sessions: List<WorkoutSessionWithDetails>, isPersian: Boolean) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val progress = remember(sessions, today, zone) { summarizeTraining(sessions, today, zone) }
    val displayDate = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val decimal = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 } }
    val weeks = remember(today) { List(365) { today.minusDays(364L - it) }.chunked(7) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LayoutSpacing.pagePadding),
        verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
    ) {
        if (sessions.isEmpty()) {
            item(key = "progress_empty", contentType = "copy") { Text(stringResource(R.string.history_empty)) }
        } else {
            item(key = "progress_summary", contentType = "card") {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(LayoutSpacing.screen), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.progress_summary), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.progress_session_count, progress.sessionCount))
                        Text(stringResource(R.string.progress_streak, progress.currentStreak, progress.longestStreak))
                    }
                }
            }
            item(key = "progress_heatmap", contentType = "card") {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(LayoutSpacing.screen), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.progress_year), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.progress_heatmap_legend), style = MaterialTheme.typography.bodyMedium)
                        val recentWeeks = rememberLazyListState(initialFirstVisibleItemIndex = (weeks.size - 9).coerceAtLeast(0))
                        LazyRow(state = recentWeeks, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(weeks, key = { it.first().toEpochDay() }, contentType = { "week" }) { week ->
                                val active = week.filter { it in progress.minutesByDay }
                                val weekDescription = if (active.isEmpty()) {
                                    stringResource(R.string.progress_week_empty, week.first().format(displayDate))
                                } else active.joinToString { day ->
                                    "${day.format(displayDate)}: ${progress.minutesByDay.getValue(day)}"
                                }.let { stringResource(R.string.progress_week_minutes, it) }
                                Column(Modifier.semantics { contentDescription = weekDescription },
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    week.forEach { day ->
                                        val minutes = progress.minutesByDay[day]
                                        val color = when {
                                            minutes == null -> MaterialTheme.colorScheme.surfaceVariant
                                            minutes < 20L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            minutes < 45L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Box(Modifier.size(14.dp).background(color, RoundedCornerShape(3.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "progress_muscle", contentType = "card") {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(LayoutSpacing.screen),
                        verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                        Text(stringResource(R.string.progress_muscles), style = MaterialTheme.typography.titleLarge)
                        if (progress.setsByRegion.isEmpty()) {
                            Text(stringResource(R.string.progress_muscles_empty))
                        } else {
                            MuscleArtwork(progress.setsByRegion.keys, Modifier.size(104.dp))
                            progress.setsByRegion.entries.sortedByDescending { it.value }.forEach { (region, count) ->
                                Text(stringResource(R.string.progress_region_sets, region.label(isPersian), count))
                            }
                        }
                    }
                }
            }
            if (progress.records.isNotEmpty()) {
                item(key = "records_header", contentType = "copy") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.progress_strength), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.progress_strength_hint), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                items(progress.records, key = { "${it.exerciseId}-${it.mode}" }, contentType = { "record" }) { record ->
                    val exercise = ExerciseCatalog.findById(record.exerciseId)
                    val name = if (isPersian) exercise?.nameFa else exercise?.nameEn
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(LayoutSpacing.screen),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(name ?: record.exerciseId, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.progress_best_set, decimal.format(record.loadKg), record.reps))
                            Text(stringResource(
                                if (record.mode == WeightLoadMode.PER_DUMBBELL) R.string.progress_max_dumbbell else R.string.progress_max,
                                decimal.format(record.estimatedMaxKg)
                            ), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
