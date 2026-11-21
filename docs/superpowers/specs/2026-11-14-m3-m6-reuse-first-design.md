# DAN Terminal M3–M6 reuse-first design

## Intent

Advance DAN Terminal through M3–M6 while Dan tests the M3.1 APK on his Redmi.
Reuse code from Dan's existing repositories where it fits, preserve the current
app-owned PTY architecture, and keep feature claims tied to test/device evidence.

## Current state and evidence

- DAN Terminal now owns one `/system/bin/sh` PTY and renders it with Termux's
  Apache-2.0 terminal-view/emulator components.
- The Android 16 emulator acceptance test, JVM tests, lint, APK assembly and
  source checks passed in run 43551499671. This is emulator evidence only; the
  Redmi/HyperOS install and interactive behavior remain Dan's device test.
- DAN Terminal already contains BLE scan/GATT discovery, Shizuku access flows,
  Termux bootstrap/bridge, a signed Obtainium prerelease workflow and signing
  configuration. Extend these rather than creating parallel implementations.
- `net-intel-app-v9` contains MIT-licensed Android connectivity measurement,
  history and report-export code (`ProbeEngine`, `ScanResult`, `HistoryStore`,
  `ReportExporter`) plus Termux/PRoot-Distro Debian launch scripts.
- `vehicle-forge-x`, `Patternx`, `gpu-efficiency-lab-android` and
  `net-intel-app-v9` contain Android release/signing workflow patterns. No
  repository inspected contains a PTY engine, SSH/Mosh client or standalone
  Linux-profile manager.

## Approaches considered

1. **Selective source reuse inside DAN Terminal (recommended).** Port relevant
   MIT code into existing modules, retain notices, adapt APIs and add DAN
   Terminal tests. Keep the Termux PRoot scripts as a bootstrap reference, not
   as the PTY transport. Keep DAN Terminal's signing key separate from every
   other app. This has low coupling between repositories and makes ownership
   and testing straightforward.
2. **Create a shared cross-repository Android library/workflow.** Centralizes
   common code but adds versioning, publishing and dependency coordination
   before the interfaces have stabilized.
3. **Rebuild each capability in DAN Terminal.** Avoids importing code but
   duplicates working diagnostics, BLE/GATT and release patterns without a
   technical benefit.

## Goals

- **M3:** Finish default-terminal routing and interactive controls around the
  tested app-owned PTY: hardware/software input, Ctrl+C/Ctrl+D, arrows, Tab,
  Esc, resize, scrollback, copy/paste, Unicode, session lifecycle and crash
  behavior. Add tabs/named sessions only after single-session behavior is
  stable. Keep first-run setup and tools accessible from Settings.
- **M4:** Add a first supported Linux profile and remote connection foundations.
  Reuse the Net Intel Debian/PRoot-Distro consent/bootstrap flow where applicable.
  Investigate the runtime/PTY boundary before committing to a backend: the
  existing Termux RUN_COMMAND bridge is one-shot and cannot be treated as a
  terminal stream. Add SSH/SFTP/host verification only after selecting a
  maintained, licensed client and implementing host-key and secret-storage
  requirements.
- **M5:** Extend DAN Terminal's existing BLE/GATT and permission/access modules.
  Port the read-only Net Intel connectivity model, local history and CSV/PDF
  export after moving blocking network probes off the UI thread and writing
  tests. Add per-operation user approval for device writes. Evaluate Kitty/Sixel
  compatibility; do not promise graphics support until a renderer passes tests.
- **M6:** Consolidate release work around DAN Terminal's existing Obtainium
  workflow. Reuse safe validation steps from other repos (version/tag checks,
  signature verification, explicit device-test gates, artifact checks), but
  keep this app's keystore unique and never copy secret values. Add dependency
  licence inventory, release artifact checksums and reproducibility evidence.

## Non-goals

- Do not replace the app-owned PTY with Termux RUN_COMMAND.
- Do not claim all Linux distributions, Mosh, remote Docker, Kitty/Sixel, or
  physical Redmi behavior before their individual acceptance checks pass.
- Do not copy signing keys, passwords, package IDs, app branding or repository
  secrets from sibling apps.
- Do not bulk-copy whole apps or add a shared library before stable shared APIs
  are demonstrated.

## Constraints and implementation calls

- The source repositories inspected use MIT for their original app code; copied
  substantial portions must retain the relevant copyright/licence notice.
  Third-party libraries and downloaded distribution images keep their own
  licences and notices.
- Net Intel's `ProbeEngine.measure()` performs blocking DNS/TCP work and is not
  suitable for direct UI-thread use. Port/adapt its domain behavior, not its
  synchronous call pattern.
- Net Intel's PRoot scripts assume Termux and an already-installed Debian
  `proot-distro`; they do not install an independent DAN Terminal runtime or
  expose an interactive PTY.
- M3.1 emulator success is a green gate for continuing code work, not a substitute
  for Dan's Redmi test. Keep the PR draft until physical acceptance and release
  signing checks are recorded.

## Acceptance evidence

- M3: Android 16 instrumented tests plus recorded Redmi checks for terminal
  startup, input, resize, interrupt, copy/paste, output and session lifecycle.
- M4: tests for profile install/upgrade failure and removal policy, and an
  authenticated test SSH endpoint covering host-key acceptance/rejection and
  interactive input/output. No host credentials in source or CI logs.
- M5: unit/instrumented tests for probe results with missing permissions,
  timeouts, history/export redaction and GATT write approval; verify that denied
  actions cause zero device writes.
- M6: signed APK certificate and monotonically increasing version verified;
  checksums, dependency licence report and release notes attached to a manually
  gated prerelease. No publication until the user records physical-device
  acceptance.

## Remaining to resolve before implementation

- Select the M4 Linux runtime backend and its supported first profile after a
  focused compatibility/licensing spike. This is the one architecture choice
  that cannot be settled by copying the PRoot scripts.
- Confirm the exact Android-side SSH library after checking maintenance,
  licence, host-key verification and encrypted-key storage support.
- Decide which M3 controls Dan's Redmi bug test identifies as blockers versus
  follow-up polish.
