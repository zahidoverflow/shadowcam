package com.shadowcam.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.shadowcam.core.model.LogLevel
import com.shadowcam.logging.LogSink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val icon: android.graphics.drawable.Drawable?
)

class InstalledAppsRepository(
    private val context: Context,
    private val logSink: LogSink
) {
    suspend fun loadLaunchableApps(includeSystemApps: Boolean): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return@withContext try {
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            resolveInfos
                .mapNotNull { info ->
                    val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!includeSystemApps && isSystem) return@mapNotNull null
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    InstalledApp(
                        packageName = appInfo.packageName,
                        label = if (label.isBlank()) appInfo.packageName else label,
                        isSystem = isSystem,
                        icon = info.loadIcon(pm)
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            logSink.log(
                LogLevel.ERROR,
                "Apps",
                "Failed to load installed apps",
                mapOf("error" to (e.message ?: "unknown"))
            )
            emptyList()
        }
    }
}
