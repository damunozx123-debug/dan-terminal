#!/usr/bin/env bash
# Called while the emulator action is still running, before it terminates ADB.
set -euo pipefail
attempt="${DAN_ACCEPTANCE_ATTEMPT:-first}"
case "$attempt" in first|retry) ;; *) echo "Invalid acceptance attempt" >&2; exit 2 ;; esac
diag="${RUNNER_TEMP:-/tmp}/dan-terminal-emulator-diagnostics/$attempt"
mkdir -p "$diag"
rm -f "$diag/tests-started" "$diag/retryable-infrastructure"
touch "$diag/runner-entered"
results="app/build/outputs/androidTest-results/connected"
# Retain each attempt below before a fresh emulator can replace Gradle's reports.
# These are generated reports only; stale XML must never validate a new attempt.
rm -rf -- "$results"

capture() {
  local result=$?
  trap - EXIT
  adb devices -l > "$diag/devices.txt" 2>&1 || true
  adb shell getprop > "$diag/properties.txt" 2>&1 || true
  adb logcat -d -t 4000 > "$diag/logcat.txt" 2>&1 || true
  if [[ -d "$results" ]]; then cp -R "$results" "$diag/test-results" || true; fi
  exit "$result"
}
trap capture EXIT

# Emulator boot completion alone does not mean Package Manager is ready.
ready=false
for ((attempt=0; attempt<60; attempt++)); do
  boot=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || boot=""
  if [[ "$boot" == 1 ]] && adb shell pm path android 2>/dev/null | grep -q '^package:'; then
    ready=true
    break
  fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  touch "$diag/retryable-infrastructure"
  echo "::error::Android boot/Package Manager readiness timed out"
  exit 1
fi

# Invoking Gradle does not prove APK installation or instrumentation startup.
touch "$diag/gradle-invoked"
set +e
./gradlew :app:connectedDebugAndroidTest --stacktrace --info --no-daemon 2>&1 | tee "$diag/gradle.log"
gradle_result=${PIPESTATUS[0]}
set -e
python3 "$(dirname "$0")/verify_android_acceptance.py" \
  --results "$results" --sources app/src/androidTest/java \
  --log "$diag/gradle.log" --diagnostics "$diag" --gradle-exit "$gradle_result"
