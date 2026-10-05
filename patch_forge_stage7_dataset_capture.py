from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("spell_picker", type=Path)
    args = parser.parse_args()
    path = args.spell_picker
    text = path.read_text(encoding="utf-8")

    marker = "public class SpellAbilityPicker {"
    replacement = marker + "\n    private static long stage7DecisionIndex = 0L;"
    text = replace_once(text, marker, replacement, "SpellAbilityPicker class declaration")

    old = """                bestSa = candidateSAs.get(bestIndex);
"""
    new = old + """                String actionIdentity = controller.getBestRootActionIdentity();
                if (Boolean.getBoolean("forge.expert.stage7.dataset") && actionIdentity != null) {
                    long decisionIndex = stage7DecisionIndex++;
                    String matchupId = System.getProperty("forge.expert.stage7.matchup", "unspecified");
                    String legalJson = LegalDecisionFeatures.export(
                            player, actionIdentity, samples, deterministicSimulationSeed(),
                            decisionIndex, matchupId);
                    System.out.println("EXPERT_STAGE7_DATA: " + legalJson);
                }
"""
    text = replace_once(text, old, new, "root executable action capture")
    path.write_text(text, encoding="utf-8")
    print(f"Patched Stage 7A legal dataset capture into {path}")


if __name__ == "__main__":
    main()
