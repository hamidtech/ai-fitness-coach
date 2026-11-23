package com.aifitnesscoach.feature.exercises

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import com.aifitnesscoach.core.media.ExerciseArtwork
import com.aifitnesscoach.core.exercise.definitions.DiscoverableExercise
import com.aifitnesscoach.core.exercise.definitions.ExerciseSource
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import com.aifitnesscoach.core.exercise.definitions.MuscleRegion
import com.aifitnesscoach.core.exercise.definitions.discoveryEntry
import com.aifitnesscoach.core.exercise.definitions.equipmentLabel
import com.aifitnesscoach.core.exercise.definitions.searchKey
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import com.aifitnesscoach.core.ui.ControlFlow
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
    initialCategoryFilter: ExerciseCategory? = null,
    onCloseQuickPicker: (() -> Unit)? = null,
    pickerTitle: Int = R.string.widget_start,
    onSelectExerciseForDetail: (ExerciseDefinition) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) }

    val quickPicker = onCloseQuickPicker != null
    var source by rememberSaveable(quickPicker, initialCategoryFilter) {
        mutableStateOf(
            when {
                initialCategoryFilter != null -> null
                quickPicker && pickerTitle == R.string.widget_start -> ExerciseSource.BUILT_IN
                else -> ExerciseSource.HASAN
            }
        )
    }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var region by rememberSaveable { mutableStateOf<MuscleRegion?>(null) }
    var openFilter by rememberSaveable { mutableStateOf<DiscoveryFilter?>(null) }
    val catalogVersion by ExternalExerciseSources.version.collectAsStateWithLifecycle()
    val catalog = remember(catalogVersion) { ExerciseCatalog.allExercises.map(::discoveryEntry) }
    val catalogBySource = remember(catalog, source, selectedCategory) {
        catalog.filter {
            (source == null || it.source == source || it.source == ExerciseSource.BUILT_IN) &&
                (selectedCategory == null || it.exercise.category == selectedCategory)
        }
    }
    val equipmentOptions = remember(catalogBySource) {
        catalogBySource.flatMap { it.equipment }.distinct().sorted()
    }
    val filtered = remember(catalogBySource, searchQuery, equipment, region, quickPicker) {
        val query = searchKey(searchQuery)
        val matches = catalogBySource.filter { it.matches(query, equipment, region) }
        if (quickPicker) matches.sortedByDescending { it.exercise.analyzerId != null } else matches
    }

    val filter = openFilter
    if (filter != null) {
        val options = when (filter) {
            DiscoveryFilter.SOURCE -> listOf(null to sourceLabel(null)) +
                ExerciseSource.entries.filter { source -> catalog.any { it.source == source } }
                    .map { it.name to sourceLabel(it) }
            DiscoveryFilter.EQUIPMENT -> listOf(null to stringResource(R.string.discovery_all_equipment)) +
                equipmentOptions.map { it to equipmentLabel(it, isPersian) }
            DiscoveryFilter.MUSCLE -> listOf(null to stringResource(R.string.discovery_all_muscles)) +
                MuscleRegion.entries.map { it.name to it.label(isPersian) }
        }
        val current = when (filter) {
            DiscoveryFilter.SOURCE -> source?.name
            DiscoveryFilter.EQUIPMENT -> equipment
            DiscoveryFilter.MUSCLE -> region?.name
        }
        AlertDialog(
            onDismissRequest = { openFilter = null },
            title = { Text(stringResource(filter.title)) },
            text = {
                LazyColumn(
                    Modifier.heightIn(max = 320.dp).selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(options, key = { it.first ?: "all" }, contentType = { "filter_option" }) { (value, label) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .sizeIn(minHeight = LayoutSpacing.touchTarget)
                                .selectable(selected = current == value, role = Role.RadioButton, onClick = {
                                    when (filter) {
                                        DiscoveryFilter.SOURCE -> {
                                            source = value?.let(ExerciseSource::valueOf)
                                            equipment = null
                                        }
                                        DiscoveryFilter.EQUIPMENT -> equipment = value
                                        DiscoveryFilter.MUSCLE -> region = value?.let(MuscleRegion::valueOf)
                                    }
                                    openFilter = null
                                }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
                        ) {
                            RadioButton(selected = current == value, onClick = null)
                            Text(label, Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openFilter = null }) { Text(stringResource(R.string.common_back)) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onCloseQuickPicker != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = LayoutSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCloseQuickPicker) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
                }
                Text(stringResource(pickerTitle), style = MaterialTheme.typography.headlineSmall)
            }
        }
        if (!quickPicker) {
            Text(
                stringResource(R.string.nav_exercises),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = LayoutSpacing.pagePadding, top = 16.dp, end = LayoutSpacing.pagePadding)
            )
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(LayoutSpacing.pagePadding),
            verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
        ) {
            item(key = "filters", contentType = "filters") {
                Column(verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                    OutlinedButton(
                        onClick = { openFilter = DiscoveryFilter.SOURCE },
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LayoutSpacing.touchTarget)
                    ) {
                        Text(stringResource(R.string.discovery_source_value, sourceLabel(source)))
                    }
                    ControlFlow {
                        FilterChip(
                            selected = equipment != null,
                            onClick = { openFilter = DiscoveryFilter.EQUIPMENT },
                            label = { Text(equipment?.let { equipmentLabel(it, isPersian) }
                                ?: stringResource(R.string.discovery_equipment)) }
                        )
                        FilterChip(
                            selected = region != null,
                            onClick = { openFilter = DiscoveryFilter.MUSCLE },
                            label = { Text(region?.label(isPersian) ?: stringResource(R.string.discovery_muscles)) }
                        )
                        selectedCategory?.let { category ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedCategory = null },
                                label = { Text(categoryLabel(category)) },
                                trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) }
                            )
                        }
                    }
                    Text(stringResource(R.string.discovery_results, filtered.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (source != ExerciseSource.BUILT_IN) {
                        Text(stringResource(R.string.discovery_open_credit), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                        Text(stringResource(R.string.exercise_search_empty), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = {
                            searchQuery = ""
                            selectedCategory = null
                            equipment = null
                            region = null
                        }) { Text(stringResource(R.string.common_clear)) }
                    }
                }
            }
            if (!quickPicker) item(key = "refresh", contentType = "action") { CatalogRefreshPanel() }
            items(filtered, key = { it.exercise.id }, contentType = { "exercise" }) { entry ->
                ExerciseListItem(entry.exercise, isPersian, entry) { onSelectExerciseForDetail(entry.exercise) }
            }
        }

    }


}

@Composable
fun ExerciseListItem(
    exercise: ExerciseDefinition,
    isPersian: Boolean,
    entry: DiscoverableExercise = remember(exercise) { discoveryEntry(exercise) },
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(LayoutSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.screen)
        ) {
            ExerciseArtwork(exercise.id, Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isPersian) exercise.nameFa else exercise.nameEn, style = MaterialTheme.typography.titleMedium)
                val metadata = (entry.muscleRegions.take(1).map { it.label(isPersian) } +
                    entry.equipment.map { equipmentLabel(it, isPersian) }).joinToString(" · ")
                Text(
                    metadata,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.source != ExerciseSource.BUILT_IN) {
                    Text(sourceLabel(entry.source), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (exercise.analyzerId != null) {
                    Text(
                        stringResource(R.string.exercise_camera_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
internal fun ExerciseWorkoutSetup(
    exercise: ExerciseDefinition,
    downloadedAsset: ExerciseAssetEntity?,
    isPersian: Boolean,
    onBack: () -> Unit,
    onOpenMedia: (() -> Unit)?,
    onOpenCameraGuide: () -> Unit,
    recentLoad: WeightEntry? = null,
    onStartWorkout: (PlannedWorkoutItem) -> Unit
) {
    var targetSets by rememberSaveable(exercise.id) { mutableStateOf("3") }
    var targetReps by rememberSaveable(exercise.id) { mutableStateOf("10") }
    var targetHoldSec by rememberSaveable(exercise.id) { mutableStateOf("30") }
    var weightText by rememberSaveable(exercise.id) {
        mutableStateOf(restoredLoadKg(recentLoad)?.toString() ?: "5")
    }
    val entry = remember(exercise) { discoveryEntry(exercise) }
    val hasDumbbells = "dumbbell" in entry.equipment
    var loadMode by rememberSaveable(exercise.id) {
        mutableStateOf(if (hasDumbbells) recentLoad?.loadMode ?: WeightLoadMode.PER_DUMBBELL else WeightLoadMode.TOTAL_LOAD)
    }
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
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) launching = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
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
                text = entry.equipment.joinToString(" · ") { equipmentLabel(it, isPersian) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (entry.source != ExerciseSource.BUILT_IN) {
                Text(sourceLabel(entry.source), style = MaterialTheme.typography.labelLarge)
            }
            ExerciseDemoMediaView(
                mediaRef = exercise.demoMedia,
                exerciseId = exercise.id,
                downloadedAsset = downloadedAsset
            )

            if (onOpenMedia != null) {
                TextButton(onClick = onOpenMedia) { Text(stringResource(R.string.exercise_media_actions)) }
            }
            Text(
                stringResource(if (exercise.analyzerId != null) R.string.setup_camera_analysis else R.string.setup_manual_mode),
                style = MaterialTheme.typography.bodyMedium
            )
            if (entry.source == ExerciseSource.BUILT_IN) {
                TextButton(onClick = onOpenCameraGuide) { Text(stringResource(R.string.camera_setup_title)) }
            }

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
                            if (hasDumbbells) ControlFlow {
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
                        ControlFlow {
                            exercise.supportedBandTypes.take(3).forEach { bt ->
                                FilterChip(
                                    selected = selectedBandType == bt,
                                    onClick = { selectedBandType = bt },
                                    label = { Text(if (isPersian) bt.labelFa else bt.labelEn) }
                                )
                            }
                        }
                        ControlFlow {
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
                val description = if (isPersian) exercise.descriptionFa else exercise.descriptionEn
                if (description.isNotBlank()) Text(description)
                if (isPersian && entry.source != ExerciseSource.BUILT_IN) {
                    Text(stringResource(R.string.discovery_upstream_english),
                        style = MaterialTheme.typography.labelLarge)
                }
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
    if (items.isEmpty()) return
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
    ImplementationStatus.MANUAL_ONLY -> stringResource(R.string.setup_manual_mode)
}

private enum class DiscoveryFilter(val title: Int) {
    SOURCE(R.string.discovery_source),
    EQUIPMENT(R.string.discovery_equipment),
    MUSCLE(R.string.discovery_muscles)
}

@Composable
private fun sourceLabel(source: ExerciseSource?): String = when (source) {
    ExerciseSource.BUILT_IN -> stringResource(R.string.discovery_builtin)
    ExerciseSource.HASAN -> stringResource(R.string.discovery_hasan)
    ExerciseSource.OPEN_EXERCISE_BASE -> "OpenExerciseBase"
    ExerciseSource.OTHER -> stringResource(R.string.catalog_other_sources)
    null -> stringResource(R.string.discovery_all_sources)
}

private fun restoredLoadKg(load: WeightEntry?): Float? {
    if (load == null || !load.value.isFinite() || load.value < 0f) return null
    return if (load.unit == WeightUnit.KG) load.value else (load.value * 0.45359237f)
}
