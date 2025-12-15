package com.shadowcam.core.engine

import com.shadowcam.core.model.AppProfile
import com.shadowcam.core.model.SourceDescriptor
import com.shadowcam.core.model.VirtualCameraMode
import com.shadowcam.core.model.VirtualCameraState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

interface VirtualCameraEngine {
    val state: StateFlow<VirtualCameraState>
    suspend fun start(source: SourceDescriptor?, profile: AppProfile? = null): Result<Unit>
    suspend fun stop(): Result<Unit>
    suspend fun applyProfile(profile: AppProfile): Result<Unit>
    fun setMode(mode: VirtualCameraMode)
}

class FakeVirtualCameraEngine : VirtualCameraEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(VirtualCameraState.Idle)
    override val state: StateFlow<VirtualCameraState> = _state

    init {
        scope.launch {
            // Simulate stats refresh while enabled.
            while (true) {
                delay(1_000)
                val current = _state.value
                if (current.isEnabled) {
                    _state.value = current.copy(
                        fps = 29f + Random.nextFloat(),
                        latencyMs = 20 + Random.nextLong(12),
                        uptimeMs = current.uptimeMs + 1_000
                    )
                }
            }
        }
    }

    override suspend fun start(source: SourceDescriptor?, profile: AppProfile?): Result<Unit> {
        _state.value = _state.value.copy(
            isEnabled = true,
            source = source ?: _state.value.source,
            profile = profile ?: _state.value.profile,
            uptimeMs = 0L
        )
        return Result.success(Unit)
    }

    override suspend fun stop(): Result<Unit> {
        _state.value = VirtualCameraState.Idle.copy(mode = _state.value.mode)
        return Result.success(Unit)
    }

    override suspend fun applyProfile(profile: AppProfile): Result<Unit> {
        _state.value = _state.value.copy(profile = profile)
        return Result.success(Unit)
    }

    override fun setMode(mode: VirtualCameraMode) {
        _state.value = _state.value.copy(mode = mode)
    }
}
