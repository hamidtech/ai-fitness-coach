package com.aifitnesscoach.core.ai.schedule

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ai.analysis.AIAnalysisCoordinator
import com.aifitnesscoach.core.ai.analysis.AnalysisType
import com.aifitnesscoach.core.ai.analysis.AnalyzedHealthMetrics
import com.aifitnesscoach.core.ai.analysis.InsightStatus
import com.aifitnesscoach.core.ai.analysis.SavedInsightRepository
import com.aifitnesscoach.core.ai.external.AIProviderRepository
import com.aifitnesscoach.core.ai.local.LiteRtLmInferenceEngine
import com.aifitnesscoach.core.ai.local.LocalModelRepository
import com.aifitnesscoach.core.database.AiExecutionMode
import com.aifitnesscoach.core.database.AnalysisRangeScope
import com.aifitnesscoach.core.database.AppDatabase
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.localization.LocaleManager
import com.aifitnesscoach.core.notifications.AppNotificationManager
import com.aifitnesscoach.core.security.AndroidKeystoreSecretStore
import com.aifitnesscoach.integration.healthconnect.HealthConnectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ScheduledAnalysisWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val runAtMs = System.currentTimeMillis()
        val prefsRepo = UserPreferencesRepository(applicationContext)
        val prefs = prefsRepo.preferencesFlow.first()
        val isPersian = LocaleManager.isPersianActive(prefs.language)

        val db = AppDatabase.getInstance(applicationContext)
        val allSessions = db.workoutDao().observeAllSessionsWithDetails().first()
        val targetSessions = selectSessionsForScope(
            sessions = allSessions,
            scope = prefs.scheduleScope,
            watermarkMs = prefs.lastAnalyzedWatermarkMs,
            nowMs = runAtMs
        )

        if (targetSessions.isEmpty()) {
            prefsRepo.recordScheduledAnalysisResult(
                runAtMs = runAtMs,
                success = true,
                analyzedCount = 0,
                newWatermarkMs = prefs.lastAnalyzedWatermarkMs,
                error = null
            )
            return@withContext Result.success()
        }

        val notifications = AppNotificationManager(applicationContext)

        // Guard heavy Local LLM background execution on low battery or low available memory
        if (prefs.scheduleProviderMode == AiExecutionMode.LOCAL_LLM && !canRunHeavyLocalLlmInBackground()) {
            val msg = applicationContext.getString(R.string.schedule_local_deferred_msg)
            prefsRepo.recordScheduledAnalysisResult(
                runAtMs = runAtMs,
                success = false,
                analyzedCount = 0,
                newWatermarkMs = null,
                error = msg
            )
            if (prefs.notificationsEnabled && prefs.scheduleNotifyOnComplete) {
                notifications.notifyAnalysisComplete(
                    title = applicationContext.getString(R.string.notif_analysis_ready_title),
                    body = msg,
                    insightId = null
                )
            }
            return@withContext Result.success()
        }

        val secretStore = AndroidKeystoreSecretStore(applicationContext)
        val localEngine = LiteRtLmInferenceEngine(applicationContext)
        val localRepo = LocalModelRepository(
            context = applicationContext,
            localModelDao = db.localModelDao(),
            preferencesRepository = prefsRepo,
            secretStore = secretStore,
            inferenceEngine = localEngine
        )
        val providerRepo = AIProviderRepository(
            dao = db.aiProviderDao(),
            secretStore = secretStore,
            preferencesRepository = prefsRepo
        )
        val insightRepo = SavedInsightRepository(db.savedInsightDao())
        val coordinator = AIAnalysisCoordinator(
            localModelRepository = localRepo,
            localInferenceEngine = localEngine,
            externalProviderRepository = providerRepo,
            savedInsightRepository = insightRepo
        )

        // Optional background Health Connect read only if enabled & granted with background permission
        val hcMetrics = readOptionalBackgroundHealth(prefs)

        val rangeLabel = when (prefs.scheduleScope) {
            AnalysisRangeScope.LAST_DAY -> if (isPersian) "۲۴ ساعت گذشته" else "Last 24 Hours"
            AnalysisRangeScope.LAST_7_DAYS -> if (isPersian) "۷ روز گذشته" else "Last 7 Days"
            AnalysisRangeScope.SINCE_LAST_ANALYSIS -> if (isPersian) "از آخرین تحلیل تاکنون" else "Since Last Analysis"
        }

        val outcome = coordinator.runAnalysis(
            sessions = targetSessions,
            preferences = prefs,
            modeOverride = prefs.scheduleProviderMode,
            analysisType = AnalysisType.SCHEDULED_SUMMARY,
            isPersian = isPersian,
            sourceRangeLabel = rangeLabel,
            healthMetrics = hcMetrics,
            externalSharingConfirmedForThisCall = prefs.allowExternalAiSharing,
            saveOnSuccess = true
        )

        val latestWorkoutStart = targetSessions.maxOf { it.session.startedAtEpochMs }
        val succeeded = outcome.insight.status != InsightStatus.FAILED.name

        if (succeeded) {
            prefsRepo.recordScheduledAnalysisResult(
                runAtMs = runAtMs,
                success = true,
                analyzedCount = targetSessions.size,
                newWatermarkMs = latestWorkoutStart,
                error = null
            )
            if (prefs.notificationsEnabled && prefs.scheduleNotifyOnComplete) {
                notifications.notifyAnalysisComplete(
                    title = outcome.insight.summaryTitle,
                    body = outcome.insight.bodyText.take(180),
                    insightId = outcome.insight.id
                )
            }
            Result.success()
        } else {
            // Save transparent rule-based fallback if external/local timed out or wasn't authorized?
            // Keep failure visible rather than hiding errors
            prefsRepo.recordScheduledAnalysisResult(
                runAtMs = runAtMs,
                success = false,
                analyzedCount = 0,
                newWatermarkMs = null,
                error = outcome.failedExternalOrLocalReason ?: outcome.insight.errorMessage
            )
            Result.success()
        }
    }

    private suspend fun readOptionalBackgroundHealth(prefs: com.aifitnesscoach.core.database.UserPreferences): AnalyzedHealthMetrics? {
        if (!prefs.healthConnectEnabled) return null
        val hc = HealthConnectManager(applicationContext)
        if (!hc.canReadHealthDataInBackground()) return null
        val metrics = hc.readDashboardMetrics(
            readWorkouts = prefs.hcReadWorkoutsEnabled,
            readSteps = prefs.hcReadStepsEnabled,
            readSleep = prefs.hcReadSleepEnabled,
            readHeartRate = prefs.hcReadHeartRateEnabled
        )
        return AnalyzedHealthMetrics(
            todaySteps = metrics.todaySteps,
            recentSleepMinutes = metrics.latestSleepMinutes,
            recentHeartRateAvgBpm = metrics.latestHeartRateAvgBpm
        )
    }

    private fun canRunHeavyLocalLlmInBackground(): Boolean {
        val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        val isCharging = bm?.isCharging ?: true
        if (!isCharging && pct < 25) return false

        val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        return !memInfo.lowMemory && (memInfo.availMem / (1024L * 1024L)) >= 900L
    }

    companion object {
        fun selectSessionsForScope(
            sessions: List<WorkoutSessionWithDetails>,
            scope: AnalysisRangeScope,
            watermarkMs: Long?,
            nowMs: Long
        ): List<WorkoutSessionWithDetails> {
            return when (scope) {
                AnalysisRangeScope.LAST_DAY ->
                    sessions.filter { it.session.startedAtEpochMs >= nowMs - 24L * 60L * 60L * 1000L }
                AnalysisRangeScope.LAST_7_DAYS ->
                    sessions.filter { it.session.startedAtEpochMs >= nowMs - 7L * 24L * 60L * 60L * 1000L }
                AnalysisRangeScope.SINCE_LAST_ANALYSIS ->
                    if (watermarkMs == null) sessions
                    else sessions.filter { it.session.startedAtEpochMs > watermarkMs }
            }
        }
    }
}
