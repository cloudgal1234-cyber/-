package com.taskalarm.app

import com.taskalarm.app.data.Days
import com.taskalarm.app.data.NextAlarmCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class NextAlarmCalculatorTest {
    private val zone = ZoneId.of("Asia/Jerusalem")
    // Friday 6 Nov 2026, 22:00
    private val now = ZonedDateTime.of(2026, 11, 6, 22, 0, 0, 0, zone)

    @Test fun oneTimeLaterToday() {
        val next = NextAlarmCalculator.next(23, 30, 0, now)
        assertEquals(ZonedDateTime.of(2026, 11, 6, 23, 30, 0, 0, zone), next)
    }

    @Test fun oneTimeAlreadyPassedGoesToTomorrow() {
        val next = NextAlarmCalculator.next(7, 0, 0, now)
        assertEquals(ZonedDateTime.of(2026, 11, 7, 7, 0, 0, 0, zone), next)
    }

    @Test fun exactNowIsNotReturned() {
        val next = NextAlarmCalculator.next(22, 0, 0, now)
        assertEquals(ZonedDateTime.of(2026, 11, 7, 22, 0, 0, 0, zone), next)
    }

    @Test fun weekdaysSkipFridayAndSaturday() {
        val next = NextAlarmCalculator.next(6, 30, Days.WEEKDAYS_IL, now)
        assertEquals(DayOfWeek.SUNDAY, next.dayOfWeek)
        assertEquals(ZonedDateTime.of(2026, 11, 8, 6, 30, 0, 0, zone), next)
    }

    @Test fun sameDayNextWeek() {
        val fridayOnly = 1 shl Days.index(DayOfWeek.FRIDAY)
        val next = NextAlarmCalculator.next(21, 0, fridayOnly, now)
        assertEquals(ZonedDateTime.of(2026, 11, 13, 21, 0, 0, 0, zone), next)
    }

    @Test fun dayIndexStartsOnSunday() {
        assertEquals(0, Days.index(DayOfWeek.SUNDAY))
        assertEquals(6, Days.index(DayOfWeek.SATURDAY))
    }
}
