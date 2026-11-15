#!/usr/bin/env bash
# Called while the emulator action is still running, before it terminates ADB.
set -euo pipefail
diag="${RUNNER_TEMP:-/tmp}/dan-terminal-emulator-diagnostics"
mkdir -p "$diag"

capture() {
  local result=$?
  trap - EXIT
  adb devices -l > "$diag/devices.txt" 2>&1 || true
  adb shell getprop > "$diag/properties.txt" 2>&1 || true
  adb logcat -d -t 4000 > "$diag/logcat.txt" 2>&1 || true
  exit "$result"
}
trap capture EXIT

# Emulator boot completion alone does not mean Package Manager is ready.
ready=false
for ((attempt=0; attempt<60; attempt++)); do
  boot=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [[ "$boot" == 1 ]] && adb shell pm path android 2>/dev/null | grep -q '^package:'; then
    ready=true
    break
  fi
  sleep 2
done
[[ "$ready" == true ]] || { echo "::error::Android boot/Package Manager readiness timed out"; exit 1; }

# This marker prevents a test failure/crash from being retried as a boot failure.
touch "$diag/tests-started"
./gradlew :app:connectedDebugAndroidTest --stacktrace --no-daemon
