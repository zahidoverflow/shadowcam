package com.shadowcam.logging

import com.shadowcam.core.model.LogEntry
import com.shadowcam.core.model.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogSink(
    private val logFile: File
) : LogSink {
    // We don't expose historical logs from file to the UI flow to keep it simple/fast.
    // The UI uses InMemoryLogSink for that.
    override val logs: Flow<List<LogEntry>> = flowOf(emptyList())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<String>(capacity = Channel.UNLIMITED)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        scope.launch {
            for (line in channel) {
                try {
                    FileWriter(logFile, true).use { writer ->
                        writer.append(line).append("\n")
                    }
                } catch (e: IOException) {
                    // Fallback? complicated. Just ignore for now.
                    e.printStackTrace()
                }
            }
        }
    }

    override fun log(level: LogLevel, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val line = "$timestamp [$level] $tag: $message"
        channel.trySend(line)
    }

    override fun clear() {
        scope.launch {
            try {
                if (logFile.exists()) {
                    logFile.writeText("")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
