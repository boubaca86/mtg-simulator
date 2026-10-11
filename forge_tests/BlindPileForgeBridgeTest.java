package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 * Stage 9D, test-only: real Forge Game/Player/Card/CardView/PlayerZone backing.
 * A trusted referee MUST establish independent authorization, pile group,
 * revision and cost payment; this fixture does not prove spell-specific
 * Magic legality or modify production Forge code. No gameplay matches.
 */
public class BlindPileForgeBridgeTest extends AITest {
    private static final class Grant {
        final Player actor;
        final int revision;
        final List<Card> cards;
        Grant(Player actor, int revision, List<Card> cards) {
            this.actor = actor;
            this.revision = revision;
            this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
        }
    }
    private static final class RefereeBridge {
        private final Map<Player, Map<String, Grant>> grants = new HashMap<>();

        Map<String, String> issue(Player actor, String pileId, int revision,
                int size, List<Card> cards, boolean permitted, boolean qualified) {
            if (actor == null || pileId == null
                    || !pileId.matches("[A-Za-z0-9_-]{1,48}")
                    || revision < 0 || size <= 0 || cards == null
                    || cards.size() != size) {
                throw new IllegalArgumentException("invalid public pile metadata");
            }
            if (!permitted || qualified) {
                throw new SecurityException("referee did not authorize blind choice");
            }
            Set<Card> seen = new HashSet<>();
            for (Card card : cards) {
                if (card == null || !seen.add(card)
                        || !card.isInZone(ZoneType.Exile) || !card.isFaceDown()
                        || card.getView().canBeShownTo(actor.getView())) {
                    throw new SecurityException("not an opaque exile pile");
                }
            }
            String id = "blind-pile:" + pileId + ":revision:" + revision;
            Map<String, Grant> byId = grants.computeIfAbsent(actor, ignored -> new HashMap<>());
            if (byId.containsKey(id)) {
                throw new IllegalStateException("duplicate grant");
            }
            byId.put(id, new Grant(actor, revision, cards));
            Map<String, String> visible = new LinkedHashMap<>();
            visible.put("id", id);
            visible.put("kind", "choose-blind-exile-pile");
            visible.put("public_cost", "");
            visible.put("public_target", "face-down exile pile " + pileId + " (" + size + " cards)");
            visible.put("public_effect", "Referee randomly selects one card after required costs are paid");
            return Collections.unmodifiableMap(visible);
        }

        /** Private result belongs only to the referee, never to the AI. */
        Card redeem(Player actor, String id, int revision, boolean costsPaid,
                IntUnaryOperator randomIndex) {
            Map<String, Grant> byId = grants.get(actor);
            Grant grant = byId == null ? null : byId.get(id);
            if (grant == null || grant.actor != actor) {
                throw new SecurityException("unissued capability");
            }
            if (grant.revision != revision) {
                byId.remove(id);
                throw new SecurityException("stale capability");
            }
            if (!costsPaid) {
                throw new SecurityException("costs not paid");
            }
            int index = randomIndex.applyAsInt(grant.cards.size());
            if (index < 0 || index >= grant.cards.size()) {
                throw new SecurityException("invalid referee random index");
            }
            byId.remove(id);
            return grant.cards.get(index);
        }
        void revokeAll() { grants.clear(); }
    }

    private List<Card> hiddenExile(Game game, Player actor, String... names) {
        Player opponent = game.getPlayers().get(0);
        List<Card> cards = new ArrayList<>();
        for (String name : names) {
            Card card = addCardToZone(name, opponent, ZoneType.Exile);
            AssertJUnit.assertTrue("face-down exile fixture", card.turnFaceDown(true));
            AssertJUnit.assertFalse("actor cannot inspect hidden original",
                    card.getView().canBeShownTo(actor.getView()));
            cards.add(card);
        }
        return cards;
    }

    private Map<String, String> decisionFor(String one, String two) {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        return new RefereeBridge().issue(actor, "pile-a", 17, 2,
                hiddenExile(game, actor, one, two), true, false);
    }

    @Test(timeOut = 120000)
    public void forgePlayPermissionDoesNotExposeHiddenCardIdentity() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card secret = hiddenExile(game, actor, "Shock").get(0);
        Card host = addCard("Runeclaw Bear", actor);
        StaticAbility ability = new StaticAbility("Mode$ Continuous | Affected$ Card.Self | Description$ Stage9D fixture", host, host.getCurrentState());
        // A fixture-issued play permission; no real spell authorization implied.
        secret.setMayPlay(actor, false, null, false, true, ability);
        AssertJUnit.assertFalse(secret.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse(secret.mayPlay(actor).isEmpty());
        boolean found = false;
        for (Card candidate : opponent.getZone(ZoneType.Exile).getCardsPlayerCanActivate(actor)) {
            if (candidate == secret) { found = true; }
        }
        AssertJUnit.assertTrue("Forge can hold a hidden card's play permission", found);
    }

    @Test(timeOut = 120000)
    public void publicActionDoesNotDependOnConcealedIdentity() {
        Map<String, String> a = decisionFor("Shock", "Giant Growth");
        Map<String, String> b = decisionFor("Lightning Bolt", "Runeclaw Bear");
        AssertJUnit.assertEquals("identical actor-visible actions", a, b);
    }

    @Test(timeOut = 120000)
    public void onlyFivePublicFieldsAreExposed() {
        Map<String, String> choice = decisionFor("Shock", "Giant Growth");
        Set<String> allowed = new HashSet<>(Arrays.asList(
                "id", "kind", "public_cost", "public_target", "public_effect"));
        AssertJUnit.assertEquals(allowed, choice.keySet());
        AssertJUnit.assertFalse(choice.toString().contains("Shock"));
        AssertJUnit.assertFalse(choice.toString().contains("Giant Growth"));
    }

    @Test(timeOut = 120000)
    public void refereeRandomizesOnlyAfterCosts() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        List<Card> cards = hiddenExile(game, actor, "Shock", "Giant Growth");
        RefereeBridge bridge = new RefereeBridge();
        String id = bridge.issue(actor, "pile-a", 17, 2, cards, true, false).get("id");
        AtomicInteger attempts = new AtomicInteger();
        IntUnaryOperator last = size -> { attempts.incrementAndGet(); return size - 1; };
        try {
            bridge.redeem(actor, id, 17, false, last);
            AssertJUnit.fail("must reject selection before costs paid");
        } catch (SecurityException expected) { }
        AssertJUnit.assertEquals(0, attempts.get());
        AssertJUnit.assertSame("only referee receives private Card", cards.get(1),
                bridge.redeem(actor, id, 17, true, last));
        AssertJUnit.assertEquals(1, attempts.get());
    }

    @Test(timeOut = 120000)
    public void actorRevisionAndSingleUseAreEnforced() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player other = game.getPlayers().get(0);
        List<Card> cards = hiddenExile(game, actor, "Shock");
        RefereeBridge bridge = new RefereeBridge();
        String id = bridge.issue(actor, "pile-a", 17, 1, cards, true, false).get("id");
        try {
            bridge.redeem(other, id, 17, true, size -> 0);
            AssertJUnit.fail("other actor cannot redeem");
        } catch (SecurityException expected) { }
        try {
            bridge.redeem(actor, id, 18, true, size -> 0);
            AssertJUnit.fail("stale revision cannot redeem");
        } catch (SecurityException expected) { }
        try {
            bridge.redeem(actor, id, 17, true, size -> 0);
            AssertJUnit.fail("stale capability cannot be replayed");
        } catch (SecurityException expected) { }
        String newer = bridge.issue(actor, "pile-a", 18, 1, cards, true, false).get("id");
        bridge.redeem(actor, newer, 18, true, size -> 0);
        try {
            bridge.redeem(actor, newer, 18, true, size -> 0);
            AssertJUnit.fail("single-use action cannot be replayed");
        } catch (SecurityException expected) { }
    }

    @Test(timeOut = 120000)
    public void permissionRestrictionAndVisibilityFailClosed() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        List<Card> cards = hiddenExile(game, actor, "Shock");
        RefereeBridge bridge = new RefereeBridge();
        try {
            bridge.issue(actor, "pile-a", 17, 1, cards, false, false);
            AssertJUnit.fail("referee authorization required");
        } catch (SecurityException expected) { }
        try {
            bridge.issue(actor, "pile-a", 17, 1, cards, true, true);
            AssertJUnit.fail("quality-restricted casting is not a blind choice");
        } catch (SecurityException expected) { }
        cards.get(0).addMayLookFaceDownExile(actor);
        AssertJUnit.assertTrue(cards.get(0).getView().canBeShownTo(actor.getView()));
        try {
            bridge.issue(actor, "pile-a", 17, 1, cards, true, false);
            AssertJUnit.fail("lookable cards require another action path");
        } catch (SecurityException expected) { }
    }
}
