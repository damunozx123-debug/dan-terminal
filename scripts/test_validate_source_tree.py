"""Exercise source validation against real source and disposable build output."""
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class SourceValidationTest(unittest.TestCase):
    def check_tree(self, extra_path, contents):
        with tempfile.TemporaryDirectory() as folder:
            tree = Path(folder) / "source"
            shutil.copytree(ROOT, tree, ignore=shutil.ignore_patterns(
                ".git", ".gradle", ".kotlin", "build", "__pycache__"))
            extra = tree / extra_path
            extra.parent.mkdir(parents=True, exist_ok=True)
            extra.write_text(contents)
            return subprocess.run(
                [sys.executable, str(tree / "scripts/validate_source_tree.py")],
                text=True, capture_output=True)

    def test_generated_empty_xml_does_not_fail_source_validation(self):
        result = self.check_tree("app/build/intermediates/lint/resources.xml", "")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_malformed_source_xml_still_fails(self):
        result = self.check_tree("app/src/main/res/xml/broken.xml", "<broken>")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("ParseError", result.stderr)

    def test_asset_named_build_is_still_validated(self):
        result = self.check_tree("app/src/main/assets/build/broken.xml", "<broken>")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("ParseError", result.stderr)


if __name__ == "__main__":
    unittest.main()
