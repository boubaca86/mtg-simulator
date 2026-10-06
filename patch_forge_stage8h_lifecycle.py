from __future__ import annotations

import argparse
from pathlib import Path


LIFECYCLE_JAVA = r'''package forge.game;

import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/** Observation-only lifecycle registry for expert-AI root actions.
 *
 * Forge legitimately reuses a SpellAbility object for later activations, while
 * each real stack entry has its own SpellAbilityStackInstance. Returned records
 * are therefore queued by reusable ability only until Forge creates the stack
 * instance; terminal tracking is keyed by exact stack-instance identity.
 *
 * Nothing here changes legality, targets, costs, choices, stack order,
 * controller results or resolution.
 */
public final class ExpertActionLifecycleAudit {
    private ExpertActionLifecycleAudit() {}

    private static final Map<SpellAbility, ArrayDeque<Record>> RETURNED = new IdentityHashMap<>();
    private static final Map<SpellAbility, SpellAbility> STACK_ALIASES = new IdentityHashMap<>();
    private static final Map<SpellAbilityStackInstance, Record> STACK = new IdentityHashMap<>();

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

    private static boolean enabled() {
        return Boolean.getBoolean("forge.expert.stage8.lifecycle");
    }

    public static synchronized void registerReturned(SpellAbility sa, long captureDecisionIndex,
            long priorityReturnIndex, String actionIdentity, long turn, String phase, String actor) {
        if (!enabled()) return;
        if (sa == null || sa.getHostCard() == null || sa.getHostCard().getGame() == null
                || captureDecisionIndex < 0 || priorityReturnIndex < 0
                || actionIdentity == null || actionIdentity.isEmpty()) {
            throw new IllegalArgumentException("Invalid Stage 8H returned-action registration");
        }
        RETURNED.computeIfAbsent(sa, key -> new ArrayDeque<>()).addLast(
                new Record(sa.getHostCard().getGame(), captureDecisionIndex,
                        priorityReturnIndex, actionIdentity, turn, phase, actor));
    }

    /** ComputerUtil may replace the returned SpellAbility (for example splice)
     * before MagicStack sees it. Keep an identity-only alias until the real stack
     * instance is created. No record is consumed at this point. */
    public static synchronized void prepareStackBinding(SpellAbility returnedSa, SpellAbility stackCandidate) {
        if (!enabled()) return;
        if (returnedSa == null || stackCandidate == null) {
            throw new IllegalArgumentException("Stage 8H stack binding requires abilities");
        }
        ArrayDeque<Record> queue = RETURNED.get(returnedSa);
        if (queue == null || queue.isEmpty() || stackCandidate == returnedSa) return;
        SpellAbility prior = STACK_ALIASES.put(stackCandidate, returnedSa);
        if (prior != null && prior != returnedSa) {
            throw new IllegalStateException("Stage 8H transformed ability alias collision");
        }
    }

    /** Bind one returned record to the exact stack instance Forge just created.
     * Activated abilities are copied by MagicStack before push(); Forge stores
     * their reusable source ability in getOriginalAbility(), so that identity is
     * also checked when no explicit transformation alias exists. */
    public static synchronized void bindStackInstance(
            SpellAbility stackSa, SpellAbilityStackInstance stackInstance) {
        if (!enabled()) return;
        if (stackSa == null || stackInstance == null) {
            throw new IllegalArgumentException("Stage 8H stack instance is required");
        }

        SpellAbility source = STACK_ALIASES.remove(stackSa);
        if (source == null && hasReturned(stackSa)) {
            source = stackSa;
        }
        if (source == null) {
            SpellAbility original = stackSa.getOriginalAbility();
            if (original != null && hasReturned(original)) {
                source = original;
            }
        }
        if (source == null) return; // trigger/default-AI/untracked stack entry

        Record record = pollReturned(source);
        if (record == null) {
            throw new IllegalStateException("Stage 8H stack binding lost returned record");
        }
        if (STACK.put(stackInstance, record) != null) {
            throw new IllegalStateException("Stage 8H stack instance already tracked");
        }
        clearAliasesFor(source);
    }

    /** Runs after PlayerControllerAi's existing dispatch attempt. A successful
     * non-land action should already have been consumed by bindStackInstance().
     * If its record remains here, Forge reported controller success without
     * producing a tracked stack entry; surface that as an audit anomaly. */
    public static synchronized void noteControllerDispatch(SpellAbility returnedSa, boolean success) {
        if (!enabled()) return;
        if (returnedSa == null) {
            throw new IllegalArgumentException("Stage 8H controller dispatch requires an ability");
        }

        if (!success) {
            Record record = pollReturned(returnedSa);
            clearAliasesFor(returnedSa);
            if (record != null) emit(record, "dispatch-failed", false, null);
            return;
        }

        if (returnedSa.isLandAbility()) {
            Record record = pollReturned(returnedSa);
            clearAliasesFor(returnedSa);
            if (record != null) emit(record, "no-stack-completed", false, false);
            return;
        }

        Record unbound = pollReturned(returnedSa);
        clearAliasesFor(returnedSa);
        if (unbound != null) {
            emit(unbound, "nonland-success-without-stack-binding", false, null);
        }
    }

    /** Called after Forge emitted GameEventSpellResolved and applied static
     * abilities, but before normal stack cleanup removes the entry. */
    public static synchronized void completeStack(
            SpellAbilityStackInstance stackInstance, boolean fizzled) {
        if (!enabled()) return;
        Record record = STACK.remove(stackInstance);
        if (record == null) return;
        emit(record, fizzled ? "fizzled" : "resolved", true, fizzled);
    }

    /** Any tracked stack entry removed without completeStack is a genuine
     * pre-resolution terminal event (for example a countered/removed entry). */
    public static synchronized void removedBeforeResolution(SpellAbilityStackInstance stackInstance) {
        if (!enabled()) return;
        if (stackInstance == null) return;
        Record record = STACK.remove(stackInstance);
        if (record == null) return;
        emit(record, "removed-before-resolution", true, null);
    }

    private static boolean hasReturned(SpellAbility sa) {
        ArrayDeque<Record> queue = RETURNED.get(sa);
        return queue != null && !queue.isEmpty();
    }

    private static Record pollReturned(SpellAbility sa) {
        ArrayDeque<Record> queue = RETURNED.get(sa);
        if (queue == null || queue.isEmpty()) return null;
        Record record = queue.removeFirst();
        if (queue.isEmpty()) RETURNED.remove(sa);
        return record;
    }

    private static void clearAliasesFor(SpellAbility source) {
        Iterator<Map.Entry<SpellAbility, SpellAbility>> it = STACK_ALIASES.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() == source) it.remove();
        }
    }

    private static void emit(Record record, String outcome, boolean stackBased, Boolean fizzled) {
        Game game = record.game;
        PhaseType phase = game.getPhaseHandler().getPhase();
        StringBuilder out = new StringBuilder(896);
        out.append('{');
        out.append("\"schema_version\":\"stage8h-action-terminal-v1\",");
        out.append("\"capture_decision_index\":").append(record.captureDecisionIndex).append(',');
        out.append("\"priority_return_index\":").append(record.priorityReturnIndex).append(',');
        out.append("\"action_identity\":\"").append(jsonEscape(record.actionIdentity)).append("\",");
        out.append("\"acting_player_name\":\"").append(jsonEscape(record.actor)).append("\",");
        out.append("\"return_turn\":").append(record.returnTurn).append(',');
        out.append("\"return_phase\":\"").append(jsonEscape(record.returnPhase)).append("\",");
        out.append("\"terminal_turn\":").append(game.getPhaseHandler().getTurn()).append(',');
        out.append("\"terminal_phase\":\"")
                .append(jsonEscape(phase == null ? "<none>" : phase.name())).append("\",");
        out.append("\"outcome\":\"").append(jsonEscape(outcome)).append("\",");
        out.append("\"stack_based\":").append(stackBased).append(',');
        out.append("\"fizzled\":").append(fizzled == null ? "null" : fizzled.toString()).append(',');
        out.append("\"boundary\":\"forge-action-terminal\",");
        out.append("\"promotion_allowed\":false");
        System.out.println("EXPERT_STAGE8H_ACTION_TERMINAL: " + out.append('}'));
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
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        """    public static boolean handlePlayingSpellAbility(final Player ai, SpellAbility sa, Consumer<SpellAbility> chooseTargets) {
        final Card source = sa.getHostCard();
""",
        """    public static boolean handlePlayingSpellAbility(final Player ai, SpellAbility sa, Consumer<SpellAbility> chooseTargets) {
        final SpellAbility expertStage8ReturnedSa = sa;
        final Card source = sa.getHostCard();
""",
        "ComputerUtil returned SpellAbility capture",
    )
    text = replace_once(
        text,
        """        if (pay.payComputerCosts(new AiCostDecision(ai, sa, false))) {
            game.getStack().addAndUnfreeze(sa);
            if (sa.getSplicedCards() != null && !sa.getSplicedCards().isEmpty()) {
""",
        """        if (pay.payComputerCosts(new AiCostDecision(ai, sa, false))) {
            ExpertActionLifecycleAudit.prepareStackBinding(expertStage8ReturnedSa, sa);
            game.getStack().addAndUnfreeze(sa);
            if (sa.getSplicedCards() != null && !sa.getSplicedCards().isEmpty()) {
""",
        "successful stack preparation",
    )
    path.write_text(text, encoding="utf-8")


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
        """        si = si == null ? new SpellAbilityStackInstance(sp, id) : si;

        stack.addFirst(si);
""",
        """        si = si == null ? new SpellAbilityStackInstance(sp, id) : si;
        ExpertActionLifecycleAudit.bindStackInstance(sp, si);

        stack.addFirst(si);
""",
        "exact stack-instance binding",
    )

    text = replace_once(
        text,
        """        // The SpellAbility isn't removed from the Stack until it finishes resolving
        // temporarily reverted removing SAs after resolution
        final SpellAbility sa = peekAbility();
""",
        """        // The SpellAbility isn't removed from the Stack until it finishes resolving
        // temporarily reverted removing SAs after resolution
        final SpellAbilityStackInstance expertStage8StackInstance = peek();
        final SpellAbility sa = expertStage8StackInstance.getSpellAbility();
""",
        "resolution stack-instance snapshot",
    )

    text = replace_once(
        text,
        """        game.getAction().checkStaticAbilities();

        finishResolving(sa, thisHasFizzled);
""",
        """        game.getAction().checkStaticAbilities();
        ExpertActionLifecycleAudit.completeStack(expertStage8StackInstance, thisHasFizzled);

        finishResolving(sa, thisHasFizzled);
""",
        "normal/fizzled resolution completion",
    )

    text = replace_once(
        text,
        """    public final void remove(final SpellAbilityStackInstance si) {
        stack.remove(si);
""",
        """    public final void remove(final SpellAbilityStackInstance si) {
        ExpertActionLifecycleAudit.removedBeforeResolution(si);
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
    print("Patched Stage 8H terminal action lifecycle into Forge")


if __name__ == "__main__":
    main()
