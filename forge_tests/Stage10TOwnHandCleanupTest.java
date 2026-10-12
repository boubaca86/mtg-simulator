package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/** Own-hand preferences only. Forge owns cleanup timing, count and discards. */
public final class Stage10TOwnHandCleanupTest extends Stage10SNaturalTwentyLifeTest {
    private static final String DENIED="stage10t: invalid own-hand cleanup choice";

    private static Actor handActor() throws Exception {
        String script=System.getenv("STAGE10T_WORKER");
        Assert.assertNotNull(script);
        return new Actor(1,script,null);
    }

    private static void validateHandOrder(JsonObject reply,int size) {
        if(size<0 || size>128 || reply==null || reply.size()!=2
                || !reply.has("intent") || !reply.get("intent").isJsonPrimitive()
                || !reply.get("intent").getAsJsonPrimitive().isString()
                || !"hand_order".equals(reply.get("intent").getAsString())
                || !reply.has("hand_priority") || !reply.get("hand_priority").isJsonArray()) {
            throw new SecurityException(DENIED);
        }
        JsonArray order=reply.getAsJsonArray("hand_priority");
        if(order.size()!=size) throw new SecurityException(DENIED);
        boolean[] seen=new boolean[size];
        for(JsonElement value:order) {
            if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new SecurityException(DENIED);
            }
            String raw=value.getAsString();
            if(!raw.matches("0|[1-9][0-9]*") || raw.length()>3) {
                throw new SecurityException(DENIED);
            }
            int index=Integer.parseInt(raw);
            if(index>=size || seen[index]) throw new SecurityException(DENIED);
            seen[index]=true;
        }
    }

    private static JsonObject askHandOrder(Actor actor,String view) throws Exception {
        JsonObject request=new JsonObject();
        request.addProperty("request","own_hand_order");
        request.add("observation",parseStrictActorReplyView(view));
        String line=exchangeActorRequestWithOneDeadline(actor.writer,actor.reader,
                actor.replyReads,request.toString(),10000L,
                () -> {actor.replyTimedOut=true;actor.process.destroyForcibly();});
        JsonObject reply=parseStrictActorReply(line);
        int size=request.getAsJsonObject("observation").getAsJsonObject("own_cards")
                .getAsJsonArray("hand").size();
        validateHandOrder(reply,size);
        return reply;
    }

    private static JsonObject parseStrictActorReplyView(String view) {
        // Trusted referee projection has the request limit, not the reply limit.
        if(view==null || view.length()>=16000) throw new SecurityException(DENIED);
        return com.google.gson.JsonParser.parseString(view).getAsJsonObject();
    }

    private static void requireCleanup(Player self,int count) {
        Game game=self.getGame();
        int size=self.getCardsIn(ZoneType.Hand).size();
        if(game.getPhaseHandler().getPhase()!=PhaseType.CLEANUP
                || game.getPhaseHandler().getPlayerTurn()!=self
                || self.isUnlimitedHandSize() || size>128 || count<=0 || count>size
                || count!=size-self.getMaxHandSize()) {
            throw new SecurityException(DENIED);
        }
    }

    private static CardCollection resolveOwnHandOrder(Player self,CardCollection snapshot,
            JsonObject reply,int count) {
        requireCleanup(self,count);
        CardCollectionView current=self.getCardsIn(ZoneType.Hand);
        if(snapshot.size()!=current.size()) throw new SecurityException(DENIED);
        for(int i=0;i<snapshot.size();i++) {
            Card card=snapshot.get(i);
            if(card!=current.get(i) || card.getOwner()!=self || card.getController()!=self
                    || card.getGame()!=self.getGame()) throw new SecurityException(DENIED);
        }
        validateHandOrder(reply,snapshot.size());
        CardCollection chosen=new CardCollection();
        for(int i=0;i<count;i++) {
            chosen.add(snapshot.get(reply.getAsJsonArray("hand_priority").get(i).getAsInt()));
        }
        return chosen;
    }

    private final class CleanupController extends ExternalCreatureController {
        int cleanupChoices;
        CardCollection selected=new CardCollection();
        CleanupController(Game game,Player self,Player other,Actor actor) {
            super(game,self,other,actor);
        }
        @Override
        public CardCollectionView chooseCardsToDiscardToMaximumHandSize(int count) {
            requireCleanup(controlled,count);
            CardCollection snapshot=new CardCollection(controlled.getCardsIn(ZoneType.Hand));
            String view=observation(controlled,opponent);
            try {
                JsonObject reply=askHandOrder(subprocess,view);
                if(!view.equals(observation(controlled,opponent))) throw new SecurityException(DENIED);
                CardCollection result=resolveOwnHandOrder(controlled,snapshot,reply,count);
                cleanupChoices++;
                selected=new CardCollection(result);
                return result; // Forge, not this callback, moves cards to the graveyard.
            } catch(SecurityException ex) {
                throw ex;
            } catch(Exception ex) {
                throw new SecurityException("stage10t: own-hand actor failed",ex);
            }
        }
    }

    private CleanupController cleanupController(Game game,Player self,Player other,Actor actor) {
        CleanupController controller=new CleanupController(game,self,other,actor);
        self.dangerouslySetController(controller);
        return controller;
    }

    private static void rejected(Runnable action) {
        try { action.run(); }
        catch(SecurityException expected) { return; }
        Assert.fail("CLEANUP_GUARD_MISSING");
    }

    private static Map<String,Integer> physicalCards(Player player) {
        Map<String,Integer> result=new TreeMap<>();
        for(ZoneType zone:Arrays.asList(ZoneType.Hand,ZoneType.Library,
                ZoneType.Battlefield,ZoneType.Graveyard)) {
            for(Card card:player.getCardsIn(zone)) {
                Assert.assertSame(card.getOwner(),player);
                result.merge(card.getName(),1,Integer::sum);
            }
        }
        return result;
    }

    private void nineCardFixture(Player self) {
        for(String name:Arrays.asList("Counterspell","Shock","Forest","Forest",
                "Grizzly Bears","Forest","Island","Mountain","Grizzly Bears")) {
            addCardToZone(name,self,ZoneType.Hand);
        }
    }

    @Test(timeOut=120000)
    public void ownHandPreferenceUsesIndependentWorkersAndIgnoresPrivateWorlds() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        nineCardFixture(self);
        try(Actor a=handActor();Actor b=handActor()) {
            Assert.assertNotEquals(a.process.pid(),b.process.pid());
            String view=observation(self,other);
            JsonObject before=askHandOrder(a,view);
            Assert.assertEquals(askHandOrder(b,view),before);
            Assert.assertEquals(before.getAsJsonArray("hand_priority").get(0).getAsInt(),0);
            Assert.assertEquals(before.getAsJsonArray("hand_priority").get(1).getAsInt(),1);
            addCardToZone("Counterspell",other,ZoneType.Hand);
            addCardToZone("Shock",other,ZoneType.Library);
            addCardToZone("Forest",self,ZoneType.Library);
            Assert.assertEquals(observation(self,other),view);
            Assert.assertEquals(askHandOrder(a,view),before);
            Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),9);
            Assert.assertTrue(self.getCardsIn(ZoneType.Graveyard).isEmpty());
        }
    }

    @Test
    public void malformedHandPreferencesCannotChooseDuplicateHiddenOrNonintegerCards() {
        validateHandOrder(parseStrictActorReply("{\"intent\":\"hand_order\",\"hand_priority\":[1,0]}"),2);
        for(String raw:Arrays.asList(
                "{}", "{\"intent\":\"hand_order\",\"hand_priority\":[0]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[0,0]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[0,2]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[-1,0]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[0.0,1]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[\"0\",1]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[true,1]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[0,1],\"count\":1}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[0,1],\"opponent_hand\":[0]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[0,1],\"hand_priority\":[1,0]}",
                "{\"intent\":\"hand_order\",\"hand_priority\":[NaN,1]}")) {
            rejected(() -> validateHandOrder(parseStrictActorReply(raw),2));
        }
    }

    @Test(timeOut=120000)
    public void cleanupRejectsWrongCountTurnAndPhaseBeforeActorChoice() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        nineCardFixture(self);
        try(Actor a=handActor();Actor b=handActor()) {
            CleanupController ca=cleanupController(game,self,other,a);
            cleanupController(game,other,self,b);
            rejected(() -> ca.chooseCardsToDiscardToMaximumHandSize(2));
            game.getPhaseHandler().devModeSet(PhaseType.CLEANUP,other);
            rejected(() -> ca.chooseCardsToDiscardToMaximumHandSize(2));
            game.getPhaseHandler().devModeSet(PhaseType.CLEANUP,self);
            for(int wrong:new int[]{-1,0,1,3,10,Integer.MAX_VALUE}) {
                rejected(() -> ca.chooseCardsToDiscardToMaximumHandSize(wrong));
            }
            self.setUnlimitedHandSize(true);
            rejected(() -> ca.chooseCardsToDiscardToMaximumHandSize(2));
            self.setUnlimitedHandSize(false);
            Assert.assertEquals(ca.cleanupChoices,0);
            Assert.assertEquals(ca.chooseCardsToDiscardToMaximumHandSize(2).size(),2,
                    "positive control: genuine count and active cleanup are accepted");
            Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),9);
            Assert.assertTrue(self.getCardsIn(ZoneType.Graveyard).isEmpty());
        }
    }

    @Test
    public void staleAndForeignHandBindingsAreRejectedBeforeAnyDiscard() {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        nineCardFixture(self);
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP,self);
        CardCollection snapshot=new CardCollection(self.getCardsIn(ZoneType.Hand));
        JsonObject order=parseStrictActorReply("{\"intent\":\"hand_order\",\"hand_priority\":[0,1,2,3,4,5,6,7,8]}");
        Assert.assertEquals(resolveOwnHandOrder(self,snapshot,order,2).size(),2);
        Card foreign=addCardToZone("Grizzly Bears",other,ZoneType.Hand);
        CardCollection forged=new CardCollection();
        forged.add(foreign);
        for(int i=1;i<snapshot.size();i++) forged.add(snapshot.get(i));
        rejected(() -> resolveOwnHandOrder(self,forged,order,2));
        game.getAction().moveToGraveyard(snapshot.get(0),null);
        addCardToZone("Forest",self,ZoneType.Hand); // same size, different physical hand
        Map<String,Integer> before=physicalCards(self);
        rejected(() -> resolveOwnHandOrder(self,snapshot,order,2));
        Assert.assertEquals(physicalCards(self),before);
        Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),9);
        Assert.assertEquals(other.getCardsIn(ZoneType.Hand).size(),1);
    }

    @Test(timeOut=120000)
    public void nativeCleanupDiscardsActorSelectedOwnCardsAndResumesTheTurnLoop() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        nineCardFixture(self);
        for(Player p:Arrays.asList(self,other)) {
            for(int i=0;i<5;i++) addCardToZone("Forest",p,ZoneType.Library);
        }
        Map<String,Integer> before=physicalCards(self),otherBefore=physicalCards(other);
        try(Actor a=handActor();Actor b=handActor()) {
            CleanupController ca=cleanupController(game,self,other,a);
            cleanupController(game,other,self,b);
            // Direct state fixture only for this cleanup integration test.
            // Native Forge enters cleanup itself and performs every discard.
            game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN,self);
            game.getPhaseHandler().onStackResolved();
            int steps=0;
            while(ca.cleanupChoices==0 && !game.isGameOver() && steps++<60) {
                game.getPhaseHandler().mainLoopStep();
            }
            Assert.assertEquals(ca.cleanupChoices,1);
            Assert.assertTrue(steps<60);
            Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),7);
            Assert.assertEquals(self.getCardsIn(ZoneType.Graveyard).size(),2);
            Assert.assertEquals(ca.selected.get(0).getName(),"Counterspell");
            Assert.assertEquals(ca.selected.get(1).getName(),"Shock");
            for(Card discarded:self.getCardsIn(ZoneType.Graveyard)) {
                Assert.assertTrue(Arrays.asList("Counterspell","Shock").contains(discarded.getName()));
            }
            Assert.assertEquals(physicalCards(self),before);
            Assert.assertEquals(physicalCards(other),otherBefore);
            while(game.getPhaseHandler().getPlayerTurn()==self && steps++<90) {
                game.getPhaseHandler().mainLoopStep();
            }
            Assert.assertSame(game.getPhaseHandler().getPlayerTurn(),other);
            Assert.assertFalse(game.isGameOver());
            Assert.assertEquals(self.getLife(),20);
            Assert.assertEquals(other.getLife(),20);
            System.out.println("STAGE10T_CLEANUP_EVIDENCE discarded=2 hand=7 nextTurn=true");
        }
    }

    @Test(timeOut=180000)
    public void uncuratedNativeSixtyCardGameEndsWithForgeOwnedOutcome() throws Exception {
        Game game=nativeRegisteredTwoCombatGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        try(Actor first=handActor();Actor second=handActor()) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            CleanupController ca=cleanupController(game,a,b,first);
            CleanupController cb=cleanupController(game,b,a,second);
            doRealNativeStartForMultiTurn(game,ca,cb);
            // No prepared hand, life changes, seeded battlefield, library
            // reordering, seed selection or forced game-over after native start.
            Assert.assertEquals(a.getLife(),20);
            Assert.assertEquals(b.getLife(),20);
            Assert.assertTrue(a.getCardsIn(ZoneType.Battlefield).isEmpty());
            Assert.assertTrue(b.getCardsIn(ZoneType.Battlefield).isEmpty());
            Map<String,Integer> aBefore=physicalCards(a),bBefore=physicalCards(b);
            Assert.assertEquals(aBefore.values().stream().mapToInt(Integer::intValue).sum(),60);
            Assert.assertEquals(bBefore.values().stream().mapToInt(Integer::intValue).sum(),60);
            int aLibrary=a.getCardsIn(ZoneType.Library).size(),bLibrary=b.getCardsIn(ZoneType.Library).size();
            int steps=0;
            while(!game.isGameOver() && steps++<6500) game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<6500,"uncurated native game must finish within its bound");
            Assert.assertTrue(game.isGameOver());
            Assert.assertNotNull(game.getOutcome());
            Assert.assertFalse(game.getOutcome().isDraw());
            Assert.assertTrue(a.hasWon()!=b.hasWon());
            Player winner=a.hasWon()?a:b,loser=a.hasWon()?b:a;
            Assert.assertTrue(loser.hasLost());
            Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
            GameLossReason reason=loser.getOutcome().lossState;
            Assert.assertTrue(reason==GameLossReason.LifeReachedZero || reason==GameLossReason.Milled,
                    "only native combat or empty-library loss is supported in this fixture");
            if(reason==GameLossReason.LifeReachedZero) Assert.assertTrue(loser.getLife()<=0);
            else Assert.assertTrue(loser.getCardsIn(ZoneType.Library).isEmpty());
            Assert.assertEquals(physicalCards(a),aBefore);
            Assert.assertEquals(physicalCards(b),bBefore);
            Assert.assertTrue(a.getCardsIn(ZoneType.Library).size()<aLibrary);
            Assert.assertTrue(b.getCardsIn(ZoneType.Library).size()<bLibrary);
            Assert.assertTrue(ca.priorityCallbacks>5 && cb.priorityCallbacks>5);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertTrue(first.process.isAlive() && second.process.isAlive());
            System.out.println("STAGE10T_UNCURATED_GAME steps="+steps+
                    " turns="+game.getPhaseHandler().getTurn()+" loss="+reason+
                    " preparedHand=false cleanupChoices="+(ca.cleanupChoices+cb.cleanupChoices));
        }
    }
}
