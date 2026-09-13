package com.selflock.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class LockoutRuleWithApps(
    @Embedded val rule: LockoutRule,
    @Relation(parentColumn = "id", entityColumn = "ruleId")
    val allowedApps: List<LockoutAllowedApp>,
    @Relation(parentColumn = "id", entityColumn = "ruleId")
    val blockedApps: List<LockoutBlockedApp> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "ruleId")
    val taskApps: List<LockoutTaskApp> = emptyList(),
    @Relation(entity = LockoutReward::class, parentColumn = "id", entityColumn = "ruleId")
    val rewards: List<LockoutRewardWithApps> = emptyList()
)
