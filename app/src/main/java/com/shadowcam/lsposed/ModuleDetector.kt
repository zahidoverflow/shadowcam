package com.shadowcam.lsposed

import android.content.Context
import android.content.pm.PackageManager

data class DetectedModule(
    val packageName: String,
    val label: String
)

object ModuleDetector {
    // Keep this list short and explicit. Users can still proceed if their module isn't listed.
    private val knownModulePackages: List<String> = listOf(
        // From the user's LSPosed logs: "Loading legacy module com.wangyiheng.vcamsx"
        "com.wangyiheng.vcamsx"
    )

    fun detectKnownModules(context: Context): List<DetectedModule> {
        val pm = context.packageManager
        return knownModulePackages.mapNotNull { pkg ->
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                return@mapNotNull null
            }
            val label = pm.getApplicationLabel(appInfo).toString().ifBlank { pkg }
            DetectedModule(pkg, label)
        }
    }
}
