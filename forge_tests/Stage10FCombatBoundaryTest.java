package forge.ai;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardView;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reuse all 51 Stage 10E regressions with a restricted combat controller.
 * This deterministic test policy is not an expert strategy or general combat
 * implementation. Forge still performs simultaneous damage and state actions. */
public final class Stage10FCombatBoundaryTest extends Stage10EMenaceBlockingTest {
    private static final String DAMAGE = "stage10f: unsupported combat assignment";
    private static final String ORDER = "stage10f: legacy combat ordering unsupported";
    private static final String REVEAL = "stage10f: reveal protocol unsupported";
    private final List<CombatController> lastControllers = new ArrayList<>();
    private Game lastGame;

    private final class CombatController extends ExternalCreatureController {
        int damageAssignments;
        int multipleOpponents;

        CombatController(Game game, Player self, Player other, Actor worker) {
            super(game, self, other, worker);
        }

        private void requireVisibleBear(Card card, Player expectedController) {
            if (card == null || card.getGame() != getGame()
                    || !expectedController.getCardsIn(ZoneType.Battlefield).contains(card)
                    || card.getController() != expectedController || card.isFaceDown()
                    || !card.isCreature() || !"Grizzly Bears".equals(card.getName())) {
                throw new SecurityException(DAMAGE);
            }
            // Evasion changes legality before this callback, not damage
            // distribution. Other keywords need separately audited semantics.
            for (KeywordInterface keyword : card.getKeywords()) {
                if (keyword.getKeyword() != Keyword.FLYING
                        && keyword.getKeyword() != Keyword.REACH
                        && keyword.getKeyword() != Keyword.MENACE) {
                    throw new SecurityException(DAMAGE);
                }
            }
        }

        @Override
        public Map<Card,Integer> assignCombatDamage(Card source, CardCollectionView opposed,
                CardCollectionView remaining, int damage, GameEntity defender, boolean overrideOrder) {
            Combat combat = getGame().getCombat();
            requireVisibleBear(source, controlled);
            if (combat == null || getGame().getRules().hasOrderCombatants()
                    || getGame().getPhaseHandler().getPhase() != PhaseType.COMBAT_DAMAGE
                    || damage < 0 || damage != source.getNetCombatDamage()
                    || opposed == null || opposed.isEmpty() || opposed.size() > 8) {
                throw new SecurityException(DAMAGE);
            }
            CardCollection actual;
            if (combat.isAttacking(source)) {
                if (defender != opponent || combat.getDefenderByAttacker(source) != opponent) {
                    throw new SecurityException(DAMAGE);
                }
                actual = combat.getBlockers(source);
            } else if (combat.isBlocking(source) && defender == null) {
                actual = combat.getAttackersBlockedBy(source);
            } else {
                throw new SecurityException(DAMAGE);
            }
            List<Card> visible = new ArrayList<>();
            for (Card card : opposed) {
                requireVisibleBear(card, opponent);
                if (visible.contains(card)) throw new SecurityException(DAMAGE);
                visible.add(card);
            }
            if (!new HashSet<>(visible).equals(new HashSet<>(actual))) {
                throw new SecurityException(DAMAGE);
            }
            // Current CR 510.1c/d permits any distribution among blockers (or
            // attackers). Concentrating all damage is a policy, not a rule.
            // Tie-break only on the same public table order actors can observe.
            visible.sort(Comparator.comparingInt(card ->
                    opponent.getCardsIn(ZoneType.Battlefield).indexOf(card)));
            Map<Card,Integer> result = new LinkedHashMap<>();
            if (damage > 0) result.put(visible.get(0), damage);
            damageAssignments++;
            if (visible.size() > 1) multipleOpponents++;
            // Never read remaining, hidden zones, AiCardMemory or AI evaluators.
            return result;
        }

        // These callbacks implement pre-Foundations damage ordering. Modern
        // Forge combat skips them; unsupported legacy choices must not use AI.
        @Override
        public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
            throw new SecurityException(ORDER);
        }

        @Override
        public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
            throw new SecurityException(ORDER);
        }

        @Override
        public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
            throw new SecurityException(ORDER);
        }

        @Override
        public void reveal(CardCollectionView cards, ZoneType zone, Player owner,
                String messagePrefix, boolean addSuffix) {
            throw new SecurityException(REVEAL);
        }

        @Override
        public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner,
                String messagePrefix, boolean addSuffix) {
            throw new SecurityException(REVEAL);
        }
    }

    @Override
    protected CombatController attach(Game game, Player self, Player opponent, Actor actor) {
        if (lastGame != game) {
            lastControllers.clear();
            lastGame = game;
        }
        CombatController controller = new CombatController(game, self, opponent, actor);
        lastControllers.add(controller);
        self.dangerouslySetController(controller);
        return controller;
    }

    private static void denied(String message, Runnable action) {
        try {
            action.run();
        } catch (SecurityException expected) {
            Assert.assertEquals(expected.getMessage(), message);
            return;
        }
        Assert.fail("COMBAT_GUARD_MISSING: " + message);
    }

    /** Direct callback fixture. Native starts/damage remain covered by inherited
     * tests and the additional real menace instrumentation test below. */
    private Combat arrangeCombat(Game game, Player self, Player other, Card source,
            Card... blockers) {
        Combat combat = new Combat(self);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DAMAGE, self);
        // devModeSet ends the previous combat; attach this fixture afterwards.
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(source, other);
        for (Card blocker : blockers) combat.addBlocker(source, blocker);
        Assert.assertSame(game.getCombat(), combat);
        Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.COMBAT_DAMAGE);
        Assert.assertTrue(combat.isAttacking(source));
        Assert.assertEquals(combat.getBlockers(source).size(), blockers.length);
        return combat;
    }

    @Test(timeOut=120000)
    public void visibleCombatAssignmentConservesDamageOnBothSides() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        Card source = addCardToZone("Grizzly Bears", self, ZoneType.Battlefield);
        Card first = addCardToZone("Grizzly Bears", other, ZoneType.Battlefield);
        Card second = addCardToZone("Grizzly Bears", other, ZoneType.Battlefield);
        arrangeCombat(game, self, other, source, first, second);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            CombatController ca = attach(game, self, other, a);
            CombatController cb = attach(game, other, self, b);
            Map<Card,Integer> attack = ca.assignCombatDamage(source,
                    new CardCollection(List.of(second, first)), null, 2, other, true);
            Assert.assertEquals(attack, Collections.singletonMap(first, 2));
            Assert.assertEquals(cb.assignCombatDamage(first, new CardCollection(source),
                    null, 2, null, true), Collections.singletonMap(source, 2));
            Assert.assertEquals(self.getLife(), 20);
            Assert.assertEquals(other.getLife(), 20);
            Assert.assertEquals(first.getDamage(), 0, "assignment does not apply damage itself");
        }
    }

    @Test(timeOut=120000)
    public void damageChoiceIgnoresPrivateZonesAndCandidateEnumeration() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        Card source = addCardToZone("Grizzly Bears", self, ZoneType.Battlefield);
        Card first = addCardToZone("Grizzly Bears", other, ZoneType.Battlefield);
        Card second = addCardToZone("Grizzly Bears", other, ZoneType.Battlefield);
        arrangeCombat(game, self, other, source, first, second);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            CombatController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            String view = observation(self, other);
            var reply = a.ask(view);
            Map<Card,Integer> before = ca.assignCombatDamage(source,
                    new CardCollection(List.of(first, second)), null, 2, other, true);
            Card secret = addCardToZone("Counterspell", other, ZoneType.Hand);
            addCardToZone("Shock", other, ZoneType.Library);
            addCardToZone("Forest", self, ZoneType.Library);
            Map<Card,Integer> after = ca.assignCombatDamage(source,
                    new CardCollection(List.of(second, first)), new CardCollection(secret), 2, other, true);
            Assert.assertEquals(after, before, "candidate order and unused remaining list carry no strategy");
            Assert.assertEquals(observation(self, other), view);
            Assert.assertEquals(a.ask(observation(self, other)), reply);
            Assert.assertEquals(other.getLife(), 20);
        }
    }

    @Test(timeOut=120000)
    public void damageAssignmentRejectsHiddenForeignStaleAndWrongBudgetInputs() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        Card source = addCardToZone("Grizzly Bears", self, ZoneType.Battlefield);
        Card blocker = addCardToZone("Grizzly Bears", other, ZoneType.Battlefield);
        Card noncombat = addCardToZone("Grizzly Bears", other, ZoneType.Battlefield);
        Card secret = addCardToZone("Grizzly Bears", other, ZoneType.Hand);
        arrangeCombat(game, self, other, source, blocker);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            CombatController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            Assert.assertEquals(ca.assignCombatDamage(source, new CardCollection(blocker),
                    null, 2, other, true), Collections.singletonMap(blocker, 2),
                    "positive control: this fixture admits its genuine combat assignment");
            for (Card wrong : List.of(noncombat, secret, source)) {
                denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(wrong),
                        null, 2, other, true));
            }
            denied(DAMAGE, () -> ca.assignCombatDamage(blocker, new CardCollection(source),
                    null, 2, self, true));
            for (int amount : new int[] {-1, 0, 1, 3, Integer.MAX_VALUE}) {
                denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(blocker),
                        null, amount, other, true));
            }
            denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(blocker),
                    null, 2, null, true));
            denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(),
                    null, 2, other, true));
            game.getRules().setOrderCombatants(true);
            denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(blocker),
                    null, 2, other, false));
            game.getRules().setOrderCombatants(false);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, self, false);
            denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(blocker),
                    null, 2, other, true));
            game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DAMAGE, self, false);
            Assert.assertTrue(blocker.turnFaceDown(true));
            denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(blocker),
                    null, 2, other, true));
            Assert.assertEquals(ca.damageAssignments, 1, "only the positive control was accepted");
            Assert.assertEquals(source.getDamage(), 0);
            Assert.assertEquals(self.getLife(), 20);
            Assert.assertEquals(other.getLife(), 20);
        }
    }

    @Test(timeOut=120000)
    public void unauditedCombatKeywordsStopWithoutPrivilegedFallback() throws Exception {
        for (String keyword : List.of("Trample", "Deathtouch", "First strike", "Double strike", "Banding")) {
            Game game = initAndCreateGame();
            Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
            Card source = addCardToZone("Grizzly Bears", self, ZoneType.Battlefield);
            Card blocker = addCardToZone("Grizzly Bears", other, ZoneType.Battlefield);
            source.addChangedCardKeywords(Collections.singletonList(keyword),
                    Collections.emptyList(), false, game.getNextTimestamp(), null);
            Assert.assertTrue(source.hasKeyword(keyword));
            arrangeCombat(game, self, other, source, blocker);
            try (Actor a = new Actor(); Actor b = new Actor()) {
                CombatController ca = attach(game, self, other, a);
                attach(game, other, self, b);
                denied(DAMAGE, () -> ca.assignCombatDamage(source, new CardCollection(blocker),
                        null, 2, other, true));
                Assert.assertEquals(ca.damageAssignments, 0);
                Assert.assertEquals(other.getLife(), 20);
            }
        }
    }

    @Test(timeOut=120000)
    public void legacyOrderingAndRevealCallbacksCannotPopulatePrivilegedAiMemory() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        Card source = addCardToZone("Grizzly Bears", self, ZoneType.Battlefield);
        Card secret = addCardToZone("Counterspell", other, ZoneType.Hand);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            CombatController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            String before = observation(self, other);
            Assert.assertFalse(AiCardMemory.isRememberedCard(self, secret,
                    AiCardMemory.MemorySet.REVEALED_CARDS));
            denied(ORDER, () -> ca.orderBlockers(source, new CardCollection()));
            denied(ORDER, () -> ca.orderBlocker(source, secret, new CardCollection()));
            denied(ORDER, () -> ca.orderAttackers(source, new CardCollection()));
            denied(REVEAL, () -> ca.reveal(new CardCollection(secret), ZoneType.Hand, other, null, true));
            denied(REVEAL, () -> ca.reveal(Collections.singletonList(secret.getView()),
                    ZoneType.Hand, other.getView(), null, true));
            Assert.assertFalse(AiCardMemory.isRememberedCard(self, secret,
                    AiCardMemory.MemorySet.REVEALED_CARDS), "neither overload may record referee cards");
            Assert.assertEquals(observation(self, other), before);
            Assert.assertEquals(other.getCardsIn(ZoneType.Hand).size(), 1);
            Assert.assertEquals(self.getLife(), 20);
        }
    }

    @Test(timeOut=120000)
    public void realMenaceCombatUsesRestrictedDamageForAttackerAndBothBlockers() throws Exception {
        super.menaceTwoBlockersAreLegalAndPreventPlayerCombatDamage();
        Assert.assertEquals(lastControllers.size(), 2);
        Assert.assertTrue(lastControllers.stream().allMatch(c -> c.damageAssignments > 0),
                "both sides must actually enter the new damage callback");
        Assert.assertTrue(lastControllers.stream().anyMatch(c -> c.multipleOpponents > 0),
                "a genuine two-block assignment must use the restricted path");
    }
}
