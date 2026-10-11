#!/usr/bin/env python3
"""Fail closed unless exactly eleven real Forge Stage9M TestNG methods ran and passed."""
import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

CLASS = "forge.ai.IsolatedForgeCreaturePriorityTest"
EXPECTED = {
    "realPriorityRequiresTwoExternalActorPassesToAdvance",
    "realForgeTurnLoopAutomaticallyDrawsWithoutDevTurnSwitch",
    "realPriorityObservationOmitsForgedOpponentSecretsAndPhase",
    "realPriorityLoopAcceptsLandAndRejectsOutOfTurnProposal",
    "realLoopEnforcesOneLandThenResetsOnNextNaturalTurn",
    "illegalExtraActorCommandIsRejectedBeforeForgeCanChangeState",
    "landThenOneManaCreaturePaysForestAndWaitsForOpponentPriority",
    "twoManaCreaturePaysTwoForestsAndResolvesAfterPass",
    "insufficientManaCannotCastOrMutateHandLandOrStack",
    "opponentCannotCastSorcerySpeedCreatureOutsideOwnTurn",
    "forgedCreatureCommandsAndUnsupportedCardAreRejected",
}

def verify(folder: Path) -> dict:
    if not folder.is_dir():
        raise ValueError("Surefire reports missing")
    seen = {}
    for path in sorted(folder.glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter():
            if case.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if case.get("classname") not in (CLASS, CLASS.rsplit(".", 1)[-1]):
                continue
            name = case.get("name")
            if not name or name in seen:
                raise ValueError("duplicated or anonymous method")
            failures = [e for e in case if e.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")]
            seen[name] = failures
    if set(seen) != EXPECTED:
        raise ValueError(f"methods differ: found {sorted(seen)}, expected {sorted(EXPECTED)}")
    for name, problems in seen.items():
        if problems:
            raise ValueError(f"test failed or skipped: {name}")
    return {"gate":"PASS","executed_methods":len(EXPECTED),
            "scope":"Genuine Forge priority with isolated actor land and mana-paid creature stack validation",
            "full_magic_game":False}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports),sort_keys=True))
    except (OSError,ValueError,ET.ParseError) as exc:
        print(json.dumps({"gate":"FAIL","reason":str(exc)}), file=sys.stderr)
        sys.exit(1)
