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
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.2.21
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

## Shizuku permission review

The source now checks service availability before requesting permission,
handles unsupported services and denial, and keeps the recovery dialog in sync
with binder/permission callbacks. Tests are supplied but have not run.
Read [the review and device test matrix](docs/SHIZUKU_REVIEW.md).

Shizuku integration is a core app feature. This M1 implementation uses the
separately installed official Shizuku service. The integrated service in the
final single APK remains future work (H08/P08).

## Build prerequisites

Generate the missing wrapper with Gradle 8.13 installed:

```sh
./scripts/bootstrap_gradle_wrapper.sh
```

CI is configured to bootstrap it in an isolated project and upload the wrapper
with build evidence. A maintainer must review and commit the generated wrapper.
The workflow itself has not been executed here.

AGP 8.13.2, Gradle 8.13 and JDK 17 support compile SDK 36. The original
8.8.2/8.10.2 conflict is resolved in the current configuration. Historical
review documents preserve the original findings; CI results establish execution.

## Open source and contributions

Original project source is available under the [MIT licence](LICENSE).
See [third-party notices](THIRD_PARTY_NOTICES.md) and [contribution rules](CONTRIBUTING.md).
Public source availability does not imply a verified APK release.

Open pull requests against `develop`; require a human approval before merging.
No release tag is approved by this source publication.
