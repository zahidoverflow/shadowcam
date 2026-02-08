package com.shadowcam.features.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shadowcam.LocalAppDependencies
import com.shadowcam.core.model.LogEntry
import com.shadowcam.core.model.LogLevel
import com.shadowcam.navigation.LocalNavController
import com.shadowcam.ui.theme.SurfaceElevated
import kotlinx.coroutines.launch

@Composable
fun LogsScreen() {
    val deps = LocalAppDependencies.current
    val navController = LocalNavController.current
    val logs by deps.logSink.logs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Logs & Console", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { navController.navigate("help") }) { Text("Help") }
            OutlinedButton(onClick = {
                scope.launch {
                    deps.logSink.log(LogLevel.DEBUG, "Logs", "Exporting logs...")
                    deps.rootCamera1Manager.exportLogs()
                }
            }) { Text("Export to Downloads") }
            Button(onClick = { deps.logSink.clear() }) { Text("Clear") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs.reversed()) { log ->
                LogRow(log)
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val metadataLine = buildString {
        entry.thread?.let { append("thread=").append(it) }
        if (entry.metadata.isNotEmpty()) {
            if (isNotEmpty()) append(" ")
            append(entry.metadata.entries.joinToString(" ") { "${it.key}=${it.value}" })
        }
    }.let { if (it.length > 160) it.take(160) + "..." else it }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("[${entry.level}] ${entry.tag}", style = MaterialTheme.typography.labelMedium)
            Text(entry.message, style = MaterialTheme.typography.bodyMedium)
            if (metadataLine.isNotBlank()) {
                Text(metadataLine, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
