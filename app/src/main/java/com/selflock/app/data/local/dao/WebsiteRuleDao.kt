package com.selflock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.selflock.app.data.local.entity.WebsiteRule
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteRuleDao {
    @Query("SELECT * FROM website_rules")
    fun getAllRules(): Flow<List<WebsiteRule>>

    @Query("SELECT * FROM website_rules WHERE isEnabled = 1")
    fun getActiveRules(): Flow<List<WebsiteRule>>

    @Query("SELECT * FROM website_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): WebsiteRule?

    @Query("SELECT * FROM website_rules WHERE isEnabled = 1")
    suspend fun getActiveRulesList(): List<WebsiteRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: WebsiteRule): Long

    @Update
    suspend fun update(rule: WebsiteRule)

    @Delete
    suspend fun delete(rule: WebsiteRule)
}
