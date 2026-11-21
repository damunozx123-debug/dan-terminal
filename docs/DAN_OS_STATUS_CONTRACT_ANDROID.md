# DAN-OS connection-status contract: Android staging gate

Based on the 2026-11-22 connection handover in all three repositories:
`dan-os`, `dan-terminal`, and `net-intel-app-v9`.

## Verified baseline (operator evidence on 22 Nov, not current uptime)

- BlackArch had working NetworkManager Wi-Fi, Tailscale and SSH.
- Private user-timer collector reported a fresh record at the time of testing.
- BlackArch Ollama was loopback-only, and phone Termux successfully reached it
  through an authenticated SSH local forward; no model ran locally in Termux.
- Backup command reported a matching digest and remote archive copy.
- Termux cron had periodic tunnel and backup jobs.

These observations do not prove the Android apps can consume the feed,
nor that the host is reachable now or after a reboot.

## Implemented in this branch

`DanOsStatusContract.kt` contains a **pure read-only classifier**. Callers
must supply an already authenticated, validated and parsed observation with
only these approved fields:

- `updated`
- `network.wifi`
- `network.tailscale`
- `services.ssh`
- `security.suspend_masked` (optional)

It returns FRESH only at ages 0 through 120 seconds, STALE beyond 120 seconds,
and UNKNOWN for missing or future timestamps. Flags are not presented as
current when the source is stale or unknown. Independent timestamped
Android-to-BlackArch reachability is REACHABLE/UNREACHABLE/UNKNOWN.
The two results are **never inferred from each other**.

Ten deterministic JVM unit tests cover freshness boundaries, unknown input,
and divergent Linux/Android reachability.

## Deliberately not implemented or claimed

- No file loading, JSON adapter or runtime status dump is bundled in the APK.
  The exact timestamp encoding and authenticated transport must be checked
  against the private BlackArch collector before such an adapter is written.
- No SSH command, Tailscale API call, public HTTP endpoint or network scan.
- No Android status badge claiming a live connection.
- No automatic backup/inference requests or credential management.
- No boot-persistence, USB/Bluetooth failover, MFA or firewall changes.

Do not commit private network endpoints, credentials, raw status dumps,
backup archives, private keys, environment variables or raw AI output.

## Verification

From BlackArch after fetching this draft branch:

    ./gradlew :app:testDebugUnitTest --console=plain

For a full build:

    ./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain

Do not merge until compilation and unit tests are verified. Actual
Android-to-Linux reachability and consumption require a separate, user-approved
integration and a physical-device test.
