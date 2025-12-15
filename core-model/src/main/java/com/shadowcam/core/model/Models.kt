package com.shadowcam.core.model

data class Resolution(val width: Int, val height: Int) {
    val label: String = "${width}x${height}"
}

enum class VirtualCameraMode { ROOT, NON_ROOT }

enum class AntiDetectLevel { SAFE, BALANCED, AGGRESSIVE }

data class VirtualCameraState(
    val isEnabled: Boolean,
    val mode: VirtualCameraMode,
    val source: SourceDescriptor?,
    val profile: AppProfile?,
    val fps: Float,
    val latencyMs: Long,
    val uptimeMs: Long
) {
    companion object {
        val Idle = VirtualCameraState(
            isEnabled = false,
            mode = VirtualCameraMode.NON_ROOT,
            source = null,
            profile = null,
            fps = 0f,
            latencyMs = 0,
            uptimeMs = 0
        )
    }
}

data class AppProfile(
    val packageName: String,
    val label: String,
    val resolution: Resolution,
    val fps: Float,
    val antiDetectLevel: AntiDetectLevel,
    val defaultSourceId: SourceId? = null,
    val exifStrategy: String = "balanced"
)

typealias SourceId = String

sealed class SourceDescriptor(open val id: SourceId, open val name: String) {
    data class Image(
        override val id: SourceId,
        override val name: String,
        val path: String,
        val loop: Boolean = false
    ) : SourceDescriptor(id, name)

    data class Video(
        override val id: SourceId,
        override val name: String,
        val path: String,
        val loop: Boolean = true
    ) : SourceDescriptor(id, name)

    data class Live(
        override val id: SourceId,
        override val name: String,
        val description: String
    ) : SourceDescriptor(id, name)

    data class Rtsp(
        override val id: SourceId,
        override val name: String,
        val url: String
    ) : SourceDescriptor(id, name)
}

enum class LogLevel { INFO, WARN, ERROR, DEBUG }

data class LogEntry(
    val timestampMs: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)

data class AntiDetectStatus(
    val isEmulator: Boolean,
    val isRootAvailable: Boolean,
    val jitterEnabled: Boolean,
    val level: AntiDetectLevel
)
