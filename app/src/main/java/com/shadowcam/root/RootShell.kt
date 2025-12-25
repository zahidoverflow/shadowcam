package com.shadowcam.root

import com.shadowcam.logging.LogSink

data class RootCommandResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)

internal fun shellQuote(value: String): String {
    return "'" + value.replace("'", "'\\''") + "'"
}

class RootShell(private val logSink: LogSink?) {
    fun isAvailable(): Boolean {
        // Simple check, don't spam logs unless necessary, but maybe debug level is fine.
        val result = run("id", silent = true)
        return result.success && result.stdout.contains("uid=0")
    }

    fun run(command: String, silent: Boolean = false): RootCommandResult {
        if (!silent) {
            logSink?.log(
                com.shadowcam.core.model.LogLevel.DEBUG,
                "RootShell",
                "Exec: $command",
                mapOf("commandLength" to command.length.toString())
            )
        }
        return try {
            val start = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            val durationMs = System.currentTimeMillis() - start
            
            if (!silent) {
                if (exitCode != 0) {
                    logSink?.log(
                        com.shadowcam.core.model.LogLevel.ERROR,
                        "RootShell",
                        "Failed ($exitCode): ${truncate(stderr)}",
                        mapOf(
                            "exitCode" to exitCode.toString(),
                            "durationMs" to durationMs.toString(),
                            "stdoutLen" to stdout.length.toString(),
                            "stderrLen" to stderr.length.toString()
                        )
                    )
                } else {
                    // Log output if it's not empty and not too long?
                    val outLog = if (stdout.length > 200) stdout.take(200) + "..." else stdout
                    logSink?.log(
                        com.shadowcam.core.model.LogLevel.DEBUG,
                        "RootShell",
                        "Success: $outLog",
                        mapOf(
                            "exitCode" to exitCode.toString(),
                            "durationMs" to durationMs.toString(),
                            "stdoutLen" to stdout.length.toString(),
                            "stderrLen" to stderr.length.toString()
                        )
                    )
                }
            }
            
            RootCommandResult(exitCode == 0, stdout, stderr, exitCode)
        } catch (e: Exception) {
            val msg = e.message ?: "Root command failed"
            if (!silent) {
                logSink?.log(
                    com.shadowcam.core.model.LogLevel.ERROR,
                    "RootShell",
                    "Exception: ${truncate(msg)}"
                )
            }
            RootCommandResult(
                success = false,
                stdout = "",
                stderr = msg,
                exitCode = -1
            )
        }
    }

    fun fileExists(path: String): Boolean {
        val result = run("test -f ${shellQuote(path)}", silent = true)
        return result.success
    }

    private fun truncate(value: String, max: Int = 300): String {
        return if (value.length <= max) value else value.take(max) + "..."
    }
}
