package com.shadowcam.logging

import com.shadowcam.core.model.LogEvent
import com.shadowcam.core.model.LogLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogRepository(
    private val capacity: Int = 500,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val events = MutableStateFlow<List<LogEvent>>(emptyList())

    val eventsFlow: Flow<List<LogEvent>> = events

    fun filtered(level: LogLevel?): Flow<List<LogEvent>> = events.map { list ->
        level?.let { lvl -> list.filter { it.level == lvl } } ?: list
    }

    suspend fun log(level: LogLevel, message: String, context: String? = null) =
        withContext(dispatcher) {
            val event = LogEvent(
                timestamp = System.currentTimeMillis(),
                level = level,
                message = message,
                context = context
            )
            val updated = (events.value + event).takeLast(capacity)
            events.emit(updated)
        }

    suspend fun clear() = withContext(dispatcher) {
        events.emit(emptyList())
    }

    fun export(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return buildString {
            events.value.forEach { event ->
                append(formatter.format(Date(event.timestamp)))
                append(" [${event.level}] ")
                if (!event.context.isNullOrEmpty()) {
                    append("${event.context}: ")
                }
                append(event.message)
                append('\n')
            }
        }
    }
}
