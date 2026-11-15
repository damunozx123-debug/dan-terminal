#!/usr/bin/env python3
"""Gate Android acceptance on real test cases, not on Gradle being invoked."""
from __future__ import annotations

import argparse
from collections import Counter
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


INSTALL_INFRASTRUCTURE = re.compile(
    r"Can't find service: package|device offline|no devices/emulators found|device ['\"]?[^\n]+['\"]? not found",
    re.IGNORECASE,
)
INSTRUMENTATION_FAILURE = re.compile(
    r"INSTRUMENTATION_FAILED|Process crashed|java\.lang\.AssertionError|INSTRUMENTATION_RESULT:\s*shortMsg=",
    re.IGNORECASE,
)
INSTALL_CONTEXT = re.compile(
    r"Failed to install(?: split)? APK|(?:com\.android\.ddmlib\.)?InstallException|package install-(?:create|write|commit)|\badb(?:\s+-s\s+\S+)?\s+install\b",
    re.IGNORECASE,
)
INSTRUMENTATION_STARTED = re.compile(
    r"\bStarting\s+[1-9]\d*\s+tests?\s+on\b|\bINSTRUMENTATION_(?:STATUS|RESULT)(?:_CODE)?:|\bam\s+instrument\b|\bTestRunner:\s*(?:started|run started)|\bInstrumentation started\b",
    re.IGNORECASE,
)


def verify(results: Path, expected_file: Path, log_file: Path, gradle_exit: int) -> dict:
    expected = {line.strip() for line in expected_file.read_text().splitlines()
                if line.strip() and not line.lstrip().startswith('#')}
    if not expected or any('#' not in name for name in expected):
        raise ValueError('Expected Android test manifest is empty or malformed')
    cases = []
    suite_problems = 0
    parse_errors = []
    evidence = log_file.read_text(errors='replace') if log_file.exists() else ''
    for report in sorted(results.rglob('*.xml')) if results.exists() else []:
        text = report.read_text(errors='replace')
        evidence += '\n' + text
        try:
            root = ET.fromstring(text)
            cases.extend(root.iter('testcase'))
            suite_problems += any(node.tag in ('failure', 'error', 'skipped') for node in root.iter())
            for suite in root.iter('testsuite'):
                suite_problems += sum(int(suite.get(key, '0')) for key in ('failures', 'errors', 'skipped'))
                if suite.get('tests') is not None and int(suite.get('tests')) != len(list(suite.iter('testcase'))):
                    parse_errors.append(f'{report.name}: declared testcase count does not match actual cases')
        except (ET.ParseError, ValueError) as error:
            parse_errors.append(f'{report.name}: {error}')
    names = [f"{case.get('classname', '')}#{case.get('name', '')}" for case in cases]
    actual = set(names)
    failed = sum(case.find('failure') is not None or case.find('error') is not None for case in cases)
    skipped = sum(case.find('skipped') is not None for case in cases)
    duplicates = sorted(name for name, count in Counter(names).items() if count != 1)
    complete = actual == expected and bool(cases) and not duplicates
    crashed = bool(INSTRUMENTATION_FAILURE.search(evidence))
    status, reason, retry = 'incomplete', 'Required Android tests did not all execute', False
    if not gradle_exit and complete and not (failed or skipped or suite_problems or parse_errors or crashed):
        status, reason = 'passed', 'Every expected Android test passed'
    elif failed or skipped or cases or suite_problems or crashed or INSTRUMENTATION_STARTED.search(evidence):
        status, reason = 'test-failure', 'Tests ran, were skipped, or instrumentation failed; preserve the failure'
    elif gradle_exit and not parse_errors and INSTALL_CONTEXT.search(evidence) and INSTALL_INFRASTRUCTURE.search(evidence):
        status, reason, retry = 'infrastructure-failure', 'Known install/ADB failure before any test executed', True
    return dict(status=status, reason=reason, retry=retry, gradle_exit=gradle_exit,
                expected=len(expected), executed=len(cases), failed=failed, skipped=skipped,
                missing=sorted(expected - actual), unexpected=sorted(actual - expected),
                duplicates=duplicates, parse_errors=parse_errors)


def retry_allowed(diagnostics: Path) -> bool:
    outcome = diagnostics / 'result.json'
    if outcome.exists():
        try:
            result = json.loads(outcome.read_text())
            return result.get('status') == 'infrastructure-failure' and result.get('retry') is True
        except (ValueError, OSError):
            return False
    # The emulator action failed before our script was invoked (boot/setup failure).
    # Once our script has started, an unclassified failure must never be retried.
    return not (diagnostics / 'attempt-started').exists()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    gate = commands.add_parser('verify')
    gate.add_argument('--results', type=Path, required=True)
    gate.add_argument('--expected', type=Path, required=True)
    gate.add_argument('--log', type=Path, required=True)
    gate.add_argument('--gradle-exit', type=int, required=True)
    gate.add_argument('--output', type=Path, required=True)
    classify = commands.add_parser('classify')
    classify.add_argument('--diagnostics', type=Path, required=True)
    classify.add_argument('--github-output', type=Path, required=True)
    args = parser.parse_args()
    if args.command == 'classify':
        retry = retry_allowed(args.diagnostics)
        with args.github_output.open('a') as output:
            output.write(f"retry={'true' if retry else 'false'}\n")
        print('Retry one fresh emulator.' if retry else 'Preserve the test or unclassified failure; no retry.')
        return 0
    result = verify(args.results, args.expected, args.log, args.gradle_exit)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(f"Android acceptance: {result['status']}; {result['executed']}/{result['expected']} tests. {result['reason']}")
    return 0 if result['status'] == 'passed' else 1


if __name__ == '__main__':
    raise SystemExit(main())
