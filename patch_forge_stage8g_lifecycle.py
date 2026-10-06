from __future__ import annotations

import argparse
from pathlib import Path


LIFECYCLE_JAVA = r'''package forge.game;

import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;

import java.util.IdentityHashMap;
import java.util.Map;

/** Observation-only lifecycle registry for expert-AI root actions.
 *
 * The registry follows Java object identity from the returned action into the
 * real Forge stack. It never changes legality, targets, costs, choices, stack
 * order, controller results or resolution.
 */
public final class ExpertActionLifecycleAudit {
    private ExpertActionLifecycleAudit() {}

    private static final Map<SpellAbility, Record> RETURNED = new IdentityHashMap<>();
    private static final Map<SpellAbility, Record> STACK = new IdentityHashMap<>();

    private static final class Record {
        final Game game;
        final long captureDecisionIndex;
        final long priorityReturnIndex;
        final String actionIdentity;
        final long returnTurn;
        final String returnPhase;
        final String actor;

        Record(Game game, long captureDecisionIndex, long priorityReturnIndex,
                String actionIdentity, long returnTurn, String returnPhase, String actor) {
            this.game = game;
            this.captureDecisionIndex = captureDecisionIndex;
            this.priorityReturnIndex = priorityReturnIndex;
            this.actionIdentity = actionIdentity;
            this.returnTurn = returnTurn;
            this.returnPhase = returnPhase;
            this.actor = actor;
        }
    }

    public static synchronized void registerReturned(SpellAbility sa, long captureDecisionIndex,
            long priorityReturnIndex, String actionIdentity, long turn, String phase, String actor) {
        if (!Boolean.getBoolean("forge.expert.stage8.lifecycle")) return;
        if (sa == null || sa.getHostCard() == null || sa.getHostCard().getGame() == null
                || captureDecisionIndex < 0 || priorityReturnIndex < 0
                || actionIdentity == null || actionIdentity.isEmpty()) {
            throw new IllegalArgumentException("Invalid Stage 8G returned-action registration");
        }
        if (RETURNED.containsKey(sa) || STACK.containsKey(sa)) {
            throw new IllegalStateException("Stage 8G SpellAbility already registered");
        }
        RETURNED.put(sa, new Record(sa.getHostCard().getGame(), captureDecisionIndex,
                priorityReturnIndex, actionIdentity, turn, phase, actor));
    }

    /** Called by MagicStack after Forge has made any activated-ability copy.
     * originalSa is the object that entered MagicStack.add; stackSa is the exact
     * object that Forge will place in its SpellAbilityStackInstance. */
    public static synchronized void bindStackAbility(SpellAbility originalSa, SpellAbility stackSa) {
        if (!Boolean.getBoolean("forge.expert.stage8.lifecycle")) return;
        Record record = RETURNED.remove(originalSa);
        if (record == null) return; // default/non-full-simulation AI action
        if (stackSa == null || STACK.containsKey(stackSa)) {
            throw new IllegalStateException("Invalid Stage 8G stack binding");
        }
        STACK.put(stackSa, record);
    }

    /** Runs after PlayerControllerAi's existing dispatch attempt. For a
     * successful non-land action the record was already moved to STACK by
     * bindStackAbility. Land actions resolve synchronously in this controller. */
    public static synchronized void noteControllerDispatch(SpellAbility originalSa, boolean success) {
        if (!Boolean.getBoolean("forge.expert.stage8.lifecycle")) return;
        Record record = RETURNED.remove(originalSa);
        if (record == null) return; // stack-bound success or untracked action
        if (!success) {
            emit(record, "dispatch-failed", false, null);
            return;
        }
        if (originalSa.isLandAbility()) {
            emit(record, "no-stack-completed", false, false);
            return;
        }
        // A successful non-land action is expected to have been moved to STACK.
        emit(record, "nonland-success-without-stack-binding", false, null);
    }

    /** Called after Forge emitted GameEventSpellResolved and applied static
     * abilities, but before normal stack cleanup removes the entry. */
    public static synchronized void completeStack(SpellAbility sa, boolean fizzled) {
        if (!Boolean.getBoolean("forge.expert.stage8.lifecycle")) return;
        Record record = STACK.remove(sa);
        if (record == null) return;
        emit(record, fizzled ? "fizzled" : "resolved", true, fizzled);
    }

    /** Any tracked stack entry removed without completeStack is a real
     * pre-resolution terminal event (for example countered/removed). */
    public static synchronized void removedBeforeResolution(SpellAbility sa) {
        if (!Boolean.getBoolean("forge.expert.stage8.lifecycle")) return;
        Record record = STACK.remove(sa);
        if (record == null) return;
        emit(record, "removed-before-resolution", true, null);
    }

    private static void emit(Record record, String outcome, boolean stackBased, Boolean fizzled) {
        Game game = record.game;
        PhaseType phase = game.getPhaseHandler().getPhase();
        StringBuilder out = new StringBuilder(896);
        out.append('{');
        out.append("\"schema_version\":\"stage8g-action-terminal-v1\",");
        out.append("\"capture_decision_index\":").append(record.captureDecisionIndex).append(',');
        out.append("\"priority_return_index\":").append(record.priorityReturnIndex).append(',');
        out.append("\"action_identity\":\"").append(jsonEscape(record.actionIdentity)).append("\",");
        out.append("\"acting_player_name\":\"").append(jsonEscape(record.actor)).append("\",");
        out.append("\"return_turn\":").append(record.returnTurn).append(',');
        out.append("\"return_phase\":\"").append(jsonEscape(record.returnPhase)).append("\",");
        out.append("\"terminal_turn\":").append(game.getPhaseHandler().getTurn()).append(',');
        out.append("\"terminal_phase\":\"").append(jsonEscape(phase == null ? "<none>" : phase.name())).append("\",");
        out.append("\"outcome\":\"").append(jsonEscape(outcome)).append("\",");
        out.append("\"stack_based\":").append(stackBased).append(',');
        out.append("\"fizzled\":").append(fizzled == null ? "null" : fizzled.toString()).append(',');
        out.append("\"boundary\":\"forge-action-terminal\",");
        out.append("\"promotion_allowed\":false");
        System.out.println("EXPERT_STAGE8_ACTION_TERMINAL: " + out.append('}'));
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


def patch_stage8f_bridge(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    old = """        PENDING.put(sa, new ReturnedAction(captureDecisionIndex, priorityReturnIndex,
                actionIdentity, turn, phase, actor));
"""
    new = old + """        forge.game.ExpertActionLifecycleAudit.registerReturned(sa, captureDecisionIndex,
                priorityReturnIndex, actionIdentity, turn, phase, actor);
"""
    text = replace_once(text, old, new, "lifecycle returned-action registration")
    path.write_text(text, encoding="utf-8")


def patch_computer_util(path: Path) -> None:
    # Stage 8G v2 intentionally does not bind in ComputerUtil. MagicStack may
    # replace an activated SpellAbility with a fresh stack copy, so binding here
    # would retain the reusable pre-stack object and miss resolution.
    text = path.read_text(encoding="utf-8")
    if "ExpertActionLifecycleAudit.bindStackAbility" in text:
        raise RuntimeError("Unexpected pre-stack Stage 8G lifecycle binding in ComputerUtil")


def patch_player_controller(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    old = """        ExpertActionAuditBridge.emitControllerAcceptance(sa, player, expertDispatchSuccess, true);
        return true;
"""
    new = """        ExpertActionAuditBridge.emitControllerAcceptance(sa, player, expertDispatchSuccess, true);
        ExpertActionLifecycleAudit.noteControllerDispatch(sa, expertDispatchSuccess);
        return true;
"""
    text = replace_once(text, old, new, "controller lifecycle dispatch note")
    path.write_text(text, encoding="utf-8")


def patch_magic_stack(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        """    public final void add(SpellAbility sp, SpellAbilityStackInstance si, int id) {
        final Card source = sp.getHostCard();
""",
        """    public final void add(SpellAbility sp, SpellAbilityStackInstance si, int id) {
        // Preserve the exact object that entered MagicStack.add. Forge may copy
        // activated abilities below; the lifecycle registry must follow that
        // new stack object, not the reusable pre-stack ability.
        final SpellAbility expertStage8ReturnedSa = sp;
        final Card source = sp.getHostCard();
""",
        "MagicStack incoming returned-action identity",
    )
    text = replace_once(
        text,
        """        if (frozen && !sp.hasParam("IgnoreFreeze") && !sp.isCastFromPlayEffect()) {
""",
        """        ExpertActionLifecycleAudit.bindStackAbility(expertStage8ReturnedSa, sp);

        if (frozen && !sp.hasParam("IgnoreFreeze") && !sp.isCastFromPlayEffect()) {
""",
        "post-copy exact stack binding",
    )
    old = """        game.fireEvent(new GameEventSpellResolved(sa, thisHasFizzled));

        game.getAction().checkStaticAbilities();

        finishResolving(sa, thisHasFizzled);
"""
    new = """        game.fireEvent(new GameEventSpellResolved(sa, thisHasFizzled));

        game.getAction().checkStaticAbilities();
        ExpertActionLifecycleAudit.completeStack(sa, thisHasFizzled);

        finishResolving(sa, thisHasFizzled);
"""
    text = replace_once(text, old, new, "normal/fizzled resolution completion")
    text = replace_once(
        text,
        """    public final void remove(final SpellAbilityStackInstance si) {
        stack.remove(si);
""",
        """    public final void remove(final SpellAbilityStackInstance si) {
        ExpertActionLifecycleAudit.removedBeforeResolution(si.getSpellAbility());
        stack.remove(si);
""",
        "pre-resolution stack removal",
    )
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--forge-game-java", required=True, type=Path)
    parser.add_argument("--expert-bridge", required=True, type=Path)
    parser.add_argument("--computer-util", required=True, type=Path)
    parser.add_argument("--player-controller-ai", required=True, type=Path)
    parser.add_argument("--magic-stack", required=True, type=Path)
    args = parser.parse_args()

    lifecycle = args.forge_game_java / "forge" / "game" / "ExpertActionLifecycleAudit.java"
    if lifecycle.exists():
        raise RuntimeError(f"Refusing to overwrite existing Forge source: {lifecycle}")
    lifecycle.write_text(LIFECYCLE_JAVA, encoding="utf-8")

    patch_stage8f_bridge(args.expert_bridge)
    patch_computer_util(args.computer_util)
    patch_player_controller(args.player_controller_ai)
    patch_magic_stack(args.magic_stack)
    print("Patched Stage 8G terminal action lifecycle into Forge")


if __name__ == "__main__":
    main()
