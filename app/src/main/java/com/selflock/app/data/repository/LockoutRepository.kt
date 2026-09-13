package com.selflock.app.data.repository

import androidx.room.withTransaction
import com.selflock.app.data.local.AppDatabase
import com.selflock.app.data.local.dao.LockoutRuleDao
import com.selflock.app.data.local.entity.LockoutAllowedApp
import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.data.local.entity.LockoutRuleWithApps
import com.selflock.app.data.local.entity.LockoutSession
import com.selflock.app.data.local.entity.LockoutBlockedApp
import com.selflock.app.data.local.entity.LockoutTaskApp
import com.selflock.app.data.local.entity.LockoutReward
import com.selflock.app.data.local.entity.LockoutRewardApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockoutRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: LockoutRuleDao
) {
    data class RewardInput(val reward: LockoutReward, val releasedPackages: List<String>)
    fun getAllRules(): Flow<List<LockoutRuleWithApps>> = dao.getAllRules()
    suspend fun getEnabledRules(): List<LockoutRuleWithApps> = dao.getEnabledRules()
    suspend fun getRuleById(id: Long): LockoutRule? = dao.getRuleById(id)
    suspend fun getSession(ruleId: Long): LockoutSession? = dao.getSession(ruleId)
    suspend fun upsertSession(session: LockoutSession) = dao.upsertSession(session)

    suspend fun insert(rule: LockoutRule, apps: List<Pair<String, String>>): Long = database.withTransaction {
        val id = dao.insertRule(rule)
        dao.insertAllowedApps(apps.map { LockoutAllowedApp(id, it.first, it.second) })
        id
    }

    suspend fun insert(rule: LockoutRule, blockedApps: List<Pair<String, String>>, taskApps: List<Pair<String, String>>, rewards: List<RewardInput>): Long = database.withTransaction {
        val id = dao.insertRule(rule)
        replaceConfiguration(id, blockedApps, taskApps, rewards)
        id
    }

    suspend fun update(rule: LockoutRule, apps: List<Pair<String, String>>? = null) = database.withTransaction {
        dao.updateRule(rule)
        if (apps != null) {
            dao.deleteAllowedApps(rule.id)
            dao.insertAllowedApps(apps.map { LockoutAllowedApp(rule.id, it.first, it.second) })
        }
    }

    suspend fun update(rule: LockoutRule, blockedApps: List<Pair<String, String>>, taskApps: List<Pair<String, String>>, rewards: List<RewardInput>) = database.withTransaction {
        dao.updateRule(rule)
        replaceConfiguration(rule.id, blockedApps, taskApps, rewards)
    }

    private suspend fun replaceConfiguration(ruleId: Long, blockedApps: List<Pair<String, String>>, taskApps: List<Pair<String, String>>, rewards: List<RewardInput>) {
        dao.deleteBlockedApps(ruleId)
        dao.deleteTaskApps(ruleId)
        dao.deleteRewardApps(ruleId)
        dao.deleteRewards(ruleId)
        dao.insertBlockedApps(blockedApps.map { LockoutBlockedApp(ruleId, it.first, it.second) })
        dao.insertTaskApps(taskApps.map { LockoutTaskApp(ruleId, it.first, it.second) })
        rewards.forEachIndexed { index, input ->
            val rewardId = dao.insertReward(input.reward.copy(id = 0, ruleId = ruleId, position = index))
            dao.insertRewardApps(input.releasedPackages.map { LockoutRewardApp(rewardId, it) })
        }
    }

    suspend fun delete(rule: LockoutRule) = database.withTransaction {
        dao.deleteSession(rule.id)
        dao.deleteAllowedApps(rule.id)
        dao.deleteBlockedApps(rule.id)
        dao.deleteTaskApps(rule.id)
        dao.deleteRewardApps(rule.id)
        dao.deleteRewards(rule.id)
        dao.deleteRule(rule)
    }
}
