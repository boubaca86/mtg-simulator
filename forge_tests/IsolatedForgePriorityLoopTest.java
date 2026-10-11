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
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Test-only real Forge referee + two separate, persistent Python actor processes.
 * Only allowlisted JSON is transferred; actors never hold Forge Game or Card.
 * Actors can choose 'pass' but cannot yet play full Magic turns.
 */
public final class IsolatedForgePriorityLoopTest extends AITest {
    private static final List<String> DECK_A = Arrays.asList("Forest","Forest","Shock");
    private static final List<String> DECK_B = Arrays.asList("Island","Counterspell","Grizzly Bears");

    private static final class Actor implements AutoCloseable {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;

        Actor() throws Exception {
            String script = System.getenv("STAGE9K_WORKER");
            Assert.assertNotNull(script, "CI must define STAGE9K_WORKER");
            ProcessBuilder builder = new ProcessBuilder("python3","-I","-S","-u",script);
            builder.environment().clear();
            builder.environment().put("PYTHONIOENCODING","utf-8");
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = builder.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        }

        JsonObject ask(String observation) throws Exception {
            Assert.assertTrue(observation.length() < 16000, "bounded referee observation");
            writer.write(observation);
            writer.newLine();
            writer.flush();
            String line = reader.readLine();
            Assert.assertNotNull(line, "actor must respond");
            Assert.assertTrue(line.length() < 2048, "bounded reply");
            JsonObject reply = JsonParser.parseString(line).getAsJsonObject();
            validateReply(reply,observation);
            Assert.assertEquals(reply.get("pid").getAsLong(),process.pid());
            return reply;
        }

        @Override public void close() throws Exception {
            writer.close();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                Assert.fail("actor process failed to exit");
            }
            Assert.assertEquals(process.exitValue(),0);
            reader.close();
        }
    }

    /** Allowlist is built on REFEREE side directly from authentic Forge state. */
    private static String observation(Player self, Player other, List<String> decklist) {
        JsonObject result = new JsonObject();
        result.addProperty("objective","Win the game under referee-enforced Magic rules");
        JsonObject own = new JsonObject();
        JsonArray hand = new JsonArray();
        for (Card card : self.getCardsIn(ZoneType.Hand)) {
            hand.add(card.isFaceDown() ? "FACE_DOWN_UNKNOWN" : card.getName());
        }
        own.add("hand",hand);
        Map<String,Integer> counts = new TreeMap<>();
        for (String name : decklist) { counts.merge(name,1,Integer::sum); }
        JsonArray deck = new JsonArray();
        for (Map.Entry<String,Integer> e : counts.entrySet()) {
            JsonArray entry = new JsonArray();
            entry.add(e.getKey());
            entry.add(e.getValue());
            deck.add(entry);
        }
        own.add("decklist_multiset",deck);
        JsonArray ownTable = new JsonArray();
        for (Card card : self.getCardsIn(ZoneType.Battlefield)) { ownTable.add(tableCard(card)); }
        own.add("table",ownTable);
        result.add("own_cards",own);
        JsonArray opponentTable = new JsonArray();
        for (Card card : other.getCardsIn(ZoneType.Battlefield)) { opponentTable.add(tableCard(card)); }
        result.add("opponent_table",opponentTable);
        JsonObject life = new JsonObject();
        life.addProperty("self",self.getLife());
        life.addProperty("opponent",other.getLife());
        result.add("life_totals",life);
        return result.toString();
    }

    private static JsonObject tableCard(Card card) {
        JsonObject record = new JsonObject();
        if (card.isFaceDown()) {
            record.addProperty("appearance","face_down"); // no true name/type/ID
        } else {
            record.addProperty("appearance","face_up");
            record.addProperty("card",card.getName());
        }
        return record;
    }

    /** Unapproved commands, including a direct life-total mutation, fail closed. */
    private static void validateReply(JsonObject reply,String observation) throws Exception {
        Assert.assertEquals(reply.size(),5,"no unapproved actor command keys");
        for (String name : Arrays.asList("intent","pid","observation_sha256","self_life","opponent_life")) {
            Assert.assertTrue(reply.has(name),"missing reply field "+name);
        }
        Assert.assertEquals(reply.get("intent").getAsString(),"pass","only inert pass is enabled");
        Assert.assertEquals(reply.get("observation_sha256").getAsString(),sha256(observation),
                "the actor received exactly the projected information");
        JsonObject life = JsonParser.parseString(observation).getAsJsonObject().getAsJsonObject("life_totals");
        Assert.assertEquals(reply.get("self_life").getAsInt(),life.get("self").getAsInt());
        Assert.assertEquals(reply.get("opponent_life").getAsInt(),life.get("opponent").getAsInt());
    }

    private static String sha256(String data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) { hex.append(String.format("%02x",b&0xff)); }
        return hex.toString();
    }


    /**
     * Actual Forge PlayerController inserted only in disposable test game.
     * The inherited Forge AI may handle automatic mandatory non-strategic
     * callbacks; when Forge asks for priority, this override MUST consult
     * the independent process, NEVER AiController's full-information policy.
     */
    private static final class ExternalPassController extends PlayerControllerAi {
        final Actor subprocess;
        final Player controlled;
        final Player opponent;
        final List<String> ownDecklist;
        int priorityCallbacks = 0;
        ExternalPassController(Game game,Player self,Player other,Actor worker,List<String> decklist) {
            super(game,self,self.getController().getLobbyPlayer());
            subprocess=worker;
            controlled=self;
            opponent=other;
            ownDecklist=decklist;
        }
        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            try {
                String view=observation(controlled,opponent,ownDecklist);
                JsonObject response=subprocess.ask(view);
                Assert.assertEquals(response.get("intent").getAsString(),"pass");
                priorityCallbacks++;
                return null; // CR 117: a real passed-priority callback
            } catch(Exception ex) {
                throw new IllegalStateException("Restricted actor priority IPC failed",ex);
            }
        }
    }

    private static ExternalPassController attach(Game game,Player self,Player opponent,Actor actor,List<String> deck) {
        ExternalPassController controller=new ExternalPassController(game,self,opponent,actor,deck);
        self.dangerouslySetController(controller);
        return controller;
    }

    private static void seedPrivateLibraries(Player a,Player b) {
        for(int i=0;i<12;i++) {
            Card ca=Card.fromPaperCard(
                forge.model.FModel.getMagicDb().getCommonCards().getCard("Forest"),a);
            ca.setGameTimestamp(a.getGame().getNextTimestamp());
            a.getZone(ZoneType.Library).add(ca);
            Card cb=Card.fromPaperCard(
                forge.model.FModel.getMagicDb().getCommonCards().getCard("Island"),b);
            cb.setGameTimestamp(b.getGame().getNextTimestamp());
            b.getZone(ZoneType.Library).add(cb);
        }
    }

    @Test(timeOut=120000)
    public void realPriorityRequiresTwoExternalActorPassesToAdvance() throws Exception {
        Game game=initAndCreateGame();
        Player p0=game.getPlayers().get(0),p1=game.getPlayers().get(1);
        seedPrivateLibraries(p0,p1);
        try(Actor a=new Actor(); Actor b=new Actor()) {
            Assert.assertNotEquals(a.process.pid(),b.process.pid());
            ExternalPassController ca=attach(game,p0,p1,a,DECK_A);
            ExternalPassController cb=attach(game,p1,p0,b,DECK_B);
            Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.MAIN1);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(ca.priorityCallbacks+cb.priorityCallbacks,1,
                "exactly one external player received the first real priority");
            Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.MAIN1,
                "one pass must not end the phase");
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(ca.priorityCallbacks+cb.priorityCallbacks,2,
                "the other external player must receive second priority");
            Assert.assertEquals(ca.priorityCallbacks,1);
            Assert.assertEquals(cb.priorityCallbacks,1);
            Assert.assertNotEquals(game.getPhaseHandler().getPhase(),PhaseType.MAIN1,
                "Forge, not a dev turn switch, advances after both priority passes");
            Assert.assertEquals(game.getStack().isEmpty(),true);
        }
    }

    @Test(timeOut=120000)
    public void realForgeTurnLoopAutomaticallyDrawsWithoutDevTurnSwitch() throws Exception {
        Game game=initAndCreateGame();
        Player p0=game.getPlayers().get(0),p1=game.getPlayers().get(1);
        seedPrivateLibraries(p0,p1);
        Player startingActive=game.getPhaseHandler().getPlayerTurn();
        Player next=startingActive==p0?p1:p0;
        int initialCards=next.getCardsIn(ZoneType.Hand).size();
        int initialLibrary=next.getCardsIn(ZoneType.Library).size();
        try(Actor a=new Actor(); Actor b=new Actor()) {
            ExternalPassController ca=attach(game,p0,p1,a,DECK_A);
            ExternalPassController cb=attach(game,p1,p0,b,DECK_B);
            int steps=0;
            boolean witnessedNextTurn=false;
            while(!game.isGameOver() && steps++<160) {
                game.getPhaseHandler().mainLoopStep();
                if(game.getPhaseHandler().getPlayerTurn()==next &&
                   next.getCardsIn(ZoneType.Hand).size()>initialCards) {
                    witnessedNextTurn=true;
                    break;
                }
            }
            Assert.assertTrue(witnessedNextTurn,
                    "genuine PhaseHandler turn transition and onPhaseBegin(DRAW) required");
            Assert.assertTrue(steps<160,"bounded real turn progression");
            Assert.assertEquals(next.getCardsIn(ZoneType.Hand).size(),initialCards+1);
            Assert.assertEquals(next.getCardsIn(ZoneType.Library).size(),initialLibrary-1);
            Assert.assertTrue(ca.priorityCallbacks>0);
            Assert.assertTrue(cb.priorityCallbacks>0);
            Assert.assertEquals(game.getStack().isEmpty(),true);
            // A real draw is visible to its owner, never to the opponent.
            Assert.assertTrue(observation(next,startingActive,DECK_B).contains(
                next.getCardsIn(ZoneType.Hand).get(0).getName()));
            Assert.assertFalse(observation(startingActive,next,DECK_A).contains(
                "\"hand\":[\""+next.getCardsIn(ZoneType.Hand).get(0).getName()+"\"]"));
        }
    }

    @Test(timeOut=120000)
    public void realPriorityObservationOmitsForgedOpponentSecretsAndPhase() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(self,other);
        try(Actor selfActor=new Actor();Actor otherActor=new Actor()) {
            ExternalPassController cs=attach(game,self,other,selfActor,DECK_A);
            attach(game,other,self,otherActor,DECK_B);
            String baseline=observation(self,other,DECK_A);
            Assert.assertFalse(baseline.contains("\"phase\""));
            Assert.assertFalse(baseline.contains("\"priority\""));
            Assert.assertFalse(baseline.contains("\"turn\""));
            Assert.assertFalse(baseline.contains("\"library_order\""));
            Assert.assertFalse(baseline.contains("\"stack\""));
            Card secret=addCardToZone("Counterspell",other,ZoneType.Hand);
            addCardToZone("Shock",other,ZoneType.Library);
            Assert.assertEquals(observation(self,other,DECK_A),baseline,
                "private actual Forge zone changes must not affect actor input");
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(cs.priorityCallbacks,1);
            Assert.assertFalse(observation(self,other,DECK_A).contains(secret.getName()));
        }
    }
}
