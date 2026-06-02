package com.selflock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selflock.app.domain.model.TargetType
import java.time.Instant

@Entity(tableName = "usage_logs")
data class UsageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: Long,
    val targetType: TargetType,
    val date: String,
    val usageSeconds: Long = 0,
    val lastUpdated: Long = Instant.now().toEpochMilli()
)
