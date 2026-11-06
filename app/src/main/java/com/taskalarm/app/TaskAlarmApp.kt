package com.taskalarm.app

import android.app.Application
import com.taskalarm.app.alarm.NotificationHelper

class TaskAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
