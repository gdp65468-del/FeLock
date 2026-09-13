package com.selflock.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "lockout_reward_apps", primaryKeys = ["rewardId", "packageName"])
data class LockoutRewardApp(val rewardId: Long, val packageName: String)
