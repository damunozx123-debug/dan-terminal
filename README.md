# DAN Terminal

Android app with BLE/GATT tools, Shizuku access recovery, Termux integration,
and an M3.1 PTY terminal prototype. The PTY source and acceptance test are in
progress; emulator and Redmi verification are pending.

[Source and review](https://github.com/damunozx123-debug/dan-terminal/pull/1) ·
[Android builds and APK downloads](https://github.com/damunozx123-debug/dan-terminal/actions/workflows/android.yml)

## Try the app

1. Open a successful **Android CI** run for the desired commit.
2. Download **dan-terminal-debug**, extract the ZIP, and install `app-debug.apk`.
3. Open **Bluetooth / BLE / GATT**, grant the requested access and start a scan.
   Tap a connectable device, then **Connect** to browse its services.
4. For Shizuku access, use the official Shizuku app to pair/start its service,
   then return to DAN Terminal and request authorization. BLE does not require it.
5. For the Termux bootstrap, install the supported Termux build, grant
   **Run commands in Termux environment**, and enable `allow-external-apps=true`
   in Termux's properties. Run the bootstrap and check its output in Termux.

This is a debug APK for testing. Physical Redmi/HyperOS testing remains required.
The CI debug signing key can change between runners, so a later debug APK may
require uninstalling the previous build (which deletes that app's local data).

## Included

- Native BLE advertisements, RSSI, service UUIDs and manufacturer data.
- Connectable-device detection, GATT connection and service/characteristic listing.
- Scan cancellation and connection cleanup when leaving a screen or backgrounding.
- Contextual Android permission recovery for scan, connection and Termux.
- Official Shizuku API integration with service-death, denial, legacy-service,
  duplicate-request and delayed-result handling.
- Repeatable Termux bootstrap that reports package failures accurately.
- A service-owned `/system/bin/sh` PTY session rendered by Termux's terminal
  libraries. Session I/O, resize and Ctrl+C acceptance test is added but has
  not yet run; Android may end the service if it kills the app process.

## Not implemented yet

Tabs, restore after Android process death, a foreground-service guarantee,
Linux distribution profiles, SSH/Mosh UI, graphics protocols, and GATT
write/notify controls remain roadmap work. Termux and Shizuku remain external
apps. There is no automatic root access.
See [the requirement tracker](docs/REQUIREMENT_TRACKER.md) for all 95 requirements.

## Build and verification

JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.2.21,
Android SDK 36, Compose BOM 2026.02.00 and Shizuku API 13.1.5 are pinned.
The committed Gradle wrapper verifies its distribution checksum.

```sh
python3 scripts/validate_source_tree.py
python3 scripts/test_bootstrap.py
./gradlew :app:assembleDebug testDebugUnitTest lintDebug :app:connectedDebugAndroidTest --continue --stacktrace
```

CI runs these checks and uploads the APK only after assembly, unit tests and lint
succeed. Reports remain available when a build fails. The APK is at
`app/build/outputs/apk/debug/app-debug.apk` when building locally.

See [current implementation status](docs/IMPLEMENTATION_STATUS.md) and the
[Shizuku review](docs/SHIZUKU_REVIEW.md). The original
[M0/M1 intake report](docs/MILESTONE_REPORT_M0_M1.md) records the earlier baseline.

## Modules and license

`app` contains the Compose screens; `core/security` handles access recovery and
Keystore storage; `core/shizuku` adapts the official API; `core/bluetooth` owns BLE
and GATT sessions; `feature/termux` sends the bootstrap to Termux.

Original project code is MIT licensed. See [LICENSE](LICENSE),
[third-party notices](THIRD_PARTY_NOTICES.md), and [contribution notes](CONTRIBUTING.md).
