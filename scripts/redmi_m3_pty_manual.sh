#!/usr/bin/env bash
# Manual, non-destructive Android PTY acceptance. Never installs, force-stops,
# sends terminal commands, enables timers, or claims APK/source equivalence.
set -Eeuo pipefail
umask 077

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE="${DAN_M3_PACKAGE:-com.dan.terminal.dev}"
case "$PACKAGE" in
  com.dan.terminal.dev|com.dan.terminal.preview) ;;
  *) echo "Only side-by-side Dev/Preview packages may be tested: $PACKAGE" >&2; exit 2 ;;
esac
TIMEOUT="${DAN_ADB_TIMEOUT:-15}"
if [[ ! "$TIMEOUT" =~ ^[1-9][0-9]{0,2}$ ]] || (( TIMEOUT > 120 )); then
  echo 'DAN_ADB_TIMEOUT must be an integer between 1 and 120 seconds' >&2
  exit 2
fi
for tool in adb timeout date awk mktemp tee grep tr git; do
  command -v "$tool" >/dev/null || { echo "Required program unavailable: $tool" >&2; exit 2; }
done
[[ -t 0 ]] || { echo 'Interactive terminal required; results must be observed on the phone.' >&2; exit 2; }

adb_run() { timeout --kill-after=3s "${TIMEOUT}s" adb "$@"; }
SERIAL="${ANDROID_SERIAL:-}"
if [[ -z "$SERIAL" ]]; then
  listing="$(adb_run devices 2>/dev/null)" || { echo 'ADB device enumeration failed' >&2; exit 3; }
  mapfile -t devices < <(printf '%s\n' "$listing" | awk 'NR>1 && $2=="device" {print $1}')
  if (( ${#devices[@]} == 0 )); then echo 'No authorized ADB device' >&2; exit 3; fi
  if (( ${#devices[@]} > 1 )); then echo 'Multiple devices: set ANDROID_SERIAL' >&2; exit 4; fi
  SERIAL="${devices[0]}"
fi
adb_dev() { adb_run -s "$SERIAL" "$@"; }
[[ "$(adb_dev get-state 2>/dev/null)" == device ]] || { echo 'Selected ADB target not connected' >&2; exit 3; }
if ! adb_dev shell pm path "$PACKAGE" 2>/dev/null | grep -q '^package:'; then
  echo "Package is not installed or unreadable: $PACKAGE" >&2
  exit 2
fi

OUT_DIR="${DAN_M3_EVIDENCE_DIR:-$HOME/dan-security-lab/android/evidence}"
mkdir -p -m 700 "$OUT_DIR"
REPORT="$(mktemp "$OUT_DIR/manual-pty-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX.txt")"
log() { printf '%s\n' "$*" | tee -a "$REPORT"; }

log 'DAN TERMINAL M3: MANUAL REDMI ACCEPTANCE'
log "utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
log "adb_target=$SERIAL"
log "package=$PACKAGE"
log "device=$(adb_dev shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
log "android=$(adb_dev shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')"
log 'apk_source_revision=UNVERIFIED (installation hash/signing identity not matched)'
log "checkout_revision=$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown) (not APK provenance)"
log "package_version=$(adb_dev shell dumpsys package "$PACKAGE" 2>/dev/null | awk '/versionName=/ {print $1; exit}')"
log 'Persistent PTY acceptance is not established by adb connectivity alone.'

if ! adb_dev shell am start -n "$PACKAGE/com.dan.terminal.MainActivity" >/dev/null 2>&1; then
  log 'DAN_M3_MANUAL_BLOCKED: could not open app activity'
  echo "Evidence: $REPORT"
  exit 3
fi

cat <<'INSTRUCTIONS'

The Dev/Preview app was opened (or brought forward) on the phone.
IN THE APP-OWNED TERMINAL, TYPE THESE COMMANDS MANUALLY:

  export DAN_PTY_MARK=alive
  export DAN_PTY_PID=$$
  cd /system
  printf 'MARK=%s PID=%s DIR=%s\n' "$DAN_PTY_MARK" "$DAN_PTY_PID" "$PWD"
  tty
  test -t 0 && test -t 1 && echo PTY_IO_OK

For navigation, resize, Home/return and lock/unlock checks, type:

  printf 'MARK=%s PID=%s DIR=%s\n' "$DAN_PTY_MARK" "$$" "$PWD"

Compare output with the original marker, PID and directory. An app process
killed by Android is expected to create a NEW session, not restore this shell.
Do NOT paste commands into the BlackArch terminal to claim on-device PTY evidence.
INSTRUCTIONS

PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
record() {
  local name="$1" instruction="$2" answer
  printf '\nCHECK: %s\n%s\n' "$name" "$instruction"
  while true; do
    if ! read -r -p 'Result [p=PASS, f=FAIL, s=NOT TESTED]: ' answer; then answer=s; fi
    case "${answer,,}" in
      p|pass) log "${name}=PASS"; PASS_COUNT=$((PASS_COUNT+1)); break;;
      f|fail) log "${name}=FAIL"; FAIL_COUNT=$((FAIL_COUNT+1)); break;;
      s|skip|'') log "${name}=NOT_TESTED"; SKIP_COUNT=$((SKIP_COUNT+1)); break;;
      *) echo 'Enter p, f or s' ;;
    esac
  done
}

record 'shell_pty_io' 'Confirm MARK, PID, /system, a /dev/pts/* TTY and PTY_IO_OK on the phone.'
record 'interrupt' 'Run sleep 20; press Ctrl+C in the app; shell stays usable afterward.'
record 'navigation' 'Tap Setup, then Open terminal console; confirm same PID, marker and directory.'
record 'activity_recreation' 'Rotate the phone and return; confirm same PID, marker and directory.'
record 'background_resume' 'Press Home, reopen app; confirm same PID, marker and directory.'
record 'font_and_input' 'Use A-/A+, arrows/Tab/Esc and confirm terminal remains readable/responds.'

log "passes=$PASS_COUNT failures=$FAIL_COUNT not_tested=$SKIP_COUNT"
if (( FAIL_COUNT > 0 )); then
  log 'DAN_M3_MANUAL_FAILED: at least one check failed'
  result=6
elif (( SKIP_COUNT > 0 )); then
  log 'DAN_M3_MANUAL_PARTIAL: checks remain untested'
  result=5
else
  log 'DAN_M3_MANUAL_PASSED: interactive observations only; app source provenance unverified'
  result=0
fi
printf '\nEvidence saved privately: %s\n' "$REPORT"
printf 'Exit code: %d\n' "$result"
exit "$result"
