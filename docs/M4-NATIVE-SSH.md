# M4.2: Native in-app BlackArch SSH (experimental)

This draft creates a separate **SSH (native)** screen in DAN Terminal Dev.
It does NOT launch Termux. It makes a direct SSHJ connection from the app
to BlackArch, allocates an SSH PTY, starts a shell and enters the existing
dan-phone tmux session. Termux and the local Android PTY remain unchanged.

## Security model

- A SHA256 SSH SERVER host-key fingerprint is mandatory. Empty/mismatched
  fingerprints are rejected. There is no accept-all host-key verifier.
- Initial authentication uses the BlackArch account password typed into
  the app, not saved to disk, prefs, Git or logs. Java Strings cannot be
  reliably zeroed in memory. This is not a hardened key implementation.
- Android app sandbox prevents reading Termux's private SSH key. The app
  does not copy or export that key.
- Remote clipboard control sequences are ignored.
- No root requirement or SSH server configuration changes.

## Step 1: independently verify BlackArch host identity

On BlackArch through your existing trusted Termux SSH connection:

    ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub -E sha256

Copy ONLY the SHA256 fingerprint field, which is public information.
Verify against your own trusted BlackArch host key before entering it.

Default profile: 100.72.159.123, port 22, username dan.
Fingerprint: you MUST supply the real value; it is not hardcoded.
Password: type at connection time; it is not saved.

If SSH negotiates a host key with another algorithm, do not bypass
verification. Verify the negotiated server host key on BlackArch.

## Step 2: build on BlackArch

    cd ~/dan-terminal-m3-verify
    git fetch origin
    git switch --track origin/feature/m4-native-ssh-20261122
    ./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest --console=plain

This builds the Dev APK without installing.

## Step 3: deliver without wireless ADB (local Redmi Termux)

    scp blackarch:/home/dan/dan-terminal-m3-verify/app/build/outputs/apk/debug/app-debug.apk \
      ~/storage/downloads/DAN-Terminal-M4-native-dev.apk

    termux-open -c application/vnd.android.package-archive \
      ~/storage/downloads/DAN-Terminal-M4-native-dev.apk

Android prompts for install permission. This updates the .dev package
and does not overwrite the separate .preview installation.
If a signature mismatch occurs, DO NOT uninstall before verifying its data.

## Step 4: physical Redmi acceptance

1. Open DAN Terminal Dev. Tap SSH (native), NOT Open in Termux.
2. Enter the independently verified SHA256 fingerprint and password.
3. Tap Connect directly; confirm Connected (verified host key).
4. Tap the dark terminal canvas to bring up Android keyboard.
5. Run hostname, expect blackarch.
6. Run tmux display-message -p '#S', expect dan-phone.
7. Test Esc, Ctrl+C, Tab, arrows, keyboard, resize and disconnect.
8. Intentionally change one character of the fingerprint and verify
   connection is rejected even if the correct password is given.

## Known limitations and release gate

This is a testable first native implementation, NOT a stable release:
- Termux TerminalEmulator processes ANSI/VT control codes, while the
  temporary Canvas painter renders MONOCHROME visible character cells.
  Color, Sixel, advanced selection and comprehensive scrollback UI
  are not yet implemented.
- Keyboard composition, lifecycle, reconnect, algorithms, and PTY resize
  still require real Redmi tests.
- Closing the screen closes SSH; no background SSH daemon or cached secret.
- No automatic GitHub merge, deployment or live scan occurs.

Do not merge until compilation, unit tests, six UI regression cases,
the negative host-key test, remote commands and disconnect/reconnect
are all verified on the physical Redmi.

SSHJ 0.41.2 provides native SSH transport and strict fingerprint
verification via addHostKeyVerifier(fingerprint).
