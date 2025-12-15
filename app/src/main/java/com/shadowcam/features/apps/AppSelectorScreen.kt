package com.shadowcam.features.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcam.core.model.CameraProfile
import com.shadowcam.ui.theme.CardSurface
import com.shadowcam.ui.theme.ColorTokens

@Composable
fun AppSelectorScreen(
    profiles: List<CameraProfile>,
    onSelect: (CameraProfile) -> Unit,
    onToggleFavorite: (CameraProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(profiles) { profile ->
            ProfileCard(
                profile = profile,
                onClick = { onSelect(profile) },
                onToggleFavorite = { onToggleFavorite(profile) }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: CameraProfile,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .background(CardSurface, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.label, color = ColorTokens.textPrimary, style = MaterialTheme.typography.titleLarge)
                Text("${profile.resolution} @ ${profile.fps}fps", color = ColorTokens.textSecondary)
                if (profile.metadataOverrides.isNotEmpty()) {
                    Text(
                        "Metadata: ${profile.metadataOverrides.keys.joinToString()}",
                        color = ColorTokens.textSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                val icon = if (profile.favorite) Icons.Filled.Star else Icons.Outlined.Star
                val tint = if (profile.favorite) MaterialTheme.colorScheme.primary else ColorTokens.textSecondary
                Icon(icon, contentDescription = "favorite", tint = tint)
            }
        }
        Spacer(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "Bound Source: ${profile.sourceId ?: "None"} · Anti-detect: ${profile.antiDetect.level.name}",
            color = ColorTokens.textSecondary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
