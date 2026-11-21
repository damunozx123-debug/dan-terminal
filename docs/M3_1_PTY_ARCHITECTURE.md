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
emulator or device. Historical CI evidence applies only to the revision tested. This document does
not establish a passing result for the current candidate.

## Scope limits

This slice verifies a single local PTY session and exposes it through the
existing terminal route. It does not claim tabs, session restoration after OS
process death, background survival guarantees, Linux distribution profiles,
accessibility review, or physical Redmi acceptance. Those remain separate work.

## Background lifetime limit

The prototype uses a started/bound service, not a foreground service. Android
may stop it after background idle once the terminal screen has unbound, or kill
the process. Navigation while the service is retained keeps shell state;
indefinite background survival is not guaranteed. `onDestroy` ends the shell.
A foreground-service/notification implementation and Redmi battery-policy
acceptance are still required for stronger background guarantees.

## Next gate: single-session ownership across screen detachment

Source regression added on 17 November 2026:
`TerminalViewAcceptanceTest.completedSetupOpensTerminalImeInputAndActivityRecreationKeepShellState`.

Acceptance:
1. Launch the actual terminal after completed setup and enter commands through
   Android's IME bridge. Change directory to /system; export a marker and the
   shell PID.
2. Navigate to Setup and assert no TerminalView remains mounted. Only after
   detachment, send a command to the retained session.
3. Require output produced while detached, conditional on the same shell PID,
   /system directory and exported marker. Construct the output marker using
   printf arguments so input echo cannot satisfy the assertion.
4. Return through Open terminal console. Require the identical TerminalSession
   object and the detached output in its transcript.
5. Recreate the activity. Require the identical TerminalSession; enter another
   command through IME and recheck PID, directory and exported marker.

Run this targeted gate on Android 16 (select the intended device explicitly
when more than one is connected):

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dan.terminal.terminal.TerminalViewAcceptanceTest#completedSetupOpensTerminalImeInputAndActivityRecreationKeepShellState
```

This focused command is for diagnosis and does not replace the full CI
acceptance manifest. The existing test name is retained, so the required-test
manifest must still demand this scenario.

Record source SHA, APK SHA-256/version, device model/Android build and the
complete test report. Physical Redmi evidence must be tied to the installed APK.
No skip, timeout, cancelled run or compilation result counts as a pass.

Status at authoring: SOURCE ADDED; compilation, installation and execution of
this change are NOT VERIFIED. The test covers screen detachment and activity
recreation while the app process lives. It does not prove Home/background-idle
survival, process-death restoration or a foreground service.

Remaining single-session checks include streamed output before command exit,
TTY on stdin/stdout/stderr, Ctrl+C recovery, view-to-PTY resize under output,
exit status 7 plus explicit fresh-shell restart, and force-stop/fresh-launch
behaviour. Full issue #3 also requires the remaining controls/TUI, tabs,
clipboard/Unicode and accessibility acceptance. Issue #3 remains open.
