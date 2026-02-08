package com.shadowcam

import com.shadowcam.antidetect.AntiDetectMonitor
import com.shadowcam.antidetect.InMemoryAntiDetectMonitor
import com.shadowcam.core.engine.VirtualCameraEngine
import android.content.Context
import android.os.Build
import android.os.Process
import com.shadowcam.apps.DataStoreTargetAppStore
import com.shadowcam.apps.InstalledAppsRepository
import com.shadowcam.apps.TargetAppStore
import com.shadowcam.engine.AppVirtualCameraEngine
import com.shadowcam.logging.InMemoryLogSink
import com.shadowcam.logging.LogSink
import com.shadowcam.logging.SessionLogSink
import com.shadowcam.onboarding.DataStoreOnboardingStore
import com.shadowcam.onboarding.OnboardingStore
import com.shadowcam.profiles.ProfileStore
import com.shadowcam.profiles.DataStoreProfileStore
import com.shadowcam.root.RootCamera1Manager
import com.shadowcam.sources.InMemorySourceRepository
import com.shadowcam.sources.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

data class AppDependencies(
    val virtualCameraEngine: VirtualCameraEngine,
    val profileStore: ProfileStore,
    val sourceRepository: SourceRepository,
    val logSink: LogSink,
    val antiDetectMonitor: AntiDetectMonitor,
    val rootCamera1Manager: RootCamera1Manager,
    val targetAppStore: TargetAppStore,
    val installedAppsRepository: InstalledAppsRepository,
    val onboardingStore: OnboardingStore
)

val LocalAppDependencies = androidx.compose.runtime.staticCompositionLocalOf<AppDependencies> {
    error("AppDependencies not provided")
}

object AppDependenciesProvider {
    lateinit var dependencies: AppDependencies
        private set
    private var observerScope: CoroutineScope? = null

    fun installDefault(context: Context) {
        val memorySink = InMemoryLogSink()
        val logFile = java.io.File(context.filesDir, "shadowcam_debug.log")
        val fileSink = com.shadowcam.logging.FileLogSink(logFile)
        val composite = com.shadowcam.logging.CompositeLogSink(listOf(memorySink, fileSink))
        val sessionId = UUID.randomUUID().toString()
        val logSink = SessionLogSink(composite, sessionId)

        val antiDetectMonitor = InMemoryAntiDetectMonitor()
        val targetAppStore = DataStoreTargetAppStore(context, logSink)
        val rootCamera1Manager = RootCamera1Manager(context, logSink, antiDetectMonitor, targetAppStore)
        val virtualCameraEngine = AppVirtualCameraEngine(context, logSink, rootCamera1Manager)
        dependencies = AppDependencies(
            virtualCameraEngine = virtualCameraEngine,
            profileStore = DataStoreProfileStore(context, logSink),
            sourceRepository = InMemorySourceRepository(),
            logSink = logSink,
            antiDetectMonitor = antiDetectMonitor,
            rootCamera1Manager = rootCamera1Manager,
            targetAppStore = targetAppStore,
            installedAppsRepository = InstalledAppsRepository(context, logSink),
            onboardingStore = DataStoreOnboardingStore(context)
        )
        observerScope?.cancel()
        observerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).apply {
            launch {
                targetAppStore.selected.collect { target ->
                    val profile = target?.packageName?.let { dependencies.profileStore.get(it) }
                    if (profile != null) {
                        dependencies.virtualCameraEngine.applyProfile(profile)
                    }
                }
            }
        }
        logSystemInfo(context, logSink, sessionId)
    }
}

@Suppress("DEPRECATION")
private fun logSystemInfo(context: Context, logSink: LogSink, sessionId: String) {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: "unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
    logSink.log(
        com.shadowcam.core.model.LogLevel.INFO,
        "Session",
        "Session started",
        mapOf(
            "session" to sessionId,
            "pid" to Process.myPid().toString(),
            "versionName" to versionName,
            "versionCode" to versionCode.toString(),
            "sdk" to Build.VERSION.SDK_INT.toString(),
            "buildType" to Build.TYPE
        )
    )
    logSink.log(
        com.shadowcam.core.model.LogLevel.INFO,
        "Device",
        "Device info",
        mapOf(
            "brand" to Build.BRAND,
            "device" to Build.DEVICE,
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "release" to Build.VERSION.RELEASE,
            "abis" to Build.SUPPORTED_ABIS.joinToString(",")
        )
    )
}
