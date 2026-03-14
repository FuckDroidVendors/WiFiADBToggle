# WiFi ADB Toggle (Quick Settings)

Minimal Android app that toggles wireless ADB from a Quick Settings tile. The tile shows the current IP address and port when available.

## Requirements
- Android 7.0+ (API 24)
- One of the following:
  - Root (Magisk works)
  - Shizuku running with the app granted permission

## How It Works
The toggle runs:
- Enable: `setprop service.adb.tcp.port 5555; stop adbd; start adbd`
- Disable: `setprop service.adb.tcp.port -1; stop adbd; start adbd`

This requires root or Shizuku because it restarts `adbd` and changes system properties.
Shizuku execution uses a UserService (AIDL binder) instead of the deprecated `newProcess` API.

## Build
1. Open the project in Android Studio.
2. Sync Gradle.
3. Run on a device.

Gradle wrapper scripts are included, but the `gradle/wrapper/gradle-wrapper.jar` file is not generated in this environment. On your machine, run `gradle wrapper` once to generate it, or let Android Studio create/update the wrapper.
You can also run `scripts/gen-wrapper.sh` to generate the wrapper JAR if Gradle is installed.

## Use
1. Launch the app once.
2. If using Shizuku, tap `Request Shizuku Permission`.
3. Add the `WiFi ADB` tile to Quick Settings.
4. Tap the tile to toggle wireless ADB.

## Notes
- The tile subtitle shows `IP:5555` when it can detect a non-loopback IPv4 address.
- If the tile says `no IP`, connect to Wi-Fi or check network availability.
- Root path uses `su -c` and works with Magisk or other superuser managers.
- Shizuku must be running; the app only requests permission and does not start it.
- Shizuku requires a `ShizukuProvider` entry in the manifest; this project includes it.
- The UserService class implements an AIDL `Stub`, and can optionally have a `Context` constructor for Shizuku v13+; this project includes both.

## Files
- Tile service: `app/src/main/java/com/example/wifitoggle/AdbTileService.kt`
- Root/Shizuku runner: `app/src/main/java/com/example/wifitoggle/ShellRunner.kt`
- Toggle logic: `app/src/main/java/com/example/wifitoggle/AdbWifiController.kt`
