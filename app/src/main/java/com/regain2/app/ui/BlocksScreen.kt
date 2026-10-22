package com.regain2.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.regain2.app.MainViewModel
import com.regain2.app.data.AppRule

private fun minText(m: Int): String =
    if (m >= 60 && m % 60 == 0) "${m / 60}h" else if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"

private fun labelOf(pm: PackageManager, pkg: String): String = try {
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
} catch (e: Exception) { pkg }

@Composable
fun BlocksScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val rules by vm.rules.collectAsState()
    val enabled by vm.guardEnabled.collectAsState()
    val shown by vm.pauseShown.collectAsState()
    val back by vm.pauseBack.collectAsState()
    val installed by vm.installedApps.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AppRule?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Blocks", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            GuardCard(enabled, shown, back) {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("App rules", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.loadInstalledApps(); picking = true }) { Text("+ Add App", color = Green) }
            }
            if (rules.isEmpty()) {
                Text("Abhi koi rule nahi hai. Add App dabao aur YouTube / Instagram par limit, block ya pause screen lagao.", color = Grey)
            }
        }
        items(rules, key = { it.packageName }) { r ->
            RuleRow(pm, r) { editing = r }
        }
    }

    if (picking) {
        PickAppDialog(
            apps = installed,
            onPick = { pkg ->
                picking = false
                editing = rules.firstOrNull { it.packageName == pkg } ?: AppRule(pkg)
            },
            onDismiss = { picking = false }
        )
    }

    editing?.let { rule ->
        RuleDialog(
            label = labelOf(pm, rule.packageName),
            rule = rule,
            isNew = rules.none { it.packageName == rule.packageName },
            onSave = { vm.saveRule(it); editing = null },
            onDelete = { vm.deleteRule(rule.packageName); editing = null },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun GuardCard(enabled: Boolean, shown: Int, back: Int, onEnable: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1).padding(16.dp)
    ) {
        if (enabled) {
            Text("\u2713 Blocking active hai", color = Green, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Aaj pause screen $shown baar aayi, aur $back baar tum khud wapas chale gaye.", color = Grey)
        } else {
            Text("Blocking abhi band hai", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Limit, block aur pause screen ke liye Accessibility permission chahiye. " +
                    "Settings mein Installed/Downloaded apps ke andar \"Regain 2.0\" dhundo aur on karo.",
                color = Grey
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onEnable) { Text("Accessibility settings kholo") }
            Spacer(Modifier.height(8.dp))
            Text(
                "Agar toggle grey ho: phone Settings \u2192 Apps \u2192 Regain 2.0 \u2192 upar-right 3 dots \u2192 " +
                    "\"Allow restricted settings\", phir dobara try karo.",
                color = Grey, style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RuleRow(pm: PackageManager, r: AppRule, onClick: () -> Unit) {
    val icon = remember(r.packageName) {
        try { pm.getApplicationIcon(r.packageName).toBitmap(96, 96).asImageBitmap() } catch (e: Exception) { null }
    }
    val parts = buildList {
        if (r.blocked) add("Blocked")
        if (r.limitMinutes > 0) add("${minText(r.limitMinutes)} limit")
        if (r.pause) add("Pause screen")
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
            Text(labelOf(pm, r.packageName), style = MaterialTheme.typography.titleMedium)
            Text(parts.joinToString(" \u2022 "), color = Orange, style = MaterialTheme.typography.bodySmall)
        }
        Text("Edit", color = Grey)
    }
}

@Composable
private fun PickAppDialog(apps: List<Pair<String, String>>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val filtered = remember(apps, q) { apps.filter { it.second.contains(q, ignoreCase = true) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("App chuno") },
        text = {
            Column {
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    placeholder = { Text("Search") }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(320.dp)) {
                    items(filtered, key = { it.first }) { app ->
                        Text(
                            app.second,
                            Modifier.fillMaxWidth().clickable { onPick(app.first) }.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun RuleDialog(
    label: String, rule: AppRule, isNew: Boolean,
    onSave: (AppRule) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit
) {
    var blocked by remember { mutableStateOf(rule.blocked) }
    var limit by remember { mutableStateOf(rule.limitMinutes) }
    var pause by remember { mutableStateOf(rule.pause) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Full block", Modifier.weight(1f))
                    Switch(checked = blocked, onCheckedChange = { blocked = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pause screen (10 sec)", Modifier.weight(1f))
                    Switch(checked = pause, onCheckedChange = { pause = it })
                }
                Spacer(Modifier.height(8.dp))
                Text("Daily limit", color = Grey)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(0, 15, 30, 60, 90, 120, 180).forEach { m ->
                        FilterChip(
                            selected = limit == m,
                            onClick = { limit = m },
                            label = { Text(if (m == 0) "None" else minText(m)) }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(AppRule(rule.packageName, blocked, limit, pause)) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text("Remove", color = Orange) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
