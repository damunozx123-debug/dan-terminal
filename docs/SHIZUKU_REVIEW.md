# Shizuku permission-flow review

Updated: 2026-11-10. Scope: M0/M1 official Shizuku API integration.
Status: reviewed and JVM-tested; physical Android execution UNVERIFIED.
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
- ShizukuManagerTest: 14 passing regression cases, including delayed grant and stale denial.
- AccessRecoveryManagerTest: 4 passing service/permission recovery cases.
- core/shizuku/build.gradle.kts: JUnit and Mockito test dependencies.

## Verification

The initial local attempt could not start because the baseline lacked a Gradle
wrapper. That historical environment blocker has been resolved through GitHub
Actions and a committed Gradle 8.13 wrapper, with AGP 8.13.2 and SDK 36.

[CI run 42791104590](https://github.com/damunozx123-debug/dan-terminal/actions/runs/42791104590)
passed all 14 Shizuku manager tests and all 4 access-recovery tests, alongside the
Bluetooth and permission suites (37 JVM tests total). Debug assembly succeeded.
App lint found the Shizuku provider missing from its direct classpath; the app now
declares that provider dependency. See [current status](IMPLEMENTATION_STATUS.md)
and the latest workflow for final build evidence.

Tests use a fake Shizuku API and mocked Android dependencies. They do not verify
real Binder IPC or OEM settings navigation.

## Remaining gates

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
- [Official Shizuku implementation](https://github.com/RikkaApps/Shizuku-API/blob/master/api/src/main/java/rikka/shizuku/Shizuku.java): sticky listener and cached permission behavior. Retrieved file blob SHA: 713a842fedf901cda069c75e94214437094806ce. The pinned dependency resolved and compiled in CI.
- [Android toolchain compatibility](https://developer.android.com/build/releases/about-agp): API and Gradle minimums.
