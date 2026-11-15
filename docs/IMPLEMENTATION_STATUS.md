# Implementation status — 15 November 2026

DAN Terminal is progressing through M3 terminal work and selected M4–M6
foundations. The 95-requirement roadmap is not complete. Physical
Redmi/HyperOS acceptance and persistent-key release verification remain pending.

## Current delivery

PR #4 has been reconciled with `main` while retaining the separate development
application ID `com.dan.terminal.dev`, its `DAN Terminal Dev` launcher label,
persistent signing configuration and checksum-pinned Gradle 8.13 wrapper.

The first correction is commit `32f4345addb496694448b7741d129149e7bd1fdf`.
[Android CI](https://github.com/damunozx123-debug/dan-terminal/actions/runs/43675351639)
is evaluating it. This run completed successfully: compilation/unit/lint, APK assembly and both
Android 16 PTY tests passed (0 skipped, 0 failed). Physical Redmi checks remain
required.

## PTY failure and correction

[Push run 43561378105](https://github.com/damunozx123-debug/dan-terminal/actions/runs/43561378105)
failed at `b44215ba` with `TerminalBuffer.setChar: column=80, mColumns=80`.
The crash occurred in the main-thread output handler while the instrumentation
test resized/read the emulator on a different thread. The pinned Termux API
requires emulator operations on the main thread. The tests now serialize these
operations, check an exact 80-cell line plus autowrap, and exercise repeated
80↔100-column reflow while shell output is active. Shell markers are formatted
from separate arguments so command echo cannot satisfy their assertions.

Emulator boot is a separate CI concern. Readiness now checks boot completion and
Package Manager, captures ADB/logcat before emulator teardown, and permits one
fresh-AVD retry only if instrumentation never started. Test failures remain
failures. The build uploads a development APK even if Android acceptance fails;
its artifact name explicitly says `unverified`.

The broader candidate `baf2de0` compiled its production APK and passed JVM
checks, but CI run 43676274888 failed Android-test lint dependency resolution:
`ui-test-junit4` had no version because its Android-test configuration lacked
the Compose BOM. The corrective change adds that BOM and compiles the test APK
before emulator startup. The full candidate is not accepted until that run
completes.

## Added source awaiting full Android verification

- M3: terminal redraw notifications, software controls (including application
  cursor-key mode), keyboard focus/IME inset handling, clipboard-copy callback,
  explicit exited-shell restart, and activity-recreation/Android-IME tests.
- M4: `M4_RUNTIME_DECISION.md` records the app-private runtime-to-PTY gate. No
  production Linux runtime or SSH backend has been selected or installed.
- M5: user-triggered connectivity diagnostics, IO-dispatched probes, local
  bounded history and scoped-document CSV/PDF export. The code adapts Net Intel
  concepts with its MIT notice. It does not discover LAN devices or detect new
  devices. PDF pagination and history lifecycle tests are included.
- M5 GATT: a pure approval-policy scaffold exists; it is not connected to a
  device write transport. Peripheral-write support is not accepted.
- M6: the candidate-verification script checks package/version, increasing
  versionCode, trusted signer fingerprint and optional APK SHA-256. Shell tests
  pass. The manual signing workflow requires device acceptance and creates a
  draft prerelease with verification evidence. It has not been dispatched.

## Evidence limits

Local source/XML/TOML checks, bootstrap execution tests, release-script tests,
workflow/shell syntax and whitespace checks pass. Local Gradle/Android execution
has not run: the environment lacks a ready Android toolchain and previous Gradle
downloads failed. GitHub CI is the build/emulator verification path.

Earlier run 43551499671 passed the previous baseline on an Android 16 emulator;
the later failure above prevents treating that older pass as current acceptance.
No emulator result substitutes for Dan's physical-device checks. No production
release or signed candidate has been produced in this work session.

## Still open

- Complete rendered-screen resize, clipboard/paste and software-key acceptance;
  then tabs/named sessions and the remaining M3 controls.
- Record Redmi tests in `REDMI_M3_ACCEPTANCE.md` for the exact delivered APK.
- Prove an ARM64 Linux child process can attach to the app-owned PTY before M4
  backend selection; then implement profile/SSH host-key and credential gates.
- Integrate GATT write approvals and verify real peripheral behavior.
- Resolve dependency licences/SBOM, verify persistent signing and record actual
  release certificate/version/checksum. A dependency tree is not an SBOM.
- Evaluate graphics protocols independently; no Kitty/Sixel support is claimed.
