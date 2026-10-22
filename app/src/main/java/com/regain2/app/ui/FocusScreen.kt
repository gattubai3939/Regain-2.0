package com.regain2.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.regain2.app.MainViewModel
import com.regain2.app.data.AppCategory
import com.regain2.app.data.AppUsage

fun fmt(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

fun AppCategory.color(): Color = when (this) {
    AppCategory.DISTRACTING -> Orange
    AppCategory.PRODUCTIVE -> Green
    AppCategory.MIXED -> Color(0xFFE5C100)
    AppCategory.OTHERS -> Grey
}

@Composable
fun FocusScreen(vm: MainViewModel) {
    val granted by vm.hasPermission.collectAsState()
    val summary by vm.summary.collectAsState()
    val unlocks by vm.unlocks.collectAsState()
    val pickups by vm.pickups.collectAsState()
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf<AppUsage?>(null) }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Usage Access chahiye", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Screen time dikhane ke liye ye permission zaroori hai. Data sirf phone par rehta hai.", color = Grey)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                Text("Permission do")
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Screen Time", color = Grey)
            Text(fmt(summary.totalMillis), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            StackedBar(summary.distractingMillis, summary.productiveMillis, summary.othersMillis)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Legend("Distracting", fmt(summary.distractingMillis), Orange)
                Legend("Productive", fmt(summary.productiveMillis), Green)
                Legend("Others", fmt(summary.othersMillis), Grey)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Unlocks", unlocks.toString(), Modifier.weight(1f))
                StatCard("Pickups", pickups.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Text("Apps (tap to change category)", color = Grey)
            Spacer(Modifier.height(8.dp))
        }
        items(summary.apps, key = { it.packageName }) { app ->
            AppRow(app) { selected = app }
        }
    }

    selected?.let { app ->
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = {},
            title = { Text(app.label) },
            text = {
                Column {
                    AppCategory.values().forEach { c ->
                        TextButton(onClick = { vm.setCategory(app.packageName, c); selected = null }) {
                            Text(c.label, color = c.color())
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun Legend(name: String, value: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(name, color = Grey, style = MaterialTheme.typography.bodySmall)
        }
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StackedBar(d: Long, p: Long, o: Long) {
    val total = (d + p + o).coerceAtLeast(1).toFloat()
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Surface1)) {
        if (d > 0) Box(Modifier.weight(d / total).fillMaxHeight().background(Orange))
        if (p > 0) Box(Modifier.weight(p / total).fillMaxHeight().background(Green))
        if (o > 0) Box(Modifier.weight(o / total).fillMaxHeight().background(Grey))
    }
}

@Composable
private fun AppRow(app: AppUsage, onClick: () -> Unit) {
    val pm = LocalContext.current.packageManager
    val icon = remember(app.packageName) {
        try { pm.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() } catch (e: Exception) { null }
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(Surface1)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Image(icon, null, Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium)
            Text(app.category.label, color = app.category.color(), style = MaterialTheme.typography.bodySmall)
        }
        Text(fmt(app.millis), color = Grey)
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp)
    ) {
        Text(title, color = Grey, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}
