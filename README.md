# HUB — standalone Android project

Source-level Android build for HUB.

## Changes in this build
- App/launcher name changed to **HUB**.
- HUB launcher logo added from the supplied HUB identity artwork.
- Android system-bar insets are handled so the bottom navigation is not covered by the gesture/navigation bar and the top content clears the status bar.
- Calendar/reminder branding changed to HUB.
- GitHub Actions workflow builds a debug APK and uploads it as an artifact.

## Build locally
Open `Academic_Hub_GitHub_Ready/academic_hub_project` in Android Studio and build the debug APK.

## GitHub build
Push this project to GitHub, open **Actions → Build APK**, choose **Run workflow**, and download the `hub-apk` artifact after the job completes.
