package com.selflock.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object ScheduleHelper {
    fun isInTimeWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (start.isBefore(end)) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    fun isScheduleActive(
        now: LocalTime,
        today: DayOfWeek,
        start: LocalTime,
        end: LocalTime,
        scheduledDays: List<String>
    ): Boolean {
        val isOvernight = !start.isBefore(end)
        val dayToCheck = if (isOvernight && now.isBefore(end)) {
            today.minus(1)
        } else {
            today
        }
        val dayAbbrev = dayToCheck.name.substring(0, 3).uppercase()
        return dayAbbrev in scheduledDays && isInTimeWindow(now, start, end)
    }

    fun minutesUntil(end: LocalTime): Long {
        val now = LocalTime.now()
        val minutes = java.time.Duration.between(now, end).toMinutes()
        return if (minutes >= 0) minutes else minutes + 1440
    }
}
