#!/usr/bin/env bash
# Android 16 G2 importer acceptance. A single retry is permitted ONLY when
# Android reports that the instrumentation process failed to attach.
# Assertion failures, crashes, missing test counts, and unknown errors stay red.
set -euo pipefail

: "${RUNNER_TEMP:?RUNNER_TEMP is required for evidence files}"
EVIDENCE="$RUNNER_TEMP/g2-diagnostic-emulator"
mkdir -p "$EVIDENCE"

APP_PACKAGE="com.dan.terminal.dev"
TEST_PACKAGE="com.dan.terminal.dev.test"
TEST_CLASS="com.dan.terminal.reports.DiagnosticReportImporterTest"
EXPECTED_TESTS=6

capture_diagnostics() {
  local label="$1"
  {
    printf 'boot_completed='
    adb shell getprop sys.boot_completed || true
    printf 'package_manager=\n'
    adb shell pm path "$APP_PACKAGE" || true
    adb shell pm path "$TEST_PACKAGE" || true
    printf 'device_state=\n'
    adb devices -l || true
  } > "$EVIDENCE/$label-state.txt" 2>&1
  adb logcat -d -v time -t 1500 > "$EVIDENCE/$label-logcat.txt" 2>&1 || true
  adb shell dumpsys activity processes > "$EVIDENCE/$label-processes.txt" 2>&1 || true
}

# Boot-completed alone is not readiness: the previous failure happened after
# the emulator's first 10-minute boot while package manager was still sluggish.
adb wait-for-device
ready=0
for i in $(seq 1 60); do
  boot="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  packages="$(adb shell cmd package list packages com.android.settings 2>/dev/null | tr -d '\r' || true)"
  if [[ "$boot" == "1" && "$packages" == *"package:com.android.settings"* ]]; then
    ready=1
    break
  fi
  sleep 2
done
if [[ "$ready" != "1" ]]; then
  echo "::error::Emulator never reached package-manager readiness"
  capture_diagnostics "boot-not-ready"
  exit 1
fi

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for package in "$APP_PACKAGE" "$TEST_PACKAGE"; do
  if ! adb shell pm path "$package" | grep -Fq 'package:'; then
    echo "::error::Installed package not resolvable: $package"
    capture_diagnostics "missing-package"
    exit 1
  fi
done

# Let the newly booted API 36 image finish package/runtime initialization.
sleep 25

run_attempt() {
  local number="$1"
  local output="$EVIDENCE/instrumentation-attempt-$number.txt"
  echo "Running G2 instrumentation attempt $number"
  set +e
  adb shell am instrument -w -r \
    -e class "$TEST_CLASS" \
    "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" \
    2>&1 | tee "$output"
  local adb_exit="${PIPESTATUS[0]}"
  set -e
  if [[ "$adb_exit" -eq 0 ]] &&
     grep -Fq "OK ($EXPECTED_TESTS tests)" "$output" &&
     ! grep -Eq 'FAILURES!!!|INSTRUMENTATION_RESULT: shortMsg=' "$output"; then
    echo "PASS: $EXPECTED_TESTS Android instrumented tests"
    return 0
  fi
  echo "Attempt $number failed (adb_exit=$adb_exit)"
  capture_diagnostics "attempt-$number"
  return 1
}

if run_attempt 1; then
  exit 0
fi

# The known infrastructure defect is explicitly reported as 'failed to attach'.
# This retry cannot mask assertion/test failures because no test was executed.
if grep -Fq 'INSTRUMENTATION_RESULT: shortMsg=failed to attach' \
    "$EVIDENCE/instrumentation-attempt-1.txt" &&
   ! grep -Eq 'FAILURES!!!|INSTRUMENTATION_STATUS: test=' \
    "$EVIDENCE/instrumentation-attempt-1.txt"; then
  echo "::warning::Android process failed to attach before any test; one bounded infrastructure retry"
  adb shell am force-stop "$APP_PACKAGE" || true
  adb shell am force-stop "$TEST_PACKAGE" || true
  sleep 45
  if run_attempt 2; then
    exit 0
  fi
fi

echo "::error::DiagnosticReportImporterTest has not passed all $EXPECTED_TESTS tests"
exit 1
