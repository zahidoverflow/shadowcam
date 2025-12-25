package com.shadowcam.logging

import com.shadowcam.core.model.LogEntry
import com.shadowcam.core.model.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface LogSink {
    val logs: Flow<List<LogEntry>>
    fun log(level: LogLevel, tag: String, message: String, metadata: Map<String, String> = emptyMap())
    fun clear()
}

class InMemoryLogSink : LogSink {
    private val backing = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logs: Flow<List<LogEntry>> = backing

    override fun log(level: LogLevel, tag: String, message: String, metadata: Map<String, String>) {
        val entry = LogEntry(
            timestampMs = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            metadata = metadata,
            thread = Thread.currentThread().name
        )
        backing.value = (backing.value + entry).takeLast(500)
    }

    override fun clear() {
        backing.value = emptyList()
    }
}
