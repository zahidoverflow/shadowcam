# ShadowCam User Guide

Welcome to **ShadowCam**, a virtual camera utility for rooted Android devices. This guide will help you set up the application, configure your virtual camera sources, and troubleshoot issues.

## 📋 Prerequisites

Before installing, ensure you meet the following requirements:

1.  **Rooted Android Device:** Your phone must be rooted (Magisk, KernelSU, or APatch).
2.  **Root Permissions:** You must grant **Superuser (Root)** access to ShadowCam when prompted.
3.  **Supported ROM:** Works best on standard Android ROMs. Heavily modified skins (like MIUI/HyperOS) may require additional steps.

---

## 🚀 Getting Started

1.  **Install the APK:** Install the `app-debug.apk` on your device.
2.  **Grant Permissions:** Open the app. You will be asked for:
    *   **Root Access:** *Critical*. Allow this immediately.
    *   **Notifications:** To show service status.
    *   **Storage/Media:** To pick videos and images.
3.  **Verify Root Status:**
    *   Go to the **Home** or **Settings** tab.
    *   Ensure it says "Root Available: Yes" or similar. If not, check your root manager (Magisk/KernelSU) and ensure ShadowCam is allowed.

---

## 🎥 Using the Virtual Camera

ShadowCam works by "injecting" a video or image into the system's camera stream (specifically targeting legacy Camera1 API used by many apps).

### Step 0: Pick a Target App (Recommended)
1.  Go to the **Apps** tab.
2.  Select the target app you plan to test.
3.  If you enable **Private Dir**, ShadowCam will sync to `DCIM/Camera1/<package.name>/`.
4.  Use **Open** to launch the target app quickly.

### Optional: Set a Profile
1.  In the **Apps** tab, tap **Profile** on the app you selected.
2.  Choose a resolution, FPS, and anti-detect level.
3.  Saving a profile applies it immediately and persists it for next launch.
4.  On app start, ShadowCam auto-applies the profile for the current target app.

### Step 1: Select Your Source
1.  Navigate to the **Sources** tab.
2.  Tap **"Select Video"** or **"Select Image"**.
3.  Choose a file from your gallery.
    *   *Tip:* For videos, standard `.mp4` files work best.
    *   *Tip:* For images, ensure they match your screen aspect ratio for best results.
4.  Use the **Preview** card to verify the source and adjust how it fits the target ratio:
    *   **Source:** shows the media in its native ratio.
    *   **Target Fit:** letterboxes to the target ratio without cropping.
    *   **Target Fill:** crops to fill the target ratio.

### Step 2: Sync to System
1.  After selecting a file, you must **Sync** it.
2.  Tap the **"Sync to Camera"** button.
3.  Wait for the success message (e.g., "Video synced to Camera1").
    *   *What this does:* It copies your file to a system-protected directory that the virtual camera module reads from.

### Optional: Arm VCAM Service
1.  Open the **Home** tab.
2.  Tap **Arm VCAM** to keep the service active while testing.
    *   If it fails, check that a source is synced and Private Dir has a target app.

### Step 3: Test It
1.  Open a target app (e.g., a browser, a social media app using the camera).
2.  If successful, you should see your selected video/image instead of the real camera feed.

---

## ⚙️ Advanced Settings (Markers)

In the **Expert** or **Settings** tab, you can control the behavior of the hook using "Markers". These are switches that control how the underlying module behaves.

*   **Disable Module:** Temporarily turns off the virtual camera without uninstalling.
*   **Hide Toasts:** Stops the "ShadowCam Active" popup messages that might appear in other apps.
*   **Force Path Toast:** Debugging tool to show which file path is being accessed.
*   **Private Dir:** Forces the module to look for camera files in app-specific directories. Requires a target app selection.
*   **Enable Audio:** Allows audio from your video file to play (experimental).

---

## 🛠 Troubleshooting & Logs

If the virtual camera is not showing up or the app is crashing:

### 1. Common Fixes
*   **Restart the App:** Force close ShadowCam and reopen it.
*   **Check Root:** Open Magisk/KernelSU and confirm ShadowCam has access.
*   **Re-Sync:** Try selecting the video again and tapping "Sync" to ensure the file wasn't corrupted.

### 2. Exporting Debug Logs
If you need to share logs with a developer for support:

1.  Open ShadowCam.
2.  Navigate to the **Logs** tab.
3.  Reproduce the issue (e.g., try to sync a video if that's failing).
4.  Tap **"Export to Downloads"**.
5.  A file named `shadowcam_debug.log` will be saved to your phone's **Downloads** folder.
6.  Send this file to the developer.
    *   Logs include session id, device info, root command results, and sync metadata (paths, sizes, durations).

### 3. "Root not available" Error
*   If you denied root permission by mistake, go to your root manager app (Magisk/KernelSU), find ShadowCam, and toggle the permission **ON**. Then restart ShadowCam.

---

## ?? Camera2 Apps (Browsers/Social)
Many Chromium-based browsers and social apps use Camera2 APIs. ShadowCam still relies on an external hook module to redirect those streams. If a browser test site shows the real camera feed:
*   Verify your hook module supports Camera2.
*   Try enabling legacy/Camera1 mode in the hook module (if supported).

---

## ⚠️ Safety & Ban Risk
*   **Anti-Detect:** This app includes basic anti-detection features, but **no method is 100% safe**.
*   **Use at your own risk:** Using virtual cameras in apps that prohibit them may lead to account bans.
