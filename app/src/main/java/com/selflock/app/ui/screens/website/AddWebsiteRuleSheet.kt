package com.selflock.app.ui.screens.website

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selflock.app.domain.model.BlockType
import com.selflock.app.ui.components.PasswordProtectionSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebsiteRuleSheet(
    onDismiss: () -> Unit,
    onSave: (
        domain: String,
        blockType: BlockType,
        startHour: Int?,
        startMinute: Int?,
        endHour: Int?,
        endMinute: Int?,
        days: String,
        dailyLimitMinutes: Int?,
        isPasswordProtected: Boolean,
        passwordHash: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var domain by remember { mutableStateOf("") }
    var blockType by remember { mutableStateOf(BlockType.SCHEDULE) }
    var selectedDays by remember { mutableStateOf(setOf("MON", "TUE", "WED", "THU", "FRI")) }
    var dailyLimitText by remember { mutableStateOf("") }
    var isPasswordProtected by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val startTimeState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
    val endTimeState = rememberTimePickerState(initialHour = 17, initialMinute = 0)
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth()
        ) {
            Text("Add Website Rule", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Domain") },
                placeholder = { Text("e.g., youtube.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = blockType == BlockType.SCHEDULE,
                    onClick = { blockType = BlockType.SCHEDULE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Schedule")
                }
                SegmentedButton(
                    selected = blockType == BlockType.DAILY_LIMIT,
                    onClick = { blockType = BlockType.DAILY_LIMIT },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Daily Limit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (blockType == BlockType.SCHEDULE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showStartTimePicker = true }) {
                        Text("Start: ${String.format("%02d:%02d", startTimeState.hour, startTimeState.minute)}")
                    }
                    TextButton(onClick = { showEndTimePicker = true }) {
                        Text("End: ${String.format("%02d:%02d", endTimeState.hour, endTimeState.minute)}")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                            },
                            label = { Text(day.take(1)) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = dailyLimitText,
                    onValueChange = { dailyLimitText = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily limit (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PasswordProtectionSection(
                isEnabled = isPasswordProtected,
                onToggle = { isPasswordProtected = it },
                password = password,
                onPasswordChange = { password = it; passwordError = null },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it; passwordError = null },
                error = passwordError
            )

            Spacer(modifier = Modifier.height(24.dp))

            val domainRegex = remember { Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$") }
            val isDomainValid = domain.isNotBlank() && domainRegex.matches(domain)

            Button(
                onClick = {
                    if (isPasswordProtected) {
                        when {
                            password.length < 4 -> {
                                passwordError = "Password must be at least 4 characters"
                                return@Button
                            }
                            password != confirmPassword -> {
                                passwordError = "Passwords do not match"
                                return@Button
                            }
                        }
                    }
                    if (isDomainValid) {
                        onSave(
                            domain,
                            blockType,
                            if (blockType == BlockType.SCHEDULE) startTimeState.hour else null,
                            if (blockType == BlockType.SCHEDULE) startTimeState.minute else null,
                            if (blockType == BlockType.SCHEDULE) endTimeState.hour else null,
                            if (blockType == BlockType.SCHEDULE) endTimeState.minute else null,
                            selectedDays.joinToString(","),
                            if (blockType == BlockType.DAILY_LIMIT) dailyLimitText.toIntOrNull() else null,
                            isPasswordProtected,
                            if (isPasswordProtected) password else null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isDomainValid
            ) {
                Text("Save Rule")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                showStartTimePicker = false
            },
            state = startTimeState
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                showEndTimePicker = false
            },
            state = endTimeState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    state: TimePickerState
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = state)
        }
    )
}
