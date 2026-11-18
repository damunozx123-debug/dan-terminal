#!/usr/bin/env bash
# Android 16 device baseline for DAN Terminal. Does NOT validate an app PTY.
set -euo pipefail
command -v adb >/dev/null || { echo "adb is required" >&2; exit 2; }
SERIAL="${ANDROID_SERIAL:-}"
PKG="${DAN_TERMINAL_PACKAGE:-com.dan.terminal.dev}"
if [[ -z "$SERIAL" ]]; then
  mapfile -t DEVICES < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  if (( ${#DEVICES[@]} != 1 )); then
    echo "Expected exactly one authorized device; found ${#DEVICES[@]}. Set ANDROID_SERIAL." >&2
    adb devices -l >&2
    exit 2
  fi
  SERIAL="${DEVICES[0]}"
fi
ADB=(adb -s "$SERIAL")
"${ADB[@]}" get-state | grep -qx device || { echo "Device $SERIAL not ready" >&2; exit 3; }
OUT="${1:-dan-terminal-android16-evidence-$(date -u +%Y%m%dT%H%M%SZ).txt}"
{
  printf 'captured_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'serial=%s\n' "$SERIAL"
  printf 'package=%s\n' "$PKG"
  for prop in ro.product.model ro.product.device ro.build.version.release ro.build.version.sdk ro.boot.flash.locked ro.build.fingerprint; do
    printf '%s=' "$prop"
    "${ADB[@]}" shell getprop "$prop" | tr -d '\r'
  done
  printf '\n=== privilege baseline ===\n'
  "${ADB[@]}" shell id
  "${ADB[@]}" shell getenforce
  printf '\n=== package detection ===\n'
  "${ADB[@]}" shell pm path "$PKG" || true
  printf '\n=== process detection ===\n'
  "${ADB[@]}" shell pidof "$PKG" || true
  printf '\n=== device status ===\n'
  "${ADB[@]}" shell dumpsys battery | grep -E 'level:|status:|USB powered:|AC powered:' || true
  printf '\nRESULT: ADB baseline only; DAN Terminal PTY NOT TESTED.\n'
} | tee "$OUT"
echo "Saved $OUT"
