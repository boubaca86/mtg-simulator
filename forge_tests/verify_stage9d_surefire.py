#!/usr/bin/env python3
"""Strict execution-evidence gate for six real Forge Stage9D TestNG methods.

This checks actual Maven Surefire XML, NOT the number of source annotations.
It does not prove that Forge's production AI uses this test-only bridge.
"""
import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

TEST_CLASS = "forge.ai.BlindPileForgeBridgeTest"
REQUIRED = {
    "forgePlayPermissionDoesNotExposeHiddenCardIdentity",
    "publicActionDoesNotDependOnConcealedIdentity",
    "onlyFivePublicFieldsAreExposed",
    "refereeRandomizesOnlyAfterCosts",
    "actorRevisionAndSingleUseAreEnforced",
    "permissionRestrictionAndVisibilityFailClosed",
}

def verify(folder: Path):
    if not folder.is_dir():
        raise ValueError("missing Surefire report directory")
    files = sorted(folder.glob("TEST-*.xml"))
    if not files:
        raise ValueError("zero Surefire reports")
    observed = {}
    for path in files:
        root = ET.parse(path).getroot()
        for element in root.iter():
            if element.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if element.get("classname") not in (TEST_CLASS, TEST_CLASS.rsplit(".", 1)[-1]):
                continue
            method = element.get("name")
            if not method or method in observed:
                raise ValueError(f"duplicate or unnamed Stage9D TestNG method: {method!r}")
            issues = [x.tag.rsplit("}", 1)[-1] for x in element
                      if x.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")]
            observed[method] = ",".join(issues) if issues else "pass"
    if set(observed) != REQUIRED:
        raise ValueError(f"Stage9D needs exactly 6 executed methods;"
                         f" missing={sorted(REQUIRED - set(observed))};"
                         f" unexpected={sorted(set(observed) - REQUIRED)}")
    failed = {name: result for name, result in observed.items() if result != "pass"}
    if failed:
        raise ValueError(f"Stage9D regressions failed/skipped/errored: {failed}")
    return {
        "gate": "PASS", "executed_methods": 6, "outcomes": dict(sorted(observed.items())),
        "scope": "test-only Forge objects; not rules authorization or production integration",
    }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", required=True, type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports), indent=2, sort_keys=True))
    except (ValueError, OSError, ET.ParseError) as error:
        print(json.dumps({"gate": "FAIL", "reason": str(error)}), file=sys.stderr)
        raise SystemExit(1)
