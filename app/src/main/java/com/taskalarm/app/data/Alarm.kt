package com.taskalarm.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** How the alarm (and its protection) is defeated: get up and photograph places in the home. */
enum class TaskType { PHOTO }

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int = 7,
    val minute: Int = 0,
    /** Bit 0 = Sunday … bit 6 = Saturday. 0 = one-time alarm. */
    val daysMask: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val taskType: TaskType = TaskType.PHOTO,
    /** Fixed mission from [PhotoCatalog]; null = a random mission every time. */
    val missionId: String? = null,
    /** Difficulty filter for random missions ([MissionDifficulty]). */
    @ColumnInfo(defaultValue = "0") val missionDifficulty: Int = MissionDifficulty.ANY,
    val sound: String = SoundIds.SIREN,
    /** Percent of the alarm stream's max volume forced while ringing. */
    val volumePercent: Int = 100,
    val vibrate: Boolean = true,
) {
    val isRepeating: Boolean get() = daysMask != 0

    /** An armed alarm is protected: disabling, editing or deleting it requires the task. */
    val isProtected: Boolean get() = enabled

    /** Short Hebrew description of the task. */
    val taskSummary: String
        get() = PhotoCatalog.mission(missionId)?.title
            ?: ("צילום אקראי" + if (missionDifficulty != MissionDifficulty.ANY) " (${MissionDifficulty.name(missionDifficulty)})" else "")
}

object SoundIds {
    const val SIREN = "builtin:siren"
    const val BEEP = "builtin:beep"
    const val CHIME = "builtin:chime"
    const val SYSTEM_DEFAULT = "system:default"
}
