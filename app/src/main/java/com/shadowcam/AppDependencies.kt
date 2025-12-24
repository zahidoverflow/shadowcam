package com.shadowcam

import com.shadowcam.antidetect.AntiDetectMonitor
import com.shadowcam.antidetect.InMemoryAntiDetectMonitor
import com.shadowcam.core.engine.FakeVirtualCameraEngine
import com.shadowcam.core.engine.VirtualCameraEngine
import android.content.Context
import com.shadowcam.logging.InMemoryLogSink
import com.shadowcam.logging.LogSink
import com.shadowcam.profiles.InMemoryProfileStore
import com.shadowcam.profiles.ProfileStore
import com.shadowcam.root.RootCamera1Manager
import com.shadowcam.sources.InMemorySourceRepository
import com.shadowcam.sources.SourceRepository

data class AppDependencies(
    val virtualCameraEngine: VirtualCameraEngine,
    val profileStore: ProfileStore,
    val sourceRepository: SourceRepository,
    val logSink: LogSink,
    val antiDetectMonitor: AntiDetectMonitor,
    val rootCamera1Manager: RootCamera1Manager
)

val LocalAppDependencies = androidx.compose.runtime.staticCompositionLocalOf<AppDependencies> {
    error("AppDependencies not provided")
}

object AppDependenciesProvider {
    lateinit var dependencies: AppDependencies
        private set

    fun installDefault(context: Context) {
        val memorySink = InMemoryLogSink()
        val logFile = java.io.File(context.filesDir, "shadowcam_debug.log")
        val fileSink = com.shadowcam.logging.FileLogSink(logFile)
        val logSink = com.shadowcam.logging.CompositeLogSink(listOf(memorySink, fileSink))

        val antiDetectMonitor = InMemoryAntiDetectMonitor()
        dependencies = AppDependencies(
            virtualCameraEngine = FakeVirtualCameraEngine(logSink),
            profileStore = InMemoryProfileStore(),
            sourceRepository = InMemorySourceRepository(),
            logSink = logSink,
            antiDetectMonitor = antiDetectMonitor,
            rootCamera1Manager = RootCamera1Manager(context, logSink, antiDetectMonitor)
        )
    }
}
