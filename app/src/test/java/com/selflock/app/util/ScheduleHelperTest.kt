package com.selflock.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class ScheduleHelperTest {
    @Test
    fun scheduleIsActiveInsideSameDayWindow() {
        assertTrue(
            ScheduleHelper.isScheduleActive(
                LocalTime.of(10, 0),
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                listOf("MON")
            )
        )
    }

    @Test
    fun overnightWindowUsesPreviousScheduledDay() {
        assertTrue(
            ScheduleHelper.isScheduleActive(
                LocalTime.of(1, 0),
                DayOfWeek.TUESDAY,
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                listOf("MON")
            )
        )
    }

    @Test
    fun scheduleIsInactiveOutsideSelectedDays() {
        assertFalse(
            ScheduleHelper.isScheduleActive(
                LocalTime.of(10, 0),
                DayOfWeek.SATURDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                listOf("MON", "TUE", "WED", "THU", "FRI")
            )
        )
    }
}
