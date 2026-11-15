"""Exercise acceptance/retry decisions with isolated ADB/Gradle fixtures."""
import os
import json
import shutil
from pathlib import Path
import subprocess
import tempfile
import unittest

RUNNER = Path(__file__).with_name("run_android_acceptance.sh").resolve()
SUCCESS = """<testsuite tests="2" failures="0" errors="0" skipped="0">
<testcase classname="com.dan.AcceptanceTest" name="shellPersists"/>
<testcase classname="com.dan.AcceptanceTest" name="wrapsAt80Columns"/>
</testsuite>"""
INSTALL_FAILURE = """Starting 0 tests on dan-api36-first(AVD) - 16
Failed to install split APK(s): [/tmp/app-debug.apk]
com.android.ddmlib.InstallException: 'package install-create -r -t' returns error 'Unknown failure: cmd: Can't find service: package'
"""


class AcceptanceRunnerTest(unittest.TestCase):
    def run_fixture(self, *, boot_ready="1", test_exit=0, transient_adb="0",
                    report=SUCCESS, output="", stale_report=None, run_retry=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bin_dir = root / "bin"
            bin_dir.mkdir()
            scripts = root / "scripts"
            scripts.mkdir()
            for name in ["run_android_acceptance.sh", "verify_android_acceptance.py"]:
                shutil.copy2(RUNNER.parent / name, scripts / name)
            (scripts / "android_acceptance_expected.txt").write_text(
                "com.dan.AcceptanceTest#shellPersists\ncom.dan.AcceptanceTest#wrapsAt80Columns\n")
            source = root / "app/src/androidTest/java/com/dan/AcceptanceTest.kt"
            source.parent.mkdir(parents=True)
            source.write_text("""package com.dan
class AcceptanceTest {
    @Test fun shellPersists() {}
    @Test
    fun wrapsAt80Columns() {}
}
""")
            results = root / "app/build/outputs/androidTest-results/connected"
            if stale_report:
                results.mkdir(parents=True)
                (results / "stale.xml").write_text(stale_report)
            (bin_dir / "adb").write_text("""#!/usr/bin/env bash
if [[ "$*" == 'shell getprop sys.boot_completed' ]]; then
  if [[ "$TRANSIENT_ADB" == 1 && ! -f adb-retried ]]; then touch adb-retried; exit 1; fi
  echo "$BOOT_READY"
elif [[ "$*" == 'shell pm path android' ]]; then echo 'package:/system/framework/framework-res.apk'
else echo 'fixture diagnostics'; fi
""")
            (bin_dir / "sleep").write_text("#!/usr/bin/env bash\nexit 0\n")
            (root / "gradlew").write_text("""#!/usr/bin/env python3
import os
import json
import shutil
from pathlib import Path
Path('gradle-called').touch()
report = os.environ['TEST_REPORT']
if report:
    target = Path('app/build/outputs/androidTest-results/connected/fixture/TEST-results.xml')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(report)
print(os.environ['TEST_OUTPUT'])
raise SystemExit(int(os.environ['TEST_EXIT']))
""")
            for file in [bin_dir / "adb", bin_dir / "sleep", root / "gradlew"]:
                file.chmod(0o755)
            env = dict(os.environ, PATH=f"{bin_dir}:{os.environ['PATH']}",
                       RUNNER_TEMP=str(root), BOOT_READY=boot_ready,
                       TEST_EXIT=str(test_exit), TRANSIENT_ADB=transient_adb,
                       TEST_REPORT=report or "", TEST_OUTPUT=output,
                       DAN_ACCEPTANCE_ATTEMPT="first")
            result = subprocess.run(["bash", str(scripts / "run_android_acceptance.sh")], cwd=root, env=env,
                                    capture_output=True, text=True, timeout=20)
            diag = root / "dan-terminal-emulator-diagnostics"
            retained = {}
            if run_retry:
                retry_env = dict(env, DAN_ACCEPTANCE_ATTEMPT="retry", TEST_EXIT="0",
                                 TEST_REPORT=SUCCESS, TEST_OUTPUT="fresh emulator")
                retry = subprocess.run(["bash", str(scripts / "run_android_acceptance.sh")], cwd=root, env=retry_env,
                                       capture_output=True, text=True, timeout=20)
                retained = {
                    "retry_exit": retry.returncode,
                    "first_log": (diag / "first/gradle.log").read_text(),
                    "retry_log": (diag / "retry/gradle.log").read_text(),
                    "first_reports": [p.read_text() for p in (diag / "first/androidTest-results").rglob("*.xml")],
                    "retry_reports": [p.read_text() for p in (diag / "retry/androidTest-results").rglob("*.xml")],
                    "first_retry": json.loads((diag / "first/result.json").read_text()).get("retry", False),
                    "retry_retry": json.loads((diag / "retry/result.json").read_text()).get("retry", False),
                }
            outcome = json.loads((diag / "first/result.json").read_text())
            return {
                "exit": result.returncode,
                "started": outcome.get("executed", 0) > 0,
                "retry": outcome.get("retry", False),
                "gradle": (root / "gradle-called").exists(),
                "diagnostics": bool(list(diag.rglob("logcat.txt"))),
                "output": result.stdout + result.stderr,
                **retained,
            }

    def test_ready_emulator_requires_the_complete_passing_suite(self):
        actual = self.run_fixture()
        self.assertEqual(0, actual["exit"], actual["output"])
        self.assertTrue(actual["started"])
        self.assertFalse(actual["retry"])
        self.assertTrue(actual["gradle"])
        self.assertTrue(actual["diagnostics"])

    def test_boot_timeout_allows_startup_retry_without_invoking_gradle(self):
        actual = self.run_fixture(boot_ready="0")
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["started"])
        self.assertFalse(actual["gradle"])
        self.assertTrue(actual["retry"])
        self.assertTrue(actual["diagnostics"])

    def test_package_service_loss_after_readiness_is_retryable(self):
        actual = self.run_fixture(test_exit=1, report='<testsuite tests="0"/>',
                                  output=INSTALL_FAILURE)
        self.assertEqual(1, actual["exit"])
        self.assertTrue(actual["gradle"])
        self.assertFalse(actual["started"])
        self.assertTrue(actual["retry"])

    def test_real_assertion_failure_preserves_exit_code_without_retry(self):
        report = SUCCESS.replace('name="shellPersists"/>',
                                 'name="shellPersists"><failure message="bad output"/></testcase>')
        actual = self.run_fixture(test_exit=7, report=report)
        self.assertEqual(7, actual["exit"])
        self.assertTrue(actual["started"])
        self.assertFalse(actual["retry"])

    def test_assertion_failure_cannot_be_hidden_by_package_service_error(self):
        report = '<testsuite tests="1"><testcase classname="com.dan.AcceptanceTest" name="shellPersists"><failure/></testcase></testsuite>'
        actual = self.run_fixture(test_exit=1, report=report, output=INSTALL_FAILURE)
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["retry"])

    def test_instrumentation_crash_before_first_test_is_not_retryable(self):
        actual = self.run_fixture(test_exit=1, report=None,
                                  output=INSTALL_FAILURE + "\nINSTRUMENTATION_RESULT: shortMsg=Process crashed.")
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["retry"])

    def test_zero_tests_cannot_pass_even_when_gradle_succeeds(self):
        actual = self.run_fixture(report='<testsuite tests="0"/>')
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["started"])
        self.assertFalse(actual["retry"])

    def test_partial_suite_cannot_pass(self):
        report = '<testsuite tests="1"><testcase classname="com.dan.AcceptanceTest" name="shellPersists"/></testsuite>'
        actual = self.run_fixture(report=report)
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["retry"])

    def test_skipped_test_cannot_pass(self):
        report = SUCCESS.replace('name="shellPersists"/>',
                                 'name="shellPersists"><skipped/></testcase>')
        self.assertNotEqual(0, self.run_fixture(report=report)["exit"])

    def test_failure_element_cannot_pass_when_gradle_returns_zero(self):
        report = SUCCESS.replace('name="shellPersists"/>',
                                 'name="shellPersists"><failure/></testcase>')
        self.assertNotEqual(0, self.run_fixture(report=report)["exit"])

    def test_suite_error_counter_cannot_pass_without_an_error_element(self):
        report = SUCCESS.replace('errors="0"', 'errors="1"')
        self.assertNotEqual(0, self.run_fixture(report=report)["exit"])

    def test_unreported_gradle_failure_is_not_retried(self):
        actual = self.run_fixture(test_exit=9, report=None, output="dependency resolution failed")
        self.assertEqual(9, actual["exit"])
        self.assertFalse(actual["started"])
        self.assertFalse(actual["retry"])

    def test_stale_passing_report_cannot_validate_an_empty_attempt(self):
        actual = self.run_fixture(report=None, stale_report=SUCCESS)
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["started"])

    def test_corrupt_report_is_neither_accepted_nor_retried(self):
        actual = self.run_fixture(test_exit=1, report='<testsuite', output=INSTALL_FAILURE)
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["retry"])

    def test_report_error_is_not_hidden_by_installation_log(self):
        report = '<testsuite tests="0" errors="1"><error message="instrumentation failed"/></testsuite>'
        actual = self.run_fixture(test_exit=1, report=report, output=INSTALL_FAILURE)
        self.assertFalse(actual["retry"])

    def test_declared_zero_total_cannot_validate_passing_cases(self):
        report = SUCCESS.replace('tests="2"', 'tests="0"')
        self.assertNotEqual(0, self.run_fixture(report=report)["exit"])

    def test_crash_log_cannot_be_hidden_by_passing_xml(self):
        actual = self.run_fixture(output="INSTRUMENTATION_RESULT: shortMsg=Process crashed.")
        self.assertNotEqual(0, actual["exit"])
        self.assertFalse(actual["retry"])

    def test_unrelated_failure_with_package_message_is_not_retryable(self):
        actual = self.run_fixture(test_exit=1, report=None,
                                  output="Dependency resolution failed: Can't find service: package")
        self.assertFalse(actual["retry"])

    def test_transient_adb_failure_is_retried_in_readiness_loop(self):
        actual = self.run_fixture(transient_adb="1")
        self.assertEqual(0, actual["exit"], actual["output"])
        self.assertTrue(actual["started"])
        self.assertTrue(actual["gradle"])

    def test_retry_retains_first_failure_evidence_without_reusing_its_state(self):
        actual = self.run_fixture(test_exit=1, report='<testsuite tests="0"/>',
                                  output=INSTALL_FAILURE, run_retry=True)
        self.assertEqual(1, actual["exit"])
        self.assertEqual(0, actual["retry_exit"])
        self.assertIn("Can't find service: package", actual["first_log"])
        self.assertEqual("fresh emulator\n", actual["retry_log"])
        self.assertEqual(['<testsuite tests="0"/>'], actual["first_reports"])
        self.assertEqual([SUCCESS], actual["retry_reports"])
        self.assertTrue(actual["first_retry"])
        self.assertFalse(actual["retry_retry"])


if __name__ == "__main__":
    unittest.main()
