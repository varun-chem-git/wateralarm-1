# Water Buddy 💧

Rings every 20 minutes (from a start time you pick), opens a screen with a
"Turn off alarm" button, and then shows the message "drink water cutuuuu".
Alarms can be paused for a chosen interval (sleep, meetings...).

## Build the APK (no Android Studio needed)
1. Push this folder to a GitHub repo (branch `main`).
2. Open the repo -> **Actions** tab -> **Build APK** -> latest run.
3. Download the **water-buddy-apk** artifact (a zip containing `app-release.apk`).

For a shareable download link: `git tag v1.0 && git push origin v1.0` -> the APK shows up
under **Releases** (public repos only give anonymous links).

## Tweak
Edit `app/src/main/java/com/waterbuddy/app/Config.kt` (interval, message, ring length),
commit, and a new APK is built automatically.
