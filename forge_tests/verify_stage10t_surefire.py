"""Require all 78 Stage10S regressions and six own-hand/native-game methods."""
import argparse
import json
from pathlib import Path
import sys
from xml.etree import ElementTree as ET
from forge_tests.verify_stage10s_surefire import EXPECTED as PREVIOUS, cases

CLASS = "forge.ai.Stage10TOwnHandCleanupTest"
NEW = {
    "ownHandPreferenceUsesIndependentWorkersAndIgnoresPrivateWorlds",
    "malformedHandPreferencesCannotChooseDuplicateHiddenOrNonintegerCards",
    "cleanupRejectsWrongCountTurnAndPhaseBeforeActorChoice",
    "staleAndForeignHandBindingsAreRejectedBeforeAnyDiscard",
    "nativeCleanupDiscardsActorSelectedOwnCardsAndResumesTheTurnLoop",
    "uncuratedNativeSixtyCardGameEndsWithForgeOwnedOutcome",
}
EXPECTED = PREVIOUS | NEW


def verify(folder):
    seen = cases(folder, expected_class=CLASS)
    if set(seen) != EXPECTED:
        raise ValueError(f"methods differ: missing={sorted(EXPECTED-seen.keys())}, extra={sorted(seen.keys()-EXPECTED)}")
    if any(seen.values()):
        raise ValueError("failed, errored or skipped method")
    return {"gate": "PASS", "test_class": CLASS, "executed_methods": len(seen),
            "previous_regressions": len(PREVIOUS), "new_methods": len(NEW),
            "uncurated_supported_fixture": True, "arbitrary_deck_support": False,
            "reserved_seed_games_run": False}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports), sort_keys=True))
    except (OSError, ValueError, ET.ParseError) as exc:
        print(json.dumps({"gate": "FAIL", "reason": str(exc)}), file=sys.stderr)
        sys.exit(1)
