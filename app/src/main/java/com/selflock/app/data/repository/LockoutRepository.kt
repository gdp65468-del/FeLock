package com.selflock.app.data.repository

import androidx.room.withTransaction
import com.selflock.app.data.local.AppDatabase
import com.selflock.app.data.local.dao.LockoutRuleDao
import com.selflock.app.data.local.entity.LockoutAllowedApp
import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.data.local.entity.LockoutRuleWithApps
import com.selflock.app.data.local.entity.LockoutSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockoutRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: LockoutRuleDao
) {
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

    suspend fun update(rule: LockoutRule, apps: List<Pair<String, String>>? = null) = database.withTransaction {
        dao.updateRule(rule)
        if (apps != null) {
            dao.deleteAllowedApps(rule.id)
            dao.insertAllowedApps(apps.map { LockoutAllowedApp(rule.id, it.first, it.second) })
        }
    }

    suspend fun delete(rule: LockoutRule) = database.withTransaction {
        dao.deleteSession(rule.id)
        dao.deleteAllowedApps(rule.id)
        dao.deleteRule(rule)
    }
}
