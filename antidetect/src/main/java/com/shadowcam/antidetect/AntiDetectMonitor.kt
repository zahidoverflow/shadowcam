package com.shadowcam.antidetect

import com.shadowcam.core.model.AntiDetectLevel
import com.shadowcam.core.model.AntiDetectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AntiDetectMonitor {
    val status: Flow<AntiDetectStatus>
    fun setLevel(level: AntiDetectLevel)
    fun toggleJitter(enabled: Boolean)
    fun setRootAvailable(available: Boolean)
}

class InMemoryAntiDetectMonitor : AntiDetectMonitor {
    private val backing = MutableStateFlow(
        AntiDetectStatus(
            isEmulator = false,
            isRootAvailable = false,
            jitterEnabled = false,
            level = AntiDetectLevel.SAFE
        )
    )
    override val status: Flow<AntiDetectStatus> = backing

    override fun setLevel(level: AntiDetectLevel) {
        backing.value = backing.value.copy(level = level)
    }

    override fun toggleJitter(enabled: Boolean) {
        backing.value = backing.value.copy(jitterEnabled = enabled)
    }

    override fun setRootAvailable(available: Boolean) {
        backing.value = backing.value.copy(isRootAvailable = available)
    }
}
