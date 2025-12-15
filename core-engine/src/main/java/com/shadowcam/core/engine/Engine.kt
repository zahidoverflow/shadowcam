package com.shadowcam.core.engine

import com.shadowcam.core.model.CameraProfile
import com.shadowcam.core.model.MediaSource
import com.shadowcam.core.model.OperationMode
import com.shadowcam.core.model.VcamStatus
import com.shadowcam.core.model.VirtualCameraState
import com.shadowcam.core.model.VirtualCameraStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface VirtualCameraEngine {
    val state: Flow<VirtualCameraState>
    fun start(profile: CameraProfile?, source: MediaSource?, mode: OperationMode)
    fun stop()
    fun attachSource(source: MediaSource?)
    fun applyProfile(profile: CameraProfile?)
    fun toggleExpertMode(enabled: Boolean)
    fun updateStats(fps: Int, resolution: String, latencyMs: Int, uptime: Duration)
}

class InMemoryVirtualCameraEngine(
    initialState: VirtualCameraState,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : VirtualCameraEngine {

    private val mutableState = MutableStateFlow(initialState)
    override val state: Flow<VirtualCameraState> = mutableState.asStateFlow()

    override fun start(profile: CameraProfile?, source: MediaSource?, mode: OperationMode) {
        scope.launch {
            mutableState.emit(
                current().copy(
                    status = VcamStatus.On,
                    activeProfile = profile ?: current().activeProfile,
                    activeSource = source ?: current().activeSource,
                    mode = mode
                )
            )
        }
    }

    override fun stop() {
        scope.launch {
            mutableState.emit(current().copy(status = VcamStatus.Off))
        }
    }

    override fun attachSource(source: MediaSource?) {
        scope.launch {
            mutableState.emit(current().copy(activeSource = source ?: current().activeSource))
        }
    }

    override fun applyProfile(profile: CameraProfile?) {
        scope.launch {
            mutableState.emit(current().copy(activeProfile = profile ?: current().activeProfile))
        }
    }

    override fun toggleExpertMode(enabled: Boolean) {
        scope.launch {
            mutableState.emit(current().copy(expertMode = enabled))
        }
    }

    override fun updateStats(fps: Int, resolution: String, latencyMs: Int, uptime: Duration) {
        scope.launch {
            mutableState.emit(
                current().copy(
                    stats = VirtualCameraStats(
                        fps = fps,
                        resolution = resolution,
                        latencyMs = latencyMs,
                        uptimeSeconds = uptime.inWholeSeconds
                    )
                )
            )
        }
    }

    private fun current(): VirtualCameraState = mutableState.value
}

class VirtualCameraController(
    private val engine: VirtualCameraEngine
) {
    fun arm(profile: CameraProfile?, source: MediaSource?, mode: OperationMode) =
        engine.start(profile, source, mode)

    fun disarm() = engine.stop()

    fun swapSource(source: MediaSource?) = engine.attachSource(source)

    fun swapProfile(profile: CameraProfile?) = engine.applyProfile(profile)

    fun setExpert(enabled: Boolean) = engine.toggleExpertMode(enabled)

    fun refreshStats() {
        // Placeholder: in production wire to real metrics.
        engine.updateStats(fps = 30, resolution = "1080p", latencyMs = 18, uptime = 120.seconds)
    }
}
