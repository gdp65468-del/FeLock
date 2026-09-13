package com.selflock.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockoutSettings @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("lockout_settings", Context.MODE_PRIVATE)

    var limitSchedulesToTwelveHours: Boolean
        get() = preferences.getBoolean(LIMIT_SCHEDULES_KEY, true)
        set(value) = preferences.edit().putBoolean(LIMIT_SCHEDULES_KEY, value).apply()

    companion object {
        private const val LIMIT_SCHEDULES_KEY = "limit_schedules_to_twelve_hours"
    }
}
