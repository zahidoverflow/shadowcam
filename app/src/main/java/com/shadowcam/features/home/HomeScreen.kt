package com.shadowcam.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shadowcam.LocalAppDependencies
import com.shadowcam.core.model.VirtualCameraMode
import com.shadowcam.core.model.VirtualCameraState
import com.shadowcam.navigation.LocalNavController
import com.shadowcam.ui.theme.AccentCyan
import com.shadowcam.ui.theme.AccentLime
import com.shadowcam.ui.theme.AccentPurple
import com.shadowcam.ui.theme.SurfaceElevated
import com.shadowcam.ui.theme.WarningAmber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val deps = LocalAppDependencies.current
    val navController = LocalNavController.current
    val ioScope = rememberIoScope()
    val state by deps.virtualCameraEngine.state.collectAsState()
    val rootState by deps.rootCamera1Manager.state.collectAsState()
    val dismissedGettingStarted by deps.onboardingStore.hasDismissedGettingStarted.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceElevated)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ShadowCam Control", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { navController.navigate("help") }) {
                Icon(Icons.Filled.Help, contentDescription = "Help")
            }
        }

        if (!dismissedGettingStarted) {
            GettingStartedCard(
                onOpenApps = { navController.navigate("apps") },
                onOpenSources = { navController.navigate("sources") },
                onOpenHelp = { navController.navigate("help") },
                onDismiss = {
                    ioScope.launch { deps.onboardingStore.setDismissedGettingStarted(true) }
                }
            )
        }

        ModeRow(state.mode) { deps.virtualCameraEngine.setMode(it) }
        StatusCards(state, rootState)
        ControlButtons(state)
        Text("Quick Stats", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item { StatPill("FPS", "${state.fps}") }
            item { StatPill("Latency", "${state.latencyMs} ms") }
            item { StatPill("Uptime", "${state.uptimeMs / 1000}s") }
        }
    }
}

@Composable
private fun GettingStartedCard(
    onOpenApps: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenHelp: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Getting Started", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onDismiss) { Text("Hide") }
            }
            Text("1. Pick a Target App (Apps tab).", style = MaterialTheme.typography.bodyMedium)
            Text("2. Select a source and Sync (Sources tab).", style = MaterialTheme.typography.bodyMedium)
            Text("3. Arm VCAM (Home tab), then open the target app.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onOpenApps) { Text("Open Apps") }
                OutlinedButton(onClick = onOpenSources) { Text("Open Sources") }
                Button(onClick = onOpenHelp) { Text("Full Guide") }
            }
        }
    }
}

@Composable
private fun ModeRow(mode: VirtualCameraMode, onModeChange: (VirtualCameraMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeChip("Non-Root", mode == VirtualCameraMode.NON_ROOT, AccentCyan) {
            onModeChange(VirtualCameraMode.NON_ROOT)
        }
        ModeChip("Root", mode == VirtualCameraMode.ROOT, AccentPurple) {
            onModeChange(VirtualCameraMode.ROOT)
        }
    }
}

@Composable
private fun StatusCards(state: VirtualCameraState, rootState: com.shadowcam.root.RootCamera1State) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("VCAM Status", style = MaterialTheme.typography.titleMedium)
                Text(if (state.isEnabled) "ON" else "OFF", color = if (state.isEnabled) AccentLime else WarningAmber)
                Text(
                    "Source: ${rootState.lastSynced?.name ?: state.source?.name ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Profile: ${state.profile?.label ?: "None"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Target: ${rootState.targetApp?.label ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Anti-Detection", style = MaterialTheme.typography.titleMedium)
                Text("Mode: ${state.mode.name}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ControlButtons(state: VirtualCameraState) {
    val deps = LocalAppDependencies.current
    val scope = rememberIoScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                if (state.isEnabled) {
                    scope.launch { deps.virtualCameraEngine.stop() }
                } else {
                    scope.launch { deps.virtualCameraEngine.start(state.source, state.profile) }
                }
            },
            modifier = Modifier.weight(1f)
        ) { Text(if (state.isEnabled) "Arm OFF" else "Arm VCAM") }
        Button(onClick = {
            scope.launch {
                deps.logSink.log(com.shadowcam.core.model.LogLevel.INFO, "Home", "Export logs requested")
                deps.rootCamera1Manager.exportLogs()
            }
        }) { Text("Export Logs") }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ModeChip(label: String, selected: Boolean, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val container = if (selected) color.copy(alpha = 0.15f) else SurfaceElevated
    val borderColor = if (selected) color else SurfaceElevated
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = container),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun rememberIoScope(): CoroutineScope = androidx.compose.runtime.remember {
    CoroutineScope(Dispatchers.IO)
}
