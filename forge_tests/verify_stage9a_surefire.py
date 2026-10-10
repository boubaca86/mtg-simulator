#!/usr/bin/env python3
"""Fail-closed Surefire/TestNG evidence gate for Stage 9A Forge tests.

This verifies execution evidence; it does not execute Forge or prove game safety.
Use a FRESH forge-gui-desktop/target/surefire-reports directory per run.
"""

import argparse
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

TEST_CLASS = "forge.ai.ObservedOnlyNoninterferenceTest"
PAIRED = frozenset({
    "opponentHiddenHandAndLibraryDoNotChangeLiveDecision",
    "opponentFaceDownExileDoesNotChangeLiveDecision",
})
RED_REGRESSIONS = frozenset({
    "opponentUnseenTopLibraryNotEnumerated",
    "ownUnseenTopLibraryNotEnumerated",
    "opponentUnseenFaceDownExileNotEnumerated",
})
PUBLIC_CONTROL = "publicBattlefieldAndOwnHandRemainCandidates"
REQUIRED = PAIRED | RED_REGRESSIONS | {PUBLIC_CONTROL}


def _tag(element):
    return element.tag.rsplit("}", 1)[-1]


def inspect_reports(reports_dir: Path, mode: str):
    if mode not in ("baseline", "patched"):
        raise ValueError("mode must be baseline or patched")
    if not reports_dir.is_dir():
        raise ValueError(f"missing Surefire reports directory: {reports_dir}")
    reports = sorted(reports_dir.glob("TEST-*.xml"))
    if not reports:
        raise ValueError("no TEST-*.xml reports; a green Maven exit is insufficient")

    observed = {}
    provenance = {}
    for report in reports:
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError as exc:
            raise ValueError(f"invalid XML report {report}: {exc}") from exc
        for testcase in root.iter():
            if _tag(testcase) != "testcase":
                continue
            classname = testcase.attrib.get("classname", "")
            if classname not in (TEST_CLASS, TEST_CLASS.rsplit(".", 1)[-1]):
                continue
            name = testcase.attrib.get("name", "")
            if name in observed:
                raise ValueError(f"duplicate method {name}: {provenance[name]} and {report}")
            if not name:
                raise ValueError(f"unnamed TestNG case in {report}")
            kinds = [_tag(c) for c in testcase if _tag(c) in ("failure", "error", "skipped")]
            if len(kinds) > 1:
                raise ValueError(f"ambiguous result for {name}: {kinds}")
            observed[name] = kinds[0] if kinds else "pass"
            provenance[name] = str(report)

    missing = sorted(REQUIRED - observed.keys())
    extra = sorted(observed.keys() - REQUIRED)
    if missing or extra or len(observed) != 6:
        raise ValueError(f"must execute exactly six named TestNG methods; missing={missing}, extra={extra}, found={len(observed)}")
    if any(v in ("error", "skipped") for v in observed.values()):
        raise ValueError(f"infrastructure error or skipped test: {observed}")
    if mode == "baseline":
        unexpected = {n: observed[n] for n in RED_REGRESSIONS if observed[n] != "failure"}
        if unexpected:
            raise ValueError(f"baseline must demonstrate three known RED regressions: {unexpected}")
        if observed[PUBLIC_CONTROL] != "pass":
            raise ValueError("public-card control must pass even in baseline")
        # Paired tests may pass or fail on stock Forge; record, never assume.
    else:
        failed = {n: s for n, s in observed.items() if s != "pass"}
        if failed:
            raise ValueError(f"patched run must pass all six methods: {failed}")
    return {
        "mode": mode,
        "test_class": TEST_CLASS,
        "executed_methods": len(observed),
        "outcomes": dict(sorted(observed.items())),
        "gate": "PASS",
        "scope": "Surefire execution evidence only; not a global noninterference proof",
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, required=True,
                        help="Fresh Forge forge-gui-desktop/target/surefire-reports directory")
    parser.add_argument("--mode", choices=("baseline", "patched"), required=True)
    args = parser.parse_args(argv)
    try:
        result = inspect_reports(args.reports, args.mode)
    except (ValueError, OSError) as exc:
        print(json.dumps({"gate": "FAIL", "reason": str(exc)}, sort_keys=True), file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
