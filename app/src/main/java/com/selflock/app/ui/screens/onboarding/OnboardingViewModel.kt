package com.selflock.app.ui.screens.onboarding

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.security.MasterPasswordManager
import com.selflock.app.security.RecoveryAccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val recoveryAccountManager: RecoveryAccountManager,
    private val masterPasswordManager: MasterPasswordManager
) : ViewModel() {

    private val _recoveryAccount = MutableStateFlow(recoveryAccountManager.getRecoveryAccountEmail())
    val recoveryAccount: StateFlow<String?> = _recoveryAccount.asStateFlow()

    private val _passwordSet = MutableStateFlow(masterPasswordManager.isEnabled())
    val passwordSet: StateFlow<Boolean> = _passwordSet.asStateFlow()

    private val _isSavingPassword = MutableStateFlow(false)
    val isSavingPassword: StateFlow<Boolean> = _isSavingPassword.asStateFlow()

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

    fun setMasterPassword(password: String, onDone: () -> Unit) {
        if (_isSavingPassword.value) return
        viewModelScope.launch {
            _isSavingPassword.value = true
            masterPasswordManager.setPassword(password)
            _passwordSet.value = true
            _isSavingPassword.value = false
            onDone()
        }
    }

    companion object {
        const val CHOOSE_ACCOUNT_REQUEST_CODE = 9001
    }
}
