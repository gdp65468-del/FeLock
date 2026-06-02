package com.selflock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.Context
import com.selflock.app.BlockOverlayActivity
import com.selflock.app.MainActivity
import com.selflock.app.R
import com.selflock.app.data.local.entity.UsageLog
import com.selflock.app.data.repository.AppRuleRepository
import com.selflock.app.data.repository.UsageRepository
import com.selflock.app.data.repository.WebsiteRuleRepository
import com.selflock.app.domain.model.TargetType
import com.selflock.app.domain.usecase.CheckBlockStatusUseCase
import com.selflock.app.util.BrowserUrlParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var usageStatsManager: UsageStatsManager
    @Inject lateinit var appRuleRepository: AppRuleRepository
    @Inject lateinit var websiteRuleRepository: WebsiteRuleRepository
    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var checkBlockStatusUseCase: CheckBlockStatusUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var lastForegroundPackage: String? = null
    private var lastPollTime: Long = 0

    companion object {
        const val CHANNEL_ID = "monitoring_service"
        const val NOTIFICATION_ID = 1002
        private const val POLL_INTERVAL_MS = 5000L

        var currentBrowserUrl: String? = null
            private set
        var currentBrowserPackage: String? = null
            private set

        fun onBrowserUrlDetected(url: String, browserPackage: String) {
            currentBrowserUrl = url
            currentBrowserPackage = browserPackage
        }
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
        val elapsed = now - lastPollTime
        lastPollTime = now

        val foregroundPackage = getForegroundPackage() ?: return

        if (foregroundPackage == "com.selflock.app") return

        val appRule = appRuleRepository.getRuleByPackageName(foregroundPackage)
        if (appRule != null) {
            val status = checkBlockStatusUseCase.checkAppRule(appRule)
            if (status.isActive) {
                launchBlockOverlay(foregroundPackage, appRule.appName, status.remainingTimeMinutes ?: 0L)
            } else {
                val today = LocalDate.now().toString()
                val existingLog = usageRepository.getUsageForToday(appRule.id, TargetType.APP, today)
                val currentSeconds = existingLog?.usageSeconds ?: 0
                usageRepository.upsertUsage(
                    UsageLog(
                        id = existingLog?.id ?: 0,
                        targetId = appRule.id,
                        targetType = TargetType.APP,
                        date = today,
                        usageSeconds = currentSeconds + (elapsed / 1000),
                        lastUpdated = now
                    )
                )
            }
        }

        if (BrowserUrlParser.isBrowserPackage(foregroundPackage)) {
            trackWebsiteUsage(elapsed, now)
        }

        lastForegroundPackage = foregroundPackage
    }

    private suspend fun trackWebsiteUsage(elapsed: Long, now: Long) {
        val url = currentBrowserUrl ?: return
        val domain = BrowserUrlParser.extractDomain(url) ?: return
        val today = LocalDate.now().toString()

        val rules = websiteRuleRepository.getActiveRulesList()
        for (rule in rules) {
            if (rule.domain.equals(domain, ignoreCase = true) || domain.endsWith(".${rule.domain}", ignoreCase = true)) {
                val status = checkBlockStatusUseCase.checkWebsiteRule(rule)
                if (!status.isActive) {
                    val existingLog = usageRepository.getUsageForToday(rule.id, TargetType.WEBSITE, today)
                    val currentSeconds = existingLog?.usageSeconds ?: 0
                    usageRepository.upsertUsage(
                        UsageLog(
                            id = existingLog?.id ?: 0,
                            targetId = rule.id,
                            targetType = TargetType.WEBSITE,
                            date = today,
                            usageSeconds = currentSeconds + (elapsed / 1000),
                            lastUpdated = now
                        )
                    )
                }
            }
        }
    }

    private fun launchBlockOverlay(packageName: String, appName: String, remainingMinutes: Long) {
        val intent = android.content.Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockOverlayActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockOverlayActivity.EXTRA_REMAINING_MINUTES, remainingMinutes)
            putExtra(BlockOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
        }
        startActivity(intent)
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
