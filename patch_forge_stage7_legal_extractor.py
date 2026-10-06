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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static String targetDescriptor(String zone, String role, Card card) {
        return "zone=" + zone + "|role=" + role + "|" + visibleDescriptor(card);
    }

    private static void addTargetCards(List<String> out, Set<Integer> matched, Set<Integer> targetIds,
            CardCollectionView cards, String zone, String role) {
        for (Card card : cards) {
            if (targetIds.contains(card.getId())) {
                out.add(targetDescriptor(zone, role, card));
                matched.add(card.getId());
            }
        }
    }

    /**
     * Resolve transient Forge object IDs only inside the rules/referee boundary,
     * then discard those IDs. The learner receives public descriptors, never raw
     * IDs that could accidentally correlate with deck construction or hidden order.
     */
    public static String describeActionTargetsJson(Player actor, String actionIdentity) {
        if (actor == null || actionIdentity == null) throw new IllegalArgumentException("actor and action are required");
        int start = actionIdentity.indexOf("|targets=");
        int end = actionIdentity.indexOf("|choices=", start < 0 ? 0 : start);
        if (start < 0 || end < 0 || end < start) throw new IllegalArgumentException("malformed complete action identity");
        String targetText = actionIdentity.substring(start + "|targets=".length(), end);
        Matcher matcher = Pattern.compile("\\((\\d+)\\)").matcher(targetText);
        Set<Integer> targetIds = new LinkedHashSet<>();
        while (matcher.find()) targetIds.add(Integer.parseInt(matcher.group(1)));
        if (targetIds.isEmpty()) return "[]";

        Player opponent = actor.getOpponents().isEmpty() ? null : actor.getOpponents().get(0);
        Game game = actor.getGame();
        List<String> values = new ArrayList<>();
        Set<Integer> matched = new LinkedHashSet<>();

        addTargetCards(values, matched, targetIds, actor.getCardsIn(ZoneType.Hand), "own_hand", "self");
        addTargetCards(values, matched, targetIds, actor.getCardsIn(ZoneType.Battlefield), "own_battlefield", "self");
        addTargetCards(values, matched, targetIds, actor.getCardsIn(ZoneType.Graveyard), "own_graveyard", "self");
        if (opponent != null) {
            // SECURITY: opponent Hand and both Libraries are intentionally never scanned.
            addTargetCards(values, matched, targetIds, opponent.getCardsIn(ZoneType.Battlefield), "opponent_battlefield", "opponent");
            addTargetCards(values, matched, targetIds, opponent.getCardsIn(ZoneType.Graveyard), "opponent_graveyard", "opponent");
        }
        addTargetCards(values, matched, targetIds, game.getCardsIn(ZoneType.Exile), "exile_public", "public");
        for (SpellAbilityStackInstance e : game.getStack()) {
            Card card = e.getSpellAbility().getHostCard();
            if (targetIds.contains(card.getId())) {
                values.add(targetDescriptor("stack_public", "public", card));
                matched.add(card.getId());
            }
        }
        for (int i = matched.size(); i < targetIds.size(); i++) {
            values.add("zone=unresolved|role=unknown|<opaque>");
        }
        Collections.sort(values);
        return jsonStrings(values);
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
