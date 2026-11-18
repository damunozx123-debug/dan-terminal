# Android 16 reconnection and M3 device-test gate (19 November 2026)

## Screenshot evidence (user-supplied)
- A remote SSH session to `192.168.86.144` ended with `Software caused connection abort` / `Broken pipe`. The host's current IP and SSH availability are not verified.
- `adb shell id` returned UID 2000 (`shell`); `getenforce` returned `Enforcing`; `ro.boot.flash.locked` returned `1`; `uname -r` returned `6.1.138-android14-11-g0c3d559bcd85-ab14529422`.
- The screenshot shows 6% battery. Charge before testing.
- The recorded GhostLock device-tree notes mark physical load address compatibility **UNVERIFIED**. They do not establish kernel arbitrary-write or root access.

## Reconnect from a computer with adb
1. Charge the phone and join the computer and phone to the same trusted network.
2. In Android Developer options > Wireless debugging, view the device **IP address & port**. If a new pairing is needed, choose **Pair device with pairing code**; run `adb pair PHONE_IP:PAIR_PORT` and enter the code. Pairing port and connection port are different.
3. Run `adb connect PHONE_IP:DEBUG_PORT`, then `adb devices -l`. Ports may change when wireless debugging restarts.
4. If USB and wireless ADB show the same phone twice, choose one serial explicitly: `ANDROID_SERIAL=PHONE_IP:DEBUG_PORT bash scripts/android16-adb-baseline.sh`. Do not run unqualified `adb shell` against multiple devices.
5. For SSH on the Linux host, reconnect using `ssh -o ServerAliveInterval=20 -o ServerAliveCountMax=3 dan@192.168.86.144` only if that is still the correct host address. Check LAN connectivity and SSH service separately. ADB reconnect does not restore an SSH session.

## Baseline command
```bash
ANDROID_SERIAL=PHONE_IP:DEBUG_PORT \
  DAN_TERMINAL_PACKAGE=com.dan.terminal.dev \
  bash scripts/android16-adb-baseline.sh
```
Output is a text evidence record. The script **does not** install APKs, request root, or exercise app PTY.

## M3 manual acceptance procedure (real phone, not CI substitute)
1. Install a specifically identified build APK; record SHA-256, package name, version, device build, and installation result.
2. First launch: complete only required setup; deny optional Bluetooth and Shizuku to verify graceful fallback.
3. Reopen app: terminal home presents a visible prompt. In the **DAN Terminal UI**, run `pwd; cd /; pwd; export DAN_M3=works; echo "$DAN_M3"` and verify persistent shell state across commands.
4. Run `cat`, type a line, verify echo, press Ctrl+C; run `top` and exit it; open `vi` or `vim` and exercise Esc. Test Tab, arrows, Ctrl+D, Unicode, scrollback, copy/paste, resize, hardware/software keyboard.
5. Test two named terminal sessions, background/foreground, process exit, forced app termination, and relaunch; document session persistence/restart behavior. Verify exit codes and actionable errors.
6. Capture a short screen recording and filtered logcat with timestamps. Confirm execution UID within app sandbox and that Shizuku/root are not silently used.

**Pass gate:** observable, app-owned persistent PTY interaction (not one-shot Termux RUN_COMMAND), with session I/O, resize, lifecycle, and input confirmed on Android 16. **Fail / unverified:** ADB shell alone, app launch alone, Gradle build success alone, or command execution through external Termux.
