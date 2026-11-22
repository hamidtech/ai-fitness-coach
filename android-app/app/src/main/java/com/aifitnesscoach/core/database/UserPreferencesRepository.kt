package com.aifitnesscoach.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.pow

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_fitness_coach_prefs")

enum class UserGender {
    MALE,
    FEMALE,
    NEUTRAL
}

enum class AppLanguage {
    SYSTEM,
    EN,
    FA
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class CameraLensPreference {
    FRONT,
    BACK
}

enum class FitnessGoal {
    GENERAL_FITNESS,
    STRENGTH,
    MUSCLE_BUILDING,
    MOBILITY,
    CORE_STABILITY,
    WEIGHT_MANAGEMENT,
    CUSTOM
}

enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class MeasurementUnit {
    METRIC,
    IMPERIAL
}

enum class AiExecutionMode {
    RULE_BASED_ONLY,
    LOCAL_LLM,
    EXTERNAL_API
}

enum class AnalysisFrequency {
    DAILY,
    WEEKLY
}

enum class AnalysisRangeScope {
    LAST_DAY,
    LAST_7_DAYS,
    SINCE_LAST_ANALYSIS
}

enum class AppColorPalette {
    AUTO,
    ATHLETIC_COBALT,
    TEAL,
    VIOLET,
    NEUTRAL
}

data class UserProfile(
    val name: String = "",
    val gender: UserGender = UserGender.NEUTRAL,
    val age: Int? = null,
    val birthYear: Int? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val fitnessGoal: FitnessGoal = FitnessGoal.GENERAL_FITNESS,
    val customGoalLabel: String = "",
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val hasCompletedOnboarding: Boolean = false
) {
    val onboardingCompleted: Boolean get() = hasCompletedOnboarding

    /**
     * Informational BMI calculation only; never presented as a medical diagnosis.
     */
    val bmi: Float?
        get() {
            val h = heightCm ?: return null
            val w = weightKg ?: return null
            if (h <= 0f || w <= 0f) return null
            val heightM = h / 100f
            return w / heightM.pow(2)
        }
}

data class UserPreferences(
    val profile: UserProfile = UserProfile(),
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.AUTO,
    val useDynamicColor: Boolean = false,
    val measurementUnit: MeasurementUnit = MeasurementUnit.METRIC,
    val cameraLens: CameraLensPreference = CameraLensPreference.FRONT,
    val cameraGuideAcknowledged: Boolean = false,
    val voiceFeedbackEnabled: Boolean = true,
    val skeletonOverlayEnabled: Boolean = true,
    val developerMetricsEnabled: Boolean = false,
    // Daily English Motivational Quote Persistence
    val dailyQuoteDateIso: String? = null,
    val dailyQuoteId: String? = null,
    val recentDailyQuoteIds: List<String> = emptyList(),
    // Optional Health Connect
    val healthConnectEnabled: Boolean = false,
    val healthAutoSyncEnabled: Boolean = false,
    val hcWriteWorkoutsEnabled: Boolean = false,
    val hcReadWorkoutsEnabled: Boolean = false,
    val hcReadStepsEnabled: Boolean = false,
    val hcReadSleepEnabled: Boolean = false,
    val hcReadHeartRateEnabled: Boolean = false,
    val lastHealthSyncEpochMs: Long? = null,
    // Notifications
    val notificationsEnabled: Boolean = false,
    val notifyAnalysisComplete: Boolean = true,
    val notifyDownloads: Boolean = true,
    val notifyWorkoutReminder: Boolean = false,
    // AI Execution & Privacy
    val aiExecutionMode: AiExecutionMode = AiExecutionMode.RULE_BASED_ONLY,
    val activeLocalModelId: String? = null,
    val activeExternalProviderId: String? = null,
    val allowExternalAiSharing: Boolean = false,
    // Scheduled Nightly Analysis
    val scheduledAnalysisEnabled: Boolean = false,
    val scheduleHour: Int = 21,
    val scheduleMinute: Int = 30,
    val scheduleFrequency: AnalysisFrequency = AnalysisFrequency.DAILY,
    val scheduleProviderMode: AiExecutionMode = AiExecutionMode.RULE_BASED_ONLY,
    val scheduleWifiOnly: Boolean = true,
    val scheduleChargingRequired: Boolean = false,
    val scheduleScope: AnalysisRangeScope = AnalysisRangeScope.SINCE_LAST_ANALYSIS,
    val scheduleNotifyOnComplete: Boolean = true,
    val lastScheduledRunMs: Long? = null,
    val lastScheduledSuccessMs: Long? = null,
    val lastScheduledError: String? = null,
    val lastAnalyzedCount: Int = 0,
    val lastAnalyzedWatermarkMs: Long? = null
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val PERSON_NAME = stringPreferencesKey("person_name")
        val GENDER = stringPreferencesKey("person_gender")
        val AGE = intPreferencesKey("person_age")
        val BIRTH_YEAR = intPreferencesKey("person_birth_year")
        val HEIGHT_CM = floatPreferencesKey("person_height_cm")
        val WEIGHT_KG = floatPreferencesKey("person_weight_kg")
        val FITNESS_GOAL = stringPreferencesKey("person_fitness_goal")
        val CUSTOM_GOAL_LABEL = stringPreferencesKey("person_custom_goal_label")
        val EXPERIENCE_LEVEL = stringPreferencesKey("person_experience_level")
        val PROFILE_COMPLETED = booleanPreferencesKey("profile_completed")

        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("app_color_palette")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val MEASUREMENT_UNIT = stringPreferencesKey("measurement_unit")
        val CAMERA_LENS = stringPreferencesKey("camera_lens")
        val CAMERA_GUIDE_ACK = booleanPreferencesKey("camera_guide_acknowledged")
        val VOICE_FEEDBACK = booleanPreferencesKey("voice_feedback")
        val SKELETON_OVERLAY = booleanPreferencesKey("skeleton_overlay")
        val DEV_METRICS = booleanPreferencesKey("dev_metrics")

        val DAILY_QUOTE_DATE = stringPreferencesKey("daily_quote_date")
        val DAILY_QUOTE_ID = stringPreferencesKey("daily_quote_id")
        val DAILY_QUOTE_RECENT_IDS = stringPreferencesKey("daily_quote_recent_ids")

        val HC_ENABLED = booleanPreferencesKey("hc_enabled")
        val HC_AUTO_SYNC = booleanPreferencesKey("hc_auto_sync")
        val HC_WRITE_WORKOUTS = booleanPreferencesKey("hc_write_workouts")
        val HC_READ_WORKOUTS = booleanPreferencesKey("hc_read_workouts")
        val HC_READ_STEPS = booleanPreferencesKey("hc_read_steps")
        val HC_READ_SLEEP = booleanPreferencesKey("hc_read_sleep")
        val HC_READ_HEART_RATE = booleanPreferencesKey("hc_read_heart_rate")
        val HC_LAST_SYNC = stringPreferencesKey("hc_last_sync")

        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIF_ANALYSIS = booleanPreferencesKey("notif_analysis")
        val NOTIF_DOWNLOADS = booleanPreferencesKey("notif_downloads")
        val NOTIF_REMINDER = booleanPreferencesKey("notif_reminder")

        val AI_EXEC_MODE = stringPreferencesKey("ai_execution_mode")
        val ACTIVE_LOCAL_MODEL_ID = stringPreferencesKey("active_local_model_id")
        val ACTIVE_EXTERNAL_PROVIDER_ID = stringPreferencesKey("active_external_provider_id")
        val ALLOW_EXTERNAL_AI_SHARING = booleanPreferencesKey("allow_external_ai_sharing")

        val SCHED_ENABLED = booleanPreferencesKey("sched_enabled")
        val SCHED_HOUR = intPreferencesKey("sched_hour")
        val SCHED_MINUTE = intPreferencesKey("sched_minute")
        val SCHED_FREQUENCY = stringPreferencesKey("sched_frequency")
        val SCHED_PROVIDER_MODE = stringPreferencesKey("sched_provider_mode")
        val SCHED_WIFI_ONLY = booleanPreferencesKey("sched_wifi_only")
        val SCHED_CHARGING_REQ = booleanPreferencesKey("sched_charging_required")
        val SCHED_SCOPE = stringPreferencesKey("sched_scope")
        val SCHED_NOTIFY = booleanPreferencesKey("sched_notify")
        val SCHED_LAST_RUN_MS = longPreferencesKey("sched_last_run_ms")
        val SCHED_LAST_SUCCESS_MS = longPreferencesKey("sched_last_success_ms")
        val SCHED_LAST_ERROR = stringPreferencesKey("sched_last_error")
        val SCHED_LAST_COUNT = intPreferencesKey("sched_last_count")
        val SCHED_WATERMARK_MS = longPreferencesKey("sched_watermark_ms")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            profile = UserProfile(
                name = prefs[Keys.PERSON_NAME] ?: "",
                gender = enumOrDefault(prefs[Keys.GENDER], UserGender.NEUTRAL),
                age = prefs[Keys.AGE],
                birthYear = prefs[Keys.BIRTH_YEAR],
                heightCm = prefs[Keys.HEIGHT_CM],
                weightKg = prefs[Keys.WEIGHT_KG],
                fitnessGoal = enumOrDefault(prefs[Keys.FITNESS_GOAL], FitnessGoal.GENERAL_FITNESS),
                customGoalLabel = prefs[Keys.CUSTOM_GOAL_LABEL] ?: "",
                experienceLevel = enumOrDefault(prefs[Keys.EXPERIENCE_LEVEL], ExperienceLevel.BEGINNER),
                hasCompletedOnboarding = prefs[Keys.PROFILE_COMPLETED] ?: false
            ),
            language = enumOrDefault(prefs[Keys.LANGUAGE], AppLanguage.SYSTEM),
            themeMode = enumOrDefault(prefs[Keys.THEME_MODE], AppThemeMode.SYSTEM),
            colorPalette = enumOrDefault(prefs[Keys.COLOR_PALETTE], AppColorPalette.AUTO),
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
            measurementUnit = enumOrDefault(prefs[Keys.MEASUREMENT_UNIT], MeasurementUnit.METRIC),
            cameraLens = enumOrDefault(prefs[Keys.CAMERA_LENS], CameraLensPreference.FRONT),
            cameraGuideAcknowledged = prefs[Keys.CAMERA_GUIDE_ACK] ?: false,
            voiceFeedbackEnabled = prefs[Keys.VOICE_FEEDBACK] ?: true,
            skeletonOverlayEnabled = prefs[Keys.SKELETON_OVERLAY] ?: true,
            developerMetricsEnabled = prefs[Keys.DEV_METRICS] ?: false,
            dailyQuoteDateIso = prefs[Keys.DAILY_QUOTE_DATE]?.takeIf { it.isNotBlank() },
            dailyQuoteId = prefs[Keys.DAILY_QUOTE_ID]?.takeIf { it.isNotBlank() },
            recentDailyQuoteIds = prefs[Keys.DAILY_QUOTE_RECENT_IDS]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList(),
            healthConnectEnabled = prefs[Keys.HC_ENABLED] ?: false,
            healthAutoSyncEnabled = prefs[Keys.HC_AUTO_SYNC] ?: false,
            hcWriteWorkoutsEnabled = prefs[Keys.HC_WRITE_WORKOUTS] ?: false,
            hcReadWorkoutsEnabled = prefs[Keys.HC_READ_WORKOUTS] ?: false,
            hcReadStepsEnabled = prefs[Keys.HC_READ_STEPS] ?: false,
            hcReadSleepEnabled = prefs[Keys.HC_READ_SLEEP] ?: false,
            hcReadHeartRateEnabled = prefs[Keys.HC_READ_HEART_RATE] ?: false,
            lastHealthSyncEpochMs = prefs[Keys.HC_LAST_SYNC]?.toLongOrNull(),
            notificationsEnabled = prefs[Keys.NOTIF_ENABLED] ?: false,
            notifyAnalysisComplete = prefs[Keys.NOTIF_ANALYSIS] ?: true,
            notifyDownloads = prefs[Keys.NOTIF_DOWNLOADS] ?: true,
            notifyWorkoutReminder = prefs[Keys.NOTIF_REMINDER] ?: false,
            aiExecutionMode = enumOrDefault(prefs[Keys.AI_EXEC_MODE], AiExecutionMode.RULE_BASED_ONLY),
            activeLocalModelId = prefs[Keys.ACTIVE_LOCAL_MODEL_ID]?.takeIf { it.isNotBlank() },
            activeExternalProviderId = prefs[Keys.ACTIVE_EXTERNAL_PROVIDER_ID]?.takeIf { it.isNotBlank() },
            allowExternalAiSharing = prefs[Keys.ALLOW_EXTERNAL_AI_SHARING] ?: false,
            scheduledAnalysisEnabled = prefs[Keys.SCHED_ENABLED] ?: false,
            scheduleHour = (prefs[Keys.SCHED_HOUR] ?: 21).coerceIn(0, 23),
            scheduleMinute = (prefs[Keys.SCHED_MINUTE] ?: 30).coerceIn(0, 59),
            scheduleFrequency = enumOrDefault(prefs[Keys.SCHED_FREQUENCY], AnalysisFrequency.DAILY),
            scheduleProviderMode = enumOrDefault(prefs[Keys.SCHED_PROVIDER_MODE], AiExecutionMode.RULE_BASED_ONLY),
            scheduleWifiOnly = prefs[Keys.SCHED_WIFI_ONLY] ?: true,
            scheduleChargingRequired = prefs[Keys.SCHED_CHARGING_REQ] ?: false,
            scheduleScope = enumOrDefault(prefs[Keys.SCHED_SCOPE], AnalysisRangeScope.SINCE_LAST_ANALYSIS),
            scheduleNotifyOnComplete = prefs[Keys.SCHED_NOTIFY] ?: true,
            lastScheduledRunMs = prefs[Keys.SCHED_LAST_RUN_MS],
            lastScheduledSuccessMs = prefs[Keys.SCHED_LAST_SUCCESS_MS],
            lastScheduledError = prefs[Keys.SCHED_LAST_ERROR],
            lastAnalyzedCount = prefs[Keys.SCHED_LAST_COUNT] ?: 0,
            lastAnalyzedWatermarkMs = prefs[Keys.SCHED_WATERMARK_MS]
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PERSON_NAME] = profile.name.trim()
            prefs[Keys.GENDER] = profile.gender.name
            if (profile.age != null) prefs[Keys.AGE] = profile.age else prefs.remove(Keys.AGE)
            if (profile.birthYear != null) prefs[Keys.BIRTH_YEAR] = profile.birthYear else prefs.remove(Keys.BIRTH_YEAR)
            if (profile.heightCm != null) prefs[Keys.HEIGHT_CM] = profile.heightCm else prefs.remove(Keys.HEIGHT_CM)
            if (profile.weightKg != null) prefs[Keys.WEIGHT_KG] = profile.weightKg else prefs.remove(Keys.WEIGHT_KG)
            prefs[Keys.FITNESS_GOAL] = profile.fitnessGoal.name
            prefs[Keys.CUSTOM_GOAL_LABEL] = profile.customGoalLabel.trim()
            prefs[Keys.EXPERIENCE_LEVEL] = profile.experienceLevel.name
            prefs[Keys.PROFILE_COMPLETED] = true
        }
    }

    suspend fun markOnboardingSkipped() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_COMPLETED] = true
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_COMPLETED] = completed
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setColorPalette(palette: AppColorPalette) {
        context.dataStore.edit { it[Keys.COLOR_PALETTE] = palette.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun saveDailyQuoteSelection(dateIso: String, quoteId: String, recentIds: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_QUOTE_DATE] = dateIso
            prefs[Keys.DAILY_QUOTE_ID] = quoteId
            prefs[Keys.DAILY_QUOTE_RECENT_IDS] = recentIds.takeLast(14).joinToString(",")
        }
    }

    suspend fun setMeasurementUnit(unit: MeasurementUnit) {
        context.dataStore.edit { it[Keys.MEASUREMENT_UNIT] = unit.name }
    }

    suspend fun setCameraLens(lens: CameraLensPreference) {
        context.dataStore.edit { it[Keys.CAMERA_LENS] = lens.name }
    }

    suspend fun setCameraGuideAcknowledged(acknowledged: Boolean) {
        context.dataStore.edit { it[Keys.CAMERA_GUIDE_ACK] = acknowledged }
    }

    suspend fun setVoiceFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_FEEDBACK] = enabled }
    }

    suspend fun setSkeletonOverlay(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SKELETON_OVERLAY] = enabled }
    }

    suspend fun setDeveloperMetrics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEV_METRICS] = enabled }
    }

    suspend fun setHealthConnectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HC_ENABLED] = enabled }
    }

    suspend fun setHealthAutoSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HC_AUTO_SYNC] = enabled }
    }

    suspend fun setHcWriteWorkouts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HC_WRITE_WORKOUTS] = enabled }
    }

    suspend fun setHcReadWorkouts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HC_READ_WORKOUTS] = enabled }
    }

    suspend fun setHcReadSteps(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HC_READ_STEPS] = enabled }
    }

    suspend fun setHcReadSleep(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HC_READ_SLEEP] = enabled }
    }

    suspend fun setHcReadHeartRate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HC_READ_HEART_RATE] = enabled }
    }

    suspend fun setLastHealthSync(epochMs: Long) {
        context.dataStore.edit { it[Keys.HC_LAST_SYNC] = epochMs.toString() }
    }

    suspend fun setNotificationPreferences(
        enabled: Boolean? = null,
        analysisComplete: Boolean? = null,
        downloads: Boolean? = null,
        workoutReminder: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            enabled?.let { prefs[Keys.NOTIF_ENABLED] = it }
            analysisComplete?.let { prefs[Keys.NOTIF_ANALYSIS] = it }
            downloads?.let { prefs[Keys.NOTIF_DOWNLOADS] = it }
            workoutReminder?.let { prefs[Keys.NOTIF_REMINDER] = it }
        }
    }

    suspend fun setAiConfiguration(
        mode: AiExecutionMode? = null,
        activeLocalModelId: String? = null,
        clearLocalModelId: Boolean = false,
        activeExternalProviderId: String? = null,
        clearExternalProviderId: Boolean = false,
        allowExternalSharing: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            mode?.let { prefs[Keys.AI_EXEC_MODE] = it.name }
            if (clearLocalModelId) {
                prefs.remove(Keys.ACTIVE_LOCAL_MODEL_ID)
            } else if (activeLocalModelId != null) {
                prefs[Keys.ACTIVE_LOCAL_MODEL_ID] = activeLocalModelId
            }
            if (clearExternalProviderId) {
                prefs.remove(Keys.ACTIVE_EXTERNAL_PROVIDER_ID)
            } else if (activeExternalProviderId != null) {
                prefs[Keys.ACTIVE_EXTERNAL_PROVIDER_ID] = activeExternalProviderId
            }
            allowExternalSharing?.let { prefs[Keys.ALLOW_EXTERNAL_AI_SHARING] = it }
        }
    }

    suspend fun setScheduleConfiguration(
        enabled: Boolean? = null,
        hour: Int? = null,
        minute: Int? = null,
        frequency: AnalysisFrequency? = null,
        providerMode: AiExecutionMode? = null,
        wifiOnly: Boolean? = null,
        chargingRequired: Boolean? = null,
        scope: AnalysisRangeScope? = null,
        notifyOnComplete: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            enabled?.let { prefs[Keys.SCHED_ENABLED] = it }
            hour?.let { prefs[Keys.SCHED_HOUR] = it.coerceIn(0, 23) }
            minute?.let { prefs[Keys.SCHED_MINUTE] = it.coerceIn(0, 59) }
            frequency?.let { prefs[Keys.SCHED_FREQUENCY] = it.name }
            providerMode?.let { prefs[Keys.SCHED_PROVIDER_MODE] = it.name }
            wifiOnly?.let { prefs[Keys.SCHED_WIFI_ONLY] = it }
            chargingRequired?.let { prefs[Keys.SCHED_CHARGING_REQ] = it }
            scope?.let { prefs[Keys.SCHED_SCOPE] = it.name }
            notifyOnComplete?.let { prefs[Keys.SCHED_NOTIFY] = it }
        }
    }

    suspend fun recordScheduledAnalysisResult(
        runAtMs: Long,
        success: Boolean,
        analyzedCount: Int,
        newWatermarkMs: Long?,
        error: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCHED_LAST_RUN_MS] = runAtMs
            prefs[Keys.SCHED_LAST_COUNT] = analyzedCount
            if (success) {
                prefs[Keys.SCHED_LAST_SUCCESS_MS] = runAtMs
                prefs.remove(Keys.SCHED_LAST_ERROR)
                newWatermarkMs?.let { prefs[Keys.SCHED_WATERMARK_MS] = it }
            } else if (!error.isNullOrBlank()) {
                prefs[Keys.SCHED_LAST_ERROR] = error.take(400)
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
