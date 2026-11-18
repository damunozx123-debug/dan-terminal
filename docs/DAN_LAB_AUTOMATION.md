# Automated Android 16 DAN lab evidence

This is a **read-only** workstation audit. It neither installs APKs nor tests an app-owned PTY or demonstrates kernel exploitation.

## Setup on BlackArch
```bash
git clone https://github.com/damunozx123-debug/dan-terminal.git ~/dan-terminal
cd ~/dan-terminal
git fetch origin automation/android-lab-daily-audit-20261119
git switch automation/android-lab-daily-audit-20261119
chmod +x scripts/dan-lab-audit.sh
ANDROID_SERIAL=52fd242b bash scripts/dan-lab-audit.sh
```
When multiple ADB transports are present, select the desired serial using `ANDROID_SERIAL`. The USB serial above was observed on 19 Nov 2026 and may differ if using Wi-Fi ADB. Logs are written privately under `~/dan-security-lab/android/evidence/`. Review them before sharing because they include device identifiers and build details.

## Optional daily systemd user timer
```bash
mkdir -p ~/.config/systemd/user
cp automation/systemd/dan-lab-audit.* ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now dan-lab-audit.timer
systemctl --user list-timers dan-lab-audit.timer
```
Timer runs at **09:00 local computer time** when user systemd is running, and catches up at next user-manager start after missed schedules. It does not necessarily run when the laptop is shut down. `systemctl --user start dan-lab-audit.service` runs it immediately. The service assumes repo checkout at `~/dan-terminal` and requires `adb` on PATH. ADB over USB or Wireless Debugging must already be authorized. To disable: `systemctl --user disable --now dan-lab-audit.timer`.

The timer writes no data to GitHub, does not enable root, and does not create remote persistent device connections. Confirm reports locally; only curated evidence should be committed.
