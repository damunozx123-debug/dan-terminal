# Terminal readability update — 16 November 2026

## Change

Dan's Redmi screenshot showed a working shell with extremely small text. The
screen passed the integer 16 to Termux TerminalView; its renderer forwards that
value to Android Paint in physical pixels. A dense phone screen therefore got
a much smaller apparent font than the rest of the Compose interface.

The terminal now converts a 14sp default through Compose Density (including
Android's non-linear accessibility scaling). A− and A+ change size by 2sp within
10–28sp. SharedPreferences retains the choice across navigation and relaunch.
Changing font size reflows the current terminal grid; it does not start a new
shell. An applied-size cache prevents renderer recreation on every output event.

## Installation path

The regular development build stays `com.dan.terminal.dev`. An additional
`preview` build uses `com.dan.terminal.preview` and the label **DAN Terminal
Preview**, with package-scoped provider authorities. It can coexist with the
installed Dev build and its private data. Its settings and shell are separate.
This is an installable test build, not a production release or a promise of
stable update signing. Keep the current working Dev app installed.

CI uploads `dan-terminal-preview-fonts-phone-unverified` before attempting the
Android emulator tests. A compiled artifact alone does not establish UI or
physical-device acceptance.

## Executed local verification

- Reproduced the legacy raw-pixel sizing: all 3 original JVM regression cases
  failed before the conversion fix.
- Final JVM suite: **69 tests, 0 failures/errors/skipped**, including 4 font
  cases for high density, accessibility scaling, selected size and half pixels.
- `:app:assembleDebug`, `:app:assemblePreview`, `:app:assembleDebugAndroidTest`,
  `testDebugUnitTest`, and `lintDebug`: **BUILD SUCCESSFUL**; no lint errors in
  any module. Existing dependency/style warnings remain.
- Source structure and whitespace checks pass. Bootstrap (3), acceptance-runner
  (21), acceptance-report regression (17), and release-gate shell tests pass.
- Preview APK badging/signature/integrity checked: expected package/label and
  launcher, unique provider authorities and ARM64 native library present.
- Independent review found no remaining blocker. A fractional-density test
  rounding mismatch found in review was corrected.

The local build used Gradle 8.13, JDK 17 and SDK 36. This Cloud host disallows
Mockito's dynamic self-attachment, so its existing Mockito dependency was loaded
at JVM startup using a local-only javaagent init script. No project dependency
or production permission was changed for that environment adjustment.

## Still pending

Android UI coverage has been added and its test APK compiles. It checks actual
terminal column reflow at the selected font, preservation of the current shell,
font persistence across activity recreation/navigation, and disabled size limits.
These scenarios have not executed locally because this host has no emulator/KVM.
The CI expected-test manifest includes them; failures cannot be reported as passes.

Font legibility and controls on the Redmi require testing with the new Preview
APK. Previous user transcripts establish command execution, PTY detection, working
directory and environment state only; see REDMI_M3_ACCEPTANCE.md. Ctrl+C,
background survival and full Shizuku behavior remain unverified on the phone.
