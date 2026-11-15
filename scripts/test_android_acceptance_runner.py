"""Verify emulator startup failures stay distinct from instrumented test failures."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

RUNNER = Path(__file__).with_name("run_android_acceptance.sh").resolve()


class AcceptanceRunnerTest(unittest.TestCase):
    def run_fixture(self, boot_ready="1", test_exit="0", transient_adb="0"):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bin_dir = root / "bin"
            bin_dir.mkdir()
            (bin_dir / "adb").write_text("""#!/usr/bin/env bash
if [[ "$*" == 'shell getprop sys.boot_completed' ]]; then
  if [[ "$TRANSIENT_ADB" == 1 && ! -f adb-retried ]]; then touch adb-retried; exit 1; fi
  echo "$BOOT_READY";
elif [[ "$*" == 'shell pm path android' ]]; then echo 'package:/system/framework/framework-res.apk';
else echo 'fixture diagnostics'; fi
""")
            (bin_dir / "sleep").write_text("#!/usr/bin/env bash\nexit 0\n")
            (root / "gradlew").write_text("#!/usr/bin/env bash\ntouch gradle-called\nexit \"$TEST_EXIT\"\n")
            for file in [bin_dir / "adb", bin_dir / "sleep", root / "gradlew"]:
                file.chmod(0o755)
            env = dict(os.environ, PATH=f"{bin_dir}:{os.environ['PATH']}",
                       RUNNER_TEMP=str(root), BOOT_READY=boot_ready, TEST_EXIT=test_exit, TRANSIENT_ADB=transient_adb)
            result = subprocess.run(["bash", str(RUNNER)], cwd=root, env=env, capture_output=True)
            diag = root / "dan-terminal-emulator-diagnostics"
            return result.returncode, (diag / "tests-started").exists(), (root / "gradle-called").exists(), (diag / "logcat.txt").exists()

    def test_ready_emulator_runs_tests_and_captures_diagnostics(self):
        self.assertEqual((0, True, True, True), self.run_fixture())

    def test_boot_timeout_never_starts_instrumentation(self):
        self.assertEqual((1, False, False, True), self.run_fixture(boot_ready="0"))

    def test_test_failure_keeps_exit_code_and_prevents_boot_retry(self):
        self.assertEqual((7, True, True, True), self.run_fixture(test_exit="7"))

    def test_transient_adb_failure_is_retried_in_readiness_loop(self):
        self.assertEqual((0, True, True, True), self.run_fixture(transient_adb="1"))


if __name__ == "__main__":
    unittest.main()
