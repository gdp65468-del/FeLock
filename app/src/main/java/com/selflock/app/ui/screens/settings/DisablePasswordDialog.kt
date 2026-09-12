package com.selflock.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DisablePasswordDialog(
    onDismiss: () -> Unit,
    onVerified: suspend () -> Unit,
    verifyPassword: suspend (String) -> Boolean
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Desativar senha mestra") },
        text = {
            Column {
                Text("Digite sua senha atual para desativar a proteção por senha mestra.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Senha atual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isVerifying) return@TextButton
                    isVerifying = true
                    scope.launch {
                        if (verifyPassword(password)) {
                            onVerified()
                            onDismiss()
                        } else {
                            error = "Senha incorreta"
                        }
                        isVerifying = false
                    }
                },
                enabled = !isVerifying && password.isNotEmpty()
            ) {
                Text("Desativar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
