package com.taskalarm.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class, Place::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun placeDao(): PlaceDao

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

        /**
         * v3: tasks that can be done in bed (shake, math, memory, typing) were removed in favour of
         * get-out-of-bed missions. Such alarms become random missions; their settings columns are dropped.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `alarms_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `hour` INTEGER NOT NULL, " +
                        "`minute` INTEGER NOT NULL, `daysMask` INTEGER NOT NULL, `label` TEXT NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, `taskType` TEXT NOT NULL, `barcodeValue` TEXT, " +
                        "`barcodeFormat` TEXT, `stepCount` INTEGER NOT NULL DEFAULT 100, `missionId` TEXT, " +
                        "`missionDifficulty` INTEGER NOT NULL DEFAULT 0, `sound` TEXT NOT NULL, " +
                        "`volumePercent` INTEGER NOT NULL, `vibrate` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `alarms_new` (id, hour, minute, daysMask, label, enabled, taskType, " +
                        "barcodeValue, barcodeFormat, stepCount, missionId, missionDifficulty, sound, volumePercent, vibrate) " +
                        "SELECT id, hour, minute, daysMask, label, enabled, " +
                        "CASE WHEN taskType IN ('BARCODE', 'STEPS') THEN taskType ELSE 'MISSION' END, " +
                        "barcodeValue, barcodeFormat, stepCount, NULL, 0, sound, volumePercent, vibrate FROM `alarms`"
                )
                db.execSQL("DROP TABLE `alarms`")
                db.execSQL("ALTER TABLE `alarms_new` RENAME TO `alarms`")
            }
        }

        /**
         * v4: every task is a photo mission. Adds the registered places table and drops the
         * barcode/steps columns; existing alarms become random photo missions.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `places` (`objectKey` TEXT NOT NULL, `features` TEXT NOT NULL, " +
                        "`threshold` REAL NOT NULL, `photoFile` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`objectKey`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `alarms_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `hour` INTEGER NOT NULL, " +
                        "`minute` INTEGER NOT NULL, `daysMask` INTEGER NOT NULL, `label` TEXT NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, `taskType` TEXT NOT NULL, `missionId` TEXT, " +
                        "`missionDifficulty` INTEGER NOT NULL DEFAULT 0, `sound` TEXT NOT NULL, " +
                        "`volumePercent` INTEGER NOT NULL, `vibrate` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `alarms_new` (id, hour, minute, daysMask, label, enabled, taskType, missionId, " +
                        "missionDifficulty, sound, volumePercent, vibrate) " +
                        "SELECT id, hour, minute, daysMask, label, enabled, 'PHOTO', NULL, 0, sound, volumePercent, vibrate " +
                        "FROM `alarms`"
                )
                db.execSQL("DROP TABLE `alarms`")
                db.execSQL("ALTER TABLE `alarms_new` RENAME TO `alarms`")
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
    }
}
