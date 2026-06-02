package com.selflock.app.data.repository

import com.selflock.app.data.local.dao.WebsiteRuleDao
import com.selflock.app.data.local.entity.WebsiteRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebsiteRuleRepository @Inject constructor(
    private val dao: WebsiteRuleDao
) {
    fun getAllRules(): Flow<List<WebsiteRule>> = dao.getAllRules()
    fun getActiveRules(): Flow<List<WebsiteRule>> = dao.getActiveRules()
    suspend fun getActiveRulesList(): List<WebsiteRule> = dao.getActiveRulesList()
    suspend fun insert(rule: WebsiteRule): Long = dao.insert(rule)
    suspend fun update(rule: WebsiteRule) = dao.update(rule)
    suspend fun delete(rule: WebsiteRule) = dao.delete(rule)
}
