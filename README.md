# YT Shorts Downloader (Android)

Quick steps to build & test:

1. Open project in Android Studio (Arctic Fox or newer). Ensure Chaquopy plugin version in Gradle matches installed Chaquopy plugin.
2. Build the project. Chaquopy will install Python packages (yt-dlp, ffmpeg-python).
3. Note: A native ffmpeg binary may be required for merging. Either bundle a static ffmpeg binary in app assets and pass its path to yt_tasks.download(ffmpeg_path="..."), or rely on devices with ffmpeg preinstalled.
4. Run on emulator/device (API 24+). Grant POST_NOTIFICATIONS on Android 13+.
5. Paste a public Shorts URL like `https://www.youtube.com/shorts/VIDEOID` and press Preview then Download.
6. Monitor downloads in Downloads folder.

Disclaimer: For personal/educational use only. Respect YouTube Terms of Service.

Status: not fully finished (requires local build/test)
- You still need to: run Gradle sync to allow Chaquopy to pip-install packages; bundle/provide ffmpeg binary for reliable merging; run the app on a device/emulator to verify end-to-end behavior.  
- If you run into build or runtime errors, paste the logs here and I will provide focused fixes (e.g., Chaquopy configuration, ffmpeg packaging, Python-to-Kotlin progress callback bridge, or UI polish).

