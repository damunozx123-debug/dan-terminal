#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/bin"
printf 'fixture apk\n' > "$tmp/app.apk"
cat > "$tmp/bin/apkanalyzer" <<'SH'
#!/usr/bin/env bash
case "$2" in
  application-id) echo "${FAKE_PACKAGE:-com.dan.terminal}" ;;
  version-name) echo "${FAKE_VERSION_NAME:-0.1.0-dev.1}" ;;
  version-code) echo "${FAKE_VERSION_CODE:-10001}" ;;
  *) exit 2 ;;
esac
SH
cat > "$tmp/bin/apksigner" <<'SH'
#!/usr/bin/env bash
[[ "${FAKE_SIGNATURE_OK:-yes}" == yes ]] || exit 1
echo 'Signer #1 certificate SHA-256 digest: 00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF'
SH
chmod +x "$tmp/bin/apkanalyzer" "$tmp/bin/apksigner"
export PATH="$tmp/bin:$PATH"
export APKANALYZER="$tmp/bin/apkanalyzer" APKSIGNER="$tmp/bin/apksigner"
script="$root/scripts/verify_release_candidate.sh"
cert=00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF
sha=$(sha256sum "$tmp/app.apk" | awk '{print $1}')

expect_fail() {
  local label=$1; shift
  if "$@" >/dev/null 2>&1; then echo "FAIL: $label unexpectedly passed" >&2; exit 1; fi
}

"$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 "$cert" "$sha" >/dev/null
expect_fail missing_apk "$script" "$tmp/missing.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 "$cert"
expect_fail wrong_package env FAKE_PACKAGE=com.wrong "$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 "$cert"
expect_fail wrong_version env FAKE_VERSION_NAME=0.1.0-dev.2 "$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 "$cert"
expect_fail stale_version_code env FAKE_VERSION_CODE=10000 "$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 "$cert"
expect_fail non_monotonic "$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10000 10000 "$cert"
expect_fail signing_failure env FAKE_SIGNATURE_OK=no "$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 "$cert"
expect_fail signer_mismatch "$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 11112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF
expect_fail checksum_mismatch "$script" "$tmp/app.apk" com.dan.terminal 0.1.0-dev.1 10001 10000 "$cert" deadbeef
echo "Release candidate verification tests passed."
