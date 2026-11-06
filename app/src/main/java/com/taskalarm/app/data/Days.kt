package com.taskalarm.app.data

import java.time.DayOfWeek

/** Week starts on Sunday (Israeli convention). */
object Days {
    val shortNames = listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
    val fullNames = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")

    const val ALL = 0b1111111
    const val WEEKDAYS_IL = 0b0011111 // Sunday–Thursday

    fun index(day: DayOfWeek): Int = day.value % 7 // MONDAY=1 … SUNDAY=7 -> 0

    fun isSet(mask: Int, index: Int): Boolean = mask and (1 shl index) != 0

    fun toggle(mask: Int, index: Int): Int = mask xor (1 shl index)

    fun describe(mask: Int): String = when (mask) {
        0 -> "פעם אחת"
        ALL -> "כל יום"
        WEEKDAYS_IL -> "ימי חול (א׳–ה׳)"
        0b1100000 -> "סופ״ש"
        else -> (0..6).filter { isSet(mask, it) }.joinToString(" ") { shortNames[it] }
    }
}
