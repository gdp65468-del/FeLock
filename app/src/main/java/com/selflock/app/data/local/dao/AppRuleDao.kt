package com.selflock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.selflock.app.data.local.entity.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules")
    fun getAllRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE isEnabled = 1")
    fun getActiveRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): AppRule?

    @Query("SELECT * FROM app_rules WHERE isEnabled = 1")
    suspend fun getActiveRulesList(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getRuleByPackageName(packageName: String): AppRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AppRule): Long

    @Update
    suspend fun update(rule: AppRule)

    @Delete
    suspend fun delete(rule: AppRule)
}
