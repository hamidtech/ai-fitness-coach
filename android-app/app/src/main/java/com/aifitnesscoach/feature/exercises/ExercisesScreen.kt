package com.aifitnesscoach.feature.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.equipment.ResistanceBandMetadata
import com.aifitnesscoach.core.equipment.ResistanceBandType
import com.aifitnesscoach.core.equipment.ResistanceLevel
import com.aifitnesscoach.core.equipment.WeightEntry
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.equipment.WeightUnit
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.ExerciseDefinition
import com.aifitnesscoach.core.exercise.ExerciseMode
import com.aifitnesscoach.core.exercise.ImplementationStatus
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.media.ExerciseDemoMediaView
import com.aifitnesscoach.feature.workout.PlannedWorkoutItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    isPersian: Boolean,
    downloadedAssets: Map<String, ExerciseAssetEntity> = emptyMap(),
    initialCategoryFilter: ExerciseCategory? = null,
    selectedExerciseForDetail: ExerciseDefinition? = null,
    onSelectExerciseForDetail: (ExerciseDefinition?) -> Unit,
    onStartWorkoutWithItem: (PlannedWorkoutItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) }

    val filtered = remember(searchQuery, selectedCategory) {
        ExerciseCatalog.allExercises.filter { ex ->
            val catMatch = selectedCategory == null || ex.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val queryMatch = q.isEmpty() ||
                ex.nameEn.lowercase().contains(q) ||
                ex.nameFa.lowercase().contains(q) ||
                ex.targetMusclesEn.any { it.lowercase().contains(q) } ||
                ex.targetMusclesFa.any { it.lowercase().contains(q) }
            catMatch && queryMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder = { Text(stringResource(R.string.exercises_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_clear))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(stringResource(R.string.exercises_all_categories)) }
                )
            }
            items(ExerciseCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(categoryLabel(cat)) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { ex ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectExerciseForDetail(ex) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isPersian) ex.nameFa else ex.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append(categoryLabel(ex.category))
                                append(" · ")
                                append(statusLabel(ex.implementationStatus))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        val muscles = if (isPersian) ex.targetMusclesFa else ex.targetMusclesEn
                        Text(
                            text = muscles.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (selectedExerciseForDetail != null) {
        ModalBottomSheet(onDismissRequest = { onSelectExerciseForDetail(null) }) {
            ExerciseDetailSheet(
                exercise = selectedExerciseForDetail,
                downloadedAsset = downloadedAssets[selectedExerciseForDetail.id],
                isPersian = isPersian,
                onStartWorkout = { configured ->
                    onSelectExerciseForDetail(null)
                    onStartWorkoutWithItem(configured)
                }
            )
        }
    }
}

@Composable
private fun ExerciseDetailSheet(
    exercise: ExerciseDefinition,
    downloadedAsset: ExerciseAssetEntity?,
    isPersian: Boolean,
    onStartWorkout: (PlannedWorkoutItem) -> Unit
) {
    var targetSets by remember(exercise.id) { mutableIntStateOf(3) }
    var targetReps by remember(exercise.id) { mutableIntStateOf(10) }
    var targetHoldSec by remember(exercise.id) { mutableIntStateOf(30) }
    var weightText by remember(exercise.id) { mutableStateOf("5") }
    var loadMode by remember(exercise.id) { mutableStateOf(WeightLoadMode.PER_DUMBBELL) }
    var selectedBandType by remember(exercise.id) {
        mutableStateOf(exercise.supportedBandTypes.firstOrNull() ?: ResistanceBandType.LONG_LOOP)
    }
    var selectedBandLevel by remember(exercise.id) { mutableStateOf(ResistanceLevel.MEDIUM) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isPersian) exercise.nameFa else exercise.nameEn,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "${categoryLabel(exercise.category)} · ${statusLabel(exercise.implementationStatus)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        ExerciseDemoMediaView(
            mediaRef = exercise.demoMedia,
            isPersian = isPersian,
            downloadedAsset = downloadedAsset
        )

        Text(
            text = if (isPersian) exercise.descriptionFa else exercise.descriptionEn,
            style = MaterialTheme.typography.bodyLarge
        )

        // Quick Set / Rep / Equipment Setup right inside Exercise Details
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.exercise_quick_configure),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetSets.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { targetSets = it.coerceIn(1, 20) } },
                        label = { Text(stringResource(R.string.equip_sets)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (exercise.exerciseMode == ExerciseMode.TIMED_HOLD) {
                        OutlinedTextField(
                            value = targetHoldSec.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { targetHoldSec = it.coerceIn(5, 600) } },
                            label = { Text(stringResource(R.string.equip_hold_sec)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedTextField(
                            value = targetReps.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { targetReps = it.coerceIn(1, 100) } },
                            label = { Text(stringResource(R.string.equip_reps)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (exercise.category == ExerciseCategory.WEIGHTED_TRAINING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(stringResource(R.string.equip_weight_value)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = loadMode == WeightLoadMode.PER_DUMBBELL,
                            onClick = { loadMode = WeightLoadMode.PER_DUMBBELL },
                            label = { Text(stringResource(R.string.equip_weight_mode_per_dumbbell)) }
                        )
                        FilterChip(
                            selected = loadMode == WeightLoadMode.TOTAL_LOAD,
                            onClick = { loadMode = WeightLoadMode.TOTAL_LOAD },
                            label = { Text(stringResource(R.string.equip_weight_mode_total)) }
                        )
                    }
                }

                if (exercise.category == ExerciseCategory.RESISTANCE_BAND_TRAINING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercise.supportedBandTypes.take(3).forEach { bt ->
                            FilterChip(
                                selected = selectedBandType == bt,
                                onClick = { selectedBandType = bt },
                                label = { Text(if (isPersian) bt.labelFa else bt.labelEn) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ResistanceLevel.LIGHT, ResistanceLevel.MEDIUM, ResistanceLevel.HEAVY).forEach { lvl ->
                            FilterChip(
                                selected = selectedBandLevel == lvl,
                                onClick = { selectedBandLevel = lvl },
                                label = { Text(if (isPersian) lvl.labelFa else lvl.labelEn) }
                            )
                        }
                    }
                }
            }
        }

        SectionBlock(
            title = stringResource(R.string.exercise_target_muscles),
            items = if (isPersian) exercise.targetMusclesFa else exercise.targetMusclesEn
        )

        if (exercise.supportedBandTypes.isNotEmpty()) {
            SectionBlock(
                title = stringResource(R.string.exercise_equipment),
                items = exercise.supportedBandTypes.map { if (isPersian) it.labelFa else it.labelEn }
            )
        }

        if (exercise.anchorSetupEn != null || exercise.requiresAnchor) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.exercise_band_setup_safety),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    val specific = if (isPersian) exercise.anchorSetupFa else exercise.anchorSetupEn
                    if (!specific.isNullOrBlank()) {
                        Text(specific, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = stringResource(R.string.exercise_band_anchor_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SectionBlock(
            title = stringResource(R.string.exercise_instructions),
            items = if (isPersian) exercise.instructionsFa else exercise.instructionsEn,
            numbered = true
        )

        SectionBlock(
            title = stringResource(R.string.exercise_common_mistakes),
            items = if (isPersian) exercise.commonMistakesFa else exercise.commonMistakesEn
        )

        val camView = if (isPersian) exercise.cameraGuide.recommendedView.labelFa else exercise.cameraGuide.recommendedView.labelEn
        val camDist = if (isPersian) exercise.cameraGuide.distanceFa else exercise.cameraGuide.distanceEn
        val camHeight = if (isPersian) exercise.cameraGuide.heightFa else exercise.cameraGuide.heightEn
        SectionBlock(
            title = stringResource(R.string.exercise_camera_guide),
            items = listOf(camView, camDist, camHeight)
        )

        SectionBlock(
            title = stringResource(R.string.exercise_tracking_capabilities),
            items = (if (isPersian) exercise.observableFormChecksFa else exercise.observableFormChecksEn) +
                (if (isPersian) exercise.limitationsFa else exercise.limitationsEn)
        )

        Button(
            onClick = {
                val configured = PlannedWorkoutItem(
                    exercise = exercise,
                    targetSets = targetSets,
                    targetReps = targetReps,
                    targetHoldSec = targetHoldSec,
                    weightEntry = if (exercise.category == ExerciseCategory.WEIGHTED_TRAINING) {
                        WeightEntry(
                            value = weightText.toFloatOrNull() ?: 5f,
                            unit = WeightUnit.KG,
                            loadMode = loadMode
                        )
                    } else null,
                    bandMetadata = if (exercise.category == ExerciseCategory.RESISTANCE_BAND_TRAINING) {
                        ResistanceBandMetadata(
                            bandType = selectedBandType,
                            resistanceLevel = selectedBandLevel
                        )
                    } else null
                )
                onStartWorkout(configured)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.exercise_start_camera_workout), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionBlock(
    title: String,
    items: List<String>,
    numbered: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        items.forEachIndexed { idx, line ->
            Text(
                text = if (numbered) "${idx + 1}. $line" else "• $line",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun categoryLabel(category: ExerciseCategory): String = when (category) {
    ExerciseCategory.WEIGHTED_TRAINING -> stringResource(R.string.cat_weighted)
    ExerciseCategory.BODYWEIGHT_TRAINING -> stringResource(R.string.cat_bodyweight)
    ExerciseCategory.CORE_TRAINING -> stringResource(R.string.cat_core)
    ExerciseCategory.RESISTANCE_BAND_TRAINING -> stringResource(R.string.cat_resistance_band)
}

@Composable
fun statusLabel(status: ImplementationStatus): String = when (status) {
    ImplementationStatus.IMPLEMENTED_NEEDS_VALIDATION -> stringResource(R.string.status_implemented)
    ImplementationStatus.EXPERIMENTAL -> stringResource(R.string.status_experimental)
    ImplementationStatus.COMING_SOON -> stringResource(R.string.status_coming_soon)
}
