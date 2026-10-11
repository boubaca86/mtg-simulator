#!/usr/bin/env python3
"""Require actual execution and success of all nine Stage9E Forge TestNG cases.

A Maven success with zero tests, a skipped test, or annotation count is not
security evidence. This verifier is intentionally independent of the test.
"""
import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

CLASS = "forge.ai.BlindPileStateIntegrityTest"
METHODS = {
    "publicChoiceIgnoresConcealedIdentities",
    "unauthorizedOrQualityRestrictedGrantIsDenied",
    "removedExileMemberInvalidatesGrantBeforeRandomness",
    "turnedFaceUpMemberInvalidatesGrantBeforeRandomness",
    "newLookPermissionInvalidatesBlindChoiceWithoutTimestampChange",
    "privatePileReplacementInvalidatesGrantWithoutEpochChange",
    "costsAndRollbackGateRefereeRandomness",
    "actorBindingAndSingleUsePreventReplays",
    "independentPublicPileGroupsNeverCrossSelect",
}

def verify(folder: Path) -> dict:
    if not folder.is_dir():
        raise ValueError("Surefire reports directory missing")
    reports = sorted(folder.glob("TEST-*.xml"))
    if not reports:
        raise ValueError("zero Surefire reports")
    found = {}
    for path in reports:
        root = ET.parse(path).getroot()
        for node in root.iter():
            if node.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if node.get("classname") not in (CLASS, CLASS.rsplit(".", 1)[-1]):
                continue
            name = node.get("name")
            if not name or name in found:
                raise ValueError(f"duplicate or unnamed TestNG method: {name!r}")
            problems = [child.tag.rsplit("}", 1)[-1] for child in node
                        if child.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")]
            found[name] = ",".join(problems) if problems else "pass"
    if set(found) != METHODS:
        raise ValueError(f"requires exactly nine methods; missing={sorted(METHODS - set(found))}; "
                         f"unexpected={sorted(set(found) - METHODS)}")
    failed = {name: outcome for name, outcome in found.items() if outcome != "pass"}
    if failed:
        raise ValueError(f"failed, errored or skipped: {failed}")
    return {
        "gate": "PASS", "executed_methods": 9,
        "outcomes": dict(sorted(found.items())),
        "scope": "Forge-backed test-only state integrity, not production card-effect legality",
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
