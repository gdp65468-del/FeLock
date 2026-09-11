package com.selflock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SCHEDULE_REFRESH = "com.selflock.app.ACTION_SCHEDULE_REFRESH"
        const val ACTION_DAILY_RESET = "com.selflock.app.ACTION_DAILY_RESET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SCHEDULE_REFRESH || intent.action == ACTION_DAILY_RESET) {
            context.startForegroundService(Intent(context, MonitoringService::class.java))
        }
    }
}
