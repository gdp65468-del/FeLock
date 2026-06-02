package com.selflock.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun FaviconImage(
    domain: String,
    size: Dp = 32.dp
) {
    var loadFailed by remember(domain) { mutableStateOf(false) }

    if (loadFailed) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(size)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = domain,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    } else {
        AsyncImage(
            model = "https://www.google.com/s2/favicons?domain=$domain&sz=64",
            contentDescription = "$domain favicon",
            modifier = Modifier.size(size).clip(CircleShape),
            onError = { loadFailed = true }
        )
    }
}
