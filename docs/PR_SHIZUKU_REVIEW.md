## Summary

Import the supplied M0/M1 source and address Shizuku permission-flow review
findings. A missing service now routes to startup recovery before permission;
unsupported services and denial produce visible guidance; callbacks refresh
pending recovery; failures clear stale local access state. Add MIT licensing
for original source, third-party notices and CI bootstrap/report steps.

## Test Plan

- EXECUTED `python3 scripts/validate_source_tree.py`: exit 0 (structural only).
- EXECUTED `git diff --check`: exit 0.
- EXECUTED `bash -n scripts/bootstrap_gradle_wrapper.sh app/src/main/assets/bootstrap/dan_bootstrap.sh`: exit 0.
- EXECUTED `./scripts/bootstrap_gradle_wrapper.sh`: exit 1, `Gradle 8.10.2 is required on PATH.` No wrapper generated.
- NOT RUN Android tests: `./gradlew :core:shizuku:testDebugUnitTest :app:testDebugUnitTest --stacktrace` could not start, exit 127 (`./gradlew: No such file or directory`).
- NOT RUN assembly, lint and instrumented/device QA. No APK exists.
- Added 12 fake-API Shizuku cases and two routing cases; no passed-test count claimed.
- CI workflow has not executed. Follow docs/SHIZUKU_REVIEW.md for device gates.

## Requirement IDs

H08, P01, P02, P04: source advanced, all remain PARTIAL.
Baseline import also covers L01, H01, H02, S02 and S07 boundaries, with gaps
recorded in the full 95-item tracker. No acceptance requirement is marked DONE.

## Screenshots

None produced; no emulator or target phone is available.

## Risk

The patch is uncompiled. The pinned AGP 8.8.2 is below the documented API 36
minimum (8.9.1); that update also requires Gradle 8.11.1. Dependency resolution
and resolved-dependency licences need review. Fake tests cannot prove Binder
IPC, process death or HyperOS pairing behavior. Permission status is not an
operation-level authorization check. The single-APK Shizuku service, reboot
receiver and destructive-operation approval persistence remain unimplemented.

Require at least one human approval and real build/test/lint evidence before
merge. This PR does not authorize a release tag. Rollback: revert the review
and publication commits; the earlier baseline remains in history.
