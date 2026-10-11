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
 * Stage 9B: independent, real-Forge candidate/priority checks for an
 * opponent's face-down battlefield shell. This does NOT prove that every
 * downstream heuristic avoids inspecting the secret original Card state.
 *
 * Run alongside (not instead of) the six frozen Stage 9A regressions.
 * No gameplay matches or reserved evaluation seeds are used.
 */
public class FaceDownBattlefieldPrivacyTest extends AITest {
    private static final class Observation {
        final String visible;
        final String decision;

        Observation(String visible, String decision) {
            this.visible = visible;
            this.decision = decision;
        }
    }

    private static String publicBattlefield(Player owner, Player viewer) {
        List<String> descriptors = new ArrayList<>();
        for (Card c : owner.getCardsIn(ZoneType.Battlefield)) {
            AssertJUnit.assertTrue("A battlefield shell is public",
                    c.getView().canBeShownTo(viewer.getView()));
            if (c.getView().isFaceDown()
                    && !c.getView().canFaceDownBeShownTo(viewer.getView())) {
                descriptors.add("<face-down>");
            } else {
                descriptors.add(c.getName());
            }
        }
        Collections.sort(descriptors);
        return descriptors.toString();
    }

    private static String publicChoice(List<SpellAbility> choices) {
        AssertJUnit.assertNotNull("Forge must return a concrete decision", choices);
        AssertJUnit.assertFalse("Cannot assert privacy from a vacuous pass", choices.isEmpty());
        List<String> descriptors = new ArrayList<>();
        for (SpellAbility sa : choices) {
            Card host = sa.getHostCard();
            AssertJUnit.assertNotNull(host);
            String hostName = host.getName();
            if (host.getView().isFaceDown()
                    && !host.getView().canFaceDownBeShownTo(sa.getActivatingPlayer().getView())) {
                hostName = "<face-down>";
            }
            Card target = sa.getTargetCard();
            String targetName = "none";
            if (target != null) {
                targetName = target.getName();
                if (target.getView().isFaceDown()
                        && !target.getView().canFaceDownBeShownTo(sa.getActivatingPlayer().getView())) {
                    targetName = "<face-down>";
                }
            }
            descriptors.add(hostName + "|api=" + sa.getApi() + "|target=" + targetName);
        }
        return descriptors.toString();
    }

    private Observation runWithConcealedCreature(String actualName) {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        actor.setTeam(0);
        opponent.setTeam(1);

        addCard("Mountain", actor);
        addCardToZone("Lightning Bolt", actor, ZoneType.Hand);
        addCardToZone("Shock", actor, ZoneType.Hand);
        Card concealed = addCard(actualName, opponent);
        AssertJUnit.assertTrue("Test fixture must be face down",
                concealed.turnFaceDown(true));
        AssertJUnit.assertFalse("Opponent may not inspect the concealed face",
                concealed.getView().canFaceDownBeShownTo(actor.getView()));
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(2, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, actor);
        game.getAction().checkStateEffects(true);

        String visible = "phase=" + game.getPhaseHandler().getPhase()
                + "|life=" + actor.getLife() + ":" + opponent.getLife()
                + "|ownHand=Lightning Bolt,Shock"
                + "|opponentBattlefield=" + publicBattlefield(opponent, actor);
        Random saved = MyRandom.getRandom();
        try {
            // A local unit-test fixture RNG, not a gameplay-evaluation seed.
            MyRandom.setRandom(new Random(0));
            List<SpellAbility> choices = actor.getController().chooseSpellAbilityToPlay();
            return new Observation(visible, publicChoice(choices));
        } finally {
            MyRandom.setRandom(saved);
        }
    }

    /**
     * Change ONLY the real concealed card identity. Its public face-down
     * shell, life totals, hand, turn and actor RNG remain identical.
     * Uses existing Forge-listed cards, not synthetic mock Card objects.
     */
    @Test(timeOut = 120000)
    public void opponentFaceDownBattlefieldIdentityDoesNotChangeLiveDecision() {
        Observation one = runWithConcealedCreature("Llanowar Elves");
        Observation two = runWithConcealedCreature("Giant Growth");
        AssertJUnit.assertEquals("Public observations must be identical",
                one.visible, two.visible);
        AssertJUnit.assertEquals("Hidden original identity cannot affect decision",
                one.decision, two.decision);
    }

    /**
     * A targetable public face-down shell must not grant the opponent an
     * actionable turn-face-up ability derived from its secret original.
     */
    @Test(timeOut = 120000)
    public void opponentFaceDownBattlefieldDoesNotOfferPrivateTurnUpAbility() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card concealed = addCard("Willbender", opponent);
        AssertJUnit.assertTrue("Fixture must be face down", concealed.turnFaceDown(true));
        AssertJUnit.assertFalse("Cannot inspect opponent's original face",
                concealed.getView().canFaceDownBeShownTo(actor.getView()));

        CardCollection candidates = ComputerUtilAbility.getAvailableCards(game, actor);
        AssertJUnit.assertTrue("Public face-down shell must remain targetable",
                candidates.contains(concealed));
        for (SpellAbility candidate : ComputerUtilAbility.getSpellAbilities(candidates, actor)) {
            AssertJUnit.assertFalse("Opponent cannot turn up concealed card via our policy",
                    candidate.getHostCard() == concealed && candidate.isTurnFaceUp());
        }
    }
}
