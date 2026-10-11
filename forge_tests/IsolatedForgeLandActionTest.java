package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Test-only Forge real land action gate, TWO independent isolated actor processes.
 * Forge validates timing, ownership, zone and the once-per-turn limit.
 * A user-visible phase/priority feed has NOT been approved; referee invokes
 * an actor at its allowed opportunity without disclosing turn/phase fields.
 * No spell casting, mana cost payment or true full-match progression here.
 */
public final class IsolatedForgeLandActionTest extends AITest {
    private static final class Worker implements AutoCloseable {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;
        Worker() throws Exception {
            String script = System.getenv("STAGE9I_WORKER");
            Assert.assertNotNull(script);
            ProcessBuilder p = new ProcessBuilder("python3","-I","-S","-u",script);
            p.environment().clear();
            p.environment().put("PYTHONIOENCODING","utf-8");
            p.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = p.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.UTF_8));
        }
        JsonObject decide(String observation) throws Exception {
            Assert.assertTrue(observation.length()<16384);
            writer.write(observation); writer.newLine(); writer.flush();
            String line=reader.readLine();
            Assert.assertNotNull(line);
            Assert.assertTrue(line.length()<1024);
            return JsonParser.parseString(line).getAsJsonObject();
        }
        public void close() throws Exception {
            writer.close();
            if(!process.waitFor(5,TimeUnit.SECONDS)) {process.destroyForcibly();Assert.fail("worker did not shut down");}
            Assert.assertEquals(process.exitValue(),0);
            reader.close();
        }
    }

    private static String observe(Player actor,Player opponent,List<String> knownDecklist) {
        JsonObject view=new JsonObject();
        view.addProperty("objective","Win the game under referee-enforced Magic rules");
        JsonObject own=new JsonObject();
        JsonArray hand=new JsonArray();
        for(Card c:actor.getCardsIn(ZoneType.Hand)) {hand.add(c.isFaceDown()?"FACE_DOWN_UNKNOWN":c.getName());}
        own.add("hand",hand);
        Map<String,Integer> multiset=new TreeMap<>();
        for(String name:knownDecklist) multiset.merge(name,1,Integer::sum);
        JsonArray deck=new JsonArray();
        for(Map.Entry<String,Integer> e:multiset.entrySet()) {
            JsonArray entry=new JsonArray();entry.add(e.getKey());entry.add(e.getValue());deck.add(entry);
        }
        own.add("decklist_multiset",deck);
        JsonArray ownBoard=new JsonArray();
        for(Card c:actor.getCardsIn(ZoneType.Battlefield)) ownBoard.add(show(c));
        own.add("table",ownBoard);
        view.add("own_cards",own);
        JsonArray visibleOpponent=new JsonArray();
        for(Card c:opponent.getCardsIn(ZoneType.Battlefield)) visibleOpponent.add(show(c));
        view.add("opponent_table",visibleOpponent);
        JsonObject life=new JsonObject();
        life.addProperty("self",actor.getLife());
        life.addProperty("opponent",opponent.getLife());
        view.add("life_totals",life);
        return view.toString();
    }

    private static JsonObject show(Card c) {
        JsonObject item=new JsonObject();
        if(c.isFaceDown()) {item.addProperty("appearance","face_down");}
        else {item.addProperty("appearance","face_up");item.addProperty("card",c.getName());}
        return item;
    }

    /**
     * Never let the actor invoke Forge action methods itself. Referee executes
     * only checked, currently legal land requests, resolving the real game move.
     */
    private static boolean refereeApply(Game game,Player actor,JsonObject request) {
        if(request==null || !request.has("intent") || !request.get("intent").isJsonPrimitive()) return false;
        String intent=request.get("intent").getAsString();
        if("pass".equals(intent)) return request.size()==1;
        if(!"play_land".equals(intent) || request.size()!=2 || !request.has("hand_index")) return false;
        if(!request.get("hand_index").isJsonPrimitive() ||
           !request.get("hand_index").getAsJsonPrimitive().isNumber()) return false;
        int index;
        try {index=request.get("hand_index").getAsInt();} catch(Exception badIndex) {return false;}
        if(index<0 || index>=actor.getCardsIn(ZoneType.Hand).size()) return false;
        if(game.getPhaseHandler().getPlayerTurn()!=actor) return false;
        Card land=actor.getCardsIn(ZoneType.Hand).get(index);
        if(land.isFaceDown() || !land.hasPlayableLandFace()) return false;
        SpellAbility cause=land.getFirstSpellAbility();
        if(!actor.canPlayLand(land,false,cause)) return false;
        Card moved=actor.playLand(land,cause);
        return moved!=null && actor.getCardsIn(ZoneType.Battlefield).contains(moved);
    }

    @Test(timeOut=120000)
    public void bothIndependentActorsPlayRealForgeLandMoves() throws Exception {
        Game game=initAndCreateGame();
        Player p0=game.getPlayers().get(0),p1=game.getPlayers().get(1);
        p0.setStartingLife(20); p1.setStartingLife(20);
        addCardToZone("Forest",p1,ZoneType.Hand);
        addCardToZone("Island",p0,ZoneType.Hand);
        try(Worker a=new Worker();Worker b=new Worker()) {
            Assert.assertNotEquals(a.process.pid(),b.process.pid());
            JsonObject first=a.decide(observe(p1,p0,Arrays.asList("Forest","Shock")));
            Assert.assertEquals(first.get("intent").getAsString(),"play_land");
            Assert.assertTrue(refereeApply(game,p1,first),"real Forge first player's land must be legal");
            Assert.assertEquals(p1.getLandsPlayedThisTurn(),1);
            Assert.assertEquals(p1.getCardsIn(ZoneType.Hand).size(),0);
            Assert.assertEquals(p1.getCardsIn(ZoneType.Battlefield).size(),1);
            Assert.assertTrue(observe(p0,p1,Arrays.asList("Island","Counterspell")).contains("Forest"));
            JsonObject outOfTurn=b.decide(observe(p0,p1,Arrays.asList("Island","Counterspell")));
            Assert.assertFalse(refereeApply(game,p0,outOfTurn),"referee must reject out-of-turn move");
            // Referee-only fixture changes active turn; no phase info is leaked.
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1,p0);
            Assert.assertTrue(refereeApply(game,p0,b.decide(observe(p0,p1,Arrays.asList("Island","Counterspell")))));
            Assert.assertEquals(p0.getLandsPlayedThisTurn(),1);
            Assert.assertEquals(p0.getCardsIn(ZoneType.Hand).size(),0);
            Assert.assertEquals(p0.getCardsIn(ZoneType.Battlefield).size(),1);
        }
    }

    @Test(timeOut=120000)
    public void secondLandSameTurnIsRejectedByActualForgeRule() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        addCardToZone("Forest",active,ZoneType.Hand);
        addCardToZone("Forest",active,ZoneType.Hand);
        try(Worker a=new Worker()) {
            List<String> deck=Arrays.asList("Forest","Forest");
            Assert.assertTrue(refereeApply(game,active,a.decide(observe(active,opponent,deck))));
            Assert.assertFalse(refereeApply(game,active,a.decide(observe(active,opponent,deck))));
            Assert.assertEquals(active.getLandsPlayedThisTurn(),1);
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),1);
        }
    }

    @Test(timeOut=120000)
    public void privateForgeZonesAndForgedCommandsFailClosed() {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        addCardToZone("Forest",a,ZoneType.Hand);
        String before=observe(a,b,Collections.singletonList("Forest"));
        addCardToZone("Counterspell",b,ZoneType.Hand);
        addCardToZone("Murder",b,ZoneType.Library);
        Assert.assertEquals(observe(a,b,Collections.singletonList("Forest")),before);
        for(String raw:Arrays.asList(
                "{\"intent\":\"set_life\",\"self\":999}",
                "{\"intent\":\"play_land\",\"hand_index\":999}",
                "{\"intent\":\"play_land\",\"hand_index\":0,\"secret\":true}",
                "{\"intent\":\"play_land\",\"hand_index\":\"Forest\"}")) {
            Assert.assertFalse(refereeApply(game,a,JsonParser.parseString(raw).getAsJsonObject()),raw);
        }
        Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(),1);
    }
}
