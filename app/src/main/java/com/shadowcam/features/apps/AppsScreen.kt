package com.shadowcam.features.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shadowcam.LocalAppDependencies
import com.shadowcam.core.model.AppProfile
import com.shadowcam.core.model.AntiDetectLevel
import com.shadowcam.core.model.Resolution
import com.shadowcam.ui.theme.AccentCyan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AppsScreen() {
    val deps = LocalAppDependencies.current
    val profiles by deps.profileStore.profiles.collectAsState(initial = emptyList())
    val scope = rememberIoScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("App Profiles", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = {
            scope.launch {
                deps.profileStore.upsert(
                    AppProfile(
                        packageName = "com.example.app",
                        label = "Example App",
                        resolution = Resolution(1920, 1080),
                        fps = 30f,
                        antiDetectLevel = AntiDetectLevel.BALANCED
                    )
                )
            }
        }) { Text("Add Sample Profile") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(profiles) { profile ->
                ProfileCard(profile) {
                    scope.launch {
                        deps.virtualCameraEngine.applyProfile(profile)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: AppProfile, onSelect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = com.shadowcam.ui.theme.SurfaceElevated),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(profile.label, style = MaterialTheme.typography.titleMedium, color = AccentCyan)
            Text(profile.packageName, style = MaterialTheme.typography.bodyMedium)
            Text("Res ${profile.resolution.label} @ ${profile.fps}fps", style = MaterialTheme.typography.bodyMedium)
            Text("Anti-detect: ${profile.antiDetectLevel}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onSelect) { Text("Apply") }
        }
    }
}

@Composable
private fun rememberIoScope(): CoroutineScope = androidx.compose.runtime.remember {
    CoroutineScope(Dispatchers.IO)
}
