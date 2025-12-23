package com.shadowcam.features.sources

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shadowcam.LocalAppDependencies
import com.shadowcam.root.MarkerFile
import com.shadowcam.root.PickedMedia
import com.shadowcam.root.RootCamera1State
import com.shadowcam.ui.theme.AccentLime
import com.shadowcam.ui.theme.SurfaceElevated
import com.shadowcam.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun SourcesScreen() {
    val deps = LocalAppDependencies.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by deps.rootCamera1Manager.state.collectAsState()
    val scrollState = rememberScrollState()

    val pickVideoLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            val name = queryDisplayName(context, uri)
            scope.launch { deps.rootCamera1Manager.selectVideo(uri, name) }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            val name = queryDisplayName(context, uri)
            scope.launch { deps.rootCamera1Manager.selectImage(uri, name) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sources", style = MaterialTheme.typography.headlineSmall)
        RootStatusCard(state, onRefresh = {
            scope.launch { deps.rootCamera1Manager.refreshRootStatus() }
        })

        MediaCard(
            title = "Video source (virtual.mp4)",
            selected = state.video,
            pickLabel = "Pick Video",
            onPick = { pickVideoLauncher.launch(arrayOf("video/*")) },
            onSync = { scope.launch { deps.rootCamera1Manager.syncVideo() } },
            enabled = state.rootAvailable && !state.busy
        )

        MediaCard(
            title = "Image source (1000.bmp)",
            selected = state.image,
            pickLabel = "Pick Image",
            onPick = { pickImageLauncher.launch(arrayOf("image/*")) },
            onSync = { scope.launch { deps.rootCamera1Manager.syncImage() } },
            enabled = state.rootAvailable && !state.busy
        )

        MarkerCard(
            state = state,
            onToggle = { marker, enabled ->
                scope.launch { deps.rootCamera1Manager.setMarker(marker, enabled) }
            },
            onRefresh = { scope.launch { deps.rootCamera1Manager.refreshMarkers() } }
        )

        Text(
            "Marker files are used by compatible Xposed or LSPosed camera modules.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RootStatusCard(state: RootCamera1State, onRefresh: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Root Camera1", style = MaterialTheme.typography.titleMedium)
            Text(
                if (state.rootAvailable) "Root available" else "Root not available",
                color = if (state.rootAvailable) AccentLime else WarningAmber
            )
            Text("Path: ${state.camera1Dir}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            }
            state.message?.let { message ->
                Text(
                    message.text,
                    color = if (message.isError) WarningAmber else AccentLime,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MediaCard(
    title: String,
    selected: PickedMedia?,
    pickLabel: String,
    onPick: () -> Unit,
    onSync: () -> Unit,
    enabled: Boolean
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "Selected: ${selected?.displayName ?: selected?.uri ?: "None"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPick) { Text(pickLabel) }
                Button(onClick = onSync, enabled = enabled && selected != null) { Text("Sync") }
            }
        }
    }
}

@Composable
private fun MarkerCard(
    state: RootCamera1State,
    onToggle: (MarkerFile, Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Marker Files", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            }
            MarkerFile.entries.forEach { marker ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(marker.label, style = MaterialTheme.typography.bodyMedium)
                        Text(marker.description, style = MaterialTheme.typography.labelMedium)
                    }
                    Switch(
                        checked = state.markers[marker] == true,
                        onCheckedChange = { onToggle(marker, it) },
                        enabled = state.rootAvailable && !state.busy
                    )
                }
            }
        }
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Best-effort only.
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            return it.getString(0)
        }
    }
    return null
}
