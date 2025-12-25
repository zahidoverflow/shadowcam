package com.shadowcam.logging

import com.shadowcam.core.model.LogEntry
import com.shadowcam.core.model.LogLevel
import kotlinx.coroutines.flow.Flow

class SessionLogSink(
    private val delegate: LogSink,
    private val sessionId: String
) : LogSink {
    override val logs: Flow<List<LogEntry>> = delegate.logs

    override fun log(level: LogLevel, tag: String, message: String, metadata: Map<String, String>) {
        val merged = if (metadata.containsKey("session")) {
            metadata
        } else {
            metadata + mapOf("session" to sessionId)
        }
        delegate.log(level, tag, message, merged)
    }

    override fun clear() = delegate.clear()
}
