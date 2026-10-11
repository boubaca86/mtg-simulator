"""Require the 23 inherited regressions and eight trigger cases, all passing."""
import argparse
import json
from pathlib import Path
import sys
from xml.etree import ElementTree as ET

from forge_tests.verify_stage9t_surefire import EXPECTED as REGRESSIONS

CLASS = "forge.ai.Stage9UTriggerBoundaryTest"
TRIGGERS = {
    "mandatoryLifeTriggerUsesRealStackAndBothActorsPriority",
    "optionalLifeTriggerStacksThenStopsAtResolution",
    "mandatoryLifeTriggerSurvivesSourceLeavingBattlefield",
    "triggerDecisionsAndMandatoryEffectsIgnoreOpponentSecrets",
    "optionalTriggerCannotBeRelabeledOrExecutedWithoutChoice",
    "noStackTriggerGuardRejectsCostsChainsAndForeignControllers",
    "multipleTriggerOrderingStopsBeforeAnyStackMutation",
    "staticTriggerAndTargetSetupCannotReachInheritedAi",
}
EXPECTED = REGRESSIONS | TRIGGERS


def verify(folder: Path) -> dict:
    if not folder.is_dir():
        raise ValueError("Surefire reports missing")
    seen = set()
    for path in sorted(folder.glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter():
            if case.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if case.get("classname") != CLASS:
                raise ValueError("unexpected test class")
            name = case.get("name")
            if not name or name in seen:
                raise ValueError("duplicate or anonymous test method")
            if any(child.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")
                   for child in case):
                raise ValueError(f"test failed or skipped: {name}")
            seen.add(name)
    if seen != EXPECTED:
        raise ValueError(f"methods differ: missing={sorted(EXPECTED - seen)}, extra={sorted(seen - EXPECTED)}")
    return {"gate": "PASS", "test_class": CLASS, "executed_methods": len(seen),
            "inherited_regressions": len(REGRESSIONS), "trigger_cases": len(TRIGGERS),
            "scope": "Test-only bounded life triggers, explicit unsupported-decision failures",
            "full_magic_game": False, "reserved_seed_games_run": False}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports), sort_keys=True))
    except (OSError, ValueError, ET.ParseError) as exc:
        print(json.dumps({"gate": "FAIL", "reason": str(exc)}), file=sys.stderr)
        sys.exit(1)
