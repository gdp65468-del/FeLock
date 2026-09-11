package com.selflock.app.ui.screens.app

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Create lockout", style = MaterialTheme.typography.headlineSmall) }
            item {
                OutlinedTextField(name, { name = it }, label = { Text("Lockout name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showStart = true }) { Text("Start: %02d:%02d".format(start.hour, start.minute)) }
                    TextButton(onClick = { showEnd = true }) { Text("End: %02d:%02d".format(end.hour, end.minute)) }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = { selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day },
                            label = { Text(day.take(1)) }
                        )
                    }
                }
            }
            item { Text("Allowed apps", style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(search, { search = it }, label = { Text("Search apps") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                item { Text("Progress app", style = MaterialTheme.typography.titleMedium) }
                items(selectedApps, key = { "progress-${it.packageName}" }) { app ->
                    FilterChip(
                        selected = progressPackage == app.packageName,
                        onClick = { progressPackage = app.packageName },
                        label = { Text(app.appName) }
                    )
                }
            }
            item { NumberField(goal, { goal = it }, "Minutes required in progress app") }
            item { NumberField(reward, { reward = it }, "Free-time reward (minutes)") }
            item { NumberField(maxRewards, { maxRewards = it }, "Maximum rewards") }
            item { NumberField(contingencyAfter, { contingencyAfter = it }, "Alternative release after (hours)") }
            item { NumberField(contingencyDuration, { contingencyDuration = it }, "Alternative release duration (minutes)") }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block Settings")
                        Text("Prevent opening Android Settings during lockout", style = MaterialTheme.typography.bodySmall)
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
                val valid = name.isNotBlank() && selectedApps.isNotEmpty() && progressApp != null && selectedDays.isNotEmpty() &&
                    listOf(goal, reward, maxRewards, contingencyAfter, contingencyDuration).all { (it.toIntOrNull() ?: 0) > 0 }
                Button(
                    onClick = {
                        if (passwordProtected && (password.length < 4 || password != confirmPassword)) {
                            passwordError = if (password.length < 4) "Password must be at least 4 characters" else "Passwords do not match"
                        } else {
                            onSave(
                                name, selectedApps, progressApp!!, start.hour, start.minute, end.hour, end.minute,
                                selectedDays.joinToString(","), goal.toInt(), reward.toInt(), maxRewards.toInt(),
                                contingencyAfter.toInt() * 60, contingencyDuration.toInt(), blockSettings,
                                passwordProtected, password.takeIf { passwordProtected }
                            )
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save lockout") }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showStart) TimePickerDialog({ showStart = false }, { _, _ -> showStart = false }, start)
    if (showEnd) TimePickerDialog({ showEnd = false }, { _, _ -> showEnd = false }, end)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) }
    )
}
