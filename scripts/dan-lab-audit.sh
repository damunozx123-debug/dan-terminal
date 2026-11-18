#!/usr/bin/env bash
# Local, read-only DAN security lab baseline; no privilege elevation or exploitation.
set -Eeuo pipefail
umask 077
LAB="${DAN_LAB_DIR:-$HOME/dan-security-lab}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="$LAB/android/evidence/audit-$STAMP.txt"
mkdir -p "$LAB/android/evidence"
command -v adb >/dev/null || { echo "adb is not installed" >&2; exit 2; }
exec > >(tee "$OUT") 2>&1
echo "DAN LAB AUDIT UTC: $STAMP"
echo "host=$(hostname) user=$(id -un)"
echo "=== TAILSCALE ==="
if command -v tailscale >/dev/null; then
  tailscale ip -4 || true
  tailscale status || true
else echo "tailscale unavailable"; fi
echo "=== TOOLCHAIN ==="
for t in adb nmap git python gdb ghidra frida qemu-system-aarch64; do
  if command -v "$t" >/dev/null; then echo "OK $t"; else echo "MISSING $t"; fi
done
echo "=== ADB DEVICE SELECTION ==="
SERIAL="${ANDROID_SERIAL:-}"
if [[ -z "$SERIAL" ]]; then
  mapfile -t DEVICES < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  if (( ${#DEVICES[@]} == 0 )); then
    echo "DEVICE_OFFLINE: no authorized ADB target; not a successful Android audit"
    exit 3
  elif (( ${#DEVICES[@]} > 1 )); then
    echo "AMBIGUOUS_DEVICE: set ANDROID_SERIAL to one serial:"
    printf '%s\n' "${DEVICES[@]}"
    exit 4
  fi
  SERIAL="${DEVICES[0]}"
fi
ADB=(adb -s "$SERIAL")
if [[ "$("${ADB[@]}" get-state 2>/dev/null)" != "device" ]]; then
  echo "DEVICE_OFFLINE: $SERIAL"
  exit 3
fi
echo "adb_serial=$SERIAL"
echo "=== PHONE BASELINE ==="
for prop in ro.product.model ro.product.device ro.build.version.release ro.build.fingerprint ro.boot.flash.locked; do
  printf '%s=' "$prop"
  "${ADB[@]}" shell getprop "$prop" | tr -d '\r'
done
printf 'kernel='
"${ADB[@]}" shell uname -r | tr -d '\r'
printf 'identity='
"${ADB[@]}" shell id | tr -d '\r'
printf 'selinux='
"${ADB[@]}" shell getenforce | tr -d '\r'
echo "=== DAN APP INSTALLS ==="
for pkg in com.dan.terminal com.dan.terminal.dev com.dan.terminal.codex; do
  echo "--- $pkg ---"
  "${ADB[@]}" shell pm path "$pkg" || true
  "${ADB[@]}" shell dumpsys package "$pkg" |
    grep -m 8 -E 'versionCode=|versionName=|firstInstallTime=|lastUpdateTime=|targetSdk=' || true
  printf 'pid='
  "${ADB[@]}" shell pidof "$pkg" || true
done
echo "=== KVM ==="
[[ -e /dev/kvm ]] && echo "present (guest acceleration unverified)" || echo "absent"
echo "=== EVIDENCE ==="
echo "ADB baseline captured; DAN Terminal persistent PTY NOT TESTED."
echo "report=$OUT"
