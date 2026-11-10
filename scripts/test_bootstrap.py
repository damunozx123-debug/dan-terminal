#!/usr/bin/env python3
"""Exercise bootstrap completion/failure without installing real packages."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'app/src/main/assets/bootstrap/dan_bootstrap.sh'


class BootstrapTest(unittest.TestCase):
    def run_bootstrap(self, fail_on=None):
        temp = tempfile.TemporaryDirectory(prefix='dan-bootstrap-test-')
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        bin_dir = root / 'bin'
        bin_dir.mkdir()
        pkg = bin_dir / 'pkg'
        pkg.write_text('#!/bin/bash\nif [[ "$1" == "${FAIL_ON:-}" ]]; then echo "test package failure" >&2; exit 42; fi\n')
        pkg.chmod(0o755)
        env = dict(os.environ, HOME=str(root), PATH=f'{bin_dir}:{os.environ["PATH"]}', FAIL_ON=fail_on or '')
        result = subprocess.run(['bash', str(SCRIPT)], env=env, capture_output=True, text=True)
        return root, result, env

    def test_success_and_repeat_create_marker(self):
        root, result, env = self.run_bootstrap()
        self.assertEqual(0, result.returncode, result.stderr)
        marker = root / '.dan-terminal/etc/bootstrap.env'
        self.assertIn('DAN_BOOTSTRAP_VERSION=1', marker.read_text())
        again = subprocess.run(['bash', str(SCRIPT)], env=env, capture_output=True, text=True)
        self.assertEqual(0, again.returncode, again.stderr)
        self.assertIn('Bootstrap complete.', again.stdout)

    def test_failed_update_does_not_claim_completion(self):
        self.check_failure('update')

    def test_failed_install_does_not_claim_completion(self):
        self.check_failure('install')

    def check_failure(self, phase):
        root, result, _ = self.run_bootstrap(phase)
        self.assertEqual(42, result.returncode)
        self.assertIn('test package failure', result.stderr)
        self.assertIn('Bootstrap failed', result.stderr)
        self.assertNotIn('Bootstrap complete.', result.stdout)
        self.assertFalse((root / '.dan-terminal/etc/bootstrap.env').exists())


if __name__ == '__main__':
    unittest.main()
