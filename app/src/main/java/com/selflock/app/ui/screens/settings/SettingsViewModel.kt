package com.selflock.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.security.MasterPasswordManager
import com.selflock.app.util.PermissionHelper
import com.selflock.app.util.LockoutSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionStatus(
    val accessibility: Boolean = false,
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val notifications: Boolean = false,
    val exactAlarms: Boolean = false,
    val batteryOptimization: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val masterPasswordManager: MasterPasswordManager,
    private val lockoutSettings: LockoutSettings
) : AndroidViewModel(app) {

    private val _permissions = MutableStateFlow(PermissionStatus())
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    private val _masterPasswordEnabled = MutableStateFlow(masterPasswordManager.isEnabled())
    val masterPasswordEnabled: StateFlow<Boolean> = _masterPasswordEnabled.asStateFlow()
    private val _limitSchedulesToTwelveHours = MutableStateFlow(lockoutSettings.limitSchedulesToTwelveHours)
    val limitSchedulesToTwelveHours: StateFlow<Boolean> = _limitSchedulesToTwelveHours.asStateFlow()

    fun setLimitSchedulesToTwelveHours(enabled: Boolean) {
        lockoutSettings.limitSchedulesToTwelveHours = enabled
        _limitSchedulesToTwelveHours.value = enabled
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _permissions.value = PermissionStatus(
                accessibility = PermissionHelper.isAccessibilityServiceEnabled(app),
                usageAccess = PermissionHelper.isUsageAccessGranted(app),
                overlay = PermissionHelper.isOverlayPermissionGranted(app),
                notifications = PermissionHelper.isNotificationPermissionGranted(app),
                exactAlarms = PermissionHelper.isExactAlarmAllowed(app),
                batteryOptimization = PermissionHelper.isBatteryOptimizationDisabled(app)
            )
        }
    }

    fun setMasterPassword(password: String) {
        viewModelScope.launch {
            masterPasswordManager.setPassword(password)
            _masterPasswordEnabled.value = true
        }
    }

    fun disableMasterPassword() {
        viewModelScope.launch {
            masterPasswordManager.clearPassword()
            _masterPasswordEnabled.value = false
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        return masterPasswordManager.verifyPassword(password)
    }
}
