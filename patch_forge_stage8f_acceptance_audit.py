from __future__ import annotations

import argparse
from pathlib import Path


AUDIT_JAVA = r'''package forge.ai.simulation;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Stage 8F audit-only handoff between SpellAbilityPicker and PlayerControllerAi.
 * The pending record is a same-thread, same-object assertion. It has no method
 * that can choose, replace, mutate or execute a SpellAbility.
 */
public final class ExpertActionAudit {
    private ExpertActionAudit() {}

    private static final class Pending {
        final SpellAbility spellAbility;
        final long captureDecisionIndex;
        final String actionIdentity;
        final int turn;
        final String phase;
        final String actorName;

        Pending(SpellAbility spellAbility, long captureDecisionIndex, String actionIdentity,
                int turn, String phase, String actorName) {
            this.spellAbility = spellAbility;
            this.captureDecisionIndex = captureDecisionIndex;
            this.actionIdentity = actionIdentity;
            this.turn = turn;
            this.phase = phase;
            this.actorName = actorName;
        }
    }

    public static final class Accepted {
        final long captureDecisionIndex;
        final String actionIdentity;
        final int turn;
        final String phase;
        final String actorName;

        private Accepted(Pending pending) {
            this.captureDecisionIndex = pending.captureDecisionIndex;
            this.actionIdentity = pending.actionIdentity;
            this.turn = pending.turn;
            this.phase = pending.phase;
            this.actorName = pending.actorName;
        }
    }

    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    public static void registerReturned(SpellAbility sa, long captureDecisionIndex,
            String actionIdentity, Player actor) {
        if (!Boolean.getBoolean("forge.expert.stage8.acceptance")) return;
        if (sa == null || actor == null || captureDecisionIndex < 0
                || actionIdentity == null || actionIdentity.isEmpty()) {
            throw new IllegalStateException("Invalid Stage 8F returned-action registration");
        }
        if (PENDING.get() != null) {
            throw new IllegalStateException("Previous Stage 8F returned action was never accepted");
        }
        PENDING.set(new Pending(
                sa, captureDecisionIndex, actionIdentity,
                actor.getGame().getPhaseHandler().getTurn(),
                actor.getGame().getPhaseHandler().getPhase().name(),
                actor.getLobbyPlayer().getName()));
    }

    /**
     * Ignore ordinary Forge-AI actions when no expert action is pending. If an
     * expert action is pending, require reference identity with the exact object
     * returned by SpellAbilityPicker.
     */
    public static Accepted acceptIfPending(SpellAbility sa, Player actor) {
        if (!Boolean.getBoolean("forge.expert.stage8.acceptance")) return null;
        Pending pending = PENDING.get();
        if (pending == null) return null;
        if (sa != pending.spellAbility) {
            throw new IllegalStateException("Stage 8F accepted a different SpellAbility object");
        }
        if (actor == null
                || !pending.actorName.equals(actor.getLobbyPlayer().getName())
                || pending.turn != actor.getGame().getPhaseHandler().getTurn()
                || !pending.phase.equals(actor.getGame().getPhaseHandler().getPhase().name())) {
            throw new IllegalStateException("Stage 8F public context changed before controller acceptance");
        }
        PENDING.remove();
        Accepted accepted = new Accepted(pending);
        System.out.println("EXPERT_STAGE8_ACCEPTED_ACTION: " + eventJson(
                "stage8f-accepted-action-v1", "PlayerControllerAi.playChosenSpellAbility-entry",
                accepted, true, null));
        return accepted;
    }

    public static void handlerReturned(Accepted accepted, String kind) {
        if (accepted == null) return;
        System.out.println("EXPERT_STAGE8_HANDLER_RETURNED: " + eventJson(
                "stage8f-handler-return-v1", "PlayerControllerAi.playChosenSpellAbility-return",
                accepted, true, kind));
    }

    private static String eventJson(String schema, String boundary, Accepted accepted,
            boolean sameObject, String kind) {
        StringBuilder out = new StringBuilder(512);
        out.append('{');
        out.append("\"schema_version\":\"").append(schema).append("\",");
        out.append("\"capture_decision_index\":").append(accepted.captureDecisionIndex).append(',');
        out.append("\"turn\":").append(accepted.turn).append(',');
        out.append("\"phase\":\"").append(escape(accepted.phase)).append("\",");
        out.append("\"acting_player_name\":\"").append(escape(accepted.actorName)).append("\",");
        out.append("\"action_identity\":\"").append(escape(accepted.actionIdentity)).append("\",");
        out.append("\"boundary\":\"").append(boundary).append("\",");
        out.append("\"same_spell_ability_object\":").append(sameObject).append(',');
        if (kind != null) {
            out.append("\"handler_kind\":\"").append(escape(kind)).append("\",");
        }
        out.append("\"resolved_or_completed\":false,");
        out.append("\"promotion_allowed\":false");
        return out.append('}').toString();
    }
}
'''


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "    private void stage8EmitReturnedAction(Plan.Decision decision) {\n",
        "    private void stage8EmitReturnedAction(SpellAbility sa, Plan.Decision decision) {\n",
        "Stage 8E returned-action helper signature",
    )
    old = """        System.out.println("EXPERT_STAGE8_RETURNED_ACTION: " + out.append('}'));
    }
"""
    new = """        System.out.println("EXPERT_STAGE8_RETURNED_ACTION: " + out.append('}'));
        if (Boolean.getBoolean("forge.expert.stage8.acceptance")) {
            ExpertActionAudit.registerReturned(sa, decision.expertCaptureDecisionIndex,
                    decision.completeActionIdentity(), player);
        }
    }
"""
    text = replace_once(text, old, new, "Stage 8F returned-action registration")
    text = replace_once(
        text,
        "        stage8EmitReturnedAction(decision);\n",
        "        stage8EmitReturnedAction(sa, decision);\n",
        "Stage 8F returned-action call",
    )
    path.write_text(text, encoding="utf-8")


def patch_player_controller(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    old = """    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        if (sa.isLandAbility()) {
            if (sa.canPlay()) {
                sa.resolve();
            }
        } else {
            ComputerUtil.handlePlayingSpellAbility(player, sa, getDeferredTargetingPlayerAction(sa));
        }
        return true;
    }
"""
    new = """    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        forge.ai.simulation.ExpertActionAudit.Accepted expertAudit =
                forge.ai.simulation.ExpertActionAudit.acceptIfPending(sa, player);
        if (sa.isLandAbility()) {
            if (sa.canPlay()) {
                sa.resolve();
            }
        } else {
            ComputerUtil.handlePlayingSpellAbility(player, sa, getDeferredTargetingPlayerAction(sa));
        }
        forge.ai.simulation.ExpertActionAudit.handlerReturned(
                expertAudit, sa.isLandAbility() ? "land" : "spell-or-ability");
        return true;
    }
"""
    text = replace_once(text, old, new, "PlayerControllerAi acceptance boundary")
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--forge-ai-java", required=True, type=Path)
    parser.add_argument("--spell-picker", required=True, type=Path)
    parser.add_argument("--player-controller", required=True, type=Path)
    args = parser.parse_args()

    target = args.forge_ai_java / "forge" / "ai" / "simulation" / "ExpertActionAudit.java"
    if target.exists():
        raise RuntimeError(f"Refusing to overwrite existing Forge source: {target}")
    target.write_text(AUDIT_JAVA, encoding="utf-8")
    patch_picker(args.spell_picker)
    patch_player_controller(args.player_controller)
    print("Patched Stage 8F controller-acceptance audit into Forge")


if __name__ == "__main__":
    main()
