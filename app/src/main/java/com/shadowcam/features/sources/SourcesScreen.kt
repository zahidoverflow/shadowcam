package com.shadowcam.features.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.shadowcam.core.model.SourceDescriptor
import com.shadowcam.core.model.SourceId
import com.shadowcam.ui.theme.SurfaceElevated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SourcesScreen() {
    val deps = LocalAppDependencies.current
    val sources by deps.sourceRepository.sources.collectAsState(initial = emptyList())
    val scope = rememberIoScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sources", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = {
            scope.launch {
                deps.sourceRepository.add(
                    SourceDescriptor.Image(
                        id = UUID.randomUUID().toString(),
                        name = "Sample Image",
                        path = "/sdcard/Pictures/sample.jpg"
                    )
                )
            }
        }) { Text("Add Sample Image") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sources) { src ->
                SourceCard(src, onSetDefault = { id ->
                    scope.launch { deps.sourceRepository.setDefault(id) }
                })
            }
        }
    }
}

@Composable
private fun SourceCard(source: SourceDescriptor, onSetDefault: (SourceId) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(source.name, style = MaterialTheme.typography.titleMedium)
            Text(
                when (source) {
                    is SourceDescriptor.Image -> "Image · ${source.path}"
                    is SourceDescriptor.Video -> "Video · ${source.path}"
                    is SourceDescriptor.Live -> "Live · ${source.description}"
                    is SourceDescriptor.Rtsp -> "RTSP · ${source.url}"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { onSetDefault(source.id) }) { Text("Set Default") }
        }
    }
}

@Composable
private fun rememberIoScope(): CoroutineScope = androidx.compose.runtime.remember {
    CoroutineScope(Dispatchers.IO)
}
