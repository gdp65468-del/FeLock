package com.selflock.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class LockoutRuleWithApps(
    @Embedded val rule: LockoutRule,
    @Relation(parentColumn = "id", entityColumn = "ruleId")
    val allowedApps: List<LockoutAllowedApp>
)
