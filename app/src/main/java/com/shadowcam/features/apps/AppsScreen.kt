@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shadowcam.features.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.shadowcam.LocalAppDependencies
import com.shadowcam.apps.InstalledApp
import com.shadowcam.apps.TargetApp
import com.shadowcam.core.model.AntiDetectLevel
import com.shadowcam.core.model.AppProfile
import com.shadowcam.core.model.Resolution
import com.shadowcam.ui.theme.AccentCyan
import com.shadowcam.ui.theme.SurfaceElevated
import kotlinx.coroutines.launch

@Composable
fun AppsScreen() {
    val deps = LocalAppDependencies.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val targetApp by deps.targetAppStore.selected.collectAsState(initial = null)
    val profiles by deps.profileStore.profiles.collectAsState(initial = emptyList())
    val profilesByPackage = remember(profiles) { profiles.associateBy { it.packageName } }
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var includeSystemApps by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var editingApp by remember { mutableStateOf<InstalledApp?>(null) }

    LaunchedEffect(includeSystemApps) {
        isLoading = true
        apps = deps.installedAppsRepository.loadLaunchableApps(includeSystemApps)
        isLoading = false
    }

    val filteredApps = remember(apps, query) {
        if (query.isBlank()) {
            apps
        } else {
            val needle = query.trim()
            apps.filter {
                it.label.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("App Targets", style = MaterialTheme.typography.headlineSmall)
        TargetCard(
            targetApp = targetApp,
            onClear = { scope.launch { deps.targetAppStore.setTarget(null) } },
            onOpen = {
                targetApp?.let { app ->
                    launchTargetApp(context, app.packageName, deps.logSink)
                }
            }
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("System apps", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = includeSystemApps, onCheckedChange = { includeSystemApps = it })
            }
            OutlinedButton(onClick = {
                scope.launch {
                    isLoading = true
                    apps = deps.installedAppsRepository.loadLaunchableApps(includeSystemApps)
                    isLoading = false
                }
            }) { Text("Refresh") }
        }
        if (isLoading) {
            Text("Loading apps...", style = MaterialTheme.typography.bodyMedium)
        } else if (filteredApps.isEmpty()) {
            Text("No apps found", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredApps) { app ->
                    val isTarget = targetApp?.packageName == app.packageName
                    val profile = profilesByPackage[app.packageName]
                    AppRow(
                        app = app,
                        isTarget = isTarget,
                        profile = profile,
                        onSetTarget = {
                            scope.launch {
                                deps.targetAppStore.setTarget(TargetApp(app.packageName, app.label))
                                profile?.let { deps.virtualCameraEngine.applyProfile(it) }
                            }
                        },
                        onEditProfile = { editingApp = app }
                    )
                }
            }
        }
    }

    editingApp?.let { app ->
        ProfileEditorDialog(
            app = app,
            existing = profilesByPackage[app.packageName],
            onSave = { profile ->
                scope.launch {
                    deps.profileStore.upsert(profile)
                    deps.virtualCameraEngine.applyProfile(profile)
                    editingApp = null
                }
            },
            onDelete = {
                scope.launch {
                    deps.profileStore.remove(app.packageName)
                    editingApp = null
                }
            },
            onDismiss = { editingApp = null }
        )
    }
}

@Composable
private fun TargetCard(targetApp: TargetApp?, onClear: () -> Unit, onOpen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Target App", style = MaterialTheme.typography.titleMedium)
            if (targetApp == null) {
                Text("None selected", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(targetApp.label, style = MaterialTheme.typography.bodyMedium, color = AccentCyan)
                Text(targetApp.packageName, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClear) { Text("Clear") }
                    Button(onClick = onOpen) { Text("Open") }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    isTarget: Boolean,
    profile: AppProfile?,
    onSetTarget: () -> Unit,
    onEditProfile: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium)
                Text(app.packageName, style = MaterialTheme.typography.labelMedium)
                if (profile != null) {
                    Text(
                        "${profile.resolution.label} @ ${profile.fps.toInt()}fps",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isTarget) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Target") }, enabled = false)
                } else {
                    OutlinedButton(onClick = onSetTarget) { Text("Target") }
                }
                OutlinedButton(onClick = onEditProfile) { Text("Profile") }
            }
        }
    }
}

@Composable
private fun AppIcon(app: InstalledApp) {
    val iconBitmap = remember(app.packageName, app.icon) {
        app.icon?.toBitmap()?.asImageBitmap()
    }
    val size = 42.dp
    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun launchTargetApp(
    context: android.content.Context,
    packageName: String,
    logSink: com.shadowcam.logging.LogSink
) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent == null) {
        logSink.log(
            com.shadowcam.core.model.LogLevel.WARN,
            "Target",
            "Launch intent not found",
            mapOf("package" to packageName)
        )
        return
    }
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    logSink.log(
        com.shadowcam.core.model.LogLevel.INFO,
        "Target",
        "Launched app",
        mapOf("package" to packageName)
    )
}

@Composable
private fun ProfileEditorDialog(
    app: InstalledApp,
    existing: AppProfile?,
    onSave: (AppProfile) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val resolutions = remember {
        listOf(
            Resolution(1920, 1080),
            Resolution(1280, 720),
            Resolution(960, 540),
            Resolution(640, 480),
            Resolution(320, 240)
        )
    }
    var selectedResolution by remember { mutableStateOf(existing?.resolution ?: resolutions.first()) }
    var fps by remember { mutableStateOf(existing?.fps ?: 30f) }
    var level by remember { mutableStateOf(existing?.antiDetectLevel ?: AntiDetectLevel.SAFE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val profile = AppProfile(
                    packageName = app.packageName,
                    label = app.label,
                    resolution = selectedResolution,
                    fps = fps,
                    antiDetectLevel = level,
                    defaultSourceId = existing?.defaultSourceId,
                    exifStrategy = existing?.exifStrategy ?: "balanced"
                )
                onSave(profile)
            }) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing != null) {
                    TextButton(onClick = onDelete) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        title = { Text("Profile: ${app.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resolution", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(resolutions) { res ->
                        FilterChip(
                            selected = res == selectedResolution,
                            onClick = { selectedResolution = res },
                            label = { Text(res.label) }
                        )
                    }
                }
                Text("FPS: ${fps.toInt()}", style = MaterialTheme.typography.titleMedium)
                androidx.compose.material3.Slider(
                    value = fps,
                    onValueChange = { fps = it },
                    valueRange = 5f..60f,
                    steps = 10
                )
                Text("Anti-detect", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AntiDetectLevel.entries.forEach { option ->
                        FilterChip(
                            selected = option == level,
                            onClick = { level = option },
                            label = { Text(option.name) }
                        )
                    }
                }
            }
        }
    )
}
