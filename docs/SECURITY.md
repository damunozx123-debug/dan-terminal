# DAN Terminal — Security Notes (M0/M1)

## Threat model at this milestone
- Unprivileged app sandbox, locked bootloader, unrooted device.
- Shizuku is treated as an optional privileged helper, not as root.
- Termux is treated as an external app the user controls.

## Implemented boundary (unverified)
- Secret values written through SecureStorage use AES/GCM with a key in Android Keystore.
- Shared preferences are excluded from cloud backup and device transfer.
- The baseline contains no telemetry or mandatory account flow.

## Non-guarantees
- M1 does not sandbox shell commands run via Termux RUN_COMMAND.
- M1 does not verify the Shizuku APK signature beyond the official client library behavior.
- M1 does not provide root.
- Termux command dispatch and HyperOS background behavior have not been verified.

## Review checklist before a verified tag
- [ ] Manifest permissions match PermissionBroker requests.
- [ ] SecureStorage key alias and block mode are reviewed.
- [ ] Shizuku provider authority is namespaced to the application ID.
- [ ] No allowBackup=true.
- [ ] No secrets, local.properties, or keystores in Git.
- [ ] No debugging println or Log.d calls in main sources.
