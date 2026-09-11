package com.selflock.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.selflock.app.domain.usecase.LockoutManager
import com.selflock.app.ui.components.BlockOverlayContent
import com.selflock.app.ui.theme.SelfLockTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockOverlayActivity : ComponentActivity() {

    @Inject lateinit var lockoutManager: LockoutManager

    private var allowExit = false

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_REMAINING_MINUTES = "extra_remaining_minutes"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val EXTRA_RULE_NAME = "extra_rule_name"
        const val EXTRA_PROGRESS_APP_NAME = "extra_progress_app_name"
        const val EXTRA_PROGRESS_PACKAGE_NAME = "extra_progress_package_name"
        const val EXTRA_PROGRESS_SECONDS = "extra_progress_seconds"
        const val EXTRA_REWARDS_USED = "extra_rewards_used"
        const val EXTRA_MAX_REWARDS = "extra_max_rewards"
        const val EXTRA_GOAL_MINUTES = "extra_goal_minutes"
        const val EXTRA_CONTINGENCY_USED = "extra_contingency_used"
        const val EXTRA_CONTINGENCY_AVAILABLE_AT = "extra_contingency_available_at"
        const val EXTRA_CONTINGENCY_MINUTES = "extra_contingency_minutes"
        private const val COOLDOWN_MS = 3000L
        var lastLaunchTime: Long = 0L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastLaunchTime = System.currentTimeMillis()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        onBackPressedDispatcher.addCallback(this) {}
        showContent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lastLaunchTime = System.currentTimeMillis()
        showContent()
    }

    private fun showContent() {
        val blockedPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val progressPackage = intent.getStringExtra(EXTRA_PROGRESS_PACKAGE_NAME).orEmpty()
        val ruleId = intent.getLongExtra(EXTRA_RULE_ID, 0)
        setContent {
            SelfLockTheme {
                BlockOverlayContent(
                    appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App",
                    packageName = blockedPackage,
                    ruleName = intent.getStringExtra(EXTRA_RULE_NAME).orEmpty(),
                    remainingMinutes = intent.getLongExtra(EXTRA_REMAINING_MINUTES, 0),
                    progressAppName = intent.getStringExtra(EXTRA_PROGRESS_APP_NAME).orEmpty(),
                    progressSeconds = intent.getLongExtra(EXTRA_PROGRESS_SECONDS, 0),
                    goalMinutes = intent.getIntExtra(EXTRA_GOAL_MINUTES, 0),
                    rewardsUsed = intent.getIntExtra(EXTRA_REWARDS_USED, 0),
                    maxRewards = intent.getIntExtra(EXTRA_MAX_REWARDS, 0),
                    contingencyUsed = intent.getBooleanExtra(EXTRA_CONTINGENCY_USED, false),
                    contingencyAvailableAt = intent.getLongExtra(EXTRA_CONTINGENCY_AVAILABLE_AT, 0),
                    contingencyMinutes = intent.getIntExtra(EXTRA_CONTINGENCY_MINUTES, 0),
                    onOpenProgressApp = { openApp(progressPackage) },
                    onRelease = {
                        lifecycleScope.launch {
                            if (lockoutManager.activateContingency(ruleId)) openApp(blockedPackage)
                        }
                    }
                )
            }
        }
    }

    private fun openApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        allowExit = true
        startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        relaunchIfNeeded()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) relaunchIfNeeded()
    }

    private fun relaunchIfNeeded() {
        if (!allowExit && System.currentTimeMillis() - lastLaunchTime >= COOLDOWN_MS) {
            startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }
}
