# Terminal readability implementation plan

**Goal:** Fix tiny text on the Redmi and let the user choose a readable size.
**Architecture:** Convert a saved 10–28sp size (default 14sp, step 2sp) through
Compose Density before calling the pixel-based terminal renderer. A−/A+ controls
update the existing view and PTY dimensions without restarting the shell.
**Tech stack:** Existing Kotlin, Compose, TerminalView and SharedPreferences.
**Scope:** Font size, regression coverage, phone evidence and a Dev APK.

## Task: readable, persistent terminal text

- [x] Reproduce the raw-pixel bug with JVM density/accessibility tests.
- [x] Add saved bounded size and accessible A−/A+ controls.
- [ ] Test actual terminal column reflow, same-session state and saved size
  through navigation and activity recreation on Android.
- [ ] Run source checks, JVM tests, assembly, lint and Android acceptance;
  report failures independently and provide the Dev APK with its exact commit.
- [x] Record user-supplied Redmi evidence without claiming Ctrl+C, background
  survival or navigation steps that have not been observed.

Local JVM tests, debug/Preview/test APK assembly, lint and source checks passed.
Android UI scenarios are compiled and queued for CI; no device font pass is claimed.
