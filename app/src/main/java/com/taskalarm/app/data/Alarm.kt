package com.taskalarm.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** How the alarm (and its protection) is defeated. Every task requires getting out of bed. */
enum class TaskType(val title: String, val description: String) {
    MISSION("משימות בוקר", "${MissionLibrary.all.size} משימות: אור, הליכה, קפיצות וסיבובים"),
    BARCODE("ברקוד קבוע", "לסרוק מוצר מסוים שבחרת מראש"),
    STEPS("הליכה", "ללכת מספר צעדים עם הטלפון"),
}

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int = 7,
    val minute: Int = 0,
    /** Bit 0 = Sunday … bit 6 = Saturday. 0 = one-time alarm. */
    val daysMask: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val taskType: TaskType = TaskType.MISSION,
    val barcodeValue: String? = null,
    val barcodeFormat: String? = null,
    @ColumnInfo(defaultValue = "100") val stepCount: Int = 100,
    /** Fixed mission from [MissionLibrary]; null = a random mission every time. */
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
        get() = when (taskType) {
            TaskType.BARCODE -> "ברקוד קבוע"
            TaskType.STEPS -> "$stepCount צעדים"
            TaskType.MISSION -> MissionLibrary.get(missionId)?.title
                ?: "משימה אקראית" + if (missionDifficulty != MissionDifficulty.ANY) " (${MissionDifficulty.name(missionDifficulty)})" else ""
        }
}

object SoundIds {
    const val SIREN = "builtin:siren"
    const val BEEP = "builtin:beep"
    const val CHIME = "builtin:chime"
    const val SYSTEM_DEFAULT = "system:default"
}
