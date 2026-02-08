package com.shadowcam.features.setup

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shadowcam.LocalAppDependencies
import com.shadowcam.lsposed.ModuleDetector
import com.shadowcam.navigation.LocalNavController
import com.shadowcam.ui.theme.AccentLime
import com.shadowcam.ui.theme.SurfaceElevated
import com.shadowcam.ui.theme.WarningAmber

@Composable
fun SetupWizardScreen() {
    val deps = LocalAppDependencies.current
    val navController = LocalNavController.current
    val context = LocalContext.current
    val state by deps.virtualCameraEngine.state.collectAsState()
    val rootState by deps.rootCamera1Manager.state.collectAsState()
    val detectedModules = remember { ModuleDetector.detectKnownModules(context) }

    val cameraPermissionGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val sourceSelected = (state.source != null) || (rootState.video != null) || (rootState.image != null)
    val syncedOnce = rootState.lastSynced != null
    val targetSelected = rootState.targetApp != null
    val xposedInjectedInThisProcess = remember { isXposedPresentInProcess() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Setup Wizard", style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            "This checklist helps you set up ShadowCam step-by-step. Fix anything marked NOT OK, then test again.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StepRow("Root available", rootState.rootAvailable)
                StepRow("Camera permission granted", cameraPermissionGranted)
                StepRow("Target app selected", targetSelected)
                StepRow("Source selected", sourceSelected)
                StepRow("Synced at least once", syncedOnce)
                StepRow("Known hook module installed", detectedModules.isNotEmpty())
                StepRow("Xposed present in this process", xposedInjectedInThisProcess)

                Text(
                    "Notes:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "- \"Known hook module installed\" checks for common packages (from your logs: com.wangyiheng.vcamsx).",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "- \"Xposed present in this process\" is a hint only. LSPosed usually injects into target apps, not always into ShadowCam.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Next Actions", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { navController.navigate("apps") }) { Text("Pick Target") }
                    OutlinedButton(onClick = { navController.navigate("sources") }) { Text("Pick Source") }
                    OutlinedButton(onClick = { navController.navigate("help") }) { Text("Help") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { navController.navigate("logs") }) { Text("Open Logs") }
                    OutlinedButton(onClick = { navController.navigate("home") }) { Text("Home") }
                }
            }
        }

        if (detectedModules.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Detected Modules", style = MaterialTheme.typography.titleMedium)
                    detectedModules.forEach { m ->
                        Text("- ${m.label} (${m.packageName})", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun isXposedPresentInProcess(): Boolean =
    try {
        Class.forName("de.robv.android.xposed.XposedBridge")
        true
    } catch (_: Throwable) {
        false
    }

@Composable
private fun StepRow(label: String, ok: Boolean) {
    val color = if (ok) AccentLime else WarningAmber
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(if (ok) "OK" else "NOT OK", style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

