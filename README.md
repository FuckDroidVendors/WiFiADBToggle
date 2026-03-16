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

To build everything from the command line:
```
./gradlew build
```

### Flavors
Build flavors are available to reduce footprint and features:
- `full`: Quick Settings tile, persistent status notification (toggleable), connection list notification (toggleable), media buttons, schedule UI, monitoring rules.
- `full21`: Same as `full`, but minSdk 21 and uses framework `MediaSession` (no `androidx.media`).
- `notify`: persistent status notification only (forced on, no UI/config or settings).
- `notifyconn`: persistent status notification (toggleable) + connection list notification (toggleable).
- `tile`: Quick Settings tile only (no notifications, no UI/config).
- `basic`: Quick Settings tile + persistent status notification (toggleable). No schedule, media, or connection list.
- `headless`: No UI, no notifications, no tile. Launch once to enable WiFi ADB and exit.

Select a flavor interactively or via `-Pmode=notify` (add `-PnoPrompt=true` to skip the prompt).
If you pass `-PdeviceApi=23` and no mode is set, it defaults to `notify` (no tile on old Android).

#### Recommended By Android Version
- Android 4.2–6.x (API 17–23): `notify` (tile is not available).
- Android 7.0+ (API 24+):
  - `tile` if you only want QS control with no notifications.
  - `basic` for QS tile + status notification.
  - `full` if you need schedule/media/monitoring features.
  - `full21` if you target API 21+ and want to avoid `androidx.media`.
  - `headless` if you want a one-shot launcher that just enables WiFi ADB.

#### Build Commands
Release APKs:
1. `./gradlew assembleFullRelease -Pmode=full -PnoPrompt=true`
2. `./gradlew assembleFull21Release -Pmode=full21 -PnoPrompt=true`
3. `./gradlew assembleNotifyRelease -Pmode=notify -PnoPrompt=true`
4. `./gradlew assembleNotifyconnRelease -Pmode=notifyconn -PnoPrompt=true`
5. `./gradlew assembleTileRelease -Pmode=tile -PnoPrompt=true`
6. `./gradlew assembleBasicRelease -Pmode=basic -PnoPrompt=true`
7. `./gradlew assembleHeadlessRelease -Pmode=headless -PnoPrompt=true`

Debug APKs:
1. `./gradlew assembleFullDebug -Pmode=full -PnoPrompt=true`
2. `./gradlew assembleFull21Debug -Pmode=full21 -PnoPrompt=true`
3. `./gradlew assembleNotifyDebug -Pmode=notify -PnoPrompt=true`
4. `./gradlew assembleNotifyconnDebug -Pmode=notifyconn -PnoPrompt=true`
5. `./gradlew assembleTileDebug -Pmode=tile -PnoPrompt=true`
6. `./gradlew assembleBasicDebug -Pmode=basic -PnoPrompt=true`
7. `./gradlew assembleHeadlessDebug -Pmode=headless -PnoPrompt=true`

#### Compile-Time Defaults (No UI Flavors)
For `notify` and `headless` you can set defaults at build time:
- ADB port: `-PadbPort=5555`
- Auto-enable on boot (headless): `-Pautoboot=true`
- Locales (default: `en`, use `all` to disable filtering): `-PresLocale=en`
- Densities (default for `notify`/`headless`: `nodpi`): `-PresDensities=nodpi`

Examples:
```
./gradlew assembleHeadlessRelease -Pmode=headless -PnoPrompt=true -PadbPort=5555 -Pautoboot=true
./gradlew assembleNotifyRelease -Pmode=notify -PnoPrompt=true -PadbPort=5555
./gradlew assembleNotifyRelease -Pmode=notify -PnoPrompt=true -PresLocale=en -PresDensities=nodpi
```

## Use
1. Launch the app once.
2. Add the `WiFi ADB` tile to Quick Settings (Android 7.0+), or enable the persistent notification.
3. Tap the tile/notification to toggle wireless ADB.

## Notes
- The tile subtitle shows `IP:5555` (IPv4) or `[IPv6]:5555` when it can detect a non-loopback address.
- If the tile says `no IP`, connect to Wi-Fi or check network availability.
- Optional ADB connection notification shows active client IPs when enabled (requires root).
- ADB port is configurable in-app; the UI will show the selected port.
- Root path uses `su -c` and works with Magisk or other superuser managers.

## Files
- Tile service: `app/src/main/java/fuck/wifiadbtoggle/droidvendorssuck/AdbTileService.kt`
- Root runner: `app/src/main/java/fuck/wifiadbtoggle/droidvendorssuck/ShellRunner.kt`
- Toggle logic: `app/src/main/java/fuck/wifiadbtoggle/droidvendorssuck/AdbWifiController.kt`
