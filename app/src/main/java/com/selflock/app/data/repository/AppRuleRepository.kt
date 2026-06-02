package com.selflock.app.data.repository

import com.selflock.app.data.local.dao.AppRuleDao
import com.selflock.app.data.local.entity.AppRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRuleRepository @Inject constructor(
    private val dao: AppRuleDao
) {
    fun getAllRules(): Flow<List<AppRule>> = dao.getAllRules()
    fun getActiveRules(): Flow<List<AppRule>> = dao.getActiveRules()
    suspend fun getActiveRulesList(): List<AppRule> = dao.getActiveRulesList()
    suspend fun getRuleByPackageName(packageName: String): AppRule? = dao.getRuleByPackageName(packageName)
    suspend fun insert(rule: AppRule): Long = dao.insert(rule)
    suspend fun update(rule: AppRule) = dao.update(rule)
    suspend fun delete(rule: AppRule) = dao.delete(rule)
}
