#!/usr/bin/env python3
"""Offline G2 source/contract checks. These are NOT Android execution evidence."""
import copy
from datetime import datetime
import hashlib
import json
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker, ValidationError

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/diagnostics"
SCHEMA_PATH = ASSETS / "diagnostic-report.schema.json"
EXPECTED_GIT_BLOB = "ca6d8784be1c29af6031906fe156ebc983d28ab6"
MAX_BYTES = 65_536
MAX_DEPTH = 16
MIME = "application/vnd.dan.diagnostic-report+json"


def reject_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate JSON field")
        result[key] = value
    return result


def reject_constant(token):
    raise ValueError(f"non-finite JSON number: {token}")


def nesting_depth(value):
    if isinstance(value, dict):
        return 1 + max((nesting_depth(item) for item in value.values()), default=0)
    if isinstance(value, list):
        return 1 + max((nesting_depth(item) for item in value), default=0)
    return 0


def git_blob_sha(raw):
    # Git hashes the LF-normalized blob even when Windows checks out CRLF text.
    raw = raw.replace(b"\r\n", b"\n")
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + bytes([0]) + raw).hexdigest()


schema_bytes = SCHEMA_PATH.read_bytes()
assert git_blob_sha(schema_bytes) == EXPECTED_GIT_BLOB, "v1.0.0 schema drift from pinned Net Intel contract"
schema = json.loads(schema_bytes)
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema, format_checker=FormatChecker())


def validate(obj):
    validator.validate(obj)
    measurements = [item["id"] for item in obj["measurements"]]
    assert len(measurements) == len(set(measurements)), "duplicate measurement ID"
    finding_ids = [item["id"] for item in obj["findings"]]
    assert len(finding_ids) == len(set(finding_ids)), "duplicate finding ID"
    for finding in obj["findings"]:
        assert set(finding["supporting_measurement_ids"]).issubset(set(measurements)), \
            "finding references an unknown measurement"
    datetime.fromisoformat(obj["created_at_utc"].replace("Z", "+00:00"))


def parse_wire(raw):
    if not (0 < len(raw) <= MAX_BYTES):
        raise ValueError("invalid report payload size")
    report = json.loads(
        raw.decode("utf-8", "strict"),
        object_pairs_hook=reject_duplicate_keys,
        parse_constant=reject_constant,
    )
    if nesting_depth(report) > MAX_DEPTH:
        raise ValueError("report nesting exceeds limit")
    validate(report)
    return report


def assert_rejected(raw):
    try:
        parse_wire(raw)
    except (ValueError, TypeError, UnicodeDecodeError, ValidationError):
        return
    raise AssertionError("invalid DiagnosticReport payload was accepted")


def assert_contract_rejected(obj):
    try:
        validate(obj)
    except (AssertionError, TypeError, ValueError, ValidationError):
        return
    raise AssertionError("invalid DiagnosticReport contract was accepted")


for name in ["simulated-connectivity.json", "missing-connectivity.json"]:
    report = parse_wire((ASSETS / "fixtures" / name).read_bytes())
    assert report["measurement_origin"] == "SIMULATED"
    assert report["schema_version"] == "1.0.0"

report = json.loads((ASSETS / "fixtures/simulated-connectivity.json").read_text(encoding="utf-8"))
bad = copy.deepcopy(report)
bad["measurements"][0]["value"] = None
assert_contract_rejected(bad)

bad = copy.deepcopy(report)
bad["pairing_token"] = "should never be allowed"
assert_contract_rejected(bad)

for raw in [
    b"",
    b"x" * (MAX_BYTES + 1),
    b"\xff",  # invalid UTF-8
    b'{"x":"\\q"}',  # invalid JSON escape
    b'{"schema_version":"1.0.0","schema_version":"1.0.0"}',
    br'{"x":1,"\u0078":2}',  # duplicate key after JSON escape decoding
    b'{"x":NaN}',
    b'{"x":Infinity}',
    b"{}{}",  # trailing JSON value
    b'{"x":' + b"[" * 17 + b"0" + b"]" * 17 + b"}",
]:
    assert_rejected(raw)

bad = copy.deepcopy(report)
bad["measurements"].append(copy.deepcopy(bad["measurements"][0]))
assert_contract_rejected(bad)

bad = copy.deepcopy(report)
bad["findings"][0]["supporting_measurement_ids"].append("unknown_measurement")
assert_contract_rejected(bad)

bad = copy.deepcopy(report)
bad["created_at_utc"] = "2026-25-99T00:00:00Z"
try:
    parse_wire(json.dumps(bad).encode("utf-8"))
except (ValueError, ValidationError):
    pass
else:
    raise AssertionError("Impossible UTC timestamp was accepted")

source = (ROOT / "app/src/main/java/com/dan/terminal/feature/reports/DiagnosticReportImporter.kt").read_text()
assert "enforceStrictJsonAndUniqueKeys" in source
assert "Duplicate JSON field" in source
assert 'val validated = parseValidatedBytes(preview.bytes)' in source
assert 'val output = File(dir, "${validated.id}.json")' in source
assert "Only shared content:// reports are accepted" in source
assert f'const val MIME = "{MIME}"' in source
assert "Report URI is unavailable or permission expired" in source
assert "data.size() + n <= MAX_BYTES" in source
assert "CodingErrorAction.REPORT" in source
assert "reader.isLenient = false" in source
assert "require(names.add(name))" in source
assert 'reader.peek() == JsonToken.END_DOCUMENT' in source
activity = (ROOT / "app/src/main/java/com/dan/terminal/MainActivity.kt").read_text()
assert "Intent.ACTION_SEND" in activity and f'candidate.type != "{MIME}"' in activity
assert "Intent.EXTRA_STREAM" in activity and 'uri?.scheme == "content"' in activity
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
assert 'android.intent.action.SEND' in manifest
assert MIME in manifest
print("G2 contract, URI boundary and negative-path source checks passed (no Android runtime exercised)")

