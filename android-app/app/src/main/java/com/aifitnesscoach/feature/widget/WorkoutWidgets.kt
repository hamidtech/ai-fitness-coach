package com.aifitnesscoach.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aifitnesscoach.R
import com.aifitnesscoach.app.MainActivity
import com.aifitnesscoach.core.database.AppDatabase
import com.aifitnesscoach.core.database.AppThemeMode
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.localization.LocaleManager
import com.aifitnesscoach.core.motivation.VerifiedQuoteRepository
import com.aifitnesscoach.core.notifications.AppNotificationManager
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

abstract class WorkoutWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        WorkoutWidgetRefresh.request(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        WorkoutWidgetRefresh.request(context)
    }
}

class QuickWorkoutWidget : WorkoutWidgetProvider()
class MotivationWorkoutWidget : WorkoutWidgetProvider()

class WidgetClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED
        )) WorkoutWidgetRefresh.request(context)
    }
}

object WorkoutWidgetRefresh {
    private const val REFRESH_NOW = "fitness_widget_refresh"
    private const val REFRESH_DATE = "fitness_widget_local_midnight"

    fun request(context: Context) {
        if (widgetIds(context, QuickWorkoutWidget::class.java).isEmpty() &&
            widgetIds(context, MotivationWorkoutWidget::class.java).isEmpty()) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WorkoutWidgetWorker>().build()
        )
    }

    internal fun scheduleNextDay(context: Context, now: ZonedDateTime) {
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        val delay = Duration.between(now, tomorrow).toMillis().coerceAtLeast(1_000L)
        // Each day has a separate work name so a worker can enqueue the next day while it runs.
        val name = "$REFRESH_DATE-${tomorrow.toInstant().epochSecond}"
        WorkManager.getInstance(context).enqueueUniqueWork(
            name, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WorkoutWidgetWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        )
    }

    internal fun widgetIds(context: Context, provider: Class<out AppWidgetProvider>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, provider))
}

class WorkoutWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val compact = WorkoutWidgetRefresh.widgetIds(context, QuickWorkoutWidget::class.java)
        val extended = WorkoutWidgetRefresh.widgetIds(context, MotivationWorkoutWidget::class.java)
        if (compact.isEmpty() && extended.isEmpty()) return Result.success()

        val now = ZonedDateTime.now()
        val date = now.toLocalDate()
        val day = WidgetDay.of(date, now.zone)
        val activity = AppDatabase.getInstance(context).workoutDao()
            .getWidgetActivity(day.startMs, day.endMs)
        val repository = UserPreferencesRepository(context)
        val prefs = repository.preferencesFlow.first()
        val locale = LocaleManager.activeLocale(prefs.language)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
            uiMode = when (prefs.themeMode) {
                AppThemeMode.SYSTEM -> uiMode
                AppThemeMode.LIGHT -> (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
                AppThemeMode.DARK -> (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
        }
        val localized = context.createConfigurationContext(config)
        val motivation = if (extended.isEmpty()) null else
            repository.dailyQuote(date, VerifiedQuoteRepository())?.let { "${it.quoteTextEn}\n— ${it.author}" }

        val manager = AppWidgetManager.getInstance(context)
        val fixedTheme = prefs.themeMode != AppThemeMode.SYSTEM
        if (compact.isNotEmpty()) manager.updateAppWidget(compact, views(localized, activity, false, null, 0, fixedTheme))
        extended.forEach { id ->
            val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val quoteLines = if (height in 1..195) 3 else 5
            manager.updateAppWidget(id, views(localized, activity, true, motivation, quoteLines, fixedTheme))
        }
        WorkoutWidgetRefresh.scheduleNextDay(context, now)
        return Result.success()
    }

    private fun views(
        context: Context,
        data: WidgetActivity,
        extended: Boolean,
        quote: String?,
        quoteLines: Int,
        fixedTheme: Boolean
    ): RemoteViews {
        val resources = context.resources
        return RemoteViews(context.packageName, if (extended) R.layout.fitness_widget_motivation else R.layout.fitness_widget).apply {
            setInt(R.id.widget_root, "setLayoutDirection", resources.configuration.layoutDirection)
            setViewVisibility(R.id.widget_quote, if (extended && quote != null) View.VISIBLE else View.GONE)
            if (extended) setInt(R.id.widget_quote, "setMaxLines", quoteLines)
            if (fixedTheme) {
                val isDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                setInt(R.id.widget_root, "setBackgroundResource", if (isDark) R.drawable.widget_background_dark else R.drawable.widget_background_light)
                val ink = if (isDark) 0xFFEAF2FF.toInt() else 0xFF142743.toInt()
                val accent = if (isDark) 0xFFADC6FF.toInt() else 0xFF254A92.toInt()
                setTextColor(R.id.widget_heading, ink)
                setTextColor(R.id.widget_activity, ink)
                setTextColor(R.id.widget_quote, ink)
                setTextColor(R.id.widget_quick_start, accent)
            }
            setTextViewText(R.id.widget_heading, context.getString(R.string.widget_today))
            setTextViewText(R.id.widget_quick_start, context.getString(R.string.widget_start))
            setTextViewText(R.id.widget_activity, if (data.workouts == 0) {
                context.getString(R.string.widget_empty)
            } else {
                resources.getQuantityString(R.plurals.widget_activity, data.workouts, data.workouts, data.activeMinutes)
            })
            setTextViewText(R.id.widget_quote, quote)
            setOnClickPendingIntent(R.id.widget_quick_start, destination(context, WorkoutWidgetRoutes.QUICK_START, 1))
            setOnClickPendingIntent(R.id.widget_activity, destination(context, WorkoutWidgetRoutes.ACTIVITY, 2))
            setOnClickPendingIntent(R.id.widget_quote, destination(context, WorkoutWidgetRoutes.MOTIVATION, 3))
        }
    }

    private fun destination(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AppNotificationManager.EXTRA_OPEN_ROUTE, route)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
