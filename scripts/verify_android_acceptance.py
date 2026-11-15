#!/usr/bin/env python3
"""Require fresh JUnit evidence; retry only known pre-instrumentation failures."""
import argparse
from pathlib import Path
import re
from xml.etree import ElementTree


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("results", "sources", "log", "diagnostics"):
        parser.add_argument(f"--{name}", type=Path, required=True)
    parser.add_argument("--gradle-exit", type=int, required=True)
    args = parser.parse_args()

    # This suite uses ordinary Kotlin JUnit4 classes, without parameterization.
    # Discover method identities so an older/partial report cannot pass a newer tree.
    expected = set()
    for source in args.sources.rglob("*.kt"):
        text = source.read_text()
        package = re.search(r"^package\s+([\w.]+)", text, re.MULTILINE)
        test_class = re.search(r"\bclass\s+(\w+)", text)
        methods = re.findall(r"@Test\s+fun\s+(\w+)\s*\(", text)
        if methods and package and test_class:
            expected.update((f"{package[1]}.{test_class[1]}", name) for name in methods)

    cases = []
    invalid = False
    failed = False
    for report in args.results.rglob("*.xml"):
        try:
            root = ElementTree.parse(report).getroot()
            cases.extend(root.iter("testcase"))
            failed |= any(node.tag in ("failure", "error", "skipped") for node in root.iter())
            for suite in root.iter("testsuite"):
                failed |= any(int(suite.get(key, "0")) != 0 for key in ("failures", "errors", "skipped"))
        except (ElementTree.ParseError, OSError, ValueError) as error:
            print(f"::error::Invalid Android report {report}: {error}")
            invalid = True

    log = args.log.read_text(errors="replace")
    started = bool(cases) or bool(re.search(
        r"Starting \d+ tests on|INSTRUMENTATION_(?:STATUS|RESULT|FAILED)\b|Process crashed",
        log, re.IGNORECASE,
    ))
    if started:
        (args.diagnostics / "tests-started").touch()

    # An app assertion/crash, corrupt report, or unknown Gradle failure stays red.
    # The known install error can occur after our initial readiness check.
    if args.gradle_exit and not started and not invalid and "Can't find service: package" in log:
        (args.diagnostics / "retryable-infrastructure").touch()
        print("::error::Package Manager failed before instrumentation; fresh-emulator retry allowed.")
        return args.gradle_exit

    actual = {(case.get("classname"), case.get("name")) for case in cases}
    missing = expected - actual
    if args.gradle_exit or invalid or failed or not expected or missing:
        print(f"::error::Android acceptance incomplete/failed: {len(actual)} observed, {len(expected)} expected.")
        for class_name, name in sorted(missing):
            print(f"Missing: {class_name}.{name}")
        return args.gradle_exit or 1

    print(f"Android acceptance: all {len(expected)} expected tests passed without skips.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
