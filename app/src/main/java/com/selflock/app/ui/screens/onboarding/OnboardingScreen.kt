package com.selflock.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selflock.app.util.PermissionHelper

data class PermissionStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = remember {
        listOf(
            PermissionStep(
                title = "Welcome to SelfLock",
                description = "Take control of your digital habits. Block distracting apps and websites on your schedule.",
                icon = Icons.Filled.Lock,
                action = {}
            ),
            PermissionStep(
                title = "Accessibility Service",
                description = "Detects which app is in the foreground and reads browser URLs for usage tracking.",
                icon = Icons.Filled.Accessibility,
                action = { context.startActivity(PermissionHelper.getAccessibilitySettingsIntent()) }
            ),
            PermissionStep(
                title = "Usage Access",
                description = "Tracks how long you spend in each app for daily budget enforcement.",
                icon = Icons.Filled.BarChart,
                action = { context.startActivity(PermissionHelper.getUsageAccessSettingsIntent()) }
            ),
            PermissionStep(
                title = "Display Over Other Apps",
                description = "Shows a blocking overlay when you try to open a restricted app.",
                icon = Icons.Filled.Layers,
                action = { context.startActivity(PermissionHelper.getOverlaySettingsIntent()) }
            ),
            PermissionStep(
                title = "Notifications",
                description = "Shows persistent notifications about service status and budget warnings.",
                icon = Icons.Filled.Notifications,
                action = { context.startActivity(PermissionHelper.getNotificationSettingsIntent(context)) }
            ),
            PermissionStep(
                title = "Exact Alarms",
                description = "Schedules precise block/unblock times for your rules.",
                icon = Icons.Filled.Security,
                action = { context.startActivity(PermissionHelper.getExactAlarmSettingsIntent()) }
            ),
            PermissionStep(
                title = "Battery Optimization",
                description = "Disabling battery optimization ensures SelfLock runs continuously in the background.",
                icon = Icons.Filled.BatteryAlert,
                action = { context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context)) }
            )
        )
    }

    val step = steps[currentStep]
    val progress = (currentStep + 1).toFloat() / steps.size

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (currentStep > 0) {
                Button(
                    onClick = { step.action() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (currentStep < steps.lastIndex) {
                        currentStep++
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentStep < steps.lastIndex) "Next" else "Get Started")
            }

            if (currentStep > 0 && currentStep < steps.lastIndex) {
                TextButton(onClick = { currentStep++ }) {
                    Text("Skip")
                }
            }
        }
    }
}
