package com.selflock.app.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.selflock.app.util.PermissionHelper
import android.accounts.AccountManager

data class PermissionStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: () -> Unit,
    val isGranted: () -> Boolean
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentStep by remember { mutableIntStateOf(0) }
    var checkTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val trigger = checkTrigger

    val chooseAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            viewModel.onAccountPicked(email)
        }
    }

    val recoveryAccount by viewModel.recoveryAccount.collectAsState()
    val passwordSet by viewModel.passwordSet.collectAsState()
    val isSavingPassword by viewModel.isSavingPassword.collectAsState()

    val steps = listOf(
        PermissionStep(
            title = "Welcome to SelfLock",
            description = "Take control of your digital habits. Block distracting apps and websites on your schedule.",
            icon = Icons.Filled.Lock,
            action = {},
            isGranted = { true }
        ),
        PermissionStep(
            title = "Recovery Account",
            description = "Pick a Google account on this device. It will be used to verify your identity if you ever forget your master password.",
            icon = Icons.Filled.AccountCircle,
            action = {
                chooseAccountLauncher.launch(viewModel.buildChooseAccountIntent())
            },
            isGranted = { recoveryAccount != null }
        ),
        PermissionStep(
            title = "Master Password",
            description = "Optionally set a master password. You'll need to enter it every time the app launches.",
            icon = Icons.Filled.VerifiedUser,
            action = {},
            isGranted = { passwordSet }
        ),
        PermissionStep(
            title = "Accessibility Service",
            description = "Detects which app is in the foreground and reads browser URLs for usage tracking.",
            icon = Icons.Filled.Accessibility,
            action = { context.startActivity(PermissionHelper.getAccessibilitySettingsIntent()) },
            isGranted = { PermissionHelper.isAccessibilityServiceEnabled(context) }
        ),
        PermissionStep(
            title = "Usage Access",
            description = "Tracks how long you spend in each app for daily budget enforcement.",
            icon = Icons.Filled.BarChart,
            action = { context.startActivity(PermissionHelper.getUsageAccessSettingsIntent()) },
            isGranted = { PermissionHelper.isUsageAccessGranted(context) }
        ),
        PermissionStep(
            title = "Display Over Other Apps",
            description = "Shows a blocking overlay when you try to open a restricted app.",
            icon = Icons.Filled.Layers,
            action = { context.startActivity(PermissionHelper.getOverlaySettingsIntent(context)) },
            isGranted = { PermissionHelper.isOverlayPermissionGranted(context) }
        ),
        PermissionStep(
            title = "Notifications",
            description = "Shows persistent notifications about service status and budget warnings.",
            icon = Icons.Filled.Notifications,
            action = { context.startActivity(PermissionHelper.getNotificationSettingsIntent(context)) },
            isGranted = { PermissionHelper.isNotificationPermissionGranted(context) }
        ),
        PermissionStep(
            title = "Exact Alarms",
            description = "Schedules precise block/unblock times for your rules.",
            icon = Icons.Filled.Security,
            action = { context.startActivity(PermissionHelper.getExactAlarmSettingsIntent()) },
            isGranted = { PermissionHelper.isExactAlarmAllowed(context) }
        ),
        PermissionStep(
            title = "Battery Optimization",
            description = "Disabling battery optimization ensures SelfLock runs continuously in the background.",
            icon = Icons.Filled.BatteryAlert,
            action = { context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context)) },
            isGranted = { PermissionHelper.isBatteryOptimizationDisabled(context) }
        )
    )

    val step = steps[currentStep]
    val progress = (currentStep + 1).toFloat() / steps.size
    val isAccountStep = currentStep == 1
    val isPasswordStep = currentStep == 2

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

                    if (isAccountStep) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (recoveryAccount != null) {
                            Text(
                                text = "Selected: $recoveryAccount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "No account selected yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isPasswordStep) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (passwordSet) {
                            Text(
                                text = "Master password is enabled. You can change it later in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            PasswordEntryFields(
                                onSet = { password ->
                                    viewModel.setMasterPassword(password) {}
                                },
                                isSaving = isSavingPassword
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isAccountStep && recoveryAccount != null) {
                OutlinedButton(
                    onClick = { viewModel.onAccountPicked(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Selection")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val isGranted = step.isGranted()

            if (!isGranted) {
                if (!isPasswordStep) {
                    Button(
                        onClick = { step.action() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAccountStep) "Choose Account" else "Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (currentStep < steps.lastIndex) {
                    TextButton(onClick = { currentStep++ }) {
                        Text("Skip")
                    }
                }
            } else {
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
            }
        }
    }
}

@Composable
private fun PasswordEntryFields(
    onSet: (String) -> Unit,
    isSaving: Boolean
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = password,
        onValueChange = { password = it; error = null },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = confirm,
        onValueChange = { confirm = it; error = null },
        label = { Text("Confirm password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = {
            when {
                password.length < 4 -> error = "Password must be at least 4 characters"
                password != confirm -> error = "Passwords do not match"
                else -> onSet(password)
            }
        },
        enabled = !isSaving && password.isNotEmpty() && confirm.isNotEmpty(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Set Password")
    }
}
