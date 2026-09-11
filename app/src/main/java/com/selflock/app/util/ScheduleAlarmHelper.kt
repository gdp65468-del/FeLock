package com.selflock.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.service.ScheduleReceiver
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ScheduleAlarmHelper {
    fun scheduleAlarmsForRule(context: Context, alarmManager: AlarmManager, rule: LockoutRule) {
        if (!rule.isEnabled) return
        val start = rule.getStartTime()
        val end = rule.getEndTime()
        scheduleTimeAlarms(context, alarmManager, rule.id, start, end)
    }

    private fun scheduleTimeAlarms(context: Context, alarmManager: AlarmManager, ruleId: Long, start: LocalTime, end: LocalTime) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()

        val startMillis = today.atTime(start).atZone(zoneId).toInstant().toEpochMilli().let {
            if (it < System.currentTimeMillis()) it + 86400000L else it
        }
        val endMillis = today.atTime(end).atZone(zoneId).toInstant().toEpochMilli().let {
            if (it < System.currentTimeMillis()) it + 86400000L else it
        }

        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_REFRESH
        }
        val startPending = PendingIntent.getBroadcast(context, ruleId.toInt() * 2, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startMillis, startPending)

        val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_REFRESH
        }
        val endPending = PendingIntent.getBroadcast(context, ruleId.toInt() * 2 + 1, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, endPending)
    }

    fun scheduleDailyReset(context: Context, alarmManager: AlarmManager) {
        val zoneId = ZoneId.systemDefault()
        val midnightMillis = LocalDate.now().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_DAILY_RESET
        }
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightMillis, pending)
    }
}
