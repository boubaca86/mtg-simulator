#!/usr/bin/env python3
"""Stage 9C real-Forge face-down exile permission evidence gate.

Reject missing, extra, skipped, duplicate, failed or errored TestNG methods.
Source annotation counts and Maven exit alone are not execution evidence.
"""
import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

TEST_CLASS = "forge.ai.FaceDownExilePermissionBoundaryTest"
REQUIRED = {
    "opponentConcealedExileWithoutLookIsNotCandidate",
    "explicitLookPermissionMakesConcealedExileCandidate",
    "exileLookPermissionPersistsAfterTemporaryGrantExpires",
    "faceUpOpponentExileRemainsPublicCandidate",
    "ownFaceDownExileNotVisibleSolelyBecauseOfOwnership",
    "lookPermissionDoesNotCreatePlayPermission",
}

def verify(reports: Path) -> dict:
    if not reports.is_dir():
        raise ValueError("missing surefire report directory")
    xmls = sorted(reports.glob("TEST-*.xml"))
    if not xmls:
        raise ValueError("no Surefire XML: reject false-green zero-test build")
    outcomes = {}
    for path in xmls:
        root = ET.parse(path).getroot()
        for node in root.iter():
            if node.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if node.get("classname") not in (TEST_CLASS, TEST_CLASS.rsplit(".", 1)[-1]):
                continue
            name = node.get("name")
            if not name or name in outcomes:
                raise ValueError(f"duplicate/anonymous TestNG method: {name!r}")
            problems = [child.tag.rsplit("}", 1)[-1] for child in node
                        if child.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")]
            outcomes[name] = ",".join(problems) if problems else "pass"
    if set(outcomes) != REQUIRED:
        raise ValueError("exactly six named Forge cases required;"
                         f" missing={sorted(REQUIRED - set(outcomes))};"
                         f" extra={sorted(set(outcomes) - REQUIRED)}")
    failures = {name: state for name, state in outcomes.items() if state != "pass"}
    if failures:
        raise ValueError(f"Stage9C Forge tests failed/skipped: {failures}")
    return {"gate": "PASS", "executed": 6, "outcomes": dict(sorted(outcomes.items())),
            "scope": "Forge visibility permission fixture, NOT spell legality or blind pile production integration"}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", required=True, type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports), indent=2, sort_keys=True))
    except (ValueError, OSError, ET.ParseError) as error:
        print(json.dumps({"gate": "FAIL", "reason": str(error)}), file=sys.stderr)
        raise SystemExit(1)
