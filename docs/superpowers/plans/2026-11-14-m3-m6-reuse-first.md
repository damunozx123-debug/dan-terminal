# DAN Terminal M3–M6 Reuse-First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the M3 interactive terminal, add the first Linux and remote foundations for M4, reuse existing diagnostics/device code for M5, and harden the signed release path for M6.

**Architecture:** Keep the app-owned PTY and Termux terminal renderer. Extend DAN Terminal's existing navigation, BLE/GATT, Shizuku and Obtainium modules. Port only the MIT-licensed Net Intel connectivity model, history and report code that passes DAN Terminal tests. Resolve the M4 PRoot/PTY boundary in a focused decision task before adding a Linux runtime.

**Tech Stack:** Kotlin, Jetpack Compose, Android services, Termux terminal-view 0.118.0, Gradle, Android 16 instrumented tests, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-14-m3-m6-reuse-first-design.md`

## Global Constraints

- Keep the terminal transport as an app-owned PTY; Termux RUN_COMMAND remains one-shot and is not a PTY.
- Android 16 emulator pass is not Redmi/HyperOS device acceptance.
- Preserve MIT notices for substantial copied app code; retain separate third-party notices.
- Keep DAN Terminal signing credentials unique; never copy sibling repository secret values.
- Keep blocking DNS/TCP work off the UI thread.
- Require explicit approval before every GATT write; a denial must produce zero writes.
- Claim Kitty/Sixel support only after renderer tests pass.

## Review Focus

- Setup-complete and first-launch routing: saved setup state must be correct after process recreation; test route selection with both preference values.
- PTY key events and software keyboard: controls must reach the active session; instrument Android key events and verify shell output.
- PTY size after rotation/insets: terminal rows and columns must follow the visible viewport; assert `stty size` after relayout.
- Service/process lifecycle: screen navigation keeps the session while the process lives; process death ends it and reopening creates a clearly fresh session; instrument both paths.
- Network denial, timeout and export injection: missing permission or failed probes remain unscored and CSV values are escaped; test with an injected fake transport and malicious field values.

---

## File Map

- `app/src/main/java/com/dan/terminal/feature/termux/LocalShellScreen.kt` — terminal view, focus, keyboard bridge, session tabs and user-visible session errors.
- `app/src/main/java/com/dan/terminal/feature/termux/PtyTerminalService.kt` — owns the PTY sessions and their lifecycle.
- `app/src/main/java/com/dan/terminal/navigation/DanNavHost.kt` — first-run setup route and later default terminal route.
- `app/src/androidTest/java/com/dan/terminal/terminal/` — PTY and rendered-screen acceptance tests.
- `core/network/` — new platform adapter, measurement model and history/export logic ported from Net Intel; one module owns diagnostic behavior.
- `app/src/main/java/com/dan/terminal/feature/network/` — user-triggered diagnostics screen and explicit consent/results UI.
- `core/bluetooth/src/main/java/com/dan/terminal/core/bluetooth/GattInspector.kt` — extend existing connection owner with write/notify callbacks and approval-safe operations.
- `app/src/main/java/com/dan/terminal/feature/bluetooth/GattScreen.kt` — render readable values and approval affordances.
- `.github/workflows/android.yml` — keep the accelerated Android 16 emulator job and APK artifact gate.
- `.github/workflows/obtainium-prerelease.yml` — add signature/version/artifact evidence gates without changing signing identity.
- `docs/IMPLEMENTATION_STATUS.md`, `docs/REQUIREMENT_TRACKER.md`, and `THIRD_PARTY_NOTICES.md` — update evidence and notices only after checks pass.

## Task 1: Exercise the actual terminal screen and default route

**Files:**
- Modify: `app/src/main/java/com/dan/terminal/navigation/DanNavHost.kt`
- Modify: `app/src/main/java/com/dan/terminal/feature/termux/LocalShellScreen.kt`
- Test: `app/src/androidTest/java/com/dan/terminal/terminal/TerminalStartupTest.kt`

**Interfaces:**
- Preserve `LocalShellScreen(onSettings: () -> Unit, onLegacyConsole: () -> Unit)`.
- Test the existing `dan_terminal_setup/setup_completed` preference contract; do not introduce a second setup flag.

- [ ] Add `TerminalStartupTest.firstLaunchSetupCompletionOpensTerminalAndNextLaunchStartsThere` covering the initial `home` route, completion action, persisted flag and subsequent `shell` route.
- [ ] Run `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dan.terminal.terminal.TerminalStartupTest` on Android 16 and confirm it fails if the route or saved preference is wrong.
- [ ] Add only the accessibility/test hooks needed to locate the setup-complete and terminal controls; retain the existing route behavior.
- [ ] Run the focused Android 16 test and confirm both route states pass.

## Task 2: Test terminal input, resize, scrollback and clipboard through the view

**Files:**
- Modify: `app/src/main/java/com/dan/terminal/feature/termux/LocalShellScreen.kt`
- Modify: `app/src/main/java/com/dan/terminal/feature/termux/PtyTerminalService.kt`
- Test: `app/src/androidTest/java/com/dan/terminal/terminal/TerminalViewAcceptanceTest.kt`

**Interfaces:**
- Keep the existing service binder and `TerminalSession`; do not add a new shell transport.
- Add `TerminalTestTags` only if Android instrumentation cannot reliably find the AndroidView through its Compose host.

- [ ] Add `TerminalViewAcceptanceTest.typedCommandAndControlKeysReachTheLiveShell` verifying text entry, Enter, Ctrl+C, Ctrl+D, arrow, Tab and Esc through Android input events, not direct `session.write()` calls.
- [ ] Add `TerminalViewAcceptanceTest.interactiveSystemEditorAndTopReceiveInput` that checks for an installed `vi`/`vim` and `top`, exercises each available interactive tool, and records missing commands as an M4 profile dependency rather than a passing terminal feature.
- [ ] Add `TerminalViewAcceptanceTest.viewportResizeUpdatesPtyRowsAndColumns` verifying `stty size` after rotation and visible-inset changes.
- [ ] Add `TerminalViewAcceptanceTest.scrollbackCopyPasteAndUnicodeRemainAvailable` verifying scrollback, clipboard round trip and a Unicode marker in rendered output.
- [ ] Run `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dan.terminal.terminal.TerminalViewAcceptanceTest` on the Android 16 emulator and capture failure output before implementation.
- [ ] Implement only the missing focus/input/resize/clipboard behavior in `TerminalView` hosting and service integration; do not replace the terminal renderer.
- [ ] Re-run the three instrumented tests and full `connectedDebugAndroidTest`.

## Task 3: Add named PTY sessions and tabs with documented recovery

**Files:**
- Modify: `app/src/main/java/com/dan/terminal/feature/termux/PtyTerminalService.kt`
- Modify: `app/src/main/java/com/dan/terminal/feature/termux/LocalShellScreen.kt`
- Test: `app/src/androidTest/java/com/dan/terminal/terminal/PtySessionRegistryTest.kt`

**Interfaces:**
- Add immutable `PtySessionInfo(id: String, name: String, isRunning: Boolean)`.
- Add binder methods `listSessions(): List<PtySessionInfo>`, `createSession(name: String): String`, `session(id: String): TerminalSession?`, and `closeSession(id: String): Boolean`.
- A process restart creates fresh shell sessions from saved names; it does not claim to restore the previous shell process or its memory.

- [ ] Add tests for unique IDs, duplicate-name validation, active-session selection, closing the active session, and recreating a fresh session after service recreation.
- [ ] Run `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dan.terminal.terminal.PtySessionRegistryTest` and confirm tests fail against the single-session service.
- [ ] Implement the smallest registry in `PtyTerminalService`; persist only session IDs/names and current selection, not process state or command history.
- [ ] Add a compact tab strip with create, select and close controls; keep setup/tools reachable from the terminal header.
- [ ] Run the PTY service and tab tests; document foreground/background and process-death behavior in `docs/M3_1_PTY_ARCHITECTURE.md`.

## Task 4: Decide the M4 Linux runtime and interactive PTY boundary

**Files:**
- Create: `docs/M4_RUNTIME_DECISION.md`
- Review: `scripts/proot-sales-live.sh` and `scripts/termux-sales-pilot.sh` in `net-intel-app-v9`.
- Review: Android app sandbox, Termux public APIs and licensing for candidate runtime packages.

**Interfaces:**
- No production interface or dependency is added in this task.
- Decision output must name one first profile (Debian recommended), the package/runtime source, storage location, update/remove behavior and PTY launch path.

- [ ] Compare app-private PRoot runtime versus a Termux companion runtime against ARM64 support, licence, package integrity, initial download/storage size, update/remove behavior and ability to attach the app-owned PTY.
- [ ] Verify from source/API evidence whether any supported Termux integration exposes a bidirectional PTY; explicitly exclude RUN_COMMAND if it only returns one-shot results.
- [ ] Write `docs/M4_RUNTIME_DECISION.md` with the chosen architecture, rejected alternatives, dependency notices and a test plan. If no candidate meets all constraints, stop before adding a production runtime.
- [ ] Run source/licence checks for the selected package before any dependency is added.

## Task 5: Build the first Linux profile and remote shell foundations

**Files:**
- Create or modify files as specified in the approved `docs/M4_RUNTIME_DECISION.md`.
- Test: module-level profile lifecycle tests and Android 16 PTY instrumentation tests.

**Interfaces:**
- Linux profile launch must return an interactive process/PTY attached to the existing terminal session API.
- Profile APIs must expose `install`, `verify`, `upgrade` and `remove` outcomes without hiding partial failures.

- [ ] Add tests for first install, interrupted download, checksum mismatch, failed upgrade rollback, re-verify and remove confirmation using a local fake package source.
- [ ] Add tests for interactive `cd`/environment persistence and `stty size` inside the selected profile.
- [ ] Implement only the first approved profile; carry over explicit consent and localhost defaults from Net Intel scripts, not their web server or application-specific code.
- [ ] Use the profile's maintained OpenSSH client for the first SSH/SFTP path if the runtime decision approves it; require host-key verification and keep private keys in app-controlled storage.
- [ ] Test unknown, accepted and changed host keys, interactive input/output, SFTP path scoping and credential-free logs against an ephemeral test SSH server.
- [ ] After the first SSH path passes, create separate M4 subtasks and acceptance checks for Mosh and remote Docker; do not mark M4 complete until the issue's M4 scope is covered.

## Task 6: Port Net Intel connectivity measurement, history and exports

**Files:**
- Create: `core/network/build.gradle.kts`
- Create: `core/network/src/main/AndroidManifest.xml`
- Create: `core/network/src/main/java/com/dan/terminal/core/network/ConnectivitySample.kt`
- Create: `core/network/src/main/java/com/dan/terminal/core/network/ConnectivityProbe.kt`
- Create: `core/network/src/main/java/com/dan/terminal/core/network/NetworkHistoryStore.kt`
- Create: `core/network/src/main/java/com/dan/terminal/core/network/ConnectivityReportExporter.kt`
- Create: `core/network/src/test/java/com/dan/terminal/core/network/ConnectivityProbeTest.kt`
- Create: `app/src/main/java/com/dan/terminal/feature/network/NetworkDiagnosticsScreen.kt`
- Modify: `settings.gradle.kts`, `app/build.gradle.kts`, `app/src/main/java/com/dan/terminal/navigation/DanNavHost.kt`.

**Interfaces:**
- `data class ConnectivitySample(source: String, timestampUtc: String, network: String, tcpMs: Double?, dnsMs: Double?, failPct: Double?, jitterMs: Double?, wifiRssiDbm: Int?, score: Int?, cause: String, notes: String)`.
- `interface ConnectivityProbe { suspend fun measure(): ConnectivitySample }`.
- `interface NetworkHistoryStore { suspend fun save(sample: ConnectivitySample); suspend fun load(): List<ConnectivitySample>; suspend fun clear() }`.
- `interface ConnectivityReportExporter { suspend fun writeCsv(sample: ConnectivitySample, output: OutputStream); suspend fun writePdf(sample: ConnectivitySample, output: OutputStream) }`.

- [ ] Add fake-transport tests for no permission, no active network, DNS failure, TCP timeout, partial failure and valid measurement; assert unavailable data remains null and score is null when insufficient.
- [ ] Add tests for history order/limit and CSV formula escaping/quotes, plus PDF export failure cleanup.
- [ ] Run `./gradlew :core:network:testDebugUnitTest` and verify probe/history/CSV tests fail before implementation.
- [ ] Add PDF-specific checks to `core/network/src/androidTest/java/com/dan/terminal/core/network/ConnectivityReportExporterTest.kt`; verify `./gradlew :core:network:connectedDebugAndroidTest` fails before implementation.
- [ ] Port Net Intel measurement semantics into `ConnectivityProbe`; inject a `NetworkTransport` for DNS/TCP and run blocking work on an injected IO dispatcher.
- [ ] Port history/export behavior while preserving MIT notices; do not reuse Net Intel's synchronous call path or app-specific screen.
- [ ] Add a user-triggered diagnostics screen with permission feedback and explicit “read-only connectivity check” labeling; no automatic periodic scans.
- [ ] Run module tests, app unit tests, lint and Android 16 instrumentation; update notices and evidence tracker only after success.

## Task 7: Extend the existing GATT inspector with approval-safe writes

**Files:**
- Modify: `core/bluetooth/src/main/java/com/dan/terminal/core/bluetooth/GattInspector.kt`
- Modify: `app/src/main/java/com/dan/terminal/feature/bluetooth/GattScreen.kt`
- Test: `core/bluetooth/src/test/java/com/dan/terminal/core/bluetooth/GattWritePolicyTest.kt`

**Interfaces:**
- Add `data class GattWriteRequest(serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray)`.
- Add a pure policy `fun approve(request: GattWriteRequest, confirmed: Boolean): GattWriteDecision`; no transport write may occur unless `confirmed == true`.
- Keep all GATT operations within the active `GattInspector` callback/session identity.

- [ ] Add fake-GATT tests for unsupported characteristic, permission revocation, callback from stale session, denied confirmation and accepted write.
- [ ] Assert denied confirmation produces zero calls to the fake transport's `writeCharacteristic`.
- [ ] Run `./gradlew :core:bluetooth:testDebugUnitTest` and confirm denial/stale-session tests fail before implementation.
- [ ] Implement API-appropriate write callbacks and explicit per-value confirmation UI; keep scan/connect lifecycle behavior unchanged.
- [ ] Run unit tests, Android permission instrumentation and lint. Keep real peripheral behavior marked unverified until Dan tests it.

## Task 8: Evaluate Kitty/Sixel renderer compatibility without overclaiming

**Files:**
- Create: `docs/M5_GRAPHICS_COMPATIBILITY.md`
- Test: `app/src/androidTest/java/com/dan/terminal/terminal/TerminalProtocolProbeTest.kt`.

- [ ] Add terminal input probes for Kitty keyboard protocol, Kitty graphics, Sixel and iTerm2 image sequences; capture rendered output and unsupported behavior.
- [ ] Check upstream library licence, protocol support and Android memory behavior for each candidate renderer.
- [ ] Record each protocol as supported, partial or unsupported with test/device evidence; do not enable a feature flag without a passing renderer test.

## Task 9: Reuse safe release gates and add M6 release evidence

**Files:**
- Modify: `.github/workflows/obtainium-prerelease.yml`
- Modify: `app/build.gradle.kts`
- Create: `scripts/verify_release_candidate.sh`
- Create: `docs/RELEASE_EVIDENCE.md`
- Modify: `THIRD_PARTY_NOTICES.md`.

**Interfaces:**
- Script accepts one APK path and expected package/version values; exits nonzero on missing APK, wrong package, non-monotonic version, missing signature or checksum mismatch.
- Workflow remains manually dispatched and uses only `DAN_UPDATE_*` secrets scoped to DAN Terminal.

- [ ] Add shell tests for absent APK, wrong package/version, signature mismatch and checksum mismatch.
- [ ] Run `bash -n scripts/verify_release_candidate.sh` and the release-script tests before workflow integration.
- [ ] Reuse proven version/tag and signer-verification steps from sibling workflows; do not copy secret values, keys or app identifiers.
- [ ] Generate a dependency SBOM/licence report with a pinned, maintained Gradle-compatible tool; include third-party notices for shipped libraries and runtime packages.
- [ ] Verify APK certificate, package ID, increasing versionCode/versionName and SHA-256 against the built file before upload.
- [ ] Keep physical-device acceptance and off-device keystore backup as manual gates; do not publish unless those inputs are explicitly recorded.
- [ ] Run workflow validation and a signed candidate build without creating a public release; attach outputs to `docs/RELEASE_EVIDENCE.md`.

## Task 10: Update milestone evidence and requirement statuses

**Files:**
- Modify: `docs/IMPLEMENTATION_STATUS.md`
- Modify: `docs/REQUIREMENT_TRACKER.md`
- Modify: `CHANGELOG.md`

- [ ] Update each requirement only when its own test/device evidence exists.
- [ ] Record CI run URLs, commit SHA, APK SHA-256, signer fingerprint and any physical-device evidence.
- [ ] Run `python3 scripts/validate_source_tree.py`, `python3 scripts/test_bootstrap.py`, `git diff --check` and the milestone-specific Gradle test commands.

## Execution order and gates

1. Tasks 1–3 complete M3 app behavior. Dan's Redmi test runs in parallel and remains the physical acceptance gate.
2. Task 4 resolves the M4 runtime/PTY architecture before Task 5 adds a production Linux profile.
3. Tasks 6–8 reuse or extend existing code for M5. The connectivity module and GATT approval can proceed independently after plan approval.
4. Task 9 consolidates M6 release checks; Task 10 updates status only from completed evidence.
5. The PR remains draft until physical Redmi acceptance and release-signing checks are recorded.
