package com.selflock.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.security.MasterPasswordManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val masterPasswordManager: MasterPasswordManager
) : ViewModel() {

    private val _passwordSet = MutableStateFlow(masterPasswordManager.isEnabled())
    val passwordSet: StateFlow<Boolean> = _passwordSet.asStateFlow()

    private val _isSavingPassword = MutableStateFlow(false)
    val isSavingPassword: StateFlow<Boolean> = _isSavingPassword.asStateFlow()

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
}
