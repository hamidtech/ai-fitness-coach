package com.aifitnesscoach.feature.workout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.core.media.ExerciseIllustration
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aifitnesscoach.R
import com.aifitnesscoach.core.camera.WorkoutCameraState
import com.aifitnesscoach.core.camera.CameraPermissionState
import com.aifitnesscoach.core.camera.cameraPermissionState
import com.aifitnesscoach.core.camera.CameraPreviewWithOverlay
import com.aifitnesscoach.core.database.CameraLensPreference
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.equipment.ResistanceBandMetadata
import com.aifitnesscoach.core.equipment.ResistanceLevel
import com.aifitnesscoach.core.equipment.WeightEntry
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.exercise.CoachExplanationProvider
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.ExerciseMode
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.localization.LocaleManager
import com.aifitnesscoach.core.personalization.PersonalizedGreeting
import com.aifitnesscoach.core.pose.TrackingQuality
import com.aifitnesscoach.feature.exercises.statusLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun WorkoutScreen(
    uiState: WorkoutUiState,
    preferences: UserPreferences,
    isPersian: Boolean,
    coachExplanationProvider: CoachExplanationProvider,
    viewModel: WorkoutViewModel,
    onCameraGuideAcknowledged: () -> Unit = {},
    onOpenCameraGuide: (String) -> Unit = {},
    onPoseTrackingActiveChanged: (Boolean) -> Unit = {},
    onSaveAndAnalyze: () -> Unit = {},
    onExitWorkout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    var hasCameraPermission by remember { mutableStateOf(hasPermission()) }
    var requestedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        requestedPermission = true
        hasCameraPermission = granted
        Log.i("WorkoutCamera", "permission_result granted=$granted")
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasCameraPermission = hasPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    val permissionState = cameraPermissionState(
        granted = hasCameraPermission,
        requested = requestedPermission,
        showRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: true
    )
    val requestPermission = {
        if (permissionState == CameraPermissionState.SETTINGS_REQUIRED) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val requiresAnalysis = uiState.currentItem?.exercise?.analyzerId != null
    var useFrontCamera by rememberSaveable(preferences.cameraLens) {
        mutableStateOf(preferences.cameraLens == CameraLensPreference.FRONT)
    }

    LaunchedEffect(uiState.stage, hasCameraPermission, uiState.currentItem?.exercise?.analyzerId) {
        onPoseTrackingActiveChanged(uiState.stage == WorkoutStage.LIVE_TRACKING && hasCameraPermission &&
            uiState.currentItem?.exercise?.analyzerId != null)
    }
    DisposableEffect(Unit) {
        onDispose { onPoseTrackingActiveChanged(false) }
    }

    when (uiState.stage) {
        WorkoutStage.PLANNING -> {
            SessionBuilderView(
                uiState = uiState,
                isPersian = isPersian,
                viewModel = viewModel,
                skipCameraGuide = preferences.cameraGuideAcknowledged,
                onCancel = onExitWorkout
            )
        }

        WorkoutStage.CAMERA_SETUP_GUIDE -> {
            CameraSetupGuideView(
                uiState = uiState,
                isPersian = isPersian,
                hasCameraPermission = hasCameraPermission,
                requiresAnalysis = requiresAnalysis,
                onRequestPermission = requestPermission,
                permissionState = permissionState,
                onOpenGuide = { uiState.currentItem?.exercise?.id?.let(onOpenCameraGuide) },
                onStartTracking = {
                    onCameraGuideAcknowledged()
                    viewModel.startLiveTracking()
                },
                onBack = onExitWorkout
            )
        }

        WorkoutStage.LIVE_TRACKING -> {
            if (requiresAnalysis && !hasCameraPermission) {
                CameraPermissionRationaleView(
                    onRequestPermission = requestPermission,
                    permissionState = permissionState,
                    onOpenGuide = { uiState.currentItem?.exercise?.id?.let(onOpenCameraGuide) },
                    onBack = onExitWorkout
                )
            } else {
                LiveTrackingWorkoutView(
                    uiState = uiState,
                    preferences = preferences,
                    isPersian = isPersian,
                    useFrontCamera = useFrontCamera,
                    hasCameraPermission = hasCameraPermission,
                    onToggleCamera = { useFrontCamera = !useFrontCamera },
                    onOpenGuide = { uiState.currentItem?.exercise?.id?.let(onOpenCameraGuide) },
                    coachExplanationProvider = coachExplanationProvider,
                    viewModel = viewModel
                )
            }
        }

        WorkoutStage.REST_TIMER -> {
            RestTimerView(
                remainingSec = uiState.restRemainingSec,
                nextExerciseName = uiState.currentItem?.let {
                    if (isPersian) it.exercise.nameFa else it.exercise.nameEn
                }.orEmpty(),
                onSkipRest = { viewModel.skipRestTimer() },
                onFinishEarly = { viewModel.finishWorkoutEarly() }
            )
        }

        WorkoutStage.SUMMARY -> {
            WorkoutSummaryView(
                uiState = uiState,
                athleteName = preferences.profile.name,
                isPersian = isPersian,
                onSave = {
                    viewModel.saveWorkoutSession(
                        syncToHealthConnect = preferences.healthConnectEnabled &&
                            preferences.hcWriteWorkoutsEnabled &&
                            preferences.healthAutoSyncEnabled,
                        onSaved = onExitWorkout
                    )
                },
                onSaveAndAnalyze = {
                    viewModel.saveWorkoutSession(
                        syncToHealthConnect = preferences.healthConnectEnabled &&
                            preferences.hcWriteWorkoutsEnabled &&
                            preferences.healthAutoSyncEnabled,
                        onSaved = onSaveAndAnalyze
                    )
                },
                onDiscard = { viewModel.discardWorkout(onExitWorkout) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionBuilderView(
    uiState: WorkoutUiState,
    isPersian: Boolean,
    viewModel: WorkoutViewModel,
    skipCameraGuide: Boolean,
    onCancel: () -> Unit
) {
    var pickerExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Text(
            text = stringResource(R.string.home_build_session),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(10.dp))

        ExposedDropdownMenuBox(
            expanded = pickerExpanded,
            onExpandedChange = { pickerExpanded = it }
        ) {
            OutlinedButton(
                onClick = { pickerExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(stringResource(R.string.workout_add_exercise_all_categories))
            }
            ExposedDropdownMenu(
                expanded = pickerExpanded,
                onDismissRequest = { pickerExpanded = false }
            ) {
                ExerciseCatalog.allExercises.forEach { ex ->
                    DropdownMenuItem(
                        text = { Text(if (isPersian) ex.nameFa else ex.nameEn) },
                        leadingIcon = { ExerciseIllustration(ex.id, Modifier.size(44.dp)) },
                        onClick = {
                            viewModel.addExerciseToPlan(ex)
                            pickerExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(uiState.plannedItems, key = { _, item -> item.uid }) { index, item ->
                PlannedItemCard(
                    item = item,
                    index = index,
                    totalCount = uiState.plannedItems.size,
                    isPersian = isPersian,
                    onUpdate = { viewModel.updatePlanItem(it) },
                    onMoveUp = { viewModel.movePlanItem(index, -1) },
                    onMoveDown = { viewModel.movePlanItem(index, 1) },
                    onRemove = { viewModel.removePlanItem(item.uid) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.workout_discard))
            }
            Button(
                onClick = { viewModel.beginSessionFromPlan(skipCameraGuide = skipCameraGuide) },
                enabled = uiState.plannedItems.isNotEmpty(),
                modifier = Modifier.weight(2f)
            ) {
                Text(stringResource(R.string.home_start_workout))
            }
        }
    }
}

@Composable
private fun PlannedItemCard(
    item: PlannedWorkoutItem,
    index: Int,
    totalCount: Int,
    isPersian: Boolean,
    onUpdate: (PlannedWorkoutItem) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPersian) item.exercise.nameFa else item.exercise.nameEn,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Outlined.ArrowUpward, contentDescription = stringResource(R.string.workout_move_up))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Outlined.ArrowDownward, contentDescription = stringResource(R.string.workout_move_down))
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.common_delete))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.targetSets.toString(),
                    onValueChange = { v ->
                        v.toIntOrNull()?.let { onUpdate(item.copy(targetSets = it.coerceIn(1, 20))) }
                    },
                    label = { Text(stringResource(R.string.equip_sets)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                if (item.exercise.exerciseMode == ExerciseMode.TIMED_HOLD) {
                    OutlinedTextField(
                        value = item.targetHoldSec.toString(),
                        onValueChange = { v ->
                            v.toIntOrNull()?.let { onUpdate(item.copy(targetHoldSec = it.coerceIn(5, 600))) }
                        },
                        label = { Text(stringResource(R.string.equip_hold_sec)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedTextField(
                        value = item.targetReps.toString(),
                        onValueChange = { v ->
                            v.toIntOrNull()?.let { onUpdate(item.copy(targetReps = it.coerceIn(1, 100))) }
                        },
                        label = { Text(stringResource(R.string.equip_reps)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = item.restDurationSec.toString(),
                    onValueChange = { v ->
                        v.toIntOrNull()?.let { onUpdate(item.copy(restDurationSec = it.coerceIn(0, 600))) }
                    },
                    label = { Text(stringResource(R.string.equip_rest_sec)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Weighted Training Manual Entry (Per Dumbbell vs Total Load)
            if (item.exercise.category == ExerciseCategory.WEIGHTED_TRAINING) {
                val w = item.weightEntry ?: WeightEntry(5f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = w.value.toString(),
                        onValueChange = { v ->
                            v.toFloatOrNull()?.let { onUpdate(item.copy(weightEntry = w.copy(value = it))) }
                        },
                        label = { Text(stringResource(R.string.equip_weight_value)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = w.loadMode == WeightLoadMode.PER_DUMBBELL,
                        onClick = { onUpdate(item.copy(weightEntry = w.copy(loadMode = WeightLoadMode.PER_DUMBBELL))) },
                        label = { Text(stringResource(R.string.equip_weight_mode_per_dumbbell)) }
                    )
                    FilterChip(
                        selected = w.loadMode == WeightLoadMode.TOTAL_LOAD,
                        onClick = { onUpdate(item.copy(weightEntry = w.copy(loadMode = WeightLoadMode.TOTAL_LOAD))) },
                        label = { Text(stringResource(R.string.equip_weight_mode_total)) }
                    )
                }
            }

            // Resistance Band Metadata Entry (never inferred from color or camera)
            if (item.exercise.category == ExerciseCategory.RESISTANCE_BAND_TRAINING) {
                val band = item.bandMetadata ?: ResistanceBandMetadata()
                Text(stringResource(R.string.band_type_label), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.exercise.supportedBandTypes.forEach { bt ->
                        FilterChip(
                            selected = band.bandType == bt,
                            onClick = { onUpdate(item.copy(bandMetadata = band.copy(bandType = bt))) },
                            label = { Text(if (isPersian) bt.labelFa else bt.labelEn) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        ResistanceLevel.LIGHT,
                        ResistanceLevel.MEDIUM,
                        ResistanceLevel.HEAVY
                    ).forEach { lvl ->
                        FilterChip(
                            selected = band.resistanceLevel == lvl,
                            onClick = { onUpdate(item.copy(bandMetadata = band.copy(resistanceLevel = lvl))) },
                            label = { Text(if (isPersian) lvl.labelFa else lvl.labelEn) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = band.brand,
                        onValueChange = { onUpdate(item.copy(bandMetadata = band.copy(brand = it))) },
                        label = { Text(stringResource(R.string.band_brand_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = band.bandColor,
                        onValueChange = { onUpdate(item.copy(bandMetadata = band.copy(bandColor = it))) },
                        label = { Text(stringResource(R.string.band_color_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(R.string.band_metadata_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CameraSetupGuideView(
    uiState: WorkoutUiState,
    isPersian: Boolean,
    hasCameraPermission: Boolean,
    requiresAnalysis: Boolean,
    onRequestPermission: () -> Unit,
    permissionState: CameraPermissionState,
    onOpenGuide: () -> Unit,
    onStartTracking: () -> Unit,
    onBack: () -> Unit
) {
    val item = uiState.currentItem ?: return
    val ex = item.exercise

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    Modifier.fillMaxWidth().padding(LayoutSpacing.screen),
                    verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
                ) {
                    if (!hasCameraPermission && requiresAnalysis) {
                        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LayoutSpacing.touchTarget)) {
                            Text(stringResource(if (permissionState == CameraPermissionState.SETTINGS_REQUIRED) R.string.camera_open_settings else R.string.camera_grant_permission))
                        }
                    } else {
                        Button(onClick = onStartTracking, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LayoutSpacing.touchTarget)) {
                            Text(
                                stringResource(if (requiresAnalysis) R.string.camera_start_tracking else R.string.home_start_workout),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.workout_discard))
                    }
                }
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.camera_setup_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = if (isPersian) ex.nameFa else ex.nameEn,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            CameraFramingExample(ex.id, Modifier.align(Alignment.CenterHorizontally))
            CameraPositionInstructions(ex, isPersian)
            TextButton(onClick = onOpenGuide) { Text(stringResource(R.string.guide_full)) }

            if (ex.analyzerId == null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = stringResource(R.string.workout_no_analyzer_notice, statusLabel(ex.implementationStatus)),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRationaleView(
    onRequestPermission: () -> Unit,
    permissionState: CameraPermissionState,
    onOpenGuide: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(if (permissionState == CameraPermissionState.REQUIRED) {
                R.string.camera_permission_title
            } else {
                R.string.camera_access_denied
            }),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.camera_permission_rationale), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(if (permissionState == CameraPermissionState.SETTINGS_REQUIRED) R.string.camera_open_settings else R.string.camera_grant_permission))
        }
        TextButton(onClick = onOpenGuide) { Text(stringResource(R.string.camera_setup_title)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.workout_discard))
        }
    }
}

@Composable
private fun LiveTrackingWorkoutView(
    uiState: WorkoutUiState,
    preferences: UserPreferences,
    isPersian: Boolean,
    useFrontCamera: Boolean,
    hasCameraPermission: Boolean,
    onToggleCamera: () -> Unit,
    onOpenGuide: () -> Unit,
    coachExplanationProvider: CoachExplanationProvider,
    viewModel: WorkoutViewModel
) {
    val item = uiState.currentItem ?: return
    val analysis = uiState.latestAnalysis
    val ttsLocale = remember(preferences.language) { LocaleManager.activeLocale(preferences.language) }

    var timerNow by remember(uiState.manualHoldStartedAtMs) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    LaunchedEffect(uiState.manualHoldStartedAtMs) {
        if (!uiState.isManualHold || uiState.manualHoldStartedAtMs == null) return@LaunchedEffect
        while (isActive) {
            timerNow = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    var cameraAttempt by remember { mutableIntStateOf(0) }
    var cameraState by remember(useFrontCamera, cameraAttempt) {
        mutableStateOf(WorkoutCameraState.INITIALIZING_CAMERA)
    }
    val cameraFailed = cameraState in setOf(
        WorkoutCameraState.CAMERA_UNAVAILABLE, WorkoutCameraState.PREVIEW_ERROR, WorkoutCameraState.POSE_ERROR
    )
    val canCompleteSet = item.exercise.analyzerId == null || cameraState == WorkoutCameraState.WORKOUT_READY

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF111827))) {
        val headerLimit = maxHeight * 0.40f
        val controlsLimit = maxHeight * 0.55f
        if (hasCameraPermission) androidx.compose.runtime.key(cameraAttempt, item.exercise.id) {
            CameraPreviewWithOverlay(
                useFrontCamera = useFrontCamera,
                showSkeletonOverlay = preferences.skeletonOverlayEnabled && cameraState == WorkoutCameraState.WORKOUT_READY,
                latestPoseFrame = uiState.latestPoseFrame,
                needsPose = item.exercise.analyzerId != null,
                onPose = { frame ->
                    viewModel.onPoseFrame(frame, preferences.voiceFeedbackEnabled, ttsLocale, isPersian)
                },
                onFps = viewModel::onFpsUpdated,
                onState = { cameraState = it },
                skeletonColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top HUD (Exercise Name, Set, Tracking Status, Help, Camera Switch)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .heightIn(max = headerLimit)
                .background(Color.Black.copy(alpha = 0.62f))
                .verticalScroll(rememberScrollState())
                .padding(LayoutSpacing.screen)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) item.exercise.nameFa else item.exercise.nameEn,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.workout_set_label, uiState.currentSetNumber, item.targetSets),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row {
                    IconButton(
                        onClick = onOpenGuide,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            Icons.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.camera_setup_title),
                            tint = Color.White
                        )
                    }
                    if (hasCameraPermission) IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Cameraswitch,
                            contentDescription = stringResource(R.string.camera_switch),
                            tint = Color.White
                        )
                    }
                }
            }

            val trackingText = if (!hasCameraPermission) stringResource(R.string.workout_manual_only) else when (cameraState) {
                WorkoutCameraState.INITIALIZING_CAMERA -> stringResource(R.string.camera_initializing)
                WorkoutCameraState.INITIALIZING_POSE -> stringResource(R.string.camera_pose_initializing)
                WorkoutCameraState.CAMERA_READY -> stringResource(R.string.camera_preview_ready)
                WorkoutCameraState.CAMERA_UNAVAILABLE -> stringResource(R.string.camera_lens_unavailable)
                WorkoutCameraState.PREVIEW_ERROR -> stringResource(R.string.camera_preview_error)
                WorkoutCameraState.POSE_ERROR -> stringResource(R.string.camera_pose_error)
                WorkoutCameraState.WORKOUT_READY -> when (analysis?.trackingQuality) {
                    TrackingQuality.GOOD -> stringResource(R.string.workout_tracking_ok)
                    TrackingQuality.LOW_CONFIDENCE -> stringResource(R.string.workout_tracking_low_conf)
                    TrackingQuality.LOST -> stringResource(R.string.workout_tracking_lost)
                    null -> stringResource(R.string.camera_enter_frame)
                }
            }
            Text(
                text = trackingText,
                color = if (analysis?.trackingQuality == TrackingQuality.GOOD) Color(0xFF86EFAC) else Color(0xFFFDE047),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (cameraFailed) {
                Button(onClick = { cameraAttempt++ }) { Text(stringResource(R.string.camera_retry)) }
            }

            if (preferences.developerMetricsEnabled && cameraState == WorkoutCameraState.WORKOUT_READY) {
                val avgConfPct = uiState.latestPoseFrame
                    ?.averageConfidence(item.exercise.requiredLandmarks)
                    ?.times(100f)
                    ?.roundToInt() ?: 0
                val phaseLabel = analysis?.phase?.name ?: "IDLE"
                Text(
                    text = "FPS: ${uiState.liveFps.toInt()} · Inf: ${uiState.latestPoseFrame?.inferenceTimeMs ?: 0}ms · Conf: $avgConfPct% · $phaseLabel · CPU",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Bottom HUD — Large Rep Counter / Timer readable from several feet away
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .heightIn(max = controlsLimit)
                .background(Color.Black.copy(alpha = 0.72f))
                .verticalScroll(rememberScrollState())
                .padding(LayoutSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (item.exercise.exerciseMode == ExerciseMode.TIMED_HOLD) {
                        val holdMs = if (uiState.isManualHold) uiState.manualHoldElapsedMs(timerNow)
                            else analysis?.holdDurationMs ?: 0L
                        val sec = holdMs / 1000L
                        Text(
                            text = "${sec}s",
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = stringResource(R.string.workout_hold_target_format, stringResource(R.string.workout_hold_label), item.targetHoldSec),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    } else {
                        val totalReps = (analysis?.repCount ?: 0) + uiState.manualRepAdjustment
                        Text(
                            text = totalReps.toString(),
                            color = Color.White,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = "${stringResource(R.string.workout_reps_label)} / ${item.targetReps}",
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = analysis?.phase?.let { if (isPersian) it.labelFa else it.labelEn } ?: "—",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    analysis?.primaryAngleDegrees?.let { angle ->
                        Text(
                            text = "${angle.toInt()}°",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            val liveTip = analysis?.activeFeedbackKey?.let {
                coachExplanationProvider.formatCoachingTip(it, isPersian)
            } ?: item.bandMetadata?.let { band ->
                "${if (isPersian) band.bandType.labelFa else band.bandType.labelEn} · ${if (isPersian) band.resistanceLevel.labelFa else band.resistanceLevel.labelEn}"
            }

            if (!liveTip.isNullOrBlank()) {
                Text(
                    text = liveTip,
                    color = Color(0xFFE0F2FE),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (item.exercise.analyzerId == null && !uiState.isManualHold) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.adjustManualReps(-1) },
                        modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                    ) { Text("-1", color = Color.White) }
                    OutlinedButton(
                        onClick = { viewModel.adjustManualReps(1) },
                        modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                    ) { Text("+1", color = Color.White) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.weight(1f).sizeIn(minHeight = LayoutSpacing.touchTarget)
                ) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = if (uiState.isPaused) stringResource(R.string.workout_resume) else stringResource(R.string.workout_pause),
                        color = Color.White
                    )
                }

                Button(
                    onClick = { viewModel.completeCurrentSet() },
                    enabled = canCompleteSet,
                    modifier = Modifier.weight(1.3f).sizeIn(minHeight = LayoutSpacing.touchTarget)
                ) {
                    Text(stringResource(R.string.workout_next_set), style = MaterialTheme.typography.titleMedium)
                }
            }
        }


    }
}

@Composable
private fun RestTimerView(
    remainingSec: Int,
    nextExerciseName: String,
    onSkipRest: () -> Unit,
    onFinishEarly: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(nextExerciseName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.workout_rest_remaining, remainingSec),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSkipRest, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(stringResource(R.string.workout_skip_rest))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onFinishEarly, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.workout_finish))
        }
    }
}

@Composable
private fun WorkoutSummaryView(
    uiState: WorkoutUiState,
    athleteName: String,
    isPersian: Boolean,
    onSave: () -> Unit,
    onSaveAndAnalyze: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.workout_summary_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = PersonalizedGreeting.workoutCompletionMessage(
                name = athleteName,
                setsCompleted = uiState.completedSets.size,
                isPersian = isPersian
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        uiState.completedSets.forEach { rec ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(
                            R.string.workout_summary_set_header,
                            if (isPersian) rec.item.exercise.nameFa else rec.item.exercise.nameEn,
                            rec.setNumber
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    val perf = if (rec.item.exercise.exerciseMode == ExerciseMode.TIMED_HOLD) {
                        stringResource(R.string.workout_hold_seconds_format, rec.holdDurationMs / 1000L)
                    } else {
                        stringResource(R.string.workout_reps_count_format, rec.completedReps)
                    }
                    Text(perf, color = MaterialTheme.colorScheme.primary)
                    rec.item.weightEntry?.let {
                        Text("${it.value} ${it.unit.symbolEn} (${it.loadMode.name})")
                    }
                    rec.item.bandMetadata?.let { b ->
                        Text("${if (isPersian) b.bandType.labelFa else b.bandType.labelEn} · ${if (isPersian) b.resistanceLevel.labelFa else b.resistanceLevel.labelEn}")
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(stringResource(R.string.workout_save_session))
        }
        OutlinedButton(
            onClick = onSaveAndAnalyze,
            enabled = !uiState.isSaving && uiState.completedSets.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(stringResource(R.string.workout_save_and_analyze))
        }
        OutlinedButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.workout_discard))
        }
    }
}
