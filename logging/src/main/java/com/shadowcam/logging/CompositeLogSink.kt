package com.shadowcam.logging

import com.shadowcam.core.model.LogEntry
import com.shadowcam.core.model.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompositeLogSink(private val sinks: List<LogSink>) : LogSink {
    
    // Combine flows from all sinks. In practice, usually one is InMemory and others are write-only.
    // If multiple sinks return logs, we might want to merge them or just take the primary one.
    // Here we assume the first one is the "primary" source of truth for UI (InMemory).
    override val logs: Flow<List<LogEntry>>
        get() = sinks.firstOrNull()?.logs ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun log(level: LogLevel, tag: String, message: String, metadata: Map<String, String>) {
        sinks.forEach { it.log(level, tag, message, metadata) }
    }

    override fun clear() {
        sinks.forEach { it.clear() }
    }
}
