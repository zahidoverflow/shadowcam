# ShadowCam

Android virtual camera controller with root Camera1 sync, a Compose-first UI, and a clean multi-module architecture.

[![Release](https://img.shields.io/github/v/release/zahidoverflow/shadowcam?include_prereleases)](https://github.com/zahidoverflow/shadowcam/releases)

## Highlights
- Root-first workflow: manage Camera1 files and marker toggles used by LSPosed/Xposed camera modules.
- Focused, dark UI with clear status feedback and log visibility.
- Persistent source picks (video and image) for quick re-sync.
- Modular codebase ready for a real virtual camera engine.

## What This Is (and Is Not)
- This app manages Camera1 files and marker flags on rooted devices.
- It does not implement a camera injection engine yet. The engine layer is stubbed.
- It is not intended for bypassing security, identity checks, or app protections. Use responsibly and lawfully.

## Root Camera1 Workflow
ShadowCam writes to `DCIM/Camera1` using root and follows common module conventions:
- Video replacement: `virtual.mp4`
- Photo replacement: `1000.bmp` (any image format renamed to .bmp)
- Marker files:
  - `disable.jpg` (disable module)
  - `no_toast.jpg` (hide module toasts)
  - `force_show.jpg` (force directory toast)
  - `private_dir.jpg` (per-app Camera1 directory)
  - `no-silent.jpg` (enable audio if supported)

Tip: Match the replacement media resolution to the target app camera resolution to avoid black screens or distortion.

## Quick Start (Root)
1. Root your device and install an LSPosed/Xposed camera module (not included here).
2. Build and install ShadowCam.
3. Open Sources -> pick a video/image -> Sync to Camera1.
4. Toggle marker files as needed.
5. Open the target app camera.

## Modules
- `app`: Compose UI, navigation, DI, service wiring.
- `core-model`: shared models.
- `core-engine`: virtual camera engine contract (stub).
- `profiles`, `sources`, `logging`, `antidetect`: in-memory stores/monitors.

## Build
Requirements: JDK 17, Android SDK, Gradle.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

APK outputs:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release (debug-key signed): `app/build/outputs/apk/release/app-release.apk`

## Releases
GitHub Actions builds a release APK on tag push matching `v*`.
Workflow: `.github/workflows/android-release.yml`

## Roadmap
- Real virtual camera engine (root + non-root paths).
- Source preview, trimming/cropping, and per-app profiles.
- Expanded logs/export and permission rationale flows.

## Contributing
Issues and PRs are welcome. Keep changes scoped and document any device-specific behavior.

## License
No license specified yet.