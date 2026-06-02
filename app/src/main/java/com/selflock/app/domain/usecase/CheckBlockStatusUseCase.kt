package com.selflock.app.domain.usecase

import com.selflock.app.data.local.entity.AppRule
import com.selflock.app.data.local.entity.WebsiteRule
import com.selflock.app.domain.model.BlockType
import com.selflock.app.domain.model.RuleStatus
import com.selflock.app.data.repository.UsageRepository
import com.selflock.app.domain.model.TargetType
import com.selflock.app.util.ScheduleHelper
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class CheckBlockStatusUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend fun checkWebsiteRule(rule: WebsiteRule): RuleStatus {
        if (!rule.isEnabled) return RuleStatus(false, false, null, null)
        val now = LocalTime.now()
        val today = LocalDate.now().dayOfWeek

        return when (rule.blockType) {
            BlockType.SCHEDULE -> {
                val start = rule.getStartTime() ?: return RuleStatus(false, false, null, null)
                val end = rule.getEndTime() ?: return RuleStatus(false, false, null, null)
                val isActive = ScheduleHelper.isScheduleActive(now, today, start, end, rule.getDaysList())
                val remaining = if (isActive) ScheduleHelper.minutesUntil(end) else null
                RuleStatus(isActive, isActive, remaining, null)
            }
            BlockType.DAILY_LIMIT -> {
                val limitMinutes = rule.dailyLimitMinutes ?: return RuleStatus(false, false, null, null)
                val usage = usageRepository.getUsageForToday(rule.id, TargetType.WEBSITE, LocalDate.now().toString())
                val usedSeconds = usage?.usageSeconds ?: 0L
                val limitSeconds = limitMinutes * 60L
                val percent = if (limitSeconds > 0) (usedSeconds.toFloat() / limitSeconds) else 0f
                val isExhausted = usedSeconds >= limitSeconds
                RuleStatus(isExhausted, isExhausted, if (isExhausted) 0 else null, percent)
            }
        }
    }

    suspend fun checkAppRule(rule: AppRule): RuleStatus {
        if (!rule.isEnabled) return RuleStatus(false, false, null, null)
        val now = LocalTime.now()
        val today = LocalDate.now().dayOfWeek

        return when (rule.blockType) {
            BlockType.SCHEDULE -> {
                val start = rule.getStartTime() ?: return RuleStatus(false, false, null, null)
                val end = rule.getEndTime() ?: return RuleStatus(false, false, null, null)
                val isActive = ScheduleHelper.isScheduleActive(now, today, start, end, rule.getDaysList())
                val remaining = if (isActive) ScheduleHelper.minutesUntil(end) else null
                RuleStatus(isActive, isActive, remaining, null)
            }
            BlockType.DAILY_LIMIT -> {
                val limitMinutes = rule.dailyLimitMinutes ?: return RuleStatus(false, false, null, null)
                val usage = usageRepository.getUsageForToday(rule.id, TargetType.APP, LocalDate.now().toString())
                val usedSeconds = usage?.usageSeconds ?: 0L
                val limitSeconds = limitMinutes * 60L
                val percent = if (limitSeconds > 0) (usedSeconds.toFloat() / limitSeconds) else 0f
                val isExhausted = usedSeconds >= limitSeconds
                RuleStatus(isExhausted, isExhausted, if (isExhausted) 0 else null, percent)
            }
        }
    }
}
