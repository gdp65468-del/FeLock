package com.selflock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.selflock.app.BlockOverlayActivity
import com.selflock.app.domain.usecase.LockoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelfLockAccessibilityService : AccessibilityService() {

    @Inject lateinit var lockoutManager: LockoutManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == packageName()) return

        scope.launch {
            val decision = lockoutManager.evaluate(packageName)
            if (decision.isBlocked) {
                val appName = runCatching {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
                }.getOrDefault("Aplicativo")
                launchBlockOverlay(packageName, appName, decision)
            }
        }
    }

    private fun launchBlockOverlay(
        packageName: String,
        appName: String,
        decision: com.selflock.app.domain.model.LockoutDecision
    ) {
        val rule = decision.rule ?: return
        startActivity(Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockOverlayActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockOverlayActivity.EXTRA_RULE_ID, rule.id)
            putExtra(BlockOverlayActivity.EXTRA_RULE_NAME, rule.name)
            putExtra(BlockOverlayActivity.EXTRA_REMAINING_MINUTES, decision.remainingMinutes)
            putExtra(BlockOverlayActivity.EXTRA_PROGRESS_APP_NAME, rule.progressAppName)
            putExtra(BlockOverlayActivity.EXTRA_PROGRESS_PACKAGE_NAME, rule.progressPackageName)
            putExtra(BlockOverlayActivity.EXTRA_PROGRESS_SECONDS, decision.progressSeconds)
            putExtra(BlockOverlayActivity.EXTRA_GOAL_MINUTES, rule.goalMinutes)
            putExtra(BlockOverlayActivity.EXTRA_REWARDS_USED, decision.rewardsUsed)
            putExtra(BlockOverlayActivity.EXTRA_MAX_REWARDS, rule.maxRewards)
            putExtra(BlockOverlayActivity.EXTRA_CONTINGENCY_USED, decision.contingencyUsed)
            putExtra(BlockOverlayActivity.EXTRA_CONTINGENCY_AVAILABLE_AT, decision.contingencyAvailableAt)
            putExtra(BlockOverlayActivity.EXTRA_CONTINGENCY_MINUTES, rule.contingencyMinutes)
        })
    }

    private fun packageName(): String = applicationContext.packageName

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
