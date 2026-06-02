package com.selflock.app.ui.screens.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.data.local.entity.AppRule
import com.selflock.app.data.repository.AppRuleRepository
import com.selflock.app.domain.model.BlockType
import com.selflock.app.domain.model.RuleStatus
import com.selflock.app.domain.usecase.CheckBlockStatusUseCase
import com.selflock.app.domain.usecase.GetInstalledAppsUseCase
import com.selflock.app.domain.usecase.InstalledApp
import com.selflock.app.domain.usecase.IsRuleLockedUseCase
import com.selflock.app.security.MasterPasswordManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AppRuleUiState(
    val rule: AppRule,
    val status: RuleStatus
)

enum class RuleAction { TOGGLE, DELETE }

data class PendingAction(
    val rule: AppRule,
    val action: RuleAction
)

@HiltViewModel
class AppBlockViewModel @Inject constructor(
    private val repository: AppRuleRepository,
    private val checkBlockStatusUseCase: CheckBlockStatusUseCase,
    private val isRuleLockedUseCase: IsRuleLockedUseCase,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val masterPasswordManager: MasterPasswordManager
) : ViewModel() {

    private val _rules = MutableStateFlow<List<AppRuleUiState>>(emptyList())
    val rules: StateFlow<List<AppRuleUiState>> = _rules.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllRules().collect { ruleList ->
                val uiStates = ruleList.map { rule ->
                    AppRuleUiState(
                        rule = rule,
                        status = checkBlockStatusUseCase.checkAppRule(rule)
                    )
                }
                _rules.value = uiStates
            }
        }
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = getInstalledAppsUseCase.execute()
        }
    }

    fun showAddSheet() { _showAddSheet.value = true }
    fun hideAddSheet() { _showAddSheet.value = false }

    fun addRule(
        packageName: String,
        appName: String,
        blockType: BlockType,
        startHour: Int?,
        startMinute: Int?,
        endHour: Int?,
        endMinute: Int?,
        days: String,
        dailyLimitMinutes: Int?,
        isPasswordProtected: Boolean,
        password: String?
    ) {
        viewModelScope.launch {
            val passwordHash = if (isPasswordProtected && password != null) {
                hashPassword(password)
            } else null

            repository.insert(
                AppRule(
                    packageName = packageName,
                    appName = appName,
                    blockType = blockType,
                    scheduleStartHour = startHour,
                    scheduleStartMinute = startMinute,
                    scheduleEndHour = endHour,
                    scheduleEndMinute = endMinute,
                    scheduleDays = days,
                    dailyLimitMinutes = dailyLimitMinutes,
                    isPasswordProtected = isPasswordProtected,
                    passwordHash = passwordHash,
                    createdAt = Instant.now().toEpochMilli(),
                    updatedAt = Instant.now().toEpochMilli()
                )
            )
            _showAddSheet.value = false
        }
    }

    fun toggleRule(rule: AppRule) {
        if (isRuleLockedUseCase.isAppRuleLocked(rule)) return
        if (rule.isPasswordProtected) {
            _pendingAction.value = PendingAction(rule, RuleAction.TOGGLE)
            return
        }
        viewModelScope.launch {
            repository.update(rule.copy(isEnabled = !rule.isEnabled, updatedAt = Instant.now().toEpochMilli()))
        }
    }

    fun deleteRule(rule: AppRule) {
        if (isRuleLockedUseCase.isAppRuleLocked(rule)) return
        if (rule.isPasswordProtected) {
            _pendingAction.value = PendingAction(rule, RuleAction.DELETE)
            return
        }
        viewModelScope.launch {
            repository.delete(rule)
        }
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
    }

    suspend fun verifyPassword(password: String, rule: AppRule): Boolean {
        if (rule.passwordHash != null && verifyRulePassword(password, rule.passwordHash)) {
            return true
        }
        if (masterPasswordManager.isEnabled()) {
            return masterPasswordManager.verifyPassword(password)
        }
        return false
    }

    fun executePendingAction() {
        val pending = _pendingAction.value ?: return
        viewModelScope.launch {
            when (pending.action) {
                RuleAction.TOGGLE -> {
                    repository.update(pending.rule.copy(isEnabled = !pending.rule.isEnabled, updatedAt = Instant.now().toEpochMilli()))
                }
                RuleAction.DELETE -> {
                    repository.delete(pending.rule)
                }
            }
            _pendingAction.value = null
        }
    }

    fun isRuleLocked(rule: AppRule): Boolean = isRuleLockedUseCase.isAppRuleLocked(rule)

    fun isMasterPasswordEnabled(): Boolean = masterPasswordManager.isEnabled()

    private suspend fun hashPassword(password: String): String {
        val salt = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 600000, 256)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP) + ":" +
            android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
    }

    private fun verifyRulePassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 600000, 256)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP) == parts[0]
    }
}
