package com.selflock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lockout_rewards")
data class LockoutReward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val position: Int,
    val name: String = "",
    val requiredMinutes: Int,
    val availabilityStartHour: Int? = null,
    val availabilityStartMinute: Int? = null,
    val availabilityEndHour: Int? = null,
    val availabilityEndMinute: Int? = null,
    val durationMinutes: Int,
    val releaseType: String
)
