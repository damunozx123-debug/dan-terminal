# M0/M1 Baseline Intake Report

Date: 2026-11-05
Status: source reconstructed; build and acceptance criteria unverified.

## Files and features

The local tree contains the supplied Gradle configuration, app and library
modules, manifests, BLE scanner and GATT state, Shizuku manager and recovery
dialog, Termux bridge and bootstrap script, tests, CI workflow, and security
notes. The six follow-up patches were incorporated into the corresponding
files.

## Verification

- Build: NOT RUN. JDK 17 is present; Gradle and wrapper are absent. Android SDK
  API 36 and a connected device/emulator are not available.
- Unit tests: NOT RUN; the wrapper could not start.
- Instrumented tests: NOT RUN; adb/device unavailable.
- Lint: NOT RUN; the wrapper could not start.
- APK: not produced.
- GitHub: the connected account was inspected; no DAN Terminal repository was
  found, and the connector does not expose a repository-creation operation.

### Commands attempted

| Command | Result |
|---|---|
| gradle wrapper --gradle-version 8.10.2 --distribution-type bin | Exit 127 — /bin/bash: line 1: gradle: command not found |
| ./gradlew clean :app:assembleDebug --stacktrace --warning-mode all | Exit 127 — /bin/bash: line 1: ./gradlew: No such file or directory |
| ./gradlew testDebugUnitTest --stacktrace | Exit 127 — /bin/bash: line 1: ./gradlew: No such file or directory |
| ./gradlew lintDebug --stacktrace | Exit 127 — /bin/bash: line 1: ./gradlew: No such file or directory |

The Gradle task runner did not start, so these are environment/bootstrap
failures, not Gradle build or test failures. No source patch can supply the
missing Gradle executable or wrapper JAR.

Static check command: python3 scripts/validate_source_tree.py — exit 0.
It parsed TOML and 11 XML files, found 95 unique requirement IDs, confirmed
Mockito configuration and Git ignores, found no main-source println or
Log.d calls, and verified the Termux RUN_COMMAND source contract uses the
absolute Bash executable path plus a separate argument array. This is a
structural check only; the test and device execution remain unverified.

## Current blockers

1. P1 — Human/environment: install Gradle 8.10.2 and Android SDK API 36, or
   provide a build runner with them. Generate and commit the Gradle wrapper.
2. P1 — Human: provide an existing DAN Terminal repository URL or create an
   empty repository; no remote branch or pull request can be prepared yet.
3. P1 — Engineering/device: the GATT read method and read-value cache exist,
   but the UI has no read controls, and write/notify operations and operation
   approval are not implemented or acceptance-tested.
4. P1 — Device: Termux RUN_COMMAND delivery, HyperOS background behavior, and Shizuku recovery
   after reboot require testing on the target phone.
5. P2 — Engineering: the current tests cover model fallback text, the
   Termux command-contract mapping, and recovery
   step presence; they do not exercise BLE scanning, GATT, Shizuku binder
   lifecycle, or Termux intent delivery.

## Build blockers and source gaps

- No gradlew, gradlew.bat, or gradle-wrapper.jar was supplied.
- The Android SDK/API 36 installation could not be checked or used because
  adb is absent.
- Dependency resolution for Compose BOM 2026.02.00 and Shizuku API 13.1.5 is
  unverified.
- The supplied workflow invokes ./gradlew, so it cannot run until the wrapper
  files are committed.
- Termux source now maps commands to `/data/data/com.termux/files/usr/bin/bash`
  plus `RUN_COMMAND_ARGUMENTS`; dispatch acceptance and HyperOS behavior have
  not been tested on-device.
- Requirement IDs in the supplied tracker did not match the authoritative
  95-item register. The tracker now uses canonical IDs and records unverified
  or incomplete acceptance criteria as PARTIAL or TODO.

## Next action

Install the pinned build toolchain, generate the Gradle wrapper, then run the
first assemble and unit-test commands in the project instructions.

## Shizuku review and open-source preparation (2026-11-06)

See `SHIZUKU_REVIEW.md` for findings, changed files and exact test-start failures.
Permission-flow source and regression cases were added; all Android execution
remains NOT RUN. Original source now has an MIT licence and Shizuku API notice.
CI now has a wrapper bootstrap, explicit SDK setup, lint and report upload;
this workflow has NOT RUN. The wrapper remains absent in the local tree.
The bootstrap script was executed and stopped with exit 1 and the message
`Gradle 8.10.2 is required on PATH.` Shell syntax checks passed.

The public-repository payload and PR body are prepared. GitHub publication is
blocked by browser sign-in; the connector cannot create repositories. No push,
PR, branch protection or release tag is claimed. See `PUBLISHING.md`.

The requested AGP 8.8.2/API 36 pairing is outside the documented supported
minimum; a supported upgrade requires a maintainer toolchain decision.
