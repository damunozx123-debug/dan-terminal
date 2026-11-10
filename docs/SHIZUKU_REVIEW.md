# Shizuku permission-flow review

Date: 2026-11-06. Scope: M0/M1 official Shizuku API integration.
Status: source review and patch; Android execution UNVERIFIED.
Requirements: H08, P01, P02, P04. All remain PARTIAL.

## Findings, ordered by severity

| Severity | Finding | Source change / remaining evidence |
|---|---|---|
| P1 | Permission requests used cached application state and silently returned when the service was stopped. | Refresh before assessments and requests; stopped service shows startup guidance. IPC exceptions clear the local grant. |
| P1 | Pre-v11 and permanent denial were not handled before calling the modern permission API. | Gate pre-v11; inspect shouldShowRequestPermissionRationale and show manager recovery. Neither path submits a permission request. |
| P1 | Service availability could be confused with permission readiness in the recovery UI. | Preserve SHIZUKU_PERMISSION while showing service prerequisites. The dialog closes only after both are available. The screen's recovery button requests the permission capability. |
| P1 | Result callbacks discarded denial and did not refresh the visible recovery dialog until resume or manual verification. | Consume matching results, show denial, and re-assess pending requirements when binder/grant state changes. No privileged operation is automatically resumed. |
| P2 | Listener-registration, browser-launch, and app-launch failures were hidden or uncaught. | Expose user-facing errors; use sticky binder subscription, idempotent lifecycle and cleanup of partially registered listeners. |
| P2 | Repeated taps could submit duplicate requests. | Track a pending request and disable repeated submission; explicit Verify/Refresh clears the local waiting state if no callback arrives. This does not grant permission or cancel the system dialog. |

## Changes

- ShizukuManager and ShizukuState: service, compatibility, denial and request state.
- ShizukuApi: injectable adapter around the official API; lifecycle remains application-scoped and main-thread driven.
- ShizukuAccess, AccessRecoveryManager, AccessCoordinator: refreshed assessments and ordered recovery.
- AccessRequiredDialog, ShizukuScreen, DanNavHost: actionable errors, callback-driven updates, and installed-manager launch.
- ShizukuManagerTest: 12 regression cases, supplied but NOT RUN.
- AccessRecoveryManagerTest: two additional service/permission routing cases, supplied but NOT RUN.
- core/shizuku/build.gradle.kts: JUnit and Mockito test dependencies.

## Verification and error record

First targeted command:

```sh
./gradlew :core:shizuku:testDebugUnitTest --stacktrace
```

Verbatim complete error (exit 127):

```text
/bin/bash: line 1: ./gradlew: No such file or directory
```

Minimal build-bootstrap remedy: generate the four Gradle wrapper files using
Gradle 8.10.2 in a real build environment. This was not executed successfully.
Root cause: the supplied baseline has no wrapper; this host also has no Gradle.
The source changes above do not resolve that environment blocker.

Re-run after the permission-flow patch:

```sh
./gradlew :core:shizuku:testDebugUnitTest :app:testDebugUnitTest --stacktrace
```

Real result: exit 127, with the same complete error above. No test runner started.

Executed and successful:

```sh
python3 scripts/validate_source_tree.py
git diff --check
bash -n app/src/main/assets/bootstrap/dan_bootstrap.sh
```

These establish source structure, whitespace and shell syntax only.
No Android compilation, unit-test pass, instrumented result, lint result or APK
is claimed. JDK 17.0.20 is present; Gradle, SDK manager and adb are not available
on PATH. A download probe to the official Gradle distribution endpoint failed
with curl exit 28: `curl: (28) Proxy CONNECT aborted due to timeout`.

## Remaining gates

- P1, owner=human: run a real Android build and all unit tests, then lint.
- P1, owner=human: pinned AGP 8.8.2 is below the documented API 36 minimum
  (AGP 8.9.1, which requires Gradle 8.11.1). Keep the requested pins until the
  maintainer decides the toolchain update; no suppression hides this conflict.
- P1, owner=human: on the target HyperOS device test installation, service
  stopped, wireless-debugging pairing, grant, denial, permanent denial,
  service death while the dialog is open, restart, rotation, and process death.
- P1, owner=engineering: no command broker, per-operation capability/UID check,
  persisted destructive-operation approval, or reboot receiver is implemented.
  Permission status is not proof that a particular privileged operation is safe
  or available. The SDK caches permission information; every future operation
  must handle service death and SecurityException at the operation boundary.
- P2, owner=engineering: Compose UI, package launch fallback and official
  adapter listener cleanup need Android/instrumented tests. Fake-API tests
  do not validate Binder IPC, package visibility or OEM settings navigation.
- Public source preparation does not constitute an M0/M1 release or verified tag.

## Sources consulted

- [Official Shizuku API guide](https://github.com/RikkaApps/Shizuku-API/blob/master/README.md): binder lifecycle, pre-v11 guard, denial rationale, provider protection.
- [Official Shizuku implementation](https://github.com/RikkaApps/Shizuku-API/blob/master/api/src/main/java/rikka/shizuku/Shizuku.java): sticky listener and cached permission behavior. Retrieved file blob SHA: 713a842fedf901cda069c75e94214437094806ce. Dependency resolution against the pinned binary remains unverified.
- [Android toolchain compatibility](https://developer.android.com/build/releases/about-agp): API and Gradle minimums.
