package com.taskalarm.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** How the alarm (and its protection) is defeated. */
enum class TaskType { BARCODE, SHAKE }

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int = 7,
    val minute: Int = 0,
    /** Bit 0 = Sunday … bit 6 = Saturday. 0 = one-time alarm. */
    val daysMask: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val taskType: TaskType = TaskType.BARCODE,
    val barcodeValue: String? = null,
    val barcodeFormat: String? = null,
    val shakeCount: Int = 30,
    val sound: String = SoundIds.SIREN,
    /** Percent of the alarm stream's max volume forced while ringing. */
    val volumePercent: Int = 100,
    val vibrate: Boolean = true,
) {
    val isRepeating: Boolean get() = daysMask != 0

    /** An armed alarm is protected: disabling, editing or deleting it requires the task. */
    val isProtected: Boolean get() = enabled
}

object SoundIds {
    const val SIREN = "builtin:siren"
    const val BEEP = "builtin:beep"
    const val CHIME = "builtin:chime"
    const val SYSTEM_DEFAULT = "system:default"
}
