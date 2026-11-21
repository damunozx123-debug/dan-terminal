#!/usr/bin/env python3
"""Analyze a completed test report, optionally publish a compact GitHub handoff.

Designed as a separate short-lived systemd transient unit after Gradle exits.
It never reruns tests or executes AI-generated code.
"""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys

from dan_ai_test_agent import handoff_text, local_ai, redact

EVIDENCE_ROOT = Path.home() / ".local/share/dan-os/test-reports/dan-terminal"

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--model", default="qwen2.5-coder:1.5b")
    parser.add_argument("--github-repo", default="damunozx123-debug/dan-terminal")
    parser.add_argument("--pr", type=int, default=20)
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()

    base = EVIDENCE_ROOT.resolve()
    output = args.evidence.expanduser().resolve()
    if output.parent != base or not output.is_dir():
        parser.error("Evidence must be one completed DAN Terminal test-run folder")
    report_path = output / "report.json"
    log = output / "gradle.log"
    if not report_path.is_file() or not log.is_file():
        parser.error("Missing report.json or gradle.log")
    report = json.loads(report_path.read_text())
    if report.get("schema") != "dan-ai-test-report/v1":
        parser.error("Unexpected report schema")
    if report.get("status") == "PASS":
        print("Tests passed; no failure analysis required.")
        return 0

    print("DAN-AI: analyzing completed build only. No tests restarted.", flush=True)
    analysis = local_ai(args.model, report, log)
    (output / "ai-analysis.txt").write_text(analysis + "\n")
    handoff = output / "chatgpt-handoff.md"
    handoff.write_text(handoff_text(report, analysis))
    print("Analysis saved:", output, flush=True)
    if args.publish:
        if not shutil.which("gh"):
            print("gh unavailable; handoff saved locally", file=sys.stderr)
            return 2
        try:
            result = subprocess.run(
                ["gh", "pr", "comment", str(args.pr), "--repo",
                 args.github_repo, "--body-file", str(handoff)],
                capture_output=True, text=True, timeout=45, check=False)
        except (OSError, subprocess.TimeoutExpired):
            print("GitHub handoff failed; local evidence preserved", file=sys.stderr)
            return 2
        if result.returncode:
            print("GitHub handoff failed: " + redact(result.stderr)[:250], file=sys.stderr)
            return 2
        print("GitHub handoff published.", flush=True)
    return 0

if __name__ == "__main__":
    sys.exit(main())
