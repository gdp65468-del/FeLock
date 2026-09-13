package com.selflock.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "lockout_blocked_apps", primaryKeys = ["ruleId", "packageName"])
data class LockoutBlockedApp(val ruleId: Long, val packageName: String, val appName: String)
