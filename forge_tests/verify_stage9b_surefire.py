#!/usr/bin/env python3
"""Fail-closed evidence gate for two independent Stage 9B real-Forge tests.

Do not infer correctness from Maven exit code or empty Surefire directories.
This is NOT a substitute for a full private-information or legality audit.
"""

import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

CLASS = "forge.ai.FaceDownBattlefieldPrivacyTest"
REQUIRED = {
    "opponentFaceDownBattlefieldIdentityDoesNotChangeLiveDecision",
    "opponentFaceDownBattlefieldDoesNotOfferPrivateTurnUpAbility",
}


def check_reports(directory: Path):
    if not directory.is_dir():
        raise ValueError(f"missing report directory: {directory}")
    paths = sorted(directory.glob("TEST-*.xml"))
    if not paths:
        raise ValueError("zero reports: a green build with no tests is insufficient")
    outcomes = {}
    for path in paths:
        root = ET.parse(path).getroot()
        for case in root.iter():
            if case.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if case.get("classname") not in (CLASS, CLASS.split(".")[-1]):
                continue
            name = case.get("name")
            if not name or name in outcomes:
                raise ValueError(f"duplicate/unnamed Stage9B method: {name!r}")
            problems = [
                child.tag.rsplit("}", 1)[-1]
                for child in case
                if child.tag.rsplit("}", 1)[-1] in {"failure", "error", "skipped"}
            ]
            if problems:
                outcomes[name] = ",".join(problems)
            else:
                outcomes[name] = "pass"

    if set(outcomes) != REQUIRED:
        raise ValueError(
            f"expected exactly two executed Stage9B methods; "
            f"missing={sorted(REQUIRED - set(outcomes))}, "
            f"unexpected={sorted(set(outcomes) - REQUIRED)}"
        )
    failed = {name: value for name, value in outcomes.items() if value != "pass"}
    if failed:
        raise ValueError(f"Stage9B failures/skips/errors: {failed}")
    return {
        "gate": "PASS",
        "executed_methods": 2,
        "scope": "Stage9B Forge face-down battlefield candidate/decision tests only",
        "outcomes": dict(sorted(outcomes.items())),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", required=True, type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(check_reports(args.reports), sort_keys=True, indent=2))
        return 0
    except (ValueError, OSError, ET.ParseError) as err:
        print(json.dumps({"gate": "FAIL", "reason": str(err)}), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
