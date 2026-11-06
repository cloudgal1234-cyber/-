package com.taskalarm.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** How the alarm (and its protection) is defeated. */
enum class TaskType(val title: String, val description: String) {
    BARCODE("סריקת ברקוד", "לקום ולסרוק מוצר שבחרת מראש"),
    SHAKE("טלטול", "לנער את הטלפון בחוזקה"),
    STEPS("צעדים", "ללכת מספר צעדים עם הטלפון"),
    MATH("תרגילי חשבון", "לפתור תרגילים כדי להעיר את המוח"),
    MEMORY("משחק זיכרון", "לחזור על רצף משבצות מהבהבות"),
    TYPING("הקלדת משפט", "להקליד משפט בדיוק כפי שמוצג"),
}

/** Difficulty for the math task. */
object MathDifficulty {
    const val EASY = 1
    const val MEDIUM = 2
    const val HARD = 3
    val names = listOf("קל", "בינוני", "קשה")
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
    val taskType: TaskType = TaskType.BARCODE,
    val barcodeValue: String? = null,
    val barcodeFormat: String? = null,
    val shakeCount: Int = 30,
    @ColumnInfo(defaultValue = "50") val stepCount: Int = 50,
    @ColumnInfo(defaultValue = "3") val mathCount: Int = 3,
    @ColumnInfo(defaultValue = "2") val mathDifficulty: Int = MathDifficulty.MEDIUM,
    @ColumnInfo(defaultValue = "3") val memoryRounds: Int = 3,
    @ColumnInfo(defaultValue = "2") val typingCount: Int = 2,
    val sound: String = SoundIds.SIREN,
    /** Percent of the alarm stream's max volume forced while ringing. */
    val volumePercent: Int = 100,
    val vibrate: Boolean = true,
) {
    val isRepeating: Boolean get() = daysMask != 0

    /** An armed alarm is protected: disabling, editing or deleting it requires the task. */
    val isProtected: Boolean get() = enabled

    /** Short Hebrew description of the task, e.g. "50 צעדים". */
    val taskSummary: String
        get() = when (taskType) {
            TaskType.BARCODE -> "סריקת ברקוד"
            TaskType.SHAKE -> "$shakeCount ניעורים"
            TaskType.STEPS -> "$stepCount צעדים"
            TaskType.MATH -> "$mathCount תרגילים (${MathDifficulty.names[(mathDifficulty - 1).coerceIn(0, 2)]})"
            TaskType.MEMORY -> "זיכרון · $memoryRounds סבבים"
            TaskType.TYPING -> "הקלדת $typingCount משפטים"
        }
}

object SoundIds {
    const val SIREN = "builtin:siren"
    const val BEEP = "builtin:beep"
    const val CHIME = "builtin:chime"
    const val SYSTEM_DEFAULT = "system:default"
}
