package com.shadowcam.antidetect

import com.shadowcam.core.model.AntiDetectConfig
import com.shadowcam.core.model.AntiDetectLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EnvironmentSignals(
    val isEmulator: Boolean,
    val isDebuggable: Boolean,
    val hasPlayServices: Boolean
)

data class AntiDetectStatus(
    val config: AntiDetectConfig,
    val signals: EnvironmentSignals,
    val riskLabel: String
)

class AntiDetectionManager(
    initialConfig: AntiDetectConfig = AntiDetectConfig(),
    initialSignals: EnvironmentSignals = EnvironmentSignals(
        isEmulator = false,
        isDebuggable = false,
        hasPlayServices = true
    )
) {
    private val state = MutableStateFlow(
        AntiDetectStatus(
            config = initialConfig,
            signals = initialSignals,
            riskLabel = risk(initialConfig)
        )
    )

    val status = state.asStateFlow()

    fun updateConfig(config: AntiDetectConfig) {
        state.value = state.value.copy(config = config, riskLabel = risk(config))
    }

    fun updateSignals(signals: EnvironmentSignals) {
        state.value = state.value.copy(signals = signals)
    }

    private fun risk(config: AntiDetectConfig): String = when (config.level) {
        AntiDetectLevel.Low -> "Low"
        AntiDetectLevel.Medium -> "Medium"
        AntiDetectLevel.High -> "High"
    }
}
