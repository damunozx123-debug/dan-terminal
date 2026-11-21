#!/usr/bin/env bash
# Offline-only tests: adb/tailscale are replaced with stubs, never real transports.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/fakebin"

cat > "$TMP/fakebin/adb" <<'MOCK'
#!/usr/bin/env bash
set -u
if [[ "${1:-}" == "devices" ]]; then
  case "${FAKE_MODE:-good}" in
    empty) printf 'List of devices attached\n';;
    multiple) printf 'List of devices attached\nA\tdevice\nB\tdevice\n';;
    list_timeout) sleep 4;;
    *) printf 'List of devices attached\nMOCK-1\tdevice\n';;
  esac
  exit 0
fi
if [[ "${1:-}" == "-s" ]]; then
  [[ "${2:-}" == "MOCK-1" ]] || exit 1
  shift 2
fi
case "${1:-}" in
  get-state) printf 'device\n';;
  shell)
    shift
    case "${1:-}" in
      getprop)
        if [[ "${FAKE_MODE:-good}" == "prop_failure" && "${2:-}" == "ro.product.model" ]]; then exit 1; fi
        if [[ "${FAKE_MODE:-good}" == "prop_timeout" && "${2:-}" == "ro.product.model" ]]; then sleep 4; fi
        if [[ "${FAKE_MODE:-good}" == "prop_empty" && "${2:-}" == "ro.product.model" ]]; then exit 0; fi
        printf 'MOCK_VALUE\r\n';;
      uname) printf '6.1.0-mock\n';;
      id) printf 'uid=2000(shell)\n';;
      getenforce) printf 'Enforcing\n';;
      pm)
        if [[ "${FAKE_MODE:-good}" == "pm_failure" ]]; then exit 7; fi
        if [[ "${FAKE_MODE:-good}" == "no_installs" ]]; then exit 0; fi
        printf 'package:/data/app/com.dan.terminal/base.apk\n';;
      dumpsys)
        if [[ "${FAKE_MODE:-good}" == "dumpsys_failure" ]]; then exit 8; fi
        printf 'versionCode=1\nversionName=0.1\ntargetSdk=36\n';;
      pidof) exit 1;;
      *) echo "unknown mock shell command" >&2; exit 9;;
    esac;;
  *) echo "unknown mock adb call" >&2; exit 9;;
esac
MOCK
cat > "$TMP/fakebin/tailscale" <<'MOCK'
#!/usr/bin/env bash
if [[ "${1:-}" == "ip" ]]; then printf '100.1.2.3\n'; else printf 'SENSITIVE_PEER_LIST\n'; fi
MOCK
chmod +x "$TMP/fakebin/adb" "$TMP/fakebin/tailscale"

PASS=0
run_case() {
  local case_name="$1" expected="$2" actual=0 dir="$TMP/case-$1"
  mkdir -p "$dir"
  env -u ANDROID_SERIAL FAKE_MODE="$case_name" DAN_LAB_DIR="$dir" DAN_ADB_TIMEOUT=1 PATH="$TMP/fakebin:$PATH" \
    bash "$ROOT/scripts/dan-lab-audit.sh" >"$dir/console.txt" 2>&1 || actual=$?
  if [[ "$actual" -ne "$expected" ]]; then
    echo "FAIL $case_name: expected exit $expected, got $actual" >&2
    cat "$dir/console.txt" >&2
    exit 1
  fi
  local files=("$dir"/android/evidence/audit-*.txt)
  if [[ ! -f "${files[0]}" ]]; then echo "FAIL $case_name: missing evidence" >&2; exit 1; fi
  cmp "${files[0]}" "$dir/console.txt" >/dev/null || { echo "FAIL $case_name: incomplete evidence" >&2; exit 1; }
  [[ "$(stat -c '%a' "${files[0]}")" == "600" ]] || { echo "FAIL $case_name: evidence not private" >&2; exit 1; }
  ! grep -q 'SENSITIVE_PEER_LIST' "$dir/console.txt" || { echo "FAIL $case_name: peer inventory leaked" >&2; exit 1; }
  PASS=$((PASS + 1))
  echo "PASS $case_name"
}

run_case good 0
grep -q 'ANDROID_AUDIT_COMPLETE:' "$TMP/case-good/console.txt"
grep -q 'pid=not-running-or-unavailable' "$TMP/case-good/console.txt"
run_case empty 3
run_case multiple 4
run_case list_timeout 3
run_case prop_failure 5
grep -q 'ro.product.model=ERROR(exit=1)' "$TMP/case-prop_failure/console.txt"
run_case prop_timeout 5
grep -q 'ro.product.model=TIMEOUT' "$TMP/case-prop_timeout/console.txt"
run_case pm_failure 5
run_case dumpsys_failure 5
run_case no_installs 0
run_case prop_empty 5
grep -q 'ro.product.model=EMPTY' "$TMP/case-prop_empty/console.txt"

echo "PASS: all $PASS offline tests"
