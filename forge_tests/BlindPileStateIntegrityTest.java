package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
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
 * Stage 9E: real Forge Game/Card/PlayerZone-backed state-integrity canary.
 *
 * Security regression addressed: the Stage9D TEST-ONLY grant accepts a
 * caller-supplied revision and does not recheck the actual pile at redemption.
 * Stage9E stores private authoritative state, captures Game.getTimestamp(),
 * and rechecks membership, visibility and face-down state before RNG.
 *
 * This is STILL A TEST-ONLY referee. Effect authorization and costsPaid are
 * trusted fixtures, NOT real Magic spell/cost legality or production AI wiring.
 * It does not play games, train policies or use evaluation seeds.
 */
public class BlindPileStateIntegrityTest extends AITest {
    private static final class Pile {
        final Player owner;
        final List<Card> members;
        Pile(Player owner, List<Card> cards) {
            this.owner = owner;
            this.members = Collections.unmodifiableList(new ArrayList<>(cards));
        }
    }

    private static final class Grant {
        final Player actor;
        final Pile pile;
        final long epoch;
        final long gameTimestamp;
        Grant(Player actor, Pile pile, long epoch, long gameTimestamp) {
            this.actor = actor;
            this.pile = pile;
            this.epoch = epoch;
            this.gameTimestamp = gameTimestamp;
        }
    }

    private static final class Referee {
        private final Game game;
        private final Map<String, Pile> piles = new HashMap<>();
        private final Map<String, Grant> grants = new HashMap<>();
        private long epoch = 0;
        private long publicIssueSequence = 0;

        Referee(Game game) { this.game = game; }

        /** Trusted fixture, not policy input. Real integration needs effect provenance. */
        void registerPile(String publicId, Player owner, List<Card> cards) {
            if (publicId == null || !publicId.matches("[A-Za-z0-9_-]{1,48}")
                    || owner == null || owner.getGame() != game
                    || cards == null || cards.isEmpty() || piles.containsKey(publicId)) {
                throw new IllegalArgumentException("invalid or duplicate public pile");
            }
            if (new HashSet<>(cards).size() != cards.size()) {
                throw new SecurityException("duplicate pile member");
            }
            for (Card card : cards) {
                if (card == null || card.getGame() != game
                        || !card.isInZone(ZoneType.Exile) || !card.isFaceDown()
                        || !owner.getZone(ZoneType.Exile).getCards().contains(card)) {
                    throw new SecurityException("not a genuine face-down Forge exile member");
                }
            }
            piles.put(publicId, new Pile(owner, cards));
        }

        private boolean valid(Pile pile, Player actor) {
            if (pile == null || actor == null || actor.getGame() != game
                    || pile.members.isEmpty()) {
                return false;
            }
            Set<Card> seen = new HashSet<>();
            for (Card card : pile.members) {
                if (card == null || !seen.add(card) || !card.isInZone(ZoneType.Exile)
                        || !pile.owner.getZone(ZoneType.Exile).getCards().contains(card)
                        || !card.isFaceDown()
                        || card.getView().canBeShownTo(actor.getView())) {
                    return false;
                }
            }
            return true;
        }

        Map<String, String> issue(Player actor, String pileId,
                boolean refereeAuthorized, boolean qualityRestricted) {
            Pile pile = piles.get(pileId);
            if (!refereeAuthorized || qualityRestricted || !valid(pile, actor)) {
                throw new SecurityException("no lawful blind pile choice grant");
            }
            // Monotone public issuance sequence prevents reuse of an old action ID.
            // Never incorporate Card IDs, names, mana values or concealed types.
            String id = "blind-pile:" + pileId + ":epoch:" + epoch
                    + ":action:" + (++publicIssueSequence);
            grants.put(id, new Grant(actor, pile, epoch, game.getTimestamp()));
            Map<String, String> visible = new LinkedHashMap<>();
            visible.put("id", id);
            visible.put("kind", "choose-blind-exile-pile");
            visible.put("public_cost", "");
            visible.put("public_target", "face-down exile pile " + pileId
                    + " (" + pile.members.size() + " cards)");
            visible.put("public_effect", "Referee selects randomly after required costs");
            return Collections.unmodifiableMap(visible);
        }

        /** Private result never goes into the actor's policy observation. */
        Card redeem(Player actor, String id, boolean costsPaid, IntUnaryOperator randomIndex) {
            Grant grant = grants.get(id);
            if (grant == null || grant.actor != actor) {
                throw new SecurityException("unissued or wrong-actor grant");
            }
            if (!costsPaid) {
                // Cost failures must not trigger a random draw or reveal.
                throw new SecurityException("required costs not settled");
            }
            if (grant.epoch != epoch || grant.gameTimestamp != game.getTimestamp()
                    || !piles.containsValue(grant.pile)
                    || !valid(grant.pile, actor)) {
                grants.remove(id);
                throw new SecurityException("stale or mutated hidden pile");
            }
            grants.remove(id); // single-use even if referee RNG fails
            int index = randomIndex.applyAsInt(grant.pile.members.size());
            if (index < 0 || index >= grant.pile.members.size()) {
                throw new SecurityException("invalid referee random index");
            }
            return grant.pile.members.get(index);
        }

        /** Fixture rollback marker: does NOT undo actual Forge mana/cost transactions. */
        void invalidateOnRollback() {
            epoch++;
            grants.clear();
        }

        /** Deliberately bypasses epoch, testing independent private pile recheck. */
        void replacePileForTest(String id, Player owner, List<Card> replacement) {
            piles.put(id, new Pile(owner, replacement));
        }
    }

    private List<Card> hiddenExile(Game game, Player actor, String... names) {
        Player opponent = game.getPlayers().get(0);
        List<Card> cards = new ArrayList<>();
        for (String name : names) {
            Card card = addCardToZone(name, opponent, ZoneType.Exile);
            AssertJUnit.assertTrue("real Forge card must become face down",
                    card.turnFaceDown(true));
            AssertJUnit.assertFalse("original concealed from actor",
                    card.getView().canBeShownTo(actor.getView()));
            cards.add(card);
        }
        return cards;
    }

    private Map<String, String> publicChoice(String first, String second) {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player owner = game.getPlayers().get(0);
        Referee referee = new Referee(game);
        referee.registerPile("event-1", owner, hiddenExile(game, actor, first, second));
        return referee.issue(actor, "event-1", true, false);
    }

    @Test(timeOut = 120000)
    public void publicChoiceIgnoresConcealedIdentities() {
        Map<String, String> a = publicChoice("Shock", "Giant Growth");
        Map<String, String> b = publicChoice("Lightning Bolt", "Runeclaw Bear");
        AssertJUnit.assertEquals(a, b);
        AssertJUnit.assertEquals(new HashSet<>(Arrays.asList(
                "id", "kind", "public_cost", "public_target", "public_effect")),
                a.keySet());
        for (String secret : Arrays.asList("Shock", "Giant Growth",
                "Lightning Bolt", "Runeclaw Bear")) {
            AssertJUnit.assertFalse(a.toString().contains(secret));
        }
    }

    @Test(timeOut = 120000)
    public void unauthorizedOrQualityRestrictedGrantIsDenied() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Referee referee = new Referee(game);
        referee.registerPile("event-1", game.getPlayers().get(0),
                hiddenExile(game, actor, "Shock"));
        try {
            referee.issue(actor, "event-1", false, false);
            AssertJUnit.fail("authorization cannot be invented by policy");
        } catch (SecurityException expected) { }
        try {
            referee.issue(actor, "event-1", true, true);
            AssertJUnit.fail("406.3b qualified blind casting requires a different path");
        } catch (SecurityException expected) { }
        try {
            referee.issue(actor, "unregistered", true, false);
            AssertJUnit.fail("must not choose an arbitrary unregistered pile");
        } catch (SecurityException expected) { }
    }

    @Test(timeOut = 120000)
    public void removedExileMemberInvalidatesGrantBeforeRandomness() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player owner = game.getPlayers().get(0);
        List<Card> cards = hiddenExile(game, actor, "Shock", "Giant Growth");
        Referee referee = new Referee(game);
        referee.registerPile("event-1", owner, cards);
        String id = referee.issue(actor, "event-1", true, false).get("id");
        owner.getZone(ZoneType.Exile).remove(cards.get(0));
        AssertJUnit.assertFalse(owner.getZone(ZoneType.Exile).getCards().contains(cards.get(0)));
        AtomicInteger draws = new AtomicInteger();
        try {
            referee.redeem(actor, id, true, size -> { draws.incrementAndGet(); return 0; });
            AssertJUnit.fail("cannot redeem a removed card");
        } catch (SecurityException expected) { }
        AssertJUnit.assertEquals(0, draws.get());
    }

    @Test(timeOut = 120000)
    public void turnedFaceUpMemberInvalidatesGrantBeforeRandomness() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        List<Card> cards = hiddenExile(game, actor, "Shock");
        Referee referee = new Referee(game);
        referee.registerPile("event-1", game.getPlayers().get(0), cards);
        String id = referee.issue(actor, "event-1", true, false).get("id");
        cards.get(0).forceTurnFaceUp();
        AssertJUnit.assertFalse(cards.get(0).isFaceDown());
        AtomicInteger draws = new AtomicInteger();
        try {
            referee.redeem(actor, id, true, size -> { draws.incrementAndGet(); return 0; });
            AssertJUnit.fail("cannot use former face-down pile");
        } catch (SecurityException expected) { }
        AssertJUnit.assertEquals(0, draws.get());
    }

    @Test(timeOut = 120000)
    public void newLookPermissionInvalidatesBlindChoiceWithoutTimestampChange() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        List<Card> cards = hiddenExile(game, actor, "Shock");
        Referee referee = new Referee(game);
        referee.registerPile("event-1", game.getPlayers().get(0), cards);
        String id = referee.issue(actor, "event-1", true, false).get("id");
        long timestamp = game.getTimestamp();
        cards.get(0).addMayLookFaceDownExile(actor);
        AssertJUnit.assertEquals("look grant may not bump Game timestamp",
                timestamp, game.getTimestamp());
        AssertJUnit.assertTrue(cards.get(0).getView().canBeShownTo(actor.getView()));
        AtomicInteger draws = new AtomicInteger();
        try {
            referee.redeem(actor, id, true, size -> { draws.incrementAndGet(); return 0; });
            AssertJUnit.fail("blind grant must fail when card becomes lookable");
        } catch (SecurityException expected) { }
        AssertJUnit.assertEquals(0, draws.get());
    }

    @Test(timeOut = 120000)
    public void privatePileReplacementInvalidatesGrantWithoutEpochChange() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player owner = game.getPlayers().get(0);
        List<Card> original = hiddenExile(game, actor, "Shock");
        List<Card> replacement = hiddenExile(game, actor, "Giant Growth");
        Referee referee = new Referee(game);
        referee.registerPile("event-1", owner, original);
        String id = referee.issue(actor, "event-1", true, false).get("id");
        long timestamp = game.getTimestamp();
        referee.replacePileForTest("event-1", owner, replacement);
        AssertJUnit.assertEquals(timestamp, game.getTimestamp());
        AtomicInteger draws = new AtomicInteger();
        try {
            referee.redeem(actor, id, true, size -> { draws.incrementAndGet(); return 0; });
            AssertJUnit.fail("old grant must not silently follow a new pile");
        } catch (SecurityException expected) { }
        AssertJUnit.assertEquals(0, draws.get());
    }

    @Test(timeOut = 120000)
    public void costsAndRollbackGateRefereeRandomness() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        List<Card> cards = hiddenExile(game, actor, "Shock", "Giant Growth");
        Referee referee = new Referee(game);
        referee.registerPile("event-1", game.getPlayers().get(0), cards);
        String id = referee.issue(actor, "event-1", true, false).get("id");
        AtomicInteger draws = new AtomicInteger();
        IntUnaryOperator rng = size -> { draws.incrementAndGet(); return size - 1; };
        try {
            referee.redeem(actor, id, false, rng);
            AssertJUnit.fail("cannot choose before costs");
        } catch (SecurityException expected) { }
        AssertJUnit.assertEquals(0, draws.get());
        AssertJUnit.assertSame(cards.get(1), referee.redeem(actor, id, true, rng));
        AssertJUnit.assertEquals(1, draws.get());
        String second = referee.issue(actor, "event-1", true, false).get("id");
        referee.invalidateOnRollback();
        try {
            referee.redeem(actor, second, true, rng);
            AssertJUnit.fail("rolled-back grant must be revoked");
        } catch (SecurityException expected) { }
        AssertJUnit.assertEquals(1, draws.get());
    }

    @Test(timeOut = 120000)
    public void actorBindingAndSingleUsePreventReplays() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player other = game.getPlayers().get(0);
        List<Card> cards = hiddenExile(game, actor, "Shock");
        Referee referee = new Referee(game);
        referee.registerPile("event-1", other, cards);
        String id = referee.issue(actor, "event-1", true, false).get("id");
        try {
            referee.redeem(other, id, true, size -> 0);
            AssertJUnit.fail("wrong actor");
        } catch (SecurityException expected) { }
        AssertJUnit.assertSame(cards.get(0), referee.redeem(actor, id, true, size -> 0));
        try {
            referee.redeem(actor, id, true, size -> 0);
            AssertJUnit.fail("single use");
        } catch (SecurityException expected) { }
        String newer = referee.issue(actor, "event-1", true, false).get("id");
        AssertJUnit.assertFalse("reissued grant needs a fresh public nonce", id.equals(newer));
        AssertJUnit.assertSame(cards.get(0), referee.redeem(actor, newer, true, size -> 0));
    }

    @Test(timeOut = 120000)
    public void independentPublicPileGroupsNeverCrossSelect() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player owner = game.getPlayers().get(0);
        List<Card> one = hiddenExile(game, actor, "Shock");
        List<Card> two = hiddenExile(game, actor, "Giant Growth");
        Referee referee = new Referee(game);
        referee.registerPile("event-1", owner, one);
        referee.registerPile("event-2", owner, two);
        String id1 = referee.issue(actor, "event-1", true, false).get("id");
        String id2 = referee.issue(actor, "event-2", true, false).get("id");
        AssertJUnit.assertSame(one.get(0), referee.redeem(actor, id1, true, size -> 0));
        AssertJUnit.assertSame(two.get(0), referee.redeem(actor, id2, true, size -> 0));
    }
}
