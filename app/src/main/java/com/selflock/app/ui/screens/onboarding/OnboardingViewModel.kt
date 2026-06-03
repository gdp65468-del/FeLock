package com.selflock.app.ui.screens.onboarding

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.selflock.app.security.RecoveryAccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val recoveryAccountManager: RecoveryAccountManager
) : ViewModel() {

    private val _recoveryAccount = MutableStateFlow(recoveryAccountManager.getRecoveryAccountEmail())
    val recoveryAccount: StateFlow<String?> = _recoveryAccount.asStateFlow()

    fun getGoogleAccounts(): List<Account> = recoveryAccountManager.getGoogleAccounts()

    fun buildChooseAccountIntent(): Intent {
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf(RecoveryAccountManager.GOOGLE_ACCOUNT_TYPE),
            false,
            null,
            null,
            null,
            null
        )
    }

    fun onAccountPicked(email: String?) {
        recoveryAccountManager.setRecoveryAccountEmail(email)
        _recoveryAccount.value = email
    }

    companion object {
        const val CHOOSE_ACCOUNT_REQUEST_CODE = 9001
    }
}
