package com.shadowcam.features.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.shadowcam.core.model.MediaSource
import com.shadowcam.ui.theme.CardSurface
import com.shadowcam.ui.theme.ColorTokens

@Composable
fun SourceManagerScreen(
    sources: List<MediaSource>,
    defaultSourceId: String?,
    onSetDefault: (MediaSource) -> Unit,
    onAssignToApp: (MediaSource) -> Unit,
    onTest: (MediaSource) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sources) { source ->
            SourceCard(
                source = source,
                isDefault = source.id == defaultSourceId,
                onSetDefault = { onSetDefault(source) },
                onAssignToApp = { onAssignToApp(source) },
                onTest = { onTest(source) }
            )
        }
    }
}

@Composable
private fun SourceCard(
    source: MediaSource,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onAssignToApp: () -> Unit,
    onTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .background(CardSurface, RoundedCornerShape(14.dp))
            .clickable { onAssignToApp() }
            .padding(14.dp)
    ) {
        Text(source.name, color = ColorTokens.textPrimary, style = MaterialTheme.typography.titleLarge)
        Text("${source.type} · ${source.details}", color = ColorTokens.textSecondary, style = MaterialTheme.typography.bodyMedium)
        if (source.metadata.isNotEmpty()) {
            Text(
                "Metadata: ${source.metadata.entries.joinToString { "${it.key}=${it.value}" }}",
                color = ColorTokens.textSecondary,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onTest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) { Text("Test", color = MaterialTheme.colorScheme.onTertiary) }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onAssignToApp,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Assign", color = MaterialTheme.colorScheme.onPrimary) }
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            onClick = onSetDefault,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDefault) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(
                    alpha = 0.5f
                )
            )
        ) {
            Text(if (isDefault) "Default Source" else "Set Default", color = MaterialTheme.colorScheme.onSecondary)
        }
    }
}
