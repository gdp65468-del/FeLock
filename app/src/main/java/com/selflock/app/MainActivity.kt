package com.selflock.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.selflock.app.security.AppLockState
import com.selflock.app.security.MasterPasswordManager
import com.selflock.app.ui.navigation.MainNavGraph
import com.selflock.app.ui.screens.lock.LockScreenActivity
import com.selflock.app.ui.theme.SelfLockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var masterPasswordManager: MasterPasswordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (OnboardingActivity.shouldShow(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        if (masterPasswordManager.isEnabled() && !AppLockState.isUnlocked) {
            startActivity(Intent(this, LockScreenActivity::class.java))
            finish()
            return
        }

        if (masterPasswordManager.isEnabled()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        enableEdgeToEdge()

        setContent {
            SelfLockTheme {
                MainNavGraph()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        AppLockState.isUnlocked = false
    }

    override fun onResume() {
        super.onResume()
        if (masterPasswordManager.isEnabled() && !AppLockState.isUnlocked) {
            startActivity(Intent(this, LockScreenActivity::class.java))
            finish()
        }
    }
}
