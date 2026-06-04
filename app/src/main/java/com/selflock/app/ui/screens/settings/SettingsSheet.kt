package com.selflock.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selflock.app.util.PermissionHelper

@Composable
fun SettingsSheet(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val permissions by viewModel.permissions.collectAsState()
    val masterPasswordEnabled by viewModel.masterPasswordEnabled.collectAsState()
    val context = LocalContext.current
    var showSetPasswordSheet by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Permissions Status",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        PermissionRow(
            name = "Accessibility Service",
            granted = permissions.accessibility,
            onFix = { context.startActivity(PermissionHelper.getAccessibilitySettingsIntent()) }
        )
        PermissionRow(
            name = "Usage Access",
            granted = permissions.usageAccess,
            onFix = { context.startActivity(PermissionHelper.getUsageAccessSettingsIntent()) }
        )
        PermissionRow(
            name = "Display Over Other Apps",
            granted = permissions.overlay,
            onFix = { context.startActivity(PermissionHelper.getOverlaySettingsIntent()) }
        )
        PermissionRow(
            name = "Notifications",
            granted = permissions.notifications,
            onFix = { context.startActivity(PermissionHelper.getNotificationSettingsIntent(context)) }
        )
        PermissionRow(
            name = "Exact Alarms",
            granted = permissions.exactAlarms,
            onFix = { context.startActivity(PermissionHelper.getExactAlarmSettingsIntent()) }
        )
        PermissionRow(
            name = "Battery Optimization",
            granted = permissions.batteryOptimization,
            grantedLabel = "Disabled",
            deniedLabel = "Enabled",
            onFix = { context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Security",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Master Password") },
            supportingContent = { Text("Require password to open the app") },
            trailingContent = {
                Switch(
                    checked = masterPasswordEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showSetPasswordSheet = true
                        } else {
                            showDisableDialog = true
                        }
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "About",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0.0") }
        )
    }

    if (showSetPasswordSheet) {
        SetPasswordSheet(
            onDismiss = { showSetPasswordSheet = false },
            onSetPassword = { password ->
                viewModel.setMasterPassword(password)
            }
        )
    }

    if (showDisableDialog) {
        DisablePasswordDialog(
            onDismiss = { showDisableDialog = false },
            onVerified = {
                viewModel.disableMasterPassword()
            },
            verifyPassword = { password ->
                viewModel.verifyPassword(password)
            }
        )
    }
}

@Composable
private fun PermissionRow(
    name: String,
    granted: Boolean,
    grantedLabel: String = "Granted",
    deniedLabel: String = "Denied",
    onFix: () -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = {
            Text(
                text = if (granted) grantedLabel else deniedLabel,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!granted) {
                FilledTonalButton(onClick = onFix) {
                    Text("Fix")
                }
            }
        }
    )
}
