package com.taskalarm.app.data

import java.time.ZonedDateTime

object NextAlarmCalculator {
    /** Next moment strictly after [now] at which an alarm with this config should ring. */
    fun next(hour: Int, minute: Int, daysMask: Int, now: ZonedDateTime): ZonedDateTime {
        val zone = now.zone
        for (offset in 0..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (daysMask != 0 && !Days.isSet(daysMask, Days.index(date.dayOfWeek))) continue
            val candidate = date.atTime(hour, minute).atZone(zone)
            if (candidate.isAfter(now)) return candidate
        }
        // Unreachable for valid input; fall back to tomorrow.
        return now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(zone)
    }

    fun next(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime =
        next(alarm.hour, alarm.minute, alarm.daysMask, now)
}
