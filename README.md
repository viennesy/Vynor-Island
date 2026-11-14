# Vynor Island

Dynamic Island-style overlay for Android (Java): notification pop-ups, music controls, timer/stopwatch, charging info.

## Build
- Android Studio: open the folder, sync Gradle, run on device (Android 8.0+).
- No PC: push to GitHub, the **Build APK** workflow produces the debug APK under Actions > Artifacts.

## Setup in app
Allow overlay -> allow notification access -> Start Island.

## Controls
- Tap island: open/close music controls
- Long press island: stop timer/stopwatch
- Island size and position: sliders in the app (applied live)
- "Block original notifications": originals are dismissed and shown only in the island; "Restore" re-posts them
