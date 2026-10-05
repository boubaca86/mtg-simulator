"""Enforce one root recipe across sampled worlds, then install that exact plan.

The filename is retained because Stage 6/7 workflows already apply this patch.
Scores below the original position still count. Missing worlds never count.
"""
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
    private RootActionTable rootActionTable;

    public void collectRootActions() {
        rootActionTable = new RootActionTable();
    }

    public RootActionTable getRootActionTable() {
        if (rootActionTable == null) throw new IllegalStateException("Root collection was not enabled");
        return rootActionTable;
    }

    /** Called before the iterator pops the just-evaluated choice nodes. */
    public void recordRootAction(Score score) {
        if (rootActionTable != null && isRootDecision()) {
            rootActionTable.record(snapshotRootAction(getLastDecision()), score);
        }
    }

    /** Install the averaged recipe without re-optimizing it in one world. */
    public void useFixedRootAction(RootActionTable.ScoredAction chosen) {
        if (!currentStack.isEmpty() || !isRootDecision() || chosen == null) {
            throw new IllegalStateException("Cannot install a root action during simulation");
        }
        bestSequence = snapshotRootAction(chosen.action);
        bestScore = chosen.score;
    }

    public String getBestRootActionIdentity() {
        Plan.Decision root = snapshotRootAction(bestSequence);
        return root == null ? null : root.completeActionIdentity();
    }

    /** Detached snapshot of the first spell and its child choices.
     * getBestPlan() mutates linked nodes; never use it while collecting recipes.
     */
    private static Plan.Decision snapshotRootAction(Plan.Decision tail) {
        if (tail == null) return null;
        List<Plan.Decision> sequence = new ArrayList<>();
        for (Plan.Decision d = tail; d != null; d = d.prevDecision) sequence.add(d);
        Collections.reverse(sequence);
        Plan.Decision root = null;
        for (Plan.Decision d : sequence) {
            if (d.saRef != null) {
                if (root != null) break; // exclude future spells in the rollout
                root = new Plan.Decision(d.initialScore, null, d.saRef);
            }
            if (root == null) throw new IllegalStateException("choice precedes root spell");
            if (d.xMana != null) root.xMana = d.xMana;
            if (d.targets != null) root.targets = d.targets;
            if (d.modes != null) {
                root.modes = d.modes.clone();
                root.modesStr = d.modesStr;
            }
            if (d.choices != null) {
                if (root.choices == null) root.choices = new ArrayList<>();
                root.choices.addAll(d.choices);
            }
        }
        return root;
    }
"""
    text = replace_once(text, marker, replacement, "controller root-action collection")
    text = replace_once(text,
        """    public Score shouldSkipTarget(SpellAbility sa, GameSimulator simulator) {
""",
        """    public Score shouldSkipTarget(SpellAbility sa, GameSimulator simulator) {
        // Cached effects omit the choice nodes needed to reconstruct a root recipe.
        if (rootActionTable != null && isRootDecision()) return null;
""", "root target cache bypass")
    path.write_text(text, encoding="utf-8")
    source = Path(__file__).parent / "forge_ai" / "RootActionTable.java"
    path.with_name("RootActionTable.java").write_text(source.read_text(encoding="utf-8"), encoding="utf-8")


def patch_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    # Boundaries belong to the exact Stage 5 patch applied immediately before this.
    start = "    private static class EnsembleResult {"
    end = "    private SpellAbility chooseSpellAbilityToPlayImpl("
    if text.count(start) != 1 or text.count(end) != 1:
        raise RuntimeError("Expected one Stage 5 ensemble implementation")
    a, b = text.index(start), text.index(end)
    text = text[:a] + """    private RootActionTable.ScoredAction evaluateInformationSetEnsemble(PhaseType phase,
            List<SpellAbility> candidateSAs, int saIndex, Score origGameScore, int samples) {
        List<RootActionTable> worlds = new ArrayList<>();
        long originalSample = GameSimulator.getInformationSetSample();
        try {
            for (int sample = 0; sample < samples; sample++) {
                GameSimulator.setInformationSetSample(sample);
                SimulationController shadowController = new SimulationController(origGameScore);
                shadowController.collectRootActions();
                evaluateSa(shadowController, phase, candidateSAs, saIndex);
                worlds.add(shadowController.getRootActionTable());
            }
        } finally {
            GameSimulator.setInformationSetSample(originalSample);
        }
        RootActionTable.Result result = RootActionTable.aggregate(worlds, samples);
        System.out.println("EXPERT_INFOSET_ACTION_COVERAGE: candidate=" + saIndex
                + " complete=" + result.completeActions + " distinct=" + result.distinctActions
                + " samples=" + samples);
        if (result.differentWorldPreferences) {
            // Expected disagreement BEFORE aggregation, not a fused selected action.
            System.out.println("EXPERT_INFOSET_ACTIONS_RECONCILED: candidate=" + saIndex
                    + " samples=" + samples + " selected="
                    + (result.best == null ? "<none>" : result.best.action.completeActionIdentity()));
        }
        if (result.best != null) {
            System.out.println("EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=" + saIndex
                    + " identifiedWorlds=" + samples + " samples=" + samples);
        }
        return result.best;
    }

""" + text[b:]
    text = replace_once(text, "            int representativeSample = 0;",
        "            RootActionTable.ScoredAction bestAction = null;", "winning complete action")
    text = replace_once(text,
        """                EnsembleResult result = evaluateInformationSetEnsemble(phase, candidateSAs, i, origGameScore, samples);
                if (result.score.value > bestAverage.value) {
                    bestAverage = result.score;
                    bestIndex = i;
                    representativeSample = result.representativeSample;
                }
""",
        """                RootActionTable.ScoredAction result = evaluateInformationSetEnsemble(phase, candidateSAs, i, origGameScore, samples);
                if (result != null && result.score.value > bestAverage.value) {
                    bestAverage = result.score;
                    bestIndex = i;
                    bestAction = result;
                }
""", "fixed-action selection")
    text = replace_once(text,
        """                // Build an executable one-action plan using a representative sampled world.
                // The root action was selected by the mean score across all samples; future
                // decisions are intentionally not retained from this representative world.
                GameSimulator.setInformationSetSample(representativeSample);
                Score executableScore = evaluateSa(controller, phase, candidateSAs, bestIndex);
                GameSimulator.setInformationSetSample(0);
                if (executableScore.value != Integer.MIN_VALUE) {
                    bestSa = candidateSAs.get(bestIndex);
                }
""",
        """                controller.useFixedRootAction(bestAction);
                if (!bestAction.action.completeActionIdentity().equals(controller.getBestRootActionIdentity())) {
                    throw new IllegalStateException("EXPERT_INFOSET_STRATEGY_FUSION: installed action differs from averaged recipe");
                }
                bestSa = candidateSAs.get(bestIndex);
""", "exact root plan installation")
    text = replace_once(text,
        """            lastScore = simulator.simulateSpellAbility(sa);
            numSimulations++;
""",
        """            lastScore = simulator.simulateSpellAbility(sa);
            controller.recordRootAction(lastScore);
            numSimulations++;
""", "record before choice-node unwinding")
    # Forge's target descriptions include concrete card IDs. The replayed index
    # must resolve to the same target, not just to a legal target with the same name.
    needle = "if (!selector.selectTargets(decision.targets)) {"
    if text.count(needle) != 2:
        raise RuntimeError("Expected exactly two planned-target replay sites")
    text = text.replace(needle, "if (!selectFixedTargets(selector, decision)) {")
    marker = "    private void printPlannedActionFailure(Plan.Decision decision, String cause) {"
    text = replace_once(text, marker,
        """    private boolean selectFixedTargets(MultiTargetSelector selector, Plan.Decision decision) {
        boolean matched = selector.selectTargets(decision.targets)
                && decision.targets.toString().equals(selector.getLastSelectedTargets().toString());
        if (!matched && informationSetSamples() > 1) {
            System.out.println("EXPERT_INFOSET_ACTION_REPLAY_MISMATCH: " + decision.completeActionIdentity());
        }
        return matched;
    }

""" + marker, "exact target replay check")
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--simulation-controller", type=Path, required=True)
    parser.add_argument("--spell-picker", type=Path, required=True)
    args = parser.parse_args()
    patch_controller(args.simulation_controller)
    patch_picker(args.spell_picker)
    print("Patched fixed-root-action aggregation and replay checks into Forge")


if __name__ == "__main__":
    main()
