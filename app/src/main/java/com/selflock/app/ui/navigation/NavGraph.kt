package com.selflock.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.selflock.app.ui.screens.app.AppBlockScreen
import com.selflock.app.ui.screens.settings.SettingsSheet
import com.selflock.app.ui.screens.statistics.StatisticsScreen

data class Screen(val title: String, val icon: ImageVector)

val screens = listOf(
    Screen("Apps", Icons.Filled.Apps),
    Screen("Stats", Icons.Filled.BarChart)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedIndex) {
            0 -> AppBlockScreen(
                modifier = Modifier.padding(paddingValues),
                onOpenSettings = { showSettings = true }
            )
            1 -> StatisticsScreen(
                modifier = Modifier.padding(paddingValues),
                onOpenSettings = { showSettings = true }
            )
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            SettingsSheet()
        }
    }
}
