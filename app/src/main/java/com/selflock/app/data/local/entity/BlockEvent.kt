package com.selflock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selflock.app.domain.model.BlockReason
import com.selflock.app.domain.model.TargetType
import java.time.Instant

@Entity(tableName = "block_events")
data class BlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: Long,
    val targetType: TargetType,
    val blockedAt: Long = Instant.now().toEpochMilli(),
    val unblockedAt: Long? = null,
    val reason: BlockReason
)
