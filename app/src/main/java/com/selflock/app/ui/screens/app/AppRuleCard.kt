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
import com.selflock.app.domain.model.BlockType
import com.selflock.app.ui.components.AppIcon
import com.selflock.app.ui.util.Formatters

@Composable
fun AppRuleCard(
    uiState: AppRuleUiState,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rule = uiState.rule
    val status = uiState.status
    val isLocked = status.isLocked

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        packageName = rule.packageName,
                        modifier = Modifier.size(40.dp),
                        contentDescription = rule.appName
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = rule.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (rule.isPasswordProtected) {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = "Password protected",
                            modifier = Modifier.padding(start = 8.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (isLocked) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.padding(start = 8.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isLocked) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { onToggle() },
                        enabled = !isLocked
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (rule.blockType) {
                BlockType.SCHEDULE -> {
                    val startTime = rule.getStartTime()
                    val endTime = rule.getEndTime()
                    if (startTime != null && endTime != null) {
                        Text(
                            text = "Blocked: ${Formatters.formatTime(startTime.hour, startTime.minute)} - ${Formatters.formatTime(endTime.hour, endTime.minute)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = rule.getDaysList().joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BlockType.DAILY_LIMIT -> {
                    val limitMinutes = rule.dailyLimitMinutes ?: 0
                    val usedMinutes = (status.usagePercent ?: 0f) * limitMinutes
                    Text(
                        text = "${usedMinutes.toInt()}m / ${limitMinutes}m daily budget",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (status.usagePercent ?: 0f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            (status.usagePercent ?: 0f) > 0.95f -> MaterialTheme.colorScheme.error
                            (status.usagePercent ?: 0f) > 0.8f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            if (status.isActive && status.remainingTimeMinutes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.formatCountdown(status.remainingTimeMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
