package com.taskalarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.NextAlarmCalculator
import com.taskalarm.app.ui.MainActivity

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    const val ACTION_FIRE = "com.taskalarm.app.action.FIRE"
    const val EXTRA_ALARM_ID = "alarm_id"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun schedule(context: Context, alarm: Alarm) {
        val am = context.getSystemService(AlarmManager::class.java)
        val triggerAt = NextAlarmCalculator.next(alarm).toInstant().toEpochMilli()
        val operation = firePendingIntent(context, alarm.id)
        try {
            if (canScheduleExact(context)) {
                // setAlarmClock: exact, exempt from Doze, shows the alarm icon in the status bar,
                // and allows the receiver to start a foreground service.
                val showIntent = PendingIntent.getActivity(
                    context, alarm.id.toInt(),
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm not permitted, falling back to inexact", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel(context: Context, alarmId: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(firePendingIntent(context, alarmId))
    }

    private fun firePendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse("taskalarm://alarm/$alarmId"))
            .putExtra(EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
