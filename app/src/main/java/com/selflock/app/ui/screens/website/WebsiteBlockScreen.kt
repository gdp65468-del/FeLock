package com.selflock.app.ui.screens.website

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selflock.app.ui.components.PasswordEntryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteBlockScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
    viewModel: WebsiteBlockViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val showAddSheet by viewModel.showAddSheet.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Website Blocks") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddSheet() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
        modifier = modifier
    ) { padding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No website rules yet.\nTap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(rules, key = { it.rule.id }) { uiState ->
                    WebsiteRuleCard(
                        uiState = uiState,
                        onToggle = { viewModel.toggleRule(uiState.rule) },
                        onDelete = { viewModel.deleteRule(uiState.rule) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddWebsiteRuleSheet(
            onDismiss = { viewModel.hideAddSheet() },
            onSave = { domain, blockType, startHour, startMinute, endHour, endMinute, days, dailyLimit, isPasswordProtected, password ->
                viewModel.addRule(domain, blockType, startHour, startMinute, endHour, endMinute, days, dailyLimit, isPasswordProtected, password)
            }
        )
    }

    if (pendingAction != null) {
        val action = pendingAction!!
        PasswordEntryDialog(
            title = "Password Required",
            message = if (viewModel.isMasterPasswordEnabled()) "Enter rule password or master password" else "Enter rule password",
            onDismiss = { viewModel.dismissPendingAction() },
            onVerified = { viewModel.executePendingAction() },
            verifyPassword = { password -> viewModel.verifyPassword(password, action.rule) }
        )
    }
}
