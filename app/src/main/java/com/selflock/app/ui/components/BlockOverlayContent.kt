package com.selflock.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selflock.app.ui.util.Formatters
import kotlinx.coroutines.delay

@Composable
fun BlockOverlayContent(
    appName: String,
    remainingMinutes: Long,
    packageName: String? = null,
    domain: String? = null
) {
    var currentMinutes by remember { mutableLongStateOf(remainingMinutes) }

    LaunchedEffect(remainingMinutes) {
        currentMinutes = remainingMinutes
        while (currentMinutes > 0) {
            delay(60000)
            currentMinutes--
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!packageName.isNullOrEmpty()) {
                AppIcon(
                    packageName = packageName,
                    modifier = Modifier.size(64.dp),
                    contentDescription = appName
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (!domain.isNullOrEmpty()) {
                FaviconImage(
                    domain = domain,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "$appName is blocked",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Schedule Block Active",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = Formatters.formatCountdown(currentMinutes),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Stay focused! You've got this.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}
