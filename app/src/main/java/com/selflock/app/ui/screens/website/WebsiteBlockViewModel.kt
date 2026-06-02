package com.selflock.app.ui.screens.website

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.data.local.entity.WebsiteRule
import com.selflock.app.data.repository.WebsiteRuleRepository
import com.selflock.app.domain.model.BlockType
import com.selflock.app.domain.model.RuleStatus
import com.selflock.app.domain.usecase.CheckBlockStatusUseCase
import com.selflock.app.domain.usecase.IsRuleLockedUseCase
import com.selflock.app.security.MasterPasswordManager
import com.selflock.app.ui.screens.app.RuleAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class WebsiteRuleUiState(
    val rule: WebsiteRule,
    val status: RuleStatus
)

data class WebsitePendingAction(
    val rule: WebsiteRule,
    val action: RuleAction
)

@HiltViewModel
class WebsiteBlockViewModel @Inject constructor(
    private val repository: WebsiteRuleRepository,
    private val checkBlockStatusUseCase: CheckBlockStatusUseCase,
    private val isRuleLockedUseCase: IsRuleLockedUseCase,
    private val masterPasswordManager: MasterPasswordManager
) : ViewModel() {

    private val _rules = MutableStateFlow<List<WebsiteRuleUiState>>(emptyList())
    val rules: StateFlow<List<WebsiteRuleUiState>> = _rules.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _pendingAction = MutableStateFlow<WebsitePendingAction?>(null)
    val pendingAction: StateFlow<WebsitePendingAction?> = _pendingAction.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllRules().collect { ruleList ->
                val uiStates = ruleList.map { rule ->
                    WebsiteRuleUiState(
                        rule = rule,
                        status = checkBlockStatusUseCase.checkWebsiteRule(rule)
                    )
                }
                _rules.value = uiStates
            }
        }
    }

    fun showAddSheet() { _showAddSheet.value = true }
    fun hideAddSheet() { _showAddSheet.value = false }

    fun addRule(
        domain: String,
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
                WebsiteRule(
                    domain = domain.lowercase().trim(),
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

    fun toggleRule(rule: WebsiteRule) {
        if (isRuleLockedUseCase.isWebsiteRuleLocked(rule)) return
        if (rule.isPasswordProtected) {
            _pendingAction.value = WebsitePendingAction(rule, RuleAction.TOGGLE)
            return
        }
        viewModelScope.launch {
            repository.update(rule.copy(isEnabled = !rule.isEnabled, updatedAt = Instant.now().toEpochMilli()))
        }
    }

    fun deleteRule(rule: WebsiteRule) {
        if (isRuleLockedUseCase.isWebsiteRuleLocked(rule)) return
        if (rule.isPasswordProtected) {
            _pendingAction.value = WebsitePendingAction(rule, RuleAction.DELETE)
            return
        }
        viewModelScope.launch {
            repository.delete(rule)
        }
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
    }

    suspend fun verifyPassword(password: String, rule: WebsiteRule): Boolean {
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

    fun isRuleLocked(rule: WebsiteRule): Boolean = isRuleLockedUseCase.isWebsiteRuleLocked(rule)

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
