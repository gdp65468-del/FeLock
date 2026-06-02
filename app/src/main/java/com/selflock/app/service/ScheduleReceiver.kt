package com.selflock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.selflock.app.vpn.BlocklistManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleReceiver : BroadcastReceiver() {

    @Inject lateinit var blocklistManager: BlocklistManager

    companion object {
        const val ACTION_SCHEDULE_REFRESH = "com.selflock.app.ACTION_SCHEDULE_REFRESH"
        const val ACTION_DAILY_RESET = "com.selflock.app.ACTION_DAILY_RESET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SCHEDULE_REFRESH, ACTION_DAILY_RESET -> {
                        blocklistManager.refreshBlocklist()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
