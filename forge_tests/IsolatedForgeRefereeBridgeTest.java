package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.effects.LifeGainEffect;
import forge.game.ability.effects.LifeLoseEffect;
import forge.game.card.Card;
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
public final class IsolatedForgeRefereeBridgeTest extends AITest {
    private static final List<String> DECK_A = Arrays.asList("Forest","Forest","Shock");
    private static final List<String> DECK_B = Arrays.asList("Island","Counterspell","Grizzly Bears");

    private static final class Actor implements AutoCloseable {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;

        Actor() throws Exception {
            String script = System.getenv("STAGE9H_WORKER");
            Assert.assertNotNull(script, "CI must define STAGE9H_WORKER");
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

    /** A real Forge SpellAbilityEffect changes life; the AI cannot do this. */
    private static void resolveLife(Card source,Player actor,int change) {
        SpellAbility sa = AbilityFactory.getAbility(
                (change<0 ? "DB$ LoseLife" : "DB$ GainLife")
                +" | Defined$ You | LifeAmount$ "+Math.abs(change),source);
        sa.setActivatingPlayer(actor);
        if (change<0) { new LifeLoseEffect().resolve(sa); }
        else { new LifeGainEffect().resolve(sa); }
    }

    @Test(timeOut = 120000)
    public void independentActorsReceiveRealForgeLifeEffectChanges() throws Exception {
        Game game = initAndCreateGame();
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        a.setStartingLife(20); b.setStartingLife(20);
        Card sourceA = addCard("Forest",a), sourceB = addCard("Island",b);
        addCardToZone("Shock",a,ZoneType.Hand);
        addCardToZone("Counterspell",b,ZoneType.Hand);
        try (Actor actorA=new Actor(); Actor actorB=new Actor()) {
            Assert.assertNotEquals(actorA.process.pid(),actorB.process.pid());
            JsonObject firstA=actorA.ask(observation(a,b,DECK_A));
            JsonObject firstB=actorB.ask(observation(b,a,DECK_B));
            Assert.assertEquals(firstA.get("self_life").getAsInt(),20);
            Assert.assertEquals(firstB.get("opponent_life").getAsInt(),20);
            resolveLife(sourceA,a,-3);
            resolveLife(sourceB,b,+4);
            Assert.assertEquals(a.getLife(),17,"Forge LifeLoseEffect must change life");
            Assert.assertEquals(b.getLife(),24,"Forge LifeGainEffect must change life");
            JsonObject afterA=actorA.ask(observation(a,b,DECK_A));
            JsonObject afterB=actorB.ask(observation(b,a,DECK_B));
            Assert.assertEquals(afterA.get("self_life").getAsInt(),17);
            Assert.assertEquals(afterA.get("opponent_life").getAsInt(),24);
            Assert.assertEquals(afterB.get("self_life").getAsInt(),24);
            Assert.assertEquals(afterB.get("opponent_life").getAsInt(),17);
            Assert.assertNotEquals(firstA.get("observation_sha256").getAsString(),
                                   afterA.get("observation_sha256").getAsString());
        }
    }

    @Test(timeOut = 120000)
    public void actualForgeHiddenHandAndLibraryDoNotInfluenceOpponentsObservation() {
        Game game = initAndCreateGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        a.setStartingLife(20); b.setStartingLife(20);
        addCard("Forest",a); addCard("Island",b);
        Card hidden=addCard("Grizzly Bears",b);
        hidden.turnFaceDown(true);
        String before=observation(a,b,DECK_A);
        Assert.assertFalse(before.contains("Grizzly Bears"));
        addCardToZone("Counterspell",b,ZoneType.Hand);
        addCardToZone("Shock",b,ZoneType.Library);
        Assert.assertEquals(observation(a,b,DECK_A),before,
                "adding actual concealed Forge state cannot change the actor view");
        JsonArray table=JsonParser.parseString(before).getAsJsonObject().getAsJsonArray("opponent_table");
        Assert.assertEquals(table.size(),2);
        Assert.assertEquals(table.get(1).getAsJsonObject().size(),1,
                "face-down name, type and ID must not be serialized");
    }

    @Test(timeOut = 120000)
    public void actorCannotForgeLifeOrHiddenInformationCommands() throws Exception {
        JsonObject injected=new JsonObject();
        injected.addProperty("intent","pass");
        injected.addProperty("pid",100);
        injected.addProperty("observation_sha256",sha256("{}"));
        injected.addProperty("self_life",20);
        injected.addProperty("opponent_life",20);
        injected.addProperty("set_life",999);
        try {
            validateReply(injected,"{}");
            Assert.fail("direct actor life commands must be rejected");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("unapproved"));
        }
    }
}
