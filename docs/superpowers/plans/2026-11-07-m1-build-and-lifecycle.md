# M1 build and lifecycle implementation plan

Goal: publish the prepared MIT source and obtain a real Android build, unit-test and lint result while fixing confirmed access and Bluetooth lifecycle defects.

Architecture: retain Kotlin, Compose and the existing five modules. Shizuku remains a core capability with official user-authorized service access. Native BLE and Termux runtime permissions stay independent. Scope is the existing M1 baseline, not a new terminal engine or embedded privileged service.

Authorization: the user asked for the best possible result after authorizing public GitHub publication and permission review. Routine implementation choices are delegated. Work runs inline and changes are published on a review branch; no release tag or production merge.

## Decisions
- Use AGP 8.13.2, Gradle 8.13 and JDK 17 for API 36; Kotlin/Compose compiler 2.2.21.
- Preserve the initialized remote README/history as the parent of the source import.
- Obtain execution evidence from GitHub Actions because this host lacks Gradle/SDK.
- Require all assemble, test and lint tasks before uploading the debug APK.
- Keep target-phone validation and unfinished requirements explicit.

## Review focus
- A missing or revoked Bluetooth permission must show recovery, never crash a callback.
- Cancelling a scan, leaving its screen or backgrounding must release the scanner.
- A stale GATT callback must not replace the current connection or leave it open.
- Runtime permission requests must match the requested capability, including Termux.
- Shizuku service death and IPC failure must invalidate cached grants.

## Tasks
- [ ] Publish the baseline on a review branch with a coherent toolchain and independently recorded CI checks.
- [ ] Add regression tests for capability-specific permission requests and GATT lifecycle failures; demonstrate failure before fixes where the runner permits.
- [ ] Fix established permission, scan cancellation and GATT cleanup defects with minimal API changes.
- [ ] Run assembly, unit tests and lint; obtain and commit the verified Gradle wrapper.
- [ ] Review the final branch, update current documentation and requirement evidence, and open/update the pull request.

## Execution ledger
- Initial repository: public damunozx123-debug/dan-terminal, main 08779108ddaf4906adbc1c75cd0683e9fce3723f, README only.
- Source baseline: local 46080b0. Existing structural checks passed; Android compilation/tests/lint have never started locally.
