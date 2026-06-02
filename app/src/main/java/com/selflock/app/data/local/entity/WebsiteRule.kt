package com.selflock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selflock.app.domain.model.BlockType
import java.time.Instant
import java.time.LocalTime

@Entity(tableName = "website_rules")
data class WebsiteRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val isEnabled: Boolean = true,
    val blockType: BlockType = BlockType.SCHEDULE,
    val scheduleStartHour: Int? = null,
    val scheduleStartMinute: Int? = null,
    val scheduleEndHour: Int? = null,
    val scheduleEndMinute: Int? = null,
    val scheduleDays: String = "MON,TUE,WED,THU,FRI",
    val dailyLimitMinutes: Int? = null,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
) {
    fun getStartTime(): LocalTime? =
        if (scheduleStartHour != null && scheduleStartMinute != null)
            LocalTime.of(scheduleStartHour, scheduleStartMinute) else null

    fun getEndTime(): LocalTime? =
        if (scheduleEndHour != null && scheduleEndMinute != null)
            LocalTime.of(scheduleEndHour, scheduleEndMinute) else null

    fun getDaysList(): List<String> = scheduleDays.split(",")
}
