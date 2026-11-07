# DAN Terminal

M0/M1 Android source baseline for a modular terminal and device-integration app.

## Modules

- app — Compose entry point and feature screens
- core/security — permission and capability recovery, Keystore secret storage
- core/shizuku — Shizuku binder and permission status
- core/bluetooth — BLE scan and GATT connection/discovery
- feature/termux — Termux RUN_COMMAND bridge and bootstrap

## Pinned toolchain

- JDK 17
- Gradle 8.10.2
- Android Gradle Plugin 8.8.2
- Kotlin 2.1.10
- Android API 36
- Compose BOM 2026.02.00
- Shizuku API 13.1.5

## Build status

This tree has not been compiled, tested, linted, or packaged. Gradle wrapper
files are not present. See docs/MILESTONE_REPORT_M0_M1.md and
docs/REQUIREMENT_TRACKER.md before treating any feature as verified.

Once the wrapper and Android SDK API 36 are available:

```sh
./gradlew clean :app:assembleDebug --stacktrace --warning-mode all
./gradlew testDebugUnitTest --stacktrace
./gradlew lintDebug --stacktrace
```

Termux RUN_COMMAND source construction uses an executable path and a separate
argument array. Delivery still requires a real device test, including
RUN_COMMAND permission, allow-external-apps, and HyperOS background behavior.
