#!/usr/bin/env python3
"""Offline structural checks for the reconstructed DAN Terminal source tree."""

from __future__ import annotations

import re
import tomllib
from pathlib import Path
from xml.etree import ElementTree


ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")


catalog = tomllib.loads((ROOT / "gradle/libs.versions.toml").read_text())
require("mockito" in catalog["versions"], "Mockito version is not pinned")
require(
    "mockito-core" in catalog["libraries"],
    "Mockito core alias is missing from the version catalog",
)
require(
    "testImplementation(libs.mockito.core)"
    in (ROOT / "app/build.gradle.kts").read_text(),
    "Mockito is not on the app unit-test classpath",
)
require(
    (ROOT / "app/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker")
    .read_text()
    .strip()
    == "mock-maker-inline",
    "Mockito inline mock-maker configuration is missing",
)

xml_files = list(ROOT.rglob("*.xml"))
for xml_file in xml_files:
    ElementTree.parse(xml_file)

tracker = (ROOT / "docs/REQUIREMENT_TRACKER.md").read_text().splitlines()
tracker_rows = [
    line
    for line in tracker
    if line.startswith("| ") and not line.startswith("| ID |") and not line.startswith("|---")
]
require(len(tracker_rows) == 95, f"Expected 95 requirements, found {len(tracker_rows)}")
require(
    len({line.split("|")[1].strip() for line in tracker_rows}) == 95,
    "Requirement IDs are not unique",
)

main_sources = (
    path.read_text(errors="ignore")
    for module in ("app", "core", "feature")
    for path in (ROOT / module).rglob("src/main/**/*.kt")
)
debug_patterns = re.compile(r"\bprintln\s*\(|\bLog\.d\s*\(")
require(
    not any(debug_patterns.search(source) for source in main_sources),
    "Debug println or Log.d call exists in a main source set",
)

ignore = (ROOT / ".gitignore").read_text()
for pattern in ("local.properties", "*.jks", "*.keystore", "**/build/"):
    require(pattern in ignore, f"Required Git ignore entry is missing: {pattern}")

required_files = (
    "settings.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/res/xml/data_extraction_rules.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "core/security/src/main/java/com/dan/terminal/core/security/AccessRequiredDialog.kt",
    "core/shizuku/src/main/java/com/dan/terminal/core/shizuku/ShizukuManager.kt",
    "core/bluetooth/src/main/java/com/dan/terminal/core/bluetooth/GattInspector.kt",
    "feature/termux/src/main/java/com/dan/terminal/feature/termux/TermuxBridge.kt",
)
for relative_path in required_files:
    require((ROOT / relative_path).is_file(), f"Required source file is missing: {relative_path}")

print(f"TOML parsed: {ROOT / 'gradle/libs.versions.toml'}")
print(f"XML parsed: {len(xml_files)} files")
print("Requirement tracker: 95 unique canonical IDs")
print("Mockito alias and inline mock maker: present")
print("Main source sets: no println or Log.d calls")
print("Git ignore: local.properties and keystores excluded")
print("Structural source checks: PASS")

termux_bridge = (
    ROOT / "feature/termux/src/main/java/com/dan/terminal/feature/termux/TermuxBridge.kt"
).read_text()
require(
    "putExtra(EXTRA_COMMAND_PATH, request.executablePath)" in termux_bridge,
    "RUN_COMMAND_PATH must receive the executable path",
)
require(
    "putExtra(EXTRA_ARGUMENTS, request.arguments.toTypedArray())" in termux_bridge,
    "RUN_COMMAND_ARGUMENTS must receive a separate String array",
)
print("Termux RUN_COMMAND source contract: executable path and arguments are separate")
print("Termux device execution and HyperOS behavior: NOT VERIFIED")
