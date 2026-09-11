package com.selflock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalTime

@Entity(tableName = "lockout_rules")
data class LockoutRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val scheduleStartHour: Int,
    val scheduleStartMinute: Int,
    val scheduleEndHour: Int,
    val scheduleEndMinute: Int,
    val scheduleDays: String,
    val progressPackageName: String,
    val progressAppName: String,
    val goalMinutes: Int,
    val rewardMinutes: Int,
    val maxRewards: Int = 3,
    val contingencyAfterMinutes: Int,
    val contingencyMinutes: Int,
    val blockSettings: Boolean = true,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
) {
    fun getStartTime(): LocalTime = LocalTime.of(scheduleStartHour, scheduleStartMinute)
    fun getEndTime(): LocalTime = LocalTime.of(scheduleEndHour, scheduleEndMinute)
    fun getDaysList(): List<String> = scheduleDays.split(",")
}
