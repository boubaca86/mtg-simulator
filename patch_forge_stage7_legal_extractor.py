from __future__ import annotations

import argparse
from pathlib import Path

JAVA = r'''package forge.ai.simulation;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static void addTargetCards(Map<Integer, String> out, Set<Integer> targetIds,
            CardCollectionView cards, String zone, String role) {
        for (Card card : cards) {
            if (targetIds.contains(card.getId())) {
                out.put(card.getId(), targetDescriptor(zone, role, card));
            }
        }
    }

    public static final String TARGET_SEMANTICS_VERSION = "forge-public-targets-v2";

    /** Internal reference only: never serialize the ID or a sampled-world card. */
    public static final class TargetReference {
        final String kind;
        final int objectId;
        private TargetReference(String kind, int objectId) { this.kind = kind; this.objectId = objectId; }
    }

    /** Snapshot types/IDs while Forge still holds the selected target objects.
     * Names such as Ai(2), card names and display punctuation are never parsed. */
    public static List<TargetReference> captureTargetReferences(Iterable<GameObject> targets) {
        List<TargetReference> refs = new ArrayList<>();
        for (GameObject target : targets) {
            if (target instanceof Card card) refs.add(new TargetReference("card", card.getId()));
            else if (target instanceof Player player) refs.add(new TargetReference("player", player.getId()));
            else if (target instanceof SpellAbility spell) refs.add(new TargetReference("spell", spell.getHostCard().getId()));
            else refs.add(new TargetReference("unknown", -1));
        }
        return Collections.unmodifiableList(refs);
    }

    private static String roleFor(Player actor, Player player) {
        if (player == null) return "unknown";
        if (player.getId() == actor.getId()) return "self";
        return actor.getOpponents().contains(player) ? "opponent" : "public";
    }

    /** Resolve only against the actual root's legal zones, not sampled objects.
     * Keep target order and multiplicity, and keep player/card ID namespaces apart. */
    public static String describeActionTargetsJson(Player actor, MultiTargetSelector.Targets targets) {
        if (actor == null) throw new IllegalArgumentException("actor is required");
        if (targets == null) return "[]";
        List<TargetReference> refs = targets.publicTargetReferences();
        Set<Integer> cardIds = new LinkedHashSet<>();
        Set<Integer> spellIds = new LinkedHashSet<>();
        for (TargetReference ref : refs) {
            if (ref.kind.equals("card")) cardIds.add(ref.objectId);
            else if (ref.kind.equals("spell")) spellIds.add(ref.objectId);
        }

        Player opponent = actor.getOpponents().isEmpty() ? null : actor.getOpponents().get(0);
        Game game = actor.getGame();
        Map<Integer, String> cards = new LinkedHashMap<>();
        Map<Integer, String> spells = new LinkedHashMap<>();
        if (!cardIds.isEmpty()) {
            addTargetCards(cards, cardIds, actor.getCardsIn(ZoneType.Hand), "own_hand", "self");
            addTargetCards(cards, cardIds, actor.getCardsIn(ZoneType.Battlefield), "own_battlefield", "self");
            addTargetCards(cards, cardIds, actor.getCardsIn(ZoneType.Graveyard), "own_graveyard", "self");
            if (opponent != null) {
                // SECURITY: opponent Hand and both Libraries are intentionally never scanned.
                addTargetCards(cards, cardIds, opponent.getCardsIn(ZoneType.Battlefield), "opponent_battlefield", "opponent");
                addTargetCards(cards, cardIds, opponent.getCardsIn(ZoneType.Graveyard), "opponent_graveyard", "opponent");
            }
            addTargetCards(cards, cardIds, game.getCardsIn(ZoneType.Exile), "exile_public", "public");
            addTargetCards(cards, cardIds, game.getCardsIn(ZoneType.Stack), "stack_public", "public");
        }
        if (!spellIds.isEmpty()) for (SpellAbilityStackInstance e : game.getStack()) {
            Card card = e.getSpellAbility().getHostCard();
            if (spellIds.contains(card.getId())) {
                spells.put(card.getId(), targetDescriptor("stack_public", roleFor(actor, e.getSpellAbility().getActivatingPlayer()), card));
            }
        }
        List<String> values = new ArrayList<>();
        for (TargetReference ref : refs) {
            String value = null;
            if (ref.kind.equals("card")) value = cards.get(ref.objectId);
            else if (ref.kind.equals("spell")) value = spells.get(ref.objectId);
            else if (ref.kind.equals("player")) {
                for (Player player : game.getPlayers()) if (player.getId() == ref.objectId) {
                    value = "zone=player|role=" + roleFor(actor, player) + "|<player>";
                    break;
                }
            }
            values.add(value == null ? "zone=unresolved|role=unknown|<opaque>" : value);
        }
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


def patch_target_references(root: Path) -> None:
    """Retain typed immutable targets alongside Forge's unchanged replay recipe."""
    folder = root / "forge" / "ai" / "simulation"
    edits = {
        folder / "PossibleTargetSelector.java": [
            ('        final String description;',
             '        final String description;\n        final List<LegalDecisionFeatures.TargetReference> publicTargetReferences;'),
            ('int targetIndex, String description)  {',
             'int targetIndex, String description, List<LegalDecisionFeatures.TargetReference> refs)  {'),
            ('            this.description = description;',
             '            this.description = description;\n            this.publicTargetReferences = java.util.Collections.unmodifiableList(new ArrayList<>(refs));'),
            ('nextTargetIndex - 1, targetingSa.getTargets().toString());',
             'nextTargetIndex - 1, targetingSa.getTargets().toString(), LegalDecisionFeatures.captureTargetReferences(targetingSa.getTargets()));'),
        ],
        folder / "MultiTargetSelector.java": [
            ('        private ArrayList<PossibleTargetSelector.Targets> targets;',
             '''        private ArrayList<PossibleTargetSelector.Targets> targets;

        public List<LegalDecisionFeatures.TargetReference> publicTargetReferences() {
            List<LegalDecisionFeatures.TargetReference> refs = new ArrayList<>();
            for (PossibleTargetSelector.Targets target : targets) refs.addAll(target.publicTargetReferences);
            return java.util.Collections.unmodifiableList(refs);
        }'''),
        ],
    }
    prepared = {}
    for path, replacements in edits.items():
        text = path.read_text()
        for old, new in replacements:
            if text.count(old) != 1:
                raise RuntimeError(f"Expected one typed-target patch site in {path.name}: {old}")
            text = text.replace(old, new, 1)
        prepared[path] = text
    for path, text in prepared.items():
        path.write_text(text)

def main() -> None:
    p=argparse.ArgumentParser(); p.add_argument("forge_ai_java",type=Path); a=p.parse_args()
    target=a.forge_ai_java/"forge"/"ai"/"simulation"/"LegalDecisionFeatures.java"; target.parent.mkdir(parents=True,exist_ok=True)
    if target.exists(): raise RuntimeError(f"Refusing to overwrite existing Forge source: {target}")
    patch_target_references(a.forge_ai_java)
    target.write_text(JAVA,encoding="utf-8"); print(f"Added Stage 7 legal-information extractor: {target}")

if __name__=="__main__": main()
