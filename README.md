# ShadowCam

Stealth-focused Android virtual camera app scaffold with Jetpack Compose UI, multi-module architecture, and CI to build signed APKs on GitHub Releases.

## Modules
- `app`: Compose UI, navigation, DI wiring, service stub.
- `core-model`: shared data models.
- `core-engine`: virtual camera engine contract (fake impl).
- `profiles`, `sources`, `logging`, `antidetect`: in-memory stores/monitors for profiles, media sources, logs, and anti-detection state.

## Build
```bash
./gradlew assembleDebug
# or release (signed with debug key for testing/CI)
./gradlew assembleRelease
```

APK outputs:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release (debug-key signed): `app/build/outputs/apk/release/app-release.apk`

## GitHub Releases automation
- Workflow: `.github/workflows/android-release.yml`
- Triggers: tag push matching `v*` or manual dispatch.
- Steps: checkout, setup Java 17, Gradle build (`assembleRelease`), attach `app-release.apk` to the GitHub Release and upload as an artifact.

## Local install
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Note
Engine/anti-detection are stubbed for now; UI and data flows are present to unblock distribution/testing. Integrate real camera injection logic before production use.
