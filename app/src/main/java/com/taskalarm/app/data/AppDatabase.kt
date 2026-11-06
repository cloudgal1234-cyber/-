package com.taskalarm.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                // Device-protected storage so alarms can be rescheduled and ring
                // after a reboot even before the user unlocks the phone.
                context.applicationContext.createDeviceProtectedStorageContext(),
                AppDatabase::class.java,
                "task_alarm.db"
            ).build().also { instance = it }
        }
    }
}
