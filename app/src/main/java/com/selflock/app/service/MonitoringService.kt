package com.selflock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.KeyguardManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.selflock.app.BlockOverlayActivity
import com.selflock.app.MainActivity
import com.selflock.app.R
import com.selflock.app.domain.usecase.LockoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var usageStatsManager: UsageStatsManager
    @Inject lateinit var lockoutManager: LockoutManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var lastPollTime: Long = 0

    companion object {
        const val CHANNEL_ID = "monitoring_service"
        const val NOTIFICATION_ID = 1002
        private const val POLL_INTERVAL_MS = 5000L

    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startPolling()
        return START_STICKY
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            lastPollTime = System.currentTimeMillis()
            while (true) {
                delay(POLL_INTERVAL_MS)
                pollUsageStats()
            }
        }
    }

    private suspend fun pollUsageStats() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = ((now - lastPollTime) / 1000).coerceAtMost(POLL_INTERVAL_MS / 1000)
        lastPollTime = now
        val foregroundPackage = getForegroundPackage() ?: return
        if (foregroundPackage == packageName) return
        val decision = lockoutManager.evaluate(foregroundPackage, now)
        if (decision.isBlocked) {
            val appName = runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(foregroundPackage, 0)).toString()
            }.getOrDefault("App")
            launchBlockOverlay(foregroundPackage, appName, decision)
        } else {
            val powerManager = getSystemService(PowerManager::class.java)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            if (powerManager.isInteractive && !keyguardManager.isDeviceLocked) {
                lockoutManager.recordProgress(foregroundPackage, elapsedSeconds, now)
            }
        }
    }

    private fun launchBlockOverlay(
        packageName: String,
        appName: String,
        decision: com.selflock.app.domain.model.LockoutDecision
    ) {
        val rule = decision.rule ?: return
        startActivity(Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockOverlayActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockOverlayActivity.EXTRA_RULE_ID, rule.id)
            putExtra(BlockOverlayActivity.EXTRA_RULE_NAME, rule.name)
            putExtra(BlockOverlayActivity.EXTRA_REMAINING_MINUTES, decision.remainingMinutes)
            putExtra(BlockOverlayActivity.EXTRA_PROGRESS_APP_NAME, rule.progressAppName)
            putExtra(BlockOverlayActivity.EXTRA_PROGRESS_PACKAGE_NAME, rule.progressPackageName)
            putExtra(BlockOverlayActivity.EXTRA_PROGRESS_SECONDS, decision.progressSeconds)
            putExtra(BlockOverlayActivity.EXTRA_GOAL_MINUTES, rule.goalMinutes)
            putExtra(BlockOverlayActivity.EXTRA_REWARDS_USED, decision.rewardsUsed)
            putExtra(BlockOverlayActivity.EXTRA_MAX_REWARDS, rule.maxRewards)
            putExtra(BlockOverlayActivity.EXTRA_CONTINGENCY_USED, decision.contingencyUsed)
            putExtra(BlockOverlayActivity.EXTRA_CONTINGENCY_AVAILABLE_AT, decision.contingencyAvailableAt)
            putExtra(BlockOverlayActivity.EXTRA_CONTINGENCY_MINUTES, rule.contingencyMinutes)
        })
    }

    private fun getForegroundPackage(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, startTime, endTime
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, MonitoringService::class.java)
        startForegroundService(restartIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_monitoring),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(getString(R.string.notification_monitoring_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
