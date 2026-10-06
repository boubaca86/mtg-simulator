from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch_plan(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "        String modesStr; // for human pretty-print consumption only\n",
        """        String modesStr; // for human pretty-print consumption only
        // Stage 8E audit metadata only. It is never used to choose or execute an action.
        long expertCaptureDecisionIndex = -1L;
""",
        "Plan.Decision audit field",
    )
    path.write_text(text, encoding="utf-8")


def patch_controller(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    marker = """    public String getBestRootActionIdentity() {
        Plan.Decision root = snapshotRootAction(bestSequence);
        return root == null ? null : root.completeActionIdentity();
    }
"""
    replacement = marker + """
    /** Attach the public capture index to the exact fixed root recipe that won
     * aggregation. This metadata follows the Plan only; it never changes scores,
     * legality, targets or card choices. */
    public void bindBestRootCapture(long decisionIndex) {
        if (decisionIndex < 0 || bestSequence == null || bestSequence.prevDecision != null) {
            throw new IllegalStateException("Cannot bind Stage 8E capture to root action");
        }
        if (bestSequence.expertCaptureDecisionIndex >= 0
                && bestSequence.expertCaptureDecisionIndex != decisionIndex) {
            throw new IllegalStateException("Stage 8E root action already bound");
        }
        bestSequence.expertCaptureDecisionIndex = decisionIndex;
    }
"""
    text = replace_once(text, marker, replacement, "root capture binding")
    path.write_text(text, encoding="utf-8")


def patch_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    marker = "    private static long stage7DecisionIndex = 0L;\n"
    text = replace_once(
        text,
        marker,
        marker + "    private static long stage8PriorityReturnIndex = 0L;\n",
        "priority-return counter",
    )

    capture = """                    if (Boolean.getBoolean("forge.expert.stage8.capture")) {
                        System.out.println("EXPERT_STAGE8_CAPTURE: "
                                + stage8CaptureJson(player, decisionIndex, deterministicSimulationSeed(), matchupId,
                                        legalJson, actionIdentity, stage8Candidates, samples));
                    }
"""
    text = replace_once(
        text,
        capture,
        capture + """                    if (Boolean.getBoolean("forge.expert.stage8.execution")) {
                        controller.bindBestRootCapture(decisionIndex);
                    }
""",
        "capture-to-plan binding",
    )

    helper_marker = "    private static String stage8JsonEscape(String value) {\n"
    helpers = r'''    private void stage8EmitReturnedAction(Plan.Decision decision) {
        if (!Boolean.getBoolean("forge.expert.stage8.execution")) return;
        if (decision == null || decision.expertCaptureDecisionIndex < 0) {
            throw new IllegalStateException("Stage 8E returned action lacks a capture binding");
        }
        long priorityIndex = stage8PriorityReturnIndex++;
        StringBuilder out = new StringBuilder(512);
        out.append('{');
        out.append("\"schema_version\":\"stage8e-returned-action-v1\",");
        out.append("\"priority_return_index\":").append(priorityIndex).append(',');
        out.append("\"capture_decision_index\":").append(decision.expertCaptureDecisionIndex).append(',');
        out.append("\"turn\":").append(game.getPhaseHandler().getTurn()).append(',');
        out.append("\"phase\":\"").append(stage8JsonEscape(game.getPhaseHandler().getPhase().name())).append("\",");
        out.append("\"acting_player_name\":\"")
                .append(stage8JsonEscape(player.getLobbyPlayer().getName())).append("\",");
        out.append("\"action_identity\":\"")
                .append(stage8JsonEscape(decision.completeActionIdentity())).append("\",");
        out.append("\"boundary\":\"spell-ability-return\",");
        out.append("\"resolved_or_completed\":false,");
        out.append("\"promotion_allowed\":false");
        System.out.println("EXPERT_STAGE8_RETURNED_ACTION: " + out.append('}'));
    }

    private void stage8EmitPriorityPass(String reason) {
        if (!Boolean.getBoolean("forge.expert.stage8.execution")) return;
        long priorityIndex = stage8PriorityReturnIndex++;
        StringBuilder out = new StringBuilder(384);
        out.append('{');
        out.append("\"schema_version\":\"stage8e-priority-pass-v1\",");
        out.append("\"priority_return_index\":").append(priorityIndex).append(',');
        out.append("\"turn\":").append(game.getPhaseHandler().getTurn()).append(',');
        out.append("\"phase\":\"").append(stage8JsonEscape(game.getPhaseHandler().getPhase().name())).append("\",");
        out.append("\"acting_player_name\":\"")
                .append(stage8JsonEscape(player.getLobbyPlayer().getName())).append("\",");
        out.append("\"reason\":\"").append(stage8JsonEscape(reason)).append("\",");
        out.append("\"boundary\":\"spell-ability-return\",");
        out.append("\"promotion_allowed\":false");
        System.out.println("EXPERT_STAGE8_PRIORITY_PASS: " + out.append('}'));
    }

'''
    text = replace_once(text, helper_marker, helpers + helper_marker, "Stage 8E emit helpers")

    old = """        if (!game.getStack().isEmpty() && game.getStack().peekAbility().getActivatingPlayer().equals(player)) {
            return null;
        }
"""
    new = """        if (!game.getStack().isEmpty() && game.getStack().peekAbility().getActivatingPlayer().equals(player)) {
            stage8EmitPriorityPass("own-stack");
            return null;
        }
"""
    text = replace_once(text, old, new, "own-stack pass")

    old = """        createNewPlan(origGameScore, candidateSAs);
        return getPlannedSpellAbility(origGameScore, candidateSAs);
"""
    new = """        createNewPlan(origGameScore, candidateSAs);
        SpellAbility selected = getPlannedSpellAbility(origGameScore, candidateSAs);
        if (selected == null) {
            stage8EmitPriorityPass("no-action-returned");
        }
        return selected;
"""
    text = replace_once(text, old, new, "final priority return")

    old = """        print("Planned decision " + plan.getNextDecisionIndex() + ": " + decision);
        return sa;
"""
    new = """        print("Planned decision " + plan.getNextDecisionIndex() + ": " + decision);
        stage8EmitReturnedAction(decision);
        return sa;
"""
    text = replace_once(text, old, new, "returned action event")

    path.write_text(text, encoding="utf-8")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--plan", required=True, type=Path)
    p.add_argument("--simulation-controller", required=True, type=Path)
    p.add_argument("--spell-picker", required=True, type=Path)
    args = p.parse_args()
    patch_plan(args.plan)
    patch_controller(args.simulation_controller)
    patch_picker(args.spell_picker)
    print("Patched Stage 8E capture-to-return binding into Forge")


if __name__ == "__main__":
    main()
