# Redmi M3 acceptance record

Device: Redmi Note 14 Pro+ Global / HyperOS 3.0
Tester: Dan
APK/build: Dan was directed to the development artifact for commit
`958b07381847820c7d841eedd6ec8e0dddc757fa`:
https://github.com/damunozx123-debug/dan-terminal/actions/runs/43746493102/artifacts/14175796246

On 16 November Dan supplied screenshots of the installed app and a shell prompt
under `/data/data/com.dan.terminal.dev/files`. The installed APK's hash/version
has not been read from the phone, so these are user-observed development-app
results, not certification of that exact artifact or any later revision.

Device reference supplied by Dan on 16 November 2026 (Settings screenshot;
capture date unknown, so reconfirm before acceptance):

| Field | Screenshot value |
|---|---|
| Model | 24115RA8EG |
| RAM / storage | 12.0 GB / 256 GB |
| HyperOS | 3.0.304.0.WOPMIXM.C07 |
| Android / build | 16 / BP2A.250605.031.A3 |
| Android security update | 2026-06-01 |
| Kernel | 6.1.138-android14-11-g0c3d559bcd85-ab14529422 |

This screenshot identifies software/device details only. It does not prove an
APK install, terminal behavior, bootloader state or kernel vulnerability status.

Use `PASS`, `FAIL` or `NOT TESTED`, and include a short note for each item.

| Check | Result | Notes |
|---|---|---|
| Install/update and first launch | PARTIAL | Development app installed and opened, shown by screenshots. In-place update compatibility and installed APK hash/version are unverified. |
| First-run setup remains accessible, then opens terminal | NOT TESTED | |
| Later launch opens the terminal by default | NOT TESTED | |
| Shell accepts typed command and Enter; output remains visible | PASS | User transcript shows `echo DAN_OUTPUT_OK` producing `DAN_OUTPUT_OK`. |
| Shell has a TTY and retains directory/environment state | PASS | `test -t 0` produced `PTY_OK`; `pwd` returned `/system`; `echo "$DAN_TEST"` returned `ready` after export. |
| Software keyboard remains available and does not cover the prompt | PARTIAL | Keyboard visible and command entry observed. All keyboard/inset configurations are not verified; terminal text is too small. |
| Ctrl+C interrupts a foreground command while the shell remains usable | NOT TESTED | |
| Ctrl+D, Esc, Tab and arrow-key controls behave as expected | NOT TESTED | The new software control row is source-only until a newer APK is built. |
| Rotate/change visible area and confirm shell `stty size` updates | NOT TESTED | |
| Scrollback, copy/paste and Unicode output | NOT TESTED | |
| Multiline paste previews text; Cancel sends nothing; Paste text confirms | NOT TESTED | Include LF and CR/CRLF clipboard text; verify shell exit/restart clears a pending paste. |
| Navigate to Settings/Tools and return without losing the live session | NOT TESTED | |
| Background/resume, screen lock and battery restrictions | NOT TESTED | |
| Force-stop/relaunch starts a clearly new PTY session | NOT TESTED | A shell process is not restored after Android process death. |

Do not mark emulator evidence as a Redmi result. Attach screenshots or a short
screen recording for failures and note the exact steps that reproduce them.

## Additional phone observations — 16 November

- BLE advertisements, signal strength, service counts and broadcast-only labels
  are visible in the user's scan screenshot. GATT connection/read behavior has
  not been demonstrated.
- Connectivity diagnostics display TCP 10.6 ms, DNS 45.2 ms and 0% failures in
  the short test. This is not a LAN inventory or security audit.
- Shizuku screen status is “Not running”; authorization/service startup is not
  accepted by these screenshots.
- Navigation persistence is still NOT TESTED: repeated transcripts demonstrate
  shell state but do not explicitly document leaving and reopening the screen.
- A font-size correction is in development. None of the observations above
  verifies its newer APK, Ctrl+C, background survival or release signing.
