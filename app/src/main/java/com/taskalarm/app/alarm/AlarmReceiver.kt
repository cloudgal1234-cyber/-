package com.taskalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (id < 0) return
        try {
            ContextCompat.startForegroundService(context, AlarmService.startIntent(context, id))
        } catch (e: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException when the alarm was scheduled inexactly.
            // The full-screen notification opens AlarmActivity, which starts the service from the foreground.
            Log.e("AlarmReceiver", "Could not start alarm service", e)
            NotificationHelper.postFallbackRinging(context, id)
        }
    }
}
