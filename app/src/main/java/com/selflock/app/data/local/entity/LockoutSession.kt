package com.selflock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lockout_sessions")
data class LockoutSession(
    @PrimaryKey val ruleId: Long,
    val sessionKey: String,
    val progressSeconds: Long = 0,
    val rewardsUsed: Int = 0,
    val rewardActiveUntil: Long = 0,
    val contingencyUsed: Boolean = false,
    val contingencyActiveUntil: Long = 0
)
