@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shadowcam.features.sources

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shadowcam.LocalAppDependencies
import com.shadowcam.core.model.Resolution
import com.shadowcam.logging.LogSink
import com.shadowcam.root.MarkerFile
import com.shadowcam.root.PickedMedia
import com.shadowcam.root.RootCamera1State
import com.shadowcam.root.SyncedMedia
import com.shadowcam.ui.theme.AccentLime
import com.shadowcam.ui.theme.SurfaceDark
import com.shadowcam.ui.theme.SurfaceElevated
import com.shadowcam.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun SourcesScreen() {
    val deps = LocalAppDependencies.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by deps.rootCamera1Manager.state.collectAsState()
    val targetApp by deps.targetAppStore.selected.collectAsState(initial = null)
    val profiles by deps.profileStore.profiles.collectAsState(initial = emptyList())
    val targetProfile = remember(targetApp, profiles) {
        profiles.firstOrNull { it.packageName == targetApp?.packageName }
    }
    val targetResolution = targetProfile?.resolution ?: Resolution(1920, 1080)
    val targetLabel = targetProfile?.label ?: "Default"
    val scrollState = rememberScrollState()
    var videoPreviewMode by remember { mutableStateOf(PreviewScaleMode.SOURCE) }
    var imagePreviewMode by remember { mutableStateOf(PreviewScaleMode.SOURCE) }

    val pickVideoLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            val name = queryDisplayName(context, uri)
            scope.launch { deps.rootCamera1Manager.selectVideo(uri, name) }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            val name = queryDisplayName(context, uri)
            scope.launch { deps.rootCamera1Manager.selectImage(uri, name) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sources", style = MaterialTheme.typography.headlineSmall)
        RootStatusCard(state, onRefresh = {
            scope.launch { deps.rootCamera1Manager.refreshRootStatus() }
        })
        if (state.markers[MarkerFile.PRIVATE_DIR] == true && state.targetApp == null) {
            Text(
                "Private dir is enabled. Select a target app to sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarningAmber
            )
        }

        MediaCard(
            title = "Video source (virtual.mp4)",
            selected = state.video,
            pickLabel = "Pick Video",
            onPick = { pickVideoLauncher.launch(arrayOf("video/*")) },
            onSync = { scope.launch { deps.rootCamera1Manager.syncVideo() } },
            enabled = state.rootAvailable && !state.busy
        )
        MediaPreviewCard(
            title = "Video Preview",
            selected = state.video,
            kind = MediaKind.VIDEO,
            targetResolution = targetResolution,
            targetLabel = targetLabel,
            previewMode = videoPreviewMode,
            onPreviewModeChange = { videoPreviewMode = it },
            logSink = deps.logSink
        )

        MediaCard(
            title = "Image source (1000.bmp)",
            selected = state.image,
            pickLabel = "Pick Image",
            onPick = { pickImageLauncher.launch(arrayOf("image/*")) },
            onSync = { scope.launch { deps.rootCamera1Manager.syncImage() } },
            enabled = state.rootAvailable && !state.busy
        )
        MediaPreviewCard(
            title = "Image Preview",
            selected = state.image,
            kind = MediaKind.IMAGE,
            targetResolution = targetResolution,
            targetLabel = targetLabel,
            previewMode = imagePreviewMode,
            onPreviewModeChange = { imagePreviewMode = it },
            logSink = deps.logSink
        )

        MarkerCard(
            state = state,
            onToggle = { marker, enabled ->
                scope.launch { deps.rootCamera1Manager.setMarker(marker, enabled) }
            },
            onRefresh = { scope.launch { deps.rootCamera1Manager.refreshMarkers() } }
        )

        state.lastSynced?.let { synced ->
            LastSyncedCard(synced)
        }

        Text(
            "Marker files are used by compatible Xposed or LSPosed camera modules.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private enum class MediaKind { VIDEO, IMAGE }

private enum class PreviewScaleMode(val label: String) {
    SOURCE("Source"),
    TARGET_FIT("Target Fit"),
    TARGET_FILL("Target Fill")
}

private data class MediaInfo(
    val width: Int,
    val height: Int
) {
    val ratio: Float = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f
}

@Composable
private fun RootStatusCard(state: RootCamera1State, onRefresh: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Root Camera1", style = MaterialTheme.typography.titleMedium)
            Text(
                if (state.rootAvailable) "Root available" else "Root not available",
                color = if (state.rootAvailable) AccentLime else WarningAmber
            )
            Text("Base path: ${state.camera1Dir}", style = MaterialTheme.typography.bodyMedium)
            Text("Active path: ${state.activeCamera1Dir}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Target app: ${state.targetApp?.label ?: "None"}",
                style = MaterialTheme.typography.bodyMedium
            )
            state.targetApp?.let {
                Text(it.packageName, style = MaterialTheme.typography.labelMedium)
            }
            if (state.busy) {
                Text("Working...", style = MaterialTheme.typography.labelMedium, color = AccentLime)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            }
            state.message?.let { message ->
                Text(
                    message.text,
                    color = if (message.isError) WarningAmber else AccentLime,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MediaPreviewCard(
    title: String,
    selected: PickedMedia?,
    kind: MediaKind,
    targetResolution: Resolution,
    targetLabel: String,
    previewMode: PreviewScaleMode,
    onPreviewModeChange: (PreviewScaleMode) -> Unit,
    logSink: LogSink
) {
    val context = LocalContext.current
    val uri = remember(selected?.uri) { selected?.uri?.let(Uri::parse) }
    val mediaInfo by rememberMediaInfo(kind, uri, context, logSink)
    val targetRatio = targetResolution.width.toFloat() / targetResolution.height.toFloat()
    val previewRatio = when (previewMode) {
        PreviewScaleMode.SOURCE -> mediaInfo?.ratio ?: targetRatio
        PreviewScaleMode.TARGET_FIT, PreviewScaleMode.TARGET_FILL -> targetRatio
    }
    val contentScale = when (previewMode) {
        PreviewScaleMode.TARGET_FILL -> ContentScale.Crop
        else -> ContentScale.Fit
    }

    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            PreviewModeRow(previewMode = previewMode, onPreviewModeChange = onPreviewModeChange)
            RatioHint(
                sourceInfo = mediaInfo,
                targetResolution = targetResolution,
                targetLabel = targetLabel
            )
            if (uri == null) {
                Text("Pick a ${kind.name.lowercase()} to preview", style = MaterialTheme.typography.bodyMedium)
            } else {
                when (kind) {
                    MediaKind.IMAGE -> {
                        val bitmap = rememberImageBitmap(context, uri)
                        ImagePreviewBox(
                            bitmap = bitmap,
                            previewRatio = previewRatio,
                            contentScale = contentScale
                        )
                    }
                    MediaKind.VIDEO -> {
                        VideoPreviewBox(
                            uri = uri,
                            sourceInfo = mediaInfo,
                            previewRatio = previewRatio,
                            fillMode = previewMode == PreviewScaleMode.TARGET_FILL,
                            logSink = logSink
                        )
                    }
                }
            }
            Text(
                "Fit keeps the full frame. Fill crops to match the target ratio.",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PreviewModeRow(
    previewMode: PreviewScaleMode,
    onPreviewModeChange: (PreviewScaleMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PreviewScaleMode.entries.forEach { mode ->
            FilterChip(
                selected = previewMode == mode,
                onClick = { onPreviewModeChange(mode) },
                label = { Text(mode.label) }
            )
        }
    }
}

@Composable
private fun RatioHint(
    sourceInfo: MediaInfo?,
    targetResolution: Resolution,
    targetLabel: String
) {
    val sourceLabel = if (sourceInfo == null) {
        "Source ratio: unknown"
    } else {
        "Source ratio: ${formatRatio(sourceInfo.width, sourceInfo.height)}"
    }
    val targetLabelText = "Target ratio ($targetLabel): ${formatRatio(targetResolution.width, targetResolution.height)}"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(sourceLabel, style = MaterialTheme.typography.labelMedium)
        Text(targetLabelText, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ImagePreviewBox(
    bitmap: ImageBitmap?,
    previewRatio: Float,
    contentScale: ContentScale
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .aspectRatio(previewRatio)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            Text("Loading preview...", style = MaterialTheme.typography.bodyMedium)
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun VideoPreviewBox(
    uri: Uri,
    sourceInfo: MediaInfo?,
    previewRatio: Float,
    fillMode: Boolean,
    logSink: LogSink
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .aspectRatio(previewRatio)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = max(1f, constraints.maxWidth.toFloat())
        val containerHeightPx = max(1f, constraints.maxHeight.toFloat())
        val srcWidth = max(1f, sourceInfo?.width?.toFloat() ?: containerWidthPx)
        val srcHeight = max(1f, sourceInfo?.height?.toFloat() ?: containerHeightPx)
        val scale = if (fillMode) {
            max(containerWidthPx / srcWidth, containerHeightPx / srcHeight)
        } else {
            min(containerWidthPx / srcWidth, containerHeightPx / srcHeight)
        }
        val viewWidth = with(density) { (srcWidth * scale).toDp() }
        val viewHeight = with(density) { (srcHeight * scale).toDp() }
        val viewHolder = remember { mutableStateOf<VideoView?>(null) }
        DisposableEffect(uri) {
            onDispose { viewHolder.value?.stopPlayback() }
        }

        AndroidView(
            factory = { context ->
                VideoView(context).also { view ->
                    viewHolder.value = view
                    view.setVideoURI(uri)
                    view.setOnPreparedListener { player ->
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        view.start()
                    }
                    view.setOnErrorListener { _, what, extra ->
                        logSink.log(
                            com.shadowcam.core.model.LogLevel.WARN,
                            "Preview",
                            "Video preview failed",
                            mapOf("what" to what.toString(), "extra" to extra.toString())
                        )
                        true
                    }
                }
            },
            update = { view ->
                val current = view.tag as? String
                val next = uri.toString()
                if (current != next) {
                    view.tag = next
                    view.setVideoURI(uri)
                    view.start()
                }
            },
            modifier = Modifier.size(viewWidth, viewHeight)
        )
    }
}

@Composable
private fun LastSyncedCard(lastSynced: SyncedMedia) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Last Synced", style = MaterialTheme.typography.titleMedium)
            Text(
                "${lastSynced.type.name.lowercase().replaceFirstChar { it.uppercase() }}: ${lastSynced.name ?: "Unnamed"}",
                style = MaterialTheme.typography.bodyMedium
            )
            lastSynced.targetPackage?.let {
                Text("Target: $it", style = MaterialTheme.typography.labelMedium)
            }
            Text(lastSynced.destPath, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MediaCard(
    title: String,
    selected: PickedMedia?,
    pickLabel: String,
    onPick: () -> Unit,
    onSync: () -> Unit,
    enabled: Boolean
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "Selected: ${selected?.displayName ?: selected?.uri ?: "None"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPick) { Text(pickLabel) }
                Button(onClick = onSync, enabled = enabled && selected != null) { Text("Sync") }
            }
        }
    }
}

@Composable
private fun MarkerCard(
    state: RootCamera1State,
    onToggle: (MarkerFile, Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Marker Files", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            }
            MarkerFile.entries.forEach { marker ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(marker.label, style = MaterialTheme.typography.bodyMedium)
                        Text(marker.description, style = MaterialTheme.typography.labelMedium)
                    }
                    Switch(
                        checked = state.markers[marker] == true,
                        onCheckedChange = { onToggle(marker, it) },
                        enabled = state.rootAvailable && !state.busy
                    )
                }
            }
        }
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Best-effort only.
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            return it.getString(0)
        }
    }
    return null
}

@Composable
private fun rememberMediaInfo(
    kind: MediaKind,
    uri: Uri?,
    context: Context,
    logSink: LogSink
): State<MediaInfo?> {
    val state = remember(uri) { mutableStateOf<MediaInfo?>(null) }
    LaunchedEffect(uri, kind) {
        if (uri == null) {
            state.value = null
            return@LaunchedEffect
        }
        state.value = withContext(Dispatchers.IO) {
            when (kind) {
                MediaKind.IMAGE -> loadImageInfo(context, uri, logSink)
                MediaKind.VIDEO -> loadVideoInfo(context, uri, logSink)
            }
        }
    }
    return state
}

@Composable
private fun rememberImageBitmap(context: Context, uri: Uri): ImageBitmap? {
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            decodeSampledBitmap(context, uri, 1080, 1080)
        }
    }
    return bitmap?.asImageBitmap()
}

private fun loadImageInfo(context: Context, uri: Uri, logSink: LogSink): MediaInfo? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        if (options.outWidth > 0 && options.outHeight > 0) {
            MediaInfo(options.outWidth, options.outHeight)
        } else {
            null
        }
    } catch (e: Exception) {
        logSink.log(
            com.shadowcam.core.model.LogLevel.WARN,
            "Preview",
            "Image info failed",
            mapOf("error" to (e.message ?: "unknown"))
        )
        null
    }
}

private fun loadVideoInfo(context: Context, uri: Uri, logSink: LogSink): MediaInfo? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) {
            MediaInfo(height, width)
        } else {
            MediaInfo(width, height)
        }
    } catch (e: Exception) {
        logSink.log(
            com.shadowcam.core.model.LogLevel.WARN,
            "Preview",
            "Video info failed",
            mapOf("error" to (e.message ?: "unknown"))
        )
        null
    } finally {
        retriever.release()
    }
}

private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (_: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun formatRatio(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "unknown"
    val gcd = gcd(width, height)
    val ratio = "${width / gcd}:${height / gcd}"
    val decimal = String.format(Locale.US, "%.2f", width.toFloat() / height.toFloat())
    return "$ratio ($decimal)  ${width}x${height}"
}

private fun gcd(a: Int, b: Int): Int {
    var x = a
    var y = b
    while (y != 0) {
        val temp = x % y
        x = y
        y = temp
    }
    return if (x == 0) 1 else x
}
