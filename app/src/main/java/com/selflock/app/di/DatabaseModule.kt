package com.selflock.app.di

import android.content.Context
import androidx.room.Room
import com.selflock.app.data.local.AppDatabase
import com.selflock.app.data.local.dao.LockoutRuleDao
import com.selflock.app.data.local.dao.BlockEventDao
import com.selflock.app.data.local.dao.UsageLogDao
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
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideLockoutRuleDao(db: AppDatabase): LockoutRuleDao = db.lockoutRuleDao()
    @Provides fun provideUsageLogDao(db: AppDatabase): UsageLogDao = db.usageLogDao()
    @Provides fun provideBlockEventDao(db: AppDatabase): BlockEventDao = db.blockEventDao()
}
