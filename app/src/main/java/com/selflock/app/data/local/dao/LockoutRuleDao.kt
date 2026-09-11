package com.selflock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.selflock.app.data.local.entity.LockoutAllowedApp
import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.data.local.entity.LockoutRuleWithApps
import com.selflock.app.data.local.entity.LockoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface LockoutRuleDao {
    @Transaction
    @Query("SELECT * FROM lockout_rules")
    fun getAllRules(): Flow<List<LockoutRuleWithApps>>

    @Transaction
    @Query("SELECT * FROM lockout_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<LockoutRuleWithApps>

    @Query("SELECT * FROM lockout_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): LockoutRule?

    @Insert
    suspend fun insertRule(rule: LockoutRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllowedApps(apps: List<LockoutAllowedApp>)

    @Update
    suspend fun updateRule(rule: LockoutRule)

    @Delete
    suspend fun deleteRule(rule: LockoutRule)

    @Query("DELETE FROM lockout_allowed_apps WHERE ruleId = :ruleId")
    suspend fun deleteAllowedApps(ruleId: Long)

    @Query("SELECT * FROM lockout_sessions WHERE ruleId = :ruleId")
    suspend fun getSession(ruleId: Long): LockoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: LockoutSession)

    @Query("DELETE FROM lockout_sessions WHERE ruleId = :ruleId")
    suspend fun deleteSession(ruleId: Long)
}
