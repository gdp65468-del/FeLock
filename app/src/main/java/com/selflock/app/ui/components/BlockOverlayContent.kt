package com.selflock.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selflock.app.ui.util.Formatters
import kotlinx.coroutines.delay

data class TaskAppOption(val packageName: String, val appName: String)

@Composable
fun BlockOverlayContent(
    appName: String,
    packageName: String,
    ruleName: String,
    remainingMinutes: Long,
    progressAppName: String,
    taskApps: List<TaskAppOption>,
    progressSeconds: Long,
    goalMinutes: Int,
    rewardsUsed: Int,
    maxRewards: Int,
    contingencyUsed: Boolean,
    contingencyAvailableAt: Long,
    contingencyMinutes: Int,
    onOpenProgressApp: (String) -> Unit,
    onRelease: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val goalSeconds = goalMinutes * 60L
    val remainingGoalMinutes = ((goalSeconds - progressSeconds).coerceAtLeast(0) + 59) / 60
    val contingencyAvailable = now >= contingencyAvailableAt
    val contingencyWaitMinutes = ((contingencyAvailableAt - now).coerceAtLeast(0) + 59_999) / 60_000

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon(packageName = packageName, modifier = Modifier.size(64.dp), contentDescription = appName)
            Spacer(modifier = Modifier.height(16.dp))
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(24.dp))
            Text("$appName está bloqueado", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(ruleName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Formatters.formatCountdown(remainingMinutes), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            if (rewardsUsed < maxRewards) Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Ganhe tempo livre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Use ${if (taskApps.size > 1) "um dos aplicativos de tarefa" else progressAppName} por mais $remainingGoalMinutes minutos.")
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (goalSeconds > 0) (progressSeconds.toFloat() / goalSeconds).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    taskApps.forEach { taskApp ->
                        Button(onClick = { onOpenProgressApp(taskApp.packageName) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Abrir ${taskApp.appName}")
                        }
                    }
                }
            } else {
                Text("Todas as $maxRewards recompensas deste bloqueio foram usadas.", textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (contingencyUsed) {
                Text("A liberação alternativa deste bloqueio já foi usada.", textAlign = TextAlign.Center)
            } else if (contingencyAvailable) {
                OutlinedButton(onClick = onRelease, modifier = Modifier.fillMaxWidth()) {
                    Text("Liberar por $contingencyMinutes minutos")
                }
            } else {
                Text(
                    "Liberação alternativa disponível em ${Formatters.formatCountdown(contingencyWaitMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
