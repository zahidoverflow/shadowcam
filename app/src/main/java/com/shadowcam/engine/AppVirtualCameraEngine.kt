package com.shadowcam.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.shadowcam.core.engine.VirtualCameraEngine
import com.shadowcam.core.model.AppProfile
import com.shadowcam.core.model.LogLevel
import com.shadowcam.core.model.SourceDescriptor
import com.shadowcam.core.model.VirtualCameraMode
import com.shadowcam.core.model.VirtualCameraState
import com.shadowcam.logging.LogSink
import com.shadowcam.root.MarkerFile
import com.shadowcam.root.RootCamera1Manager
import com.shadowcam.root.SyncedMediaType
import com.shadowcam.service.VirtualCameraService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppVirtualCameraEngine(
    private val context: Context,
    private val logSink: LogSink,
    private val rootCamera1Manager: RootCamera1Manager
) : VirtualCameraEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(VirtualCameraState.Idle)
    override val state: StateFlow<VirtualCameraState> = _state

    init {
        scope.launch {
            while (true) {
                delay(1_000)
                val current = _state.value
                if (current.isEnabled) {
                    _state.value = current.copy(uptimeMs = current.uptimeMs + 1_000)
                }
            }
        }
        scope.launch {
            rootCamera1Manager.state.collect { rootState ->
                if (_state.value.mode == VirtualCameraMode.ROOT) {
                    val newSource = sourceFromSynced(rootState.lastSynced)
                    if (newSource != null && newSource != _state.value.source) {
                        _state.value = _state.value.copy(source = newSource)
                    }
                }
            }
        }
    }

    override suspend fun start(source: SourceDescriptor?, profile: AppProfile?): Result<Unit> {
        val mode = _state.value.mode
        if (mode == VirtualCameraMode.ROOT) {
            rootCamera1Manager.refreshRootStatus()
            val rootState = rootCamera1Manager.state.value
            if (!rootState.rootAvailable) {
                logSink.log(LogLevel.WARN, "Engine", "Start blocked: root unavailable")
                return Result.failure(IllegalStateException("Root unavailable"))
            }
            val privateDir = rootState.markers[MarkerFile.PRIVATE_DIR] == true
            val targetMismatch = privateDir &&
                rootState.targetApp?.packageName != rootState.lastSynced?.targetPackage
            if (rootState.lastSynced == null || targetMismatch) {
                val syncResult = when {
                    rootState.video != null -> rootCamera1Manager.syncVideo()
                    rootState.image != null -> rootCamera1Manager.syncImage()
                    else -> Result.failure(IllegalStateException("No source selected"))
                }
                if (syncResult.isFailure) {
                    logSink.log(LogLevel.WARN, "Engine", "Start blocked: sync failed")
                    return syncResult
                }
            }
            if (rootCamera1Manager.state.value.lastSynced == null) {
                logSink.log(LogLevel.WARN, "Engine", "Start blocked: no synced source")
                return Result.failure(IllegalStateException("No synced source"))
            }
        }
        startService()
        val resolvedSource = if (mode == VirtualCameraMode.ROOT) {
            sourceFromSynced(rootCamera1Manager.state.value.lastSynced) ?: source
        } else {
            source
        }
        _state.value = _state.value.copy(
            isEnabled = true,
            source = resolvedSource ?: _state.value.source,
            profile = profile ?: _state.value.profile,
            uptimeMs = 0L
        )
        logSink.log(LogLevel.INFO, "Engine", "VCAM started", mapOf("mode" to mode.name))
        return Result.success(Unit)
    }

    override suspend fun stop(): Result<Unit> {
        stopService()
        _state.value = VirtualCameraState.Idle.copy(mode = _state.value.mode)
        logSink.log(LogLevel.INFO, "Engine", "VCAM stopped", mapOf("mode" to _state.value.mode.name))
        return Result.success(Unit)
    }

    override suspend fun applyProfile(profile: AppProfile): Result<Unit> {
        _state.value = _state.value.copy(profile = profile)
        logSink.log(LogLevel.DEBUG, "Engine", "Profile applied", mapOf("package" to profile.packageName))
        return Result.success(Unit)
    }

    override fun setMode(mode: VirtualCameraMode) {
        _state.value = _state.value.copy(mode = mode)
        logSink.log(LogLevel.DEBUG, "Engine", "Mode set", mapOf("mode" to mode.name))
    }

    private fun startService() {
        val intent = Intent(context, VirtualCameraService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopService() {
        val intent = Intent(context, VirtualCameraService::class.java)
        context.stopService(intent)
    }

    private fun sourceFromSynced(media: com.shadowcam.root.SyncedMedia?): SourceDescriptor? {
        if (media == null) return null
        return when (media.type) {
            SyncedMediaType.VIDEO -> SourceDescriptor.Video(
                id = "root-video",
                name = media.name ?: "Root video",
                path = media.destPath,
                loop = true
            )
            SyncedMediaType.IMAGE -> SourceDescriptor.Image(
                id = "root-image",
                name = media.name ?: "Root image",
                path = media.destPath,
                loop = false
            )
        }
    }
}
