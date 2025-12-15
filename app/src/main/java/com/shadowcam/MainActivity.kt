package com.shadowcam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.shadowcam.ui.theme.ShadowCamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val deps = AppDependenciesProvider.dependencies
            ShadowCamTheme(darkTheme = isSystemInDarkTheme()) {
                CompositionLocalProvider(LocalAppDependencies provides deps) {
                    ShadowCamAppRoot()
                }
            }
        }
    }
}
