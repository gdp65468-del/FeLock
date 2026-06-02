package com.selflock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.selflock.app.data.local.entity.BlockEvent
import com.selflock.app.domain.model.TargetType

@Dao
interface BlockEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BlockEvent): Long

    @Update
    suspend fun update(event: BlockEvent)

    @Query("SELECT * FROM block_events WHERE targetId = :targetId AND targetType = :type AND unblockedAt IS NULL ORDER BY blockedAt DESC LIMIT 1")
    suspend fun getActiveBlockEvent(targetId: Long, type: TargetType): BlockEvent?

    @Query("SELECT * FROM block_events WHERE targetType = :type ORDER BY blockedAt DESC LIMIT 50")
    suspend fun getRecentEvents(type: TargetType): List<BlockEvent>

    @Query("DELETE FROM block_events WHERE blockedAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("SELECT * FROM block_events WHERE blockedAt >= :startTime AND blockedAt <= :endTime ORDER BY blockedAt DESC")
    suspend fun getEventsInRange(startTime: Long, endTime: Long): List<BlockEvent>

    @Query("SELECT COUNT(*) FROM block_events WHERE blockedAt >= :startTime AND blockedAt <= :endTime")
    suspend fun getBlockCountInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT COALESCE(SUM(unblockedAt - blockedAt), 0) FROM block_events WHERE blockedAt >= :startTime AND blockedAt <= :endTime AND unblockedAt IS NOT NULL")
    suspend fun getTotalBlockedTimeInRange(startTime: Long, endTime: Long): Long
}
