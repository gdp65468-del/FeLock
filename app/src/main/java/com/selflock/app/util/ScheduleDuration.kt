package com.selflock.app.util

object ScheduleDuration {
    fun minutes(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Int {
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute
        return (end - start + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    fun isAllowed(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, limitToTwelveHours: Boolean): Boolean {
        val duration = minutes(startHour, startMinute, endHour, endMinute)
        return duration > 0 && (!limitToTwelveHours || duration <= MAX_SCHEDULE_MINUTES)
    }

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MAX_SCHEDULE_MINUTES = 12 * 60
}
