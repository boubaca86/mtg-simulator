package forge.ai;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.effects.ChangeZoneEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Stage 9F — real Forge card-effect canary, not a production policy test.
 * Uses Wall of Mourning's SVar parsed by Forge AbilityFactory and the
 * genuine ChangeZoneEffect resolver. No caller-supplied RNG.
 * Does not prove end-step trigger eligibility, costs or global privacy.
 */
public class WallOfMourningForgeEffectTest extends AITest {
    private SpellAbility returnAbility(Card wall, Player actor) {
        String script = wall.getSVar("TrigReturn");
        AssertJUnit.assertNotNull("actual Wall of Mourning SVar must exist", script);
        SpellAbility ability = AbilityFactory.getAbility(script, wall);
        ability.setActivatingPlayer(actor);
        return ability;
    }

    @Test(timeOut = 120000)
    public void returnAbilityRequiresBlindRandomSelection() {
        Game game = initAndCreateThreePlayerGame();
        Player actor = game.getPlayers().get(1);
        AssertJUnit.assertEquals("real three-player fixture", 3, game.getPlayers().size());
        Card wall = addCard("Wall of Mourning", actor);
        SpellAbility ability = returnAbility(wall, actor);
        AssertJUnit.assertTrue("return effect must be Hidden", ability.isHidden());
        AssertJUnit.assertEquals("exile origin", "Exile", ability.getParam("Origin"));
        AssertJUnit.assertEquals("hand destination", "Hand", ability.getParam("Destination"));
        AssertJUnit.assertEquals("linked exile only", "Card.ExiledWithSource", ability.getParam("ChangeType"));
        AssertJUnit.assertEquals("one returned card", "1", ability.getParam("ChangeNum"));
        AssertJUnit.assertTrue("Wall of Mourning requires blind referee randomness; pinned baseline intentionally fails", ability.hasParam("AtRandom"));
    }

    @Test(timeOut = 120000)
    public void returnEffectMovesExactlyOneLinkedFaceDownExile() {
        Game game = initAndCreateThreePlayerGame();
        Player actor = game.getPlayers().get(1);
        Card wall = addCard("Wall of Mourning", actor);
        Card first = addCardToZone("Shock", actor, ZoneType.Exile);
        Card second = addCardToZone("Giant Growth", actor, ZoneType.Exile);
        Card unlinked = addCardToZone("Lightning Bolt", actor, ZoneType.Exile);
        for (Card card : new Card[]{first, second, unlinked}) {
            AssertJUnit.assertTrue("real Forge exile must be concealed", card.turnFaceDown(true));
        }
        for (Card linked : new Card[]{first, second}) {
            linked.setExiledWith(wall);
            wall.addExiledCard(linked);
        }
        AssertJUnit.assertEquals("two genuine linked exiles", 2, wall.getExiledCards().size());
        SpellAbility ability = returnAbility(wall, actor);
        AssertJUnit.assertTrue("requires blind referee path", ability.hasParam("AtRandom"));
        new ChangeZoneEffect().resolve(ability);
        int linkedInHand = 0;
        for (Card c : actor.getCardsIn(ZoneType.Hand)) {
            if ("Shock".equals(c.getName()) || "Giant Growth".equals(c.getName())) {
                linkedInHand++;
            }
            AssertJUnit.assertFalse("unlinked secret must never return", "Lightning Bolt".equals(c.getName()));
        }
        AssertJUnit.assertEquals("exactly one linked card moved into hand", 1, linkedInHand);
        AssertJUnit.assertTrue("unlinked card remains in exile", actor.getCardsIn(ZoneType.Exile).contains(unlinked));
    }
}
