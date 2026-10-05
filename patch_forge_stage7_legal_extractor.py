from __future__ import annotations

import argparse
from pathlib import Path


JAVA = r'''package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 7 legal-information boundary for value-model datasets.
 *
 * Exports only information legally available to the acting player. Hidden zones
 * are never enumerated: opponent hand and both libraries contribute counts only.
 * Public zones are controller/owner separated so a learner can distinguish board
 * advantage without receiving secret identities. Forge remains the rules referee.
 */
public final class LegalDecisionFeatures {
    public static final String SCHEMA_VERSION = "stage7c-v3";

    private LegalDecisionFeatures() {}

    private static List<String> sortedNames(CardCollectionView cards) {
        List<String> names = new ArrayList<>();
        for (Card card : cards) names.add(visibleName(card));
        Collections.sort(names);
        return names;
    }

    // Conservatively redact all face-down objects, even if this actor may look.
    // Public zone membership alone never makes a face-down identity public.
    private static String visibleName(Card card) {
        return card.isFaceDown() ? "<face-down>" : card.getName();
    }

    private static List<String> stackNames(Game game) {
        List<String> names = new ArrayList<>();
        for (SpellAbilityStackInstance entry : game.getStack()) {
            names.add(visibleName(entry.getSpellAbility().getHostCard()));
        }
        return names; // preserve stack order; these are objects, not string characters
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String jsonStrings(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append(',');
            out.append('\"').append(jsonEscape(values.get(i))).append('\"');
        }
        return out.append(']').toString();
    }

    /** Deterministic JSON: stable field order and sorted unordered card-name lists. */
    public static String export(Player actor, String completeActionIdentity, int infosetSampleCount,
            long runSeed, long decisionIndex, String matchupId) {
        if (actor == null || completeActionIdentity == null || completeActionIdentity.isEmpty())
            throw new IllegalArgumentException("actor and complete action identity are required");
        if (infosetSampleCount < 1) throw new IllegalArgumentException("infoset sample count must be positive");
        if (actor.getOpponents().isEmpty()) throw new IllegalArgumentException("Stage 7 extractor requires an opponent");

        Player opponent = actor.getOpponents().get(0);
        Game game = actor.getGame();
        StringBuilder out = new StringBuilder(1024);
        out.append('{');
        field(out, "schema_version", SCHEMA_VERSION).append(',');
        number(out, "run_seed", runSeed).append(',');
        number(out, "decision_index", decisionIndex).append(',');
        number(out, "acting_player", actor.getId()).append(',');
        field(out, "acting_player_name", actor.getLobbyPlayer().getName()).append(',');
        number(out, "turn", game.getPhaseHandler().getTurn()).append(',');
        field(out, "phase", game.getPhaseHandler().getPhase().name()).append(',');
        number(out, "acting_life", actor.getLife()).append(',');
        number(out, "opponent_life", opponent.getLife()).append(',');
        raw(out, "own_hand", jsonStrings(sortedNames(actor.getCardsIn(ZoneType.Hand)))).append(',');

        // SECURITY BOUNDARY: never enumerate opponent Hand or either Library.
        number(out, "opponent_unknown_hand_count", opponent.getCardsIn(ZoneType.Hand).size()).append(',');
        number(out, "own_library_count", actor.getCardsIn(ZoneType.Library).size()).append(',');
        number(out, "opponent_library_count", opponent.getCardsIn(ZoneType.Library).size()).append(',');

        // Public information, separated by perspective. This fixes the v1 ambiguity
        // where a learner could see names but not which side controlled/owned them.
        raw(out, "own_battlefield", jsonStrings(sortedNames(actor.getCardsIn(ZoneType.Battlefield)))).append(',');
        raw(out, "opponent_battlefield", jsonStrings(sortedNames(opponent.getCardsIn(ZoneType.Battlefield)))).append(',');
        raw(out, "own_graveyard", jsonStrings(sortedNames(actor.getCardsIn(ZoneType.Graveyard)))).append(',');
        raw(out, "opponent_graveyard", jsonStrings(sortedNames(opponent.getCardsIn(ZoneType.Graveyard)))).append(',');
        raw(out, "exile_public", jsonStrings(sortedNames(game.getCardsIn(ZoneType.Exile)))).append(',');
        raw(out, "stack_public", jsonStrings(stackNames(game))).append(',');
        field(out, "matchup_id", matchupId == null ? "" : matchupId).append(',');
        field(out, "complete_action_identity", completeActionIdentity).append(',');
        number(out, "infoset_sample_count", infosetSampleCount);
        return out.append('}').toString();
    }

    private static StringBuilder field(StringBuilder out, String key, String value) {
        return out.append('\"').append(key).append("\":\"").append(jsonEscape(value)).append('\"');
    }
    private static StringBuilder number(StringBuilder out, String key, long value) {
        return out.append('\"').append(key).append("\":").append(value);
    }
    private static StringBuilder raw(StringBuilder out, String key, String value) {
        return out.append('\"').append(key).append("\":").append(value);
    }
}
'''


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("forge_ai_java", type=Path)
    args = parser.parse_args()
    target = args.forge_ai_java / "forge" / "ai" / "simulation" / "LegalDecisionFeatures.java"
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists():
        raise RuntimeError(f"Refusing to overwrite existing Forge source: {target}")
    target.write_text(JAVA, encoding="utf-8")
    print(f"Added Stage 7 legal-information extractor: {target}")


if __name__ == "__main__":
    main()
