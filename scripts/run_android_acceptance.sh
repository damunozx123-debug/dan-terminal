#!/usr/bin/env bash
# Run inside the emulator action while ADB is still available.
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
attempt="${DAN_ACCEPTANCE_ATTEMPT:-first}"
[[ "$attempt" == first || "$attempt" == retry ]] || { echo 'Invalid acceptance attempt'; exit 1; }
diag="${RUNNER_TEMP:-/tmp}/dan-terminal-emulator-diagnostics/$attempt"
mkdir -p "$diag"
touch "$diag/attempt-started"

capture() {
  local result=$?
  trap - EXIT
  adb devices -l > "$diag/devices.txt" 2>&1 || true
  adb shell getprop > "$diag/properties.txt" 2>&1 || true
  adb shell pm path com.dan.terminal.dev > "$diag/app-path.txt" 2>&1 || true
  adb shell dumpsys package com.dan.terminal.dev > "$diag/app-package.txt" 2>&1 || true
  adb logcat -d -t 4000 > "$diag/logcat.txt" 2>&1 || true
  # Keep both attempts; the second attempt must not overwrite the first reports.
  if [[ -d app/build/outputs/androidTest-results/connected ]]; then
    cp -R app/build/outputs/androidTest-results/connected "$diag/androidTest-results"
  fi
  if [[ -d app/build/reports/androidTests/connected ]]; then
    cp -R app/build/reports/androidTests/connected "$diag/androidTest-reports"
  fi
  exit "$result"
}
trap capture EXIT

# Never accept a stale XML file left by a previous device/attempt.
rm -rf app/build/outputs/androidTest-results/connected app/build/reports/androidTests/connected
ready=false
for ((check=0; check<60; check++)); do
  boot=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || boot=""
  # Package Manager may be available while the shared-media FUSE mount is broken.
  # UTP writes test output there; retain its readiness failure instead of hanging.
  if [[ "$boot" == 1 ]] && adb shell pm path android 2>/dev/null | grep -q '^package:' \
    && adb shell 'mkdir -p /sdcard/Download && touch /sdcard/Download/.dan-ci-readiness && rm -f /sdcard/Download/.dan-ci-readiness' > "$diag/storage-readiness.txt" 2>&1; then
    ready=true
    break
  fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  printf '%s\n' '{"status":"infrastructure-failure","reason":"boot-framework-storage-timeout","retry":true,"executed":0}' > "$diag/result.json"
  echo '::error::Android boot/Package Manager/shared-storage readiness timed out'
  exit 1
fi
touch "$diag/readiness-passed"
# This marker means invocation only. Actual tests are established by XML below.
touch "$diag/gradle-started"
set +e
./gradlew :app:connectedDebugAndroidTest --stacktrace --info --no-daemon 2>&1 | tee "$diag/gradle.log"
gradle_status=${PIPESTATUS[0]}
set -e
set +e
python3 "$script_dir/verify_android_acceptance.py" verify \
  --results app/build/outputs/androidTest-results/connected \
  --expected "$script_dir/android_acceptance_expected.txt" \
  --log "$diag/gradle.log" --gradle-exit "$gradle_status" --output "$diag/result.json"
verification_status=$?
set -e
[[ "$gradle_status" == 0 ]] || exit "$gradle_status"
exit "$verification_status"
