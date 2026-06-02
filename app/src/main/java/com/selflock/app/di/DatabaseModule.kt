package com.selflock.app.di

import android.content.Context
import androidx.room.Room
import com.selflock.app.data.local.AppDatabase
import com.selflock.app.data.local.dao.AppRuleDao
import com.selflock.app.data.local.dao.BlockEventDao
import com.selflock.app.data.local.dao.UsageLogDao
import com.selflock.app.data.local.dao.WebsiteRuleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "selflock_db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideWebsiteRuleDao(db: AppDatabase): WebsiteRuleDao = db.websiteRuleDao()
    @Provides fun provideAppRuleDao(db: AppDatabase): AppRuleDao = db.appRuleDao()
    @Provides fun provideUsageLogDao(db: AppDatabase): UsageLogDao = db.usageLogDao()
    @Provides fun provideBlockEventDao(db: AppDatabase): BlockEventDao = db.blockEventDao()
}
