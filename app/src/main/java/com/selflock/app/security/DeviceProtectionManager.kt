package com.selflock.app.security

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceProtectionManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val policyManager = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, DeviceOwnerReceiver::class.java)

    fun isDeviceOwner(): Boolean = policyManager.isDeviceOwnerApp(context.packageName)

    fun updateAllowedPackages(packages: Set<String>) {
        if (!isDeviceOwner()) return
        policyManager.setLockTaskPackages(admin, packages.toTypedArray())
    }

    fun enter(activity: Activity, packages: Set<String>) {
        if (!isDeviceOwner()) return
        updateAllowedPackages(packages)
        runCatching { activity.startLockTask() }
    }
}
