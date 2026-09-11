package com.selflock.app.ui.screens.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    onResetApp: () -> Unit,
    viewModel: LockScreenViewModel = hiltViewModel()
) {
    val password by viewModel.password.collectAsState()
    val error by viewModel.error.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    val unlocked by viewModel.unlocked.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    if (unlocked) {
        onUnlocked()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SelfLock está bloqueado",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.onUnlock() }),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::onUnlock,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isVerifying && password.isNotEmpty()
        ) {
            if (isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Desbloquear")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { showForgotDialog = true }) {
            Text("Esqueceu a senha?")
        }
    }
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            viewModel = viewModel,
            onDismiss = { showForgotDialog = false },
            onResetApp = {
                showForgotDialog = false
                viewModel.onResetApp(onResetApp)
            }
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    viewModel: LockScreenViewModel,
    onDismiss: () -> Unit,
    onResetApp: () -> Unit
) {
    val recoveryEmail = remember { viewModel.getRecoveryAccountEmail() }
    val accountOnDevice = remember { viewModel.isRecoveryAccountOnDevice() }
    var stage by remember { mutableStateOf(ForgotStage.Confirm) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    when (stage) {
        ForgotStage.Confirm -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Esqueceu a senha?") },
                text = {
                    if (recoveryEmail != null && accountOnDevice) {
                        Text("Confirme com a conta de recuperação $recoveryEmail para definir uma nova senha mestra.")
                    } else if (recoveryEmail != null) {
                        Text("A conta de recuperação $recoveryEmail não está conectada neste dispositivo. Adicione-a nas configurações do Android ou redefina o aplicativo.")
                    } else {
                        Text("Nenhuma conta de recuperação foi definida. Só é possível redefinir o aplicativo, apagando bloqueios, configurações e a senha mestra.")
                    }
                },
                confirmButton = {
                    when {
                        recoveryEmail != null && accountOnDevice -> {
                            TextButton(onClick = { stage = ForgotStage.NewPassword }) {
                                Text("Usar $recoveryEmail")
                            }
                        }
                        else -> {
                            TextButton(onClick = onResetApp) {
                                Text("Redefinir aplicativo")
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            )
        }

        ForgotStage.NewPassword -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Definir nova senha mestra") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; resetError = null },
                            label = { Text("Nova senha") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = resetError != null,
                            supportingText = resetError?.let { { Text(it) } }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; resetError = null },
                            label = { Text("Confirmar senha") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = resetError != null
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isSaving) return@TextButton
                            when {
                                newPassword.length < 4 -> {
                                    resetError = "A senha deve ter pelo menos 4 caracteres"
                                }
                                newPassword != confirmPassword -> {
                                    resetError = "As senhas não coincidem"
                                }
                                else -> {
                                    isSaving = true
                                    viewModel.resetPasswordWithRecovery(newPassword) {
                                        isSaving = false
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        enabled = !isSaving && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

private enum class ForgotStage { Confirm, NewPassword }
