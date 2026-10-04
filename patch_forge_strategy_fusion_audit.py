from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch_controller(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    marker = """    public Score getBestScore() {
        return bestScore;
    }
"""
    replacement = marker + """
    /** Stage 6 audit hook: identity of the best executable root action found in this world. */
    public String getBestRootActionIdentity() {
        if (bestSequence == null) {
            return null;
        }
        Plan.Decision root = bestSequence;
        while (root.prevDecision != null) {
            root = root.prevDecision;
        }
        return root.completeActionIdentity();
    }
"""
    text = replace_once(text, marker, replacement, "controller audit accessor")
    path.write_text(text, encoding="utf-8")


def patch_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    old = """        int representativeSample = 0;
        int representativeValue = Integer.MIN_VALUE;

        for (int sample = 0; sample < samples; sample++) {
"""
    new = """        int representativeSample = 0;
        int representativeValue = Integer.MIN_VALUE;
        String firstActionIdentity = null;
        boolean strategyFusionObserved = false;

        for (int sample = 0; sample < samples; sample++) {
"""
    text = replace_once(text, old, new, "ensemble audit state")

    old = """            Score value = evaluateSa(shadowController, phase, candidateSAs, saIndex);
            if (value.value == Integer.MIN_VALUE) {
                continue;
            }
"""
    new = """            Score value = evaluateSa(shadowController, phase, candidateSAs, saIndex);
            if (value.value == Integer.MIN_VALUE) {
                continue;
            }
            String actionIdentity = shadowController.getBestRootActionIdentity();
            if (actionIdentity != null) {
                if (firstActionIdentity == null) {
                    firstActionIdentity = actionIdentity;
                } else if (!firstActionIdentity.equals(actionIdentity)) {
                    strategyFusionObserved = true;
                }
            }
"""
    text = replace_once(text, old, new, "per-world action audit")

    old = """        int avgValue = (int) (sumValue / validSamples);
        int avgAvailable = (int) (sumAvailable / validSamples);
        return new EnsembleResult(new Score(avgValue, avgAvailable), representativeSample);
"""
    new = """        int avgValue = (int) (sumValue / validSamples);
        int avgAvailable = (int) (sumAvailable / validSamples);
        if (strategyFusionObserved) {
            System.out.println("EXPERT_INFOSET_STRATEGY_FUSION: candidate=" + saIndex
                    + " samples=" + samples);
        }
        return new EnsembleResult(new Score(avgValue, avgAvailable), representativeSample);
"""
    text = replace_once(text, old, new, "fusion diagnostic")
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--simulation-controller", type=Path, required=True)
    parser.add_argument("--spell-picker", type=Path, required=True)
    args = parser.parse_args()
    patch_controller(args.simulation_controller)
    patch_picker(args.spell_picker)
    print("Patched live strategy-fusion audit into Forge")


if __name__ == "__main__":
    main()
