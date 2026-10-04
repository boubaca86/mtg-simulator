from __future__ import annotations

import argparse
from pathlib import Path


JAVA = r'''package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 7A legal-information boundary for future value-model datasets.
 *
 * This class intentionally receives the real Forge Player/Game and exports only
 * information legally available to the acting player.  In particular it never
 * enumerates an opponent hand or either library.  Hidden zones contribute counts
 * only.  Forge remains the rules referee; this is observation-only instrumentation.
 */
public final class LegalDecisionFeatures {
    public static final String SCHEMA_VERSION = "stage7a-v1";

    private LegalDecisionFeatures() {}

    private static List<String> sortedNames(CardCollectionView cards) {
        List<String> names = new ArrayList<>();
        for (Card card : cards) {
            names.add(card.getName());
        }
        Collections.sort(names);
        return names;
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
        if (actor == null || completeActionIdentity == null || completeActionIdentity.isEmpty()) {
            throw new IllegalArgumentException("actor and complete action identity are required");
        }
        if (infosetSampleCount < 1) {
            throw new IllegalArgumentException("infoset sample count must be positive");
        }
        if (actor.getOpponents().isEmpty()) {
            throw new IllegalArgumentException("Stage 7A extractor requires an opponent");
        }

        Player opponent = actor.getOpponents().get(0);
        Game game = actor.getGame();
        StringBuilder out = new StringBuilder(768);
        out.append('{');
        field(out, "schema_version", SCHEMA_VERSION).append(',');
        number(out, "run_seed", runSeed).append(',');
        number(out, "decision_index", decisionIndex).append(',');
        number(out, "acting_player", actor.getId()).append(',');
        number(out, "turn", game.getPhaseHandler().getTurn()).append(',');
        field(out, "phase", game.getPhaseHandler().getPhase().name()).append(',');
        number(out, "acting_life", actor.getLife()).append(',');
        number(out, "opponent_life", opponent.getLife()).append(',');
        raw(out, "own_hand", jsonStrings(sortedNames(actor.getCardsIn(ZoneType.Hand)))).append(',');

        // SECURITY BOUNDARY: never enumerate opponent Hand or either Library.
        number(out, "opponent_unknown_hand_count", opponent.getCardsIn(ZoneType.Hand).size()).append(',');
        number(out, "own_library_count", actor.getCardsIn(ZoneType.Library).size()).append(',');
        number(out, "opponent_library_count", opponent.getCardsIn(ZoneType.Library).size()).append(',');

        raw(out, "battlefield_public", jsonStrings(sortedNames(game.getCardsIn(ZoneType.Battlefield)))).append(',');
        raw(out, "graveyard_public", jsonStrings(sortedNames(game.getCardsIn(ZoneType.Graveyard)))).append(',');
        raw(out, "exile_public", jsonStrings(sortedNames(game.getCardsIn(ZoneType.Exile)))).append(',');
        field(out, "stack_public", game.getStack().toString()).append(',');
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
    print(f"Added Stage 7A legal-information extractor: {target}")


if __name__ == "__main__":
    main()
