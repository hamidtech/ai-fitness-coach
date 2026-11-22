package com.aifitnesscoach.feature.exercises

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import com.aifitnesscoach.core.media.ExerciseIllustration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import com.aifitnesscoach.core.ui.LayoutSpacing
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onCloseQuickPicker: (() -> Unit)? = null,
    selectedExerciseForDetail: ExerciseDefinition? = null,
    onSelectExerciseForDetail: (ExerciseDefinition?) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenCameraGuide: (String) -> Unit,
    onStartWorkoutWithItem: (PlannedWorkoutItem) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) }

    val quickPicker = onCloseQuickPicker != null
    val filtered = remember(searchQuery, selectedCategory, quickPicker) {
        ExerciseCatalog.allExercises.filter { ex ->
            val catMatch = selectedCategory == null || ex.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val queryMatch = q.isEmpty() ||
                ex.nameEn.lowercase().contains(q) ||
                ex.nameFa.lowercase().contains(q) ||
                ex.targetMusclesEn.any { it.lowercase().contains(q) } ||
                ex.targetMusclesFa.any { it.lowercase().contains(q) }
            catMatch && queryMatch
        }.let { list -> if (quickPicker) list.sortedByDescending { it.analyzerId != null } else list }
    }

    if (selectedExerciseForDetail != null) {
        val exercise = selectedExerciseForDetail
        val close = { onSelectExerciseForDetail(null) }
        BackHandler(onBack = close)
        ExerciseWorkoutSetup(
            exercise = exercise,
            downloadedAsset = downloadedAssets[exercise.id],
            isPersian = isPersian,
            onBack = close,
            onOpenMedia = { onOpenMedia(exercise.id) },
            onOpenCameraGuide = { onOpenCameraGuide(exercise.id) },
            onStartWorkout = onStartWorkoutWithItem
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onCloseQuickPicker != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = LayoutSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCloseQuickPicker) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
                }
                Text(stringResource(R.string.widget_start), style = MaterialTheme.typography.headlineSmall)
            }
        }
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
            contentPadding = PaddingValues(LayoutSpacing.pagePadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { ex ->
                ExerciseListItem(ex, isPersian) { onSelectExerciseForDetail(ex) }
            }
        }
    }


}

@Composable
fun ExerciseListItem(exercise: ExerciseDefinition, isPersian: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(LayoutSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.screen)
        ) {
            ExerciseIllustration(exercise.id, Modifier.size(76.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isPersian) exercise.nameFa else exercise.nameEn, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${categoryLabel(exercise.category)} · ${statusLabel(exercise.implementationStatus)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseWorkoutSetup(
    exercise: ExerciseDefinition,
    downloadedAsset: ExerciseAssetEntity?,
    isPersian: Boolean,
    onBack: () -> Unit,
    onOpenMedia: () -> Unit,
    onOpenCameraGuide: () -> Unit,
    onStartWorkout: (PlannedWorkoutItem) -> Unit
) {
    var targetSets by rememberSaveable(exercise.id) { mutableStateOf("3") }
    var targetReps by rememberSaveable(exercise.id) { mutableStateOf("10") }
    var targetHoldSec by rememberSaveable(exercise.id) { mutableStateOf("30") }
    var weightText by rememberSaveable(exercise.id) { mutableStateOf("5") }
    var loadMode by rememberSaveable(exercise.id) { mutableStateOf(WeightLoadMode.PER_DUMBBELL) }
    var selectedBandType by rememberSaveable(exercise.id) {
        mutableStateOf(exercise.supportedBandTypes.firstOrNull() ?: ResistanceBandType.LONG_LOOP)
    }
    var selectedBandLevel by rememberSaveable(exercise.id) { mutableStateOf(ResistanceLevel.MEDIUM) }

    val sets = targetSets.toIntOrNull()
    val reps = targetReps.toIntOrNull()
    val hold = targetHoldSec.toIntOrNull()
    val weight = weightText.map { it.digitToIntOrNull()?.digitToChar() ?: if (it == '٫' || it == ',') '.' else it }.joinToString("")
        .toFloatOrNull()?.takeIf { it.isFinite() && it >= 0f }
    val problem = when (workoutSetupProblem(exercise.exerciseMode, exercise.category, sets, reps, hold, weight)) {
        WorkoutSetupProblem.SETS -> R.string.setup_valid_sets
        WorkoutSetupProblem.HOLD -> R.string.setup_valid_hold
        WorkoutSetupProblem.REPS -> R.string.setup_valid_reps
        WorkoutSetupProblem.WEIGHT -> R.string.setup_valid_weight
        null -> null
    }
    var launching by remember(exercise.id) { mutableStateOf(false) }
    var showTechnique by rememberSaveable(exercise.id) { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    Modifier.fillMaxWidth().padding(LayoutSpacing.screen),
                    verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
                ) {
                    if (problem != null) Text(stringResource(problem), color = MaterialTheme.colorScheme.error)
                    Button(
                        enabled = problem == null && !launching,
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LayoutSpacing.touchTarget),
                        onClick = start@{
                            if (launching || problem != null || sets == null) return@start
                            launching = true
                            val configured = PlannedWorkoutItem(
                                exercise = exercise,
                                targetSets = sets,
                                targetReps = reps ?: 10,
                                targetHoldSec = hold ?: 30,
                                weightEntry = if (exercise.category == ExerciseCategory.WEIGHTED_TRAINING) {
                                    WeightEntry(
                                        value = requireNotNull(weight),
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
                        }
                    ) {
                        Text(stringResource(R.string.home_start_workout))
                    }
                }
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(LayoutSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
            }
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
                exerciseId = exercise.id,
                downloadedAsset = downloadedAsset
            )

            TextButton(onClick = onOpenMedia) { Text(stringResource(R.string.exercise_media_actions)) }
            Text(
                stringResource(if (exercise.analyzerId != null) R.string.setup_camera_analysis else R.string.setup_manual_mode),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onOpenCameraGuide) { Text(stringResource(R.string.camera_setup_title)) }

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
                            value = targetSets,
                            onValueChange = { targetSets = it.filter(Char::isDigit).take(2) },
                            label = { Text(stringResource(R.string.equip_sets)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (exercise.exerciseMode == ExerciseMode.TIMED_HOLD) {
                            OutlinedTextField(
                                value = targetHoldSec,
                                onValueChange = { targetHoldSec = it.filter(Char::isDigit).take(3) },
                                label = { Text(stringResource(R.string.equip_hold_sec)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            OutlinedTextField(
                                value = targetReps,
                                onValueChange = { targetReps = it.filter(Char::isDigit).take(3) },
                                label = { Text(stringResource(R.string.equip_reps)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (exercise.category == ExerciseCategory.WEIGHTED_TRAINING) {
                        Column {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch in ".,٫" }.take(8) },
                                label = { Text(stringResource(R.string.equip_weight_value)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
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
                    }

                    if (exercise.category == ExerciseCategory.RESISTANCE_BAND_TRAINING) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                            exercise.supportedBandTypes.take(3).forEach { bt ->
                                FilterChip(
                                    selected = selectedBandType == bt,
                                    onClick = { selectedBandType = bt },
                                    label = { Text(if (isPersian) bt.labelFa else bt.labelEn) }
                                )
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
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

            TextButton(onClick = { showTechnique = !showTechnique }) {
                Text(stringResource(if (showTechnique) R.string.setup_hide_technique else R.string.setup_show_technique))
            }
            if (showTechnique) {
                Text(if (isPersian) exercise.descriptionFa else exercise.descriptionEn)
                SectionBlock(
                    title = stringResource(R.string.exercise_target_muscles),
                    items = if (isPersian) exercise.targetMusclesFa else exercise.targetMusclesEn
                )

                SectionBlock(
                    title = stringResource(R.string.exercise_instructions),
                    items = if (isPersian) exercise.instructionsFa else exercise.instructionsEn,
                    numbered = true
                )

                SectionBlock(
                    title = stringResource(R.string.exercise_common_mistakes),
                    items = if (isPersian) exercise.commonMistakesFa else exercise.commonMistakesEn
                )

                SectionBlock(
                    title = stringResource(R.string.exercise_tracking_capabilities),
                    items = (if (isPersian) exercise.observableFormChecksFa else exercise.observableFormChecksEn) +
                        (if (isPersian) exercise.limitationsFa else exercise.limitationsEn)
                )

            }
        }
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
