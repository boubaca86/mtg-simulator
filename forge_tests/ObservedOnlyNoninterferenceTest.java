package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Stage 9A: actual Forge Game + stock heuristic decision-path metamorphic tests.
 *
 * These are isolated priority decisions in legal-looking constructed game states,
 * NOT complete matches, win-rate evaluations, learned-policy tests, or proof of
 * global noninterference. No reserved gameplay seed is used.
 */
public class ObservedOnlyNoninterferenceTest extends AITest {
    private static final class Trace {
        final String actorObservation;
        final String selectedAction;

        Trace(String actorObservation, String selectedAction) {
            this.actorObservation = actorObservation;
            this.selectedAction = selectedAction;
        }
    }

    private static String visibleNames(Player owner, ZoneType zone, Player viewer) {
        List<String> names = new ArrayList<>();
        for (Card card : owner.getCardsIn(zone)) {
            AssertJUnit.assertTrue("Only visible zones may enter public trace",
                    card.getView().canBeShownTo(viewer.getView()));
            // A face-down battlefield card has a public shell, not a public identity.
            // CardView.canBeShownTo() alone is NOT an identity permission.
            if (card.getView().isFaceDown()
                    && !card.getView().canFaceDownBeShownTo(viewer.getView())) {
                names.add("<face-down>");
            } else {
                names.add(card.getName());
            }
        }
        Collections.sort(names);
        return names.toString();
    }

    private static String actorObservation(Game game, Player actor, Player opponent) {
        // No hidden identities, order, card IDs, or all-card database data.
        // This is a scenario-specific public observation, not a complete MTG view.
        return "phase=" + game.getPhaseHandler().getPhase()
                + "|actorLife=" + actor.getLife()
                + "|opponentLife=" + opponent.getLife()
                + "|ownHand=" + visibleNames(actor, ZoneType.Hand, actor)
                + "|ownBattlefield=" + visibleNames(actor, ZoneType.Battlefield, actor)
                + "|opponentBattlefield=" + visibleNames(opponent, ZoneType.Battlefield, actor)
                + "|ownLibraryCount=" + actor.getCardsIn(ZoneType.Library).size()
                + "|opponentLibraryCount=" + opponent.getCardsIn(ZoneType.Library).size()
                + "|opponentHandCount=" + opponent.getCardsIn(ZoneType.Hand).size()
                + "|opponentExileCount=" + opponent.getCardsIn(ZoneType.Exile).size();
    }

    private static String selectedAction(List<SpellAbility> choices) {
        AssertJUnit.assertNotNull("Forge must make a real decision", choices);
        AssertJUnit.assertFalse("A nonempty action is required; pass-only is vacuous",
                choices.isEmpty());
        List<String> actions = new ArrayList<>();
        for (SpellAbility ability : choices) {
            AssertJUnit.assertNotNull(ability);
            AssertJUnit.assertNotNull(ability.getHostCard());
            Player targetedPlayer = ability.getTargets().getFirstTargetedPlayer();
            Card targetedCard = ability.getTargetCard();
            actions.add(ability.getHostCard().getName()
                    + "|api=" + ability.getApi()
                    + "|targetPlayer=" + (targetedPlayer == null ? "none" : targetedPlayer.getName())
                    + "|targetCard=" + (targetedCard == null ? "none" : targetedCard.getName()));
        }
        return actions.toString();
    }

    private Trace takeDecision(boolean alternateSecrets, boolean opponentFaceDownExile) {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        actor.setTeam(0);
        opponent.setTeam(1);

        // Both runs have identical visible cards and own deck composition.
        addCard("Mountain", actor);
        addCardToZone("Lightning Bolt", actor, ZoneType.Hand);
        addCardToZone("Shock", actor, ZoneType.Hand);
        if (alternateSecrets) {
            addCardToZone("Forest", actor, ZoneType.Library);
            addCardToZone("Island", actor, ZoneType.Library);
        } else {
            addCardToZone("Island", actor, ZoneType.Library);
            addCardToZone("Forest", actor, ZoneType.Library);
        }

        // Hidden enemy hand and hidden enemy library identities are different,
        // while their counts and every actor-visible public fact are unchanged.
        Card hiddenHand = addCardToZone(
                alternateSecrets ? "Giant Growth" : "Counterspell",
                opponent, ZoneType.Hand);
        addCardToZone(alternateSecrets ? "Swamp" : "Island",
                opponent, ZoneType.Library);
        addCardToZone(alternateSecrets ? "Forest" : "Mountain",
                opponent, ZoneType.Library);
        AssertJUnit.assertFalse("Opponent hand is private",
                hiddenHand.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse("Own library order is private",
                actor.getCardsIn(ZoneType.Library).get(0).getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse("Opponent library is private",
                opponent.getCardsIn(ZoneType.Library).get(0).getView().canBeShownTo(actor.getView()));

        if (opponentFaceDownExile) {
            Card hiddenExile = addCardToZone(
                    alternateSecrets ? "Giant Growth" : "Counterspell",
                    opponent, ZoneType.Exile);
            AssertJUnit.assertTrue("Exiled card must actually be face down",
                    hiddenExile.turnFaceDown(true));
            AssertJUnit.assertFalse("Face-down opponent exile is private",
                    hiddenExile.getView().canBeShownTo(actor.getView()));
        }

        addCard("Runeclaw Bear", opponent);
        opponent.setLife(2, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, actor);
        game.getAction().checkStateEffects(true);

        String observation = actorObservation(game, actor, opponent);
        // Test-fixture RNG reset only; 0 is NOT a gameplay evaluation seed.
        Random previous = MyRandom.getRandom();
        try {
            MyRandom.setRandom(new Random(0));
            List<SpellAbility> choices = actor.getController().chooseSpellAbilityToPlay();
            return new Trace(observation, selectedAction(choices));
        } finally {
            MyRandom.setRandom(previous);
        }
    }

    @Test(timeOut = 120000)
    public void opponentHiddenHandAndLibraryDoNotChangeLiveDecision() {
        Trace first = takeDecision(false, false);
        Trace second = takeDecision(true, false);
        AssertJUnit.assertEquals("Actor-visible observations must match",
                first.actorObservation, second.actorObservation);
        AssertJUnit.assertEquals("Same observation and RNG must yield same actual Forge decision",
                first.selectedAction, second.selectedAction);
    }

    @Test(timeOut = 120000)
    public void opponentFaceDownExileDoesNotChangeLiveDecision() {
        Trace first = takeDecision(false, true);
        Trace second = takeDecision(true, true);
        AssertJUnit.assertEquals("Actor-visible observations must match",
                first.actorObservation, second.actorObservation);
        AssertJUnit.assertEquals("Private face-down exile must not change Forge decision",
                first.selectedAction, second.selectedAction);
    }

    /**
     * Security-negative regression: stock Forge 2.0.15 currently returns the
     * opponent's unobserved library top from getAvailableCards(). This MUST
     * FAIL until the source is fixed; a red test is not a working firewall.
     *
     * No external permission, public reveal, or playable-from-library effect
     * is installed in this fixture.
     */
    @Test(timeOut = 120000)
    public void opponentUnseenTopLibraryNotEnumerated() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card secret = addCardToZone("Counterspell", opponent, ZoneType.Library);
        AssertJUnit.assertFalse("The card must really be private in this scenario",
                secret.getView().canBeShownTo(actor.getView()));
        CardCollection candidates = ComputerUtilAbility.getAvailableCards(game, actor);
        AssertJUnit.assertFalse("Cannot offer an opponent's private library-top card to the AI",
                candidates.contains(secret));
    }

    /**
     * No top-of-library look/play effect: the AI cannot consult the identity
     * or abilities of its own future draw merely because it owns the deck.
     */
    @Test(timeOut = 120000)
    public void ownUnseenTopLibraryNotEnumerated() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Card secret = addCardToZone("Lightning Bolt", actor, ZoneType.Library);
        AssertJUnit.assertFalse("Unobserved top of own library must remain private",
                secret.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse("Own decklist does not reveal draw order",
                ComputerUtilAbility.getAvailableCards(game, actor).contains(secret));
    }

    /**
     * Security-negative regression for face-down exile. This fixture grants
     * no look or play permission, so card identity cannot be a candidate.
     */
    @Test(timeOut = 120000)
    public void opponentUnseenFaceDownExileNotEnumerated() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card secret = addCardToZone("Giant Growth", opponent, ZoneType.Exile);
        AssertJUnit.assertTrue("Fixture requires a face-down exile card", secret.turnFaceDown(true));
        AssertJUnit.assertFalse("The actor must not be able to examine the secret",
                secret.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse("Unexaminable face-down exile card cannot reach ability enumeration",
                ComputerUtilAbility.getAvailableCards(game, actor).contains(secret));
    }

    /**
     * Sanity control: ordinary known cards remain available to the AI.
     */
    @Test(timeOut = 120000)
    public void publicBattlefieldAndOwnHandRemainCandidates() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card hand = addCardToZone("Shock", actor, ZoneType.Hand);
        Card battlefield = addCard("Runeclaw Bear", opponent);
        Card concealed = addCard("Llanowar Elves", opponent);
        AssertJUnit.assertTrue("Fixture must have a face-down battlefield permanent",
                concealed.turnFaceDown(true));
        AssertJUnit.assertTrue("Public face-down shell remains visible",
                concealed.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse("Public shell is not permission to examine hidden identity",
                concealed.getView().canFaceDownBeShownTo(actor.getView()));
        String visibleBattlefield = visibleNames(opponent, ZoneType.Battlefield, actor);
        AssertJUnit.assertTrue("Trace must represent the public face-down shell",
                visibleBattlefield.contains("<face-down>"));
        AssertJUnit.assertFalse("Trace must not contain concealed card's original identity",
                visibleBattlefield.contains("Llanowar Elves"));
        CardCollection candidates = ComputerUtilAbility.getAvailableCards(game, actor);
        AssertJUnit.assertTrue("Own hand stays available", candidates.contains(hand));
        AssertJUnit.assertTrue("Opponent public battlefield stays available", candidates.contains(battlefield));
        AssertJUnit.assertTrue("Face-down battlefield shell stays targetable",
                candidates.contains(concealed));
    }

}
