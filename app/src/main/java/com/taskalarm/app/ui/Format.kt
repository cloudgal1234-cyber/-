package com.taskalarm.app.ui

import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val HebrewLocale: Locale = Locale.forLanguageTag("he-IL")
private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'ב'MMMM", HebrewLocale)

fun formatTime(hour: Int, minute: Int): String = String.format(Locale.US, "%02d:%02d", hour, minute)

fun formatDate(dateTime: ZonedDateTime): String = dateFormatter.format(dateTime)

fun formatCountdown(from: ZonedDateTime, to: ZonedDateTime): String {
    val total = Duration.between(from, to).toMinutes().coerceAtLeast(0) + 1
    val days = total / (60 * 24)
    val hours = (total % (60 * 24)) / 60
    val minutes = total % 60
    val parts = buildList {
        if (days > 0) add(if (days == 1L) "יום" else "$days ימים")
        if (hours > 0) add(if (hours == 1L) "שעה" else "$hours שעות")
        if (minutes > 0 && days == 0L) add(if (minutes == 1L) "דקה" else "$minutes דקות")
    }
    return "בעוד " + when (parts.size) {
        0 -> "פחות מדקה"
        1 -> parts[0]
        else -> parts.dropLast(1).joinToString(", ") + " ו־" + parts.last()
    }
}
