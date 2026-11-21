# Automated Android 16 DAN lab evidence

This is a **read-only** workstation audit. It does not install APKs, run a persistent PTY test, enable privileges, or perform kernel exploitation. An offline or unresponsive ADB device yields a nonzero exit status. Query failures produce an explicitly **PARTIAL** report, never a success claim.

## Review and run manually on BlackArch

```bash
git clone https://github.com/damunozx123-debug/dan-terminal.git ~/dan-terminal
cd ~/dan-terminal
git fetch origin automation/android-lab-daily-audit-20261119
git switch automation/android-lab-daily-audit-20261119
bash -n scripts/dan-lab-audit.sh
bash tests/dan-lab-audit.test.sh
adb devices
# Auto-selects only if exactly one device is authorized. Otherwise set ANDROID_SERIAL.
bash scripts/dan-lab-audit.sh
```

The manual audit requires a previously authorized, connected ADB transport. If there are multiple authorized devices, explicitly supply the serial. `DAN_ADB_TIMEOUT` sets per-command timeout in seconds (default 15, maximum 120). Logs are private by default under `~/dan-security-lab/android/evidence/` (or `DAN_LAB_DIR`) but include device identifiers, build details and the local Tailscale IP. Review and redact them before sharing. The audit intentionally does **not** record the Tailscale peer list.

## Optional systemd user timer (manual opt-in only)

The unit assumes the checkout is at `~/dan-terminal`. If it is elsewhere, **edit `ExecStart` in the copied service file** before use. The unit uses a five-minute overall execution limit and no user-manager `network-online.target` dependency.

```bash
mkdir -p ~/.config/systemd/user
cp automation/systemd/dan-lab-audit.* ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user cat dan-lab-audit.service
# Do not enable the timer until the script has been reviewed and run manually.
# To opt in later:
# systemctl --user enable --now dan-lab-audit.timer
# systemctl --user list-timers dan-lab-audit.timer
```

If enabled, the timer fires at 09:00 workstation-local time while the user manager is available. `Persistent=true` may cause a catch-up run when the user manager next starts. Shutdown prevents execution until that next start. Disable with `systemctl --user disable --now dan-lab-audit.timer`.

No audit data is uploaded to GitHub. Commit only curated, redacted evidence. This task does not establish success of app-owned PTY persistence or live device acceptance.
