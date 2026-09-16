package com.selflock.app.domain.model

import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.data.local.entity.LockoutRewardWithApps

data class TaskApp(val packageName: String, val appName: String)

data class LockoutDecision(
    val isBlocked: Boolean,
    val rule: LockoutRule? = null,
    val remainingMinutes: Long = 0,
    val progressSeconds: Long = 0,
    val rewardsUsed: Int = 0,
    val contingencyUsed: Boolean = false,
    val contingencyAvailableAt: Long = 0,
    val contingencyAvailable: Boolean = false,
    val reward: LockoutRewardWithApps? = null,
    val taskAppName: String = "",
    val taskApps: List<TaskApp> = emptyList(),
    val rewardActiveUntil: Long = 0,
    val endedByReward: Boolean = false
)
