# M3 paste, callback and acceptance repairs

Scope: fix the three confirmed defects on PR #4's exact head, preserving the existing feature branch and draft status. Do not merge, sign, publish a release, or mark physical Redmi acceptance complete.

1. Repair CI classification using actual XML test results and retained install logs. Retry one fresh emulator only for positively identified pre-test infrastructure failures. Fail on zero, missing, skipped or failed required tests. Preserve per-attempt diagnostics.
2. Use the pinned Termux PendingIntent `exitCode` contract. Add actual Android receiver tests for success, failure, execution error, missing result and invalid request.
3. Implement clipboard paste through TerminalEmulator.paste. Require an explicit confirmation for multiline text, clear pending text on cancel/session replacement, and add Android tests through the production callback and UI.
4. Run offline regression/structural suites, review changes, update PR #4 atomically and inspect the exact new CI run. Record unavailable local Gradle/SDK access and all remaining device/signing gates honestly.

Baseline: 3be0cafb45ad74cc2b8dfb6cd7d3fafaea3eab81. Offline runner (4), bootstrap (3), release verifier and structural checks pass. Local Gradle cannot download its pinned distribution because this executor's network cannot reach services.gradle.org; Android/Kotlin validation must run in GitHub CI.

Integrated concurrent parent `a67fef666ab6d1caa63599a759d1e9e1e91d860a`: retained its JVM receiver regression tests and device reference table, combined its acceptance fixtures with the stronger report/retry gate, and preserved per-attempt evidence.
