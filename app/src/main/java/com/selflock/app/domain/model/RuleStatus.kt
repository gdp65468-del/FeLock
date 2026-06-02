package com.selflock.app.domain.model

data class RuleStatus(
    val isActive: Boolean,
    val isLocked: Boolean,
    val remainingTimeMinutes: Long?,
    val usagePercent: Float?
)
