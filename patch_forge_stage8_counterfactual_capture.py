from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("spell_picker", type=Path)
    a = p.parse_args()
    path = a.spell_picker
    text = path.read_text(encoding="utf-8")

    # Stage 6 currently returns only the best complete action for each top-level
    # SpellAbility. Stage 8 must retain every complete cross-world action without
    # changing which action Forge actually executes.
    text = replace_once(
        text,
        """    private RootActionTable.ScoredAction evaluateInformationSetEnsemble(PhaseType phase,
            List<SpellAbility> candidateSAs, int saIndex, Score origGameScore, int samples) {
""",
        """    private RootActionTable.Result evaluateInformationSetEnsemble(PhaseType phase,
            List<SpellAbility> candidateSAs, int saIndex, Score origGameScore, int samples) {
""",
        "Stage 8 aggregate return type",
    )
    text = replace_once(text, "        return result.best;\n    }\n\n",
                        "        return result;\n    }\n\n", "Stage 8 aggregate return value")

    text = replace_once(
        text,
        "            RootActionTable.ScoredAction bestAction = null;\n",
        """            RootActionTable.ScoredAction bestAction = null;
            List<RootActionTable.ScoredAction> stage8Candidates = new ArrayList<>();
""",
        "Stage 8 candidate pool",
    )

    text = replace_once(
        text,
        """                RootActionTable.ScoredAction result = evaluateInformationSetEnsemble(phase, candidateSAs, i, origGameScore, samples);
                if (result != null && result.score.value > bestAverage.value) {
                    bestAverage = result.score;
                    bestIndex = i;
                    bestAction = result;
                }
""",
        """                RootActionTable.Result result = evaluateInformationSetEnsemble(phase, candidateSAs, i, origGameScore, samples);
                stage8Candidates.addAll(result.completeActionScores);
                if (result.best != null && result.best.score.value > bestAverage.value) {
                    bestAverage = result.best.score;
                    bestIndex = i;
                    bestAction = result.best;
                }
""",
        "Stage 8 retain complete candidates",
    )

    # The Stage 7 capture patch is applied immediately before this patch. Reuse
    # its legal-information JSON and exact decision index so Stage 8 rows cannot
    # drift from the public observation that produced the selected action.
    old = """                    System.out.println("EXPERT_STAGE7_DATA: " + legalJson);
"""
    new = old + """                    if (Boolean.getBoolean("forge.expert.stage8.capture")) {
                        System.out.println("EXPERT_STAGE8_CAPTURE: "
                                + stage8CaptureJson(player, decisionIndex, deterministicSimulationSeed(), matchupId,
                                        legalJson, actionIdentity, stage8Candidates, samples));
                    }
"""
    text = replace_once(text, old, new, "Stage 8 capture emission")

    marker = "    private static long stage7DecisionIndex = 0L;\n"
    helper = marker + r'''

    private static String stage8JsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    /** Offline learner-boundary event. Scores are already fixed-root means over
     * every required hidden-world sample. This method never affects move choice. */
    private static String stage8CaptureJson(Player actor, long decisionIndex, long runSeed, String matchupId,
            String legalJson, String selectedIdentity,
            List<RootActionTable.ScoredAction> candidates, int samples) {
        java.util.LinkedHashMap<String, RootActionTable.ScoredAction> unique = new java.util.LinkedHashMap<>();
        for (RootActionTable.ScoredAction candidate : candidates) {
            String identity = candidate.action.completeActionIdentity();
            RootActionTable.ScoredAction prior = unique.get(identity);
            if (prior != null && prior.score.value != candidate.score.value) {
                throw new IllegalStateException("Stage 8 duplicate action has inconsistent aggregate score: " + identity);
            }
            if (prior == null) unique.put(identity, candidate);
        }
        StringBuilder out = new StringBuilder(4096);
        out.append('{');
        out.append("\"schema_version\":\"stage8-capture-v1\",");
        out.append("\"decision_index\":").append(decisionIndex).append(',');
        out.append("\"run_seed\":").append(runSeed).append(',');
        out.append("\"matchup_id\":\"").append(stage8JsonEscape(matchupId == null ? "" : matchupId)).append("\",");
        out.append("\"public_state\":").append(legalJson).append(',');
        out.append("\"selected_action\":\"").append(stage8JsonEscape(selectedIdentity)).append("\",");
        out.append("\"candidates\":[");
        int i = 0;
        for (RootActionTable.ScoredAction candidate : unique.values()) {
            if (i++ > 0) out.append(',');
            out.append('{');
            String candidateIdentity = candidate.action.completeActionIdentity();
            out.append("\"action_identity\":\"").append(stage8JsonEscape(candidateIdentity)).append("\",");
            out.append("\"aggregate_score\":").append(candidate.score.value).append(',');
            out.append("\"replay_valid_count\":").append(samples).append(',');
            out.append("\"target_semantics_version\":\"")
                    .append(LegalDecisionFeatures.TARGET_SEMANTICS_VERSION).append("\",");
            out.append("\"target_public_semantics\":")
                    .append(LegalDecisionFeatures.describeActionTargetsJson(actor, candidate.action.targets));
            out.append('}');
        }
        out.append("],\"information_set_samples\":").append(samples);
        return out.append('}').toString();
    }
'''
    text = replace_once(text, marker, helper, "Stage 8 JSON helper")

    path.write_text(text, encoding="utf-8")
    print(f"Patched Stage 8A counterfactual capture into {path}")


if __name__ == "__main__":
    main()
