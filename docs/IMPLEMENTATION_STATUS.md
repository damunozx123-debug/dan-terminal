# Implementation status — 10 November 2026

DAN Terminal is an early M0/M1 Android companion app. The full 95-item roadmap
is not complete. Device acceptance on the target Redmi/HyperOS phone is pending.

## Evidence

- Public repository and PR #1 are available; source checks pass.
- [Run 42789936760](https://github.com/damunozx123-debug/dan-terminal/actions/runs/42789936760)
  reproduced 8 failures in 10 Bluetooth regression tests before the fixes.
- [Run 42791104590](https://github.com/damunozx123-debug/dan-terminal/actions/runs/42791104590)
  compiled the debug APK and passed all **37 JVM unit tests** (0 failures/errors).
  Lint found a missing app-level Shizuku provider dependency. That declaration
  has been corrected; follow the latest CI run for the subsequent lint result.
- Three bootstrap execution tests pass locally: success/retry, failed update,
  and failed package installation. These use a fake package manager and do not
  validate real Termux package installation.
- Shell syntax, XML/TOML structure, requirement IDs and whitespace checks pass.
- The Gradle 8.13 wrapper was generated in CI and retrieved with a verified
  artifact SHA-256. Its distribution is checksum-pinned.
- Independent static review found no additional blocking issue in the BLE,
  GATT, Shizuku and permission changes. Static review is not device testing.

## Implemented behavior

- Contextual runtime permission requests for BLE scan, BLE connection and Termux.
- Shizuku service/authorization recovery, refusal for unsupported legacy services,
  blocked permission guidance and correct handling of delayed permission results.
- BLE advertisements remain visible when a protected device name cannot be read;
  broadcast-only advertisements do not offer GATT connections.
- Each GATT attempt owns its callback identity. Old callbacks cannot reopen or
  replace a new session. Terminal failures and disconnects close the connection.
- Scans and connections stop when their screens are left or the app backgrounds.
- Termux bootstrap package failures remain visible and prevent false completion.

## Phone acceptance still required

1. Install and launch the debug APK on the Redmi.
2. Deny and grant Nearby devices; scan with Bluetooth on/off; leave and return
   during a scan. Background the app and verify scanning stops.
3. Connect to a known BLE peripheral; browse services; disconnect/reconnect;
   background the app; turn Bluetooth off during a connection.
4. Start with Shizuku stopped, pair/start it, grant/deny authorization, revoke it
   in Shizuku, stop/restart the service, rotate the screen and reopen the app.
5. Install/open Termux, enable external apps, grant RUN_COMMAND, run the
   bootstrap twice and inspect its output. Verify HyperOS background restrictions.

No instrumented tests, physical BLE session, Shizuku Binder operation or real
Termux execution on the phone has been observed in this work session.

## Remaining scope

There is no embedded terminal/PTY or bundled Shizuku service yet. Distribution
profiles, SSH/Mosh screens, graphics protocols and GATT read/write/notify UI
remain roadmap work. Existing partial requirement statuses remain partial.
