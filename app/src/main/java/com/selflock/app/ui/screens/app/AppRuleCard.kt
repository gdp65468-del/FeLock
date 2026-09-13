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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rule = uiState.ruleWithApps.rule
    val now = System.currentTimeMillis()
    val freeUntil = maxOf(uiState.rewardActiveUntil, uiState.contingencyActiveUntil)
    val freeTimeActive = freeUntil > now
    val overnight = !rule.getStartTime().isBefore(rule.getEndTime())
    val dayLabels = mapOf("MON" to "Seg", "TUE" to "Ter", "WED" to "Qua", "THU" to "Qui", "FRI" to "Sex", "SAT" to "Sáb", "SUN" to "Dom")
    val selectedDays = rule.getDaysList()
    val daysText = if (selectedDays.size == 7) "Todos os dias" else selectedDays.joinToString(", ") { dayLabels[it] ?: it }
    val nextReward = uiState.ruleWithApps.rewards.sortedBy { it.reward.position }.getOrNull(uiState.rewardsUsed)
    val requiredMinutes = nextReward?.reward?.requiredMinutes ?: rule.goalMinutes
    val progress = (uiState.progressSeconds.toFloat() / (requiredMinutes * 60L)).coerceIn(0f, 1f)
    val progressMinutes = uiState.progressSeconds / 60
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AppIcon(rule.progressPackageName, Modifier.size(44.dp), rule.progressAppName)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (rule.isPasswordProtected) {
                                Icon(Icons.Filled.Key, "Protegido por senha", Modifier.padding(start = 8.dp).size(16.dp))
                            }
                        }
                        Text(
                            if (rule.usesBlockedApps) "${uiState.ruleWithApps.blockedApps.size} apps bloqueados • ${uiState.ruleWithApps.taskApps.size} de tarefa" else "${uiState.ruleWithApps.allowedApps.size} aplicativos permitidos",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (uiState.endedByReward) "Encerrado" else if (uiState.isActive) "Ativo agora" else if (rule.isEnabled) "Agendado" else "Pausado",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(rule.isEnabled, { onToggle() }, enabled = !uiState.isActive)
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "Mais opções")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar configuração") },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                enabled = !uiState.isActive,
                                onClick = { showMenu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicar bloqueio") },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                onClick = { showMenu = false; onDuplicate() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (rule.isEnabled) "Pausar bloqueio" else "Ativar bloqueio") },
                                leadingIcon = { Icon(if (rule.isEnabled) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle, null) },
                                enabled = !uiState.isActive,
                                onClick = { showMenu = false; onToggle() }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir bloqueio") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                enabled = !uiState.isActive,
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Horário", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "${Formatters.formatTime(rule.scheduleStartHour, rule.scheduleStartMinute)} às ${Formatters.formatTime(rule.scheduleEndHour, rule.scheduleEndMinute)}${if (overnight) " (dia seguinte)" else ""} • $daysText"
            )
            if (overnight) {
                Text("Passa da meia-noite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))
            Text("Tempo livre", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            val taskNames = if (rule.usesBlockedApps) uiState.ruleWithApps.taskApps.joinToString { it.appName } else rule.progressAppName
            Text(if (nextReward != null) "$taskNames: $progressMinutes de ${nextReward.reward.requiredMinutes} minutos concluídos" else "Todas as recompensas foram concluídas")
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                "Progresso da tarefa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Text("Recompensa", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("${uiState.rewardsUsed} de ${if (rule.usesBlockedApps) uiState.ruleWithApps.rewards.size else rule.maxRewards} concluídas")
            if (nextReward != null) Text("Próxima: ${nextReward.reward.name.ifBlank { "Recompensa ${nextReward.reward.position + 1}" }}", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))
            Text("Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                when {
                    uiState.endedByReward -> "Bloqueio encerrado por recompensa"
                    freeTimeActive -> "Tempo livre ativo"
                    uiState.isActive -> Formatters.formatCountdown(uiState.remainingMinutes)
                    rule.isEnabled -> "Aguardando o próximo horário"
                    else -> "Bloqueio pausado"
                },
                fontWeight = FontWeight.Bold,
                color = if (freeTimeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (uiState.isActive) {
                Text("A edição fica disponível quando o período ativo terminar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
