package com.aifitnesscoach.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aifitnesscoach.R
import com.aifitnesscoach.app.MainActivity

class AppNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ANALYSIS = "aifc_analysis_complete"
        const val CHANNEL_DOWNLOADS = "aifc_downloads"
        const val CHANNEL_REMINDERS = "aifc_workout_reminders"

        const val EXTRA_OPEN_ROUTE = "aifc_open_route"
        const val EXTRA_INSIGHT_ID = "aifc_insight_id"
        const val ROUTE_INSIGHTS = "insights"
    }

    fun ensureChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_ANALYSIS,
                context.getString(R.string.notif_channel_analysis_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_analysis_desc)
            },
            NotificationChannel(
                CHANNEL_DOWNLOADS,
                context.getString(R.string.notif_channel_downloads_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_downloads_desc)
            },
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_reminders_desc)
            }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyAnalysisComplete(
        title: String,
        body: String,
        insightId: String? = null
    ) {
        if (!hasNotificationPermission()) return
        ensureChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ROUTE, ROUTE_INSIGHTS)
            insightId?.let { putExtra(EXTRA_INSIGHT_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (insightId ?: "insights").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ANALYSIS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(1001, notification)
        }
    }

    fun notifyDownloadEvent(
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission()) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    fun notifyWorkoutReminder(title: String, body: String) {
        if (!hasNotificationPermission()) return
        ensureChannels()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(2001, notification)
        }
    }
}
