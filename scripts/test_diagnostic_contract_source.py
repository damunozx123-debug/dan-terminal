#!/usr/bin/env python3
"""Offline G2 source/contract checks. These are NOT Android execution evidence."""
import copy
import hashlib
import json
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/diagnostics"
SCHEMA_PATH = ASSETS / "diagnostic-report.schema.json"
EXPECTED_GIT_BLOB = "ca6d8784be1c29af6031906fe156ebc983d28ab6"
MAX_BYTES = 65_536


def git_blob_sha(raw):
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\\0" + raw).hexdigest()


schema_bytes = SCHEMA_PATH.read_bytes()
assert git_blob_sha(schema_bytes) == EXPECTED_GIT_BLOB, "v1.0.0 schema drift from pinned Net Intel contract"
schema = json.loads(schema_bytes)
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema, format_checker=FormatChecker())


def validate(obj):
    raw = json.dumps(obj, separators=(",", ":")).encode("utf-8")
    assert len(raw) <= MAX_BYTES
    validator.validate(obj)


for name in ["simulated-connectivity.json", "missing-connectivity.json"]:
    report = json.loads((ASSETS / "fixtures" / name).read_text(encoding="utf-8"))
    validate(report)
    assert report["measurement_origin"] == "SIMULATED"
    assert report["schema_version"] == "1.0.0"

report = json.loads((ASSETS / "fixtures/simulated-connectivity.json").read_text(encoding="utf-8"))
bad = copy.deepcopy(report)
bad["measurements"][0]["value"] = None
try:
    validate(bad)
except Exception:
    pass
else:
    raise AssertionError("Measured null value was accepted")

bad = copy.deepcopy(report)
bad["pairing_token"] = "should never be allowed"
try:
    validate(bad)
except Exception:
    pass
else:
    raise AssertionError("Unexpected cross-app secret field was accepted")

source = (ROOT / "app/src/main/java/com/dan/terminal/feature/reports/DiagnosticReportImporter.kt").read_text()
assert "enforceStrictJsonAndUniqueKeys" in source
assert "Duplicate JSON field" in source
assert 'val output = File(dir, "${preview.id}.json")' in source
assert "Only shared content:// reports are accepted" in source
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
assert 'android.intent.action.SEND' in manifest
assert 'application/vnd.dan.diagnostic-report+json' in manifest
print("G2 schema source checks passed (no Android runtime exercised)")
