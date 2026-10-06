from __future__ import annotations

import argparse
from pathlib import Path


BRIDGE_JAVA = r'''package forge.ai.simulation;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.IdentityHashMap;
import java.util.Map;

/** Audit-only bridge from the full-simulation return boundary to the real
 * PlayerControllerAi dispatch boundary. Keys use Java object identity and are
 * consumed exactly once. Nothing here can choose, mutate or execute an action. */
public final class ExpertActionAuditBridge {
    private ExpertActionAuditBridge() {}

    private static final Map<SpellAbility, ReturnedAction> PENDING = new IdentityHashMap<>();

    private static final class ReturnedAction {
        final long captureDecisionIndex;
        final long priorityReturnIndex;
        final String actionIdentity;
        final long turn;
        final String phase;
        final String actor;

        ReturnedAction(long captureDecisionIndex, long priorityReturnIndex, String actionIdentity,
                long turn, String phase, String actor) {
            this.captureDecisionIndex = captureDecisionIndex;
            this.priorityReturnIndex = priorityReturnIndex;
            this.actionIdentity = actionIdentity;
            this.turn = turn;
            this.phase = phase;
            this.actor = actor;
        }
    }

    public static synchronized void register(SpellAbility sa, long captureDecisionIndex,
            long priorityReturnIndex, String actionIdentity, long turn, String phase, String actor) {
        if (!Boolean.getBoolean("forge.expert.stage8.acceptance")) return;
        if (sa == null || captureDecisionIndex < 0 || priorityReturnIndex < 0
                || actionIdentity == null || actionIdentity.isEmpty()) {
            throw new IllegalArgumentException("Invalid Stage 8F returned-action registration");
        }
        if (PENDING.containsKey(sa)) {
            throw new IllegalStateException("Stage 8F SpellAbility already registered");
        }
        PENDING.put(sa, new ReturnedAction(captureDecisionIndex, priorityReturnIndex,
                actionIdentity, turn, phase, actor));
    }

    private static synchronized ReturnedAction take(SpellAbility sa) {
        return PENDING.remove(sa);
    }

    public static void emitControllerAcceptance(SpellAbility sa, Player actor,
            boolean dispatchSuccess, boolean controllerReturnValue) {
        if (!Boolean.getBoolean("forge.expert.stage8.acceptance")) return;
        ReturnedAction record = take(sa);
        // Default/non-full-simulation AI actions legitimately have no Stage 8 record.
        if (record == null) return;
        if (actor == null) throw new IllegalArgumentException("Stage 8F actor is required");
        Game game = actor.getGame();
        PhaseType phase = game.getPhaseHandler().getPhase();
        String actorName = actor.getLobbyPlayer().getName();

        StringBuilder out = new StringBuilder(768);
        out.append('{');
        out.append("\"schema_version\":\"stage8f-controller-acceptance-v1\",");
        out.append("\"capture_decision_index\":").append(record.captureDecisionIndex).append(',');
        out.append("\"priority_return_index\":").append(record.priorityReturnIndex).append(',');
        out.append("\"turn\":").append(game.getPhaseHandler().getTurn()).append(',');
        out.append("\"phase\":\"").append(jsonEscape(phase.name())).append("\",");
        out.append("\"acting_player_name\":\"").append(jsonEscape(actorName)).append("\",");
        out.append("\"action_identity\":\"").append(jsonEscape(record.actionIdentity)).append("\",");
        out.append("\"dispatch_success\":").append(dispatchSuccess).append(',');
        out.append("\"controller_return_value\":").append(controllerReturnValue).append(',');
        out.append("\"land_ability\":").append(sa.isLandAbility()).append(',');
        out.append("\"skip_after_dispatch\":").append(sa.isSkip()).append(',');
        out.append("\"return_context_matches\":")
                .append(record.turn == game.getPhaseHandler().getTurn()
                        && record.phase.equals(phase.name())
                        && record.actor.equals(actorName)).append(',');
        out.append("\"boundary\":\"player-controller-dispatch\",");
        out.append("\"resolved_or_completed\":false,");
        out.append("\"promotion_allowed\":false");
        System.out.println("EXPERT_STAGE8_CONTROLLER_ACCEPTANCE: " + out.append('}'));
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
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
        "    private void stage8EmitReturnedAction(Plan.Decision decision, SpellAbility sa) {\n",
        "returned-action helper signature",
    )
    old = """        System.out.println("EXPERT_STAGE8_RETURNED_ACTION: " + out.append('}'));
    }
"""
    new = """        System.out.println("EXPERT_STAGE8_RETURNED_ACTION: " + out.append('}'));
        ExpertActionAuditBridge.register(sa, decision.expertCaptureDecisionIndex, priorityIndex,
                decision.completeActionIdentity(), game.getPhaseHandler().getTurn(),
                game.getPhaseHandler().getPhase().name(), player.getLobbyPlayer().getName());
    }
"""
    text = replace_once(text, old, new, "returned-action bridge registration")
    text = replace_once(
        text,
        "        stage8EmitReturnedAction(decision);\n",
        "        stage8EmitReturnedAction(decision, sa);\n",
        "returned-action helper call",
    )
    path.write_text(text, encoding="utf-8")


def patch_player_controller(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "import forge.ai.ability.ProtectAi;\n",
        "import forge.ai.ability.ProtectAi;\nimport forge.ai.simulation.ExpertActionAuditBridge;\n",
        "audit bridge import",
    )
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
        boolean expertDispatchSuccess;
        if (sa.isLandAbility()) {
            expertDispatchSuccess = sa.canPlay();
            if (expertDispatchSuccess) {
                sa.resolve();
            }
        } else {
            expertDispatchSuccess = ComputerUtil.handlePlayingSpellAbility(
                    player, sa, getDeferredTargetingPlayerAction(sa));
        }
        // Preserve Forge 2.0.15 gameplay semantics exactly: this controller method
        // still returns true regardless of ComputerUtil's internal success result.
        ExpertActionAuditBridge.emitControllerAcceptance(sa, player, expertDispatchSuccess, true);
        return true;
    }
"""
    text = replace_once(text, old, new, "PlayerControllerAi dispatch instrumentation")
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--forge-ai-java", required=True, type=Path)
    parser.add_argument("--spell-picker", required=True, type=Path)
    parser.add_argument("--player-controller-ai", required=True, type=Path)
    args = parser.parse_args()

    bridge = args.forge_ai_java / "forge" / "ai" / "simulation" / "ExpertActionAuditBridge.java"
    if bridge.exists():
        raise RuntimeError(f"Refusing to overwrite existing Forge source: {bridge}")
    bridge.write_text(BRIDGE_JAVA, encoding="utf-8")
    patch_picker(args.spell_picker)
    patch_player_controller(args.player_controller_ai)
    print("Patched Stage 8F controller-acceptance binding into Forge")


if __name__ == "__main__":
    main()
