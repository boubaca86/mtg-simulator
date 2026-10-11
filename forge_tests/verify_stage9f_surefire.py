#!/usr/bin/env python3
"""Require genuine Surefire execution evidence for Stage9F baseline and patched Forge.

The pinned baseline must fail exactly the blind-random assertion; the
disposable patched checkout must execute both methods with zero failures.
No test-annotation counts, fabricated XML, or policy-training seeds qualify.
"""
import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

CLASS = "forge.ai.WallOfMourningForgeEffectTest"
METHODS = {
    "returnAbilityRequiresBlindRandomSelection",
    "returnEffectMovesExactlyOneLinkedFaceDownExile",
}

def verify(folder: Path, mode: str) -> dict:
    seen = {}
    if not folder.is_dir():
        raise ValueError("Surefire reports directory missing")
    paths = sorted(folder.glob("TEST-*.xml"))
    if not paths:
        raise ValueError("zero Surefire reports")
    for path in paths:
        root = ET.parse(path).getroot()
        for node in root.iter():
            if node.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if node.get("classname") not in (CLASS, CLASS.rsplit(".", 1)[-1]):
                continue
            name = node.get("name")
            if not name or name in seen:
                raise ValueError(f"duplicate or anonymous test: {name!r}")
            errors = [n for n in node if n.tag.rsplit("}", 1)[-1] in ("failure", "error")]
            skipped = [n for n in node if n.tag.rsplit("}", 1)[-1] == "skipped"]
            seen[name] = {"errors": errors, "skipped": skipped}
    expected = {"returnAbilityRequiresBlindRandomSelection"} if mode == "baseline" else METHODS
    if set(seen) != expected:
        raise ValueError(f"missing or extra methods: found={sorted(seen)} expected={sorted(expected)}")
    if mode == "baseline":
        failure = seen["returnAbilityRequiresBlindRandomSelection"]["errors"]
        if len(failure) != 1 or seen["returnAbilityRequiresBlindRandomSelection"]["skipped"]:
            raise ValueError("baseline must execute and fail exactly once")
        info = str(failure[0].attrib) + (failure[0].text or "")
        if "Wall of Mourning requires blind referee randomness" not in info:
            raise ValueError("baseline failure must be the intended missing-random guard")
    else:
        for name, status in seen.items():
            if status["errors"] or status["skipped"]:
                raise ValueError(f"patched effect test not green: {name}")
    return {
        "gate": "PASS",
        "mode": mode,
        "executed": len(expected),
        "scope": "Pinned Forge named card ability and direct effect resolver, not end-step trigger or production policy",
    }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", required=True, type=Path)
    parser.add_argument("--mode", required=True, choices=["baseline", "patched"])
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports, args.mode), sort_keys=True))
    except (ValueError, OSError, ET.ParseError) as error:
        print(json.dumps({"gate": "FAIL", "reason": str(error)}), file=sys.stderr)
        raise SystemExit(1)
