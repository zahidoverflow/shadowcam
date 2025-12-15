package com.shadowcam.features.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.shadowcam.LocalAppDependencies
import com.shadowcam.core.model.AntiDetectLevel
import com.shadowcam.ui.theme.SurfaceElevated

@Composable
fun ExpertScreen() {
    val deps = LocalAppDependencies.current
    val status by deps.antiDetectMonitor.status.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Expert Mode", style = MaterialTheme.typography.headlineSmall)
        status?.let { s ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Environment", style = MaterialTheme.typography.titleMedium)
                    Text(if (s.isEmulator) "Emulator detected" else "Physical device")
                    Text(if (s.isRootAvailable) "Root available" else "Non-root path")
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Anti-detection Level: ${s.level}", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = levelToFloat(s.level),
                        onValueChange = {
                            val level = floatToLevel(it)
                            deps.antiDetectMonitor.setLevel(level)
                        }
                    )
                    RowWithLabel("Timing jitter", s.jitterEnabled) {
                        deps.antiDetectMonitor.toggleJitter(it)
                    }
                }
            }
        }
    }
}

private fun levelToFloat(level: AntiDetectLevel): Float = when (level) {
    AntiDetectLevel.SAFE -> 0f
    AntiDetectLevel.BALANCED -> 0.5f
    AntiDetectLevel.AGGRESSIVE -> 1f
}

private fun floatToLevel(value: Float): AntiDetectLevel = when {
    value < 0.33f -> AntiDetectLevel.SAFE
    value < 0.66f -> AntiDetectLevel.BALANCED
    else -> AntiDetectLevel.AGGRESSIVE
}

@Composable
private fun RowWithLabel(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
