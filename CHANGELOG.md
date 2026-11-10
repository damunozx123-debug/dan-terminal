# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Added
- Reconstructed the supplied M0/M1 Android source baseline.
- Added Mockito inline mock maker configuration.
- Wired Shizuku app launch callbacks through the Access Required dialog.
- Passed the selected BLE device into the GATT screen.
- Added a more actionable Termux dispatch failure message.
- Corrected Termux RUN_COMMAND executable-path and argument separation.
- Added a GATT read-value cache for API 33+ and the legacy callback.

### Fixed
- Reviewed Shizuku service/permission recovery, denial handling, callback synchronization and listener lifecycle (source only; H08/P01/P02/P04).
- Added fake-API regression tests for the permission flow; execution is blocked by the missing wrapper.

### Verification
- Not compiled or tested in this environment. See docs/MILESTONE_REPORT_M0_M1.md.
