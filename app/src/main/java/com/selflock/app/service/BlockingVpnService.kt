package com.selflock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.selflock.app.MainActivity
import com.selflock.app.R
import com.selflock.app.vpn.BlocklistManager
import com.selflock.app.vpn.PacketProcessor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BlockingVpnService : VpnService() {

    @Inject lateinit var packetProcessor: PacketProcessor
    @Inject lateinit var blocklistManager: BlocklistManager

    private var tunFd: ParcelFileDescriptor? = null
    private var processingThread: Thread? = null

    companion object {
        const val CHANNEL_ID = "vpn_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.selflock.app.START_VPN"
        const val ACTION_STOP = "com.selflock.app.STOP_VPN"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
        }

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (tunFd != null) return

        val builder = Builder()
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setMtu(1500)
            .setMetered(false)
            .setSession("SelfLock")

        try {
            tunFd = builder.establish()
        } catch (_: Exception) {
            stopSelf()
            return
        }

        val fd = tunFd ?: return
        processingThread = Thread {
            packetProcessor.start(fd) { socket -> protect(socket) }
        }.apply {
            isDaemon = true
            name = "VpnPacketProcessor"
            start()
        }

        blocklistManager.refreshBlocklist()
    }

    private fun stopVpn() {
        packetProcessor.stop()
        processingThread?.interrupt()
        processingThread = null
        tunFd?.close()
        tunFd = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
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
