#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 APK EXPECTED_PACKAGE EXPECTED_VERSION_NAME EXPECTED_VERSION_CODE PREVIOUS_VERSION_CODE EXPECTED_CERT_SHA256 [EXPECTED_APK_SHA256]" >&2
  exit 2
}

[[ $# -ge 6 && $# -le 7 ]] || usage
apk=$1
expected_package=$2
expected_name=$3
expected_code=$4
previous_code=$5
expected_cert=${6//:/}
expected_apk_sha=${7:-}
apkanalyzer=${APKANALYZER:-apkanalyzer}
apksigner=${APKSIGNER:-apksigner}

[[ "$expected_cert" =~ ^[[:xdigit:]]{64}$ ]] || { echo "ERROR: signer fingerprint must be 64 hexadecimal characters" >&2; exit 1; }
[[ -s "$apk" ]] || { echo "ERROR: APK is missing or empty" >&2; exit 1; }
[[ "$expected_code" =~ ^[0-9]+$ && "$previous_code" =~ ^[0-9]+$ ]] || { echo "ERROR: version codes must be integers" >&2; exit 1; }
(( expected_code > previous_code )) || { echo "ERROR: version code is not greater than the previous version" >&2; exit 1; }

actual_package=$("$apkanalyzer" manifest application-id "$apk")
actual_name=$("$apkanalyzer" manifest version-name "$apk")
actual_code=$("$apkanalyzer" manifest version-code "$apk")
[[ "$actual_package" == "$expected_package" ]] || { echo "ERROR: package mismatch: $actual_package" >&2; exit 1; }
[[ "$actual_name" == "$expected_name" ]] || { echo "ERROR: version name mismatch: $actual_name" >&2; exit 1; }
[[ "$actual_code" == "$expected_code" ]] || { echo "ERROR: version code mismatch: $actual_code" >&2; exit 1; }

signature_output=$("$apksigner" verify --verbose --print-certs "$apk")
actual_cert=$(sed -nE 's/.*certificate SHA-256 digest: ([[:xdigit:]:]+).*/\1/ip' <<< "$signature_output" | head -n 1 | tr -d ':[:space:]' | tr '[:lower:]' '[:upper:]')
expected_cert=$(tr '[:lower:]' '[:upper:]' <<< "$expected_cert" | tr -d '[:space:]')
[[ -n "$actual_cert" && "$actual_cert" == "$expected_cert" ]] || { echo "ERROR: signer certificate fingerprint mismatch or missing" >&2; exit 1; }

actual_apk_sha=$(sha256sum "$apk" | awk '{print toupper($1)}')
if [[ -n "$expected_apk_sha" && "${expected_apk_sha^^}" != "$actual_apk_sha" ]]; then
  echo "ERROR: APK SHA-256 mismatch" >&2
  exit 1
fi

printf 'APK: %s\nPackage: %s\nVersion: %s (%s)\nSigner SHA-256: %s\nAPK SHA-256: %s\n' \
  "$apk" "$actual_package" "$actual_name" "$actual_code" "$actual_cert" "$actual_apk_sha"
