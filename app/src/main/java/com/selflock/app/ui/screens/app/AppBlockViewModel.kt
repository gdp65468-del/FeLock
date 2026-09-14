package com.selflock.app.ui.screens.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.data.local.entity.LockoutRule
import com.selflock.app.data.local.entity.LockoutRuleWithApps
import com.selflock.app.data.repository.LockoutRepository
import com.selflock.app.domain.usecase.GetInstalledAppsUseCase
import com.selflock.app.domain.usecase.InstalledApp
import com.selflock.app.domain.usecase.LockoutManager
import com.selflock.app.security.MasterPasswordManager
import com.selflock.app.util.LockoutSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AppRuleUiState(
    val ruleWithApps: LockoutRuleWithApps,
    val isActive: Boolean,
    val remainingMinutes: Long,
    val progressSeconds: Long,
    val rewardsUsed: Int,
    val rewardActiveUntil: Long,
    val contingencyActiveUntil: Long,
    val endedByReward: Boolean
)

enum class RuleAction { TOGGLE, DELETE, EDIT }

data class PendingAction(val rule: LockoutRule, val action: RuleAction)

@HiltViewModel
class AppBlockViewModel @Inject constructor(
    private val repository: LockoutRepository,
    private val lockoutManager: LockoutManager,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val masterPasswordManager: MasterPasswordManager,
    private val lockoutSettings: LockoutSettings
) : ViewModel() {
    private val _rules = MutableStateFlow<List<AppRuleUiState>>(emptyList())
    val rules: StateFlow<List<AppRuleUiState>> = _rules.asStateFlow()
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()
    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()
    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()
    private val _editingRule = MutableStateFlow<LockoutRuleWithApps?>(null)
    val editingRule: StateFlow<LockoutRuleWithApps?> = _editingRule.asStateFlow()
    var addDraft: RuleEditorDraft? = null
        private set
    var editDraft: RuleEditorDraft? = null
        private set
    private var ruleList: List<LockoutRuleWithApps> = emptyList()
    val limitSchedulesToTwelveHours: Boolean
        get() = lockoutSettings.limitSchedulesToTwelveHours

    init {
        viewModelScope.launch {
            repository.getAllRules().collect {
                ruleList = it
                refreshStates()
            }
        }
        viewModelScope.launch {
            while (true) {
                refreshStates()
                delay(5000)
            }
        }
        viewModelScope.launch { _installedApps.value = getInstalledAppsUseCase.execute() }
    }

    private suspend fun refreshStates() {
        _rules.value = ruleList.map { ruleWithApps ->
            val rule = ruleWithApps.rule
            val active = lockoutManager.isActive(rule)
            val session = lockoutManager.getSession(rule)
            AppRuleUiState(
                ruleWithApps = ruleWithApps,
                isActive = active,
                remainingMinutes = lockoutManager.remainingMinutes(rule),
                progressSeconds = session.progressSeconds,
                rewardsUsed = session.rewardsUsed,
                rewardActiveUntil = session.rewardActiveUntil,
                contingencyActiveUntil = session.contingencyActiveUntil,
                endedByReward = session.endedByReward
            )
        }
    }

    fun showAddSheet() { _showAddSheet.value = true }
    fun hideAddSheet() { _showAddSheet.value = false }
    fun hideEditSheet() { _editingRule.value = null }
    fun saveAddDraft(draft: RuleEditorDraft) { addDraft = draft }
    fun saveEditDraft(draft: RuleEditorDraft) { editDraft = draft }
    fun discardAddDraft() { addDraft = null; _showAddSheet.value = false }
    fun discardEditDraft() { editDraft = null; _editingRule.value = null }

    fun addRule(data: RuleEditorData) {
        addDraft = null
        viewModelScope.launch {
            val firstTask = data.taskApps.first()
            val firstReward = data.rewards.first()
            repository.insert(
                LockoutRule(
                    name = data.name.trim(),
                    scheduleStartHour = data.startHour,
                    scheduleStartMinute = data.startMinute,
                    scheduleEndHour = data.endHour,
                    scheduleEndMinute = data.endMinute,
                    scheduleDays = data.days,
                    usesBlockedApps = true,
                    progressPackageName = firstTask.packageName,
                    progressAppName = firstTask.appName,
                    goalMinutes = firstReward.requiredMinutes.toInt(),
                    rewardMinutes = firstReward.durationMinutes.toIntOrNull() ?: 0,
                    maxRewards = data.rewards.size,
                    contingencyAfterMinutes = 120,
                    contingencyMinutes = 10,
                    blockSettings = data.blockSettings,
                    isPasswordProtected = data.passwordProtected,
                    passwordHash = if (data.passwordProtected && data.password != null) hashPassword(data.password) else null,
                    createdAt = Instant.now().toEpochMilli(),
                    updatedAt = Instant.now().toEpochMilli()
                ),
                data.blockedApps.map { it.packageName to it.appName },
                data.taskApps.map { it.packageName to it.appName },
                rewardInputs(data.rewards)
            )
            _showAddSheet.value = false
        }
    }

    fun toggleRule(rule: LockoutRule) {
        if (lockoutManager.isActive(rule)) return
        if (rule.isPasswordProtected) {
            _pendingAction.value = PendingAction(rule, RuleAction.TOGGLE)
        } else {
            viewModelScope.launch { repository.update(rule.copy(isEnabled = !rule.isEnabled, updatedAt = Instant.now().toEpochMilli())) }
        }
    }

    fun deleteRule(rule: LockoutRule) {
        if (lockoutManager.isActive(rule)) return
        if (rule.isPasswordProtected) {
            _pendingAction.value = PendingAction(rule, RuleAction.DELETE)
        } else {
            viewModelScope.launch { repository.delete(rule) }
        }
    }

    fun editRule(ruleWithApps: LockoutRuleWithApps) {
        if (lockoutManager.isActive(ruleWithApps.rule)) return
        if (ruleWithApps.rule.isPasswordProtected) {
            _pendingAction.value = PendingAction(ruleWithApps.rule, RuleAction.EDIT)
        } else {
            editDraft = null
            _editingRule.value = ruleWithApps
        }
    }

    fun updateRule(original: LockoutRuleWithApps, data: RuleEditorData) {
        if (lockoutManager.isActive(original.rule)) return
        editDraft = null
        viewModelScope.launch {
            val firstTask = data.taskApps.first()
            val firstReward = data.rewards.first()
            repository.update(
                original.rule.copy(
                    name = data.name.trim(),
                    scheduleStartHour = data.startHour,
                    scheduleStartMinute = data.startMinute,
                    scheduleEndHour = data.endHour,
                    scheduleEndMinute = data.endMinute,
                    scheduleDays = data.days,
                    usesBlockedApps = true,
                    progressPackageName = firstTask.packageName,
                    progressAppName = firstTask.appName,
                    goalMinutes = firstReward.requiredMinutes.toInt(),
                    rewardMinutes = firstReward.durationMinutes.toIntOrNull() ?: 0,
                    maxRewards = data.rewards.size,
                    blockSettings = data.blockSettings,
                    isPasswordProtected = data.passwordProtected,
                    passwordHash = when {
                        !data.passwordProtected -> null
                        data.password != null -> hashPassword(data.password)
                        else -> original.rule.passwordHash
                    },
                    updatedAt = Instant.now().toEpochMilli()
                ),
                data.blockedApps.map { it.packageName to it.appName },
                data.taskApps.map { it.packageName to it.appName },
                rewardInputs(data.rewards)
            )
            _editingRule.value = null
        }
    }

    fun duplicateRule(ruleWithApps: LockoutRuleWithApps) {
        viewModelScope.launch {
            val now = Instant.now().toEpochMilli()
            val copy = ruleWithApps.rule.copy(
                    id = 0,
                    name = "${ruleWithApps.rule.name} (cópia)",
                    isEnabled = false,
                    createdAt = now,
                    updatedAt = now
                )
            if (copy.usesBlockedApps) repository.insert(
                copy,
                ruleWithApps.blockedApps.map { it.packageName to it.appName },
                ruleWithApps.taskApps.map { it.packageName to it.appName },
                ruleWithApps.rewards.sortedBy { it.reward.position }.map { LockoutRepository.RewardInput(it.reward, it.releasedApps.map { app -> app.packageName }) }
            ) else repository.insert(copy, ruleWithApps.allowedApps.map { it.packageName to it.appName })
        }
    }

    fun dismissPendingAction() { _pendingAction.value = null }

    suspend fun verifyPassword(password: String, rule: LockoutRule): Boolean {
        if (rule.passwordHash != null && verifyRulePassword(password, rule.passwordHash)) return true
        return masterPasswordManager.isEnabled() && masterPasswordManager.verifyPassword(password)
    }

    fun executePendingAction() {
        val pending = _pendingAction.value ?: return
        viewModelScope.launch {
            when (pending.action) {
                RuleAction.TOGGLE -> repository.update(pending.rule.copy(isEnabled = !pending.rule.isEnabled, updatedAt = Instant.now().toEpochMilli()))
                RuleAction.DELETE -> repository.delete(pending.rule)
                RuleAction.EDIT -> _editingRule.value = ruleList.firstOrNull { it.rule.id == pending.rule.id }
            }
            _pendingAction.value = null
        }
    }

    fun isMasterPasswordEnabled(): Boolean = masterPasswordManager.isEnabled()

    private fun rewardInputs(rewards: List<RewardDraft>) = rewards.mapIndexed { index, draft ->
        val start = parseEditorTime(draft.startTime)
        val end = parseEditorTime(draft.endTime)
        LockoutRepository.RewardInput(
            com.selflock.app.data.local.entity.LockoutReward(
                ruleId = 0,
                position = index,
                name = draft.name.trim(),
                requiredMinutes = draft.requiredMinutes.toInt(),
                availabilityStartHour = start?.first.takeIf { draft.scheduled },
                availabilityStartMinute = start?.second.takeIf { draft.scheduled },
                availabilityEndHour = end?.first.takeIf { draft.scheduled },
                availabilityEndMinute = end?.second.takeIf { draft.scheduled },
                durationMinutes = draft.durationMinutes.toIntOrNull() ?: 0,
                releaseType = draft.releaseType
            ),
            draft.releasedPackages.toList()
        )
    }

    private fun parseEditorTime(value: String): Pair<Int, Int>? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return hour to minute
    }

    private suspend fun hashPassword(password: String): String = withContext(Dispatchers.Default) {
        val salt = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 600000, 256)
        val hash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP) + ":" + android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
    }

    private suspend fun verifyRulePassword(password: String, storedHash: String): Boolean = withContext(Dispatchers.Default) {
        val parts = storedHash.split(":")
        if (parts.size != 2) return@withContext false
        val salt = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 600000, 256)
        val hash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP) == parts[0]
    }
}
