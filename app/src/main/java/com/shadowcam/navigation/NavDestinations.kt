package com.shadowcam.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home("home", "Home", Icons.Default.Home),
    Apps("apps", "Apps", Icons.Default.List),
    Sources("sources", "Sources", Icons.Default.VideoLibrary),
    Expert("expert", "Expert", Icons.Default.Bolt),
    Logs("logs", "Logs", Icons.Default.BugReport)
}
