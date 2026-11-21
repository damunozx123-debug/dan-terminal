#!/usr/bin/env bash
# Offline smoke tests for manual script; fake ADB only, no Android device.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin"
cat >"$TMP/bin/adb" <<'MOCK'
#!/usr/bin/env bash
set -u
if [[ "${1:-}" == devices ]]; then
  case "${MOCK_MODE:-normal}" in
    absent) printf 'List of devices attached\n';;
    multiple) printf 'List of devices attached\nA\tdevice\nB\tdevice\n';;
    *) printf 'List of devices attached\nSERIAL\tdevice\n';;
  esac
  exit
fi
if [[ "${1:-}" == -s ]]; then
  [[ "${2:-}" == SERIAL ]] || exit 3
  shift 2
fi
case "${1:-}" in
  get-state) echo device;;
  shell)
    shift
    case "${1:-}" in
      pm)
        [[ "${MOCK_MODE:-normal}" == missing ]] && exit 0
        echo 'package:/data/app/base.apk';;
      getprop) printf 'MOCK_DEVICE\r\n';;
      dumpsys) printf 'versionName=0.1.0-M1\n';;
      am)
        [[ "${2:-}" == start ]] || exit 2
        [[ "${3:-}" == -n ]] || exit 2
        printf 'Starting activity\n';;
      *) echo "unexpected mock shell command: ${1:-}" >&2; exit 2;;
    esac;;
  *) echo "unexpected mock adb command: ${1:-}" >&2; exit 2;;
esac
MOCK
chmod +x "$TMP/bin/adb"

# Script requires a TTY for human evidence; use script(1) to simulate one, piping
# test responses only to the fake-A/DB test process.
PASS=0
run_case() {
  local name="$1" expected="$2" answers="$3" rc=0
  mkdir -p "$TMP/$name"
  # stdin is connected to a pseudo terminal by script(1); input is automated
  printf '%s' "$answers" | env MOCK_MODE="$name" PATH="$TMP/bin:$PATH" \
    DAN_M3_EVIDENCE_DIR="$TMP/$name" \
    script -q -e -c "bash '$ROOT/scripts/redmi_m3_pty_manual.sh'" "$TMP/$name/tty.txt" >"$TMP/$name/out.txt" 2>&1 || rc=$?
  [[ "$rc" == "$expected" ]] || { echo "FAIL $name: expected $expected got $rc"; cat "$TMP/$name/out.txt"; exit 1; }
  ((PASS+=1))
  echo "PASS $name"
}

bash -n "$ROOT/scripts/redmi_m3_pty_manual.sh"
bash -n "${BASH_SOURCE[0]}"
command -v script >/dev/null || { echo 'script(1) is needed for offline test harness'; exit 2; }

run_case normal 0 $'p\np\np\np\np\np\n'
run_case partial 5 $'p\ns\np\np\np\np\n'
run_case failing 6 $'p\nf\np\np\np\np\n'
run_case absent 3 ''
run_case multiple 4 ''
run_case missing 2 ''

REPORT="$(find "$TMP/normal" -name 'manual-pty-*.txt' -print -quit)"
[[ -n "$REPORT" ]] || { echo 'FAIL: report missing'; exit 1; }
[[ "$(stat -c %a "$REPORT")" == 600 ]] || { echo 'FAIL: report not private'; exit 1; }
grep -q 'DAN_M3_MANUAL_PASSED' "$REPORT"
if grep -q 'force-stop\|adb install\|SENSITIVE' "$REPORT"; then echo 'FAIL: unsafe content'; exit 1; fi
printf 'PASS: %d offline scenarios plus report assertions\n' "$PASS"
