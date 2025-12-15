package com.shadowcam

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shadowcam.features.apps.AppsScreen
import com.shadowcam.features.expert.ExpertScreen
import com.shadowcam.features.home.HomeScreen
import com.shadowcam.features.logs.LogsScreen
import com.shadowcam.features.sources.SourcesScreen
import com.shadowcam.navigation.NavDestination
import com.shadowcam.navigation.bottomDestinations
import com.shadowcam.ui.theme.AccentCyan
import com.shadowcam.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowCamAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = SurfaceElevated,
        bottomBar = {
            NavigationBar(containerColor = SurfaceElevated) {
                bottomDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(destination.icon, destination.label) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCyan,
                            selectedTextColor = AccentCyan
                        )
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.Home.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(NavDestination.Home.route) { HomeScreen() }
            composable(NavDestination.Apps.route) { AppsScreen() }
            composable(NavDestination.Sources.route) { SourcesScreen() }
            composable(NavDestination.Expert.route) { ExpertScreen() }
            composable(NavDestination.Logs.route) { LogsScreen() }
        }
    }
}
