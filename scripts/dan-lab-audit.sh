#!/usr/bin/env bash
# Read-only DAN Android lab inventory. Never runs PTY tests or privileged commands.
set -Eeuo pipefail
umask 077

LAB="${DAN_LAB_DIR:-$HOME/dan-security-lab}"
ADB_TIMEOUT="${DAN_ADB_TIMEOUT:-15}"
if [[ ! "$ADB_TIMEOUT" =~ ^[1-9][0-9]{0,2}$ ]] || (( ADB_TIMEOUT > 120 )); then
  echo "DAN_ADB_TIMEOUT must be an integer between 1 and 120 seconds" >&2
  exit 2
fi
for tool in adb timeout tee awk; do
  command -v "$tool" >/dev/null || { echo "Required tool missing: $tool" >&2; exit 2; }
done

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT_DIR="$LAB/android/evidence"
mkdir -p -m 700 "$OUT_DIR"
OUT="$OUT_DIR/audit-$STAMP-$$.txt"

adb_run() { timeout --kill-after=3s "${ADB_TIMEOUT}s" adb "$@"; }
adb_dev() { adb_run -s "$SERIAL" "$@"; }

# Sets QUERY_VALUE and returns failure on errors; never converts an error to success.
query_shell() {
  local rc=0
  QUERY_VALUE="$(adb_dev shell "$@" 2>/dev/null)" || rc=$?
  QUERY_VALUE="${QUERY_VALUE//$'\r'/}"
  if (( rc != 0 )); then
    if (( rc == 124 || rc == 137 )); then
      QUERY_VALUE="TIMEOUT"
    else
      QUERY_VALUE="ERROR(exit=$rc)"
    fi
    return 1
  fi
  return 0
}

FAILURES=0
report_query() {
  local key="$1"
  shift
  if query_shell "$@" && [[ -n "$QUERY_VALUE" ]]; then
    printf '%s=%s\n' "$key" "$QUERY_VALUE"
  else
    if [[ -z "$QUERY_VALUE" ]]; then QUERY_VALUE="EMPTY"; fi
    printf '%s=%s\n' "$key" "$QUERY_VALUE"
    FAILURES=$((FAILURES + 1))
  fi
}

main() {
  echo "DAN LAB AUDIT UTC: $STAMP"
  echo "host=$(hostname) user=$(id -un)"
  echo "=== TAILSCALE ==="
  if command -v tailscale >/dev/null; then
    # Only the local tailnet IP, never peer inventory.
    timeout --kill-after=2s 10s tailscale ip -4 2>/dev/null || echo "tailscale ip unavailable"
  else
    echo "tailscale unavailable"
  fi

  echo "=== TOOLCHAIN ==="
  for tool in adb nmap git python gdb ghidra frida qemu-system-aarch64; do
    if command -v "$tool" >/dev/null; then echo "OK $tool"; else echo "MISSING $tool"; fi
  done

  echo "=== ADB DEVICE SELECTION ==="
  SERIAL="${ANDROID_SERIAL:-}"
  if [[ -z "$SERIAL" ]]; then
    local listing
    if ! listing="$(adb_run devices 2>/dev/null)"; then
      echo "ADB_ERROR: device enumeration failed or timed out; Android audit NOT completed"
      return 3
    fi
    local -a devices=()
    mapfile -t devices < <(printf '%s\n' "$listing" | awk 'NR>1 && $2=="device" {print $1}')
    if (( ${#devices[@]} == 0 )); then
      echo "DEVICE_OFFLINE: no authorized ADB target; Android audit NOT completed"
      return 3
    fi
    if (( ${#devices[@]} > 1 )); then
      echo "AMBIGUOUS_DEVICE: set ANDROID_SERIAL to an explicit authorized device"
      return 4
    fi
    SERIAL="${devices[0]}"
  fi

  local state
  if ! state="$(adb_dev get-state 2>/dev/null)" || [[ "$state" != "device" ]]; then
    echo "DEVICE_OFFLINE: selected ADB target unavailable; Android audit NOT completed"
    return 3
  fi
  echo "adb_serial=$SERIAL"

  echo "=== PHONE BASELINE ==="
  local prop
  for prop in ro.product.model ro.product.device ro.build.version.release ro.build.fingerprint ro.boot.flash.locked; do
    report_query "$prop" getprop "$prop"
  done
  report_query kernel uname -r
  report_query identity id
  report_query selinux getenforce

  echo "=== DAN APP INSTALLS ==="
  local pkg rc
  for pkg in com.dan.terminal com.dan.terminal.dev com.dan.terminal.codex; do
    echo "--- $pkg ---"
    if query_shell pm path "$pkg"; then
      if [[ "$QUERY_VALUE" != *"package:"* ]]; then
        echo "NOT_INSTALLED"
        continue
      fi
      printf '%s\n' "$QUERY_VALUE"
    else
      echo "pm_path=$QUERY_VALUE"
      FAILURES=$((FAILURES + 1))
      continue
    fi

    if query_shell dumpsys package "$pkg"; then
      printf '%s\n' "$QUERY_VALUE" | awk '/versionCode=|versionName=|firstInstallTime=|lastUpdateTime=|targetSdk=/ {print; if (++n == 8) exit}'
    else
      echo "dumpsys=$QUERY_VALUE"
      FAILURES=$((FAILURES + 1))
    fi

    rc=0
    query_shell pidof "$pkg" || rc=$?
    if (( rc == 0 )) && [[ -n "$QUERY_VALUE" ]]; then
      echo "pid=$QUERY_VALUE"
    elif [[ "$QUERY_VALUE" == "TIMEOUT" ]]; then
      echo "pid=TIMEOUT"
      FAILURES=$((FAILURES + 1))
    else
      echo "pid=not-running-or-unavailable"
    fi
  done

  echo "=== KVM ==="
  [[ -e /dev/kvm ]] && echo "present (guest acceleration unverified)" || echo "absent"
  echo "=== EVIDENCE ==="
  echo "DAN Terminal persistent PTY NOT TESTED."
  echo "report=$OUT"
  if (( FAILURES > 0 )); then
    echo "ANDROID_AUDIT_PARTIAL: $FAILURES query failures or missing required values"
    return 5
  fi
  echo "ANDROID_AUDIT_COMPLETE: read-only ADB baseline captured"
}

# A real pipeline waits for tee to finish writing even when main exits early.
# Capture both statuses so an output-write error cannot be silently ignored.
set +e
main 2>&1 | tee "$OUT"
statuses=("${PIPESTATUS[@]}")
set -e
if (( statuses[1] != 0 )); then
  echo "Evidence output failed (tee exit=${statuses[1]})" >&2
  exit 6
fi
exit "${statuses[0]}"
