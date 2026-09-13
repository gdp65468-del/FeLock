package com.selflock.app.ui.screens.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.selflock.app.data.local.entity.LockoutRuleWithApps
import com.selflock.app.domain.usecase.InstalledApp
import com.selflock.app.ui.components.PasswordProtectionSection
import com.selflock.app.util.ScheduleDuration

data class RewardDraft(
    val name: String = "",
    val requiredMinutes: String = "7",
    val scheduled: Boolean = false,
    val startTime: String = "07:00",
    val endTime: String = "08:00",
    val durationMinutes: String = "15",
    val releaseType: String = "TEMPORARY",
    val releasedPackages: Set<String> = emptySet()
)

data class RuleEditorData(
    val name: String,
    val blockedApps: List<InstalledApp>,
    val taskApps: List<InstalledApp>,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val days: String,
    val rewards: List<RewardDraft>,
    val blockSettings: Boolean,
    val passwordProtected: Boolean,
    val password: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppRuleSheet(
    installedApps: List<InstalledApp>,
    initialRule: LockoutRuleWithApps? = null,
    limitSchedulesToTwelveHours: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (RuleEditorData) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initialRule) { mutableStateOf(initialRule?.rule?.name.orEmpty()) }
    var blockedPackages by remember(initialRule) { mutableStateOf(initialRule?.blockedApps?.map { it.packageName }?.toSet().orEmpty()) }
    var taskPackages by remember(initialRule) { mutableStateOf((initialRule?.taskApps?.map { it.packageName } ?: listOfNotNull(initialRule?.rule?.progressPackageName)).toSet()) }
    var selectedDays by remember(initialRule) { mutableStateOf(initialRule?.rule?.getDaysList()?.toSet() ?: setOf("MON", "TUE", "WED", "THU", "FRI")) }
    var search by remember { mutableStateOf("") }
    var rewards by remember(initialRule) {
        mutableStateOf(initialRule?.rewards?.sortedBy { it.reward.position }?.map { item ->
            RewardDraft(
                name = item.reward.name,
                requiredMinutes = item.reward.requiredMinutes.toString(),
                scheduled = item.reward.availabilityStartHour != null,
                startTime = "%02d:%02d".format(item.reward.availabilityStartHour ?: 7, item.reward.availabilityStartMinute ?: 0),
                endTime = "%02d:%02d".format(item.reward.availabilityEndHour ?: 8, item.reward.availabilityEndMinute ?: 0),
                durationMinutes = item.reward.durationMinutes.toString(),
                releaseType = item.reward.releaseType,
                releasedPackages = item.releasedApps.map { it.packageName }.toSet()
            )
        }?.ifEmpty { null } ?: listOf(RewardDraft(releasedPackages = blockedPackages)))
    }
    var blockSettings by remember(initialRule) { mutableStateOf(initialRule?.rule?.blockSettings ?: false) }
    var passwordProtected by remember(initialRule) { mutableStateOf(initialRule?.rule?.isPasswordProtected ?: false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var expandedSection by remember { mutableStateOf(1) }
    val start = rememberTimePickerState(initialHour = initialRule?.rule?.scheduleStartHour ?: 19, initialMinute = initialRule?.rule?.scheduleStartMinute ?: 0)
    val end = rememberTimePickerState(initialHour = initialRule?.rule?.scheduleEndHour ?: 23, initialMinute = initialRule?.rule?.scheduleEndMinute ?: 0)
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    val filteredApps = installedApps.filter { it.appName.contains(search, true) || it.packageName.contains(search, true) }
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val dayLabels = mapOf("MON" to "S", "TUE" to "T", "WED" to "Q", "THU" to "Q", "FRI" to "S", "SAT" to "S", "SUN" to "D")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (initialRule == null) "Novo plano de foco" else "Editar plano de foco", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Defina quando o bloqueio funciona, o que você fará e como conquistará suas recompensas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Como funciona", fontWeight = FontWeight.Bold)
                        Text("1. Os apps escolhidos ficam bloqueados no horário do plano.")
                        Text("2. Use um app de tarefa para acumular progresso.")
                        Text("3. Complete as recompensas em ordem para liberar apenas os apps definidos.")
                    }
                }
            }
            item { OutlinedTextField(name, { name = it }, label = { Text("Nome do plano") }, supportingText = { Text("Exemplo: Dormir, Estudar ou Trabalho") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { SectionHeader(1, "Horário do bloqueio", "%02d:%02d às %02d:%02d • ${selectedDays.size} dias".format(start.hour, start.minute, end.hour, end.minute), expandedSection == 1) { expandedSection = if (expandedSection == 1) 0 else 1 } }
            if (expandedSection == 1) item {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton({ showStart = true }) { Text("Início: %02d:%02d".format(start.hour, start.minute)) }
                        TextButton({ showEnd = true }) { Text("Fim: %02d:%02d".format(end.hour, end.minute)) }
                    }
                    val duration = ScheduleDuration.minutes(start.hour, start.minute, end.hour, end.minute)
                    if (!ScheduleDuration.isAllowed(start.hour, start.minute, end.hour, end.minute, limitSchedulesToTwelveHours)) {
                        Text(if (duration == 0) "O início e o fim devem ser diferentes" else "O bloqueio não pode passar de 12 horas", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (expandedSection == 1) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { day -> FilterChip(day in selectedDays, { selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day }, { Text(dayLabels.getValue(day)) }) }
                }
            }
            if (expandedSection == 1) item { Text("O bloqueio começa e termina automaticamente nos dias selecionados. Horários noturnos podem atravessar a meia-noite.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { SectionHeader(2, "Aplicativos bloqueados", "${blockedPackages.size} selecionados", expandedSection == 2) { expandedSection = if (expandedSection == 2) 0 else 2; search = "" } }
            if (expandedSection == 2) item { Text("Escolha somente os apps que deseja impedir durante este plano. Apps essenciais do Android não aparecem aqui.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (expandedSection == 2) item { OutlinedTextField(search, { search = it }, label = { Text("Buscar apps para bloquear") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            if (expandedSection == 2) items(filteredApps, key = { "blocked-${it.packageName}" }) { app ->
                AppSelectionRow(app, app.packageName in blockedPackages) {
                    blockedPackages = toggle(blockedPackages, app.packageName)
                    if (app.packageName !in blockedPackages) rewards = rewards.map { it.copy(releasedPackages = it.releasedPackages - app.packageName) }
                    if (app.packageName in blockedPackages) taskPackages -= app.packageName
                }
            }
            item { SectionHeader(3, "Aplicativos de tarefa", "${taskPackages.size} selecionados", expandedSection == 3) { expandedSection = if (expandedSection == 3) 0 else 3; search = "" } }
            if (expandedSection == 3) item { Text("O tempo só avança enquanto um destes apps estiver realmente em primeiro plano. Você pode sair e continuar depois sem perder o progresso.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (expandedSection == 3) item { OutlinedTextField(search, { search = it }, label = { Text("Buscar apps de tarefa") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            if (expandedSection == 3) items(filteredApps, key = { "task-${it.packageName}" }) { app ->
                AppSelectionRow(app, app.packageName in taskPackages) {
                    taskPackages = toggle(taskPackages, app.packageName)
                    if (app.packageName in taskPackages) blockedPackages -= app.packageName
                }
            }
            item { SectionHeader(4, "Recompensas", "${rewards.size} configuradas", expandedSection == 4) { expandedSection = if (expandedSection == 4) 0 else 4 } }
            if (expandedSection == 4) item { Text("As recompensas são conquistadas em ordem. Durante um tempo livre, somente os apps marcados naquela recompensa serão liberados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (expandedSection == 4) items(rewards.indices.toList(), key = { "reward-$it" }) { index ->
                RewardEditor(index, rewards[index], installedApps.filter { it.packageName in blockedPackages },
                    onChange = { changed -> rewards = rewards.toMutableList().also { it[index] = changed } },
                    onRemove = { rewards = rewards.toMutableList().also { it.removeAt(index) } })
            }
            if (expandedSection == 4) item { Button({ rewards = rewards + RewardDraft(releasedPackages = blockedPackages) }, modifier = Modifier.fillMaxWidth()) { Text("Adicionar recompensa") } }
            item { SectionHeader(5, "Proteção extra", if (passwordProtected) "Protegido por senha" else "Senha opcional", expandedSection == 5) { expandedSection = if (expandedSection == 5) 0 else 5 } }
            if (expandedSection == 5) item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Bloquear Configurações"); Text("Apps essenciais do sistema nunca serão bloqueados", style = MaterialTheme.typography.bodySmall) }
                    Switch(blockSettings, { blockSettings = it })
                }
            }
            if (expandedSection == 5) item {
                PasswordProtectionSection(passwordProtected, { passwordProtected = it }, password, { password = it; passwordError = null }, confirmPassword, { confirmPassword = it; passwordError = null }, passwordError)
            }
            item {
                val validRewards = rewards.isNotEmpty() && rewards.all { rewardValid(it) && (it.releaseType == "END_SESSION" || it.releasedPackages.isNotEmpty()) }
                val valid = name.isNotBlank() && blockedPackages.isNotEmpty() && taskPackages.isNotEmpty() && selectedDays.isNotEmpty() && validRewards && ScheduleDuration.isAllowed(start.hour, start.minute, end.hour, end.minute, limitSchedulesToTwelveHours)
                if (!valid) Text(
                    "Revise os campos: informe o nome, escolha apps bloqueados e de tarefa e configure pelo menos uma recompensa completa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = {
                    val passwordRequired = initialRule?.rule?.isPasswordProtected != true
                    if (passwordProtected && (password.isNotEmpty() || passwordRequired) && (password.length < 4 || password != confirmPassword)) {
                        passwordError = if (password.length < 4) "A senha deve ter pelo menos 4 caracteres" else "As senhas não coincidem"
                    } else onSave(RuleEditorData(name, installedApps.filter { it.packageName in blockedPackages }, installedApps.filter { it.packageName in taskPackages }, start.hour, start.minute, end.hour, end.minute, selectedDays.joinToString(","), rewards, blockSettings, passwordProtected, password.takeIf { passwordProtected && it.isNotEmpty() }))
                }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text(if (initialRule == null) "Salvar bloqueio" else "Salvar alterações") }
            }
            item { TextButton(onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
    if (showStart) TimePickerDialog({ showStart = false }, { showStart = false }, start)
    if (showEnd) TimePickerDialog({ showEnd = false }, { showEnd = false }, end)
}

@Composable
private fun SectionHeader(number: Int, title: String, summary: String, expanded: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (expanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text(number.toString(), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (expanded) "Fechar" else "Configurar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable private fun AppSelectionRow(app: InstalledApp, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(app.appName) },
        supportingContent = { Text(if (selected) "Selecionado" else "Toque para selecionar") },
        trailingContent = { Switch(selected, null) },
        modifier = Modifier.clickable(onClickLabel = if (selected) "Remover ${app.appName}" else "Selecionar ${app.appName}", onClick = onClick)
    )
}

@Composable private fun RewardEditor(index: Int, reward: RewardDraft, blockedApps: List<InstalledApp>, onChange: (RewardDraft) -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${index + 1}. ${reward.name.ifBlank { "Recompensa sem nome" }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Conclua a tarefa para desbloquear os apps escolhidos abaixo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(reward.name, { onChange(reward.copy(name = it)) }, label = { Text("Nome opcional") }, modifier = Modifier.fillMaxWidth())
            NumberField(reward.requiredMinutes, { onChange(reward.copy(requiredMinutes = it)) }, "Tempo necessário no app de tarefa (min)")
            Text("Quando pode ser conquistada", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!reward.scheduled, { onChange(reward.copy(scheduled = false)) }, { Text("Imediatamente") })
                FilterChip(reward.scheduled, { onChange(reward.copy(scheduled = true)) }, { Text("Em um horário") })
            }
            if (reward.scheduled) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(reward.startTime, { onChange(reward.copy(startTime = it.take(5))) }, label = { Text("Início") }, modifier = Modifier.weight(1f))
                OutlinedTextField(reward.endTime, { onChange(reward.copy(endTime = it.take(5))) }, label = { Text("Fim") }, modifier = Modifier.weight(1f))
            }
            Text("Tipo de liberação")
            listOf("TEMPORARY" to "Por tempo definido", "UNTIL_END" to "Até o fim da sessão", "END_SESSION" to "Encerrar bloqueio atual").forEach { (value, label) ->
                FilterChip(reward.releaseType == value, { onChange(reward.copy(releaseType = value)) }, { Text(label) })
            }
            Text(
                when (reward.releaseType) {
                    "UNTIL_END" -> "Os apps escolhidos ficam livres até o horário final deste bloqueio."
                    "END_SESSION" -> "Encerra este ciclo e libera todos os apps até o próximo início agendado."
                    else -> "Os apps escolhidos ficam livres pelo número de minutos definido."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (reward.releaseType == "TEMPORARY") NumberField(reward.durationMinutes, { onChange(reward.copy(durationMinutes = it)) }, "Duração da recompensa (min)")
            if (reward.releaseType != "END_SESSION") {
                Text("Aplicativos liberados (${reward.releasedPackages.size})", fontWeight = FontWeight.Bold)
                blockedApps.forEach { app -> AppSelectionRow(app, app.packageName in reward.releasedPackages) { onChange(reward.copy(releasedPackages = toggle(reward.releasedPackages, app.packageName))) } }
            }
            TextButton(onRemove) { Text("Remover recompensa") }
        }
    }
}

private fun rewardValid(reward: RewardDraft): Boolean = (reward.requiredMinutes.toIntOrNull() ?: 0) > 0 && (reward.releaseType != "TEMPORARY" || (reward.durationMinutes.toIntOrNull() ?: 0) > 0) && (!reward.scheduled || parseTime(reward.startTime) != null && parseTime(reward.endTime) != null && reward.startTime != reward.endTime)
private fun parseTime(value: String): Pair<Int, Int>? { val parts = value.split(":"); val h = parts.getOrNull(0)?.toIntOrNull() ?: return null; val m = parts.getOrNull(1)?.toIntOrNull() ?: return null; return if (h in 0..23 && m in 0..59) h to m else null }
private fun toggle(values: Set<String>, value: String): Set<String> = if (value in values) values - value else values + value

@Composable private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String) = OutlinedTextField(value, { onValueChange(it.filter(Char::isDigit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, state: TimePickerState) = androidx.compose.material3.AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onConfirm) { Text("OK") } }, dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }, text = { TimePicker(state = state) })
