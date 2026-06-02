package com.selflock.app.data.repository

import com.selflock.app.data.local.dao.BlockEventDao
import com.selflock.app.data.local.dao.UsageLogDao
import com.selflock.app.data.local.entity.BlockEvent
import com.selflock.app.data.local.entity.UsageLog
import com.selflock.app.domain.model.TargetType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageLogDao: UsageLogDao,
    private val blockEventDao: BlockEventDao
) {
    suspend fun getUsageForToday(targetId: Long, type: TargetType, date: String): UsageLog? =
        usageLogDao.getUsageForToday(targetId, type, date)

    suspend fun upsertUsage(log: UsageLog) = usageLogDao.upsert(log)

    suspend fun logBlockEvent(event: BlockEvent): Long = blockEventDao.insert(event)

    suspend fun updateBlockEvent(event: BlockEvent) = blockEventDao.update(event)

    suspend fun getActiveBlockEvent(targetId: Long, type: TargetType): BlockEvent? =
        blockEventDao.getActiveBlockEvent(targetId, type)

    suspend fun getUsageInRange(startDate: String, endDate: String): List<UsageLog> =
        usageLogDao.getUsageInRange(startDate, endDate)

    suspend fun getAggregatedUsage(startDate: String, endDate: String): List<com.selflock.app.data.local.entity.AggregatedUsage> =
        usageLogDao.getAggregatedUsageInRange(startDate, endDate)

    suspend fun getTotalUsage(startDate: String, endDate: String): Long =
        usageLogDao.getTotalUsageInRange(startDate, endDate)

    suspend fun getBlockEvents(startTime: Long, endTime: Long): List<BlockEvent> =
        blockEventDao.getEventsInRange(startTime, endTime)

    suspend fun getBlockCount(startTime: Long, endTime: Long): Int =
        blockEventDao.getBlockCountInRange(startTime, endTime)

    suspend fun getTotalBlockedTime(startTime: Long, endTime: Long): Long =
        blockEventDao.getTotalBlockedTimeInRange(startTime, endTime)
}
