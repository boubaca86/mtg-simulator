package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Stage 9C real-Forge visibility-permission probes, NOT a Forge blind-play
 * adapter and NOT proof that a candidate is a legal cast.
 *
 * These tests run on a disposable Forge 2.0.15 checkout with the proposed
 * Stage 9A privacy patch applied. They specifically test the separation of:
 *   (a) knowing a concealed card's face, (b) play permission, and
 *   (c) being passed as an AI identity-bearing candidate.
 *
 * Rule 406.3 preserves acquired look permission while a card stays exiled.
 * Rule 406.4 blind-pile selection is NOT implemented by this test fixture.
 * No gameplay matches, training, or reserved evaluation seeds are used.
 */
public class FaceDownExilePermissionBoundaryTest extends AITest {
    private Card putFaceDown(String name, Player owner) {
        Card card = addCardToZone(name, owner, ZoneType.Exile);
        AssertJUnit.assertTrue("Fixture must turn face down", card.turnFaceDown(true));
        AssertJUnit.assertTrue(card.isFaceDown());
        return card;
    }

    private static boolean candidate(Game game, Player actor, Card card) {
        CardCollection cards = ComputerUtilAbility.getAvailableCards(game, actor);
        return cards.contains(card);
    }

    @Test(timeOut = 120000)
    public void opponentConcealedExileWithoutLookIsNotCandidate() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card secret = putFaceDown("Counterspell", opponent);
        AssertJUnit.assertFalse(secret.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse(secret.mayPlayerLook(actor));
        AssertJUnit.assertFalse("Private original must not enter AI candidate list",
                candidate(game, actor, secret));
    }

    @Test(timeOut = 120000)
    public void explicitLookPermissionMakesConcealedExileCandidate() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card card = putFaceDown("Counterspell", opponent);
        AssertJUnit.assertFalse(candidate(game, actor, card));
        card.addMayLookFaceDownExile(actor);
        AssertJUnit.assertTrue("Referee grants actual viewing permission",
                card.mayPlayerLook(actor));
        AssertJUnit.assertTrue(card.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertTrue("Visibility patch must not suppress authorized inspection",
                candidate(game, actor, card));
    }

    @Test(timeOut = 120000)
    public void exileLookPermissionPersistsAfterTemporaryGrantExpires() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card card = putFaceDown("Giant Growth", opponent);
        card.addMayLookTemp(actor);
        AssertJUnit.assertTrue(card.getView().canBeShownTo(actor.getView()));
        card.removeMayLookTemp(actor);
        AssertJUnit.assertTrue("Rule 406.3: may keep looking while still exiled",
                card.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertTrue(candidate(game, actor, card));
    }

    @Test(timeOut = 120000)
    public void faceUpOpponentExileRemainsPublicCandidate() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card card = addCardToZone("Shock", opponent, ZoneType.Exile);
        AssertJUnit.assertFalse(card.isFaceDown());
        AssertJUnit.assertTrue(card.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertTrue("Public exile remains observable",
                candidate(game, actor, card));
    }

    @Test(timeOut = 120000)
    public void ownFaceDownExileNotVisibleSolelyBecauseOfOwnership() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Card card = putFaceDown("Counterspell", actor);
        AssertJUnit.assertFalse("Ownership is not itself look permission",
                card.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertFalse(candidate(game, actor, card));
        card.addMayLookFaceDownExile(actor);
        AssertJUnit.assertTrue(card.getView().canBeShownTo(actor.getView()));
        AssertJUnit.assertTrue(candidate(game, actor, card));
    }

    @Test(timeOut = 120000)
    public void lookPermissionDoesNotCreatePlayPermission() {
        Game game = initAndCreateGame();
        Player actor = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card card = putFaceDown("Counterspell", opponent);
        AssertJUnit.assertTrue("Fixture must have no play grant",
                card.mayPlay(actor).isEmpty());
        card.addMayLookFaceDownExile(actor);
        AssertJUnit.assertTrue(card.mayPlayerLook(actor));
        AssertJUnit.assertTrue("Known candidate is not necessarily legal to cast",
                candidate(game, actor, card));
        AssertJUnit.assertTrue("Viewing a card must not itself grant play permission",
                card.mayPlay(actor).isEmpty());
    }
}
