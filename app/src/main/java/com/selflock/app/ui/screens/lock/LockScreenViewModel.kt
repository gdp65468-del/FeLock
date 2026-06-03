package com.selflock.app.ui.screens.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.security.AppLockState
import com.selflock.app.security.MasterPasswordManager
import com.selflock.app.security.RecoveryAccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockScreenViewModel @Inject constructor(
    private val masterPasswordManager: MasterPasswordManager,
    private val recoveryAccountManager: RecoveryAccountManager
) : ViewModel() {

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun onPasswordChange(value: String) {
        _password.value = value
        _error.value = null
    }

    fun onUnlock() {
        if (_isVerifying.value) return
        viewModelScope.launch {
            _isVerifying.value = true
            val correct = masterPasswordManager.verifyPassword(_password.value)
            if (correct) {
                AppLockState.isUnlocked = true
                _unlocked.value = true
            } else {
                _error.value = "Incorrect password"
            }
            _isVerifying.value = false
        }
    }

    fun getRecoveryAccountEmail(): String? = recoveryAccountManager.getRecoveryAccountEmail()

    fun isRecoveryAccountOnDevice(): Boolean = recoveryAccountManager.isRecoveryAccountOnDevice()

    fun resetPasswordWithRecovery(newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            masterPasswordManager.setPassword(newPassword)
            AppLockState.isUnlocked = true
            _unlocked.value = true
            onSuccess()
        }
    }

    fun onResetApp(onReset: () -> Unit) {
        masterPasswordManager.clearPassword()
        AppLockState.isUnlocked = true
        onReset()
    }
}
