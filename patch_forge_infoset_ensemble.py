from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch_game_simulator(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "public class GameSimulator {\n    public static boolean COPY_STACK = false;",
        """public class GameSimulator {
    private static final ThreadLocal<Long> INFORMATION_SET_SAMPLE = ThreadLocal.withInitial(() -> 0L);

    public static void setInformationSetSample(long sample) {
        INFORMATION_SET_SAMPLE.set(sample);
    }

    public static long getInformationSetSample() {
        return INFORMATION_SET_SAMPLE.get();
    }

    public static boolean COPY_STACK = false;""",
        "information-set sample state",
    )

    text = replace_once(
        text,
        """        if (Boolean.getBoolean("forge.expert.infoset")) {
            determinizeOpponentHiddenInformation();
        }
""",
        """        if (Boolean.getBoolean("forge.expert.infoset") && controller.isRootDecision()) {
            determinizeOpponentHiddenInformation();
        }
""",
        "root-only determinization",
    )

    text = replace_once(
        text,
        """        long seed = 0x6A09E667F3BCC909L;
        seed = seed * 31 + simGame.getPhaseHandler().getTurn();
""",
        """        long seed = 0x6A09E667F3BCC909L;
        seed = seed * 31 + getInformationSetSample();
        seed = seed * 31 + simGame.getPhaseHandler().getTurn();
""",
        "sample-dependent determinization seed",
    )

    path.write_text(text, encoding="utf-8")
    print(f"Patched Stage 5 information-set sample support into {path}")


def patch_simulation_controller(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        """    private int getRecursionDepth() {
        return scoreStack.size() - 1;
    }
""",
        """    private int getRecursionDepth() {
        return scoreStack.size() - 1;
    }

    public boolean isRootDecision() {
        return getRecursionDepth() == 0;
    }
""",
        "root decision accessor",
    )

    path.write_text(text, encoding="utf-8")
    print(f"Patched Stage 5 root-search accessor into {path}")


def patch_spell_ability_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        """    private Plan formulatePlanWithPhase(Score origGameScore, List<SpellAbility> candidateSAs, PhaseType phase) {
        SimulationController controller = new SimulationController(origGameScore);
""",
        """    private int informationSetSamples() {
        if (!Boolean.getBoolean("forge.expert.infoset")) {
            return 1;
        }
        return Math.max(1, Integer.getInteger("forge.expert.infoset.samples", 1));
    }

    private long deterministicSimulationSeed() {
        long seed = 0xBB67AE8584CAA73BL;
        seed = seed * 31 + game.getPhaseHandler().getTurn();
        seed = seed * 31 + game.getPhaseHandler().getPhase().ordinal();
        seed = seed * 31 + player.getId();
        seed = seed * 31 + GameSimulator.getInformationSetSample();
        return seed;
    }

    private Plan formulatePlanWithPhase(Score origGameScore, List<SpellAbility> candidateSAs, PhaseType phase) {
        // With multiple hidden-world samples, only execute the root decision from a
        // sampled plan. The next real priority window will re-search from fresh
        // information instead of committing to a multi-step line tailored to one
        // particular hidden-world determinization.
        SimulationController controller = informationSetSamples() > 1
                ? new SimulationController(origGameScore, 0)
                : new SimulationController(origGameScore);
""",
        "information-set helper methods",
    )

    old_method = """    private SpellAbility chooseSpellAbilityToPlayImpl(SimulationController controller, List<SpellAbility> candidateSAs, Score origGameScore, PhaseType phase) {
        long startTime = System.currentTimeMillis();

        SpellAbility bestSa = null;
        Score bestSaValue = origGameScore;
        print("Evaluating... (orig score = " + origGameScore +  ")");
        for (int i = 0; i < candidateSAs.size(); i++) {
            Score value = evaluateSa(controller, phase, candidateSAs, i);
            if (value.value > bestSaValue.value) {
                bestSaValue = value;
                bestSa = candidateSAs.get(i);
            }
        }

        // To make the AI hold off on plays that only add unavailable resources, check the score
        // while excluding phased-out permanents and summon sick creatures before MAIN2.
        // Do it here on the best SA, rather than for all evaluations, so that if the best SA
        // is indeed a creature spell, we don't pick something else to play now and then have
        // no mana to play the truly best SA post-combat.
        if (bestSa != null && bestSaValue.availableValue <= origGameScore.availableValue) {
            bestSa = null;
        }

        long execTime = System.currentTimeMillis() - startTime;
        print("BEST: " + abilityToString(bestSa) + " SCORE: " + bestSaValue.availableValue + " TIME: " + execTime);
        this.bestScore = bestSaValue;
        return bestSa;
    }
"""

    new_method = """    private static class EnsembleResult {
        final Score score;
        final int representativeSample;

        EnsembleResult(Score score, int representativeSample) {
            this.score = score;
            this.representativeSample = representativeSample;
        }
    }

    private EnsembleResult evaluateInformationSetEnsemble(PhaseType phase, List<SpellAbility> candidateSAs,
            int saIndex, Score origGameScore, int samples) {
        long sumValue = 0;
        long sumAvailable = 0;
        int validSamples = 0;
        int representativeSample = 0;
        int representativeValue = Integer.MIN_VALUE;

        for (int sample = 0; sample < samples; sample++) {
            GameSimulator.setInformationSetSample(sample);
            SimulationController shadowController = new SimulationController(origGameScore);
            Score value = evaluateSa(shadowController, phase, candidateSAs, saIndex);
            if (value.value == Integer.MIN_VALUE) {
                continue;
            }
            sumValue += value.value;
            sumAvailable += value.availableValue;
            validSamples++;
            if (value.value > representativeValue) {
                representativeValue = value.value;
                representativeSample = sample;
            }
        }

        GameSimulator.setInformationSetSample(0);
        if (validSamples == 0) {
            return new EnsembleResult(new Score(Integer.MIN_VALUE), 0);
        }

        int avgValue = (int) (sumValue / validSamples);
        int avgAvailable = (int) (sumAvailable / validSamples);
        return new EnsembleResult(new Score(avgValue, avgAvailable), representativeSample);
    }

    private SpellAbility chooseSpellAbilityToPlayImpl(SimulationController controller, List<SpellAbility> candidateSAs, Score origGameScore, PhaseType phase) {
        long startTime = System.currentTimeMillis();

        int samples = informationSetSamples();
        if (controller.isRootDecision() && samples > 1) {
            int bestIndex = -1;
            int representativeSample = 0;
            Score bestAverage = origGameScore;
            print("Evaluating information-set ensemble (samples = " + samples + ", orig score = " + origGameScore + ")");

            for (int i = 0; i < candidateSAs.size(); i++) {
                EnsembleResult result = evaluateInformationSetEnsemble(phase, candidateSAs, i, origGameScore, samples);
                if (result.score.value > bestAverage.value) {
                    bestAverage = result.score;
                    bestIndex = i;
                    representativeSample = result.representativeSample;
                }
            }

            SpellAbility bestSa = null;
            if (bestIndex >= 0 && bestAverage.availableValue > origGameScore.availableValue) {
                // Build an executable one-action plan using a representative sampled world.
                // The root action was selected by the mean score across all samples; future
                // decisions are intentionally not retained from this representative world.
                GameSimulator.setInformationSetSample(representativeSample);
                Score executableScore = evaluateSa(controller, phase, candidateSAs, bestIndex);
                GameSimulator.setInformationSetSample(0);
                if (executableScore.value != Integer.MIN_VALUE) {
                    bestSa = candidateSAs.get(bestIndex);
                }
            }

            long execTime = System.currentTimeMillis() - startTime;
            print("ENSEMBLE BEST: " + abilityToString(bestSa) + " AVG SCORE: " + bestAverage.availableValue
                    + " TIME: " + execTime);
            this.bestScore = bestAverage;
            return bestSa;
        }

        SpellAbility bestSa = null;
        Score bestSaValue = origGameScore;
        print("Evaluating... (orig score = " + origGameScore +  ")");
        for (int i = 0; i < candidateSAs.size(); i++) {
            Score value = evaluateSa(controller, phase, candidateSAs, i);
            if (value.value > bestSaValue.value) {
                bestSaValue = value;
                bestSa = candidateSAs.get(i);
            }
        }

        // To make the AI hold off on plays that only add unavailable resources, check the score
        // while excluding phased-out permanents and summon sick creatures before MAIN2.
        // Do it here on the best SA, rather than for all evaluations, so that if the best SA
        // is indeed a creature spell, we don't pick something else to play now and then have
        // no mana to play the truly best SA post-combat.
        if (bestSa != null && bestSaValue.availableValue <= origGameScore.availableValue) {
            bestSa = null;
        }

        long execTime = System.currentTimeMillis() - startTime;
        print("BEST: " + abilityToString(bestSa) + " SCORE: " + bestSaValue.availableValue + " TIME: " + execTime);
        this.bestScore = bestSaValue;
        return bestSa;
    }
"""

    text = replace_once(text, old_method, new_method, "root ensemble search")

    text = replace_once(
        text,
        """        Random origRandom = MyRandom.getRandom();
        long randomSeedToUse = origRandom.nextLong();
""",
        """        Random origRandom = MyRandom.getRandom();
        long randomSeedToUse = informationSetSamples() > 1
                ? deterministicSimulationSeed()
                : origRandom.nextLong();
""",
        "ensemble-safe simulation RNG",
    )

    path.write_text(text, encoding="utf-8")
    print(f"Patched Stage 5 multi-sample information-set search into {path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--game-simulator", type=Path, required=True)
    parser.add_argument("--simulation-controller", type=Path, required=True)
    parser.add_argument("--spell-picker", type=Path, required=True)
    args = parser.parse_args()

    patch_simulation_controller(args.simulation_controller)
    patch_game_simulator(args.game_simulator)
    patch_spell_ability_picker(args.spell_picker)


if __name__ == "__main__":
    main()
