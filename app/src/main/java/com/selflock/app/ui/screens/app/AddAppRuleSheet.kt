package com.selflock.app.ui.screens.app

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selflock.app.domain.usecase.InstalledApp
import com.selflock.app.ui.components.AppIcon
import com.selflock.app.ui.components.PasswordProtectionSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppRuleSheet(
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onSave: (
        String, List<InstalledApp>, InstalledApp, Int, Int, Int, Int, String,
        Int, Int, Int, Int, Int, Boolean, Boolean, String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var progressPackage by remember { mutableStateOf<String?>(null) }
    var selectedDays by remember { mutableStateOf(setOf("MON", "TUE", "WED", "THU", "FRI")) }
    var search by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("30") }
    var reward by remember { mutableStateOf("15") }
    var maxRewards by remember { mutableStateOf("3") }
    var contingencyAfter by remember { mutableStateOf("2") }
    var contingencyDuration by remember { mutableStateOf("10") }
    var blockSettings by remember { mutableStateOf(true) }
    var passwordProtected by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val start = rememberTimePickerState(initialHour = 19, initialMinute = 0)
    val end = rememberTimePickerState(initialHour = 23, initialMinute = 0)
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    val selectedApps = installedApps.filter { it.packageName in selectedPackages }
    val progressApp = installedApps.firstOrNull { it.packageName == progressPackage }
    val filteredApps = installedApps.filter { it.appName.contains(search, true) || it.packageName.contains(search, true) }
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val goalValue = goal.toIntOrNull()
    val rewardValue = reward.toIntOrNull()
    val maxRewardsValue = maxRewards.toIntOrNull()
    val contingencyAfterValue = contingencyAfter.toIntOrNull()
    val contingencyDurationValue = contingencyDuration.toIntOrNull()
    val validationMessage = when {
        name.isBlank() -> "Informe um nome para identificar o bloqueio."
        selectedDays.isEmpty() -> "Selecione pelo menos um dia da semana."
        selectedApps.isEmpty() -> "Selecione pelo menos um aplicativo permitido."
        progressApp == null -> "Escolha qual aplicativo permitido acumulará progresso."
        goalValue == null || goalValue !in 1..1440 -> "A meta deve ficar entre 1 e 1.440 minutos."
        rewardValue == null || rewardValue !in 1..1440 -> "A recompensa deve ficar entre 1 e 1.440 minutos."
        maxRewardsValue == null || maxRewardsValue !in 1..20 -> "O máximo de recompensas deve ficar entre 1 e 20."
        contingencyAfterValue == null || contingencyAfterValue !in 1..168 -> "A espera alternativa deve ficar entre 1 e 168 horas."
        contingencyDurationValue == null || contingencyDurationValue !in 1..1440 -> "A liberação alternativa deve ficar entre 1 e 1.440 minutos."
        else -> null
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Criar bloqueio", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Durante o período escolhido, somente os aplicativos permitidos poderão ser usados.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                SectionHeader(
                    "1. Horário do bloqueio",
                    "Defina quando o bloqueio começa, termina e em quais dias ele será repetido."
                )
            }
            item {
                OutlinedTextField(name, { name = it }, label = { Text("Nome do bloqueio") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showStart = true }) { Text("Início: %02d:%02d".format(start.hour, start.minute)) }
                    TextButton(onClick = { showEnd = true }) { Text("Fim: %02d:%02d".format(end.hour, end.minute)) }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = { selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day },
                            label = { Text(dayLabel(day)) }
                        )
                    }
                }
            }
            item {
                SectionHeader(
                    "2. Aplicativos permitidos",
                    "Estes aplicativos continuarão funcionando. Todos os demais ficarão bloqueados."
                )
            }
            item {
                OutlinedTextField(search, { search = it }, label = { Text("Pesquisar aplicativos") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item { Text("${selectedApps.size} aplicativo(s) permitido(s)", style = MaterialTheme.typography.bodySmall) }
            if (filteredApps.isEmpty()) {
                item { Text("Nenhum aplicativo encontrado.", style = MaterialTheme.typography.bodySmall) }
            } else if (filteredApps.size > 12) {
                item { Text("Mostrando os 12 primeiros resultados. Use a pesquisa para encontrar outros aplicativos.", style = MaterialTheme.typography.bodySmall) }
            }
            items(filteredApps.take(12), key = { it.packageName }) { app ->
                val selected = app.packageName in selectedPackages
                ListItem(
                    leadingContent = { AppIcon(app.packageName, Modifier.size(36.dp), app.appName) },
                    headlineContent = { Text(app.appName) },
                    trailingContent = { Switch(selected, null) },
                    modifier = Modifier.clickable {
                        selectedPackages = if (selected) selectedPackages - app.packageName else selectedPackages + app.packageName
                        if (selected && progressPackage == app.packageName) progressPackage = null
                    }
                )
            }
            if (selectedApps.isNotEmpty()) {
                item {
                    SectionHeader(
                        "3. Aplicativo de progresso",
                        "O tempo de uso deste aplicativo é acumulado até atingir a meta e liberar o aparelho temporariamente."
                    )
                }
                items(selectedApps, key = { "progress-${it.packageName}" }) { app ->
                    FilterChip(
                        selected = progressPackage == app.packageName,
                        onClick = { progressPackage = app.packageName },
                        label = { Text(app.appName) }
                    )
                }
            }
            item {
                SectionHeader(
                    "4. Meta e recompensa",
                    "Ao cumprir a meta acumulada, o aparelho fica livre pelo tempo da recompensa. Isso pode se repetir até o limite definido."
                )
            }
            item { NumberField(goal, { goal = it }, "Meta no aplicativo de progresso (minutos)") }
            item { NumberField(reward, { reward = it }, "Tempo livre por recompensa (minutos)") }
            item { NumberField(maxRewards, { maxRewards = it }, "Máximo de recompensas por período") }
            item {
                SectionHeader(
                    "5. Liberação alternativa",
                    "Se a meta não for cumprida, esta opção permite liberar o aparelho após a espera configurada."
                )
            }
            item { NumberField(contingencyAfter, { contingencyAfter = it }, "Disponível depois de (horas)") }
            item { NumberField(contingencyDuration, { contingencyDuration = it }, "Duração da liberação (minutos)") }
            item {
                SectionHeader(
                    "6. Proteção",
                    "Escolha como impedir alterações que possam contornar o bloqueio."
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bloquear Configurações")
                        Text("Impede abrir as configurações do Android durante o bloqueio", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(blockSettings, { blockSettings = it })
                }
            }
            item {
                PasswordProtectionSection(
                    isEnabled = passwordProtected,
                    onToggle = { passwordProtected = it },
                    password = password,
                    onPasswordChange = { password = it; passwordError = null },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it; passwordError = null },
                    error = passwordError
                )
            }
            item {
                validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = {
                        if (passwordProtected && (password.length < 4 || password != confirmPassword)) {
                            passwordError = if (password.length < 4) "A senha deve ter pelo menos 4 caracteres" else "As senhas não coincidem"
                        } else {
                            onSave(
                                name, selectedApps, progressApp!!, start.hour, start.minute, end.hour, end.minute,
                                selectedDays.joinToString(","), goalValue!!, rewardValue!!, maxRewardsValue!!,
                                contingencyAfterValue!! * 60, contingencyDurationValue!!, blockSettings,
                                passwordProtected, password.takeIf { passwordProtected }
                            )
                        }
                    },
                    enabled = validationMessage == null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salvar bloqueio") }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showStart) TimePickerDialog({ showStart = false }, { _, _ -> showStart = false }, start)
    if (showEnd) TimePickerDialog({ showEnd = false }, { _, _ -> showEnd = false }, end)
}

@Composable
private fun SectionHeader(title: String, help: String) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { Toast.makeText(context, help, Toast.LENGTH_LONG).show() }) {
            Icon(Icons.Filled.Info, contentDescription = "Ajuda sobre $title")
        }
    }
    Text(help, style = MaterialTheme.typography.bodySmall)
}

private fun dayLabel(day: String) = when (day) {
    "MON" -> "2ª"
    "TUE" -> "3ª"
    "WED" -> "4ª"
    "THU" -> "5ª"
    "FRI" -> "6ª"
    "SAT" -> "Sáb"
    else -> "Dom"
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit, state: TimePickerState) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = { TimePicker(state = state) }
    )
}
