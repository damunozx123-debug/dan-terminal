"""Exercise install/boot classification, XML acceptance and isolated retries."""
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('acceptance', SCRIPTS / 'verify_android_acceptance.py')
acceptance = importlib.util.module_from_spec(spec)
spec.loader.exec_module(acceptance)


class AcceptanceRunnerTest(unittest.TestCase):
    def run_fixture(self, boot_ready='1', test_exit='0', transient_adb='0', mode='pass', retry=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bin_dir, scripts = root / 'bin', root / 'scripts'
            bin_dir.mkdir()
            scripts.mkdir()
            for name in ['run_android_acceptance.sh', 'verify_android_acceptance.py']:
                shutil.copy2(SCRIPTS / name, scripts / name)
            (scripts / 'android_acceptance_expected.txt').write_text('test.First#works\ntest.Second#works\n')
            (bin_dir / 'adb').write_text('''#!/usr/bin/env bash
if [[ "$*" == 'shell getprop sys.boot_completed' ]]; then
  if [[ "$TRANSIENT_ADB" == 1 && ! -f adb-retried ]]; then touch adb-retried; exit 1; fi
  echo "$BOOT_READY"
elif [[ "$*" == 'shell pm path android' ]]; then echo 'package:/system/framework/framework-res.apk'
else echo 'fixture diagnostics'; fi
''')
            (bin_dir / 'sleep').write_text('#!/usr/bin/env bash\nexit 0\n')
            (root / 'gradlew').write_text('''#!/usr/bin/env python3
import os
from pathlib import Path
Path('gradle-called').touch()
mode = os.environ['MODE']
folder = Path('app/build/outputs/androidTest-results/connected/debug')
folder.mkdir(parents=True, exist_ok=True)
cases = '<testcase classname="test.First" name="works"/><testcase classname="test.Second" name="works"/>'
if mode == 'install':
    cases = ''
    print("Failed to install split APK(s): Unknown failure: cmd: Can't find service: package")
elif mode == 'started-offline':
    cases = ''
    print('Starting 2 tests on fixture')
    print('device offline')
elif mode == 'offline':
    cases = ''
    print('device offline')
elif mode == 'crash':
    cases = ''
    print('INSTRUMENTATION_FAILED: Process crashed')
elif mode == 'unknown':
    cases = ''
    print('Unclassified Gradle failure')
elif mode == 'zero': cases = ''
elif mode == 'missing': cases = '<testcase classname="test.First" name="works"/>'
elif mode == 'skipped': cases = cases.replace('name="works"/>', 'name="works"><skipped/></testcase>', 1)
elif mode == 'assertion': cases = cases.replace('name="works"/>', 'name="works"><failure>assertion failed</failure></testcase>', 1)
report = '<testsuite>' + cases + '</testsuite>'
if mode == 'malformed': report = '<testsuite'
(folder / 'TEST-fixture.xml').write_text(report)
raise SystemExit(int(os.environ['TEST_EXIT']))
''')
            for file in [bin_dir / 'adb', bin_dir / 'sleep', root / 'gradlew']:
                file.chmod(0o755)
            # A stale passing report must never fill in a missing current test.
            stale = root / 'app/build/outputs/androidTest-results/connected/debug/TEST-stale.xml'
            stale.parent.mkdir(parents=True)
            stale.write_text('<testsuite><testcase classname="stale" name="old"/></testsuite>')
            env = dict(os.environ, PATH=f"{bin_dir}:{os.environ['PATH']}", RUNNER_TEMP=str(root),
                       BOOT_READY=boot_ready, TEST_EXIT=str(test_exit), TRANSIENT_ADB=transient_adb, MODE=mode)
            result = subprocess.run(['bash', str(scripts / 'run_android_acceptance.sh')], cwd=root, env=env, capture_output=True, text=True)
            diag = root / 'dan-terminal-emulator-diagnostics/first'
            self.assertTrue((diag / 'logcat.txt').exists(), result.stderr)
            self.assertFalse((diag / 'tests-started').exists())
            outcome = json.loads((diag / 'result.json').read_text())
            outcome['exit'] = result.returncode
            outcome['gradle_called'] = (root / 'gradle-called').exists()
            if retry:
                first = (diag / 'result.json').read_text()
                env.update(MODE='pass', TEST_EXIT='0', DAN_ACCEPTANCE_ATTEMPT='retry')
                second = subprocess.run(['bash', str(scripts / 'run_android_acceptance.sh')], cwd=root, env=env, capture_output=True, text=True)
                self.assertEqual(0, second.returncode, second.stdout + second.stderr)
                self.assertEqual(first, (diag / 'result.json').read_text())
                self.assertTrue((diag / 'androidTest-results/debug/TEST-fixture.xml').exists())
                retry_diag = root / 'dan-terminal-emulator-diagnostics/retry'
                self.assertEqual('passed', json.loads((retry_diag / 'result.json').read_text())['status'])
            outcome['retry_classification'] = acceptance.retry_allowed(diag)
            return outcome

    def test_ready_emulator_requires_all_expected_tests(self):
        r = self.run_fixture()
        self.assertEqual(('passed', 0, 2, False), (r['status'], r['exit'], r['executed'], r['retry_classification']))

    def test_boot_timeout_never_invokes_gradle(self):
        r = self.run_fixture(boot_ready='0')
        self.assertEqual(('infrastructure-failure', 1, False, True), (r['status'], r['exit'], r['gradle_called'], r['retry_classification']))

    def test_package_service_loss_does_not_claim_tests_started(self):
        r = self.run_fixture(test_exit=7, mode='install')
        self.assertEqual(('infrastructure-failure', 7, 0, True), (r['status'], r['exit'], r['executed'], r['retry_classification']))

    def test_actual_assertion_failure_is_not_retried(self):
        r = self.run_fixture(test_exit=7, mode='assertion')
        self.assertEqual(('test-failure', 7, False), (r['status'], r['exit'], r['retry_classification']))

    def test_instrumentation_crash_before_first_test_is_not_retried(self):
        r = self.run_fixture(test_exit=7, mode='crash')
        self.assertEqual(('test-failure', 0, False), (r['status'], r['executed'], r['retry_classification']))

    def test_transient_adb_failure_is_retried_only_during_readiness(self):
        self.assertEqual('passed', self.run_fixture(transient_adb='1')['status'])

    def test_zero_tests_never_pass_even_with_gradle_exit_zero(self):
        r = self.run_fixture(mode='zero')
        self.assertEqual(('incomplete', 1, False), (r['status'], r['exit'], r['retry_classification']))

    def test_stale_reports_cannot_fill_missing_expected_tests(self):
        r = self.run_fixture(mode='missing')
        self.assertEqual((1, 1, ['test.Second#works']), (r['exit'], r['executed'], r['missing']))

    def test_skipped_tests_fail_acceptance(self):
        self.assertEqual(1, self.run_fixture(mode='skipped')['exit'])

    def test_malformed_report_fails_acceptance(self):
        r = self.run_fixture(mode='malformed')
        self.assertEqual(1, r['exit'])
        self.assertTrue(r['parse_errors'])
        self.assertFalse(r['retry_classification'])

    def test_unknown_gradle_failure_is_not_retried(self):
        self.assertFalse(self.run_fixture(test_exit=9, mode='unknown')['retry_classification'])

    def test_started_instrumentation_without_xml_is_not_retried(self):
        self.assertFalse(self.run_fixture(test_exit=7, mode='started-offline')['retry_classification'])

    def test_generic_offline_error_without_install_context_is_not_retried(self):
        self.assertFalse(self.run_fixture(test_exit=7, mode='offline')['retry_classification'])

    def test_fresh_retry_preserves_first_attempt_evidence(self):
        self.run_fixture(test_exit=7, mode='install', retry=True)

    def test_emulator_action_failure_before_runner_may_retry_once(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.assertTrue(acceptance.retry_allowed(root))
            (root / 'attempt-started').touch()
            self.assertFalse(acceptance.retry_allowed(root))

    def test_expected_manifest_matches_all_android_test_methods(self):
        found = set()
        for file in (SCRIPTS.parent / 'app/src/androidTest/java').rglob('*.kt'):
            source = file.read_text()
            package = re.search(r'^package\s+(\S+)', source, re.M).group(1)
            cls = re.search(r'\bclass\s+(\w+)', source).group(1)
            found.update(f'{package}.{cls}#{name}' for name in re.findall(r'@Test\s+fun\s+(\w+)\s*\(', source))
        expected = set((SCRIPTS / 'android_acceptance_expected.txt').read_text().splitlines())
        self.assertTrue(found)
        self.assertEqual(found, expected)


if __name__ == '__main__':
    unittest.main()
