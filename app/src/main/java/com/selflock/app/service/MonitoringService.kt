package com.selflock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.KeyguardManager
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var usageStatsManager: UsageStatsManager
    @Inject lateinit var lockoutManager: LockoutManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private val observationMutex = Mutex()
    private lateinit var timeAccumulator: ForegroundTimeAccumulator
    private var lastUsageQueryTime = 0L
    @Volatile private var lastAccessibilityObservationElapsed = 0L
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                scope.launch { observeForeground(null) }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "monitoring_service"
        const val NOTIFICATION_ID = 1002
        private const val POLL_INTERVAL_MS = 1000L
        private const val ACCESSIBILITY_PRIORITY_MS = 2500L
        const val ACTION_FOREGROUND_CHANGED = "com.selflock.app.FOREGROUND_CHANGED"
        const val EXTRA_FOREGROUND_PACKAGE = "foreground_package"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        timeAccumulator = ForegroundTimeAccumulator(SystemClock.elapsedRealtime())
        lastUsageQueryTime = System.currentTimeMillis() - 60_000L
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startPolling()
        if (intent?.action == ACTION_FOREGROUND_CHANGED) {
            val packageName = intent.getStringExtra(EXTRA_FOREGROUND_PACKAGE)
            lastAccessibilityObservationElapsed = SystemClock.elapsedRealtime()
            scope.launch { observeForeground(packageName) }
        }
        return START_STICKY
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                pollUsageStats()
            }
        }
    }

    private suspend fun pollUsageStats() {
        if (SystemClock.elapsedRealtime() - lastAccessibilityObservationElapsed < ACCESSIBILITY_PRIORITY_MS) return
        observationMutex.withLock {
            observeForegroundLocked(readForegroundPackage())
        }
    }

    private suspend fun observeForeground(packageName: String?) = observationMutex.withLock {
        observeForegroundLocked(packageName)
    }

    private suspend fun observeForegroundLocked(packageName: String?) {
        val nowElapsed = SystemClock.elapsedRealtime()
        val powerManager = getSystemService(PowerManager::class.java)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val interval = timeAccumulator.observe(
            packageName,
            nowElapsed,
            powerManager.isInteractive && !keyguardManager.isDeviceLocked
        )
        if (interval != null) lockoutManager.recordProgress(interval.packageName, interval.seconds)
        if (packageName == null || packageName == this.packageName) return
        val now = System.currentTimeMillis()
        val decision = lockoutManager.evaluate(packageName, now)
        if (decision.isBlocked) {
            val appName = runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault("Aplicativo")
            launchBlockOverlay(packageName, appName, decision)
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

    private fun readForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(lastUsageQueryTime, now)
        lastUsageQueryTime = now
        val event = UsageEvents.Event()
        var currentPackage = timeAccumulator.currentPackage
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> currentPackage = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> if (currentPackage == event.packageName) currentPackage = null
            }
        }
        return currentPackage
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, MonitoringService::class.java)
        startForegroundService(restartIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        unregisterReceiver(screenReceiver)
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
