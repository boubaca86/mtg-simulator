package forge.ai.simulation;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Localizer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Real Forge objects, including colliding card/player IDs and hidden-zone traps. */
public final class Stage8TargetSemanticsRegression {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static Card card(Player owner, int id, String name, int power, ZoneType zone) {
        // Supply real printed rules so getCMC does not consult the global card
        // database, which a focused fixture does not initialize.
        CardRules rules = CardRules.fromScript(List.of("Name:" + name, "ManaCost:2",
                "Types:Creature", "PT:" + power + "/" + power, "Oracle:Regression fixture."));
        Card card = new Card(id, new PaperCard(rules, "TEST", CardRarity.Common), owner.getGame());
        card.setName(name);
        card.setManaCost(rules.getManaCost());
        card.setOwner(owner);
        card.setController(owner, 0);
        card.addType("Creature");
        card.setBasePower(power);
        card.setBaseToughness(power);
        owner.getZone(zone).add(card);
        return card;
    }

    private static MultiTargetSelector.Targets targets(GameObject... objects) throws Exception {
        // Construct Forge's private replay snapshot with exactly the typed refs
        // its patched getLastSelectedTargets captures from these real objects.
        ArrayList<GameObject> source = new ArrayList<>(Arrays.asList(objects));
        List<LegalDecisionFeatures.TargetReference> refs = LegalDecisionFeatures.captureTargetReferences(source);
        Constructor<PossibleTargetSelector.Targets> ctor = PossibleTargetSelector.Targets.class
                .getDeclaredConstructor(int.class, int.class, int.class, String.class, List.class);
        ctor.setAccessible(true);
        PossibleTargetSelector.Targets selected = ctor.newInstance(0, source.size(), 0, source.toString(), refs);
        source.clear();
        MultiTargetSelector.Targets result = new MultiTargetSelector.Targets();
        Field f = MultiTargetSelector.Targets.class.getDeclaredField("targets");
        f.setAccessible(true);
        f.set(result, new ArrayList<>(List.of(selected)));
        return result;
    }

    private static String describe(Player actor, GameObject... objects) throws Exception {
        return LegalDecisionFeatures.describeActionTargetsJson(actor, targets(objects));
    }

    public static void main(String[] args) throws Exception {
        Localizer.getInstance().initialize("en-US", "forge-src/forge-gui/res/languages");
        GameRules rules = new GameRules(GameType.Constructed);
        Game game = new Game(List.of(), rules, new Match(rules, List.of(), "target regression"));
        Player actor = new Player("Ai(1)-Fixture", game, 1);
        Player opponent = new Player("Ai(2)-Fixture with (22), punctuation", game, 2);
        actor.setTeam(1); opponent.setTeam(2);
        game.getPlayers().add(actor); game.getPlayers().add(opponent);
        game.getRegisteredPlayers().add(actor); game.getRegisteredPlayers().add(opponent);

        Card decoy = card(actor, 2, "Decoy with (99), punctuation", 2, ZoneType.Battlefield);
        String player = describe(actor, opponent);
        require(player.equals("[\"zone=player|role=opponent|<player>\"]"), "player number resolved as a card: " + player);
        require(describe(actor, actor).contains("role=self|<player>"), "self target role lost");
        String actualCard = describe(actor, decoy);
        require(actualCard.contains("Decoy with (99), punctuation|mv="), "card display punctuation was parsed as IDs");
        require(!actualCard.contains("<player>"), "card ID resolved in player namespace");

        String ordered = describe(actor, decoy, opponent, decoy);
        int first = ordered.indexOf("Decoy");
        int middle = ordered.indexOf("<player>");
        int last = ordered.lastIndexOf("Decoy");
        require(first >= 0 && first < middle && middle < last, "target order or repeated target was lost");
        MultiTargetSelector.Targets immutable = targets(decoy, opponent);
        boolean rejected = false;
        try { immutable.publicTargetReferences().clear(); } catch (UnsupportedOperationException expected) { rejected = true; }
        require(rejected, "typed target snapshot is mutable");
        require(LegalDecisionFeatures.describeActionTargetsJson(actor, null).equals("[]"), "untargeted action gained targets");

        Card weak = card(opponent, 22, "Goblin", 1, ZoneType.Battlefield);
        Card strong = card(opponent, 23, "Goblin", 5, ZoneType.Battlefield);
        require(describe(actor, weak).contains("|p=1|t=1") && describe(actor, strong).contains("|p=5|t=5"),
                "same-name objects lost their actual public characteristics");
        Card sampledCopy = new Card(22, null);
        sampledCopy.setName("WRONG SAMPLED WORLD CHARACTERISTICS");
        require(describe(actor, sampledCopy).equals(describe(actor, weak)), "sampled-world data crossed root boundary");

        Card faceDown = card(opponent, 30, "SECRET FACE DOWN", 9, ZoneType.Battlefield);
        faceDown.setFaceDown(true);
        require(describe(actor, faceDown).equals("[\"zone=opponent_battlefield|role=opponent|<face-down>\"]"),
                "face-down identity or characteristics leaked");
        Card hand = card(opponent, 40, "SECRET HAND", 9, ZoneType.Hand);
        Card ownLibrary = card(actor, 41, "SECRET OWN LIBRARY", 9, ZoneType.Library);
        Card theirLibrary = card(opponent, 42, "SECRET OPPONENT LIBRARY", 9, ZoneType.Library);
        String hidden = describe(actor, hand, ownLibrary, theirLibrary);
        require(!hidden.contains("SECRET") && !hidden.contains("|p="), "a hidden target was resolved from a restricted zone");
        require(hidden.split("<opaque>", -1).length == 4, "hidden targets were dropped or conflated");
        hand.setName("CHANGED HIDDEN HAND");
        require(describe(actor, hand, ownLibrary, theirLibrary).equals(hidden), "hidden identity changed learner output");
        Card knownHand = card(actor, 43, "Known own hand", 3, ZoneType.Hand);
        require(describe(actor, knownHand).contains("zone=own_hand|role=self|Known own hand"), "legal own-hand target became hidden");
        SpellAbility offStack = new SpellAbility.EmptySa(decoy, null);
        require(describe(actor, offStack).contains("zone=unresolved|role=unknown|<opaque>"),
                "spell target fell back to its battlefield host card");
        System.out.println("Stage 8 typed target regression: PASS (player/card collisions, punctuation, order, multiplicity, immutable snapshots, same-name stats, root-only resolution, hidden zones, face-down opacity, spell namespace)");
    }
}
