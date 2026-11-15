# Redmi M3 acceptance record

Device: Redmi Note 14 Pro+ Global / HyperOS 3.0
Tester: Dan
APK/build: record the exact artifact URL, version and SHA-256 before testing.

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
| Install/update and first launch | NOT TESTED | |
| First-run setup remains accessible, then opens terminal | NOT TESTED | |
| Later launch opens the terminal by default | NOT TESTED | |
| Shell accepts typed command and Enter; output remains visible | NOT TESTED | |
| Software keyboard remains available and does not cover the prompt | NOT TESTED | |
| Ctrl+C interrupts a foreground command while the shell remains usable | NOT TESTED | |
| Ctrl+D, Esc, Tab and arrow-key controls behave as expected | NOT TESTED | The new software control row is source-only until a newer APK is built. |
| Rotate/change visible area and confirm shell `stty size` updates | NOT TESTED | |
| Scrollback, copy/paste and Unicode output | NOT TESTED | |
| Navigate to Settings/Tools and return without losing the live session | NOT TESTED | |
| Background/resume, screen lock and battery restrictions | NOT TESTED | |
| Force-stop/relaunch starts a clearly new PTY session | NOT TESTED | A shell process is not restored after Android process death. |

Do not mark emulator evidence as a Redmi result. Attach screenshots or a short
screen recording for failures and note the exact steps that reproduce them.
