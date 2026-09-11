package com.selflock.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "lockout_allowed_apps",
    primaryKeys = ["ruleId", "packageName"]
)
data class LockoutAllowedApp(
    val ruleId: Long,
    val packageName: String,
    val appName: String
)
