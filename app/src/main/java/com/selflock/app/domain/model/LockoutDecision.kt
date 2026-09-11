package com.selflock.app.domain.model

import com.selflock.app.data.local.entity.LockoutRule

data class LockoutDecision(
    val isBlocked: Boolean,
    val rule: LockoutRule? = null,
    val remainingMinutes: Long = 0,
    val progressSeconds: Long = 0,
    val rewardsUsed: Int = 0,
    val contingencyUsed: Boolean = false,
    val contingencyAvailableAt: Long = 0,
    val contingencyAvailable: Boolean = false
)
