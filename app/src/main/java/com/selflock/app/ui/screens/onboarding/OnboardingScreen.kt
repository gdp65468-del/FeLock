package com.selflock.app.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.selflock.app.util.PermissionHelper
import android.accounts.AccountManager

data class PermissionStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: () -> Unit,
    val isGranted: () -> Boolean
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentStep by remember { mutableIntStateOf(0) }
    var checkTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val trigger = checkTrigger

    val chooseAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            viewModel.onAccountPicked(email)
        }
    }

    val recoveryAccount by viewModel.recoveryAccount.collectAsState()
    val passwordSet by viewModel.passwordSet.collectAsState()
    val isSavingPassword by viewModel.isSavingPassword.collectAsState()

    val steps = listOf(
        PermissionStep(
            title = "Bem-vindo ao feLock",
            description = "Assuma o controle dos seus hábitos digitais. Bloqueie aplicativos que distraem nos horários definidos.",
            icon = Icons.Filled.Lock,
            action = {},
            isGranted = { true }
        ),
        PermissionStep(
            title = "Conta de recuperação",
            description = "Escolha uma conta Google deste dispositivo. Ela será usada para confirmar sua identidade caso você esqueça a senha mestra.",
            icon = Icons.Filled.AccountCircle,
            action = {
                chooseAccountLauncher.launch(viewModel.buildChooseAccountIntent())
            },
            isGranted = { recoveryAccount != null }
        ),
        PermissionStep(
            title = "Senha mestra",
            description = "Defina uma senha mestra opcional. Você deverá digitá-la sempre que o aplicativo for aberto.",
            icon = Icons.Filled.VerifiedUser,
            action = {},
            isGranted = { passwordSet }
        ),
        PermissionStep(
            title = "Serviço de acessibilidade",
            description = "Detecta qual aplicativo está em primeiro plano para aplicar o bloqueio imediatamente.",
            icon = Icons.Filled.Accessibility,
            action = { context.startActivity(PermissionHelper.getAccessibilitySettingsIntent()) },
            isGranted = { PermissionHelper.isAccessibilityServiceEnabled(context) }
        ),
        PermissionStep(
            title = "Acesso ao uso",
            description = "Registra o tempo no aplicativo de progresso selecionado e reforça o bloqueio como alternativa.",
            icon = Icons.Filled.BarChart,
            action = { context.startActivity(PermissionHelper.getUsageAccessSettingsIntent()) },
            isGranted = { PermissionHelper.isUsageAccessGranted(context) }
        ),
        PermissionStep(
            title = "Exibir sobre outros aplicativos",
            description = "Exibe uma tela de bloqueio quando você tenta abrir um aplicativo restrito.",
            icon = Icons.Filled.Layers,
            action = { context.startActivity(PermissionHelper.getOverlaySettingsIntent(context)) },
            isGranted = { PermissionHelper.isOverlayPermissionGranted(context) }
        ),
        PermissionStep(
            title = "Notificações",
            description = "Exibe notificações persistentes sobre o status do serviço e avisos de limite.",
            icon = Icons.Filled.Notifications,
            action = { context.startActivity(PermissionHelper.getNotificationSettingsIntent(context)) },
            isGranted = { PermissionHelper.isNotificationPermissionGranted(context) }
        ),
        PermissionStep(
            title = "Alarmes exatos",
            description = "Agenda horários exatos de bloqueio e desbloqueio para suas regras.",
            icon = Icons.Filled.Security,
            action = { context.startActivity(PermissionHelper.getExactAlarmSettingsIntent()) },
            isGranted = { PermissionHelper.isExactAlarmAllowed(context) }
        ),
        PermissionStep(
            title = "Otimização da bateria",
            description = "Desativar a otimização da bateria permite que o feLock funcione continuamente em segundo plano.",
            icon = Icons.Filled.BatteryAlert,
            action = { context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context)) },
            isGranted = { PermissionHelper.isBatteryOptimizationDisabled(context) }
        )
    )

    val step = steps[currentStep]
    val progress = (currentStep + 1).toFloat() / steps.size
    val isAccountStep = currentStep == 1
    val isPasswordStep = currentStep == 2

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isAccountStep) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (recoveryAccount != null) {
                            Text(
                                text = "Selecionada: $recoveryAccount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Nenhuma conta selecionada",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isPasswordStep) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (passwordSet) {
                            Text(
                                text = "A senha mestra está ativada. Você poderá alterá-la depois em Configurações.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            PasswordEntryFields(
                                onSet = { password ->
                                    viewModel.setMasterPassword(password) {}
                                },
                                isSaving = isSavingPassword
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isAccountStep && recoveryAccount != null) {
                OutlinedButton(
                    onClick = { viewModel.onAccountPicked(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Limpar seleção")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val isGranted = step.isGranted()

            if (!isGranted) {
                if (!isPasswordStep) {
                    Button(
                        onClick = { step.action() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAccountStep) "Escolher conta" else "Conceder permissão")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (currentStep < steps.lastIndex) {
                    TextButton(onClick = { currentStep++ }) {
                        Text("Pular")
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (currentStep < steps.lastIndex) {
                            currentStep++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentStep < steps.lastIndex) "Avançar" else "Começar")
                }
            }
        }
    }
}

@Composable
private fun PasswordEntryFields(
    onSet: (String) -> Unit,
    isSaving: Boolean
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = password,
        onValueChange = { password = it; error = null },
        label = { Text("Senha") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = confirm,
        onValueChange = { confirm = it; error = null },
        label = { Text("Confirmar senha") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = {
            when {
                password.length < 4 -> error = "A senha deve ter pelo menos 4 caracteres"
                password != confirm -> error = "As senhas não coincidem"
                else -> onSet(password)
            }
        },
        enabled = !isSaving && password.isNotEmpty() && confirm.isNotEmpty(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Definir senha")
    }
}
