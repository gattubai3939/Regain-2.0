package com.regain2.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.regain2.app.MainViewModel

private enum class Tab(val title: String, val icon: ImageVector) {
    FOCUS("Focus", Icons.Default.Home),
    SCHEDULE("Schedule", Icons.Default.DateRange),
    BLOCKS("Blocks", Icons.Default.Lock)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    var tab by remember { mutableStateOf(Tab.FOCUS) }
    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Bg) {
                Tab.values().forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, it.title) },
                        label = { Text(it.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Green, selectedTextColor = Green,
                            indicatorColor = Surface1
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(Bg)) {
            when (tab) {
                Tab.FOCUS -> FocusScreen(vm)
                Tab.SCHEDULE -> Placeholder("Schedule", "Study schedules & reminders (Phase 4)")
                Tab.BLOCKS -> Placeholder("Blocks", "App limits, pause screen, full block (Phase 2)")
            }
        }
    }
}

@Composable
fun Placeholder(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Grey)
    }
}
