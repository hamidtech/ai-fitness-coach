package com.aifitnesscoach.core.ai.schedule

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aifitnesscoach.core.database.AiExecutionMode
import com.aifitnesscoach.core.database.AnalysisFrequency
import com.aifitnesscoach.core.database.UserPreferences
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

data class SchedulePolicySpec(
    val enabled: Boolean,
    val repeatIntervalDays: Long,
    val initialDelayMinutes: Long,
    val networkType: NetworkType,
    val requiresCharging: Boolean
)

/**
 * Schedules periodic workout analysis with Android WorkManager without exact-alarm abuse.
 * Respects unmetered/Wi-Fi, charging, and battery-not-low constraints.
 */
class AnalysisScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun syncSchedule(preferences: UserPreferences) {
        if (!preferences.scheduledAnalysisEnabled) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
            return
        }

        val spec = computePolicy(preferences)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(spec.networkType)
            .setRequiresCharging(spec.requiresCharging)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ScheduledAnalysisWorker>(
            spec.repeatIntervalDays,
            TimeUnit.DAYS,
            2,
            TimeUnit.HOURS
        )
            .setInitialDelay(spec.initialDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(TAG_SCHEDULED_ANALYSIS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<ScheduledAnalysisWorker>()
            .addTag(TAG_SCHEDULED_ANALYSIS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "aifc_scheduled_analysis_periodic"
        const val UNIQUE_IMMEDIATE_WORK_NAME = "aifc_scheduled_analysis_now"
        const val TAG_SCHEDULED_ANALYSIS = "aifc_scheduled_analysis"

        fun computePolicy(
            preferences: UserPreferences,
            now: ZonedDateTime = ZonedDateTime.now()
        ): SchedulePolicySpec {
            val intervalDays = if (preferences.scheduleFrequency == AnalysisFrequency.WEEKLY) 7L else 1L

            var nextRun = now
                .withHour(preferences.scheduleHour.coerceIn(0, 23))
                .withMinute(preferences.scheduleMinute.coerceIn(0, 59))
                .withSecond(0)
                .withNano(0)
            if (!nextRun.isAfter(now)) {
                nextRun = nextRun.plusDays(intervalDays)
            }
            val delayMin = Duration.between(now, nextRun).toMinutes().coerceAtLeast(15L)

            val needsNetwork = preferences.scheduleProviderMode == AiExecutionMode.EXTERNAL_API
            val netType = when {
                !needsNetwork -> NetworkType.NOT_REQUIRED
                preferences.scheduleWifiOnly -> NetworkType.UNMETERED
                else -> NetworkType.CONNECTED
            }

            return SchedulePolicySpec(
                enabled = preferences.scheduledAnalysisEnabled,
                repeatIntervalDays = intervalDays,
                initialDelayMinutes = delayMin,
                networkType = netType,
                requiresCharging = preferences.scheduleChargingRequired
            )
        }
    }
}
