package com.selflock.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class LockoutRewardWithApps(
    @Embedded val reward: LockoutReward,
    @Relation(parentColumn = "id", entityColumn = "rewardId") val releasedApps: List<LockoutRewardApp>
)
