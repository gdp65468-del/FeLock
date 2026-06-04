package com.selflock.app.security

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryAccountManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRecoveryAccountEmail(): String? = prefs.getString(KEY_ACCOUNT, null)

    fun setRecoveryAccountEmail(email: String?) {
        prefs.edit().apply {
            if (email == null) remove(KEY_ACCOUNT) else putString(KEY_ACCOUNT, email)
            apply()
        }
    }

    fun getGoogleAccounts(): List<Account> {
        val accountManager = AccountManager.get(context)
        return accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE).toList()
    }

    fun isRecoveryAccountOnDevice(): Boolean {
        val email = getRecoveryAccountEmail() ?: return false
        return getGoogleAccounts().any { it.name.equals(email, ignoreCase = true) }
    }

    companion object {
        private const val PREFS_NAME = "selflock_recovery_prefs"
        private const val KEY_ACCOUNT = "recovery_account_email"
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}
