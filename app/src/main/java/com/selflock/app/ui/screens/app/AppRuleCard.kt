package com.selflock.app.ui.screens.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.selflock.app.ui.components.AppIcon
import com.selflock.app.ui.util.Formatters

@Composable
fun AppRuleCard(
    uiState: AppRuleUiState,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rule = uiState.ruleWithApps.rule
    val now = System.currentTimeMillis()
    val freeUntil = maxOf(uiState.rewardActiveUntil, uiState.contingencyActiveUntil)
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AppIcon(rule.progressPackageName, Modifier.size(40.dp), rule.progressAppName)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${uiState.ruleWithApps.allowedApps.size} aplicativos permitidos", style = MaterialTheme.typography.bodySmall)
                    }
                    if (rule.isPasswordProtected) Icon(Icons.Filled.Key, "Protegido por senha", Modifier.padding(start = 8.dp).size(16.dp))
                    if (uiState.isActive) Icon(Icons.Filled.Lock, "Bloqueio ativo", Modifier.padding(start = 8.dp).size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!uiState.isActive) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Excluir") }
                    Switch(rule.isEnabled, { onToggle() }, enabled = !uiState.isActive)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("${Formatters.formatTime(rule.scheduleStartHour, rule.scheduleStartMinute)} - ${Formatters.formatTime(rule.scheduleEndHour, rule.scheduleEndMinute)}")
            Text(Formatters.formatDays(rule.getDaysList()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("${rule.goalMinutes} min em ${rule.progressAppName} libera ${rule.rewardMinutes} min")
            val progress = (uiState.progressSeconds.toFloat() / (rule.goalMinutes * 60L)).coerceIn(0f, 1f)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Text("Recompensas: ${uiState.rewardsUsed}/${rule.maxRewards}", style = MaterialTheme.typography.bodySmall)
            Text("Liberação alternativa após ${rule.contingencyAfterMinutes / 60}h por ${rule.contingencyMinutes}m", style = MaterialTheme.typography.bodySmall)
            if (uiState.isActive) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (freeUntil > now) "Tempo livre ativo" else Formatters.formatCountdown(uiState.remainingMinutes),
                    color = if (freeUntil > now) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
