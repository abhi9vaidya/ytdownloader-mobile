YTDownloader Mobile (Chaquopy + yt-dlp)
=====================================

What I changed so far
---------------------
- Wired Chaquopy use in `app/build.gradle` (pip install `yt-dlp`).
- Improved Kotlin code to use WorkManager and a `DownloadWorker` which runs Chaquopy Python downloader in the background with a foreground notification.
- Made `DownloadRepository.downloadVideo` suspendable and safer for Chaquopy callbacks.
- Fixed Compose UI state handling in `app/src/main/java/.../ui/MainScreen.kt`.
- `DownloadService` now enqueues the `DownloadWorker` (thin wrapper so the UI can start a foreground download reliably).
- Left the Python downloader at `app/src/main/python/downloader.py` (has `get_video_info` and `download_video` functions and fallback behavior when ffmpeg merging is missing).

Why I didn't produce an APK here
--------------------------------
This environment doesn't have a usable Gradle wrapper present in the repo and/or a configured Android SDK/Gradle system available to run a full `assembleDebug` here. Building Android apps requires the local Android SDK, appropriate Java toolchain, and typically the Gradle wrapper (or a system Gradle) — those must run on your machine or in CI. The repository is ready for a local/Studio build; follow the steps below.

Quick checklist to get the app running locally (recommended)
-----------------------------------------------------------
1. Open the project in Android Studio (recommended). Let it sync Gradle and install any missing SDK components.
2. Run on an emulator or a connected device.

If you prefer command-line builds, see the "Command-line build" section below.

Command-line build (Windows / PowerShell / CMD)
-----------------------------------------------
Prerequisites
- Java JDK 17+
- Android SDK (with platform 34 and an emulator/device)
- Either the Gradle wrapper (gradlew.bat) in the repo OR a system Gradle installation

Steps
1) If you don't have a gradlew wrapper file in the repo, generate it locally (requires system Gradle):

```powershell
# Run from project root (requires gradle installed on your machine)
gradle wrapper --gradle-version 8.1.1
```

2) Build the debug APK (from repo root):

```powershell
# Use wrapper if present
.\gradlew.bat assembleDebug --stacktrace --info
```

3) Install the debug APK on an emulator/device:

```powershell
.\gradlew.bat installDebug
# or
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

4) Start an emulator (if needed):

```powershell
# Example - change AVD name to your AVD
"C:\Users\<you>\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_5_API_33
```

Runtime test (once app is installed)
------------------------------------
1. Open the app on the emulator/device.
2. Grant permissions when prompted (notifications on Android 13+, storage prompt for API < 29).
3. Paste a sample Shorts URL into the text field. Example: `https://www.youtube.com/shorts/dQw4w9WgXcQ` (public; if it fails try another public short).
4. Tap "Download Video". The UI starts a foreground Service which enqueues a WorkManager job that runs Chaquopy Python to call `yt_dlp`.
5. On success, the Worker broadcasts a final `DOWNLOAD_PROGRESS` with progress 100. The saved file will be in the app's external files Download folder:

Path on device/emulator (app-private):

```
/sdcard/Android/data/com.abhi9vaidya.ytdownloader/files/Downloads
```

(Or the equivalent returned by `context.getExternalFilesDir("Downloads")`.)

Troubleshooting & common issues
-------------------------------
- Chaquopy pip install of `yt-dlp` failed at build time: ensure you have network access during Gradle build, and that Chaquopy plugin version in `build.gradle` matches the Chaquopy docs. If you see pip resolution errors, consider pinning a working `yt-dlp` version in `app/build.gradle`:

```gradle
python {
  pip {
    install "yt-dlp==2025.12.30" // example pin. Use the latest stable known-good.
  }
}
```

- Builds failing due to missing Gradle wrapper: generate the wrapper locally (see steps above) or open the project in Android Studio which can handle Gradle automatically.

- `ffmpeg` not found or merging errors: Chaquopy cannot install native ffmpeg easily. The Python code aims to select single-file mp4 formats; in some cases yt-dlp still tries to merge and fails. If you need guaranteed merging, you'll have to bundle a ffmpeg binary compatible with Android or use a remote server (outside the scope here).

Capturing logs
--------------
To debug runtime issues, use adb logcat and filter by our tags or by Chaquopy/WorkManager tags:

```powershell
adb logcat *:S Chaquopy:V WorkManager:V yt_dlp:V Downloader:V
```

Or capture the full Gradle build log to paste back for help:

```powershell
.\gradlew.bat assembleDebug --stacktrace --info > build_log.txt
```

If you hit build errors
-----------------------
- Paste the Gradle console output (or attach `build_log.txt`) and I'll iterate on fixes.
- Common fixes I can apply remotely:
  - Pin Chaquopy/plugin versions or adjust `build.gradle` if the plugin version is incompatible with the Android Gradle Plugin.
  - Fix missing imports or Kotlin compile issues (I've already validated there are no static code errors in modified files).
  - Add WorkManager observer wiring so the UI reflects Work status and progress.

Next suggested improvements I can implement now
---------------------------------------------
1. Observe `WorkManager` work status in `DownloaderViewModel` and update the Compose UI with real-time progress/state (instead of relying on broadcasts). This will require adding a small observer to map WorkInfo progress into the ViewModel state.
2. Add an "About / Disclaimer" screen accessible from the UI.
3. Improve Python `download_video` to optionally write a small sidecar JSON with the final path and metadata, or to accept an explicit filename sanitized on the Kotlin side.
4. Add simple unit tests for Kotlin ViewModel logic.

What I need from you to finish building & testing locally
--------------------------------------------------------
- Option A (recommended): Open the project in Android Studio on your machine and run it (it will handle Gradle/SDK). If build fails, paste the build log here and I'll fix the issues.
- Option B: If you prefer CLI, run the `gradle wrapper` command (if Gradle is installed), then run the assemble/install commands above and paste any errors.

If you want me to continue now, choose one:
- I will implement WorkManager → ViewModel observation and UI wiring (option 1 above).
- I will add an in-repo Gradle wrapper skeleton (not recommended—the wrapper JAR normally must be generated locally).
- I will wait for you to run a build and share errors so I can fix them.

---

If you'd like, I can now implement the WorkManager observer wiring so the UI shows progress and success/failure even without the broadcast receiver. Say "implement progress observer" and I'll add the code and run static checks.
