package com.shadowcam.root

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.shadowcam.antidetect.AntiDetectMonitor
import com.shadowcam.apps.TargetApp
import com.shadowcam.apps.TargetAppStore
import com.shadowcam.core.model.LogLevel
import com.shadowcam.logging.LogSink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PickedMedia(
    val uri: String,
    val displayName: String?
)

data class RootStatusMessage(
    val text: String,
    val isError: Boolean
)

enum class SyncedMediaType { VIDEO, IMAGE }

data class SyncedMedia(
    val type: SyncedMediaType,
    val name: String?,
    val syncedAtMs: Long,
    val destPath: String,
    val targetPackage: String?
)

enum class MarkerFile(val fileName: String, val label: String, val description: String) {
    DISABLE("disable.jpg", "Disable module", "Disable the VCAM module"),
    NO_TOAST("no_toast.jpg", "Hide toasts", "Suppress module toast messages"),
    FORCE_SHOW("force_show.jpg", "Force path toast", "Show Camera1 directory toast"),
    PRIVATE_DIR("private_dir.jpg", "Private dir per app", "Force app private Camera1"),
    NO_SILENT("no-silent.jpg", "Enable audio", "Allow video audio if supported")
}

data class RootCamera1State(
    val rootAvailable: Boolean = false,
    val camera1Dir: String = defaultCamera1Dir(),
    val activeCamera1Dir: String = defaultCamera1Dir(),
    val targetApp: TargetApp? = null,
    val video: PickedMedia? = null,
    val image: PickedMedia? = null,
    val markers: Map<MarkerFile, Boolean> = defaultMarkerState(),
    val message: RootStatusMessage? = null,
    val busy: Boolean = false,
    val lastSynced: SyncedMedia? = null
)

class RootCamera1Manager(
    context: Context,
    private val logSink: LogSink,
    private val antiDetectMonitor: AntiDetectMonitor,
    private val targetAppStore: TargetAppStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val shell = RootShell(logSink)
    private val settingsStore = RootSettingsStore(appContext)
    private val camera1Dir = defaultCamera1Dir()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(RootCamera1State(camera1Dir = camera1Dir, activeCamera1Dir = camera1Dir))
    val state: StateFlow<RootCamera1State> = _state

    init {
        scope.launch {
            settingsStore.settings.collect { settings ->
                updateState { current ->
                    current.copy(
                        video = settings.videoUri?.let { PickedMedia(it, settings.videoName) },
                        image = settings.imageUri?.let { PickedMedia(it, settings.imageName) }
                    )
                }
            }
        }
        scope.launch {
            targetAppStore.selected.collect { target ->
                updateState { current -> current.copy(targetApp = target) }
                logSink.log(
                    LogLevel.DEBUG,
                    "Root",
                    "Target app updated",
                    mapOf("target" to (target?.packageName ?: "none"))
                )
            }
        }
        scope.launch { refreshRootStatus() }
    }

    suspend fun refreshRootStatus() = withContext(dispatcher) {
        val available = shell.isAvailable()
        updateState { it.copy(rootAvailable = available) }
        antiDetectMonitor.setRootAvailable(available)
        logSink.log(
            LogLevel.DEBUG,
            "Root",
            "Root status refreshed",
            mapOf("available" to available.toString())
        )
        if (available) {
            refreshMarkersInternal()
        } else {
            setMessage("Root not available", true)
        }
    }

    suspend fun refreshMarkers() = withContext(dispatcher) {
        if (!ensureRoot()) return@withContext
        refreshMarkersInternal()
    }

    suspend fun selectVideo(uri: Uri, name: String?) = withContext(dispatcher) {
        settingsStore.saveVideo(uri, name)
        logSink.log(
            LogLevel.INFO,
            "Root",
            "Video selected",
            mapOf(
                "uri" to uri.toString(),
                "name" to (name ?: "unknown")
            )
        )
        setMessage("Video selected", false)
    }

    suspend fun selectImage(uri: Uri, name: String?) = withContext(dispatcher) {
        settingsStore.saveImage(uri, name)
        logSink.log(
            LogLevel.INFO,
            "Root",
            "Image selected",
            mapOf(
                "uri" to uri.toString(),
                "name" to (name ?: "unknown")
            )
        )
        setMessage("Image selected", false)
    }

    suspend fun syncVideo(): Result<Unit> = withContext(dispatcher) {
        if (!ensureRoot()) return@withContext failure("Root not available")
        val media = _state.value.video
        if (media == null) {
            setMessage("Pick a video first", true)
            return@withContext failure("No video selected")
        }
        setBusy(true)
        val usePrivate = _state.value.markers[MarkerFile.PRIVATE_DIR] == true
        if (usePrivate && _state.value.targetApp == null) {
            setBusy(false)
            setMessage("Select a target app for private dir", true)
            logSink.log(LogLevel.WARN, "Root", "Private dir enabled without target")
            return@withContext failure("Private dir enabled without target")
        }
        val resolvedDir = resolveCamera1Dir(_state.value)
        logSink.log(
            LogLevel.DEBUG,
            "Root",
            "Sync video requested",
            mapOf(
                "uri" to media.uri,
                "name" to (media.displayName ?: "unknown"),
                "destDir" to resolvedDir,
                "privateDir" to usePrivate.toString(),
                "target" to (_state.value.targetApp?.packageName ?: "none")
            )
        )
        val tempFile = copyUriToTemp(Uri.parse(media.uri), ".mp4")
        if (tempFile == null) {
            setBusy(false)
            setMessage("Unable to read video", true)
            return@withContext failure("Unable to read video")
        }
        val destPath = File(resolvedDir, "virtual.mp4").absolutePath
        val command = buildCopyCommand(resolvedDir, tempFile.absolutePath, destPath)
        val result = shell.run(command)
        val sizeBytes = tempFile.length()
        tempFile.delete()
        setBusy(false)
        if (result.success) {
            logSink.log(LogLevel.INFO, "Root", "Video synced to Camera1")
            updateState {
                it.copy(
                    lastSynced = SyncedMedia(
                        type = SyncedMediaType.VIDEO,
                        name = media.displayName,
                        syncedAtMs = System.currentTimeMillis(),
                        destPath = destPath,
                        targetPackage = it.targetApp?.packageName
                    )
                )
            }
            setMessage("Video synced to Camera1", false)
        } else {
            logSink.log(LogLevel.ERROR, "Root", "Video sync failed: ${result.stderr}")
            setMessage("Video sync failed", true)
        }
        logSink.log(
            LogLevel.DEBUG,
            "Root",
            "Sync video finished",
            mapOf(
                "destPath" to destPath,
                "sizeBytes" to sizeBytes.toString(),
                "success" to result.success.toString()
            )
        )
        if (result.success) {
            Result.success(Unit)
        } else {
            failure("Video sync failed")
        }
    }

    suspend fun syncImage(): Result<Unit> = withContext(dispatcher) {
        if (!ensureRoot()) return@withContext failure("Root not available")
        val media = _state.value.image
        if (media == null) {
            setMessage("Pick an image first", true)
            return@withContext failure("No image selected")
        }
        setBusy(true)
        val usePrivate = _state.value.markers[MarkerFile.PRIVATE_DIR] == true
        if (usePrivate && _state.value.targetApp == null) {
            setBusy(false)
            setMessage("Select a target app for private dir", true)
            logSink.log(LogLevel.WARN, "Root", "Private dir enabled without target")
            return@withContext failure("Private dir enabled without target")
        }
        val resolvedDir = resolveCamera1Dir(_state.value)
        logSink.log(
            LogLevel.DEBUG,
            "Root",
            "Sync image requested",
            mapOf(
                "uri" to media.uri,
                "name" to (media.displayName ?: "unknown"),
                "destDir" to resolvedDir,
                "privateDir" to usePrivate.toString(),
                "target" to (_state.value.targetApp?.packageName ?: "none")
            )
        )
        val tempFile = copyUriToTemp(Uri.parse(media.uri), ".bmp")
        if (tempFile == null) {
            setBusy(false)
            setMessage("Unable to read image", true)
            return@withContext failure("Unable to read image")
        }
        val destPath = File(resolvedDir, "1000.bmp").absolutePath
        val command = buildCopyCommand(resolvedDir, tempFile.absolutePath, destPath)
        val result = shell.run(command)
        val sizeBytes = tempFile.length()
        tempFile.delete()
        setBusy(false)
        if (result.success) {
            logSink.log(LogLevel.INFO, "Root", "Image synced to Camera1")
            updateState {
                it.copy(
                    lastSynced = SyncedMedia(
                        type = SyncedMediaType.IMAGE,
                        name = media.displayName,
                        syncedAtMs = System.currentTimeMillis(),
                        destPath = destPath,
                        targetPackage = it.targetApp?.packageName
                    )
                )
            }
            setMessage("Image synced to Camera1", false)
        } else {
            logSink.log(LogLevel.ERROR, "Root", "Image sync failed: ${result.stderr}")
            setMessage("Image sync failed", true)
        }
        logSink.log(
            LogLevel.DEBUG,
            "Root",
            "Sync image finished",
            mapOf(
                "destPath" to destPath,
                "sizeBytes" to sizeBytes.toString(),
                "success" to result.success.toString()
            )
        )
        if (result.success) {
            Result.success(Unit)
        } else {
            failure("Image sync failed")
        }
    }

    suspend fun setMarker(marker: MarkerFile, enabled: Boolean) = withContext(dispatcher) {
        if (!ensureRoot()) return@withContext
        setBusy(true)
        val path = markerPath(marker)
        val command = if (enabled) {
            "mkdir -p ${shellQuote(camera1Dir)} && : > ${shellQuote(path)} && chmod 644 ${shellQuote(path)}"
        } else {
            "rm -f ${shellQuote(path)}"
        }
        val result = shell.run(command)
        setBusy(false)
        if (result.success) {
            updateState { current ->
                current.copy(markers = current.markers.toMutableMap().apply { put(marker, enabled) })
            }
            logSink.log(
                LogLevel.INFO,
                "Root",
                "${marker.label} ${if (enabled) "enabled" else "disabled"}",
                mapOf("path" to path)
            )
            setMessage("${marker.label} ${if (enabled) "enabled" else "disabled"}", false)
        } else {
            logSink.log(LogLevel.ERROR, "Root", "Marker update failed: ${result.stderr}")
            setMessage("Marker update failed", true)
        }
    }

    suspend fun exportLogs() = withContext(dispatcher) {
        if (!ensureRoot()) {
            setMessage("Root needed to export logs", true)
            return@withContext
        }
        val src = File(appContext.filesDir, "shadowcam_debug.log").absolutePath
        val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "shadowcam_debug.log").absolutePath
        // rm dest first to ensure fresh copy? cp -f does it.
        val command = "cp -f ${shellQuote(src)} ${shellQuote(dest)} && chmod 644 ${shellQuote(dest)}"
        
        val result = shell.run(command)
        if (result.success) {
            logSink.log(
                LogLevel.INFO,
                "Root",
                "Logs exported to $dest",
                mapOf("dest" to dest)
            )
            setMessage("Logs exported to Downloads", false)
        } else {
            logSink.log(LogLevel.ERROR, "Root", "Log export failed: ${result.stderr}")
            setMessage("Log export failed", true)
        }
    }

    private suspend fun ensureRoot(): Boolean {
        if (_state.value.rootAvailable) return true
        val available = shell.isAvailable()
        updateState { it.copy(rootAvailable = available) }
        antiDetectMonitor.setRootAvailable(available)
        if (!available) {
            setMessage("Root not available", true)
            logSink.log(LogLevel.WARN, "Root", "Root unavailable")
        }
        return available
    }

    private fun refreshMarkersInternal() {
        val markers = MarkerFile.entries.associateWith { shell.fileExists(markerPath(it)) }
        updateState { it.copy(markers = markers) }
    }

    private fun markerPath(marker: MarkerFile): String = File(camera1Dir, marker.fileName).absolutePath

    private fun buildCopyCommand(destDir: String, sourcePath: String, destPath: String): String {
        return "mkdir -p ${shellQuote(destDir)} && cp -f ${shellQuote(sourcePath)} ${shellQuote(destPath)} && chmod 644 ${shellQuote(destPath)}"
    }

    private fun copyUriToTemp(uri: Uri, suffix: String): File? {
        return try {
            val tempFile = File.createTempFile("shadowcam_", suffix, appContext.cacheDir)
            val input = appContext.contentResolver.openInputStream(uri)
            if (input == null) {
                tempFile.delete()
                return null
            }
            input.use { stream ->
                tempFile.outputStream().use { output ->
                    stream.copyTo(output)
                }
            }
            tempFile
        } catch (_: Exception) {
            // Best-effort cleanup.
            appContext.cacheDir.listFiles()?.filter { it.name.startsWith("shadowcam_") }?.forEach {
                it.delete()
            }
            null
        }
    }

    private fun setMessage(text: String, isError: Boolean) {
        updateState { it.copy(message = RootStatusMessage(text, isError)) }
    }

    private fun setBusy(isBusy: Boolean) {
        updateState { it.copy(busy = isBusy) }
    }

    private fun failure(message: String): Result<Unit> {
        return Result.failure(IllegalStateException(message))
    }

    private fun updateState(transform: (RootCamera1State) -> RootCamera1State) {
        _state.update { current ->
            updateActiveDir(transform(current))
        }
    }

    private fun updateActiveDir(state: RootCamera1State): RootCamera1State {
        val resolved = resolveCamera1Dir(state)
        return if (state.activeCamera1Dir == resolved) {
            state
        } else {
            logSink.log(
                LogLevel.DEBUG,
                "Root",
                "Active Camera1 dir updated",
                mapOf(
                    "from" to state.activeCamera1Dir,
                    "to" to resolved,
                    "target" to (state.targetApp?.packageName ?: "none"),
                    "privateDir" to (state.markers[MarkerFile.PRIVATE_DIR] == true).toString()
                )
            )
            state.copy(activeCamera1Dir = resolved)
        }
    }

    private fun resolveCamera1Dir(state: RootCamera1State): String {
        val usePrivate = state.markers[MarkerFile.PRIVATE_DIR] == true
        val target = state.targetApp?.packageName
        return if (usePrivate && !target.isNullOrBlank()) {
            File(state.camera1Dir, target).absolutePath
        } else {
            state.camera1Dir
        }
    }
}

@Suppress("DEPRECATION")
private fun defaultCamera1Dir(): String {
    val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
    return File(base, "Camera1").absolutePath
}

private fun defaultMarkerState(): Map<MarkerFile, Boolean> =
    MarkerFile.entries.associateWith { false }
