package com.selflock.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selflock.app.data.local.converter.Converters
import com.selflock.app.data.local.dao.BlockEventDao
import com.selflock.app.data.local.dao.LockoutRuleDao
import com.selflock.app.data.local.dao.UsageLogDao
import com.selflock.app.data.local.entity.BlockEvent
import com.selflock.app.data.local.entity.LockoutAllowedApp
import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.data.local.entity.LockoutSession
import com.selflock.app.data.local.entity.LockoutBlockedApp
import com.selflock.app.data.local.entity.LockoutTaskApp
import com.selflock.app.data.local.entity.LockoutReward
import com.selflock.app.data.local.entity.LockoutRewardApp
import com.selflock.app.data.local.entity.UsageLog

@Database(
    entities = [LockoutRule::class, LockoutAllowedApp::class, LockoutBlockedApp::class, LockoutTaskApp::class, LockoutReward::class, LockoutRewardApp::class, LockoutSession::class, UsageLog::class, BlockEvent::class],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lockoutRuleDao(): LockoutRuleDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_rules ADD COLUMN isPasswordProtected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_rules ADD COLUMN passwordHash TEXT")
                db.execSQL("ALTER TABLE website_rules ADD COLUMN isPasswordProtected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE website_rules ADD COLUMN passwordHash TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM usage_logs WHERE targetType = 'WEBSITE'")
                db.execSQL("DELETE FROM block_events WHERE targetType = 'WEBSITE'")
                db.execSQL("DROP TABLE website_rules")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS lockout_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isEnabled INTEGER NOT NULL, scheduleStartHour INTEGER NOT NULL, scheduleStartMinute INTEGER NOT NULL, scheduleEndHour INTEGER NOT NULL, scheduleEndMinute INTEGER NOT NULL, scheduleDays TEXT NOT NULL, progressPackageName TEXT NOT NULL, progressAppName TEXT NOT NULL, goalMinutes INTEGER NOT NULL, rewardMinutes INTEGER NOT NULL, maxRewards INTEGER NOT NULL, contingencyAfterMinutes INTEGER NOT NULL, contingencyMinutes INTEGER NOT NULL, blockSettings INTEGER NOT NULL, isPasswordProtected INTEGER NOT NULL, passwordHash TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS lockout_allowed_apps (ruleId INTEGER NOT NULL, packageName TEXT NOT NULL, appName TEXT NOT NULL, PRIMARY KEY(ruleId, packageName))")
                db.execSQL("CREATE TABLE IF NOT EXISTS lockout_sessions (ruleId INTEGER NOT NULL, sessionKey TEXT NOT NULL, progressSeconds INTEGER NOT NULL, rewardsUsed INTEGER NOT NULL, rewardActiveUntil INTEGER NOT NULL, contingencyUsed INTEGER NOT NULL, contingencyActiveUntil INTEGER NOT NULL, PRIMARY KEY(ruleId))")
                db.execSQL("DELETE FROM usage_logs")
                db.execSQL("DELETE FROM block_events")
                db.execSQL("DROP TABLE app_rules")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lockout_rules ADD COLUMN usesBlockedApps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE lockout_sessions ADD COLUMN currentRewardPosition INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE lockout_sessions ADD COLUMN activeRewardId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE lockout_sessions ADD COLUMN endedByReward INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS lockout_blocked_apps (ruleId INTEGER NOT NULL, packageName TEXT NOT NULL, appName TEXT NOT NULL, PRIMARY KEY(ruleId, packageName))")
                db.execSQL("CREATE TABLE IF NOT EXISTS lockout_task_apps (ruleId INTEGER NOT NULL, packageName TEXT NOT NULL, appName TEXT NOT NULL, PRIMARY KEY(ruleId, packageName))")
                db.execSQL("CREATE TABLE IF NOT EXISTS lockout_rewards (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ruleId INTEGER NOT NULL, position INTEGER NOT NULL, name TEXT NOT NULL, requiredMinutes INTEGER NOT NULL, availabilityStartHour INTEGER, availabilityStartMinute INTEGER, availabilityEndHour INTEGER, availabilityEndMinute INTEGER, durationMinutes INTEGER NOT NULL, releaseType TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS lockout_reward_apps (rewardId INTEGER NOT NULL, packageName TEXT NOT NULL, PRIMARY KEY(rewardId, packageName))")
                db.execSQL("INSERT INTO lockout_task_apps (ruleId, packageName, appName) SELECT id, progressPackageName, progressAppName FROM lockout_rules")
                db.execSQL("INSERT INTO lockout_rewards (ruleId, position, name, requiredMinutes, durationMinutes, releaseType) SELECT id, 0, 'Recompensa', goalMinutes, rewardMinutes, 'TEMPORARY' FROM lockout_rules")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lockout_rules ADD COLUMN managedProtection INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
