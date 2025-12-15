package com.shadowcam

import com.shadowcam.antidetect.AntiDetectMonitor
import com.shadowcam.antidetect.InMemoryAntiDetectMonitor
import com.shadowcam.core.engine.FakeVirtualCameraEngine
import com.shadowcam.core.engine.VirtualCameraEngine
import com.shadowcam.logging.InMemoryLogSink
import com.shadowcam.logging.LogSink
import com.shadowcam.profiles.InMemoryProfileStore
import com.shadowcam.profiles.ProfileStore
import com.shadowcam.sources.InMemorySourceRepository
import com.shadowcam.sources.SourceRepository

data class AppDependencies(
    val virtualCameraEngine: VirtualCameraEngine,
    val profileStore: ProfileStore,
    val sourceRepository: SourceRepository,
    val logSink: LogSink,
    val antiDetectMonitor: AntiDetectMonitor
)

val LocalAppDependencies = androidx.compose.runtime.staticCompositionLocalOf<AppDependencies> {
    error("AppDependencies not provided")
}

object AppDependenciesProvider {
    lateinit var dependencies: AppDependencies
        private set

    fun installDefault() {
        dependencies = AppDependencies(
            virtualCameraEngine = FakeVirtualCameraEngine(),
            profileStore = InMemoryProfileStore(),
            sourceRepository = InMemorySourceRepository(),
            logSink = InMemoryLogSink(),
            antiDetectMonitor = InMemoryAntiDetectMonitor()
        )
    }
}
