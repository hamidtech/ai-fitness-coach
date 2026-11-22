package com.aifitnesscoach.feature.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifitnesscoach.core.database.ExerciseSetEntity
import com.aifitnesscoach.core.database.FormObservationEntity
import com.aifitnesscoach.core.database.WorkoutDao
import com.aifitnesscoach.core.database.WorkoutSessionEntity
import com.aifitnesscoach.core.equipment.ResistanceBandMetadata
import com.aifitnesscoach.core.equipment.WeightEntry
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.equipment.WeightUnit
import com.aifitnesscoach.core.exercise.AnalysisResult
import com.aifitnesscoach.core.exercise.AnalyzerRegistry
import com.aifitnesscoach.core.exercise.CoachExplanationProvider
import com.aifitnesscoach.core.exercise.ExerciseAnalyzer
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.ExerciseDefinition
import com.aifitnesscoach.core.exercise.ExerciseMode
import com.aifitnesscoach.core.exercise.FormObservation
import com.aifitnesscoach.core.exercise.MovementPhase
import com.aifitnesscoach.core.media.TtsCoachManager
import com.aifitnesscoach.core.pose.PoseFrame
import com.aifitnesscoach.core.pose.TrackingQuality
import com.aifitnesscoach.integration.healthconnect.HealthConnectManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class PlannedWorkoutItem(
    val uid: String = UUID.randomUUID().toString(),
    val exercise: ExerciseDefinition,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetHoldSec: Int = 30,
    val restDurationSec: Int = 60,
    val weightEntry: WeightEntry? = if (exercise.category == ExerciseCategory.WEIGHTED_TRAINING) {
        WeightEntry(value = 5f, unit = WeightUnit.KG, loadMode = WeightLoadMode.PER_DUMBBELL)
    } else null,
    val bandMetadata: ResistanceBandMetadata? = if (exercise.category == ExerciseCategory.RESISTANCE_BAND_TRAINING) {
        ResistanceBandMetadata(bandType = exercise.supportedBandTypes.firstOrNull() ?: com.aifitnesscoach.core.equipment.ResistanceBandType.LONG_LOOP)
    } else null
)

data class CompletedSetRecord(
    val item: PlannedWorkoutItem,
    val setNumber: Int,
    val completedReps: Int,
    val holdDurationMs: Long,
    val observations: List<FormObservation>
)

enum class WorkoutStage {
    PLANNING,
    CAMERA_SETUP_GUIDE,
    LIVE_TRACKING,
    REST_TIMER,
    SUMMARY
}

data class WorkoutUiState(
    val stage: WorkoutStage = WorkoutStage.PLANNING,
    val plannedItems: List<PlannedWorkoutItem> = emptyList(),
    val currentItemIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val isPaused: Boolean = false,
    val restRemainingSec: Int = 0,
    val sessionStartedAtMs: Long = 0L,
    val completedSets: List<CompletedSetRecord> = emptyList(),
    val latestPoseFrame: PoseFrame? = null,
    val latestAnalysis: AnalysisResult? = null,
    val liveFps: Float = 0f,
    val manualRepAdjustment: Int = 0,
    val manualHoldStartedAtMs: Long? = null,
    val manualHoldAccumulatedMs: Long = 0L,
    val isSaving: Boolean = false
) {
    val currentItem: PlannedWorkoutItem? get() = plannedItems.getOrNull(currentItemIndex)

    val isManualHold: Boolean get() = currentItem?.exercise?.let {
        it.analyzerId == null && it.exerciseMode == ExerciseMode.TIMED_HOLD
    } == true

    fun manualHoldElapsedMs(nowMs: Long): Long = manualHoldAccumulatedMs +
        (manualHoldStartedAtMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)

    fun startManualHold(nowMs: Long): WorkoutUiState =
        if (isManualHold && stage == WorkoutStage.LIVE_TRACKING && !isPaused) {
            copy(manualHoldStartedAtMs = nowMs)
        } else {
            this
        }
}

class WorkoutViewModel(
    private val workoutDao: WorkoutDao,
    private val coachExplanationProvider: CoachExplanationProvider,
    private val ttsCoachManager: TtsCoachManager,
    private val healthConnectManager: HealthConnectManager,
    private val onWorkoutPersisted: () -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var activeAnalyzer: ExerciseAnalyzer? = null
    private var restTimerJob: Job? = null

    fun startQuickExercise(
        exercise: ExerciseDefinition,
        customItem: PlannedWorkoutItem? = null,
        skipCameraGuide: Boolean = false
    ) {
        restTimerJob?.cancel()
        val item = customItem ?: PlannedWorkoutItem(exercise = exercise, targetSets = 3)
        _uiState.value = WorkoutUiState(
            stage = if (skipCameraGuide || item.exercise.analyzerId == null) WorkoutStage.LIVE_TRACKING else WorkoutStage.CAMERA_SETUP_GUIDE,
            plannedItems = listOf(item),
            sessionStartedAtMs = System.currentTimeMillis()
        ).startManualHold(SystemClock.elapsedRealtime())
        prepareAnalyzerForCurrentItem()
    }

    fun openSessionBuilder(initialExercises: List<ExerciseDefinition> = emptyList()) {
        restTimerJob?.cancel()
        _uiState.value = WorkoutUiState(
            stage = WorkoutStage.PLANNING,
            plannedItems = initialExercises.map { PlannedWorkoutItem(exercise = it) }
        )
    }

    fun addExerciseToPlan(exercise: ExerciseDefinition) {
        _uiState.update { state ->
            state.copy(plannedItems = state.plannedItems + PlannedWorkoutItem(exercise = exercise))
        }
    }

    fun removePlanItem(uid: String) {
        _uiState.update { state ->
            state.copy(plannedItems = state.plannedItems.filterNot { it.uid == uid })
        }
    }

    fun movePlanItem(index: Int, delta: Int) {
        _uiState.update { state ->
            val list = state.plannedItems.toMutableList()
            val target = index + delta
            if (index !in list.indices || target !in list.indices) return@update state
            val removed = list.removeAt(index)
            list.add(target, removed)
            state.copy(plannedItems = list)
        }
    }

    fun updatePlanItem(updated: PlannedWorkoutItem) {
        _uiState.update { state ->
            state.copy(plannedItems = state.plannedItems.map { if (it.uid == updated.uid) updated else it })
        }
    }

    fun beginSessionFromPlan(skipCameraGuide: Boolean = false) {
        if (_uiState.value.plannedItems.isEmpty()) return
        _uiState.update {
            it.copy(
                stage = if (skipCameraGuide || it.plannedItems.first().exercise.analyzerId == null) {
                    WorkoutStage.LIVE_TRACKING
                } else {
                    WorkoutStage.CAMERA_SETUP_GUIDE
                },
                currentItemIndex = 0,
                currentSetNumber = 1,
                sessionStartedAtMs = System.currentTimeMillis(),
                completedSets = emptyList()
            ).startManualHold(SystemClock.elapsedRealtime())
        }
        prepareAnalyzerForCurrentItem()
    }

    fun startLiveTracking() {
        prepareAnalyzerForCurrentItem()
        _uiState.update {
            it.copy(
                stage = WorkoutStage.LIVE_TRACKING,
                isPaused = false,
                manualRepAdjustment = 0
            ).startManualHold(SystemClock.elapsedRealtime())
        }
    }

    fun togglePause() {
        val now = SystemClock.elapsedRealtime()
        _uiState.update { current ->
            val updated = current.copy(isPaused = !current.isPaused)
            when {
                !current.isManualHold -> updated
                updated.isPaused -> updated.copy(
                    manualHoldStartedAtMs = null,
                    manualHoldAccumulatedMs = current.manualHoldElapsedMs(now)
                )
                else -> updated.startManualHold(now)
            }
        }
    }

    fun adjustManualReps(delta: Int) {
        _uiState.update {
            it.copy(manualRepAdjustment = (it.manualRepAdjustment + delta).coerceAtLeast(0))
        }
    }

    fun onPoseFrame(
        frame: PoseFrame,
        voiceEnabled: Boolean,
        ttsLocale: Locale,
        isPersian: Boolean
    ) {
        val state = _uiState.value
        if (state.stage != WorkoutStage.LIVE_TRACKING || state.isPaused) {
            _uiState.update { it.copy(latestPoseFrame = frame) }
            return
        }

        val analyzer = activeAnalyzer
        if (analyzer == null) {
            _uiState.update { it.copy(latestPoseFrame = frame) }
            return
        }

        val result = analyzer.processFrame(frame)
        val prevRepCount = state.latestAnalysis?.repCount ?: 0

        _uiState.update {
            it.copy(
                latestPoseFrame = frame,
                latestAnalysis = result
            )
        }

        if (result.repCount > prevRepCount) {
            ttsCoachManager.speakIfReady(
                text = result.repCount.toString(),
                locale = ttsLocale,
                enabled = voiceEnabled,
                force = true
            )
        } else if (result.activeFeedbackKey != null && result.trackingQuality == TrackingQuality.GOOD) {
            val tip = coachExplanationProvider.formatCoachingTip(result.activeFeedbackKey, isPersian)
            if (result.phase == MovementPhase.PAUSED_TRACKING_LOST ||
                result.observations.lastOrNull()?.severity == com.aifitnesscoach.core.exercise.FeedbackSeverity.WARNING
            ) {
                ttsCoachManager.speakIfReady(tip, ttsLocale, voiceEnabled)
            }
        }
    }

    fun onFpsUpdated(fps: Float) {
        _uiState.update { it.copy(liveFps = fps) }
    }

    fun completeCurrentSet() {
        val state = _uiState.value
        if (state.stage != WorkoutStage.LIVE_TRACKING) return
        val item = state.currentItem ?: return
        val analysis = state.latestAnalysis

        val reps = if (item.exercise.exerciseMode == ExerciseMode.TIMED_HOLD) {
            0
        } else {
            (analysis?.repCount ?: 0) + state.manualRepAdjustment
        }
        val holdMs = if (state.isManualHold) {
            state.manualHoldElapsedMs(SystemClock.elapsedRealtime())
        } else {
            analysis?.holdDurationMs ?: 0L
        }

        val record = CompletedSetRecord(
            item = item,
            setNumber = state.currentSetNumber,
            completedReps = reps,
            holdDurationMs = holdMs,
            observations = analysis?.observations.orEmpty()
        )

        val updatedCompleted = state.completedSets + record
        val hasMoreSetsInExercise = state.currentSetNumber < item.targetSets
        val hasMoreExercises = state.currentItemIndex + 1 < state.plannedItems.size

        when {
            hasMoreSetsInExercise -> {
                _uiState.update {
                    it.copy(
                        completedSets = updatedCompleted,
                        currentSetNumber = state.currentSetNumber + 1,
                        stage = WorkoutStage.REST_TIMER,
                        restRemainingSec = item.restDurationSec
                    )
                }
                startRestTimer(item.restDurationSec)
            }

            hasMoreExercises -> {
                _uiState.update {
                    it.copy(
                        completedSets = updatedCompleted,
                        currentItemIndex = state.currentItemIndex + 1,
                        currentSetNumber = 1,
                        stage = WorkoutStage.REST_TIMER,
                        restRemainingSec = item.restDurationSec
                    )
                }
                startRestTimer(item.restDurationSec)
            }

            else -> {
                _uiState.update {
                    it.copy(
                        completedSets = updatedCompleted,
                        stage = WorkoutStage.SUMMARY
                    )
                }
            }
        }
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        if (seconds <= 0) {
            finishRestTimer()
            return
        }
        restTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1
                _uiState.update { it.copy(restRemainingSec = remaining) }
            }
            finishRestTimer()
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        finishRestTimer()
    }

    private fun finishRestTimer() {
        prepareAnalyzerForCurrentItem()
        _uiState.update {
            it.copy(
                stage = WorkoutStage.LIVE_TRACKING,
                restRemainingSec = 0,
                isPaused = false,
                manualRepAdjustment = 0,
                manualHoldStartedAtMs = null,
                manualHoldAccumulatedMs = 0L,
                latestAnalysis = null
            ).startManualHold(SystemClock.elapsedRealtime())
        }
    }

    fun finishWorkoutEarly() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(stage = WorkoutStage.SUMMARY) }
    }

    fun saveWorkoutSession(
        syncToHealthConnect: Boolean,
        onSaved: () -> Unit
    ) {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val startTime = state.sessionStartedAtMs.takeIf { it > 0L } ?: endTime
            val distinctExercises = state.completedSets.map { it.item.exercise }.distinctBy { it.id }
            val titleEn = when {
                distinctExercises.isEmpty() -> "Workout Session"
                distinctExercises.size == 1 -> distinctExercises.first().nameEn
                else -> "${distinctExercises.first().nameEn} + ${distinctExercises.size - 1} more"
            }
            val titleFa = when {
                distinctExercises.isEmpty() -> "جلسه تمرین"
                distinctExercises.size == 1 -> distinctExercises.first().nameFa
                else -> "${distinctExercises.first().nameFa} و ${distinctExercises.size - 1} حرکت دیگر"
            }

            val session = WorkoutSessionEntity(
                clientRecordId = "aifc_${UUID.randomUUID()}",
                titleEn = titleEn,
                titleFa = titleFa,
                startedAtEpochMs = startTime,
                endedAtEpochMs = endTime,
                totalDurationMs = (endTime - startTime).coerceAtLeast(1000L)
            )

            val sets = state.completedSets.map { rec ->
                ExerciseSetEntity(
                    sessionId = 0L,
                    exerciseId = rec.item.exercise.id,
                    categoryName = rec.item.exercise.category.name,
                    setNumber = rec.setNumber,
                    targetReps = rec.item.targetReps,
                    completedReps = rec.completedReps,
                    holdDurationMs = rec.holdDurationMs,
                    restDurationSec = rec.item.restDurationSec,
                    weightValue = rec.item.weightEntry?.value,
                    weightUnit = rec.item.weightEntry?.unit?.name,
                    weightLoadMode = rec.item.weightEntry?.loadMode?.name,
                    bandType = rec.item.bandMetadata?.bandType?.name,
                    bandBrand = rec.item.bandMetadata?.brand,
                    bandManufacturerLabel = rec.item.bandMetadata?.manufacturerLabel,
                    bandResistanceLevel = rec.item.bandMetadata?.resistanceLevel?.name,
                    bandColor = rec.item.bandMetadata?.bandColor,
                    bandCount = rec.item.bandMetadata?.bandCount,
                    bandRangeMin = rec.item.bandMetadata?.minManufacturerResistance,
                    bandRangeMax = rec.item.bandMetadata?.maxManufacturerResistance,
                    bandRangeUnit = rec.item.bandMetadata?.resistanceUnit?.name
                )
            }

            val observations = state.completedSets.flatMap { rec ->
                rec.observations.map { obs ->
                    FormObservationEntity(
                        sessionId = 0L,
                        exerciseId = rec.item.exercise.id,
                        severity = obs.severity.name,
                        detailEn = obs.detailEn,
                        detailFa = obs.detailFa,
                        timestampMs = obs.timestampMs
                    )
                }
            }

            val insertedId = workoutDao.saveFullWorkout(session, sets, observations)
            onWorkoutPersisted()

            if (syncToHealthConnect) {
                healthConnectManager.writeWorkoutSession(session.copy(id = insertedId), sets)
                    .onSuccess { recordId ->
                        workoutDao.markSessionSynced(insertedId, recordId)
                    }
            }

            _uiState.value = WorkoutUiState()
            onSaved()
        }
    }

    fun discardWorkout(onDone: () -> Unit) {
        restTimerJob?.cancel()
        _uiState.value = WorkoutUiState()
        onDone()
    }

    private fun prepareAnalyzerForCurrentItem() {
        val current = _uiState.value.currentItem ?: return
        activeAnalyzer = AnalyzerRegistry.createForExercise(current.exercise)
        activeAnalyzer?.reset()
        _uiState.update { it.copy(latestAnalysis = null) }
    }

    override fun onCleared() {
        super.onCleared()
        restTimerJob?.cancel()
    }
}
