package com.taskalarm.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskalarm.app.R
import com.taskalarm.app.ui.ringing.AlarmActivity

object NotificationHelper {
    const val CHANNEL_ALARM = "alarm_ringing_v1"
    const val RINGING_NOTIFICATION_ID = 1001

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            setSound(null, null)          // the service plays the sound itself on the alarm stream
            enableVibration(false)        // the service vibrates itself
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    fun buildRinging(context: Context, alarmId: Long, label: String?, fromFallback: Boolean = false): Notification {
        val fullScreen = PendingIntent.getActivity(
            context, alarmId.toInt(),
            AlarmActivity.intent(context, alarmId, fromFallback),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = buildString {
            if (!label.isNullOrBlank()) append(label).append(" · ")
            append(context.getString(R.string.notif_ringing_text))
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.notif_ringing_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (!fromFallback) {
            // If the user swipes the notification away (possible on Android 14+), post it again.
            builder.setDeleteIntent(
                PendingIntent.getService(
                    context, 0, AlarmService.repostIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        return builder.build().apply { flags = flags or Notification.FLAG_NO_CLEAR }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun postFallbackRinging(context: Context, alarmId: Long) {
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) {
            nm.notify(RINGING_NOTIFICATION_ID, buildRinging(context, alarmId, null, fromFallback = true))
        }
    }
}
