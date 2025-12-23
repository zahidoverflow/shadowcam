package com.shadowcam.root

data class RootCommandResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)

internal fun shellQuote(value: String): String {
    return "'" + value.replace("'", "'\\''") + "'"
}

class RootShell {
    fun isAvailable(): Boolean {
        val result = run("id")
        return result.success && result.stdout.contains("uid=0")
    }

    fun run(command: String): RootCommandResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            RootCommandResult(exitCode == 0, stdout, stderr, exitCode)
        } catch (e: Exception) {
            RootCommandResult(
                success = false,
                stdout = "",
                stderr = e.message ?: "Root command failed",
                exitCode = -1
            )
        }
    }

    fun fileExists(path: String): Boolean {
        val result = run("test -f ${shellQuote(path)}")
        return result.success
    }
}
