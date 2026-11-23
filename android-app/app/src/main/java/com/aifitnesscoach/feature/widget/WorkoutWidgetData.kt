package com.aifitnesscoach.feature.widget

import java.time.LocalDate
import java.time.ZoneId

data class WidgetActivity(val workouts: Int, val durationMs: Long) {
    val activeMinutes: Long get() = if (durationMs <= 0L) 0 else (durationMs - 1) / 60_000 + 1
}

data class WidgetDay(val startMs: Long, val endMs: Long) {
    companion object {
        fun of(date: LocalDate, zone: ZoneId) = WidgetDay(
            date.atStartOfDay(zone).toInstant().toEpochMilli(),
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }
}

object WorkoutWidgetRoutes {
    const val QUICK_START = "widget_quick_start"
    const val ACTIVITY = "widget_daily_activity"
    const val MOTIVATION = "widget_daily_motivation"
}
