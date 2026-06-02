package com.selflock.app.domain.usecase

import com.selflock.app.data.local.entity.AppRule
import com.selflock.app.data.local.entity.WebsiteRule
import com.selflock.app.data.repository.UsageRepository
import com.selflock.app.domain.model.BlockType
import com.selflock.app.domain.model.TargetType
import com.selflock.app.util.ScheduleHelper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class IsRuleLockedUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {

    fun isWebsiteRuleLocked(rule: WebsiteRule): Boolean {
        if (!rule.isEnabled) return false
        return when (rule.blockType) {
            BlockType.SCHEDULE -> {
                val now = LocalTime.now()
                val today = LocalDate.now().dayOfWeek
                val start = rule.getStartTime() ?: return false
                val end = rule.getEndTime() ?: return false
                ScheduleHelper.isScheduleActive(now, today, start, end, rule.getDaysList())
            }
            BlockType.DAILY_LIMIT -> {
                val limitMinutes = rule.dailyLimitMinutes ?: return false
                runBlocking {
                    val usage = usageRepository.getUsageForToday(rule.id, TargetType.WEBSITE, LocalDate.now().toString())
                    val usedSeconds = usage?.usageSeconds ?: 0L
                    usedSeconds >= limitMinutes * 60L
                }
            }
        }
    }

    fun isAppRuleLocked(rule: AppRule): Boolean {
        if (!rule.isEnabled) return false
        return when (rule.blockType) {
            BlockType.SCHEDULE -> {
                val now = LocalTime.now()
                val today = LocalDate.now().dayOfWeek
                val start = rule.getStartTime() ?: return false
                val end = rule.getEndTime() ?: return false
                ScheduleHelper.isScheduleActive(now, today, start, end, rule.getDaysList())
            }
            BlockType.DAILY_LIMIT -> {
                val limitMinutes = rule.dailyLimitMinutes ?: return false
                runBlocking {
                    val usage = usageRepository.getUsageForToday(rule.id, TargetType.APP, LocalDate.now().toString())
                    val usedSeconds = usage?.usageSeconds ?: 0L
                    usedSeconds >= limitMinutes * 60L
                }
            }
        }
    }
}
