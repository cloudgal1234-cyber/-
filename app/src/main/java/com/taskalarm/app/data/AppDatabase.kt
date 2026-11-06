package com.taskalarm.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        /** v2: settings for the step, math, memory and typing tasks. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN stepCount INTEGER NOT NULL DEFAULT 50")
                db.execSQL("ALTER TABLE alarms ADD COLUMN mathCount INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE alarms ADD COLUMN mathDifficulty INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE alarms ADD COLUMN memoryRounds INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE alarms ADD COLUMN typingCount INTEGER NOT NULL DEFAULT 2")
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                // Device-protected storage so alarms can be rescheduled and ring
                // after a reboot even before the user unlocks the phone.
                context.applicationContext.createDeviceProtectedStorageContext(),
                AppDatabase::class.java,
                "task_alarm.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
