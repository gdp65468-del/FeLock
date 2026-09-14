package com.selflock.app.ui.screens.app

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
fun AppBlockScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
    viewModel: AppBlockViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val showAddSheet by viewModel.showAddSheet.collectAsState()
    val editingRule by viewModel.editingRule.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloqueios") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddSheet() }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar")
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
                    text = "Nenhum bloqueio criado.\nToque em + para criar um.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(rules, key = { it.ruleWithApps.rule.id }) { uiState ->
                    AppRuleCard(
                        uiState = uiState,
                        onToggle = { viewModel.toggleRule(uiState.ruleWithApps.rule) },
                        onEdit = { viewModel.editRule(uiState.ruleWithApps) },
                        onDuplicate = { viewModel.duplicateRule(uiState.ruleWithApps) },
                        onDelete = { viewModel.deleteRule(uiState.ruleWithApps.rule) }
                    )
                }
            }
        }
    }

    if (editingRule != null) {
        val original = editingRule!!
        AddAppRuleSheet(
            installedApps = installedApps,
            initialRule = original,
            savedDraft = viewModel.editDraft,
            limitSchedulesToTwelveHours = viewModel.limitSchedulesToTwelveHours,
            onDismiss = { viewModel.hideEditSheet() },
            onDiscard = { viewModel.discardEditDraft() },
            onDraftChange = viewModel::saveEditDraft,
            onSave = { viewModel.updateRule(original, it) }
        )
    }

    if (showAddSheet) {
        AddAppRuleSheet(
            installedApps = installedApps,
            savedDraft = viewModel.addDraft,
            limitSchedulesToTwelveHours = viewModel.limitSchedulesToTwelveHours,
            onDismiss = { viewModel.hideAddSheet() },
            onDiscard = { viewModel.discardAddDraft() },
            onDraftChange = viewModel::saveAddDraft,
            onSave = viewModel::addRule
        )
    }

    if (pendingAction != null) {
        val action = pendingAction!!
        PasswordEntryDialog(
            title = "Senha necessária",
            message = if (viewModel.isMasterPasswordEnabled()) "Digite a senha da regra ou a senha mestra" else "Digite a senha da regra",
            onDismiss = { viewModel.dismissPendingAction() },
            onVerified = { viewModel.executePendingAction() },
            verifyPassword = { password -> viewModel.verifyPassword(password, action.rule) }
        )
    }
}
