package com.selflock.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selflock.app.data.local.converter.Converters
import com.selflock.app.data.local.dao.AppRuleDao
import com.selflock.app.data.local.dao.BlockEventDao
import com.selflock.app.data.local.dao.UsageLogDao
import com.selflock.app.data.local.dao.WebsiteRuleDao
import com.selflock.app.data.local.entity.AppRule
import com.selflock.app.data.local.entity.BlockEvent
import com.selflock.app.data.local.entity.UsageLog
import com.selflock.app.data.local.entity.WebsiteRule

@Database(
    entities = [WebsiteRule::class, AppRule::class, UsageLog::class, BlockEvent::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun websiteRuleDao(): WebsiteRuleDao
    abstract fun appRuleDao(): AppRuleDao
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
    }
}
