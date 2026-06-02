package com.selflock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.selflock.app.data.local.entity.UsageLog
import com.selflock.app.domain.model.TargetType

@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_logs WHERE targetId = :targetId AND targetType = :type AND date = :date")
    suspend fun getUsageForToday(targetId: Long, type: TargetType, date: String): UsageLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: UsageLog)

    @Query("SELECT COALESCE(SUM(usageSeconds), 0) FROM usage_logs WHERE targetType = :type AND date = :date")
    suspend fun getTotalUsageForDate(type: TargetType, date: String): Long

    @Query("SELECT * FROM usage_logs WHERE date = :date")
    suspend fun getAllUsageForDate(date: String): List<UsageLog>

    @Query("DELETE FROM usage_logs WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("SELECT * FROM usage_logs WHERE date >= :startDate AND date <= :endDate")
    suspend fun getUsageInRange(startDate: String, endDate: String): List<UsageLog>

    @Query("SELECT targetId, targetType, SUM(usageSeconds) as totalSeconds FROM usage_logs WHERE date >= :startDate AND date <= :endDate GROUP BY targetId, targetType ORDER BY totalSeconds DESC")
    suspend fun getAggregatedUsageInRange(startDate: String, endDate: String): List<com.selflock.app.data.local.entity.AggregatedUsage>

    @Query("SELECT COALESCE(SUM(usageSeconds), 0) FROM usage_logs WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalUsageInRange(startDate: String, endDate: String): Long
}
