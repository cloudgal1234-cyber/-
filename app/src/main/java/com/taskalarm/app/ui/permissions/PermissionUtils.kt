package com.taskalarm.app.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskalarm.app.alarm.AlarmScheduler
import com.taskalarm.app.alarm.NotificationHelper

object PermissionUtils {
    fun hasCamera(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /** Step detector access (Android 10+). Optional: the steps task falls back to the accelerometer. */
    fun hasActivityRecognition(context: Context) = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun hasNotifications(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun hasExactAlarms(context: Context) = AlarmScheduler.canScheduleExact(context)

    fun hasFullScreen(context: Context) = NotificationHelper.canUseFullScreenIntent(context)

    /** Everything an alarm needs to ring reliably. */
    fun criticalMissing(context: Context): Boolean =
        !hasNotifications(context) || !hasExactAlarms(context) || !hasFullScreen(context) || !hasCamera(context)

    fun exactAlarmSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 31) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else appSettingsIntent(context)

    fun fullScreenSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        } else appSettingsIntent(context)

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun open(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { context.startActivity(appSettingsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}
