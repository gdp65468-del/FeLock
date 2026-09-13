package com.selflock.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "lockout_task_apps", primaryKeys = ["ruleId", "packageName"])
data class LockoutTaskApp(val ruleId: Long, val packageName: String, val appName: String)
