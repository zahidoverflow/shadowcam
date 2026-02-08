package com.shadowcam.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavDestination(val route: String, val label: String, val icon: ImageVector) {
    object Home : NavDestination("home", "Home", Icons.Filled.Dashboard)
    object Apps : NavDestination("apps", "Apps", Icons.Filled.List)
    object Sources : NavDestination("sources", "Sources", Icons.Filled.Source)
    object Expert : NavDestination("expert", "Expert", Icons.Filled.Build)
    object Logs : NavDestination("logs", "Logs", Icons.Filled.Settings)
    object Help : NavDestination("help", "Help", Icons.Filled.Help)
}

val bottomDestinations = listOf(
    NavDestination.Home,
    NavDestination.Apps,
    NavDestination.Sources,
    NavDestination.Expert,
    NavDestination.Logs
)
