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

/** Legal-information boundary for expert-AI datasets. Forge remains referee. */
public final class LegalDecisionFeatures {
    public static final String SCHEMA_VERSION = "stage7d-v1";
    private LegalDecisionFeatures() {}

    private static String visibleName(Card card) {
        return card.isFaceDown() ? "<face-down>" : card.getName();
    }

    /**
     * Public/legally-known semantics only. A face-down object is deliberately
     * opaque. These values describe Forge's current visible game object; they do
     * not reinterpret or alter any card rule.
     */
    private static String visibleDescriptor(Card card) {
        if (card.isFaceDown()) return "<face-down>";
        StringBuilder d = new StringBuilder();
        d.append(card.getName()).append("|mv=").append(card.getCMC());
        d.append("|type=").append(card.getType().toString());
        if (card.isCreature()) {
            d.append("|p=").append(card.getNetPower());
            d.append("|t=").append(card.getNetToughness());
        }
        return d.toString();
    }

    private static List<String> sortedNames(CardCollectionView cards) {
        List<String> values = new ArrayList<>();
        for (Card card : cards) values.add(visibleName(card));
        Collections.sort(values); return values;
    }
    private static List<String> sortedDescriptors(CardCollectionView cards) {
        List<String> values = new ArrayList<>();
        for (Card card : cards) values.add(visibleDescriptor(card));
        Collections.sort(values); return values;
    }
    private static List<String> stackNames(Game game) {
        List<String> values = new ArrayList<>();
        for (SpellAbilityStackInstance e : game.getStack()) values.add(visibleName(e.getSpellAbility().getHostCard()));
        return values;
    }
    private static List<String> stackDescriptors(Game game) {
        List<String> values = new ArrayList<>();
        for (SpellAbilityStackInstance e : game.getStack()) values.add(visibleDescriptor(e.getSpellAbility().getHostCard()));
        return values;
    }
    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    private static String jsonStrings(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i=0;i<values.size();i++) { if (i>0) out.append(','); out.append('\"').append(jsonEscape(values.get(i))).append('\"'); }
        return out.append(']').toString();
    }

    public static String export(Player actor, String completeActionIdentity, int infosetSampleCount,
            long runSeed, long decisionIndex, String matchupId) {
        if (actor == null || completeActionIdentity == null || completeActionIdentity.isEmpty()) throw new IllegalArgumentException("actor and complete action identity are required");
        if (infosetSampleCount < 1) throw new IllegalArgumentException("infoset sample count must be positive");
        if (actor.getOpponents().isEmpty()) throw new IllegalArgumentException("Stage 7 extractor requires an opponent");
        Player opponent=actor.getOpponents().get(0); Game game=actor.getGame(); StringBuilder out=new StringBuilder(2048); out.append('{');
        field(out,"schema_version",SCHEMA_VERSION).append(','); number(out,"run_seed",runSeed).append(','); number(out,"decision_index",decisionIndex).append(',');
        number(out,"acting_player",actor.getId()).append(','); field(out,"acting_player_name",actor.getLobbyPlayer().getName()).append(','); number(out,"turn",game.getPhaseHandler().getTurn()).append(','); field(out,"phase",game.getPhaseHandler().getPhase().name()).append(',');
        number(out,"acting_life",actor.getLife()).append(','); number(out,"opponent_life",opponent.getLife()).append(',');
        raw(out,"own_hand",jsonStrings(sortedNames(actor.getCardsIn(ZoneType.Hand)))).append(',');
        raw(out,"own_hand_semantics",jsonStrings(sortedDescriptors(actor.getCardsIn(ZoneType.Hand)))).append(',');
        // SECURITY BOUNDARY: opponent Hand and both Libraries are counts only.
        number(out,"opponent_unknown_hand_count",opponent.getCardsIn(ZoneType.Hand).size()).append(','); number(out,"own_library_count",actor.getCardsIn(ZoneType.Library).size()).append(','); number(out,"opponent_library_count",opponent.getCardsIn(ZoneType.Library).size()).append(',');
        raw(out,"own_battlefield",jsonStrings(sortedNames(actor.getCardsIn(ZoneType.Battlefield)))).append(','); raw(out,"own_battlefield_semantics",jsonStrings(sortedDescriptors(actor.getCardsIn(ZoneType.Battlefield)))).append(',');
        raw(out,"opponent_battlefield",jsonStrings(sortedNames(opponent.getCardsIn(ZoneType.Battlefield)))).append(','); raw(out,"opponent_battlefield_semantics",jsonStrings(sortedDescriptors(opponent.getCardsIn(ZoneType.Battlefield)))).append(',');
        raw(out,"own_graveyard",jsonStrings(sortedNames(actor.getCardsIn(ZoneType.Graveyard)))).append(','); raw(out,"own_graveyard_semantics",jsonStrings(sortedDescriptors(actor.getCardsIn(ZoneType.Graveyard)))).append(',');
        raw(out,"opponent_graveyard",jsonStrings(sortedNames(opponent.getCardsIn(ZoneType.Graveyard)))).append(','); raw(out,"opponent_graveyard_semantics",jsonStrings(sortedDescriptors(opponent.getCardsIn(ZoneType.Graveyard)))).append(',');
        raw(out,"exile_public",jsonStrings(sortedNames(game.getCardsIn(ZoneType.Exile)))).append(','); raw(out,"exile_public_semantics",jsonStrings(sortedDescriptors(game.getCardsIn(ZoneType.Exile)))).append(',');
        raw(out,"stack_public",jsonStrings(stackNames(game))).append(','); raw(out,"stack_public_semantics",jsonStrings(stackDescriptors(game))).append(',');
        field(out,"matchup_id",matchupId==null?"":matchupId).append(','); field(out,"complete_action_identity",completeActionIdentity).append(','); number(out,"infoset_sample_count",infosetSampleCount).append(','); field(out,"search_policy","fixed-root-v1"); return out.append('}').toString();
    }
    private static StringBuilder field(StringBuilder out,String key,String value){return out.append('\"').append(key).append("\":\"").append(jsonEscape(value)).append('\"');}
    private static StringBuilder number(StringBuilder out,String key,long value){return out.append('\"').append(key).append("\":").append(value);}
    private static StringBuilder raw(StringBuilder out,String key,String value){return out.append('\"').append(key).append("\":").append(value);}
}
'''

def main() -> None:
    p=argparse.ArgumentParser(); p.add_argument("forge_ai_java",type=Path); a=p.parse_args()
    target=a.forge_ai_java/"forge"/"ai"/"simulation"/"LegalDecisionFeatures.java"; target.parent.mkdir(parents=True,exist_ok=True)
    if target.exists(): raise RuntimeError(f"Refusing to overwrite existing Forge source: {target}")
    target.write_text(JAVA,encoding="utf-8"); print(f"Added Stage 7 legal-information extractor: {target}")

if __name__=="__main__": main()
