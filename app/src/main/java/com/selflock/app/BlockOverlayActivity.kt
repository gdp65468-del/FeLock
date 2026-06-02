package com.selflock.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import com.selflock.app.ui.components.BlockOverlayContent
import com.selflock.app.ui.theme.SelfLockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_REMAINING_MINUTES = "extra_remaining_minutes"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_DOMAIN = "extra_domain"
        private const val COOLDOWN_MS = 3000L
        var lastLaunchTime: Long = 0L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastLaunchTime = System.currentTimeMillis()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val remainingMinutes = intent.getLongExtra(EXTRA_REMAINING_MINUTES, 0L)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val domain = intent.getStringExtra(EXTRA_DOMAIN)

        onBackPressedDispatcher.addCallback(this) {
        }

        setContent {
            SelfLockTheme {
                BlockOverlayContent(
                    appName = appName,
                    remainingMinutes = remainingMinutes,
                    packageName = packageName,
                    domain = domain
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (System.currentTimeMillis() - lastLaunchTime >= COOLDOWN_MS) {
            val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                putExtra(EXTRA_APP_NAME, this@BlockOverlayActivity.intent.getStringExtra(EXTRA_APP_NAME))
                putExtra(EXTRA_REMAINING_MINUTES, this@BlockOverlayActivity.intent.getLongExtra(EXTRA_REMAINING_MINUTES, 0L))
                putExtra(EXTRA_PACKAGE_NAME, this@BlockOverlayActivity.intent.getStringExtra(EXTRA_PACKAGE_NAME))
                putExtra(EXTRA_DOMAIN, this@BlockOverlayActivity.intent.getStringExtra(EXTRA_DOMAIN))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && System.currentTimeMillis() - lastLaunchTime >= COOLDOWN_MS) {
            val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                putExtra(EXTRA_APP_NAME, this@BlockOverlayActivity.intent.getStringExtra(EXTRA_APP_NAME))
                putExtra(EXTRA_REMAINING_MINUTES, this@BlockOverlayActivity.intent.getLongExtra(EXTRA_REMAINING_MINUTES, 0L))
                putExtra(EXTRA_PACKAGE_NAME, this@BlockOverlayActivity.intent.getStringExtra(EXTRA_PACKAGE_NAME))
                putExtra(EXTRA_DOMAIN, this@BlockOverlayActivity.intent.getStringExtra(EXTRA_DOMAIN))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }
}
