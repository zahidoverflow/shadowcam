# Keep Compose preview annotations.
-keep class androidx.compose.ui.tooling.preview.PreviewParameterProvider

# LSPosed/Xposed legacy module entrypoint must remain reachable.
-keep class com.shadowcam.xposed.** { *; }
