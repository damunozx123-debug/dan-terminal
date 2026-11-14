# M3.1 app-owned PTY session

## Decision

Use Termux's separately published `terminal-view` 0.118.0 library, which brings
in `terminal-emulator`, for Android terminal rendering and its JNI-backed PTY
subprocess. DAN Terminal creates and owns the `TerminalSession` inside its
non-exported `PtyTerminalService`; the Compose shell screen binds to that service
and hosts `TerminalView`. Termux `RUN_COMMAND` remains an optional one-shot
integration and is not the shell transport.

The Termux application repository is GPLv3-only, while its upstream licence
identifies `terminal-view` and `terminal-emulator` as Apache-2.0 exceptions.
DAN Terminal imports only the two terminal libraries, not `termux-shared` or the
Termux application. See `THIRD_PARTY_NOTICES.md` and `licenses/Apache-2.0.txt`.

## Process and session lifetime

The service is started and bound when the terminal screen opens. It owns one
interactive `/system/bin/sh` session running with the DAN Terminal app UID and
sandbox paths. It remains available when the Compose screen is left while the
app process and started service remain alive. It is not a foreground service;
Android may stop it under background or memory pressure. Process death ends the
PTY and starts a fresh shell on the next open. No elevated Shizuku/root execution
is used.

## M3.1 acceptance test

`PtyTerminalServiceTest.serviceOwnsInteractivePtyAcrossInputResizeAndInterrupt`
starts the actual Android service and PTY, checks `test -t 0`, verifies `cd` and
an exported variable persist in the same shell, checks `stty size` after a PTY
resize, sends Ctrl+C to a running command, and verifies the shell accepts input
again. Run with `./gradlew :app:connectedDebugAndroidTest` on an Android 16
emulator or device. This repository checkout has not run that test yet.

## Scope limits

This slice verifies a single local PTY session and exposes it through the
existing terminal route. It does not claim tabs, session restoration after OS
process death, background survival guarantees, Linux distribution profiles,
accessibility review, or physical Redmi acceptance. Those remain separate work.
