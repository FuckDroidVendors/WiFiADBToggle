# WiFi ADB Toggle (Quick Settings)

Minimal Android app that toggles wireless ADB from a Quick Settings tile or notification. The tile shows the current IP address and port when available.

## Requirements
- Android 4.2+ (API 17) for the app; Quick Settings tile is available on Android 7.0+.
- Root (Magisk works).

## How It Works
The toggle runs:
- Enable: `setprop service.adb.tcp.port 5555; stop adbd; start adbd`
- Disable: `setprop service.adb.tcp.port -1; stop adbd; start adbd`

This requires root because it restarts `adbd` and changes system properties.

## Build
1. Open the project in Android Studio.
2. Sync Gradle.
3. Run on a device.

Gradle wrapper scripts are included, but the `gradle/wrapper/gradle-wrapper.jar` file is not generated in this environment. On your machine, run `gradle wrapper` once to generate it, or let Android Studio create/update the wrapper.
You can also run `scripts/gen-wrapper.sh` to generate the wrapper JAR if Gradle is installed.

## Use
1. Launch the app once.
2. Add the `WiFi ADB` tile to Quick Settings (Android 7.0+), or enable the persistent notification.
3. Tap the tile/notification to toggle wireless ADB.

## Notes
- The tile subtitle shows `IP:5555` when it can detect a non-loopback IPv4 address.
- If the tile says `no IP`, connect to Wi-Fi or check network availability.
- Root path uses `su -c` and works with Magisk or other superuser managers.

## Files
- Tile service: `app/src/main/java/fuck/wifiadbtoggle/droidvendorssuck/AdbTileService.kt`
- Root runner: `app/src/main/java/fuck/wifiadbtoggle/droidvendorssuck/ShellRunner.kt`
- Toggle logic: `app/src/main/java/fuck/wifiadbtoggle/droidvendorssuck/AdbWifiController.kt`
