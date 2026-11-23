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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.outlined.History
import com.aifitnesscoach.core.equipment.ResistanceBandType
import com.aifitnesscoach.core.equipment.ResistanceLevel
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.equipment.WeightUnit
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
    showAnalytics: Boolean = true,
    onAnalyzeWorkouts: ((Long?) -> Unit)?
) {
    var wantsOverview by rememberSaveable { mutableStateOf(true) }
    val showProgress = wantsOverview && showAnalytics
    var categoryFilter by rememberSaveable { mutableStateOf<ExerciseCategory?>(null) }

    val filtered = remember(sessions, categoryFilter) {
        if (categoryFilter == null) sessions
        else sessions.filter { item ->
            item.sets.any { it.categoryName == categoryFilter?.name }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(LayoutSpacing.pagePadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(if (showAnalytics) R.string.nav_progress else R.string.nav_history), style = MaterialTheme.typography.headlineMedium)
            if (!showProgress && sessions.isNotEmpty() && onAnalyzeWorkouts != null) {
                TextButton(onClick = { onAnalyzeWorkouts(null) }) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.ai_analyze_history_btn))
                }
            }
        }

        if (showAnalytics) TabRow(selectedTabIndex = if (showProgress) 0 else 1) {
            Tab(selected = showProgress, onClick = { wantsOverview = true },
                text = { Text(stringResource(R.string.progress_overview)) })
            Tab(selected = !showProgress, onClick = { wantsOverview = false },
                text = { Text(stringResource(R.string.nav_history)) })
        }

        if (showProgress) {
            TrainingProgressView(sessions, isPersian)
            return
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
            Column(
                modifier = Modifier.padding(LayoutSpacing.pagePadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(if (sessions.isEmpty()) R.string.history_empty else R.string.history_filter_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sessions.isNotEmpty()) {
                    TextButton(onClick = { categoryFilter = null }) { Text(stringResource(R.string.common_clear)) }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(LayoutSpacing.pagePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.session.id }) { item ->
                    var expanded by rememberSaveable(item.session.id) { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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

                            TextButton(onClick = { expanded = !expanded }) {
                                Text(stringResource(if (expanded) R.string.history_collapse else R.string.history_expand, item.sets.size))
                            }
                            if (expanded) {
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))
                                item.sets.forEach { set ->
                                    val ex = ExerciseCatalog.findById(set.exerciseId)
                                    val exName = if (isPersian) ex?.nameFa ?: set.exerciseId else ex?.nameEn ?: set.exerciseId
                                    val equipDetail = buildList {
                                        set.weightValue?.let { weight ->
                                            val unit = WeightUnit.entries.firstOrNull { it.name == set.weightUnit }
                                            add("${java.text.NumberFormat.getNumberInstance().format(weight)} ${unit?.let { if (isPersian) it.symbolFa else it.symbolEn } ?: set.weightUnit.orEmpty()}")
                                            if (set.weightLoadMode == WeightLoadMode.PER_DUMBBELL.name) {
                                                add(stringResource(R.string.equip_weight_mode_per_dumbbell))
                                            } else if (set.weightLoadMode == WeightLoadMode.TOTAL_LOAD.name) {
                                                add(stringResource(R.string.equip_weight_mode_total))
                                            }
                                        }
                                        ResistanceBandType.entries.firstOrNull { it.name == set.bandType }?.let {
                                            add(if (isPersian) it.labelFa else it.labelEn)
                                        }
                                        ResistanceLevel.entries.firstOrNull { it.name == set.bandResistanceLevel }?.let {
                                            add(if (isPersian) it.labelFa else it.labelEn)
                                        }
                                        set.bandColor?.takeIf(String::isNotBlank)?.let(::add)
                                    }.joinToString(separator = " · ", prefix = " · ").takeUnless { it == " · " }.orEmpty()
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
                                if (onAnalyzeWorkouts != null) OutlinedButton(onClick = { onAnalyzeWorkouts(item.session.id) }) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                                    Text(stringResource(R.string.ai_analyze_this_workout), modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
