package com.selflock.app.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.data.local.entity.LockoutRuleWithApps
import com.selflock.app.data.local.entity.LockoutSession
import com.selflock.app.data.repository.LockoutRepository
import com.selflock.app.data.repository.UsageRepository
import com.selflock.app.domain.model.LockoutDecision
import com.selflock.app.domain.model.TaskApp
import com.selflock.app.domain.model.TargetType
import com.selflock.app.data.local.entity.UsageLog
import com.selflock.app.util.ScheduleHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class LockoutManager @Inject constructor(
    private val repository: LockoutRepository,
    private val usageRepository: UsageRepository,
    @ApplicationContext private val context: Context
) {
    private val mutex = Mutex()

    suspend fun evaluate(packageName: String, nowMillis: Long = System.currentTimeMillis()): LockoutDecision = mutex.withLock {
        val activeRules = repository.getEnabledRules().filter { isActive(it.rule, nowMillis) }
        for (ruleWithApps in activeRules) {
            val rule = ruleWithApps.rule
            val session = currentSession(rule, nowMillis)
            val isSettingsBlocked = packageName == SETTINGS_PACKAGE && rule.blockSettings
            if (session.endedByReward && !isSettingsBlocked) continue
            if (!isSettingsBlocked) {
                if (isEssentialPackage(packageName)) continue
                if (rule.usesBlockedApps) {
                    if (ruleWithApps.taskApps.any { it.packageName == packageName }) continue
                    if (ruleWithApps.blockedApps.none { it.packageName == packageName }) continue
                    if (isReleasedByActiveReward(ruleWithApps, session, packageName, nowMillis)) continue
                } else {
                    if (isTemporarilyUnlocked(session, nowMillis)) continue
                    if (ruleWithApps.allowedApps.any { it.packageName == packageName }) continue
                }
            }
            val endMillis = windowEndMillis(rule, nowMillis)
            val contingencyAt = if (session.contingencyUsed) Long.MAX_VALUE else
                windowStartMillis(rule, nowMillis) + rule.contingencyAfterMinutes * 60_000L
            return LockoutDecision(
                isBlocked = true,
                rule = rule,
                remainingMinutes = ((endMillis - nowMillis).coerceAtLeast(0) + 59_999) / 60_000,
                progressSeconds = session.progressSeconds,
                rewardsUsed = session.rewardsUsed,
                contingencyUsed = session.contingencyUsed,
                contingencyAvailableAt = contingencyAt,
                contingencyAvailable = !session.contingencyUsed && nowMillis >= contingencyAt,
                reward = ruleWithApps.rewards.sortedBy { it.reward.position }.getOrNull(session.currentRewardPosition),
                taskAppName = ruleWithApps.taskApps.firstOrNull()?.appName ?: rule.progressAppName,
                taskApps = ruleWithApps.taskApps.map { TaskApp(it.packageName, it.appName) },
                rewardActiveUntil = session.rewardActiveUntil,
                endedByReward = session.endedByReward
            )
        }
        LockoutDecision(false)
    }

    suspend fun recordProgress(packageName: String, elapsedSeconds: Long, nowMillis: Long = System.currentTimeMillis()) = mutex.withLock {
        if (elapsedSeconds <= 0) return@withLock
        repository.getEnabledRules().filter { isActive(it.rule, nowMillis) }.forEach { ruleWithApps ->
            val rule = ruleWithApps.rule
            var session = currentSession(rule, nowMillis)
            if (session.endedByReward || isTemporarilyUnlocked(session, nowMillis)) return@forEach
            if (!rule.usesBlockedApps && session.rewardsUsed >= rule.maxRewards) return@forEach
            val rewards = ruleWithApps.rewards.sortedBy { it.reward.position }
            val reward = rewards.getOrNull(session.currentRewardPosition)
            val isTaskApp = if (rule.usesBlockedApps) ruleWithApps.taskApps.any { it.packageName == packageName } else rule.progressPackageName == packageName
            if (!isTaskApp) return@forEach
            if (rule.usesBlockedApps && (reward == null || !isRewardAvailable(reward.reward, nowMillis))) return@forEach
            val goalSeconds = (reward?.reward?.requiredMinutes ?: rule.goalMinutes) * 60L
            val creditedSeconds = RewardPolicy.creditedSeconds(session.progressSeconds, elapsedSeconds, goalSeconds)
            val progress = session.progressSeconds + creditedSeconds
            session = if (progress >= goalSeconds) {
                if (rule.usesBlockedApps && reward != null) completeReward(rule, session, reward, nowMillis)
                else session.copy(
                    progressSeconds = 0,
                    rewardsUsed = session.rewardsUsed + 1,
                    rewardActiveUntil = minOf(nowMillis + rule.rewardMinutes * 60_000L, windowEndMillis(rule, nowMillis))
                )
            } else {
                session.copy(progressSeconds = progress)
            }
            repository.upsertSession(session)
            val date = LocalDate.now().toString()
            val usage = usageRepository.getUsageForToday(rule.id, TargetType.APP, date)
            usageRepository.upsertUsage(
                UsageLog(
                    id = usage?.id ?: 0,
                    targetId = rule.id,
                    targetType = TargetType.APP,
                    date = date,
                    usageSeconds = (usage?.usageSeconds ?: 0) + creditedSeconds,
                    lastUpdated = nowMillis
                )
            )
        }
    }

    suspend fun activateContingency(ruleId: Long, nowMillis: Long = System.currentTimeMillis()): Boolean = mutex.withLock {
        val rule = repository.getRuleById(ruleId) ?: return@withLock false
        if (!isActive(rule, nowMillis)) return@withLock false
        val session = currentSession(rule, nowMillis)
        val availableAt = windowStartMillis(rule, nowMillis) + rule.contingencyAfterMinutes * 60_000L
        if (session.contingencyUsed || nowMillis < availableAt) return@withLock false
        repository.upsertSession(
            session.copy(
                contingencyUsed = true,
                contingencyActiveUntil = minOf(nowMillis + rule.contingencyMinutes * 60_000L, windowEndMillis(rule, nowMillis))
            )
        )
        true
    }

    suspend fun getSession(rule: LockoutRule, nowMillis: Long = System.currentTimeMillis()): LockoutSession = mutex.withLock {
        currentSession(rule, nowMillis)
    }

    suspend fun managedLockTaskPackages(nowMillis: Long = System.currentTimeMillis()): Set<String> = mutex.withLock {
        repository.getEnabledRules()
            .filter { isActive(it.rule, nowMillis) && it.rule.managedProtection }
            .flatMap { ruleWithApps ->
                ruleWithApps.allowedApps.map { it.packageName } +
                    ruleWithApps.blockedApps.map { it.packageName } +
                    ruleWithApps.taskApps.map { it.packageName } +
                    ruleWithApps.rewards.flatMap { reward -> reward.releasedApps.map { it.packageName } }
            }
            .plus(context.packageName)
            .toSet()
    }

    fun isActive(rule: LockoutRule, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        return rule.isEnabled && ScheduleHelper.isScheduleActive(
            dateTime.toLocalTime(),
            dateTime.dayOfWeek,
            rule.getStartTime(),
            rule.getEndTime(),
            rule.getDaysList()
        )
    }

    fun remainingMinutes(rule: LockoutRule, nowMillis: Long = System.currentTimeMillis()): Long {
        if (!isActive(rule, nowMillis)) return 0
        return ((windowEndMillis(rule, nowMillis) - nowMillis).coerceAtLeast(0) + 59_999) / 60_000
    }

    private suspend fun currentSession(rule: LockoutRule, nowMillis: Long): LockoutSession {
        val key = windowStartDate(rule, nowMillis).toString()
        val existing = repository.getSession(rule.id)
        if (existing != null && existing.sessionKey == key) return existing
        val session = LockoutSession(ruleId = rule.id, sessionKey = key)
        repository.upsertSession(session)
        return session
    }

    private fun isTemporarilyUnlocked(session: LockoutSession, nowMillis: Long): Boolean =
        session.rewardActiveUntil > nowMillis || session.contingencyActiveUntil > nowMillis

    private fun completeReward(rule: LockoutRule, session: LockoutSession, reward: com.selflock.app.data.local.entity.LockoutRewardWithApps, nowMillis: Long): LockoutSession {
        val end = windowEndMillis(rule, nowMillis)
        return when (reward.reward.releaseType) {
            "END_SESSION" -> session.copy(progressSeconds = reward.reward.requiredMinutes * 60L, rewardsUsed = session.rewardsUsed + 1, currentRewardPosition = session.currentRewardPosition + 1, endedByReward = true)
            "UNTIL_END" -> session.copy(progressSeconds = 0, rewardsUsed = session.rewardsUsed + 1, currentRewardPosition = session.currentRewardPosition + 1, activeRewardId = reward.reward.id, rewardActiveUntil = end)
            else -> session.copy(progressSeconds = 0, rewardsUsed = session.rewardsUsed + 1, currentRewardPosition = session.currentRewardPosition + 1, activeRewardId = reward.reward.id, rewardActiveUntil = minOf(nowMillis + reward.reward.durationMinutes * 60_000L, end))
        }
    }

    private fun isReleasedByActiveReward(ruleWithApps: LockoutRuleWithApps, session: LockoutSession, packageName: String, nowMillis: Long): Boolean {
        if (session.contingencyActiveUntil > nowMillis) return true
        if (session.rewardActiveUntil <= nowMillis) return false
        val reward = ruleWithApps.rewards.firstOrNull { it.reward.id == session.activeRewardId } ?: return false
        return RewardPolicy.isReleased(session.rewardActiveUntil, nowMillis, reward.releasedApps.mapTo(mutableSetOf()) { it.packageName }, packageName)
    }

    private fun isRewardAvailable(reward: com.selflock.app.data.local.entity.LockoutReward, nowMillis: Long): Boolean {
        val startHour = reward.availabilityStartHour ?: return true
        val start = LocalTime.of(startHour, reward.availabilityStartMinute ?: 0)
        val end = LocalTime.of(reward.availabilityEndHour ?: return true, reward.availabilityEndMinute ?: 0)
        val now = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault()).toLocalTime()
        return RewardPolicy.isAvailable(now, start, end)
    }

    private fun windowStartDate(rule: LockoutRule, nowMillis: Long): LocalDate {
        val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val start = rule.getStartTime()
        val end = rule.getEndTime()
        return if (!start.isBefore(end) && dateTime.toLocalTime().isBefore(end)) {
            dateTime.toLocalDate().minusDays(1)
        } else {
            dateTime.toLocalDate()
        }
    }

    private fun windowStartMillis(rule: LockoutRule, nowMillis: Long): Long =
        windowStartDate(rule, nowMillis).atTime(rule.getStartTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun windowEndMillis(rule: LockoutRule, nowMillis: Long): Long {
        val startDate = windowStartDate(rule, nowMillis)
        val endDate = if (rule.getStartTime().isBefore(rule.getEndTime())) startDate else startDate.plusDays(1)
        return endDate.atTime(rule.getEndTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun isEssentialPackage(packageName: String): Boolean =
        packageName == SELF_PACKAGE || packageName in ESSENTIAL_PACKAGES || packageName in homePackages() || isSystemPackage(packageName)

    private fun isSystemPackage(packageName: String): Boolean = runCatching {
        val flags = context.packageManager.getApplicationInfo(packageName, 0).flags
        flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }.getOrDefault(false)

    private fun homePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.queryIntentActivities(intent, 0).mapTo(mutableSetOf()) { it.activityInfo.packageName }
    }

    companion object {
        private const val SELF_PACKAGE = "com.selflock.app"
        private const val SETTINGS_PACKAGE = "com.android.settings"
        private val ESSENTIAL_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.mi.android.globallauncher",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.google.android.gms"
        )
    }
}
