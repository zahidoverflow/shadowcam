# ShadowCam Build Plan

## Product Pillars
- Stealth-first: AMOLED dark, minimal chrome, clear root/non-root split with sandbox cues.
- Precision control: per-app profiles, deterministic spoofing, explicit metadata overrides.
- Trust and safety: permission rationale, risk labels, exportable logs, reversible toggles.
- Performance: low-latency virtual stream, resource-aware on lower-end devices.

## Primary Screens & Key Components
- Home Dashboard: status cards (VCAM, App Hooked, Source Loaded), global toggle, quick inject CTA, Expert Mode pill, Logs chip.
- App Selector: searchable grid, favorites star, filter chips, long-press quick profile (resolution/FPS/EXIF preset), status accent color (cyan active, purple custom, amber warning).
- Source Manager: tabs (Images, Video, Live, RTSP); each item shows spoof mode (loop/single), duration, metadata badge; quick actions (Set Default, Assign to App, Test Feed); add modal with picker + preview + crop/trim.
- Expert Mode: sections for Sensor Spoof (res/FPS matrix, noise profile), EXIF/Metadata (GPS/model/timestamp), Anti-Detection (randomize intervals, sandbox-aware switch, camera2/legacy toggle), Compatibility (root hooks, non-root service); risk pill Low/Med/High on each group.
- Logs & Console: scrollback with level chips (INFO/CMD/WARN), export/copy/clear, per-app filter, mini latency/FPS sparkline.
- Permissions & Trust: overlay explaining hooks, per-permission rationale, “Simulate Detection” test view showing what target apps would read.

## Architecture Blueprint
- Tech stack: Android, Kotlin, Jetpack Compose UI, CameraX abstractions where possible, native bridge (JNI) for injection hooks, WorkManager for background service upkeep, protobuf/JSON for profile serialization.
- Modules (Gradle):
  - `app`: Compose UI, navigation, DI wiring, permission flows.
  - `core-model`: data models, validation, serialization.
  - `core-engine`: virtual camera pipeline, media routing, metadata injector.
  - `profiles`: per-app profile store, defaults, migrations.
  - `sources`: source ingestion (gallery picker, file access, RTSP, screen capture), caching.
  - `logging`: structured logs, export, log filters, in-app console feed.
  - `antidetect`: heuristics, sandbox detection, randomized timing controls.
- Services & Flows:
  - Virtual Camera Service: maintains injection state, exposes binder interface for UI; handles ON/OFF, attaches selected source, and surfaces live stats (fps/latency/uptime).
  - Source Manager: validates media, normalizes frame rate/resolution, handles loop vs single-shot, and exposes test feed.
  - Profile Manager: maps app package -> profile (resolution/FPS/EXIF preset/anti-detect level/source binding), applies profile on app foreground or request.
  - Metadata Injector: overrides EXIF (where applicable) and camera characteristics (reported res/FPS/sensor props) with bounds checking.
  - Anti-Detection: toggles randomized timing, sandbox-aware mode (flags for emulator/VM signals), camera2/legacy mode switch; exposes risk levels for UI badges.
  - Logging & Export: ring buffer + persistent store, level filters, export to file/share sheet; redact sensitive fields by default.
- Root vs Non-Root Strategy:
  - Non-Root: virtual camera via app-scoped service + CameraX/MediaProjection for feed; relies on per-app camera provider selection where possible; requires user-set default camera in supported apps; limited metadata spoof depth.
  - Root: optional native injector (JNI) hooking camera provider to present virtual device; deeper metadata overrides; guarded behind explicit consent + warning.
- Permissions: CAMERA, READ_MEDIA_* / READ_EXTERNAL_STORAGE (legacy), RECORD_AUDIO (if passthrough), FOREGROUND_SERVICE, BIND_ACCESSIBILITY_SERVICE (optional for automation), MANAGE_EXTERNAL_STORAGE avoided unless necessary; clear rationale screens.

## Anti-Detection Considerations
- Mode banner shows current surface (emulator/VM signals vs physical).
- Randomize timing jitter only in High-risk mode; default deterministic.
- Safe defaults prevent aggressive sensor spoofing unless Expert Mode enabled.
- Simulate Detection tool surfaces what target app can read (device model, API level, camera ID list, reported caps).

## Testing & Telemetry
- Unit: profile validation, metadata bounds, source normalization.
- Instrumentation: Compose UI navigation, permission gating, foreground/background transitions.
- Integration: virtual feed loop test, app-switch profile swap, RTSP ingest latency, log export.
- Load soak: long-running virtual feed at various FPS/res on mid-range device.
- Observability: structured logs with session id; optional anonymized metrics off by default.

## Build Backlog (milestones)
- M0 Foundations (week 1–2): scaffold modules, DI, navigation skeleton, theme, design tokens (colors/typography), state containers, logging stub.
- M1 Virtual Camera Core: virtual camera service (non-root path), source ingest (image/video loop), test feed screen, VCAM ON/OFF + stats, basic profile store.
- M2 App Profiles: app selector grid, favorites, profile quick menu, per-app apply on foreground, defaults/migrations.
- M3 Metadata & Anti-Detection: EXIF overrides, sensor spoof presets, anti-detect toggles (jitter, sandbox-aware, legacy/camera2), risk pills.
- M4 Sources Expansion: RTSP ingest, live screen capture, trim/crop tools, assign-to-app flow, "Set Default" + validation.
- M5 Logs & Trust: console view, export/copy, permission rationale overlay, Simulate Detection flow, warnings for root hooks.
- M6 Polish & QA: haptics/micro-animations, accessibility review, long-run soak tests, battery/perf tuning, store assets (screens, icon).

## Future Enhancements (post-initial release)
- Device validation matrix: non-root and KernelSU/root paths on representative devices (e.g., vayu), with log-captured outcomes.
- Root injector hardening: SELinux allowlist/module guidance, ABI checks, camera provider hook fallback for vendor differences.
- Source tooling: RTSP ingest with latency budget, trim/crop for media, and "Set Default"/assign-to-app validation UX.
- Anti-detection: sandbox-aware mode banner, jitter presets by risk pill, Camera2/legacy toggle verification per device.
- Trust & export: Simulate Detection view, log redaction defaults, one-tap log export/share with session metadata.
- Test coverage: unit for profile validation/engine state, instrumentation smoke (launch -> toggle VCAM -> apply profile), soak tests for long-running feed.

## Current Implementation Notes
- Target app selection with installed app search and per-app profile editor persisted in DataStore.
- Root Camera1 sync respects private_dir + target app, tracks last synced media, and exposes active sync path in UI.
- Foreground VCAM service start/stop wired to the engine; injection still external.
- Profiles auto-apply when a target app is loaded at startup.
- Target app launch shortcut added in the Apps screen.
- Sources screen includes in-app preview with ratio hints and Source/Target Fit/Fill modes.
- Logs now include session/device metadata and root command context for debugging.

## To-dos.md Status (root-first pass)
### Done
- Root Camera1 integration for LSPosed/Xposed-style modules: root availability check, sync `virtual.mp4` and `1000.bmp`, marker toggles (`disable.jpg`, `no_toast.jpg`, `force_show.jpg`, `private_dir.jpg`, `no-silent.jpg`), persisted picks, Sources UI wiring.
- Target app selection and per-app profile editing/persistence.
- Root private-dir sync path targeting with last-synced status.
- Structured log metadata (session/device/root command context).
- VCAM service start/stop wired to engine state.

### Not Done Yet
- Live broadcast & video management (RTMP, playlists, aspect/resolution handling).
- Virtual camera injection for apps/browsers (no actual camera hook or browser optimizations).
- Decoding/rendering controls (HW/SW decode toggle, flip/rotation, A/V sync switch, manual alignment).
- Advanced anti-detect and system spoofing features (fingerprint/ID spoofing, root hiding, NFC, multi-account).
- Deepfake/replacement features, network emulation/proxy tooling, and PC synchronization.
