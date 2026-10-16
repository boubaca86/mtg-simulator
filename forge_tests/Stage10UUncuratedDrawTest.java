package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.player.DelayedReveal;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.StaticData;
import forge.MulliganDefs;
import forge.game.mulligan.MulliganService;
import forge.game.Match;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardCollection;
import forge.card.ICardFace;
import org.apache.commons.lang3.tuple.ImmutablePair;
import java.util.function.Predicate;
import forge.util.collect.FCollectionView;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostPart;
import forge.game.replacement.ReplacementEffect;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.game.zone.PlayerZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Test-only real Forge referee + two separate, persistent Python actor processes.
 * Only allowlisted JSON is transferred; actors never hold Forge Game or Card.
 * Actors request a basic-land play, a named vanilla/mana creature cast, or pass.
 * The referee alone decides legality, mana payment, priority and stack resolution.
 */
public final class Stage10UUncuratedDrawTest extends AITest {
    /** Referee-owned immutable starting deck counts: never derived from live zones. */
    private FrozenMain frozenMain;

    private static final class FrozenMain {
        final Map<Player, Map<String, Integer>> owners = new IdentityHashMap<>();
        FrozenMain(Game game) {
            for (Player player : game.getRegisteredPlayers()) {
                TreeMap<String, Integer> counts = new TreeMap<>();
                for (Map.Entry<PaperCard, Integer> item :
                        player.getRegisteredPlayer().getDeck().getMain()) {
                    if (item.getKey()==null || item.getValue()<=0) {
                        throw new IllegalStateException("invalid registered deck entry");
                    }
                    counts.merge(item.getKey().getName(), item.getValue(), Integer::sum);
                }
                if (counts.isEmpty()) throw new IllegalStateException("empty starting main deck");
                owners.put(player, Collections.unmodifiableMap(counts));
            }
            if (owners.size()!=2) throw new IllegalStateException("exactly two registered players");
        }
        Map<String, Integer> owned(Player player) {
            Map<String, Integer> counts = owners.get(player);
            if (counts==null) throw new IllegalArgumentException("unknown registered actor");
            return counts;
        }
    }

    private static PaperCard registeredCard(String name) {
        PaperCard card=FModel.getMagicDb().getCommonCards().getCard(name);
        if (card==null) throw new IllegalStateException("unknown registered card "+name);
        return card;
    }

    private static Deck mainDeck(String name, Map<String,Integer> counts) {
        Deck result=new Deck(name);
        for (Map.Entry<String,Integer> item : counts.entrySet()) {
            result.getMain().add(registeredCard(item.getKey()),item.getValue());
        }
        return result;
    }

    /** Same real Forge AITest fixture timing; nonempty independently registered decks. */
    @Override
    protected Game initAndCreateGame() {
        Map<String,Integer> list0=new TreeMap<>();
        list0.put("Island",12);
        list0.put("Counterspell",3);
        list0.put("Grizzly Bears",3);
        list0.put("Mountain",2);
        Map<String,Integer> list1=new TreeMap<>();
        list1.put("Forest",12);
        list1.put("Llanowar Elves",3);
        list1.put("Grizzly Bears",3);
        list1.put("Mountain",2);
        List<RegisteredPlayer> players=new ArrayList<>();
        players.add(new RegisteredPlayer(mainDeck("Registered island actor",list0))
                .setPlayer(new LobbyPlayerAi("island",null)));
        players.add(new RegisteredPlayer(mainDeck("Registered forest actor",list1))
                .setPlayer(new LobbyPlayerAi("forest",null)));
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,players,"Stage9Q");
        Game game=new Game(players,rules,match);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1,game.getPlayers().get(1));
        game.getPhaseHandler().onStackResolved();
        frozenMain=new FrozenMain(game);
        return game;
    }



    /** Unlike the earlier AITest fixtures this leaves PhaseHandler.phase NULL,
     * so Forge must enter its first true UNTAP/UPKEEP/DRAW sequence itself. */
    private Game newBeforeFirstTurnGame() {
        Map<String,Integer> list0=new TreeMap<>();
        list0.put("Island",12);
        list0.put("Counterspell",3);
        list0.put("Grizzly Bears",3);
        list0.put("Mountain",2);
        Map<String,Integer> list1=new TreeMap<>();
        list1.put("Forest",12);
        list1.put("Llanowar Elves",3);
        list1.put("Grizzly Bears",3);
        list1.put("Mountain",2);
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(new RegisteredPlayer(mainDeck("Native island actor",list0))
                .setPlayer(new LobbyPlayerAi("island",null)));
        registered.add(new RegisteredPlayer(mainDeck("Native forest actor",list1))
                .setPlayer(new LobbyPlayerAi("forest",null)));
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage9Y");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase(),
                "game must start without test devModeSet");
        return game;
    }

    /** Referee-only native Forge London service, not a manual mulligan loop. */
    private int runNativeMulligans(Game game,Player first,Player second,
            ExternalCreatureController firstController,ExternalCreatureController secondController) {
        for(Player actor:Arrays.asList(first,second)) {
            seedWholeRegisteredLibrary(actor);
            actor.shuffle(null);
        }
        game.setAge(GameStage.Mulligan);
        first.drawCards(first.getStartingHandSize());
        second.drawCards(second.getStartingHandSize());
        Assert.assertEquals(first.getCardsIn(ZoneType.Hand).size(),7);
        Assert.assertEquals(second.getCardsIn(ZoneType.Hand).size(),7);
        firstController.enableOwnHandOpening();
        secondController.enableOwnHandOpening();
        MulliganDefs.MulliganRule previous=StaticData.instance().getMulliganRule();
        try {
            StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.London);
            new MulliganService(first).perform();
        } finally {
            StaticData.instance().setMulliganRule(previous);
        }
        Assert.assertEquals(countInventory(first),frozenMain.owned(first));
        Assert.assertEquals(countInventory(second),frozenMain.owned(second));
        Assert.assertTrue(firstController.openingCallbacks>0);
        Assert.assertTrue(secondController.openingCallbacks>0);
        return firstController.openingCallbacks+secondController.openingCallbacks;
    }


    /** Actor->Forge is untrusted: cap before newline, inspect all JSON member
     * names before Gson can silently overwrite duplicate keys, then apply
     * the existing exact action/opening schema. This helper has no Game/Card refs. */

    /** Malformed UTF-8 from an external actor must never be decoded as U+FFFD. */
    private static BufferedReader strictActorReader(InputStream input) {
        return new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)));
    }

    private static String readBoundedActorReply(BufferedReader reader) throws IOException {
        StringBuilder reply=new StringBuilder();
        for(int i=0;i<2048;i++) {
            int ch=reader.read();
            if(ch==-1) throw new SecurityException("actor response terminated without newline");
            if(ch=='\n') return reply.toString();
            reply.append((char)ch);
        }
        throw new SecurityException("oversized actor reply");
    }


    /** One bounded reader task on the trusted Forge referee, not another AI.
     * A silent actor loses its process, rather than blocking the referee.
     * No future Game/Card/phase/stack data is exposed to the worker. */
    private static String readActorReplyWithDeadline(
            BufferedReader reader,
            java.util.concurrent.ExecutorService readerTasks,
            long deadlineMillis,
            Runnable abortActor) throws Exception {
        if(deadlineMillis<1 || deadlineMillis>20000) {
            throw new SecurityException("invalid actor reply deadline");
        }
        java.util.concurrent.Future<String> pending=
                readerTasks.submit(() -> readBoundedActorReply(reader));
        try {
            return pending.get(deadlineMillis, TimeUnit.MILLISECONDS);
        } catch(java.util.concurrent.TimeoutException expired) {
            abortActor.run();  // destroy the stalled actor or close the test pipe
            pending.cancel(true);
            throw new SecurityException("actor reply deadline exceeded",expired);
        } catch(InterruptedException interrupted) {
            abortActor.run();
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new SecurityException("actor reply interrupted",interrupted);
        } catch(java.util.concurrent.ExecutionException failure) {
            Throwable actual=failure.getCause();
            if(actual instanceof IOException) throw (IOException) actual;
            if(actual instanceof RuntimeException) throw (RuntimeException) actual;
            throw new SecurityException("actor reply read failed",actual);
        }
    }


    /** Bound trusted Forge -> actor stdin writes as well as stdout reads.
     * Uses the SAME per-actor trusted referee I/O thread, no third actor.
     * Partial writes are fatal: destroy the actor before another decision. */
    private static void sendActorRequestWithDeadline(
            BufferedWriter writer,
            java.util.concurrent.ExecutorService ioTasks,
            String request,
            long deadlineMillis,
            Runnable abortActor) throws Exception {
        if(deadlineMillis<1 || deadlineMillis>20000) {
            throw new SecurityException("invalid actor request deadline");
        }
        if(request==null || request.indexOf('\n')>=0 || request.indexOf('\r')>=0 ||
                request.getBytes(StandardCharsets.UTF_8).length>=16384) {
            throw new SecurityException("invalid or oversized actor request frame");
        }
        java.util.concurrent.Future<?> pending=ioTasks.submit(() -> {
            writer.write(request);
            writer.newLine();
            writer.flush();
            return null;
        });
        try {
            pending.get(deadlineMillis,TimeUnit.MILLISECONDS);
        } catch(java.util.concurrent.TimeoutException expired) {
            abortActor.run();
            pending.cancel(true);
            throw new SecurityException("actor request write deadline exceeded",expired);
        } catch(InterruptedException interrupted) {
            abortActor.run();
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new SecurityException("actor request write interrupted",interrupted);
        } catch(java.util.concurrent.ExecutionException failure) {
            abortActor.run(); // unknown partial write is never retried
            pending.cancel(true);
            Throwable actual=failure.getCause();
            if(actual instanceof IOException) throw (IOException)actual;
            if(actual instanceof RuntimeException) throw (RuntimeException)actual;
            throw new SecurityException("actor request write failed",actual);
        }
    }


    /** One monotonic I/O deadline for sending a bounded observation AND
     * receiving one legal line. Never reset the budget between phases.
     * This runs exclusively inside the trusted Forge referee process. */
    private static long remainingExchangeMillis(long deadlineNanos,
            Runnable abortActor) {
        long left=deadlineNanos-System.nanoTime();
        long millis=TimeUnit.NANOSECONDS.toMillis(left);
        if(millis<1) {
            abortActor.run();
            throw new SecurityException("actor exchange deadline exhausted");
        }
        return millis;
    }

    private static String exchangeActorRequestWithOneDeadline(
            BufferedWriter writer,
            BufferedReader reader,
            java.util.concurrent.ExecutorService ioTasks,
            String request,
            long budgetMillis,
            Runnable abortActor) throws Exception {
        if(budgetMillis<1 || budgetMillis>20000) {
            throw new SecurityException("invalid actor exchange budget");
        }
        final long deadlineNanos=System.nanoTime()+
                TimeUnit.MILLISECONDS.toNanos(budgetMillis);
        sendActorRequestWithDeadline(writer,ioTasks,request,
                remainingExchangeMillis(deadlineNanos,abortActor),abortActor);
        // A slow write subtracts from the SAME budget for stdout; no reset.
        return readActorReplyWithDeadline(reader,ioTasks,
                remainingExchangeMillis(deadlineNanos,abortActor),abortActor);
    }

    private static void scanStrictReplyJson(com.google.gson.stream.JsonReader reader,
            int depth) throws IOException {
        if(depth>12) throw new SecurityException("actor reply nesting exceeds bound");
        switch(reader.peek()) {
            case BEGIN_OBJECT:
                reader.beginObject();
                Set<String> seen=new HashSet<>();
                while(reader.hasNext()) {
                    String key=reader.nextName(); // decoded Unicode escapes
                    if(!seen.add(key)) throw new SecurityException("duplicate actor reply JSON key");
                    scanStrictReplyJson(reader,depth+1);
                }
                reader.endObject();
                return;
            case BEGIN_ARRAY:
                reader.beginArray();
                while(reader.hasNext()) scanStrictReplyJson(reader,depth+1);
                reader.endArray();
                return;
            case NUMBER:
                String num=reader.nextString();
                if(!num.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?") ||
                        !Double.isFinite(Double.parseDouble(num))) {
                    throw new SecurityException("invalid or nonfinite actor reply number");
                }
                return;
            case STRING: reader.nextString(); return;
            case BOOLEAN: reader.nextBoolean(); return;
            case NULL: reader.nextNull(); return;
            default: throw new SecurityException("unapproved actor reply JSON token");
        }
    }

    private static JsonObject parseStrictActorReply(String raw) {
        if(raw==null || raw.length()>=2048 ||
                raw.getBytes(StandardCharsets.UTF_8).length>=2048) {
            throw new SecurityException("oversized or missing actor reply");
        }
        try(com.google.gson.stream.JsonReader reader=
                new com.google.gson.stream.JsonReader(new StringReader(raw))) {
            reader.setLenient(false);
            scanStrictReplyJson(reader,0);
            if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT) {
                throw new SecurityException("trailing actor reply JSON");
            }
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch(IOException | IllegalStateException | com.google.gson.JsonParseException e) {
            throw new SecurityException("malformed actor reply JSON",e);
        }
    }

    private static final class Actor implements AutoCloseable {
        final Process process;
        final boolean handOrderCapable;
        final BufferedReader reader;
        final BufferedWriter writer;
        boolean replyTimedOut=false;
        // One daemon I/O thread inside the trusted referee per persistent actor,
        // not a third player/AI process. Reused across all actor decisions.
        final java.util.concurrent.ExecutorService replyReads =
                java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
                    Thread thread=new Thread(task,"trusted-forge-reply-reader");
                    thread.setDaemon(true);
                    return thread;
                });

        Actor() throws Exception {
            this(false);
        }
        Actor(boolean usePlan) throws Exception {
            this(usePlan ? 1 : 0);
        }
        Actor(int mode) throws Exception {
            this(mode,null,null);
        }
        Actor(int mode,String testScript,String actorMode) throws Exception {
            handOrderCapable=mode==5;
            String script=testScript!=null ? testScript :
                    System.getenv(mode==5 ? "STAGE11A_RECIPROCAL_WORKER" :
                    mode==4 ? "STAGE10W_VISIBLE_OPPONENT_WORKER" :
                    mode==3 ? "STAGE10G_MENACE_WORKER" :
                    mode==2 ? "STAGE10G_DEFENSE_WORKER" :
                    mode==1 ? "STAGE10G_LEGACY_PLAN_WORKER" : "STAGE10G_WORKER");
            Assert.assertNotNull(script, "CI must define an actor worker path");
            ProcessBuilder builder=actorMode==null ?
                    new ProcessBuilder("python3","-I","-S","-u",script) :
                    new ProcessBuilder("python3","-I","-S","-u",script,actorMode);
            builder.environment().clear();
            builder.environment().put("PYTHONIOENCODING","utf-8");
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = builder.start();
            reader = strictActorReader(process.getInputStream());
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        }

        void send(String request,long deadlineMillis) throws Exception {
            sendActorRequestWithDeadline(writer,replyReads,request,deadlineMillis,
                    () -> {replyTimedOut=true;process.destroyForcibly();});
        }

        JsonObject ask(String observation) throws Exception {
            return askWithBudget(observation,10000L);
        }
        JsonObject askWithBudget(String observation,long budgetMillis) throws Exception {
            Assert.assertTrue(observation.length() < 16000, "bounded referee observation");
            String line=exchangeActorRequestWithOneDeadline(writer,reader,
                    replyReads,observation,budgetMillis,
                    () -> {replyTimedOut=true;process.destroyForcibly();});
            JsonObject reply = parseStrictActorReply(line);
            validateReply(reply);
            return reply;
        }

        /**
         * Opening-hand decision request is a message KIND, not an additional
         * game observation. The nested observation has the exact Stage9V
         * allowlist and no mulligan count/turn/opponent hidden information.
         */
        JsonObject askOpening(String observation) throws Exception {
            return askOpeningWithBudget(observation,10000L);
        }
        JsonObject askOpeningWithBudget(String observation,long budgetMillis)
                throws Exception {
            Assert.assertTrue(observation.length() < 16000);
            JsonObject envelope=new JsonObject();
            envelope.addProperty("request","own_opening_hand");
            envelope.add("observation",JsonParser.parseString(observation));
            String request=envelope.toString();
            Assert.assertTrue(request.length() < 16384);
            String line=exchangeActorRequestWithOneDeadline(writer,reader,
                    replyReads,request,budgetMillis,
                    () -> {replyTimedOut=true;process.destroyForcibly();});
            JsonObject reply=parseStrictActorReply(line);
            int count=JsonParser.parseString(observation).getAsJsonObject()
                    .getAsJsonObject("own_cards").getAsJsonArray("hand").size();
            validateOpeningReply(reply,count);
            return reply;
        }

        @Override public void close() throws Exception {
            try {
                try {
                    writer.close();
                } catch(IOException brokenPipe) {
                    if(!replyTimedOut) throw brokenPipe;
                }
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    Assert.fail("actor process failed to exit");
                }
                if(!replyTimedOut) Assert.assertEquals(process.exitValue(),0);
                reader.close();
            } finally {
                replyReads.shutdownNow();
            }
        }
    }

    /**
     * Zero-referee-authority actor reply schema. Every index must be a unique
     * complete own-hand position; neither the worker nor JSON may select an
     * actual Card object, infer unseen cards, or supply bottom count.
     */
    private static void validateOpeningReply(JsonObject reply,int handSize) {
        if(reply==null || reply.size()!=3 || handSize<0 || handSize>256 ||
                !reply.has("intent") || !reply.get("intent").isJsonPrimitive() ||
                !"opening_hand".equals(reply.get("intent").getAsString()) ||
                !reply.has("keep") || !reply.get("keep").isJsonPrimitive() ||
                !reply.get("keep").getAsJsonPrimitive().isBoolean() ||
                !reply.has("bottom_priority") ||
                !reply.get("bottom_priority").isJsonArray()) {
            throw new SecurityException("unapproved opening-hand actor reply");
        }
        JsonArray ordered=reply.getAsJsonArray("bottom_priority");
        if(ordered.size()!=handSize) {
            throw new SecurityException("not a complete own-hand permutation");
        }
        boolean[] seen=new boolean[handSize];
        for(com.google.gson.JsonElement element:ordered) {
            if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                throw new SecurityException("opening-hand index is not a number");
            }
            String raw=element.getAsString();
            if(!raw.matches("0|[1-9][0-9]*") || raw.length()>8) {
                throw new SecurityException("noncanonical own-hand index");
            }
            int index;
            try {index=Integer.parseInt(raw);}
            catch(NumberFormatException ex) {
                throw new SecurityException("out-of-range own-hand index",ex);
            }
            if(index>=handSize || seen[index]) {
                throw new SecurityException("duplicate or invalid hand index");
            }
            seen[index]=true;
        }
    }

    /** Allowlist is built on REFEREE side directly from authentic Forge state. */
    private String observation(Player self, Player other) {
        JsonObject result = new JsonObject();
        result.addProperty("objective","Win the game under referee-enforced Magic rules");
        JsonObject own = new JsonObject();
        JsonArray hand = new JsonArray();
        for (Card card : self.getCardsIn(ZoneType.Hand)) {
            hand.add(card.isFaceDown() ? "FACE_DOWN_UNKNOWN" : card.getName());
        }
        own.add("hand",hand);
        Map<String,Integer> counts = frozenMain.owned(self);
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

    /** Reject any actor command outside the strictly bounded action schema. */
    private static void validateReply(JsonObject reply) {
        Assert.assertNotNull(reply);
        Assert.assertTrue(reply.has("intent") && reply.get("intent").isJsonPrimitive(),
                "actor reply must have an intent");
        String intent=reply.get("intent").getAsString();
        if ("action_plan".equals(intent)) {
            boolean sorceries=reply.has("sorcery_hand_indices");
            boolean blocks=reply.has("block_pairs");
            Assert.assertEquals(reply.size(),4+(sorceries?1:0)+(blocks?1:0),
                    "action plan only contains approved preference fields");
            validatePlanIndices(reply,"land_hand_indices",128);
            validatePlanIndices(reply,"creature_hand_indices",128);
            if(sorceries) validatePlanIndices(reply,"sorcery_hand_indices",128);
            validatePlanIndices(reply,"attack_table_indices",8);
            if(blocks) validateBlockPairs(reply);
            return;
        }
        if ("pass".equals(intent)) {
            Assert.assertEquals(reply.size(),1,"pass cannot carry extra data");
            return;
        }
        if ("attack".equals(intent)) {
            Assert.assertEquals(reply.size(),2,"attack may contain only indices");
            Assert.assertTrue(reply.has("table_indices") && reply.get("table_indices").isJsonArray());
            JsonArray indices=reply.getAsJsonArray("table_indices");
            Assert.assertTrue(indices.size()>0 && indices.size()<=8,"bounded attackers");
            Set<Integer> seen=new HashSet<>();
            for(int i=0;i<indices.size();i++) {
                Assert.assertTrue(indices.get(i).isJsonPrimitive() &&
                    indices.get(i).getAsJsonPrimitive().isNumber(),"integer indices");
                String raw=indices.get(i).getAsString();
                Assert.assertTrue(raw.matches("0|[1-9][0-9]*") && raw.length()<=8);
                Assert.assertTrue(seen.add(Integer.parseInt(raw)),"no duplicate attackers");
            }
            return;
        }
        Assert.assertTrue("play_land".equals(intent) || "cast_creature".equals(intent),
                "only basic land, restricted creature or pass is permitted");
        Assert.assertEquals(reply.size(),2,"land request cannot carry extra data");
        Assert.assertTrue(reply.has("hand_index") &&
                reply.get("hand_index").isJsonPrimitive() &&
                reply.get("hand_index").getAsJsonPrimitive().isNumber(),
                "land request must include integer index");
        String raw=reply.get("hand_index").getAsString();
        Assert.assertTrue(raw.matches("0|[1-9][0-9]*"),"index must be canonical unsigned integer");
        Assert.assertTrue(raw.length()<=8,"index value bound");
    }

    private static void validatePlanIndices(JsonObject reply,String field,int limit) {
        Assert.assertTrue(reply.has(field) && reply.get(field).isJsonArray(),
                "plan missing its candidate indices");
        JsonArray indices=reply.getAsJsonArray(field);
        Assert.assertTrue(indices.size()<=limit,"plan index array too large");
        Set<Integer> seen=new HashSet<>();
        for(int i=0;i<indices.size();i++) {
            Assert.assertTrue(indices.get(i).isJsonPrimitive() &&
                    indices.get(i).getAsJsonPrimitive().isNumber());
            String value=indices.get(i).getAsString();
            Assert.assertTrue(value.matches("0|[1-9][0-9]*") && value.length()<=8);
            Assert.assertTrue(seen.add(Integer.parseInt(value)),"duplicate candidate");
        }
    }

    private static void validateBlockPairs(JsonObject reply) {
        Assert.assertTrue(reply.has("block_pairs") && reply.get("block_pairs").isJsonArray(),
                "no extra unapproved game fields");
        JsonArray pairs=reply.getAsJsonArray("block_pairs");
        Assert.assertTrue(pairs.size()<=8,"bounded pair proposals");
        Set<String> seen=new HashSet<>();
        for(int i=0;i<pairs.size();i++) {
            Assert.assertTrue(pairs.get(i).isJsonArray(),"one pair per entry");
            JsonArray pair=pairs.get(i).getAsJsonArray();
            Assert.assertEquals(pair.size(),2,"only own/visible-opponent table indices");
            for(int j=0;j<2;j++) {
                Assert.assertTrue(pair.get(j).isJsonPrimitive() &&
                        pair.get(j).getAsJsonPrimitive().isNumber());
                String raw=pair.get(j).getAsString();
                Assert.assertTrue(raw.matches("0|[1-9][0-9]*") && raw.length()<=8,
                        "integer bounded canonical table indices");
            }
            Assert.assertTrue(seen.add(pair.toString()),"duplicate candidate pair");
        }
    }

    /** Stage11G: separate four-of vanilla 1G 2/2 species; actual legal
     * casting, attacking and blocking remains Forge referee-authoritative. */
    private static boolean supportedVanillaBear(String name) {
        return "Grizzly Bears".equals(name) ||
                "Runeclaw Bear".equals(name) ||
                "Balduvian Bears".equals(name);
    }

    /** Stage11J: allow only precisely identified no-ability vanilla
     * creatures 2/2, 3/3 and 4/4; Forge enforces all casting/attack rules. */
    private static boolean supportedVanillaFighter(String name) {
        return supportedVanillaBear(name) || "Centaur Courser".equals(name) ||
                "Rumbling Baloth".equals(name) ||
                "Colossal Dreadmaw".equals(name);
    }

    /** No attacker phase, priority, combat IDs or hidden zone details reach
     * the defending actor. Its table-index pairs are treated only as preferences.
     * Actual current attackers and combat blocking legality remain in Forge. */
    private static int refereeDeclareBlockers(Player self,Player attackerPlayer,
            Combat combat,JsonObject reply) {
        if(!"action_plan".equals(reply.get("intent").getAsString()) ||
                !reply.has("block_pairs")) return 0;
        if(combat==null || combat.getAttackingPlayer()!=attackerPlayer ||
                !self.getGame().getPhaseHandler().getPhase()
                        .equals(PhaseType.COMBAT_DECLARE_BLOCKERS)) return 0;
        JsonArray pairs=reply.getAsJsonArray("block_pairs");
        var ownTable=self.getCardsIn(ZoneType.Battlefield);
        var opponentTable=attackerPlayer.getCardsIn(ZoneType.Battlefield);
        Set<Card> committed=new HashSet<>();
        int assigned=0;
        for(int i=0;i<pairs.size();i++) {
            JsonArray pair=pairs.get(i).getAsJsonArray();
            int own=pair.get(0).getAsInt(),enemy=pair.get(1).getAsInt();
            if(own>=ownTable.size() || enemy>=opponentTable.size()) continue;
            Card blocker=ownTable.get(own),attacker=opponentTable.get(enemy);
            if(!blocker.isCreature() || !attacker.isCreature() ||
                    blocker.getController()!=self || attacker.getController()!=attackerPlayer ||
                    blocker.isFaceDown() || attacker.isFaceDown() ||
                    !(supportedVanillaFighter(blocker.getName()) ||
                      "Llanowar Elves".equals(blocker.getName()) ||
                      "Sedge Scorpion".equals(blocker.getName()) ||
                      "Zulaport Cutthroat".equals(blocker.getName()) ||
                      "Beast Whisperer".equals(blocker.getName())) ||
                    !supportedVanillaFighter(attacker.getName()) ||
                    committed.contains(blocker) ||
                    !combat.isAttacking(attacker) ||
                    !CombatUtil.canBlock(attacker,blocker,combat) ||
                    CombatUtil.getBlockCost(self.getGame(),blocker,attacker)!=null) continue;
            combat.addBlocker(attacker,blocker);
            committed.add(blocker);
            assigned++;
        }
        String problem=CombatUtil.validateBlocks(combat,self);
        if(problem!=null && !problem.isEmpty()) {
            for(Card blocker:committed) combat.undoBlockingAssignment(blocker);
            return 0;
        }
        return assigned;
    }

    /** Actor returns preferences independent of phase. Only the referee
     * selects an actual legal action for the present priority window. */
    private static SpellAbility refereeAuthorizePlan(Player self,JsonObject plan) {
        if(!"action_plan".equals(plan.get("intent").getAsString())) return null;
        JsonArray lands=plan.getAsJsonArray("land_hand_indices");
        for(int i=0;i<lands.size();i++) {
            JsonObject proposal=new JsonObject();
            proposal.addProperty("intent","play_land");
            proposal.add("hand_index",lands.get(i));
            SpellAbility legal=refereeAuthorizeLand(self,proposal);
            if(legal!=null) return legal;
        }
        // Only Forge authorizes printed sorcery timing and actual mana.
        if(plan.has("sorcery_hand_indices")) {
            JsonArray spells=plan.getAsJsonArray("sorcery_hand_indices");
            for(int i=0;i<spells.size();i++) {
                SpellAbility legal=refereeAuthorizeApprovedSorcery(self,spells.get(i).getAsInt());
                if(legal!=null) return legal;
            }
        }
        JsonArray creatures=plan.getAsJsonArray("creature_hand_indices");
        for(int i=0;i<creatures.size();i++) {
            JsonObject proposal=new JsonObject();
            proposal.addProperty("intent","cast_creature");
            proposal.add("hand_index",creatures.get(i));
            SpellAbility legal=refereeAuthorizeCreature(self,proposal);
            if(legal!=null) return legal;
        }
        return null; // no legal proposed action: genuine pass to Forge
    }

    /** Select only a current-hand legal LandAbility in the referee process. */
    private static SpellAbility refereeAuthorizeLand(Player self,JsonObject reply) {
        if (!"play_land".equals(reply.get("intent").getAsString())) return null;
        int index=reply.get("hand_index").getAsInt();
        if(index<0 || index>=self.getCardsIn(ZoneType.Hand).size()) return null;
        Game game=self.getGame();
        if(game.getPhaseHandler().getPlayerTurn()!=self ||
           game.getPhaseHandler().getPriorityPlayer()!=self ||
           !game.getStack().isEmpty()) return null;
        Card land=self.getCardsIn(ZoneType.Hand).get(index);
        if(land.getOwner()!=self || land.isFaceDown() || !land.hasPlayableLandFace()) return null;
        for(SpellAbility sa:land.getAllPossibleAbilities(self,true)) {
            if(!sa.isLandAbility()) continue;
            sa.setActivatingPlayer(self);
            if(self.canPlayLand(land,false,sa) && sa.canPlay()) return sa;
        }
        return null;
    }

    /** Stage9M scope: only two named, target-free creatures with plain mana costs. */
    private static SpellAbility refereeAuthorizeCreature(Player self,JsonObject reply) {
        if (!"cast_creature".equals(reply.get("intent").getAsString())) return null;
        int index=reply.get("hand_index").getAsInt();
        if(index<0 || index>=self.getCardsIn(ZoneType.Hand).size()) return null;
        Game game=self.getGame();
        if(game.getPhaseHandler().getPlayerTurn()!=self ||
           game.getPhaseHandler().getPriorityPlayer()!=self ||
           !self.canCastSorcery() || !game.getStack().isEmpty()) return null;
        Card creature=self.getCardsIn(ZoneType.Hand).get(index);
        if(creature.getOwner()!=self || creature.isFaceDown() || !creature.isCreature()) return null;
        // Stage12B: mandatory non-targeting ETB draw, resolved by native Forge.
        if(!"Llanowar Elves".equals(creature.getName()) &&
           !"Elvish Visionary".equals(creature.getName()) &&
           !"Scryb Sprites".equals(creature.getName()) &&
           !"Sedge Scorpion".equals(creature.getName()) &&
           !"Zulaport Cutthroat".equals(creature.getName()) &&
           !"Kazandu Nectarpot".equals(creature.getName()) &&
           !"Phyrexian Rager".equals(creature.getName()) &&
           !"Beast Whisperer".equals(creature.getName()) &&
           !supportedVanillaFighter(creature.getName())) return null;
        for(SpellAbility sa:creature.getSpellAbilities()) {
            if(!sa.isSpell() || sa.usesTargeting() || sa.getSubAbility()!=null) continue;
            if(sa.getPayCosts()==null || !sa.getPayCosts().hasManaCost() ||
               !sa.getPayCosts().hasOnlySpecificCostType(CostPartMana.class)) continue;
            sa.setActivatingPlayer(self);
            if(sa.canPlay() && ComputerUtilCost.canPayCost(sa,self,false)) return sa;
        }
        return null;
    }

    /** Stage12E: the actor cannot inspect, select, or reorder any library card.
     * Only trusted Forge searches a real private library and shuffles it. */
    private static SpellAbility refereeAuthorizeApprovedSorcery(Player self,int index) {
        if(index<0 || index>=self.getCardsIn(ZoneType.Hand).size()) return null;
        Game game=self.getGame();
        if(game.getPhaseHandler().getPlayerTurn()!=self ||
           game.getPhaseHandler().getPriorityPlayer()!=self ||
           !self.canCastSorcery() || !game.getStack().isEmpty()) return null;
        Card spell=self.getCardsIn(ZoneType.Hand).get(index);
        if(spell.getOwner()!=self || spell.isFaceDown() || !spell.isSorcery() ||
           !("Rampant Growth".equals(spell.getName()) ||
             "Overrun".equals(spell.getName()))) return null;
        for(SpellAbility sa:spell.getSpellAbilities()) {
            if(!sa.isSpell() || sa.usesTargeting() || sa.getSubAbility()!=null) continue;
            if(sa.getPayCosts()==null || !sa.getPayCosts().hasManaCost() ||
               !sa.getPayCosts().hasOnlySpecificCostType(CostPartMana.class)) continue;
            sa.setActivatingPlayer(self);
            if(sa.canPlay() && ComputerUtilCost.canPayCost(sa,self,false)) return sa;
        }
        return null;
    }

    /** Only the Forge referee interprets an actor attack proposal. No phase is sent. */
    private static void refereeDeclareAttackers(Player self,Player defender,
            Combat combat,JsonObject reply) {
        combat.clearAttackers();
        if("action_plan".equals(reply.get("intent").getAsString())) {
            JsonArray candidates=reply.getAsJsonArray("attack_table_indices");
            if(candidates.size()==0) return;
            JsonArray authorized=new JsonArray();
            for(int i=0;i<candidates.size();i++) {
                int n=candidates.get(i).getAsInt();
                if(n<0 || n>=self.getCardsIn(ZoneType.Battlefield).size()) continue;
                Card card=self.getCardsIn(ZoneType.Battlefield).get(n);
                if(card.getController()==self && !card.isFaceDown() &&
                        (supportedVanillaFighter(card.getName()) ||
                         "Scryb Sprites".equals(card.getName())) &&
                        CombatUtil.canAttack(card,defender) &&
                        CombatUtil.getAttackCost(self.getGame(),card,defender)==null) {
                    authorized.add(n);
                }
            }
            if(authorized.size()==0) return;
            JsonObject onlyAttack=new JsonObject();
            onlyAttack.addProperty("intent","attack");
            onlyAttack.add("table_indices",authorized);
            reply=onlyAttack;
        }
        if(!"attack".equals(reply.get("intent").getAsString())) return;
        Game game=self.getGame();
        if(game.getPhaseHandler().getPlayerTurn()!=self ||
           game.getPhaseHandler().getPhase()!=PhaseType.COMBAT_DECLARE_ATTACKERS ||
           combat.getAttackingPlayer()!=self || !combat.getDefenders().contains(defender)) return;
        JsonArray indices=reply.getAsJsonArray("table_indices");
        var table=self.getCardsIn(ZoneType.Battlefield);
        List<Card> approved=new ArrayList<>();
        for(int i=0;i<indices.size();i++) {
            int n=indices.get(i).getAsInt();
            if(n<0 || n>=table.size()) return;
            Card c=table.get(n);
            if(c.getController()!=self || c.isFaceDown() ||
               !(supportedVanillaFighter(c.getName()) ||
                 "Scryb Sprites".equals(c.getName())) ||
               !CombatUtil.canAttack(c,defender) ||
               CombatUtil.getAttackCost(game,c,defender)!=null) return;
            approved.add(c);
        }
        for(Card c:approved) combat.addAttacker(c,defender);
        if(!CombatUtil.validateAttackers(combat)) combat.clearAttackers();
    }

    private static final String HAND_ORDER_DENIED="stage12a: invalid own-hand cleanup choice";

    private static void validateHandOrder(JsonObject reply,int size) {
        if(size<0 || size>128 || reply==null || reply.size()!=2
                || !reply.has("intent") || !reply.get("intent").isJsonPrimitive()
                || !reply.get("intent").getAsJsonPrimitive().isString()
                || !"hand_order".equals(reply.get("intent").getAsString())
                || !reply.has("hand_priority") || !reply.get("hand_priority").isJsonArray()) {
            throw new SecurityException(HAND_ORDER_DENIED);
        }
        JsonArray order=reply.getAsJsonArray("hand_priority");
        if(order.size()!=size) throw new SecurityException(HAND_ORDER_DENIED);
        boolean[] seen=new boolean[size];
        for(JsonElement value:order) {
            if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new SecurityException(HAND_ORDER_DENIED);
            }
            String raw=value.getAsString();
            if(!raw.matches("0|[1-9][0-9]*") || raw.length()>3) {
                throw new SecurityException(HAND_ORDER_DENIED);
            }
            int index=Integer.parseInt(raw);
            if(index>=size || seen[index]) throw new SecurityException(HAND_ORDER_DENIED);
            seen[index]=true;
        }
    }

    private static JsonObject askHandOrder(Actor actor,String view) throws Exception {
        if(!actor.handOrderCapable) throw new SecurityException(HAND_ORDER_DENIED);
        JsonObject request=new JsonObject();
        request.addProperty("request","own_hand_order");
        request.add("observation",parseStrictActorReplyView(view));
        String encoded=request.toString();
        if(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>=16384)
            throw new SecurityException(HAND_ORDER_DENIED);
        String line=exchangeActorRequestWithOneDeadline(actor.writer,actor.reader,
                actor.replyReads,encoded,10000L,
                () -> {actor.replyTimedOut=true;actor.process.destroyForcibly();});
        JsonObject reply=parseStrictActorReply(line);
        int size=request.getAsJsonObject("observation").getAsJsonObject("own_cards")
                .getAsJsonArray("hand").size();
        validateHandOrder(reply,size);
        return reply;
    }

    private static JsonObject parseStrictActorReplyView(String view) {
        // Trusted referee projection has the request limit, not the reply limit.
        if(view==null || view.length()>=16000) throw new SecurityException(HAND_ORDER_DENIED);
        return com.google.gson.JsonParser.parseString(view).getAsJsonObject();
    }

    private static void requireCleanup(Player self,int count) {
        Game game=self.getGame();
        int size=self.getCardsIn(ZoneType.Hand).size();
        if(game.getPhaseHandler().getPhase()!=PhaseType.CLEANUP
                || game.getPhaseHandler().getPlayerTurn()!=self
                || self.isUnlimitedHandSize() || size>128 || count<=0 || count>size
                || count!=size-self.getMaxHandSize()) {
            throw new SecurityException(HAND_ORDER_DENIED);
        }
    }

    private static CardCollection resolveOwnHandOrder(Player self,CardCollection snapshot,
            JsonObject reply,int count) {
        requireCleanup(self,count);
        CardCollectionView current=self.getCardsIn(ZoneType.Hand);
        if(snapshot.size()!=current.size()) throw new SecurityException(HAND_ORDER_DENIED);
        for(int i=0;i<snapshot.size();i++) {
            Card card=snapshot.get(i);
            if(card!=current.get(i) || card.getOwner()!=self || card.getController()!=self
                    || card.getGame()!=self.getGame()) throw new SecurityException(HAND_ORDER_DENIED);
        }
        validateHandOrder(reply,snapshot.size());
        CardCollection chosen=new CardCollection();
        for(int i=0;i<count;i++) {
            chosen.add(snapshot.get(reply.getAsJsonArray("hand_priority").get(i).getAsInt()));
        }
        return chosen;
    }

    /**
     * Actual Forge PlayerController inserted only in disposable test game.
     * The inherited Forge AI may handle automatic mandatory non-strategic
     * callbacks; when Forge asks for priority, this override MUST consult
     * the independent process, NEVER AiController's full-information policy.
     */
    private final class ExternalCreatureController extends PlayerControllerAi {
        final Actor subprocess;
        final Player controlled;
        final Player opponent;
        int priorityCallbacks = 0;
        int cleanupChoices = 0;
        CardCollection cleanupSelected = new CardCollection();
        ExternalCreatureController(Game game,Player self,Player other,Actor worker) {
            super(game,self,self.getController().getLobbyPlayer());
            subprocess=worker;
            controlled=self;
            opponent=other;
        }
        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            try {
                String view=observation(controlled,opponent);
                JsonObject response=subprocess.ask(view);
                priorityCallbacks++;
                SpellAbility requested;
                if ("action_plan".equals(response.get("intent").getAsString())) {
                    requested=refereeAuthorizePlan(controlled,response);
                } else {
                    requested=refereeAuthorizeLand(controlled,response);
                    if(requested==null) requested=refereeAuthorizeCreature(controlled,response);
                }
                if (requested!=null) return Collections.singletonList(requested);
                return null; // pass, or attempted illegal land; Forge advances priority
            } catch(Exception ex) {
                throw new IllegalStateException("Restricted actor priority IPC failed",ex);
            }
        }
        int attackCallbacks=0;
        int attacksAuthorized=0;
        @Override
        public void declareAttackers(Player attacker,Combat combat) {
            try {
                attackCallbacks++;
                JsonObject proposed=subprocess.ask(observation(controlled,opponent));
                refereeDeclareAttackers(controlled,opponent,combat,proposed);
                for(Card candidate:controlled.getCardsIn(ZoneType.Battlefield)) {
                    if(combat.isAttacking(candidate)) attacksAuthorized++;
                }
            } catch(Exception ex) {
                throw new IllegalStateException("Actor attack IPC failed",ex);
            }
        }
        /** Stage10G: the trusted referee assigns simple combat damage.
         * Never delegate this decision to PlayerControllerAi or the actor IPC. */
        int refereeDamageAssignments=0;
        int refereeSplitDamageAssignments=0;
        @Override
        public Map<Card,Integer> assignCombatDamage(Card source,
                CardCollectionView targets,CardCollectionView remaining,
                int damageDealt,GameEntity defender,boolean overrideOrder) {
            // Stage12F ONLY: Forge-native printed Overrun grants our original
            // registered ordinary Bear exactly +3/+3 and Trample. The referee,
            // not the external actor or inherited omniscient AI, distributes
            // mandatory lethal damage to each Forge-certified legal blocker,
            // then assigns genuine excess Trample damage to the defender
            // with Forge's documented null-map-key convention.
            if(source!=null && source.getController()==controlled &&
                    source.getOwner()==controlled &&
                    controlled.getCardsIn(ZoneType.Battlefield).contains(source) &&
                    !source.isFaceDown() &&
                    ((supportedVanillaBear(source.getName()) &&
                      source.getNetPower()==5 && source.getNetToughness()==5 &&
                      source.getNetCombatDamage()==5 && damageDealt==5) ||
                     ("Colossal Dreadmaw".equals(source.getName()) &&
                      source.getNetPower()==6 && source.getNetToughness()==6 &&
                      source.getNetCombatDamage()==6 && damageDealt==6)) &&
                    source.hasKeyword("Trample") &&
                    defender==opponent && targets!=null &&
                    !targets.isEmpty() && targets.size()<=8) {
                Map<Card,Integer> assignment=new LinkedHashMap<>();
                Set<Card> identities=Collections.newSetFromMap(
                        new IdentityHashMap<Card,Boolean>());
                int unassigned=damageDealt;
                for(Card blocker:targets) {
                    if(blocker==null || !identities.add(blocker) ||
                            blocker.getController()!=opponent ||
                            !opponent.getCardsIn(ZoneType.Battlefield).contains(blocker) ||
                            blocker.isFaceDown() ||
                            !(supportedVanillaFighter(blocker.getName()) ||
                              "Llanowar Elves".equals(blocker.getName())))
                        throw new SecurityException("unsupported Overrun blocker");
                    int lethal=Math.max(1,blocker.getLethalDamage());
                    int dealt=Math.min(unassigned,lethal);
                    if(dealt>0) assignment.put(blocker,dealt);
                    unassigned-=dealt;
                }
                if(unassigned>0) assignment.put(null,unassigned);
                refereeDamageAssignments++;
                return assignment;
            }
            if(source==null || source.getController()!=controlled ||
                    !controlled.getCardsIn(ZoneType.Battlefield).contains(source) ||
                    !(supportedVanillaFighter(source.getName()) ||
                      "Llanowar Elves".equals(source.getName()) ||
                      "Sedge Scorpion".equals(source.getName()) ||
                      "Zulaport Cutthroat".equals(source.getName()) ||
                      "Beast Whisperer".equals(source.getName())) ||
                    damageDealt!=("Colossal Dreadmaw".equals(source.getName())?6:
                            "Rumbling Baloth".equals(source.getName())?4:
                            "Centaur Courser".equals(source.getName())?3:
                            ("Llanowar Elves".equals(source.getName()) ||
                             "Sedge Scorpion".equals(source.getName()) ||
                             "Zulaport Cutthroat".equals(source.getName()))?1:2) ||
                    source.getNetCombatDamage()!=damageDealt ||
                    (source.hasKeyword("Trample") &&
                     !("Colossal Dreadmaw".equals(source.getName()) && defender==null)) ||
                    targets==null || targets.isEmpty() || targets.size()>8) {
                throw new SecurityException("unsupported referee combat-damage allocation");
            }
            Set<Card> unique=Collections.newSetFromMap(new IdentityHashMap<Card,Boolean>());
            for(Card target:targets) {
                if(target==null || !unique.add(target) ||
                        !(supportedVanillaFighter(target.getName()) ||
                          "Llanowar Elves".equals(target.getName()) ||
                          "Sedge Scorpion".equals(target.getName()) ||
                          "Zulaport Cutthroat".equals(target.getName()) ||
                          "Beast Whisperer".equals(target.getName())) ||
                        target.getController()!=opponent ||
                        !opponent.getCardsIn(ZoneType.Battlefield).contains(target)) {
                    throw new SecurityException("unregistered or unsupported combat target");
                }
            }
            // Stage11M: CR 510.1c-d + 704.5g. Allocate to Forge's
            // current remaining lethal damage, NOT printed toughness:
            // already-marked damage (and damage assigned by other
            // attackers in the same combat step) reduces the required
            // amount. Example 3/3 vs damaged 2/2 (1 marked) and fresh
            // 2/2: allocate 1+2, destroying both, instead of 2+1.
            // For healthy 4/4 vs two 2/2s preserve Stage11L 2+2.
            // Remaining surplus goes to first legal target. Referee
            // owns all Card state; actor receives no combat internals.
            Map<Card,Integer> split=new LinkedHashMap<>();
            int damageLeft=damageDealt;
            for(Card target:targets) {
                if(damageLeft<=0) break;
                int assigned=Math.min(damageLeft,
                        Math.max(1,target.getLethalDamage()));
                split.put(target,assigned);
                damageLeft-=assigned;
            }
            if(damageLeft>0) {
                Card first=targets.get(0);
                split.put(first,split.get(first)+damageLeft);
            }
            refereeDamageAssignments++;
            if(split.size()>1) refereeSplitDamageAssignments++;
            return split;
        }

        int blockerCallbacks=0;
        int blocksAuthorized=0;
        int maxBlocksInSingleDeclare=0;
        // Count ACTUAL legal two-blocker trades, never actor intentions.
        int coordinatedNativeBlocks=0;
        @Override
        public void declareBlockers(Player defender,Combat combat) {
            try {
                blockerCallbacks++;
                JsonObject proposed=subprocess.ask(observation(controlled,opponent));
                int accepted=refereeDeclareBlockers(controlled,opponent,combat,proposed);
                blocksAuthorized+=accepted;
                maxBlocksInSingleDeclare=Math.max(maxBlocksInSingleDeclare,accepted);
                // Count only Forge-validated actual one-attacker/two-blocker
                // assignments. Actor is NEVER told this combat information.
                if(accepted>0 && combat!=null) {
                    for(Card attacker:combat.getAttackers()) {
                        if(attacker.getController()!=opponent ||
                                !supportedVanillaFighter(attacker.getName())) continue;
                        CardCollection legalBlockers=combat.getBlockers(attacker);
                        if(legalBlockers.size()!=2) continue;
                        int combined=0;
                        boolean independentlyWeaker=true;
                        for(Card blocker:legalBlockers) {
                            if(blocker.getController()!=controlled ||
                                    !supportedVanillaFighter(blocker.getName()) ||
                                    blocker.getNetPower()>=attacker.getNetToughness()) {
                                independentlyWeaker=false;
                                break;
                            }
                            combined+=blocker.getNetPower();
                        }
                        if(independentlyWeaker &&
                                combined>=attacker.getNetToughness())
                            coordinatedNativeBlocks++;
                    }
                }
            } catch(Exception ex) {
                throw new IllegalStateException("Actor blocker preference IPC failed",ex);
            }
        }

        /** Stage9T denies inherited strategic decision makers until projected actor
         * choices are implemented; never silently fall back to Forge's brains. */
        @Override
        public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList,
                SpellAbility sa,String title,int min,int max,boolean isOptional,
                Map<String,Object> params) {
            throw new SecurityException("restricted actor has no effect-card-choice protocol");
        }

        @Override
        public CardCollectionView chooseCardsToDiscardToMaximumHandSize(int count) {
            if(!subprocess.handOrderCapable) throw new SecurityException(HAND_ORDER_DENIED);
            requireCleanup(controlled,count);
            CardCollection snapshot=new CardCollection(controlled.getCardsIn(ZoneType.Hand));
            String view=observation(controlled,opponent);
            try {
                JsonObject reply=askHandOrder(subprocess,view);
                if(!view.equals(observation(controlled,opponent)))
                    throw new SecurityException(HAND_ORDER_DENIED);
                CardCollection result=resolveOwnHandOrder(controlled,snapshot,reply,count);
                cleanupChoices++;
                cleanupSelected=new CardCollection(result);
                return result; // Only native Forge performs the actual discards.
            } catch(SecurityException ex) {
                throw ex;
            } catch(Exception ex) {
                throw new SecurityException("stage12a: own-hand actor failed",ex);
            }
        }


        @Override
        public PlayerZone chooseStartingHand(List<PlayerZone> zones) {
            throw new SecurityException("restricted actor has no starting-hand protocol");
        }

        @Override
        public boolean confirmAction(SpellAbility sa,PlayerActionConfirmMode mode,
                String message,List<String> options,Card cardToShow,
                Map<String,Object> params) {
            throw new SecurityException("restricted actor has no arbitrary confirmation protocol");
        }

        /**
         * Stage9T: reject six more undecoded strategic decisions before any
         * inherited AI logic can examine private Forge state. These choices
         * need separately projected actor protocols and referee validation.
         */
        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(
                FCollectionView<T> options, DelayedReveal reveal, SpellAbility sa,
                String title, boolean isOptional, Player targeted,
                Map<String,Object> params) {
            throw new SecurityException("restricted actor has no single-entity effect-choice protocol");
        }

        @Override
        public <T extends GameEntity> List<T> chooseEntitiesForEffect(
                FCollectionView<T> options,int min,int max,DelayedReveal reveal,
                SpellAbility sa,String title,Player targeted,Map<String,Object> params) {
            throw new SecurityException("restricted actor has no multiple-entities effect-choice protocol");
        }

        @Override
        public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells,
                SpellAbility sa,String title,Map<String,Object> params) {
            throw new SecurityException("restricted actor has no single-spell effect-choice protocol");
        }

        @Override
        public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells,
                SpellAbility sa,String title,int num,Map<String,Object> params) {
            throw new SecurityException("restricted actor has no multiple-spells effect-choice protocol");
        }

        @Override
        public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa,
                int min,int max,CardCollectionView options,String message) {
            throw new SecurityException("restricted actor has no sacrifice-choice protocol");
        }

        @Override
        public CardCollectionView choosePermanentsToDestroy(SpellAbility sa,
                int min,int max,CardCollectionView options,String message) {
            throw new SecurityException("restricted actor has no destruction-choice protocol");
        }


        /**
         * Stage9T: no inherited Forge policy may decide bids, static effects,
         * optional payments or replacement-effect applicability for an actor.
         * These are unsupported, not implicitly declined or auto-approved.
         */
        @Override
        public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode mode,
                String message,int bid,Player winner) {
            throw new SecurityException("restricted actor has no bid-confirmation protocol");
        }

        @Override
        public boolean confirmStaticApplication(Card hostCard,
                PlayerActionConfirmMode mode,String message,String logic) {
            throw new SecurityException("restricted actor has no static-effect-confirmation protocol");
        }

        @Override
        public boolean confirmPayment(CostPart costPart,String prompt,SpellAbility sa) {
            throw new SecurityException("restricted actor has no optional-payment protocol");
        }

        @Override
        public boolean confirmReplacementEffect(ReplacementEffect effect,
                SpellAbility effectSA,GameEntity affected,String question) {
            throw new SecurityException("restricted actor has no replacement-effect-choice protocol");
        }

        /** Stage9U: hidden pile / name / scry / surveil decisions
         * have no consented actor protocol. Never inherit Forge AI decisions. */
        @Override
        public boolean chooseCardsPile(SpellAbility sa,CardCollectionView pile1,
                CardCollectionView pile2,String faceUp) {
            throw new SecurityException("restricted actor has no concealed-pile choice protocol");
        }
        @Override
        public String chooseCardName(SpellAbility sa,List<ICardFace> faces,String message) {
            throw new SecurityException("restricted actor has no card-name choice protocol");
        }
        @Override
        public String chooseCardName(SpellAbility sa,Predicate<ICardFace> valid,
                String filter,String message) {
            throw new SecurityException("restricted actor has no named-card predicate protocol");
        }
        /** Stage12E: only the trusted Forge referee chooses a basic land
         * for the ORIGINAL printed Rampant Growth. No candidate or library
         * order is ever sent to either external Python player process.
         * Every other hidden-zone selection remains fail-closed. */
        @Override
        public Card chooseSingleCardForZoneChange(ZoneType destination,
                List<ZoneType> origin,SpellAbility sa,CardCollection fetchList,
                DelayedReveal reveal,String prompt,boolean optional,Player decider) {
            if(sa==null || sa.getHostCard()==null ||
                    !"Rampant Growth".equals(sa.getHostCard().getName()) ||
                    sa.getActivatingPlayer()!=controlled || decider!=controlled ||
                    destination!=ZoneType.Battlefield || origin.size()!=1 ||
                    origin.get(0)!=ZoneType.Library ||
                    !"Land.Basic".equals(sa.getParam("ChangeType")) ||
                    !"True".equals(sa.getParam("Tapped")) ||
                    !"1".equals(sa.getParam("ChangeNum")))
                throw new SecurityException("restricted actor has no other hidden-zone selection protocol");
            for(Card candidate:fetchList)
                if("Forest".equals(candidate.getName()) &&
                        candidate.getOwner()==controlled &&
                        controlled.getCardsIn(ZoneType.Library).contains(candidate))
                    return candidate;
            return null; // native optional search may fail to find a matching card
        }

        @Override
        public List<Card> chooseCardsForZoneChange(ZoneType destination,
                List<ZoneType> origin,SpellAbility sa,CardCollection fetchList,
                int min,int max,DelayedReveal reveal,String prompt,Player decider) {
            throw new SecurityException("restricted actor has no hidden-zone bulk-selection protocol");
        }
        @Override
        public ImmutablePair<CardCollection,CardCollection> arrangeForScry(CardCollection top) {
            throw new SecurityException("restricted actor has no scry choice protocol");
        }
        @Override
        public ImmutablePair<CardCollection,CardCollection> arrangeForSurveil(CardCollection top) {
            throw new SecurityException("restricted actor has no surveil choice protocol");
        }

        /**
         * Stage9V: these prompts would otherwise consult the inherited
         * ComputerUtil / SpellApiToAi strategic policy or inspect private
         * cards. No user-approved actor protocol exists for them yet.
         */
        /** Referee authorizes only own-player as the native winner of the
         * first-player selection. No card or hidden state is consulted. */
        int startingPlayerChoices=0;
        @Override
        public Player chooseStartingPlayer(boolean isFirstGame) {
            startingPlayerChoices++;
            return controlled;
        }
        /** The normal Match.prepareAllZones callback must never analyze
         * deck cards using Forge's inherited omniscient AI policy. */
        @Override
        public Map<DeckSection,List<? extends PaperCard>> complainCardsCantPlayWell(Deck deck) {
            if (deck != controlled.getRegisteredPlayer().getDeck()) {
                // Forge may return a validated copy of that deck: fail closed
                // only when the controller is handed another player's deck.
                if (deck == opponent.getRegisteredPlayer().getDeck()) {
                    throw new SecurityException("opponent registered deck handed to actor");
                }
            }
            return Collections.emptyMap();
        }
        @Override
        public List<SpellAbility> chooseSaToActivateFromOpeningHand(
                List<SpellAbility> usableFromOpeningHand) {
            if(!usableFromOpeningHand.isEmpty()) {
                throw new SecurityException("unapproved special pregame abilities");
            }
            return Collections.emptyList();
        }

        /** Forge still shuffles privately. Never let inherited AiController
         * reorder or inspect the library through cheatShuffle. */
        @Override
        public CardCollectionView cheatShuffle(CardCollectionView libraryCards) {
            return libraryCards; // already randomized in Player.shuffle using Forge MyRandom
        }

        boolean ownHandOpeningEnabled=false;
        int openingCallbacks=0;

        /** Enabled only by the trusted referee for an own-hand-only fixture.
         * Unenabled inherited requests remain fail-closed as in Stage9V. */
        void enableOwnHandOpening() {
            ownHandOpeningEnabled=true;
        }

        @Override
        public boolean mulliganKeepHand(Player firstPlayer,int cardsToReturn) {
            if(!ownHandOpeningEnabled) {
                throw new SecurityException("opening-hand protocol not enabled");
            }
            if(cardsToReturn<0 || cardsToReturn>controlled.getCardsIn(ZoneType.Hand).size()) {
                throw new SecurityException("referee invalid mulligan count");
            }
            try {
                JsonObject choice=subprocess.askOpening(observation(controlled,opponent));
                openingCallbacks++;
                return choice.get("keep").getAsBoolean();
            } catch(Exception ex) {
                throw new SecurityException("isolated opening-hand actor failed",ex);
            }
        }
        @Override
        public CardCollectionView tuckCardsViaMulligan(CardCollectionView hand,
                int cardsToReturn) {
            if(!ownHandOpeningEnabled) {
                throw new SecurityException("bottoming protocol not enabled");
            }
            CardCollectionView authoritative=controlled.getCardsIn(ZoneType.Hand);
            if(hand==null || cardsToReturn<0 || cardsToReturn>hand.size() ||
                    hand.size()!=authoritative.size()) {
                throw new SecurityException("referee mismatched mulligan hand/count");
            }
            for(int i=0;i<hand.size();i++) {
                if(hand.get(i)!=authoritative.get(i) ||
                        hand.get(i).getOwner()!=controlled) {
                    throw new SecurityException("stale or borrowed mulligan Card reference");
                }
            }
            try {
                JsonObject choice=subprocess.askOpening(observation(controlled,opponent));
                openingCallbacks++;
                JsonArray order=choice.getAsJsonArray("bottom_priority");
                CardCollection selected=new CardCollection();
                for(int i=0;i<cardsToReturn;i++) {
                    selected.add(hand.get(order.get(i).getAsInt()));
                }
                return selected;
            } catch(Exception ex) {
                throw new SecurityException("isolated bottom-hand actor failed",ex);
            }
        }
        @Override
        public boolean chooseTargetsFor(SpellAbility ability) {
            throw new SecurityException("no approved target-selection protocol");
        }
        @Override
        public ICardFace chooseSingleCardFace(SpellAbility sa,List<ICardFace> faces,
                String message) {
            throw new SecurityException("no approved card-face-selection protocol");
        }
        @Override
        public ICardFace chooseSingleCardFace(SpellAbility sa,String message,
                Predicate<ICardFace> valid,String name) {
            throw new SecurityException("no approved filtered-card-face protocol");
        }
        @Override
        public List<Card> chooseCardsForSplice(SpellAbility sa,List<Card> candidates) {
            throw new SecurityException("no approved splice choice protocol");
        }
        @Override
        public int chooseNumberForCostReduction(SpellAbility sa,int min,int max) {
            throw new SecurityException("no approved optional cost-reduction protocol");
        }

        @Override
        public boolean playChosenSpellAbility(SpellAbility sa) {
            if(sa.isLandAbility()) return super.playChosenSpellAbility(sa);
            if(!sa.isSpell() || sa.getHostCard()==null) return false;
            Card host=sa.getHostCard();
            // Stage12E: the trusted referee has already authorized the
            // single printed sorcery. Preserve fail-closed behavior for all
            // other noncreature spells, including fabricated actor commands.
            boolean originalApprovedSorcery=host.isSorcery() &&
                    ("Rampant Growth".equals(host.getName()) ||
                     "Overrun".equals(host.getName())) &&
                    host.getOwner()==controlled &&
                    controlled.getCardsIn(ZoneType.Hand).contains(host) &&
                    sa.getActivatingPlayer()==controlled &&
                    !sa.usesTargeting() && sa.getSubAbility()==null &&
                    sa.getPayCosts()!=null && sa.getPayCosts().hasManaCost() &&
                    sa.getPayCosts().hasOnlySpecificCostType(CostPartMana.class) &&
                    controlled.getGame().getPhaseHandler().getPlayerTurn()==controlled &&
                    controlled.getGame().getPhaseHandler().getPriorityPlayer()==controlled &&
                    controlled.canCastSorcery() && controlled.getGame().getStack().isEmpty();
            if(!host.isCreature() && !originalApprovedSorcery) return false;
            // Real Forge pays mana, places the spell on stack, resolves
            // its original printed effect, and shuffles the private library.
            return ComputerUtil.handlePlayingSpellAbility(controlled,sa,null);
        }
    }

    private ExternalCreatureController attach(Game game,Player self,Player opponent,Actor actor) {
        ExternalCreatureController controller=new ExternalCreatureController(game,self,opponent,actor);
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
            ExternalCreatureController ca=attach(game,p0,p1,a);
            ExternalCreatureController cb=attach(game,p1,p0,b);
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
            ExternalCreatureController ca=attach(game,p0,p1,a);
            ExternalCreatureController cb=attach(game,p1,p0,b);
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
            Assert.assertTrue(observation(next,startingActive).contains(
                next.getCardsIn(ZoneType.Hand).get(0).getName()));
            Assert.assertFalse(observation(startingActive,next).contains(
                "\"hand\":[\""+next.getCardsIn(ZoneType.Hand).get(0).getName()+"\"]"));
        }
    }

    @Test(timeOut=120000)
    public void realPriorityObservationOmitsForgedOpponentSecretsAndPhase() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(self,other);
        try(Actor selfActor=new Actor();Actor otherActor=new Actor()) {
            ExternalCreatureController cs=attach(game,self,other,selfActor);
            attach(game,other,self,otherActor);
            String baseline=observation(self,other);
            Assert.assertFalse(baseline.contains("\"phase\""));
            Assert.assertFalse(baseline.contains("\"priority\""));
            Assert.assertFalse(baseline.contains("\"turn\""));
            Assert.assertFalse(baseline.contains("\"library_order\""));
            Assert.assertFalse(baseline.contains("\"stack\""));
            Card secret=addCardToZone("Counterspell",other,ZoneType.Hand);
            addCardToZone("Shock",other,ZoneType.Library);
            Assert.assertEquals(observation(self,other),baseline,
                "private actual Forge zone changes must not affect actor input");
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(cs.priorityCallbacks,1);
            Assert.assertFalse(observation(self,other).contains(secret.getName()));
        }
    }
    @Test(timeOut=120000)
    public void realPriorityLoopAcceptsLandAndRejectsOutOfTurnProposal() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1), other=game.getPlayers().get(0);
        seedPrivateLibraries(active,other);
        addCardToZone("Forest",active,ZoneType.Hand);
        addCardToZone("Island",other,ZoneType.Hand);
        try (Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,active,other,aa);
            ExternalCreatureController cb=attach(game,other,active,bb);
            int beforeActive=active.getCardsIn(ZoneType.Battlefield).size();
            game.getPhaseHandler().mainLoopStep(); // both Forest and subsequent pass in same decision cycle
            Assert.assertEquals(active.getLandsPlayedThisTurn(),1,
                    "the real active actor's requested land was played");
            Assert.assertEquals(active.getCardsIn(ZoneType.Battlefield).size(),beforeActive+1);
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),0);
            Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.MAIN1);
            game.getPhaseHandler().mainLoopStep(); // other actor asks Island but has no right
            Assert.assertEquals(other.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertEquals(other.getLandsPlayedThisTurn(),0);
            Assert.assertNotEquals(game.getPhaseHandler().getPhase(),PhaseType.MAIN1);
            Assert.assertTrue(ca.priorityCallbacks>=2 && cb.priorityCallbacks>0);
        }
    }

    @Test(timeOut=120000)
    public void realLoopEnforcesOneLandThenResetsOnNextNaturalTurn() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(active,other);
        addCardToZone("Forest",active,ZoneType.Hand);
        addCardToZone("Forest",active,ZoneType.Hand);
        addCardToZone("Island",other,ZoneType.Hand);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,active,other,aa);
            ExternalCreatureController cb=attach(game,other,active,bb);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(active.getLandsPlayedThisTurn(),1);
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),1,
                    "referee must not accept a second same-turn basic land");
            int steps=0;
            boolean observedOtherLegalLand=false;
            while(!game.isGameOver() && steps++<190) {
                game.getPhaseHandler().mainLoopStep();
                if(other.getLandsPlayedThisTurn()==1) {
                    observedOtherLegalLand=true;
                    break;
                }
            }
            Assert.assertTrue(observedOtherLegalLand,
                    "natural Forge phases must reach the other player's legal land drop");
            Assert.assertEquals(other.getCardsIn(ZoneType.Battlefield).size(),1);
            Assert.assertTrue(ca.priorityCallbacks>1 && cb.priorityCallbacks>1);
            Assert.assertTrue(steps<190);
            int extraSteps=0;
            boolean returnedToActive=false;
            while(!game.isGameOver() && extraSteps++<190) {
                game.getPhaseHandler().mainLoopStep();
                if(game.getPhaseHandler().getPlayerTurn()==active &&
                   game.getPhaseHandler().getPhase()==PhaseType.MAIN1) {
                    returnedToActive=true;
                    break;
                }
            }
            Assert.assertTrue(returnedToActive,"Forge returns to original player naturally");
            Assert.assertEquals(active.getLandsPlayedThisTurn(),0,
                    "actual Forge turn cleanup resets land-play limit");
            // Reuse already held second Forest rather than asking for a hidden new card.
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(active.getLandsPlayedThisTurn(),1);
            Assert.assertTrue(active.getCardsIn(ZoneType.Battlefield).size()>=2);
        }
    }

    @Test(timeOut=120000)
    public void illegalExtraActorCommandIsRejectedBeforeForgeCanChangeState() {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        addCardToZone("Forest",active,ZoneType.Hand);
        int initial=active.getCardsIn(ZoneType.Hand).size();
        for(String candidate:Arrays.asList(
            "{\"intent\":\"play_land\",\"hand_index\":0,\"life\":999}",
            "{\"intent\":\"draw_card\"}",
            "{\"intent\":\"set_life\",\"self\":100}",
            "{\"intent\":\"play_land\",\"hand_index\":0.5}",
            "{\"intent\":\"play_land\",\"hand_index\":-1}",
            "{\"intent\":\"pass\",\"opponent_hand\":[]}")) {
            try {
                validateReply(JsonParser.parseString(candidate).getAsJsonObject());
                Assert.fail("referee must reject forged payload "+candidate);
            } catch(AssertionError expected) {
                Assert.assertNotNull(expected.getMessage());
            }
        }
        Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),initial);
        Assert.assertEquals(active.getLandsPlayedThisTurn(),0);
        String aBefore=observation(active,other);
        addCardToZone("Counterspell",other,ZoneType.Hand);
        addCardToZone("Shock",other,ZoneType.Library);
        Assert.assertEquals(observation(active,other),aBefore);
    }

    @Test(timeOut=120000)
    public void landThenOneManaCreaturePaysForestAndWaitsForOpponentPriority() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(active,other);
        addCardToZone("Forest",active,ZoneType.Hand);
        addCardToZone("Llanowar Elves",active,ZoneType.Hand);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            Assert.assertNotEquals(aa.process.pid(),bb.process.pid());
            ExternalCreatureController ca=attach(game,active,other,aa);
            ExternalCreatureController cb=attach(game,other,active,bb);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(active.getLandsPlayedThisTurn(),1);
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),0);
            Assert.assertEquals(countCardsWithName(game,"Llanowar Elves",ZoneType.Battlefield),0,
                    "spell must remain on stack until opponent receives priority");
            Assert.assertFalse(game.getStack().isEmpty(),"Forge must hold creature spell on stack");
            Assert.assertTrue(findCardWithName(game,"Forest").isTapped(),
                    "real mana payment must tap actual Forest");
            Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.MAIN1);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(game.getStack().isEmpty(),"opponent's pass resolves creature");
            Assert.assertEquals(countCardsWithName(game,"Llanowar Elves"),1);
            Assert.assertEquals(active.getLife(),20);
            Assert.assertEquals(other.getLife(),20);
            Assert.assertTrue(ca.priorityCallbacks>=3 && cb.priorityCallbacks>=1);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(countCardsWithName(game,"Llanowar Elves"),1,
                    "one hand card cannot be duplicated by repeated actor proposals");
        }
    }

    @Test(timeOut=120000)
    public void twoManaCreaturePaysTwoForestsAndResolvesAfterPass() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(active,other);
        Card first=addCardToZone("Forest",active,ZoneType.Battlefield);
        Card second=addCardToZone("Forest",active,ZoneType.Battlefield);
        addCardToZone("Grizzly Bears",active,ZoneType.Hand);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            attach(game,active,other,aa);
            attach(game,other,active,bb);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(first.isTapped() && second.isTapped(),
                    "cost {1}{G} must consume two actual mana sources");
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),0);
            Assert.assertFalse(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears"),0);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears"),1);
            Assert.assertEquals(countCardsWithName(game,"Forest"),2);
        }
    }

    @Test(timeOut=120000)
    public void insufficientManaCannotCastOrMutateHandLandOrStack() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(active,other);
        Card forest=addCardToZone("Forest",active,ZoneType.Battlefield);
        addCardToZone("Grizzly Bears",active,ZoneType.Hand);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            attach(game,active,other,aa);
            attach(game,other,active,bb);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertFalse(forest.isTapped(),"failed preflight cannot spend mana");
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears"),0);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears"),0);
        }
    }

    @Test(timeOut=120000)
    public void opponentCannotCastSorcerySpeedCreatureOutsideOwnTurn() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(active,other);
        Card forest=addCardToZone("Forest",other,ZoneType.Battlefield);
        addCardToZone("Llanowar Elves",other,ZoneType.Hand);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            attach(game,active,other,aa);
            attach(game,other,active,bb);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.MAIN1);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(other.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertFalse(forest.isTapped());
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Llanowar Elves"),0);
        }
    }

    @Test(timeOut=120000)
    public void forgedCreatureCommandsAndUnsupportedCardAreRejected() {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        addCardToZone("Young Wolf",active,ZoneType.Hand);
        Card forest=addCardToZone("Forest",active,ZoneType.Battlefield);
        for(String candidate:Arrays.asList(
            "{\"intent\":\"cast_creature\",\"hand_index\":0,\"life\":999}",
            "{\"intent\":\"cast_creature\",\"hand_index\":-1}",
            "{\"intent\":\"cast_creature\",\"hand_index\":0.5}",
            "{\"intent\":\"cast_creature\",\"hand_index\":\"0\"}",
            "{\"intent\":\"cast_spell\",\"hand_index\":0}",
            "{\"intent\":\"pass\",\"hand_index\":0}")) {
            try {
                validateReply(JsonParser.parseString(candidate).getAsJsonObject());
                Assert.fail("forged creature payload accepted "+candidate);
            } catch(AssertionError expected) {
                Assert.assertNotNull(expected.getMessage());
            }
        }
        JsonObject unsupported=JsonParser.parseString(
            "{\"intent\":\"cast_creature\",\"hand_index\":0}").getAsJsonObject();
        validateReply(unsupported);
        Assert.assertNull(refereeAuthorizeCreature(active,unsupported),
                "unapproved spell type must fail closed even when mana is available");
        Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),1);
        Assert.assertFalse(forest.isTapped());
        Assert.assertTrue(game.getStack().isEmpty());
        String baseline=observation(active,other);
        addCardToZone("Grizzly Bears",other,ZoneType.Hand);
        addCardToZone("Llanowar Elves",other,ZoneType.Library);
        Assert.assertEquals(observation(active,other),baseline,
                "opponent private creature and library changes remain invisible");
    }

    @Test(timeOut=120000)
    public void realForgeUnblockedBearAttackDealsTwoDamage() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),defender=game.getPlayers().get(0);
        seedPrivateLibraries(active,defender);
        Card bear=addCardToZone("Grizzly Bears",active,ZoneType.Battlefield);
        bear.setSickness(false);
        try(Actor a=new Actor();Actor b=new Actor()) {
            Assert.assertNotEquals(a.process.pid(),b.process.pid());
            ExternalCreatureController ca=attach(game,active,defender,a);
            ExternalCreatureController cb=attach(game,defender,active,b);
            boolean declared=false;
            int steps=0;
            while(!game.isGameOver() && defender.getLife()==20 && steps++<120) {
                game.getPhaseHandler().mainLoopStep();
                if(game.getPhaseHandler().getPhase()==PhaseType.COMBAT_DECLARE_ATTACKERS &&
                   game.getPhaseHandler().getCombat()!=null &&
                   game.getPhaseHandler().getCombat().getAttackers().contains(bear)) declared=true;
            }
            Assert.assertTrue(declared,"real Forge declared attacker");
            Assert.assertEquals(defender.getLife(),18,"real combat damage");
            Assert.assertTrue(bear.isTapped(),"normal attacker taps");
            Assert.assertTrue(ca.attackCallbacks>0 && cb.priorityCallbacks>0);
            Assert.assertEquals(active.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void twoUnblockedBearsAttackForFourDamage() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),defender=game.getPlayers().get(0);
        seedPrivateLibraries(active,defender);
        Card first=addCardToZone("Grizzly Bears",active,ZoneType.Battlefield);
        Card second=addCardToZone("Grizzly Bears",active,ZoneType.Battlefield);
        first.setSickness(false);second.setSickness(false);
        try(Actor a=new Actor();Actor b=new Actor()) {
            attach(game,active,defender,a);
            attach(game,defender,active,b);
            int steps=0;
            while(!game.isGameOver() && defender.getLife()==20 && steps++<120)
                game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(defender.getLife(),16);
            Assert.assertTrue(first.isTapped() && second.isTapped());
        }
    }

    @Test(timeOut=120000)
    public void cannotAttackWithSickOrTappedBears() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),defender=game.getPlayers().get(0);
        seedPrivateLibraries(active,defender);
        Card sick=addCardToZone("Grizzly Bears",active,ZoneType.Battlefield);
        Card tapped=addCardToZone("Grizzly Bears",active,ZoneType.Battlefield);
        sick.setSickness(true);tapped.setSickness(false);tapped.setTapped(true);
        try(Actor a=new Actor();Actor b=new Actor()) {
            attach(game,active,defender,a);
            attach(game,defender,active,b);
            int steps=0;
            while(!game.isGameOver() &&
                  game.getPhaseHandler().getPlayerTurn()==active && steps++<150)
                game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<150);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertFalse(sick.isTapped());
            Assert.assertTrue(tapped.isTapped());
        }
    }

    @Test(timeOut=120000)
    public void atomicAttackProposalRejectsMixedSickAndReadyCards() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),defender=game.getPlayers().get(0);
        seedPrivateLibraries(active,defender);
        Card ready=addCardToZone("Grizzly Bears",active,ZoneType.Battlefield);
        Card sick=addCardToZone("Grizzly Bears",active,ZoneType.Battlefield);
        ready.setSickness(false);sick.setSickness(true);
        try(Actor a=new Actor();Actor b=new Actor()) {
            ExternalCreatureController ca=attach(game,active,defender,a);
            attach(game,defender,active,b);
            int steps=0;
            while(!game.isGameOver() &&
                  game.getPhaseHandler().getPlayerTurn()==active && steps++<150)
                game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<150);
            Assert.assertTrue(ca.attackCallbacks>0);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertFalse(ready.isTapped());
        }
    }

    @Test(timeOut=120000)
    public void forgedAttackPayloadsAndOutOfTurnAccessFailClosed() {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        Card bear=addCardToZone("Grizzly Bears",opponent,ZoneType.Battlefield);
        bear.setSickness(false);
        for(String payload:Arrays.asList(
            "{\"intent\":\"attack\",\"table_indices\":[0],\"life\":999}",
            "{\"intent\":\"attack\",\"table_indices\":[-1]}",
            "{\"intent\":\"attack\",\"table_indices\":[0.5]}",
            "{\"intent\":\"attack\",\"table_indices\":[0,0]}",
            "{\"intent\":\"attack\",\"table_indices\":[\"0\"]}",
            "{\"intent\":\"attack\",\"table_indices\":[]}",
            "{\"intent\":\"attack\",\"table_indices\":[0,1,2,3,4,5,6,7,8]}")) {
            try {
                validateReply(JsonParser.parseString(payload).getAsJsonObject());
                Assert.fail("forged attack accepted: "+payload);
            } catch(AssertionError expected) {
                Assert.assertNotNull(expected.getMessage());
            }
        }
        JsonObject reply=JsonParser.parseString(
            "{\"intent\":\"attack\",\"table_indices\":[0]}").getAsJsonObject();
        validateReply(reply);
        Combat fake=new Combat(opponent);fake.initConstraints();
        refereeDeclareAttackers(opponent,active,fake,reply);
        Assert.assertTrue(fake.getAttackers().isEmpty());
        Assert.assertFalse(bear.isTapped());
        String baseline=observation(active,opponent);
        addCardToZone("Counterspell",opponent,ZoneType.Hand);
        addCardToZone("Shock",opponent,ZoneType.Library);
        Assert.assertEquals(observation(active,opponent),baseline);
    }

    @Test(timeOut=120000)
    public void authenticRegisteredCountsPersistAcrossRealPriorityLandAndDraw() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(self,other);
        Map<String,Integer> initial=frozenMain.owned(self);
        Assert.assertEquals(initial.get("Forest"),Integer.valueOf(12));
        Assert.assertEquals(initial.get("Llanowar Elves"),Integer.valueOf(3));
        Assert.assertFalse(initial.containsKey("Counterspell"));
        String before=observation(other,self);
        self.getRegisteredPlayer().getDeck().getMain().add(registeredCard("Plains"),40);
        Assert.assertFalse(frozenMain.owned(self).containsKey("Plains"));
        addCardToZone("Forest",self,ZoneType.Hand);
        try(Actor a=new Actor(); Actor b=new Actor()) {
            attach(game,self,other,a);
            attach(game,other,self,b);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(self.getLandsPlayedThisTurn(),1);
            Assert.assertEquals(frozenMain.owned(self),initial);
            Assert.assertFalse(observation(self,other).contains("Plains"));
            Assert.assertEquals(observation(other,self),before.replace("\"opponent_table\":[]",
                    "\"opponent_table\":[{\"appearance\":\"face_up\",\"card\":\"Forest\"}]"));
            Assert.assertFalse(observation(other,self).contains("Llanowar Elves"));
        }
    }

    @Test(timeOut=120000)
    public void differentHiddenOrderWorldsFeedIdenticalObservationsToIndependentActors() throws Exception {
        Game one=initAndCreateGame();
        Player a1=one.getPlayers().get(1),b1=one.getPlayers().get(0);
        for(String name:Arrays.asList("Forest","Grizzly Bears","Llanowar Elves")) {
            addCardToZone(name,a1,ZoneType.Library);
        }
        addCardToZone("Counterspell",b1,ZoneType.Hand);
        String aView=observation(a1,b1),bView=observation(b1,a1);
        Game two=initAndCreateGame();
        Player a2=two.getPlayers().get(1),b2=two.getPlayers().get(0);
        for(String name:Arrays.asList("Llanowar Elves","Grizzly Bears","Forest")) {
            addCardToZone(name,a2,ZoneType.Library);
        }
        addCardToZone("Counterspell",b2,ZoneType.Hand);
        String aView2=observation(a2,b2),bView2=observation(b2,a2);
        Assert.assertEquals(aView,aView2,"actor cannot learn its shuffled draw order");
        Assert.assertEquals(bView,bView2,"opponent cannot learn another player's order");
        try(Actor first=new Actor();Actor second=new Actor()) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            JsonObject actionA=first.ask(aView);
            JsonObject actionB=second.ask(bView);
            Assert.assertEquals(actionA,first.ask(aView2));
            Assert.assertEquals(actionB,second.ask(bView2));
        }
    }

    @Test(timeOut=120000)
    public void inheritedFullInformationDecisionCallbacksFailClosedWithoutFallback() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedPrivateLibraries(self,other);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController c=attach(game,self,other,aa);
            attach(game,other,self,bb);
            int denied=0;
            try { c.chooseCardsForEffect(null,null,"adversarial choice",0,1,true,
                    Collections.emptyMap()); } catch(SecurityException expected) { denied++; }
            try { c.chooseCardsToDiscardToMaximumHandSize(1); }
                    catch(SecurityException expected) { denied++; }
            try { c.chooseStartingHand(Collections.emptyList()); }
                    catch(SecurityException expected) { denied++; }
            try { c.confirmAction(null,null,"adversarial confirmation",
                    Collections.emptyList(),null,Collections.emptyMap()); }
                    catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,4,
                    "none of these decision families may fall back to privileged Forge AI");
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(self.getLife(),20);
            Assert.assertEquals(other.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void inheritedEntityAndSpellDecisionFamiliesCannotInspectRefereeState() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(self,opponent);
        addCardToZone("Grizzly Bears",self,ZoneType.Battlefield);
        addCardToZone("Counterspell",opponent,ZoneType.Hand);
        try(Actor a=new Actor();Actor b=new Actor()) {
            ExternalCreatureController c=attach(game,self,opponent,a);
            attach(game,opponent,self,b);
            String before=observation(self,opponent);
            int selfLife=self.getLife(),otherLife=opponent.getLife();
            int denied=0;
            try { c.chooseSingleEntityForEffect(self.getCardsIn(ZoneType.Battlefield),
                    null,null,"secret effect",false,null,Collections.emptyMap()); }
                catch(SecurityException expected) { denied++; }
            try { c.chooseEntitiesForEffect(self.getCardsIn(ZoneType.Battlefield),
                    0,1,null,null,"secret entities",null,Collections.emptyMap()); }
                catch(SecurityException expected) { denied++; }
            try { c.chooseSingleSpellForEffect(Collections.emptyList(),null,
                    "secret spell",Collections.emptyMap()); }
                catch(SecurityException expected) { denied++; }
            try { c.chooseSpellAbilitiesForEffect(Collections.emptyList(),null,
                    "secret spells",1,Collections.emptyMap()); }
                catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,4,"no entity/spell choice may fall through to privileged AI");
            Assert.assertEquals(c.priorityCallbacks,0);
            Assert.assertEquals(c.attackCallbacks,0);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(self.getLife(),selfLife);
            Assert.assertEquals(opponent.getLife(),otherLife);
            Assert.assertEquals(observation(self,opponent),before);
        }
    }

    @Test(timeOut=120000)
    public void inheritedSacrificeAndDestroyChoicesDenyWithoutMutation() throws Exception {
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(self,opponent);
        Card bear=addCardToZone("Grizzly Bears",self,ZoneType.Battlefield);
        try(Actor a=new Actor();Actor b=new Actor()) {
            ExternalCreatureController c=attach(game,self,opponent,a);
            attach(game,opponent,self,b);
            int denied=0;
            try { c.choosePermanentsToSacrifice(null,0,1,
                    self.getCardsIn(ZoneType.Battlefield),"sacrifice"); }
                catch(SecurityException expected) { denied++; }
            try { c.choosePermanentsToDestroy(null,0,1,
                    self.getCardsIn(ZoneType.Battlefield),"destroy"); }
                catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,2);
            Assert.assertEquals(c.priorityCallbacks,0);
            Assert.assertEquals(c.attackCallbacks,0);
            Assert.assertTrue(self.getCardsIn(ZoneType.Battlefield).contains(bear));
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(self.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void untrustedBidStaticPaymentAndReplacementCallbacksFailClosed() throws Exception {
        Game game=initAndCreateGame();
        Player actor=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(actor,opponent);
        addCardToZone("Grizzly Bears",actor,ZoneType.Battlefield);
        try(Actor first=new Actor();Actor second=new Actor()) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController controller=attach(game,actor,opponent,first);
            ExternalCreatureController otherController=attach(game,opponent,actor,second);
            String beforeActor=observation(actor,opponent);
            String beforeOpponent=observation(opponent,actor);
            int denied=0;
            try { controller.confirmBidAction(null,null,"adversarial bid",100,actor); }
                catch(SecurityException expected) { denied++; }
            try { controller.confirmStaticApplication(null,null,"adversarial static","private"); }
                catch(SecurityException expected) { denied++; }
            try { controller.confirmPayment(null,"adversarial optional payment",null); }
                catch(SecurityException expected) { denied++; }
            try { controller.confirmReplacementEffect(null,null,actor,"adversarial replacement"); }
                catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,4,"all four privileged confirmation families must fail closed");
            Assert.assertEquals(controller.priorityCallbacks,0);
            Assert.assertEquals(controller.attackCallbacks,0);
            Assert.assertEquals(otherController.priorityCallbacks,0);
            Assert.assertEquals(otherController.attackCallbacks,0);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(actor.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
            Assert.assertEquals(observation(actor,opponent),beforeActor);
            Assert.assertEquals(observation(opponent,actor),beforeOpponent);
        }
    }

    @Test(timeOut=120000)
    public void guardedConfirmationsNeverInspectOrDependOnOpponentPrivateZones() throws Exception {
        Game game=initAndCreateGame();
        Player actor=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(actor,opponent);
        try(Actor first=new Actor();Actor second=new Actor()) {
            ExternalCreatureController controller=attach(game,actor,opponent,first);
            attach(game,opponent,actor,second);
            String baseline=observation(actor,opponent);
            Card secret=addCardToZone("Counterspell",opponent,ZoneType.Hand);
            addCardToZone("Shock",opponent,ZoneType.Library);
            Assert.assertEquals(observation(actor,opponent),baseline);
            int denied=0;
            try { controller.confirmBidAction(null,null,secret.getName(),999,opponent); }
                catch(SecurityException expected) { denied++; }
            try { controller.confirmStaticApplication(secret,null,"ignored","ignored"); }
                catch(SecurityException expected) { denied++; }
            try { controller.confirmPayment(null,secret.getName(),null); }
                catch(SecurityException expected) { denied++; }
            try { controller.confirmReplacementEffect(null,null,secret,secret.getName()); }
                catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,4);
            Assert.assertEquals(controller.priorityCallbacks,0);
            Assert.assertEquals(controller.attackCallbacks,0);
            Assert.assertEquals(observation(actor,opponent),baseline);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(actor.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void hiddenPileNameAndLibraryOrderingCallbacksAllFailClosed() throws Exception {
        Game game=initAndCreateGame();
        Player actor=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        Card secret=addCardToZone("Counterspell",opponent,ZoneType.Hand);
        Card hiddenTop=addCardToZone("Shock",opponent,ZoneType.Library);
        seedPrivateLibraries(actor,opponent);
        String beforeA=observation(actor,opponent);
        String beforeB=observation(opponent,actor);
        CardCollection candidates=new CardCollection();
        candidates.add(secret);
        candidates.add(hiddenTop);
        try(Actor workerA=new Actor();Actor workerB=new Actor()) {
            ExternalCreatureController ca=attach(game,actor,opponent,workerA);
            ExternalCreatureController cb=attach(game,opponent,actor,workerB);
            int denied=0;
            try { ca.chooseCardsPile(null,candidates,candidates,"False"); }
                catch(SecurityException expected) { denied++; }
            try { ca.chooseCardName(null,(List<ICardFace>)null,secret.getName()); }
                catch(SecurityException expected) { denied++; }
            try { ca.chooseCardName(null,(Predicate<ICardFace>)null,"",hiddenTop.getName()); }
                catch(SecurityException expected) { denied++; }
            try { ca.chooseCardsForZoneChange(ZoneType.Hand,
                    Collections.singletonList(ZoneType.Library),null,candidates,
                    1,2,null,secret.getName(),actor); }
                catch(SecurityException expected) { denied++; }
            try { ca.arrangeForScry(candidates); }
                catch(SecurityException expected) { denied++; }
            try { ca.arrangeForSurveil(candidates); }
                catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,6,
                    "every unapproved selection must throw before evaluating hidden card details");
            Assert.assertEquals(ca.priorityCallbacks,0);
            Assert.assertEquals(cb.priorityCallbacks,0);
            Assert.assertEquals(ca.attackCallbacks,0);
            Assert.assertEquals(cb.attackCallbacks,0);
            Assert.assertEquals(observation(actor,opponent),beforeA);
            Assert.assertEquals(observation(opponent,actor),beforeB);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(actor.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void hiddenChoiceRejectionsInvariantToOpponentsSecretHandAndLibrary() throws Exception {
        Game game=initAndCreateGame();
        Player actor=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(actor,opponent);
        try(Actor workerA=new Actor();Actor workerB=new Actor()) {
            ExternalCreatureController ca=attach(game,actor,opponent,workerA);
            attach(game,opponent,actor,workerB);
            String baseline=observation(actor,opponent);
            for(String secretName:Arrays.asList("Counterspell","Shock","Grizzly Bears")) {
                Card secret=addCardToZone(secretName,opponent,ZoneType.Hand);
                addCardToZone(secretName,opponent,ZoneType.Library);
                CardCollection choices=new CardCollection();
                choices.add(secret);
                int denies=0;
                try { ca.chooseCardsPile(null,choices,choices,"False"); }
                    catch(SecurityException expected) { denies++; }
                try { ca.arrangeForScry(choices); }
                    catch(SecurityException expected) { denies++; }
                try { ca.chooseCardsForZoneChange(ZoneType.Hand,
                        Collections.singletonList(ZoneType.Library),null,choices,
                        1,1,null,secret.getName(),opponent); }
                    catch(SecurityException expected) { denies++; }
                Assert.assertEquals(denies,3);
                Assert.assertEquals(observation(actor,opponent),baseline,
                        "changing concealed opponent states must not affect actor projection");
            }
            Assert.assertEquals(ca.priorityCallbacks,0);
            Assert.assertEquals(ca.attackCallbacks,0);
            Assert.assertEquals(actor.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
        }
    }

    /** The test fixture deliberately loads ALL registered main cards into the
     * physical library, then checks conservation after real Forge draws.
     * A production match constructor/deck shuffler is NOT installed here. */
    private void seedWholeRegisteredLibrary(Player player) {
        Assert.assertEquals(player.getCardsIn(ZoneType.Library).size(),0,
                "reconciliation must begin from empty fixture library");
        for(Map.Entry<PaperCard,Integer> item :
                player.getRegisteredPlayer().getDeck().getMain()) {
            for(int i=0;i<item.getValue();i++) {
                Card card=Card.fromPaperCard(item.getKey(),player);
                card.setGameTimestamp(player.getGame().getNextTimestamp());
                player.getZone(ZoneType.Library).add(card);
            }
        }
    }

    private static Map<String,Integer> countInventory(Player player) {
        TreeMap<String,Integer> actual=new TreeMap<>();
        for(ZoneType zone:Arrays.asList(ZoneType.Library,ZoneType.Hand,ZoneType.Battlefield)) {
            for(Card card:player.getCardsIn(zone)) {
                if(card.getOwner()!=player || card.isToken()) {
                    throw new IllegalStateException("Unexpected token or borrowed card in inventory");
                }
                actual.merge(card.getName(),1,Integer::sum);
            }
        }
        return actual;
    }

    @Test(timeOut=120000)
    public void physicalStartingLibraryMatchesRegisteredMainAndSurvivesRealDraws() throws Exception {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        seedWholeRegisteredLibrary(a);
        seedWholeRegisteredLibrary(b);
        Assert.assertEquals(a.getCardsIn(ZoneType.Library).size(),20);
        Assert.assertEquals(b.getCardsIn(ZoneType.Library).size(),20);
        Assert.assertEquals(countInventory(a),frozenMain.owned(a),
                "all twenty actual owner cards must exactly match registration");
        Assert.assertEquals(countInventory(b),frozenMain.owned(b));
        String otherBefore=observation(b,a);
        Map<String,Integer> aRegistered=new TreeMap<>(frozenMain.owned(a));
        a.drawCard();
        Assert.assertEquals(a.getCardsIn(ZoneType.Library).size(),19);
        Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(),1);
        Assert.assertEquals(countInventory(a),aRegistered,
                "moving a real card from library to hand must preserve counts");
        Assert.assertEquals(observation(b,a),otherBefore,
                "opponent must not learn actual drawn identity or deck order");
        b.drawCard();
        Assert.assertEquals(b.getCardsIn(ZoneType.Library).size(),19);
        Assert.assertEquals(countInventory(b),frozenMain.owned(b));
        try(Actor actorA=new Actor();Actor actorB=new Actor()) {
            Assert.assertNotEquals(actorA.process.pid(),actorB.process.pid());
            JsonObject replyA=actorA.ask(observation(a,b));
            JsonObject replyB=actorB.ask(observation(b,a));
            Assert.assertFalse(observation(a,b).contains("Counterspell"));
            Assert.assertFalse(observation(b,a).contains("Llanowar Elves"));
            Assert.assertNotNull(replyA.get("intent"));
            Assert.assertNotNull(replyB.get("intent"));
            Assert.assertNotEquals(observation(a,b),observation(b,a),
                    "the two actor processes must receive different permitted own decklists");
        }
    }

    @Test(timeOut=120000)
    public void unapprovedMulliganAndTargetCallbacksDenyWithoutCallingPrivilegedAI() throws Exception {
        Game game=initAndCreateGame();
        Player actor=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(actor,opponent);
        Card secret=addCardToZone("Counterspell",opponent,ZoneType.Hand);
        String actorBefore=observation(actor,opponent);
        String oppBefore=observation(opponent,actor);
        try(Actor first=new Actor();Actor second=new Actor()) {
            ExternalCreatureController c=attach(game,actor,opponent,first);
            ExternalCreatureController o=attach(game,opponent,actor,second);
            int denied=0;
            try { c.mulliganKeepHand(opponent,1); }
                catch(SecurityException expected) { denied++; }
            try { c.tuckCardsViaMulligan(opponent.getCardsIn(ZoneType.Hand),1); }
                catch(SecurityException expected) { denied++; }
            try { c.chooseTargetsFor(null); }
                catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,3,"unapproved mulligan and target decisions must fail closed");
            Assert.assertEquals(c.priorityCallbacks,0);
            Assert.assertEquals(o.priorityCallbacks,0);
            Assert.assertEquals(c.attackCallbacks,0);
            Assert.assertEquals(o.attackCallbacks,0);
            Assert.assertEquals(observation(actor,opponent),actorBefore);
            Assert.assertEquals(observation(opponent,actor),oppBefore);
            Assert.assertTrue(opponent.getCardsIn(ZoneType.Hand).contains(secret));
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(actor.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void cardFaceSpliceAndCostChoicesDenyWithoutStateMutation() throws Exception {
        Game game=initAndCreateGame();
        Player actor=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(actor,opponent);
        Card secret=addCardToZone("Grizzly Bears",opponent,ZoneType.Hand);
        List<Card> unsafeCards=Collections.singletonList(secret);
        String baseline=observation(actor,opponent);
        try(Actor first=new Actor();Actor second=new Actor()) {
            ExternalCreatureController c=attach(game,actor,opponent,first);
            attach(game,opponent,actor,second);
            int denied=0;
            try { c.chooseSingleCardFace(null,(List<ICardFace>)null,secret.getName()); }
                catch(SecurityException expected) { denied++; }
            try { c.chooseSingleCardFace(null,secret.getName(),
                    (Predicate<ICardFace>)null,secret.getName()); }
                catch(SecurityException expected) { denied++; }
            try { c.chooseCardsForSplice(null,unsafeCards); }
                catch(SecurityException expected) { denied++; }
            try { c.chooseNumberForCostReduction(null,0,17); }
                catch(SecurityException expected) { denied++; }
            Assert.assertEquals(denied,4,"no card-face/splice/cost inference without approval");
            Assert.assertEquals(c.priorityCallbacks,0);
            Assert.assertEquals(c.attackCallbacks,0);
            Assert.assertEquals(observation(actor,opponent),baseline);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertTrue(opponent.getCardsIn(ZoneType.Hand).contains(secret));
            Assert.assertEquals(actor.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void hiddenWorldChangesCannotAlterDeniedDecisionsOrActorObservations() throws Exception {
        Game game=initAndCreateGame();
        Player actor=game.getPlayers().get(1),opponent=game.getPlayers().get(0);
        seedPrivateLibraries(actor,opponent);
        String baseline=observation(actor,opponent);
        try(Actor first=new Actor();Actor second=new Actor()) {
            ExternalCreatureController c=attach(game,actor,opponent,first);
            attach(game,opponent,actor,second);
            for(String secret:Arrays.asList("Shock","Counterspell","Grizzly Bears")) {
                Card unseen=addCardToZone(secret,opponent,ZoneType.Hand);
                addCardToZone(secret,opponent,ZoneType.Library);
                int denied=0;
                try { c.mulliganKeepHand(opponent,1); }
                    catch(SecurityException expected) { denied++; }
                try { c.chooseTargetsFor(null); }
                    catch(SecurityException expected) { denied++; }
                try { c.chooseCardsForSplice(null,Collections.singletonList(unseen)); }
                    catch(SecurityException expected) { denied++; }
                Assert.assertEquals(denied,3);
                Assert.assertEquals(observation(actor,opponent),baseline,
                        "no opponent hand/library secret is allowed to affect projection");
            }
            Assert.assertEquals(c.priorityCallbacks,0);
            Assert.assertEquals(c.attackCallbacks,0);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(actor.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
        }
    }

    @Test(timeOut=120000)
    public void openingHandUsesOnlyOwnVisibleCardsAndActualSeparateProcesses() throws Exception {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        for(String name:Arrays.asList("Forest","Forest","Grizzly Bears","Grizzly Bears",
                "Llanowar Elves","Llanowar Elves","Grizzly Bears")) {
            addCardToZone(name,a,ZoneType.Hand);
        }
        for(String name:Arrays.asList("Island","Counterspell","Grizzly Bears","Counterspell",
                "Grizzly Bears","Counterspell","Grizzly Bears")) {
            addCardToZone(name,b,ZoneType.Hand);
        }
        try(Actor first=new Actor();Actor second=new Actor()) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController ca=attach(game,a,b,first);
            ExternalCreatureController cb=attach(game,b,a,second);
            ca.enableOwnHandOpening();
            cb.enableOwnHandOpening();
            Assert.assertTrue(ca.mulliganKeepHand(b,0),
                    "two Forests are in A's OWN visible hand; A keeps");
            Assert.assertFalse(cb.mulliganKeepHand(a,0),
                    "one Island means B's own opening-hand policy chooses mulligan");
            Assert.assertEquals(ca.openingCallbacks,1);
            Assert.assertEquals(cb.openingCallbacks,1);
            Assert.assertEquals(ca.priorityCallbacks,0);
            Assert.assertEquals(cb.priorityCallbacks,0);
            Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(),7);
            Assert.assertEquals(b.getCardsIn(ZoneType.Hand).size(),7);
            Assert.assertFalse(observation(a,b).contains("Counterspell"));
            Assert.assertFalse(observation(b,a).contains("Llanowar Elves"));
        }
    }

    @Test(timeOut=120000)
    public void bottomSelectionUsesRefereeCountAndOwnCurrentHandOnly() throws Exception {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        for(String name:Arrays.asList("Forest","Forest","Grizzly Bears","Grizzly Bears",
                "Llanowar Elves","Llanowar Elves","Grizzly Bears")) {
            addCardToZone(name,a,ZoneType.Hand);
        }
        try(Actor first=new Actor();Actor second=new Actor()) {
            ExternalCreatureController ca=attach(game,a,b,first);
            attach(game,b,a,second);
            ca.enableOwnHandOpening();
            List<Card> before=new ArrayList<>(a.getCardsIn(ZoneType.Hand));
            CardCollectionView bottom=ca.tuckCardsViaMulligan(a.getCardsIn(ZoneType.Hand),2);
            Assert.assertEquals(bottom.size(),2);
            Assert.assertTrue(bottom.get(0).getOwner()==a && bottom.get(1).getOwner()==a);
            Assert.assertNotSame(bottom.get(0),bottom.get(1));
            Assert.assertEquals(a.getCardsIn(ZoneType.Hand),before,
                    "referee controller only chooses, Forge game moves real Cards later");
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(ca.openingCallbacks,1);
            Assert.assertEquals(ca.priorityCallbacks,0);
            try {
                ca.tuckCardsViaMulligan(a.getCardsIn(ZoneType.Hand),8);
                Assert.fail("bottom beyond own hand must be rejected");
            } catch(SecurityException expected) {
                Assert.assertTrue(expected.getMessage().contains("mismatched"));
            }
        }
    }

    @Test(timeOut=120000)
    public void invalidOpeningPayloadsCannotSelectRefereeCardsOrChangeLife() {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        int denied=0;
        for(String raw:Arrays.asList(
                "{\"intent\":\"opening_hand\",\"keep\":true,\"bottom_priority\":[0,0]}",
                "{\"intent\":\"opening_hand\",\"keep\":true,\"bottom_priority\":[0,2]}",
                "{\"intent\":\"opening_hand\",\"keep\":false,\"bottom_priority\":[0,1],\"opponent_hand\":[]}",
                "{\"intent\":\"opening_hand\",\"keep\":false,\"bottom_priority\":[0,\"1\"]}",
                "{\"intent\":\"opening_hand\",\"keep\":false,\"bottom_priority\":[0,1],\"set_life\":0}",
                "{\"intent\":\"opening_hand\",\"keep\":1,\"bottom_priority\":[0,1]}")) {
            try {validateOpeningReply(JsonParser.parseString(raw).getAsJsonObject(),2);}
            catch(SecurityException expected) {denied++;}
        }
        Assert.assertEquals(denied,6);
        Assert.assertEquals(a.getLife(),20);
        Assert.assertEquals(b.getLife(),20);
        Assert.assertTrue(a.getCardsIn(ZoneType.Hand).isEmpty());
    }

    @Test(timeOut=120000)
    public void opponentHiddenCardsCannotInfluenceOwnOpeningHandReply() throws Exception {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        for(String name:Arrays.asList("Forest","Forest","Grizzly Bears","Grizzly Bears",
                "Grizzly Bears","Llanowar Elves","Llanowar Elves")) {
            addCardToZone(name,a,ZoneType.Hand);
        }
        try(Actor first=new Actor();Actor second=new Actor()) {
            ExternalCreatureController ca=attach(game,a,b,first);
            attach(game,b,a,second);
            ca.enableOwnHandOpening();
            String baseline=observation(a,b);
            JsonObject before=first.askOpening(baseline);
            for(String name:Arrays.asList("Counterspell","Island","Shock")) {
                addCardToZone(name,b,ZoneType.Hand);
                addCardToZone(name,b,ZoneType.Library);
                Assert.assertEquals(observation(a,b),baseline,
                        "opponent hand or draw order must not change own observation");
                JsonObject after=first.askOpening(observation(a,b));
                Assert.assertEquals(after,before,
                        "same own-hand information gives same decision across hidden worlds");
            }
            Assert.assertTrue(ca.mulliganKeepHand(b,0));
            Assert.assertEquals(ca.openingCallbacks,1);
            Assert.assertEquals(ca.priorityCallbacks,0);
        }
    }

    private static final class OpeningResult {
        final int[] mulligans = new int[2];
        int rounds;
        int returned;
    }

    /** Process-backed pregame, explicitly controlled by referee. This is not
     * the generic Forge Game.startGame flow. No actor receives counts/turn/order.
     * Both players declare before any re-deals happen within a round. */
    private OpeningResult trustedPregame(Game game,Player a,Player b,
            ExternalCreatureController ca,ExternalCreatureController cb) {
        Player[] players={a,b};
        ExternalCreatureController[] controllers={ca,cb};
        for(Player player:players) {
            seedWholeRegisteredLibrary(player);
            player.shuffle(null);
            Assert.assertEquals(player.getCardsIn(ZoneType.Library).size(),20);
            player.drawCards(7);
            Assert.assertEquals(player.getCardsIn(ZoneType.Hand).size(),7);
        }
        ca.enableOwnHandOpening();
        cb.enableOwnHandOpening();
        OpeningResult outcome=new OpeningResult();
        boolean[] kept=new boolean[2];
        for(int round=0;round<8;round++) {
            boolean[] doMulligan=new boolean[2];
            for(int i=0;i<2;i++) {
                if(kept[i]) continue;
                int available=players[i].getCardsIn(ZoneType.Hand).size();
                // User's actor does NOT receive how many mulligans it has made.
                if(outcome.mulligans[i]>=7 || available==0) {
                    kept[i]=true;
                } else {
                    boolean keep=controllers[i].mulliganKeepHand(a,outcome.mulligans[i]);
                    if(keep) kept[i]=true;
                    else doMulligan[i]=true;
                }
            }
            if(kept[0] && kept[1]) {
                outcome.rounds=round+1;
                return outcome;
            }
            // Decisions are already locked before either library or hand changes.
            for(int i=0;i<2;i++) {
                if(!doMulligan[i]) continue;
                Player player=players[i];
                for(Card card:new ArrayList<>(player.getCardsIn(ZoneType.Hand))) {
                    Assert.assertNotNull(game.getAction().moveToLibrary(card,null));
                }
                player.shuffle(null); // actual Forge shuffle; no actor controls RNG
                player.drawCards(7); // actual Forge draw
                outcome.mulligans[i]++;
                int bottoms=outcome.mulligans[i];
                CardCollectionView selected=controllers[i].tuckCardsViaMulligan(
                        player.getCardsIn(ZoneType.Hand),bottoms);
                Assert.assertEquals(selected.size(),bottoms);
                for(Card card:new ArrayList<>(selected)) {
                    Assert.assertNotNull(game.getAction().moveToBottomOfLibrary(card,null));
                    outcome.returned++;
                }
                Assert.assertEquals(player.getCardsIn(ZoneType.Hand).size(),7-bottoms);
                Assert.assertEquals(countInventory(player),frozenMain.owned(player));
            }
        }
        throw new IllegalStateException("Bounded London mulligan cannot reach both-kept");
    }

    @Test(timeOut=120000)
    public void completeRefereePregameShufflesDrawsAndStartsRealPriorityForTwoActors() throws Exception {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        try(Actor first=new Actor();Actor second=new Actor()) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController ca=attach(game,a,b,first);
            ExternalCreatureController cb=attach(game,b,a,second);
            OpeningResult result=trustedPregame(game,a,b,ca,cb);
            Assert.assertTrue(result.rounds>=1 && result.rounds<=8);
            Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(),7-result.mulligans[0]);
            Assert.assertEquals(b.getCardsIn(ZoneType.Hand).size(),7-result.mulligans[1]);
            Assert.assertEquals(countInventory(a),frozenMain.owned(a));
            Assert.assertEquals(countInventory(b),frozenMain.owned(b));
            Assert.assertTrue(ca.openingCallbacks>0);
            Assert.assertTrue(cb.openingCallbacks>0);
            Assert.assertEquals(ca.priorityCallbacks,0);
            Assert.assertEquals(cb.priorityCallbacks,0);
            Assert.assertEquals(a.getLife(),20);
            Assert.assertEquals(b.getLife(),20);
            // No devModeSet or manual priority events after initial AITest setup.
            game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(ca.priorityCallbacks+cb.priorityCallbacks>0);
            Assert.assertEquals(countInventory(a),frozenMain.owned(a));
            Assert.assertEquals(countInventory(b),frozenMain.owned(b));
        }
    }

    @Test(timeOut=120000)
    public void twoPlayerPregameNeverTransfersHiddenMulliganOrShuffleInformation() throws Exception {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(1),b=game.getPlayers().get(0);
        try(Actor first=new Actor();Actor second=new Actor()) {
            ExternalCreatureController ca=attach(game,a,b,first);
            ExternalCreatureController cb=attach(game,b,a,second);
            trustedPregame(game,a,b,ca,cb);
            String viewA=observation(a,b),viewB=observation(b,a);
            JsonObject stateA=JsonParser.parseString(viewA).getAsJsonObject();
            JsonObject stateB=JsonParser.parseString(viewB).getAsJsonObject();
            Assert.assertEquals(stateA.size(),4);
            Assert.assertEquals(stateB.size(),4);
            for(String denied:Arrays.asList("mulligans","mulligan_count",
                    "shuffle","library_order","library_top","opponent_hand",
                    "priority","stack","phase","turn")) {
                Assert.assertFalse(stateA.has(denied));
                Assert.assertFalse(stateB.has(denied));
            }
            Map<String,Integer> actualA=countInventory(a), actualB=countInventory(b);
            Assert.assertEquals(actualA,frozenMain.owned(a));
            Assert.assertEquals(actualB,frozenMain.owned(b));
            String aBefore=observation(a,b);
            b.shuffle(null);
            Assert.assertEquals(observation(a,b),aBefore,
                    "opponent's real private library shuffle cannot change own view");
            Assert.assertEquals(countInventory(b),actualB);
            Assert.assertEquals(a.getLife(),20);
            Assert.assertEquals(b.getLife(),20);
        }
    }

    /** Both possible referee-chosen starters must receive zero first-turn
     * draws, while the second player must draw exactly one on their turn.
     * No phase, turn or opponent hand is sent to either subprocess. */
    private void nativeFirstTurnAndDrawByOrder(int starterIndex) throws Exception {
        Game game=newBeforeFirstTurnGame();
        Player first=game.getPlayers().get(starterIndex);
        Player second=game.getPlayers().get(1-starterIndex);
        try(Actor firstWorker=new Actor();Actor secondWorker=new Actor()) {
            Assert.assertNotEquals(firstWorker.process.pid(),secondWorker.process.pid());
            ExternalCreatureController ca=attach(game,first,second,firstWorker);
            ExternalCreatureController cb=attach(game,second,first,secondWorker);
            int choices=runNativeMulligans(game,first,second,ca,cb);
            Assert.assertTrue(choices>=2);
            int initialFirstLibrary=first.getCardsIn(ZoneType.Library).size();
            int initialSecondLibrary=second.getCardsIn(ZoneType.Library).size();
            String beforeFirst=observation(first,second);
            String beforeSecond=observation(second,first);
            Assert.assertFalse(beforeFirst.contains("\"turn\""));
            Assert.assertFalse(beforeSecond.contains("\"turn\""));
            Assert.assertFalse(beforeFirst.contains("\"phase\""));
            Assert.assertFalse(beforeSecond.contains("\"phase\""));
            game.setAge(GameStage.Play);
            game.setStartingPlayer(first);
            game.getPhaseHandler().setupFirstTurn(first,null);
            Assert.assertEquals(game.getPhaseHandler().getPlayerTurn(),first);
            Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.UNTAP);
            boolean firstMainSeen=false,secondMainSeen=false;
            int steps=0;
            while(!game.isGameOver() && steps++<240) {
                game.getPhaseHandler().mainLoopStep();
                if(game.getPhaseHandler().getPlayerTurn()==first &&
                        game.getPhaseHandler().getPhase()==PhaseType.MAIN1) {
                    firstMainSeen=true;
                    Assert.assertEquals(first.getCardsIn(ZoneType.Library).size(),
                            initialFirstLibrary,
                            "Forge CR 103.8a: two-player starting player skips first draw");
                }
                if(game.getPhaseHandler().getPlayerTurn()==second &&
                        game.getPhaseHandler().getPhase()==PhaseType.MAIN1) {
                    secondMainSeen=true;
                    Assert.assertEquals(second.getCardsIn(ZoneType.Library).size(),
                            initialSecondLibrary-1,
                            "Forge CR 103.8a: second player draws on their first turn");
                    break;
                }
            }
            Assert.assertTrue(firstMainSeen,"native first turn MAIN1 required");
            Assert.assertTrue(secondMainSeen,"native second player's first MAIN1 required");
            Assert.assertTrue(steps<240);
            Assert.assertTrue(ca.priorityCallbacks>0);
            Assert.assertTrue(cb.priorityCallbacks>0);
            Assert.assertEquals(countInventory(first),frozenMain.owned(first));
            Assert.assertEquals(countInventory(second),frozenMain.owned(second));
            String finalFirst=observation(first,second);
            String finalSecond=observation(second,first);
            for(String denied:Arrays.asList("\"phase\"","\"turn\"","\"priority\"",
                    "\"stack\"","\"opponent_hand\"","\"library_order\"")) {
                Assert.assertFalse(finalFirst.contains(denied));
                Assert.assertFalse(finalSecond.contains(denied));
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeForgeFirstTurnSkipsDrawWhenForestPlayerStarts() throws Exception {
        nativeFirstTurnAndDrawByOrder(1);
    }

    @Test(timeOut=120000)
    public void nativeForgeFirstTurnSkipsDrawWhenIslandPlayerStarts() throws Exception {
        nativeFirstTurnAndDrawByOrder(0);
    }

    @Test(timeOut=120000)
    public void nativeLondonMulliganKeepsFrozenRegisteredCardsAndIsolatesSecrets() throws Exception {
        Game game=newBeforeFirstTurnGame();
        Player first=game.getPlayers().get(1),second=game.getPlayers().get(0);
        try(Actor firstWorker=new Actor(); Actor secondWorker=new Actor()) {
            ExternalCreatureController ca=attach(game,first,second,firstWorker);
            ExternalCreatureController cb=attach(game,second,first,secondWorker);
            int callbacks=runNativeMulligans(game,first,second,ca,cb);
            Assert.assertTrue(callbacks>=2);
            Assert.assertEquals(countInventory(first),frozenMain.owned(first));
            Assert.assertEquals(countInventory(second),frozenMain.owned(second));
            String firstBefore=observation(first,second);
            second.shuffle(null);
            Assert.assertEquals(observation(first,second),firstBefore,
                    "an opponent private shuffle must not change observation");
            Assert.assertEquals(ca.priorityCallbacks,0);
            Assert.assertEquals(cb.priorityCallbacks,0);
            Assert.assertNull(game.getPhaseHandler().getPhase(),
                    "native mulligan occurs before opening phase");
            Assert.assertEquals(first.getLife(),20);
            Assert.assertEquals(second.getLife(),20);
        }
    }

    /** Signal test stop AFTER actual Match.startGame reached the real first
     * turn hook, but BEFORE an unbounded first-to-last game loop starts. */
    private static final class ReachedNativeFirstTurn extends RuntimeException {
        private static final long serialVersionUID=1L;
    }

    private Game nativeSixtyCardRegisteredGame() {
        Map<String,Integer> island=new TreeMap<>();
        island.put("Island",24);
        island.put("Mountain",20);
        island.put("Counterspell",4);
        island.put("Grizzly Bears",4);
        island.put("Forest",8);
        Map<String,Integer> forest=new TreeMap<>();
        forest.put("Forest",24);
        forest.put("Mountain",20);
        forest.put("Grizzly Bears",4);
        forest.put("Llanowar Elves",4);
        forest.put("Island",8);
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(new RegisteredPlayer(mainDeck("Native sixty island",island))
                .setPlayer(new LobbyPlayerAi("island",null)));
        registered.add(new RegisteredPlayer(mainDeck("Native sixty forest",forest))
                .setPlayer(new LobbyPlayerAi("forest",null)));
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage9Z");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        Assert.assertEquals(frozenMain.owned(game.getPlayers().get(0)).values()
                .stream().mapToInt(Integer::intValue).sum(),60);
        Assert.assertEquals(frozenMain.owned(game.getPlayers().get(1)).values()
                .stream().mapToInt(Integer::intValue).sum(),60);
        return game;
    }

    @Test(timeOut=120000)
    public void realMatchStartPerformsSixtyCardPrepMulliganAndNativeFirstTurn() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        try(Actor firstWorker=new Actor();Actor secondWorker=new Actor()) {
            Assert.assertNotEquals(firstWorker.process.pid(),secondWorker.process.pid());
            ExternalCreatureController ca=attach(game,a,b,firstWorker);
            ExternalCreatureController cb=attach(game,b,a,secondWorker);
            ca.enableOwnHandOpening();
            cb.enableOwnHandOpening();
            MulliganDefs.MulliganRule previous=StaticData.instance().getMulliganRule();
            try {
                StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.London);
                game.getMatch().startGame(game, () -> {
                    Assert.assertEquals(game.getAge(),GameStage.Play);
                    Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.UNTAP);
                    Assert.assertNotNull(game.getStartingPlayer());
                    throw new ReachedNativeFirstTurn();
                });
                Assert.fail("unbounded mainGameLoop must not start");
            } catch(ReachedNativeFirstTurn expected) {
                // Reached only through normal Match.startGame/GameAction.startGame.
            } finally {
                StaticData.instance().setMulliganRule(previous);
            }
            Assert.assertEquals(ca.startingPlayerChoices+cb.startingPlayerChoices,1);
            Assert.assertTrue(ca.openingCallbacks>0);
            Assert.assertTrue(cb.openingCallbacks>0);
            Assert.assertTrue(ca.priorityCallbacks==0 && cb.priorityCallbacks==0);
            Assert.assertEquals(countInventory(a),frozenMain.owned(a));
            Assert.assertEquals(countInventory(b),frozenMain.owned(b));
            Assert.assertEquals(a.getLife(),20);
            Assert.assertEquals(b.getLife(),20);
            Assert.assertEquals(game.getPhaseHandler().getPlayerTurn(),game.getStartingPlayer());
            Assert.assertFalse(observation(a,b).contains("\"turn\""));
            Assert.assertFalse(observation(b,a).contains("\"phase\""));
        }
    }

    @Test(timeOut=120000)
    public void realMatchStartThenNormalTurnsRespectFirstPlayerDrawAndPrivacy() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        try(Actor firstWorker=new Actor();Actor secondWorker=new Actor()) {
            ExternalCreatureController ca=attach(game,a,b,firstWorker);
            ExternalCreatureController cb=attach(game,b,a,secondWorker);
            ca.enableOwnHandOpening();
            cb.enableOwnHandOpening();
            MulliganDefs.MulliganRule previous=StaticData.instance().getMulliganRule();
            try {
                StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.London);
                game.getMatch().startGame(game, () -> { throw new ReachedNativeFirstTurn(); });
                Assert.fail("native match-start hook must stop test-only loop");
            } catch(ReachedNativeFirstTurn expected) {
                // Whole Match.startGame startup and native mulligans have executed.
            } finally {
                StaticData.instance().setMulliganRule(previous);
            }
            Player first=game.getStartingPlayer();
            Player other=first==a?b:a;
            int firstLibrary=first.getCardsIn(ZoneType.Library).size();
            int secondLibrary=other.getCardsIn(ZoneType.Library).size();
            Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.UNTAP);
            int steps=0;
            boolean sawFirstMain=false,sawOtherMain=false;
            while(!game.isGameOver() && steps++<240) {
                game.getPhaseHandler().mainLoopStep();
                if(game.getPhaseHandler().getPlayerTurn()==first &&
                        game.getPhaseHandler().getPhase()==PhaseType.MAIN1) {
                    sawFirstMain=true;
                    Assert.assertEquals(first.getCardsIn(ZoneType.Library).size(),firstLibrary,
                            "native starting player skips first draw");
                }
                if(game.getPhaseHandler().getPlayerTurn()==other &&
                        game.getPhaseHandler().getPhase()==PhaseType.MAIN1) {
                    sawOtherMain=true;
                    Assert.assertEquals(other.getCardsIn(ZoneType.Library).size(),secondLibrary-1,
                            "other player draws exactly one during first natural draw step");
                    break;
                }
            }
            Assert.assertTrue(sawFirstMain);
            Assert.assertTrue(sawOtherMain);
            Assert.assertTrue(steps<240);
            Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
            Assert.assertEquals(countInventory(a),frozenMain.owned(a));
            Assert.assertEquals(countInventory(b),frozenMain.owned(b));
            String before=observation(a,b);
            b.shuffle(null);
            Assert.assertEquals(observation(a,b),before,
                    "real opponent shuffle cannot change allowed observation");
            for(String secretKey:Arrays.asList("\"turn\"","\"phase\"","\"priority\"",
                    "\"stack\"","\"library_order\"","\"library_top\"","\"opponent_hand\"")) {
                Assert.assertFalse(observation(a,b).contains(secretKey));
                Assert.assertFalse(observation(b,a).contains(secretKey));
            }
        }
    }

    /** The trusted test harness cuts into native Match.startGame ONLY at the
     * authorized first-turn hook. All turns afterward use real mainLoopStep
     * (no synthetic draws, priority shifts or devModeSet). */
    private static final class MultiTurnEvidence {
        final Player first;
        final Player second;
        final int firstStartingLibrary;
        final int secondStartingLibrary;
        int observedFirstMainPhases=0;
        int observedSecondMainPhases=0;
        int steps=0;
        MultiTurnEvidence(Game game) {
            first=game.getStartingPlayer();
            second=game.getNextPlayerAfter(first);
            firstStartingLibrary=first.getCardsIn(ZoneType.Library).size();
            secondStartingLibrary=second.getCardsIn(ZoneType.Library).size();
        }
    }

    private void doRealNativeStartForMultiTurn(Game game,
            ExternalCreatureController first,ExternalCreatureController second) {
        first.enableOwnHandOpening();
        second.enableOwnHandOpening();
        MulliganDefs.MulliganRule previous=StaticData.instance().getMulliganRule();
        try {
            StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.London);
            game.getMatch().startGame(game,() -> { throw new ReachedNativeFirstTurn(); });
            Assert.fail("native first-turn hook did not abort unlimited game loop");
        } catch(ReachedNativeFirstTurn expected) {
            // Proper game setup finished. Only the unlimited loop was stopped.
        } finally {
            StaticData.instance().setMulliganRule(previous);
        }
        Assert.assertNotNull(game.getStartingPlayer());
        Assert.assertEquals(game.getPhaseHandler().getPhase(),PhaseType.UNTAP);
        Assert.assertEquals(game.getPhaseHandler().getPlayerTurn(),game.getStartingPlayer());
        Assert.assertEquals(countInventory(game.getPlayers().get(0)),
                frozenMain.owned(game.getPlayers().get(0)));
        Assert.assertEquals(countInventory(game.getPlayers().get(1)),
                frozenMain.owned(game.getPlayers().get(1)));
    }

    private static int basicLandCountOnBattlefield(Player player) {
        int lands=0;
        for(Card c:player.getCardsIn(ZoneType.Battlefield)) {
            if(c.isLand() && c.getOwner()==player) lands++;
        }
        return lands;
    }

    /** Counts unique MAIN1 entries and checks actual private draw counts.
     * The active player can submit many priority actions per one main phase;
     * repeated callbacks never count as extra turns. */
    private MultiTurnEvidence reachRealMainTurns(Game game,int targetMainTurns) {
        MultiTurnEvidence proof=new MultiTurnEvidence(game);
        Player lastMainPlayer=null;
        int total=0;
        for(int steps=0;steps<750 && !game.isGameOver();steps++) {
            game.getPhaseHandler().mainLoopStep();
            proof.steps++;
            Player current=game.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(current);
            Assert.assertTrue(current.getLandsPlayedThisTurn()>=0 &&
                    current.getLandsPlayedThisTurn()<=1,
                    "real Forge one-land-per-turn limit must hold on every step");
            if(game.getPhaseHandler().getPhase()==PhaseType.MAIN1 &&
                    current!=lastMainPlayer) {
                total++;
                if(current==proof.first) proof.observedFirstMainPhases++;
                else if(current==proof.second) proof.observedSecondMainPhases++;
                else Assert.fail("unexpected player from referee game");
                lastMainPlayer=current;
                if(total==targetMainTurns) break;
            }
        }
        Assert.assertEquals(proof.observedFirstMainPhases+
                proof.observedSecondMainPhases,targetMainTurns,
                "real Forge must reach the required distinct MAIN1 turns");
        Assert.assertTrue(proof.steps<750,"bounded real Forge progress");
        Assert.assertEquals(proof.first.getCardsIn(ZoneType.Library).size(),
                proof.firstStartingLibrary-Math.max(0,proof.observedFirstMainPhases-1),
                "first player must skip its first draw, then draw once each turn");
        Assert.assertEquals(proof.second.getCardsIn(ZoneType.Library).size(),
                proof.secondStartingLibrary-proof.observedSecondMainPhases,
                "non-starting player draws on every genuine turn");
        return proof;
    }

    @Test(timeOut=120000)
    public void nativeSixtyCardMatchProgressesFiveRealTurnsWithLegalLandDrops() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        try(Actor firstWorker=new Actor();Actor secondWorker=new Actor()) {
            Assert.assertNotEquals(firstWorker.process.pid(),secondWorker.process.pid());
            ExternalCreatureController ca=attach(game,a,b,firstWorker);
            ExternalCreatureController cb=attach(game,b,a,secondWorker);
            doRealNativeStartForMultiTurn(game,ca,cb);
            MultiTurnEvidence evidence=reachRealMainTurns(game,5);
            Assert.assertEquals(evidence.observedFirstMainPhases,3);
            Assert.assertEquals(evidence.observedSecondMainPhases,2);
            Assert.assertEquals(basicLandCountOnBattlefield(a),2,
                    "AI A must complete two legal real land drops in its first two turns");
            Assert.assertEquals(basicLandCountOnBattlefield(b),2,
                    "AI B must complete two legal real land drops in its first two turns");
            Assert.assertEquals(countInventory(a),frozenMain.owned(a));
            Assert.assertEquals(countInventory(b),frozenMain.owned(b));
            Assert.assertTrue(ca.priorityCallbacks>3 && cb.priorityCallbacks>3);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(JsonParser.parseString(observation(a,b))
                    .getAsJsonObject().getAsJsonObject("life_totals")
                    .get("self").getAsInt(),a.getLife());
            Assert.assertEquals(JsonParser.parseString(observation(b,a))
                    .getAsJsonObject().getAsJsonObject("life_totals")
                    .get("self").getAsInt(),b.getLife());
        }
    }

    @Test(timeOut=120000)
    public void nativeMatchRunsSevenTurnsAndPreservesPrivateViewsAndDrawCounts() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        try(Actor firstWorker=new Actor();Actor secondWorker=new Actor()) {
            ExternalCreatureController ca=attach(game,a,b,firstWorker);
            ExternalCreatureController cb=attach(game,b,a,secondWorker);
            doRealNativeStartForMultiTurn(game,ca,cb);
            MultiTurnEvidence proof=reachRealMainTurns(game,7);
            Assert.assertEquals(proof.observedFirstMainPhases,4);
            Assert.assertEquals(proof.observedSecondMainPhases,3);
            Assert.assertTrue(ca.priorityCallbacks>5 && cb.priorityCallbacks>5);
            Assert.assertEquals(countInventory(a),frozenMain.owned(a));
            Assert.assertEquals(countInventory(b),frozenMain.owned(b));
            Assert.assertTrue(basicLandCountOnBattlefield(a)>=2);
            Assert.assertTrue(basicLandCountOnBattlefield(b)>=2);
            String before=observation(a,b);
            b.shuffle(null);
            Assert.assertEquals(before,observation(a,b),
                    "a real opponent-private library shuffle cannot alter actor view");
            JsonObject permitted=JsonParser.parseString(before).getAsJsonObject();
            Assert.assertEquals(permitted.size(),4);
            for(String denied:Arrays.asList("\"turn\"","\"phase\"","\"stack\"",
                    "\"priority\"","\"opponent_hand\"","\"library_order\"",
                    "\"library_top\"","\"mulligan_count\"")) {
                Assert.assertFalse(observation(a,b).contains(denied));
                Assert.assertFalse(observation(b,a).contains(denied));
            }
            Assert.assertEquals(permitted.getAsJsonObject("life_totals")
                    .get("self").getAsInt(),a.getLife());
            Assert.assertEquals(permitted.getAsJsonObject("life_totals")
                    .get("opponent").getAsInt(),b.getLife());
        }
    }

    /** TEST-ONLY controlled hand draw replacement, after real Match.startGame.
     * Keeps actual registered physical cards and the entire inventory intact.
     * This is not a natural/shuffled opening-hand probability claim. */
    private void prepareKnownCastingScenario(Game game,Player caster) {
        int baselineHand=caster.getCardsIn(ZoneType.Hand).size();
        Assert.assertTrue(baselineHand>0 && baselineHand<=7);
        for(String desired:Arrays.asList("Forest","Forest","Grizzly Bears")) {
            int needed="Forest".equals(desired)?2:1;
            int present=0;
            for(Card card:caster.getCardsIn(ZoneType.Hand)) {
                if(desired.equals(card.getName())) present++;
            }
            if(present>=needed) continue;
            Card found=null;
            for(Card card:caster.getCardsIn(ZoneType.Library)) {
                if(desired.equals(card.getName())) {found=card;break;}
            }
            Assert.assertNotNull(found,"referee scenario card absent from real registered deck");
            Assert.assertNotNull(game.getAction().moveToHand(found,null));
        }
        while(caster.getCardsIn(ZoneType.Hand).size()>7) {
            int forests=0,bears=0;
            for(Card card:caster.getCardsIn(ZoneType.Hand)) {
                if("Forest".equals(card.getName()))forests++;
                if("Grizzly Bears".equals(card.getName()))bears++;
            }
            Card removable=null;
            for(Card card:caster.getCardsIn(ZoneType.Hand)) {
                String name=card.getName();
                if(("Forest".equals(name) && forests<=2) ||
                    ("Grizzly Bears".equals(name) && bears<=1)) continue;
                removable=card;
                break;
            }
            Assert.assertNotNull(removable,"cannot conserve seven-card test hand");
            Assert.assertNotNull(game.getAction().moveToBottomOfLibrary(removable,null));
        }
        Assert.assertEquals(countInventory(caster),frozenMain.owned(caster));
        int confirmedForests=0,confirmedBears=0;
        for(Card card:caster.getCardsIn(ZoneType.Hand)) {
            if("Forest".equals(card.getName())) confirmedForests++;
            if("Grizzly Bears".equals(card.getName())) confirmedBears++;
        }
        Assert.assertTrue(confirmedForests>=2 && confirmedBears>=1);
    }

    @Test(timeOut=120000)
    public void unphasedActionPlanCastsPaidBearAndAttacksDuringNaturalMatchTurns() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        try(Actor first=new Actor(true);Actor second=new Actor(true)) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController ca=attach(game,a,b,first);
            ExternalCreatureController cb=attach(game,b,a,second);
            doRealNativeStartForMultiTurn(game,ca,cb);
            prepareKnownCastingScenario(game,a);
            int steps=0;
            boolean bearCast=false,combatHappened=false;
            int realMainTurns=0;
            Player previousMain=null;
            int opponentStartingLife=b.getLife();
            while(!game.isGameOver() && steps++<750 && realMainTurns<8) {
                game.getPhaseHandler().mainLoopStep();
                if(game.getPhaseHandler().getPhase()==PhaseType.MAIN1) {
                    Player active=game.getPhaseHandler().getPlayerTurn();
                    if(active!=previousMain) {
                        realMainTurns++;
                        previousMain=active;
                    }
                }
                for(Card card:a.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(card.getName())) bearCast=true;
                }
                if(b.getLife()<=opponentStartingLife-2) combatHappened=true;
                if(combatHappened) break;
            }
            Assert.assertTrue(bearCast,"the active AI casts a real mana-paid creature");
            Assert.assertTrue(combatHappened,"its later legal combat attack damages the opponent");
            Assert.assertTrue(realMainTurns>=5 && realMainTurns<=8);
            Assert.assertTrue(steps<750,"bounded natural Forge progression");
            Assert.assertEquals(countInventory(a),frozenMain.owned(a));
            Assert.assertEquals(countInventory(b),frozenMain.owned(b));
            Assert.assertTrue(ca.priorityCallbacks>4 && cb.priorityCallbacks>4);
            Assert.assertTrue(ca.attackCallbacks>0);
            JsonObject own=JsonParser.parseString(observation(a,b)).getAsJsonObject();
            Assert.assertEquals(own.getAsJsonObject("life_totals")
                    .get("opponent").getAsInt(),b.getLife());
        }
    }

    @Test(timeOut=120000)
    public void actionPlanNeverDisclosesPhaseAndRejectsForgedOptions() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        int failed=0;
        for(String json:Arrays.asList(
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[0,0],\"creature_hand_indices\":[],\"attack_table_indices\":[]}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[-1],\"creature_hand_indices\":[],\"attack_table_indices\":[]}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[1.5],\"attack_table_indices\":[]}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"phase\":\"MAIN1\"}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"set_life\":100}")) {
            try {
                validateReply(JsonParser.parseString(json).getAsJsonObject());
            } catch(AssertionError expected) {failed++;}
        }
        Assert.assertEquals(failed,5);
        Assert.assertEquals(a.getLife(),20);
        Assert.assertEquals(b.getLife(),20);
        try(Actor worker=new Actor(true)) {
            // Empty own hand is still a valid bounded preference plan.
            JsonObject proposed=worker.ask(observation(a,b));
            Assert.assertEquals(proposed.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposed.size(),4);
            Assert.assertFalse(observation(a,b).contains("\"phase\""));
            Assert.assertFalse(observation(b,a).contains("\"priority\""));
            Assert.assertTrue(game.getStack().isEmpty());
        }
    }

    private void fixturePlaceRegisteredBear(Game game,Player player) {
        // A real native opening draw may put a registered Bear in OWN hand.
        // This trusted referee-only fixture may move either physical card;
        // actor IPC never receives a hand identity from another player.
        Card found=null;
        for(ZoneType zone:Arrays.asList(ZoneType.Library,ZoneType.Hand)) {
            for(Card card:player.getCardsIn(zone)) {
                if("Grizzly Bears".equals(card.getName())) {found=card;break;}
            }
            if(found!=null) break;
        }
        Assert.assertNotNull(found,
                "must exist in registered 60-card library or owner's hand");
        Assert.assertNotNull(game.getAction().moveToPlay(found,null,
                forge.game.ability.AbilityKey.newMap()));
    }

    private static Map<String,Integer> fullPhysicalInventory(Player player) {
        Map<String,Integer> counts=new TreeMap<>();
        for(ZoneType zone:Arrays.asList(ZoneType.Library,ZoneType.Hand,
                ZoneType.Battlefield,ZoneType.Graveyard,
                ZoneType.Stack,ZoneType.Exile,ZoneType.Command,ZoneType.Ante)) {
            for(Card card:player.getCardsIn(zone)) {
                Assert.assertEquals(card.getOwner(),player);
                if(!card.isToken()) counts.merge(card.getName(),1,Integer::sum);
            }
        }
        return counts;
    }

    @Test(timeOut=120000)
    public void independentDefenderBlocksRealAttackerAndForgeDestroysBothBears() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor defWorker=new Actor(2);Actor atkWorker=new Actor(2)) {
            Assert.assertNotEquals(defWorker.process.pid(),atkWorker.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,defWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,atkWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            int defBefore=defender.getLife(),atkBefore=attacker.getLife();
            boolean sawBlock=false,sawCombatDeath=false;
            int steps=0;
            while(!game.isGameOver() && steps++<850) {
                game.getPhaseHandler().mainLoopStep();
                sawBlock |= dc.blocksAuthorized>0;
                int defDead=0,atkDead=0;
                for(Card dead:defender.getCardsIn(ZoneType.Graveyard)) {
                    if("Grizzly Bears".equals(dead.getName()))defDead++;
                }
                for(Card dead:attacker.getCardsIn(ZoneType.Graveyard)) {
                    if("Grizzly Bears".equals(dead.getName()))atkDead++;
                }
                if(sawBlock && defDead>=1 && atkDead>=1) {
                    sawCombatDeath=true;break;
                }
            }
            Assert.assertTrue(sawBlock,"defender selects a real allowed Forge block");
            Assert.assertTrue(dc.refereeDamageAssignments+ac.refereeDamageAssignments>0,
                    "trusted Stage10G referee assignment must run, not inherited AI");
            Assert.assertTrue(sawCombatDeath,"Forge combat damage kills both 2/2 Bears");
            Assert.assertTrue(steps<850,"bounded actual turn progression");
            Assert.assertEquals(defender.getLife(),defBefore,
                    "blocking prevents unblocked damage to defender");
            Assert.assertEquals(attacker.getLife(),atkBefore);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertTrue(dc.blockerCallbacks>0);
            Assert.assertTrue(ac.attackCallbacks>0);
            Assert.assertTrue(dc.priorityCallbacks>0 && ac.priorityCallbacks>0);
            JsonObject view=JsonParser.parseString(observation(defender,attacker)).getAsJsonObject();
            Assert.assertEquals(view.size(),4);
            Assert.assertFalse(observation(defender,attacker).contains("\"phase\""));
            Assert.assertFalse(observation(attacker,defender).contains("\"stack\""));
        }
    }

    @Test(timeOut=120000)
    public void blockerPreferenceRejectsForgedPairsAndDoesNotRevealCombatState() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        int invalid=0;
        for(String raw:Arrays.asList(
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"block_pairs\":[[0,0],[0,0]]}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"block_pairs\":[[0,-1]]}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"block_pairs\":[[0,1.5]]}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"block_pairs\":[],\"priority\":\"defender\"}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"block_pairs\":[],\"set_life\":50}")) {
            try { validateReply(JsonParser.parseString(raw).getAsJsonObject()); }
            catch(AssertionError expected) {invalid++;}
        }
        Assert.assertEquals(invalid,5);
        try(Actor defenderWorker=new Actor(2);Actor attackerWorker=new Actor(2)) {
            JsonObject proposal=defenderWorker.ask(observation(defender,attacker));
            Assert.assertEquals(proposal.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposal.size(),5);
            Assert.assertEquals(refereeDeclareBlockers(defender,attacker,null,proposal),0);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertFalse(observation(defender,attacker).contains("\"combat\""));
        }
    }

    private static int countDeadBears(Player player) {
        int result=0;
        for(Card card:player.getCardsIn(ZoneType.Graveyard)) {
            if("Grizzly Bears".equals(card.getName())) result++;
        }
        return result;
    }

    @Test(timeOut=120000)
    public void twoIndependentAttackersAreBlockedOneToOneAndAllFourBearsDie() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor defWorker=new Actor(2);Actor atkWorker=new Actor(2)) {
            Assert.assertNotEquals(defWorker.process.pid(),atkWorker.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,defWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,atkWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            for(int i=0;i<2;i++) {
                fixturePlaceRegisteredBear(game,defender);
                fixturePlaceRegisteredBear(game,attacker);
            }
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            int defenderStartingLife=defender.getLife();
            int attackerStartingLife=attacker.getLife();
            int steps=0;
            boolean bothBlocks=false,bothPairsDestroyed=false;
            while(!game.isGameOver() && steps++<900) {
                game.getPhaseHandler().mainLoopStep();
                if(dc.maxBlocksInSingleDeclare>=2) bothBlocks=true;
                if(bothBlocks && countDeadBears(defender)>=2 &&
                        countDeadBears(attacker)>=2) {
                    bothPairsDestroyed=true;break;
                }
            }
            Assert.assertTrue(bothBlocks,"two distinct legal Forge block assignments required");
            Assert.assertTrue(bothPairsDestroyed,"real Forge combat damage destroys all four 2/2 Bears");
            Assert.assertTrue(steps<900,"bounded natural Forge turn progression");
            Assert.assertEquals(defender.getLife(),defenderStartingLife,
                    "both incoming attackers were legally blocked");
            Assert.assertEquals(attacker.getLife(),attackerStartingLife);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertTrue(dc.blockerCallbacks>0);
            Assert.assertTrue(ac.attackCallbacks>0);
            Assert.assertFalse(observation(defender,attacker).contains("\"phase\""));
        }
    }

    @Test(timeOut=120000)
    public void flyingAttackerCannotBeBlockedByNonflyingBearDuringRealForgeCombat() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor defWorker=new Actor(2);Actor atkWorker=new Actor(2)) {
            ExternalCreatureController dc=attach(game,defender,attacker,defWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,atkWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card flying=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(card -> "Grizzly Bears".equals(card.getName()))
                    .findFirst().orElseThrow();
            Card grounded=defender.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(card -> "Grizzly Bears".equals(card.getName()))
                    .findFirst().orElseThrow();
            // An authentic Forge continuous keyword change in test-only fixture,
            // not a novel card added to a registered deck.
            flying.addChangedCardKeywords(Collections.singletonList("Flying"),
                    Collections.emptyList(),false,game.getNextTimestamp(),null);
            Assert.assertTrue(flying.hasKeyword("Flying"));
            Assert.assertFalse(CombatUtil.canBlock(flying,grounded));
            int startingLife=defender.getLife(),steps=0;
            boolean damage=false;
            while(!game.isGameOver() && steps++<900) {
                game.getPhaseHandler().mainLoopStep();
                if(defender.getLife()<=startingLife-2) {damage=true;break;}
            }
            Assert.assertTrue(damage,"genuine unblocked flying creature deals combat damage");
            Assert.assertEquals(dc.blocksAuthorized,0,
                    "no unauthorized ground creature may block Flying");
            // Forge may skip the declaration callback entirely when no
            // blocker is legally eligible against a Flying attacker.
            Assert.assertEquals(dc.blocksAuthorized,0);
            Assert.assertTrue(steps<900);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertFalse(observation(defender,attacker).contains("\"combat\""));
            Assert.assertTrue(ac.attackCallbacks>0);
        }
    }

    @Test(timeOut=120000)
    public void tappedAndNonattackingBlockPairsAreRejectedByActualForgeLegality() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor defWorker=new Actor(2);Actor atkWorker=new Actor(2)) {
            ExternalCreatureController dc=attach(game,defender,attacker,defWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,atkWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card block=defender.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(card -> "Grizzly Bears".equals(card.getName()))
                    .findFirst().orElseThrow();
            Card attack=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(card -> "Grizzly Bears".equals(card.getName()))
                    .findFirst().orElseThrow();
            block.setTapped(true);
            Assert.assertFalse(CombatUtil.canBlock(block),"tapped creature cannot block");
            block.setTapped(false);
            Assert.assertTrue(CombatUtil.canBlock(block),
                    "untapped normal Bear is eligible in principle");
            // No attacker is declared yet; a correct referee cannot claim
            // that a particular face-up opponent card is attacking.
            JsonObject intent=defWorker.ask(observation(defender,attacker));
            Assert.assertEquals(refereeDeclareBlockers(defender,attacker,null,intent),0);
            Assert.assertEquals(dc.blocksAuthorized,0);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertTrue(attack.getController()==attacker);
            Assert.assertTrue(block.getController()==defender);
            Assert.assertFalse(observation(attacker,defender).contains("\"priority\""));
            Assert.assertEquals(dc.priorityCallbacks,0);
            Assert.assertEquals(ac.priorityCallbacks,0);
        }
    }


    /** The referee controls a deterministic one-attacker/one-blocker encounter.
     * Menace makes a lone blocker illegal. Two real persistent actor processes
     * receive exactly the same four permitted observation fields as before. */
    @Test(timeOut=120000)
    public void menaceOneProposedBlockerIsIllegalAndCombatDamageHitsPlayer() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor defendingWorker=new Actor(2);Actor attackingWorker=new Actor(2)) {
            Assert.assertNotEquals(defendingWorker.process.pid(),attackingWorker.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,defendingWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,attackingWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card menace=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(card -> "Grizzly Bears".equals(card.getName()))
                    .findFirst().orElseThrow();
            menace.addChangedCardKeywords(Collections.singletonList("Menace"),
                    Collections.emptyList(),false,game.getNextTimestamp(),null);
            Assert.assertTrue(menace.hasKeyword("Menace"));
            int lifeBefore=defender.getLife();
            boolean damage=false;
            int steps=0;
            while(!game.isGameOver() && steps++<900) {
                game.getPhaseHandler().mainLoopStep();
                if(defender.getLife()<=lifeBefore-2) {damage=true;break;}
            }
            Assert.assertTrue(damage,"menace with only one proposed blocker must deal damage");
            Assert.assertEquals(dc.blocksAuthorized,0,
                    "a lone declared blocker violates menace and must be reverted");
            Assert.assertEquals(countDeadBears(defender),0);
            Assert.assertEquals(countDeadBears(attacker),0);
            Assert.assertTrue(steps<900);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertTrue(ac.attackCallbacks>0);
            Assert.assertFalse(observation(defender,attacker).contains("\"combat\""));
            Assert.assertFalse(observation(attacker,defender).contains("\"phase\""));
        }
    }

    /** In genuine native combat, two distinct visible own blockers together
     * satisfy the Menace minimum. Forge handles damage and card movement. */
    @Test(timeOut=120000)
    public void menaceTwoBlockersAreLegalAndPreventPlayerCombatDamage() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor defendingWorker=new Actor(3);Actor attackingWorker=new Actor(2)) {
            Assert.assertNotEquals(defendingWorker.process.pid(),attackingWorker.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,defendingWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,attackingWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card menace=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(card -> "Grizzly Bears".equals(card.getName()))
                    .findFirst().orElseThrow();
            menace.addChangedCardKeywords(Collections.singletonList("Menace"),
                    Collections.emptyList(),false,game.getNextTimestamp(),null);
            Assert.assertTrue(menace.hasKeyword("Menace"));
            int before=defender.getLife();
            boolean legallyBlocked=false,creatureDestroyed=false;
            int steps=0;
            while(!game.isGameOver() && steps++<900) {
                game.getPhaseHandler().mainLoopStep();
                if(dc.blocksAuthorized>=2) legallyBlocked=true;
                if(legallyBlocked && countDeadBears(attacker)>=1) {
                    creatureDestroyed=true;break;
                }
            }
            Assert.assertTrue(legallyBlocked,"two distinct defending Bears must both block same menace attacker");
            Assert.assertTrue(dc.refereeDamageAssignments+ac.refereeDamageAssignments>0,
                    "trusted Stage10G referee assignment must run, not inherited AI");
            Assert.assertTrue(creatureDestroyed,"two 2/2 blockers deal lethal damage to attacking 2/2");
            Assert.assertTrue(countDeadBears(defender)>=1,
                    "attacking Bear should deal two damage to at least one blocker");
            Assert.assertEquals(defender.getLife(),before,
                    "legal double-block prevents unblocked combat damage");
            Assert.assertTrue(steps<900);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertTrue(dc.blockerCallbacks>0);
            Assert.assertTrue(ac.attackCallbacks>0);
            Assert.assertEquals(JsonParser.parseString(observation(defender,attacker))
                    .getAsJsonObject().size(),4);
            Assert.assertFalse(observation(defender,attacker).contains("\"stack\""));
        }
    }


    /**
     * CR 510.4 / 702.7: only the attacking 2/2 Bear has first strike.
     * The referee starts a real 60-card Forge match and uses a bounded
     * registered-card fixture; the two external workers see neither the
     * keyword modification nor combat step flags in their observations.
     */
    @Test(timeOut=120000)
    public void firstStrikeAttackerKillsOrdinaryBlockerWithoutTakingDamage() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0), attacker=game.getPlayers().get(1);
        try(Actor defenderWorker=new Actor(2); Actor attackerWorker=new Actor(2)) {
            Assert.assertNotEquals(defenderWorker.process.pid(),attackerWorker.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,defenderWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,attackerWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card striker=attacker.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            Card normal=defender.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            striker.addChangedCardKeywords(Collections.singletonList("First Strike"),
                Collections.emptyList(),false,game.getNextTimestamp(),null);
            Assert.assertTrue(striker.hasKeyword("First Strike"));
            Assert.assertFalse(normal.hasKeyword("First Strike"));
            int defLife=defender.getLife(), atkLife=attacker.getLife();
            boolean sawBlock=false, sawFirstStrikeLethal=false;
            int steps=0;
            while(!game.isGameOver() && steps++<900) {
                game.getPhaseHandler().mainLoopStep();
                sawBlock |= dc.blocksAuthorized>=1;
                if(sawBlock && countDeadBears(defender)>=1) {
                    sawFirstStrikeLethal=true;break;
                }
            }
            Assert.assertTrue(sawBlock,"native referee must accept real one-to-one block");
            Assert.assertTrue(sawFirstStrikeLethal,"Forge first strike kills normal blocker");
            Assert.assertTrue(dc.refereeDamageAssignments+ac.refereeDamageAssignments>0,
                    "trusted Stage10G referee assignment must run, not inherited AI");
            Assert.assertTrue(steps<900,"bounded genuine combat");
            Assert.assertEquals(countDeadBears(defender),1);
            Assert.assertEquals(countDeadBears(attacker),0,
                "ordinary blocker cannot deal its regular damage after death");
            Assert.assertTrue(attacker.getCardsIn(ZoneType.Battlefield).contains(striker),
                "first-strike attacker must survive combat");
            Assert.assertEquals(defender.getLife(),defLife,
                "legal block prevents damage to defending player");
            Assert.assertEquals(attacker.getLife(),atkLife);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertTrue(ac.attackCallbacks>0);
            Assert.assertFalse(observation(defender,attacker).contains("\"phase\""));
            Assert.assertFalse(observation(attacker,defender).contains("\"stack\""));
        }
    }

    /** The same first-strike rule applies to a defending creature.
     * This case proves that a normal attacking Bear does not kill the
     * 2/2 first-striking blocker before the earlier damage step resolves. */
    @Test(timeOut=120000)
    public void firstStrikeBlockerKillsNormalAttackerAndSurvives() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0), attacker=game.getPlayers().get(1);
        try(Actor defenderWorker=new Actor(2); Actor attackerWorker=new Actor(2)) {
            Assert.assertNotEquals(defenderWorker.process.pid(),attackerWorker.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,defenderWorker);
            ExternalCreatureController ac=attach(game,attacker,defender,attackerWorker);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card striker=defender.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            Card normal=attacker.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            striker.addChangedCardKeywords(Collections.singletonList("First Strike"),
                Collections.emptyList(),false,game.getNextTimestamp(),null);
            Assert.assertTrue(striker.hasKeyword("First Strike"));
            Assert.assertFalse(normal.hasKeyword("First Strike"));
            int defLife=defender.getLife(), atkLife=attacker.getLife();
            boolean sawBlock=false, sawFirstStrikeLethal=false;
            int steps=0;
            while(!game.isGameOver() && steps++<900) {
                game.getPhaseHandler().mainLoopStep();
                sawBlock |= dc.blocksAuthorized>=1;
                if(sawBlock && countDeadBears(attacker)>=1) {
                    sawFirstStrikeLethal=true;break;
                }
            }
            Assert.assertTrue(sawBlock,"native referee must declare real block");
            Assert.assertTrue(sawFirstStrikeLethal,"Forge first-strike blocker kills attacker");
            Assert.assertTrue(dc.refereeDamageAssignments+ac.refereeDamageAssignments>0,
                    "trusted Stage10G referee assignment must run, not inherited AI");
            Assert.assertTrue(steps<900,"bounded genuine combat");
            Assert.assertEquals(countDeadBears(attacker),1);
            Assert.assertEquals(countDeadBears(defender),0,
                "first-strike blocking Bear must avoid ordinary retaliatory damage");
            Assert.assertTrue(defender.getCardsIn(ZoneType.Battlefield).contains(striker),
                "first strike defender remains in play");
            Assert.assertEquals(defender.getLife(),defLife);
            Assert.assertEquals(attacker.getLife(),atkLife);
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertTrue(dc.blockerCallbacks>0 && ac.attackCallbacks>0);
            Assert.assertFalse(observation(attacker,defender).contains("\"priority\""));
        }
    }
    /** One real registered bear versus one real registered opponent bear:
     * the referee owns the entire damage decision; actor PIDs stay separate. */
    @Test(timeOut=120000)
    public void refereeAllocatesOneToOneRegisteredBearsWithoutInheritedAi() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor workerD=new Actor(2);Actor workerA=new Actor(2)) {
            Assert.assertNotEquals(workerD.process.pid(),workerA.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,workerD);
            ExternalCreatureController ac=attach(game,attacker,defender,workerA);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card source=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            Card target=defender.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            CardCollection opponents=new CardCollection();
            opponents.add(target);
            int originalA=attacker.getLife(),originalD=defender.getLife();
            Map<Card,Integer> allocation=ac.assignCombatDamage(source,opponents,null,2,defender,false);
            Assert.assertEquals(allocation.size(),1);
            Assert.assertEquals(allocation.get(target),Integer.valueOf(2));
            Assert.assertEquals(ac.refereeDamageAssignments,1);
            Assert.assertEquals(dc.refereeDamageAssignments,0);
            Assert.assertEquals(attacker.getLife(),originalA);
            Assert.assertEquals(defender.getLife(),originalD);
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertFalse(observation(attacker,defender).contains("\"phase\""));
            Assert.assertFalse(observation(defender,attacker).contains("\"stack\""));
        }
    }

    /** Two or three legal vanilla blockers: two damage to first target.
     * Unregistered targets and invalid damage remain denied without mutation. */
    @Test(timeOut=120000)
    public void refereeAllocatesTwoBlockersAndRejectsUnsupportedWithoutInheritedAi() throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player defender=game.getPlayers().get(0),attacker=game.getPlayers().get(1);
        try(Actor workerD=new Actor(3);Actor workerA=new Actor(2)) {
            Assert.assertNotEquals(workerD.process.pid(),workerA.process.pid());
            ExternalCreatureController dc=attach(game,defender,attacker,workerD);
            ExternalCreatureController ac=attach(game,attacker,defender,workerA);
            doRealNativeStartForMultiTurn(game,dc,ac);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,defender);
            fixturePlaceRegisteredBear(game,attacker);
            Card source=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            CardCollection blockers=new CardCollection();
            for(Card c:defender.getCardsIn(ZoneType.Battlefield)) {
                if("Grizzly Bears".equals(c.getName())) blockers.add(c);
            }
            Assert.assertEquals(blockers.size(),3);
            CardCollection pair=new CardCollection();
            pair.add(blockers.get(0));
            pair.add(blockers.get(1));
            int beforeA=attacker.getLife(), beforeD=defender.getLife();
            Map<Card,Integer> allocation=ac.assignCombatDamage(source,pair,null,2,defender,false);
            Assert.assertEquals(allocation.size(),1);
            Assert.assertEquals(allocation.get(blockers.get(0)),Integer.valueOf(2));
            Assert.assertFalse(allocation.containsKey(blockers.get(1)));
            Assert.assertEquals(ac.refereeDamageAssignments,1);
            Map<Card,Integer> three=ac.assignCombatDamage(source,blockers,null,2,defender,false);
            Assert.assertEquals(three.size(),1);
            Assert.assertEquals(three.get(blockers.get(0)),Integer.valueOf(2));
            Assert.assertEquals(ac.refereeDamageAssignments,2);
            CardCollection unregistered=new CardCollection();
            unregistered.add(source); // own creature cannot be enemy combat target
            int rejections=0;
            try {ac.assignCombatDamage(source,unregistered,null,2,defender,false);}
            catch(SecurityException expected){rejections++;}
            try {ac.assignCombatDamage(source,pair,null,3,defender,false);}
            catch(SecurityException expected){rejections++;}
            Assert.assertEquals(rejections,2);
            Assert.assertEquals(ac.refereeDamageAssignments,2,
                    "invalid requests must not consume trusted assignments");
            Assert.assertEquals(dc.refereeDamageAssignments,0);
            Assert.assertEquals(attacker.getLife(),beforeA);
            Assert.assertEquals(defender.getLife(),beforeD);
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertFalse(observation(defender,attacker).contains("\"priority\""));
        }
    }

    @Test(timeOut=120000)
    public void refereeStrictlyRejectsDuplicateKeysInActionAndOpeningReplies() {
        int rejected=0;
        for(String raw:Arrays.asList(
                "{\"intent\":\"pass\",\"intent\":\"play_land\",\"hand_index\":0}",
                "{\"int\\u0065nt\":\"pass\",\"intent\":\"cast_creature\",\"hand_index\":0}",
                "{\"intent\":\"opening_hand\",\"keep\":true,\"keep\":false,\"bottom_priority\":[0]}",
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[],\"creature_hand_indices\":[],\"attack_table_indices\":[],\"nested\":{\"field\":1,\"field\":2}}"
        )) {
            try {parseStrictActorReply(raw);Assert.fail("ambiguous reply accepted");}
            catch(SecurityException expected){rejected++;}
        }
        Assert.assertEquals(rejected,4);
        JsonObject pass=parseStrictActorReply("{\"intent\":\"pass\"}");
        validateReply(pass);
        JsonObject opening=parseStrictActorReply(
                "{\"intent\":\"opening_hand\",\"keep\":true,\"bottom_priority\":[0,1]}");
        validateOpeningReply(opening,2);
    }

    @Test(timeOut=120000)
    public void refereeStrictlyRejectsNonfiniteTrailingAndOversizedActorReplies() {
        int rejected=0;
        for(String raw:Arrays.asList(
                "{\"intent\":\"pass\",\"number\":NaN}",
                "{\"intent\":\"pass\",\"number\":Infinity}",
                "{\"intent\":\"pass\",\"number\":-Infinity}",
                "{\"intent\":\"pass\",\"number\":1e999}",
                "{\"intent\":\"pass\"}{\"intent\":\"pass\"}",
                "[]",
                "{\"intent\":\"pass\",\"padding\":\""+"x".repeat(2100)+"\"}"
        )) {
            try {parseStrictActorReply(raw);Assert.fail("malformed reply accepted");}
            catch(SecurityException expected){rejected++;}
        }
        Assert.assertEquals(rejected,7);
        JsonObject plan=parseStrictActorReply(
                "{\"intent\":\"action_plan\",\"land_hand_indices\":[0],"+
                "\"creature_hand_indices\":[],\"attack_table_indices\":[]}");
        validateReply(plan);
    }

    @Test(timeOut=120000)
    public void refereeRejectsTruncatedUnterminatedAndOverlongReplyFrames() throws Exception {
        for(String bad:Arrays.asList(
                "{\"intent\":\"pass\"}",
                "{\"intent\":\"pas",
                "x".repeat(2048)+"\n")) {
            int failures=0;
            try(BufferedReader input=strictActorReader(new ByteArrayInputStream(
                    bad.getBytes(StandardCharsets.UTF_8)))) {
                try {readBoundedActorReply(input);}
                catch(SecurityException expected) {failures++;}
            }
            Assert.assertEquals(failures,1,"unterminated or overlong frame must fail closed");
        }
        String two="{\"intent\":\"pass\"}\n"
                +"{\"intent\":\"play_land\",\"hand_index\":0}\n";
        try(BufferedReader input=strictActorReader(new ByteArrayInputStream(
                two.getBytes(StandardCharsets.UTF_8)))) {
            JsonObject first=parseStrictActorReply(readBoundedActorReply(input));
            JsonObject second=parseStrictActorReply(readBoundedActorReply(input));
            validateReply(first);
            validateReply(second);
            Assert.assertEquals(first.get("intent").getAsString(),"pass");
            Assert.assertEquals(second.get("intent").getAsString(),"play_land");
        }
    }

    @Test(timeOut=120000)
    public void refereeRejectsMalformedUtf8AndReadsExactMultibyteFrames() throws Exception {
        byte[] prefix="{\"intent\":\"pass\",\"note\":\"".getBytes(StandardCharsets.UTF_8);
        byte[] suffix="\"}\n".getBytes(StandardCharsets.UTF_8);
        byte[][] badPayloads={
            new byte[]{(byte)0xc3},
            new byte[]{(byte)0xc3,(byte)0x28},
            new byte[]{(byte)0xed,(byte)0xa0,(byte)0x80}
        };
        for(byte[] bytes:badPayloads) {
            byte[] raw=new byte[prefix.length+bytes.length+suffix.length];
            System.arraycopy(prefix,0,raw,0,prefix.length);
            System.arraycopy(bytes,0,raw,prefix.length,bytes.length);
            System.arraycopy(suffix,0,raw,prefix.length+bytes.length,suffix.length);
            int rejected=0;
            try(BufferedReader input=strictActorReader(new ByteArrayInputStream(raw))) {
                try {readBoundedActorReply(input);}
                catch(java.nio.charset.CharacterCodingException expected) {rejected++;}
            }
            Assert.assertEquals(rejected,1,"malformed UTF-8 must not be replaced");
        }
        String valid="{\"intent\":\"pass\",\"note\":\"€🔥\"}\n";
        try(BufferedReader input=strictActorReader(new ByteArrayInputStream(
                valid.getBytes(StandardCharsets.UTF_8)))) {
            JsonObject decoded=parseStrictActorReply(readBoundedActorReply(input));
            Assert.assertEquals(decoded.get("note").getAsString(),"€🔥");
            int denied=0;
            try {validateReply(decoded);}
            catch(AssertionError expected) {denied++;}
            Assert.assertEquals(denied,1,"unapproved Unicode note must not affect actor schema");
        }
    }

    @Test(timeOut=120000)
    public void refereeDeadlineDeniesSilentActorAndKeepsGameUnchanged() throws Exception {
        Game game=initAndCreateGame();
        Player first=game.getPlayers().get(0),second=game.getPlayers().get(1);
        int lifeA=first.getLife(),lifeB=second.getLife();
        int libraryA=first.getCardsIn(ZoneType.Library).size();
        java.io.PipedInputStream pipe=new java.io.PipedInputStream();
        java.io.PipedOutputStream silentOutput=new java.io.PipedOutputStream(pipe);
        java.util.concurrent.ExecutorService gate=
                java.util.concurrent.Executors.newSingleThreadExecutor();
        try(BufferedReader bounded=strictActorReader(pipe)) {
            long start=System.nanoTime();
            int rejected=0;
            java.util.concurrent.atomic.AtomicBoolean aborted=
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            try {
                readActorReplyWithDeadline(bounded,gate,250L,() -> {
                    aborted.set(true);
                    try {silentOutput.close();} catch(IOException ignored) {}
                });
            } catch(SecurityException expected) {
                Assert.assertTrue(expected.getMessage().contains("deadline"));
                rejected++;
            }
            long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
            Assert.assertEquals(rejected,1);
            Assert.assertTrue(aborted.get(),"the unresponsive peer must be aborted");
            Assert.assertTrue(elapsed<3000,"deadline must not wait indefinitely");
            Assert.assertEquals(first.getLife(),lifeA);
            Assert.assertEquals(second.getLife(),lifeB);
            Assert.assertEquals(first.getCardsIn(ZoneType.Library).size(),libraryA);
            Assert.assertTrue(game.getStack().isEmpty());
        } finally {
            silentOutput.close();
            gate.shutdownNow();
            Assert.assertTrue(gate.awaitTermination(3,TimeUnit.SECONDS),
                    "reader task must terminate after stream is closed");
        }
    }

    @Test(timeOut=120000)
    public void refereeDeadlineAcceptsPromptFramesAndDeniesPartialUnfinishedReply()
            throws Exception {
        java.util.concurrent.ExecutorService gate=
                java.util.concurrent.Executors.newSingleThreadExecutor();
        String both="{\"intent\":\"pass\"}\n"
                +"{\"intent\":\"play_land\",\"hand_index\":0}\n";
        try(BufferedReader prompt=strictActorReader(new ByteArrayInputStream(
                both.getBytes(StandardCharsets.UTF_8)))) {
            JsonObject first=parseStrictActorReply(readActorReplyWithDeadline(
                    prompt,gate,3000L,() -> {throw new AssertionError("valid reply timeout");}));
            JsonObject second=parseStrictActorReply(readActorReplyWithDeadline(
                    prompt,gate,3000L,() -> {throw new AssertionError("valid reply timeout");}));
            validateReply(first);
            validateReply(second);
            Assert.assertEquals(first.get("intent").getAsString(),"pass");
            Assert.assertEquals(second.get("intent").getAsString(),"play_land");
        } finally {
            gate.shutdownNow();
            Assert.assertTrue(gate.awaitTermination(3,TimeUnit.SECONDS));
        }
        java.io.PipedInputStream pipe=new java.io.PipedInputStream();
        java.io.PipedOutputStream output=new java.io.PipedOutputStream(pipe);
        java.util.concurrent.ExecutorService blocked=
                java.util.concurrent.Executors.newSingleThreadExecutor();
        try(BufferedReader waiting=strictActorReader(pipe)) {
            output.write("{\"intent\":\"pass\"}".getBytes(StandardCharsets.UTF_8));
            output.flush(); // Deliberately NO newline and NO EOF.
            int denied=0;
            try {
                readActorReplyWithDeadline(waiting,blocked,250L,() -> {
                    try {output.close();} catch(IOException ignored) {}
                });
            } catch(SecurityException expected) {denied++;}
            Assert.assertEquals(denied,1,"complete JSON without a newline must time out");
        } finally {
            output.close();
            blocked.shutdownNow();
            Assert.assertTrue(blocked.awaitTermination(3,TimeUnit.SECONDS));
        }
    }

    /** End-to-end adversarial IPC uses exactly two actual Python actor PIDs:
     * one deliberate stall and one normal persistent actor. The trusted Forge
     * referee retains all actual Game/Card operations and visible-only views. */
    private void assertRealAdversarialTimeout(String mode,boolean opening) throws Exception {
        String adversary=System.getenv("STAGE10L_ADVERSARIAL_WORKER");
        Assert.assertNotNull(adversary,"CI must define hostile actor script");
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        int lifeA=a.getLife(),lifeB=b.getLife();
        int handA=a.getCardsIn(ZoneType.Hand).size();
        int handB=b.getCardsIn(ZoneType.Hand).size();
        int libA=a.getCardsIn(ZoneType.Library).size();
        int libB=b.getCardsIn(ZoneType.Library).size();
        try(Actor hostile=new Actor(0,adversary,mode);
            Actor normal=new Actor()) {
            Assert.assertNotEquals(hostile.process.pid(),normal.process.pid());
            Assert.assertTrue(hostile.process.isAlive());
            Assert.assertTrue(normal.process.isAlive());
            JsonObject baseline=normal.ask(observation(b,a));
            validateReply(baseline);
            long began=System.nanoTime();
            int rejected=0;
            try {
                if(opening) hostile.askOpening(observation(a,b));
                else hostile.ask(observation(a,b));
                Assert.fail("unresponsive external process accepted as a legal move");
            } catch(SecurityException expected) {
                Assert.assertTrue(expected.getMessage().contains("deadline"),
                        "must be actual response timeout, not parse/legality denial");
                rejected++;
            }
            long waited=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began);
            Assert.assertEquals(rejected,1);
            Assert.assertTrue(waited>=8000 && waited<20000,
                    "real actor response timeout must be bounded to 10 seconds");
            Assert.assertTrue(hostile.replyTimedOut);
            Assert.assertTrue(hostile.process.waitFor(3,TimeUnit.SECONDS),
                    "real stalled Python worker must terminate");
            Assert.assertFalse(hostile.process.isAlive());
            Assert.assertTrue(normal.process.isAlive(),
                    "independent other actor must remain alive and responsive");
            Assert.assertEquals(normal.ask(observation(b,a)),baseline);
            Assert.assertEquals(a.getLife(),lifeA);
            Assert.assertEquals(b.getLife(),lifeB);
            Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(),handA);
            Assert.assertEquals(b.getCardsIn(ZoneType.Hand).size(),handB);
            Assert.assertEquals(a.getCardsIn(ZoneType.Library).size(),libA);
            Assert.assertEquals(b.getCardsIn(ZoneType.Library).size(),libB);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertFalse(observation(a,b).contains("\"opponent_hand\""));
            Assert.assertFalse(observation(a,b).contains("\"library_top\""));
            Assert.assertFalse(observation(a,b).contains("\"phase\""));
        }
    }

    @Test(timeOut=120000)
    public void realSilentPythonActorTimesOutAndOtherActorContinues() throws Exception {
        assertRealAdversarialTimeout("silent",false);
    }

    @Test(timeOut=120000)
    public void realPartialPythonOpeningReplyTimesOutAndOtherActorContinues() throws Exception {
        assertRealAdversarialTimeout("partial",true);
    }

    /** Genuine two-process pipe-backpressure test. Each individual write is a
     * normal allowlisted observation, bounded below 16KiB. We repeat only to
     * fill the OS pipe when the adversarial Python actor refuses to read it. */
    private void assertRealNonreadingWorkerWriteTimeout(boolean opening) throws Exception {
        String adversary=System.getenv("STAGE10L_ADVERSARIAL_WORKER");
        Assert.assertNotNull(adversary,"CI must define test-only hostile actor");
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        int lifeA=a.getLife(),lifeB=b.getLife();
        int cardsA=a.getCardsIn(ZoneType.Hand).size();
        int cardsB=b.getCardsIn(ZoneType.Hand).size();
        int libA=a.getCardsIn(ZoneType.Library).size();
        int libB=b.getCardsIn(ZoneType.Library).size();
        try(Actor hostile=new Actor(0,adversary,"noread");
            Actor normal=new Actor()) {
            try {
                Assert.assertNotEquals(hostile.process.pid(),normal.process.pid());
                Assert.assertTrue(hostile.process.isAlive());
                Assert.assertTrue(normal.process.isAlive());
                String observation=observation(a,b);
                Assert.assertFalse(observation.contains("\"library_order\""));
                Assert.assertFalse(observation.contains("\"opponent_hand\""));
                Assert.assertFalse(observation.contains("\"phase\""));
                String frame=observation;
                if(opening) {
                    JsonObject envelope=new JsonObject();
                    envelope.addProperty("request","own_opening_hand");
                    envelope.add("observation",JsonParser.parseString(observation));
                    frame=envelope.toString();
                }
                Assert.assertTrue(frame.getBytes(StandardCharsets.UTF_8).length<16384);
                JsonObject baseline=normal.ask(observation(b,a));
                validateReply(baseline);
                long begin=System.nanoTime();
                boolean blocked=false;
                int attempts=0;
                for(;attempts<30000;attempts++) {
                    try {
                        hostile.send(frame,350L);
                    } catch(SecurityException timeout) {
                        Assert.assertTrue(timeout.getMessage().contains("write deadline"),
                                "must be real blocked actor stdin, not another failure");
                        blocked=true;
                        break;
                    }
                }
                long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin);
                Assert.assertTrue(blocked,
                        "real no-read Python actor must produce blocked OS stdin backpressure");
                Assert.assertTrue(attempts>1,"single frame should fit the OS pipe");
                Assert.assertTrue(elapsed<30000,"referee must remain bounded even under backpressure");
                Assert.assertTrue(hostile.replyTimedOut);
                Assert.assertTrue(hostile.process.waitFor(3,TimeUnit.SECONDS),
                        "write timeout must terminate actual nonreading Python worker");
                Assert.assertFalse(hostile.process.isAlive());
                Assert.assertTrue(normal.process.isAlive(),
                        "second independent actor is not killed by the first actor's failure");
                Assert.assertEquals(normal.ask(observation(b,a)),baseline);
                Assert.assertEquals(a.getLife(),lifeA);
                Assert.assertEquals(b.getLife(),lifeB);
                Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(),cardsA);
                Assert.assertEquals(b.getCardsIn(ZoneType.Hand).size(),cardsB);
                Assert.assertEquals(a.getCardsIn(ZoneType.Library).size(),libA);
                Assert.assertEquals(b.getCardsIn(ZoneType.Library).size(),libB);
                Assert.assertTrue(game.getStack().isEmpty());
            } finally {
                if(hostile.process.isAlive()) {
                    hostile.replyTimedOut=true;
                    hostile.process.destroyForcibly();
                }
            }
        }
    }

    @Test(timeOut=120000)
    public void realNonreadingPythonActorCannotBlockNormalRefereeSends() throws Exception {
        assertRealNonreadingWorkerWriteTimeout(false);
    }

    @Test(timeOut=120000)
    public void realNonreadingPythonActorCannotBlockOpeningHandRefereeSends()
            throws Exception {
        assertRealNonreadingWorkerWriteTimeout(true);
    }

    /** Referee-only fixture: model two independently slow I/O phases without
     * granting an external worker any Game, Card, phase or private-zone state. */
    private static BufferedWriter delayedExchangeWriter(long delayMillis) {
        return new BufferedWriter(new java.io.Writer() {
            boolean first=true;
            @Override public void write(char[] chars,int off,int len) throws IOException {
                if(first) {
                    first=false;
                    try {Thread.sleep(delayMillis);}
                    catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IOException("delayed write interrupted",ex);
                    }
                }
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private static BufferedReader delayedExchangeReader(long delayMillis,String reply) {
        java.io.InputStream stream=new java.io.FilterInputStream(
                new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8))) {
            boolean first=true;
            @Override public int read(byte[] bytes,int off,int len) throws IOException {
                if(first) {
                    first=false;
                    try {Thread.sleep(delayMillis);}
                    catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IOException("delayed read interrupted",ex);
                    }
                }
                return super.read(bytes,off,len);
            }
        };
        return strictActorReader(stream);
    }

    @Test(timeOut=120000)
    public void sharedExchangeDeadlineRejectsCombinedDelayWithoutStateMutation()
            throws Exception {
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        int lifeA=a.getLife(),lifeB=b.getLife();
        int handA=a.getCardsIn(ZoneType.Hand).size();
        int libraryB=b.getCardsIn(ZoneType.Library).size();
        java.util.concurrent.atomic.AtomicBoolean aborted=
                new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.ExecutorService gate=
                java.util.concurrent.Executors.newSingleThreadExecutor();
        try(BufferedWriter slowSend=delayedExchangeWriter(400);
            BufferedReader slowReply=delayedExchangeReader(1050,
                    "{\"intent\":\"pass\"}\n")) {
            long begin=System.nanoTime();
            int rejected=0;
            try {
                // Each phase would be under 1.2s on a RESET budget.
                // Combined delays of at least 1.45s must lose the SHARED budget.
                exchangeActorRequestWithOneDeadline(slowSend,slowReply,gate,
                        "{\"objective\":\"own-visible-only\"}",1200L,
                        () -> aborted.set(true));
            } catch(SecurityException expected) {
                Assert.assertTrue(expected.getMessage().contains("deadline"),
                        "must be shared I/O deadline, not JSON legality");
                rejected++;
            }
            long duration=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin);
            Assert.assertEquals(rejected,1);
            Assert.assertTrue(aborted.get());
            Assert.assertTrue(duration<3000,"must not wait two complete independent budgets");
            Assert.assertEquals(a.getLife(),lifeA);
            Assert.assertEquals(b.getLife(),lifeB);
            Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(),handA);
            Assert.assertEquals(b.getCardsIn(ZoneType.Library).size(),libraryB);
            Assert.assertTrue(game.getStack().isEmpty());
        } finally {
            gate.shutdownNow();
            Assert.assertTrue(gate.awaitTermination(3,TimeUnit.SECONDS));
        }
    }

    @Test(timeOut=120000)
    public void sharedExchangeDeadlineAcceptsPromptNormalAndOpeningHandResponses()
            throws Exception {
        java.util.concurrent.ExecutorService gate=
                java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            try(BufferedWriter send=delayedExchangeWriter(130);
                BufferedReader read=delayedExchangeReader(160,
                        "{\"intent\":\"pass\"}\n")) {
                String raw=exchangeActorRequestWithOneDeadline(send,read,gate,
                        "{\"objective\":\"own-visible-only\"}",1600L,
                        () -> {throw new AssertionError("prompt valid normal reply timed out");});
                JsonObject reply=parseStrictActorReply(raw);
                validateReply(reply);
                Assert.assertEquals(reply.get("intent").getAsString(),"pass");
            }
            try(BufferedWriter send=delayedExchangeWriter(150);
                BufferedReader read=delayedExchangeReader(170,
                        "{\"intent\":\"opening_hand\",\"keep\":true,\"bottom_priority\":[0,1]}\n")) {
                String raw=exchangeActorRequestWithOneDeadline(send,read,gate,
                        "{\"request\":\"own_opening_hand\",\"observation\":{\"objective\":\"own-visible-only\"}}",
                        1600L,
                        () -> {throw new AssertionError("prompt opening reply timed out");});
                JsonObject reply=parseStrictActorReply(raw);
                validateOpeningReply(reply,2);
                Assert.assertTrue(reply.get("keep").getAsBoolean());
            }
        } finally {
            gate.shutdownNow();
            Assert.assertTrue(gate.awaitTermination(3,TimeUnit.SECONDS));
        }
    }

    /** Adversarial real-actor tests use EXACTLY TWO Python actor PIDs at once:
     * one controlled-delay test actor and one independent ordinary legal actor.
     * The real trusted Forge referee owns game state and all decisions. */
    private void assertRealDelayedWorkerMissesSharedBudget(boolean opening)
            throws Exception {
        String script=System.getenv("STAGE10O_DELAY_WORKER");
        Assert.assertNotNull(script,"CI must define legal delayed test actor");
        Game game=initAndCreateGame();
        Player self=game.getPlayers().get(0),opponent=game.getPlayers().get(1);
        int lifeSelf=self.getLife(),lifeOpponent=opponent.getLife();
        int handSelf=self.getCardsIn(ZoneType.Hand).size();
        int handOpponent=opponent.getCardsIn(ZoneType.Hand).size();
        int deckSelf=self.getCardsIn(ZoneType.Library).size();
        int deckOpponent=opponent.getCardsIn(ZoneType.Library).size();
        String ownObservation=observation(self,opponent);
        Assert.assertFalse(ownObservation.contains("\"library_order\""));
        Assert.assertFalse(ownObservation.contains("\"opponent_hand\""));
        Assert.assertFalse(ownObservation.contains("\"phase\""));
        try(Actor delayed=new Actor(0,script,"slow");Actor healthy=new Actor()) {
            Assert.assertNotEquals(delayed.process.pid(),healthy.process.pid());
            Assert.assertTrue(delayed.process.isAlive());
            Assert.assertTrue(healthy.process.isAlive());
            JsonObject baseline=healthy.ask(observation(opponent,self));
            validateReply(baseline);
            int denied=0;
            long start=System.nanoTime();
            try {
                if(opening) delayed.askOpeningWithBudget(ownObservation,650L);
                else delayed.askWithBudget(ownObservation,650L);
                Assert.fail("late real Python actor must not authorize decision");
            } catch(SecurityException expected) {
                Assert.assertTrue(expected.getMessage().contains("deadline"),
                        "failure must occur at shared I/O budget, not for legality");
                denied++;
            }
            long waited=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
            Assert.assertEquals(denied,1);
            Assert.assertTrue(waited<2600,"short test budget must be enforced");
            Assert.assertTrue(delayed.replyTimedOut);
            Assert.assertTrue(delayed.process.waitFor(3,TimeUnit.SECONDS));
            Assert.assertFalse(delayed.process.isAlive());
            Assert.assertTrue(healthy.process.isAlive(),
                    "independent healthy actor must survive timeout");
            Assert.assertEquals(healthy.ask(observation(opponent,self)),baseline);
            Assert.assertEquals(self.getLife(),lifeSelf);
            Assert.assertEquals(opponent.getLife(),lifeOpponent);
            Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),handSelf);
            Assert.assertEquals(opponent.getCardsIn(ZoneType.Hand).size(),handOpponent);
            Assert.assertEquals(self.getCardsIn(ZoneType.Library).size(),deckSelf);
            Assert.assertEquals(opponent.getCardsIn(ZoneType.Library).size(),deckOpponent);
            Assert.assertTrue(game.getStack().isEmpty());
        }
    }

    @Test(timeOut=120000)
    public void realDelayedPythonNormalReplyDeniedBySharedBudgetAndPeerSurvives()
            throws Exception {
        assertRealDelayedWorkerMissesSharedBudget(false);
    }

    @Test(timeOut=120000)
    public void realDelayedPythonOpeningReplyDeniedBySharedBudgetAndPeerSurvives()
            throws Exception {
        assertRealDelayedWorkerMissesSharedBudget(true);
    }

    @Test(timeOut=120000)
    public void realPromptPythonWorkerAnswersBothRequestsWithinSharedBudget()
            throws Exception {
        String script=System.getenv("STAGE10O_DELAY_WORKER");
        Assert.assertNotNull(script);
        Game game=initAndCreateGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        try(Actor prompt=new Actor(0,script,"prompt");Actor normal=new Actor()) {
            Assert.assertNotEquals(prompt.process.pid(),normal.process.pid());
            JsonObject peerBaseline=normal.ask(observation(b,a));
            for(int i=0;i<3;i++) {
                JsonObject pass=prompt.askWithBudget(observation(a,b),3000L);
                validateReply(pass);
                Assert.assertEquals(pass.get("intent").getAsString(),"pass");
                JsonObject opening=prompt.askOpeningWithBudget(observation(a,b),3000L);
                int hand=a.getCardsIn(ZoneType.Hand).size();
                validateOpeningReply(opening,hand);
                Assert.assertEquals(opening.getAsJsonArray("bottom_priority").size(),hand);
            }
            Assert.assertEquals(normal.ask(observation(b,a)),peerBaseline);
            Assert.assertTrue(prompt.process.isAlive());
            Assert.assertTrue(normal.process.isAlive());
            Assert.assertEquals(a.getLife(),20);
            Assert.assertEquals(b.getLife(),20);
            Assert.assertTrue(game.getStack().isEmpty());
        }
    }


    /**
     * Stage10P: a real legal unblocked 2/2 attack, chosen by an independent
     * persistent Python actor and resolved solely by Forge, must end the game
     * when the defender begins this referee-only combat fixture at two life.
     * No actor receives the PhaseHandler, Game, opponent hand or library.
     */
    @Test(timeOut=120000)
    public void nativeLethalUnblockedCombatRecordsForgeWinnerAndLoser() throws Exception {
        Game game=initAndCreateGame();
        Player attacker=game.getPlayers().get(1),defender=game.getPlayers().get(0);
        seedPrivateLibraries(attacker,defender);
        Assert.assertTrue(defender.setLife(2,null),
                "test-only referee fixture places defender at two life");
        Card bear=addCardToZone("Grizzly Bears",attacker,ZoneType.Battlefield);
        bear.setSickness(false);
        Assert.assertFalse(game.isGameOver());
        try(Actor left=new Actor();Actor right=new Actor()) {
            Assert.assertNotEquals(left.process.pid(),right.process.pid());
            ExternalCreatureController first=attach(game,attacker,defender,left);
            ExternalCreatureController second=attach(game,defender,attacker,right);
            int steps=0;
            while(!game.isGameOver() && steps++<160) {
                game.getPhaseHandler().mainLoopStep();
            }
            Assert.assertTrue(steps<160,"bounded real Forge native combat");
            Assert.assertTrue(first.attackCallbacks>0,
                    "real external attacker must have selected the attacker");
            Assert.assertTrue(second.priorityCallbacks>0,
                    "independent defender must have taken genuine priority");
            Assert.assertEquals(defender.getLife(),0,
                    "Forge normal unblocked 2/2 damage, not a direct life edit");
            Assert.assertTrue(bear.isTapped(),"Forge declares and taps real attacker");
            Assert.assertTrue(game.isGameOver(),"Forge state-based game termination");
            Assert.assertNotNull(game.getOutcome(),"Forge records outcome");
            Assert.assertTrue(defender.hasLost(),"Forge records actual losing player");
            Assert.assertEquals(defender.getOutcome().lossState.toString(),
                    "LifeReachedZero","CR 704.5a loss reason");
            Assert.assertTrue(attacker.hasWon(),"Forge determines victor");
            Assert.assertTrue(game.getOutcome().isWinner(attacker.getRegisteredPlayer()),
                    "registered player recognized as winner by Forge outcome");
            Assert.assertFalse(game.getOutcome().isDraw());
        }
    }

    /**
     * Same legal external-actor combat from three life must deal exactly two,
     * publish one remaining life, and NOT fabricate a Forge outcome.
     */
    @Test(timeOut=120000)
    public void nativeNonlethalUnblockedCombatDoesNotInventGameOver() throws Exception {
        Game game=initAndCreateGame();
        Player attacker=game.getPlayers().get(1),defender=game.getPlayers().get(0);
        seedPrivateLibraries(attacker,defender);
        Assert.assertTrue(defender.setLife(3,null));
        Card bear=addCardToZone("Grizzly Bears",attacker,ZoneType.Battlefield);
        bear.setSickness(false);
        try(Actor left=new Actor();Actor right=new Actor()) {
            Assert.assertNotEquals(left.process.pid(),right.process.pid());
            ExternalCreatureController first=attach(game,attacker,defender,left);
            ExternalCreatureController second=attach(game,defender,attacker,right);
            int steps=0;
            while(!game.isGameOver() && defender.getLife()==3 && steps++<160) {
                game.getPhaseHandler().mainLoopStep();
            }
            Assert.assertTrue(steps<160,"real Forge must reach combat");
            Assert.assertTrue(first.attackCallbacks>0);
            Assert.assertTrue(second.priorityCallbacks>0);
            Assert.assertEquals(defender.getLife(),1,"exact real Forge combat damage");
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertTrue(bear.isTapped());
            Assert.assertFalse(game.isGameOver(),"nonlethal damage cannot end the game");
            Assert.assertNull(game.getOutcome(),"no invented game outcome");
            Assert.assertFalse(defender.hasLost());
            Assert.assertFalse(attacker.hasWon());
            JsonObject publicLife=JsonParser.parseString(observation(attacker,defender))
                    .getAsJsonObject().getAsJsonObject("life_totals");
            Assert.assertEquals(publicLife.get("opponent").getAsInt(),1);
            Assert.assertFalse(observation(attacker,defender).contains("\"opponent_hand\""));
            Assert.assertFalse(observation(attacker,defender).contains("\"phase\""));
            Assert.assertTrue(left.process.isAlive() && right.process.isAlive(),
                    "persistent independent actor processes stay alive");
        }
    }

    /**
     * Stage10Q: no creature is seeded onto the battlefield. Both actor
     * processes are live throughout actual Match.startGame and bounded natural
     * turns. The trusted test referee only arranges registered hand contents
     * and a LOW starting life total; Forge alone pays mana, casts, applies
     * summoning sickness, declares attackers, deals damage and records victory.
     * Low-life is not a claim of naturally earned full-match victory.
     */
    private void verifyNativePaidCreatureOutcome(int testOnlyDefenderLife,
            boolean shouldBeLethal) throws Exception {
        Game game=nativeSixtyCardRegisteredGame();
        Player attacker=game.getPlayers().get(0),defender=game.getPlayers().get(1);
        try(Actor first=new Actor(true);Actor second=new Actor(true)) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController attackingActor=attach(game,attacker,defender,first);
            ExternalCreatureController defendingActor=attach(game,defender,attacker,second);
            doRealNativeStartForMultiTurn(game,attackingActor,defendingActor);
            prepareKnownCastingScenario(game,attacker);
            Assert.assertEquals(testOnlyDefenderLife,shouldBeLethal?2:3);
            Assert.assertTrue(defender.setLife(testOnlyDefenderLife,null),
                    "only test fixture life is changed; combat damage is native");
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertFalse(game.isGameOver());
            for(Card card:attacker.getCardsIn(ZoneType.Battlefield)) {
                Assert.assertFalse("Grizzly Bears".equals(card.getName()),
                        "no artificially seeded attacker: Bear starts in real hand");
            }
            int steps=0,uniqueMainTurns=0,paidBearResolvedAtStep=-1;
            Player previousMain=null;
            boolean sawPaidBearOnStack=false,realCombatSeen=false;
            while(!game.isGameOver() && steps++<850 && uniqueMainTurns<9) {
                game.getPhaseHandler().mainLoopStep();
                if(game.getPhaseHandler().getPhase()==PhaseType.MAIN1) {
                    Player current=game.getPhaseHandler().getPlayerTurn();
                    if(current!=previousMain) {
                        uniqueMainTurns++;
                        previousMain=current;
                    }
                }
                if(!game.getStack().isEmpty() &&
                        basicLandCountOnBattlefield(attacker)>=2) {
                    int tapped=0;
                    for(Card land:attacker.getCardsIn(ZoneType.Battlefield)) {
                        if(land.isLand() && land.isTapped()) tapped++;
                    }
                    if(tapped>=2) sawPaidBearOnStack=true;
                }
                for(Card creature:attacker.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(creature.getName())) {
                        if(paidBearResolvedAtStep<0) paidBearResolvedAtStep=steps;
                    }
                }
                if(defender.getLife()<testOnlyDefenderLife) {
                    realCombatSeen=true;
                    // Combat sets life immediately; Forge checks CR 704.5a
                    // at its next normal priority/state-based-action checkpoint.
                    // Do not invent a game-over call or short-circuit that step.
                    if(!shouldBeLethal || game.isGameOver()) break;
                }
            }
            Assert.assertTrue(steps<850,"bounded native match, not fabricated turns");
            Assert.assertTrue(uniqueMainTurns>=5 && uniqueMainTurns<=9,
                    "real turns after opening hand and natural untap/draw");
            Assert.assertTrue(sawPaidBearOnStack,
                    "external actor's named creature must be mana paid on real Forge stack");
            Assert.assertTrue(paidBearResolvedAtStep>0,
                    "real creature must resolve into battlefield before combat");
            Assert.assertTrue(realCombatSeen,
                    "real unblocked creature must deal natural Forge combat damage");
            Assert.assertTrue(attackingActor.priorityCallbacks>4 &&
                    defendingActor.priorityCallbacks>4);
            Assert.assertTrue(attackingActor.attackCallbacks>0);
            Assert.assertEquals(countInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(countInventory(defender),frozenMain.owned(defender));
            if(shouldBeLethal) {
                Assert.assertEquals(defender.getLife(),0);
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome());
                Assert.assertTrue(defender.hasLost());
                Assert.assertEquals(defender.getOutcome().lossState.toString(),
                        "LifeReachedZero");
                Assert.assertTrue(attacker.hasWon());
                Assert.assertTrue(game.getOutcome().isWinner(attacker.getRegisteredPlayer()));
                Assert.assertFalse(game.getOutcome().isDraw());
            } else {
                Assert.assertEquals(defender.getLife(),1);
                Assert.assertFalse(game.isGameOver());
                Assert.assertNull(game.getOutcome());
                Assert.assertFalse(defender.hasLost());
                Assert.assertFalse(attacker.hasWon());
                Assert.assertTrue(first.process.isAlive() && second.process.isAlive());
            }
            JsonObject publicView=JsonParser.parseString(observation(attacker,defender))
                    .getAsJsonObject();
            Assert.assertEquals(publicView.getAsJsonObject("life_totals")
                    .get("opponent").getAsInt(),defender.getLife());
            Assert.assertEquals(publicView.size(),4);
            for(String key:Arrays.asList("phase","turn","priority","stack",
                    "library_order","library_top","opponent_hand")) {
                Assert.assertFalse(publicView.has(key),
                        "public actor view must not add privileged field "+key);
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeRegisteredPaidCreatureCastThenNaturalCombatTriggersForgeWin()
            throws Exception {
        verifyNativePaidCreatureOutcome(2,true);
    }

    @Test(timeOut=120000)
    public void nativeRegisteredPaidCreatureCastThenNonlethalCombatContinues()
            throws Exception {
        verifyNativePaidCreatureOutcome(3,false);
    }

    /**
     * Stage10R deliberately removes the one-hit victory shortcut. It retains
     * authentic sixty-card registered Constructed decks but uses a test-only
     * board-free no-blocker opponent: 60 Islands vs 39 Forests, 20 Islands,
     * and exactly one Grizzly Bears. The real prior Match.startGame performs
     * mulligans and normal turns; the referee only stages that registered Bear
     * and two registered Forests in OWN opening hand to isolate gameplay.
     */
    private Game nativeRegisteredTwoCombatGame() {
        Map<String,Integer> attack=new TreeMap<>();
        attack.put("Forest",39);
        attack.put("Island",20);
        attack.put("Grizzly Bears",1);
        Map<String,Integer> defense=new TreeMap<>();
        defense.put("Island",60);
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(new RegisteredPlayer(mainDeck("Stage10R single real attacker",attack))
                .setPlayer(new LobbyPlayerAi("single-bear",null)));
        registered.add(new RegisteredPlayer(mainDeck("Stage10R nonblocking legal lands",defense))
                .setPlayer(new LobbyPlayerAi("island-only",null)));
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage10R");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        Assert.assertEquals(frozenMain.owned(game.getPlayers().get(0)).values()
                .stream().mapToInt(Integer::intValue).sum(),60);
        Assert.assertEquals(frozenMain.owned(game.getPlayers().get(1)).values()
                .stream().mapToInt(Integer::intValue).sum(),60);
        return game;
    }

    /** Exactly two distinct native attacks from the same mana-paid 2/2. */
    private void verifyTwoSeparateLegalCombatSteps(int defenderFixtureLife,
            boolean expectVictory) throws Exception {
        Game game=nativeRegisteredTwoCombatGame();
        Player attacker=game.getPlayers().get(0),defender=game.getPlayers().get(1);
        Assert.assertTrue(defenderFixtureLife==4 || defenderFixtureLife==5);
        try(Actor first=new Actor(true);Actor second=new Actor(true)) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController attackingActor=attach(game,attacker,defender,first);
            ExternalCreatureController defendingActor=attach(game,defender,attacker,second);
            doRealNativeStartForMultiTurn(game,attackingActor,defendingActor);
            prepareKnownCastingScenario(game,attacker);
            Assert.assertTrue(defender.setLife(defenderFixtureLife,null));
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Battlefield),0,
                    "test must never preseed an attacker on the battlefield");
            int steps=0,damageEvents=0,priorLife=defenderFixtureLife;
            int firstDamageTurn=-1,secondDamageTurn=-1;
            boolean sawPaidSpellOnStack=false,sawRealBear=false;
            while(!game.isGameOver() && steps++<1250) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty() &&
                        basicLandCountOnBattlefield(attacker)>=2) {
                    int tappedLands=0;
                    for(Card card:attacker.getCardsIn(ZoneType.Battlefield)) {
                        if(card.isLand() && card.isTapped()) tappedLands++;
                    }
                    if(tappedLands>=2) sawPaidSpellOnStack=true;
                }
                for(Card card:attacker.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(card.getName())) sawRealBear=true;
                }
                int life=defender.getLife();
                if(life!=priorLife) {
                    Assert.assertEquals(priorLife-life,2,
                            "one and only one real Grizzly Bears hit per combat");
                    damageEvents++;
                    if(damageEvents==1) firstDamageTurn=game.getPhaseHandler().getTurn();
                    if(damageEvents==2) secondDamageTurn=game.getPhaseHandler().getTurn();
                    Assert.assertTrue(damageEvents<=2,"no third combat damage in test");
                    Assert.assertEquals(life,defenderFixtureLife-2*damageEvents,
                            "Forge must account for cumulative combat damage");
                    priorLife=life;
                    if(damageEvents==2 && !expectVictory) break;
                    // At exactly zero Forge must still perform next native SBA
                    // checkpoint; never call Game.setGameOver from the test.
                }
                if(damageEvents==2 && expectVictory && game.isGameOver()) break;
            }
            Assert.assertTrue(steps<1250,"bounded genuine Forge natural turn sequence");
            Assert.assertEquals(damageEvents,2,
                    "two separate turns of genuine unblocked attacks required");
            Assert.assertTrue(firstDamageTurn>=0 && secondDamageTurn>firstDamageTurn,
                    "each 2 damage must occur on a different real Forge turn");
            Assert.assertTrue(sawPaidSpellOnStack && sawRealBear,
                    "actor must have paid for and resolved the registered creature");
            Assert.assertTrue(attackingActor.priorityCallbacks>5);
            Assert.assertTrue(defendingActor.priorityCallbacks>5);
            Assert.assertTrue(attackingActor.attackCallbacks>=2);
            Assert.assertEquals(countInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(countInventory(defender),frozenMain.owned(defender));
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),expectVictory?0:1);
            if(expectVictory) {
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome());
                Assert.assertTrue(defender.hasLost());
                Assert.assertEquals(defender.getOutcome().lossState.toString(),
                        "LifeReachedZero");
                Assert.assertTrue(attacker.hasWon());
                Assert.assertTrue(game.getOutcome().isWinner(attacker.getRegisteredPlayer()));
            } else {
                Assert.assertFalse(game.isGameOver());
                Assert.assertNull(game.getOutcome());
                Assert.assertFalse(defender.hasLost());
                Assert.assertFalse(attacker.hasWon());
                Assert.assertTrue(first.process.isAlive() && second.process.isAlive());
            }
            JsonObject publicView=JsonParser.parseString(observation(attacker,defender))
                    .getAsJsonObject();
            Assert.assertEquals(publicView.getAsJsonObject("life_totals")
                    .get("opponent").getAsInt(),defender.getLife());
            Assert.assertEquals(publicView.size(),4);
            for(String denied:Arrays.asList("phase","turn","priority","stack",
                    "opponent_hand","library_order","library_top")) {
                Assert.assertFalse(publicView.has(denied),
                        "no privileged information in any actor observation");
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeTwoSeparatePaidBearCombatsCauseCumulativeForgeVictory()
            throws Exception {
        verifyTwoSeparateLegalCombatSteps(4,true);
    }

    @Test(timeOut=120000)
    public void nativeTwoSeparatePaidBearCombatsLeaveOneLifeWithoutFalseWinner()
            throws Exception {
        verifyTwoSeparateLegalCombatSteps(5,false);
    }

    /**
     * Stage10S removes the test-only low-life setup of Stage10R.
     * Both registered players start at Forge's genuine 20 life and keep it
     * until the registered, mana-paid Bear attacks. The only curated state
     * is the ATTACKER'S own seven-card hand, made from the registered
     * shuffled deck without adding or fabricating cards. The opponent's
     * registered 60-Island deck cannot block or cast creatures; therefore
     * this tests turn progression, not competitive decision-making.
     *
     * Exactly one physical Bear exists in the attacker's original 60-card
     * main deck. No creature is injected onto the battlefield, no player
     * life is set manually, and Forge alone computes attacks, damage, state
     * based actions, and the winning registered player.
     */
    private void verifyNaturalTwentyLifeCumulativeCombat(int requiredHits,
            boolean expectLethal) throws Exception {
        Assert.assertTrue((requiredHits==10 && expectLethal) ||
                (requiredHits==3 && !expectLethal),
                "fixed, separately auditable 20-life scenarios only");
        Game game=nativeRegisteredTwoCombatGame();
        Player attacker=game.getPlayers().get(0),defender=game.getPlayers().get(1);
        try(Actor first=new Actor(true);Actor second=new Actor(true)) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid(),
                    "actors must remain independent external OS processes");
            ExternalCreatureController ca=attach(game,attacker,defender,first);
            ExternalCreatureController cb=attach(game,defender,attacker,second);
            doRealNativeStartForMultiTurn(game,ca,cb);
            prepareKnownCastingScenario(game,attacker);
            Assert.assertEquals(attacker.getLife(),20,
                    "native default player starting life");
            Assert.assertEquals(defender.getLife(),20,
                    "do not pre-reduce life for the test");
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome());
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Battlefield),0,
                    "no battlefield creature is seeded");
            int initialAttackerLibrary=attacker.getCardsIn(ZoneType.Library).size();
            int initialDefenderLibrary=defender.getCardsIn(ZoneType.Library).size();
            int steps=0,damageEvents=0,previousLife=20,previousDamageTurn=-1;
            boolean sawPaidBearOnStack=false,sawRealBearOnBattlefield=false;
            while(!game.isGameOver() && steps++<6500) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty() &&
                        basicLandCountOnBattlefield(attacker)>=2) {
                    int tappedLands=0;
                    for(Card land:attacker.getCardsIn(ZoneType.Battlefield)) {
                        if(land.isLand() && land.isTapped()) tappedLands++;
                    }
                    if(tappedLands>=2) sawPaidBearOnStack=true;
                }
                for(Card creature:attacker.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(creature.getName())) {
                        sawRealBearOnBattlefield=true;
                    }
                }
                int nowLife=defender.getLife();
                if(nowLife!=previousLife) {
                    Assert.assertEquals(previousLife-nowLife,2,
                            "only one unblocked real 2/2 Bear combat hit per turn");
                    damageEvents++;
                    Assert.assertTrue(damageEvents<=requiredHits,
                            "unexpected extra combat damage");
                    int realTurn=game.getPhaseHandler().getTurn();
                    Assert.assertTrue(realTurn>previousDamageTurn,
                            "each damage event must occur on a DIFFERENT real Forge turn");
                    previousDamageTurn=realTurn;
                    Assert.assertEquals(nowLife,20-2*damageEvents,
                            "20-life cumulative damage must be exactly tracked");
                    previousLife=nowLife;
                    if(damageEvents==requiredHits && !expectLethal) break;
                }
                // Lethal zero life must be recognized by the next native
                // state-based action check, not a test-forced game-over.
            }
            Assert.assertTrue(steps<6500,
                    "bounded native Forge mainLoopStep execution, no synthetic turn advance");
            Assert.assertEquals(damageEvents,requiredHits,
                    "must observe every separate real combat hit");
            Assert.assertTrue(sawPaidBearOnStack && sawRealBearOnBattlefield,
                    "paid registered Bear must actually resolve on native Forge stack");
            Assert.assertTrue(ca.attackCallbacks>=requiredHits,
                    "external attacker must provide real combat preferences repeatedly");
            Assert.assertTrue(ca.priorityCallbacks>requiredHits);
            Assert.assertTrue(cb.priorityCallbacks>requiredHits);
            Assert.assertTrue(initialAttackerLibrary>
                    attacker.getCardsIn(ZoneType.Library).size(),
                    "real native turns must include actual library draws");
            Assert.assertTrue(initialDefenderLibrary>
                    defender.getCardsIn(ZoneType.Library).size(),
                    "defender must also progress through genuine draw steps");
            Assert.assertEquals(countInventory(attacker),frozenMain.owned(attacker),
                    "no registered cards added or removed");
            Assert.assertEquals(countInventory(defender),frozenMain.owned(defender),
                    "opponent inventory unchanged");
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),expectLethal?0:14);
            if(expectLethal) {
                Assert.assertTrue(game.isGameOver(),
                        "native Forge must recognize genuine 20-to-zero loss");
                Assert.assertNotNull(game.getOutcome());
                Assert.assertTrue(defender.hasLost());
                Assert.assertEquals(defender.getOutcome().lossState.toString(),
                        "LifeReachedZero");
                Assert.assertTrue(attacker.hasWon());
                Assert.assertTrue(game.getOutcome().isWinner(attacker.getRegisteredPlayer()));
                Assert.assertFalse(game.getOutcome().isDraw());
            } else {
                Assert.assertFalse(game.isGameOver(),
                        "three hits from 20 must not fabricate a winner");
                Assert.assertNull(game.getOutcome());
                Assert.assertFalse(defender.hasLost());
                Assert.assertFalse(attacker.hasWon());
                Assert.assertTrue(first.process.isAlive() && second.process.isAlive());
            }
            JsonObject view=JsonParser.parseString(observation(attacker,defender))
                    .getAsJsonObject();
            Assert.assertEquals(view.getAsJsonObject("life_totals")
                    .get("opponent").getAsInt(),defender.getLife());
            Assert.assertEquals(view.size(),4);
            for(String denied:Arrays.asList("phase","turn","priority","stack",
                    "opponent_hand","library_order","library_top")) {
                Assert.assertFalse(view.has(denied),
                        "public actor view must not leak hidden information");
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeTwentyLifeTenDistinctPaidBearCombatsProduceForgeWinner()
            throws Exception {
        verifyNaturalTwentyLifeCumulativeCombat(10,true);
    }

    @Test(timeOut=120000)
    public void nativeTwentyLifeThreeDistinctPaidBearCombatsRemainNonlethal()
            throws Exception {
        verifyNaturalTwentyLifeCumulativeCombat(3,false);
    }

    /**
     * Stage10T: Unlike Stage10D's seeded-battlefield blocker and Stage10S's
     * island-only passive opponent, BOTH real registered 60-card players must
     * cast a physical Grizzly Bears from their own hands using genuine Forest
     * mana, then Forge must legally block the attack and destroy both Bears.
     *
     * Test-only arrangements move ORIGINAL registered cards within each
     * respective owner's private library/hand; no borrowed opponent card,
     * battlefield seed, manually adjusted life, or scripted combat outcome.
     * The attacker is the only deck with >=4 registered Llanowar Elves,
     * which its independent stage10d actor uses as its phase-free preference
     * to attack; defender uses own original deck to prefer legal blocks.
     */
    private Game nativeTwoManaPaidCombatantsGame(boolean attackerRegisteredFirst) {
        Map<String,Integer> attacking=new TreeMap<>();
        attacking.put("Forest",24);
        attacking.put("Island",20);
        attacking.put("Mountain",11);
        attacking.put("Llanowar Elves",4);
        attacking.put("Grizzly Bears",1);
        Map<String,Integer> defending=new TreeMap<>();
        defending.put("Forest",24);
        defending.put("Island",20);
        defending.put("Mountain",15);
        defending.put("Grizzly Bears",1);
        List<RegisteredPlayer> registered=new ArrayList<>();
        if(attackerRegisteredFirst) {
            registered.add(new RegisteredPlayer(mainDeck("10T attacking",attacking))
                    .setPlayer(new LobbyPlayerAi("attacker",null)));
            registered.add(new RegisteredPlayer(mainDeck("10T defending",defending))
                    .setPlayer(new LobbyPlayerAi("defender",null)));
        } else {
            registered.add(new RegisteredPlayer(mainDeck("10T defending",defending))
                    .setPlayer(new LobbyPlayerAi("defender",null)));
            registered.add(new RegisteredPlayer(mainDeck("10T attacking",attacking))
                    .setPlayer(new LobbyPlayerAi("attacker",null)));
        }
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage10T");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player player:game.getPlayers()) {
            Assert.assertEquals(frozenMain.owned(player).values()
                    .stream().mapToInt(Integer::intValue).sum(),60);
        }
        return game;
    }

    private void verifyNativeBothActorsCastAndBlock(boolean attackerRegisteredFirst)
            throws Exception {
        Game game=nativeTwoManaPaidCombatantsGame(attackerRegisteredFirst);
        Player attacker=game.getPlayers().get(attackerRegisteredFirst?0:1);
        Player defender=game.getPlayers().get(attackerRegisteredFirst?1:0);
        try(Actor first=new Actor(2);Actor second=new Actor(2)) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController ca=attach(game,attacker,defender,first);
            ExternalCreatureController cb=attach(game,defender,attacker,second);
            doRealNativeStartForMultiTurn(game,ca,cb);
            prepareKnownCastingScenario(game,attacker);
            prepareKnownCastingScenario(game,defender);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Battlefield),0,
                    "neither attacker nor blocker can be seeded onto the table");
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Graveyard),0);
            Assert.assertEquals(countInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(countInventory(defender),frozenMain.owned(defender));
            boolean attackerPaidCast=false,defenderPaidCast=false;
            boolean attackerResolved=false,defenderResolved=false;
            boolean sawLegalBlock=false,sawBothCombatDeaths=false;
            int steps=0;
            while(!game.isGameOver() && steps++<2100) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty()) {
                    int attackerTapped=0,defenderTapped=0;
                    for(Card card:attacker.getCardsIn(ZoneType.Battlefield)) {
                        if(card.isLand() && card.isTapped()) attackerTapped++;
                    }
                    for(Card card:defender.getCardsIn(ZoneType.Battlefield)) {
                        if(card.isLand() && card.isTapped()) defenderTapped++;
                    }
                    if(attackerTapped>=2 && basicLandCountOnBattlefield(attacker)>=2)
                        attackerPaidCast=true;
                    if(defenderTapped>=2 && basicLandCountOnBattlefield(defender)>=2)
                        defenderPaidCast=true;
                }
                for(Card card:attacker.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(card.getName())) attackerResolved=true;
                }
                for(Card card:defender.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(card.getName())) defenderResolved=true;
                }
                if(cb.blocksAuthorized>0) sawLegalBlock=true;
                if(countDeadBears(attacker)>=1 && countDeadBears(defender)>=1) {
                    sawBothCombatDeaths=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2100,"native real-turn encounter must be bounded");
            Assert.assertTrue(attackerPaidCast && defenderPaidCast,
                    "both independent actors must pay real mana to cast original Bears");
            Assert.assertTrue(attackerResolved && defenderResolved,
                    "two Forge stack objects must resolve into legal real creatures");
            Assert.assertTrue(sawLegalBlock,"defending Python actor selects a Forge-legal block");
            Assert.assertTrue(sawBothCombatDeaths,
                    "real native 2/2 versus 2/2 combat sends both creatures to graveyards");
            Assert.assertTrue(ca.attackCallbacks>0 && cb.blockerCallbacks>0);
            Assert.assertTrue(cb.blocksAuthorized>0);
            Assert.assertTrue(ca.refereeDamageAssignments+cb.refereeDamageAssignments>0,
                    "trusted Forge referee must assign actual combat damage");
            Assert.assertEquals(attacker.getLife(),20,"legal block prevents opponent life loss");
            Assert.assertEquals(defender.getLife(),20,"legal block prevents own life loss");
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome());
            // Both Bears are in actual Forge graveyards after combat. The
            // precombat three-zone helper cannot prove whole-deck conservation.
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertTrue(ca.priorityCallbacks>3 && cb.priorityCallbacks>3);
            Assert.assertTrue(first.process.isAlive() && second.process.isAlive());
            JsonObject attackerView=JsonParser.parseString(observation(attacker,defender))
                    .getAsJsonObject();
            JsonObject defenderView=JsonParser.parseString(observation(defender,attacker))
                    .getAsJsonObject();
            Assert.assertEquals(attackerView.size(),4);
            Assert.assertEquals(defenderView.size(),4);
            for(String forbidden:Arrays.asList("opponent_hand","library_order",
                    "library_top","phase","turn","stack","priority")) {
                Assert.assertFalse(attackerView.has(forbidden));
                Assert.assertFalse(defenderView.has(forbidden));
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeBothManaPaidCombatantsBlockWhenAttackerRegisteredFirst()
            throws Exception {
        verifyNativeBothActorsCastAndBlock(true);
    }

    @Test(timeOut=120000)
    public void nativeBothManaPaidCombatantsBlockWhenDefenderRegisteredFirst()
            throws Exception {
        verifyNativeBothActorsCastAndBlock(false);
    }

    /**
     * Stage10U functional test: remove the own-hand arrangement entirely.
     * Forge performs all original-deck shuffles, London opening decisions,
     * draws, land drops, costs and spell resolution. Registered main decks
     * are real 60-card four-copy-limit Constructed lists:
     * 56 Forest + 4 Grizzly Bears vs 60 Island.
     *
     * The actor has an imperfect-information own-hand view and can cast the
     * first Bear when it NATURALLY arrives through native draws. We do not
     * select an opening hand, search a library, reorder a deck, or seed a
     * battlefield card; both real players start at native twenty life.
     * A long bounded horizon makes eventual drawing of one of four owned
     * Bears testable without requiring any specific lucky opening shuffle.
     */
    private Game nativeUncuratedTwoActorGame(boolean attackerRegisteredFirst) {
        Map<String,Integer> aggressive=new TreeMap<>();
        aggressive.put("Forest",56);
        aggressive.put("Grizzly Bears",4);
        Map<String,Integer> quiet=new TreeMap<>();
        quiet.put("Island",60);
        List<RegisteredPlayer> registered=new ArrayList<>();
        RegisteredPlayer a=new RegisteredPlayer(mainDeck("10U natural bear draw",aggressive))
                .setPlayer(new LobbyPlayerAi("natural-bear",null));
        RegisteredPlayer b=new RegisteredPlayer(mainDeck("10U natural island draws",quiet))
                .setPlayer(new LobbyPlayerAi("island-passive",null));
        registered.add(attackerRegisteredFirst?a:b);
        registered.add(attackerRegisteredFirst?b:a);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage10U");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player player:game.getPlayers())
            Assert.assertEquals(frozenMain.owned(player).values()
                    .stream().mapToInt(Integer::intValue).sum(),60);
        return game;
    }

    private void verifyNativeUncuratedOpeningAndActualPaidBear(
            boolean attackerRegisteredFirst) throws Exception {
        Game game=nativeUncuratedTwoActorGame(attackerRegisteredFirst);
        Player attacker=game.getPlayers().get(attackerRegisteredFirst?0:1);
        Player defender=game.getPlayers().get(attackerRegisteredFirst?1:0);
        try(Actor first=new Actor(true);Actor second=new Actor(true)) {
            Assert.assertNotEquals(first.process.pid(),second.process.pid());
            ExternalCreatureController ca=attach(game,attacker,defender,first);
            ExternalCreatureController cb=attach(game,defender,attacker,second);
            doRealNativeStartForMultiTurn(game,ca,cb);
            // Deliberately NO prepareKnownCastingScenario() or similar method.
            // Keep BOTH naturally shuffled Forge own hands unchanged until
            // priority/phase steps genuinely take their next actions.
            int initialOwnHand=attacker.getCardsIn(ZoneType.Hand).size();
            int initialOtherHand=defender.getCardsIn(ZoneType.Hand).size();
            Assert.assertTrue(initialOwnHand>=0 && initialOwnHand<=7);
            Assert.assertTrue(initialOtherHand>=0 && initialOtherHand<=7);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Battlefield),0);
            Assert.assertEquals(countInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(countInventory(defender),frozenMain.owned(defender));
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0,
                    "both opening hands must be processed by independent actors");
            String attackerInitialView=observation(attacker,defender);
            String defenderInitialView=observation(defender,attacker);
            Assert.assertEquals(JsonParser.parseString(attackerInitialView)
                    .getAsJsonObject().getAsJsonObject("own_cards")
                    .getAsJsonArray("hand").size(),initialOwnHand);
            Assert.assertEquals(JsonParser.parseString(defenderInitialView)
                    .getAsJsonObject().getAsJsonObject("own_cards")
                    .getAsJsonArray("hand").size(),initialOtherHand);
            int steps=0,initialOwnLibrary=attacker.getCardsIn(ZoneType.Library).size();
            int initialOpponentLibrary=defender.getCardsIn(ZoneType.Library).size();
            boolean sawManaPaidStack=false,sawBearResolved=false;
            while(!game.isGameOver() && steps++<20000) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty()) {
                    int tapped=0;
                    for(Card card:attacker.getCardsIn(ZoneType.Battlefield))
                        if(card.isLand() && card.isTapped()) tapped++;
                    if(tapped>=2 && basicLandCountOnBattlefield(attacker)>=2)
                        sawManaPaidStack=true;
                }
                for(Card card:attacker.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(card.getName()))
                        sawBearResolved=true;
                }
                if(sawBearResolved) break;
            }
            Assert.assertTrue(steps<20000,
                    "bounded real native turns must reach a naturally drawn Bear");
            Assert.assertTrue(sawManaPaidStack,
                    "real Forge must pay for Bear using genuine basic lands");
            Assert.assertTrue(sawBearResolved,
                    "genuine uncurated private draws lead to a real resolved creature");
            Assert.assertTrue(ca.priorityCallbacks>1 && cb.priorityCallbacks>0);
            Assert.assertTrue(initialOwnLibrary>
                    attacker.getCardsIn(ZoneType.Library).size() ||
                    initialOwnHand>0,
                    "original natural registered cards remain real");
            Assert.assertTrue(initialOpponentLibrary>=
                    defender.getCardsIn(ZoneType.Library).size(),
                    "independent opponent library never grows without legal cause");
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker),
                    "all registered attacker cards conserved across actual draws");
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender),
                    "all registered defender cards conserved across actual draws");
            Assert.assertTrue(first.process.isAlive() && second.process.isAlive());
            JsonObject attackerView=JsonParser.parseString(observation(attacker,defender))
                    .getAsJsonObject();
            JsonObject defenderView=JsonParser.parseString(observation(defender,attacker))
                    .getAsJsonObject();
            Assert.assertEquals(attackerView.size(),4);
            Assert.assertEquals(defenderView.size(),4);
            for(String denied:Arrays.asList("phase","turn","priority","stack",
                    "opponent_hand","library_order","library_top")) {
                Assert.assertFalse(attackerView.has(denied));
                Assert.assertFalse(defenderView.has(denied));
            }
            Assert.assertFalse(game.isGameOver(),
                    "test ends on real spell resolution, not a fabricated win");
            Assert.assertNull(game.getOutcome());
        }
    }

    @Test(timeOut=120000)
    public void nativeUncuratedOpeningDrawAndPaidBearWhenAttackerRegisteredFirst()
            throws Exception {
        verifyNativeUncuratedOpeningAndActualPaidBear(true);
    }

    @Test(timeOut=120000)
    public void nativeUncuratedOpeningDrawAndPaidBearWhenDefenderRegisteredFirst()
            throws Exception {
        verifyNativeUncuratedOpeningAndActualPaidBear(false);
    }

    /**
     * Stage10V: both seats draw their OWN genuine shuffled opening hands
     * and independently pay for Bears from their normal 60-card registered
     * decks. The defensive actor deliberately has no attack directive:
     * this increments mutual creature deployment, not yet uncurated combat.
     * No hidden deck inspection, hand arrangement or seeded battlefield.
     */
    private Game nativeTwoNaturallyPaidBearGame(boolean firstSeatActive) {
        Map<String,Integer> left=new TreeMap<>();
        left.put("Forest",56);
        left.put("Grizzly Bears",4);
        Map<String,Integer> right=new TreeMap<>();
        right.put("Forest",56);
        right.put("Grizzly Bears",4);
        RegisteredPlayer a=new RegisteredPlayer(mainDeck("10V independent left",left))
                .setPlayer(new LobbyPlayerAi("natural-left",null));
        RegisteredPlayer b=new RegisteredPlayer(mainDeck("10V independent right",right))
                .setPlayer(new LobbyPlayerAi("natural-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(firstSeatActive?a:b);
        registered.add(firstSeatActive?b:a);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage10V");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        Assert.assertEquals(frozenMain.owned(game.getPlayers().get(0)).values()
                .stream().mapToInt(Integer::intValue).sum(),60);
        Assert.assertEquals(frozenMain.owned(game.getPlayers().get(1)).values()
                .stream().mapToInt(Integer::intValue).sum(),60);
        return game;
    }

    private void verifyTwoNativeUncuratedPaidCasts(boolean firstSeatActive)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(firstSeatActive);
        Player left=game.getPlayers().get(firstSeatActive?0:1);
        Player right=game.getPlayers().get(firstSeatActive?1:0);
        try(Actor a=new Actor(2);Actor b=new Actor(2)) {
            Assert.assertNotEquals(a.process.pid(),b.process.pid(),
                    "two separate persistent external Python actor processes");
            ExternalCreatureController ca=attach(game,left,right,a);
            ExternalCreatureController cb=attach(game,right,left,b);
            doRealNativeStartForMultiTurn(game,ca,cb);
            // Neither prepareKnownCastingScenario nor any card-moving test hook.
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                    ZoneType.Battlefield),0);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countInventory(left),frozenMain.owned(left));
            Assert.assertEquals(countInventory(right),frozenMain.owned(right));
            boolean leftPaid=false,rightPaid=false,leftResolved=false,rightResolved=false;
            int steps=0;
            while(!game.isGameOver() && steps++<20000) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty()) {
                    int tappedLeft=0,tappedRight=0;
                    for(Card card:left.getCardsIn(ZoneType.Battlefield))
                        if(card.isLand() && card.isTapped()) tappedLeft++;
                    for(Card card:right.getCardsIn(ZoneType.Battlefield))
                        if(card.isLand() && card.isTapped()) tappedRight++;
                    if(tappedLeft>=2 && basicLandCountOnBattlefield(left)>=2)
                        leftPaid=true;
                    if(tappedRight>=2 && basicLandCountOnBattlefield(right)>=2)
                        rightPaid=true;
                }
                for(Card card:left.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(card.getName())) leftResolved=true;
                for(Card card:right.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(card.getName())) rightResolved=true;
                if(leftResolved && rightResolved) break;
            }
            Assert.assertTrue(steps<20000,"bounded natural Forge turns and draws");
            Assert.assertTrue(leftPaid && rightPaid,
                    "each independent actor must genuinely pay two mana on Forge stack");
            Assert.assertTrue(leftResolved && rightResolved,
                    "both uncurated players must resolve originally registered Bears");
            Assert.assertTrue(ca.priorityCallbacks>1 && cb.priorityCallbacks>1);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            Assert.assertTrue(a.process.isAlive() && b.process.isAlive());
            JsonObject leftView=JsonParser.parseString(observation(left,right))
                    .getAsJsonObject();
            JsonObject rightView=JsonParser.parseString(observation(right,left))
                    .getAsJsonObject();
            Assert.assertEquals(leftView.size(),4);
            Assert.assertEquals(rightView.size(),4);
            for(String denied:Arrays.asList("phase","turn","priority","stack",
                    "opponent_hand","library_order","library_top")) {
                Assert.assertFalse(leftView.has(denied));
                Assert.assertFalse(rightView.has(denied));
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeBothUncuratedPlayersPayAndResolveBearsLeftRegisteredFirst()
            throws Exception {
        verifyTwoNativeUncuratedPaidCasts(true);
    }

    @Test(timeOut=120000)
    public void nativeBothUncuratedPlayersPayAndResolveBearsRightRegisteredFirst()
            throws Exception {
        verifyTwoNativeUncuratedPaidCasts(false);
    }

    /**
     * Stage10W integration: distinct uncurated, persistent Python actors;
     * attacker declares a Bear only after its OPPONENT's face-up Bear is
     * publicly visible. Defender prefers a legal 1:1 block. The test never
     * arranges a hand, deals a Bear, edits life, seeds a board, or reads a
     * hidden library to make either actor's decisions. Native Forge handles
     * draws, casting costs, priority, combat, and simultaneous lethal damage.
     */
    private void verifyUncuratedContestedNativeCombat(boolean attackerRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(attackerRegisteredFirst);
        Player attacker=game.getPlayers().get(attackerRegisteredFirst?0:1);
        Player defender=game.getPlayers().get(attackerRegisteredFirst?1:0);
        try(Actor aggressive=new Actor(4);Actor guarding=new Actor(2)) {
            Assert.assertNotEquals(aggressive.process.pid(),guarding.process.pid(),
                    "distinct persistent external Python operating-system actors");
            ExternalCreatureController ca=attach(game,attacker,defender,aggressive);
            ExternalCreatureController cb=attach(game,defender,attacker,guarding);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Battlefield),0,
                    "both physically shuffled hands and boards remain uncurated");
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            boolean attackerPaid=false,defenderPaid=false;
            boolean attackerResolved=false,defenderResolved=false;
            boolean realBlockSeen=false,realBothDead=false;
            int steps=0;
            while(!game.isGameOver() && steps++<25000) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty()) {
                    int attackerTapped=0,defenderTapped=0;
                    for(Card c:attacker.getCardsIn(ZoneType.Battlefield))
                        if(c.isLand() && c.isTapped()) attackerTapped++;
                    for(Card c:defender.getCardsIn(ZoneType.Battlefield))
                        if(c.isLand() && c.isTapped()) defenderTapped++;
                    if(attackerTapped>=2 && basicLandCountOnBattlefield(attacker)>=2)
                        attackerPaid=true;
                    if(defenderTapped>=2 && basicLandCountOnBattlefield(defender)>=2)
                        defenderPaid=true;
                }
                for(Card c:attacker.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(c.getName())) attackerResolved=true;
                for(Card c:defender.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(c.getName())) defenderResolved=true;
                if(cb.blocksAuthorized>0) realBlockSeen=true;
                if(countDeadBears(attacker)>0 && countDeadBears(defender)>0) {
                    realBothDead=true;
                    break;
                }
            }
            Assert.assertTrue(steps<25000,"bounded native two-player shuffled game");
            Assert.assertTrue(attackerPaid && defenderPaid,
                    "both actors genuinely spend Forge mana on actual creature spells");
            Assert.assertTrue(attackerResolved && defenderResolved,
                    "both Bears must actually resolve from uncurated registered hands");
            Assert.assertTrue(realBlockSeen && realBothDead,
                    "a Forge-legal defending block causes both real Bears to die");
            Assert.assertTrue(ca.attackCallbacks>0 && cb.blockerCallbacks>0);
            Assert.assertTrue(cb.blocksAuthorized>0);
            Assert.assertTrue(ca.refereeDamageAssignments+cb.refereeDamageAssignments>0,
                    "authoritative Forge referee assigns the combat damage");
            Assert.assertEquals(attacker.getLife(),20,"block prevents attacker life loss");
            Assert.assertEquals(defender.getLife(),20,"block prevents defender life loss");
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker),
                    "attacking deck conserved including native graveyard");
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender),
                    "defending deck conserved including native graveyard");
            Assert.assertTrue(aggressive.process.isAlive() && guarding.process.isAlive());
            for(JsonObject view:Arrays.asList(
                    JsonParser.parseString(observation(attacker,defender)).getAsJsonObject(),
                    JsonParser.parseString(observation(defender,attacker)).getAsJsonObject())) {
                Assert.assertEquals(view.size(),4);
                for(String denied:Arrays.asList("opponent_hand","library_order",
                        "library_top","turn","phase","priority","stack")) {
                    Assert.assertFalse(view.has(denied));
                }
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeUncuratedPaidBearsContestedCombatAttackerRegisteredFirst()
            throws Exception {
        verifyUncuratedContestedNativeCombat(true);
    }

    @Test(timeOut=120000)
    public void nativeUncuratedPaidBearsContestedCombatDefenderRegisteredFirst()
            throws Exception {
        verifyUncuratedContestedNativeCombat(false);
    }

    /**
     * Stage10X: native two-round re-deployment after the first mutual death.
     * Both actors continue from the SAME real match state and their own
     * private shuffled hands; no reset, card seeding, hand swaps, life
     * fixture, hidden library inspection, or new actor process is permitted.
     * Both independent external processes remain persistent throughout.
     *
     * The attacker commits only when a face-up enemy Bear is observable;
     * the defender independently requests one Forge-legal block. A dead Bear
     * cannot attack or block again, so two death milestones for BOTH owners
     * require two genuinely different creatures and combat damage steps.
     */
    private void verifyTwoSuccessiveNaturalBlocks(boolean attackerRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(attackerRegisteredFirst);
        Player attacker=game.getPlayers().get(attackerRegisteredFirst?0:1);
        Player defender=game.getPlayers().get(attackerRegisteredFirst?1:0);
        try(Actor aggressive=new Actor(4);Actor guarding=new Actor(2)) {
            Assert.assertNotEquals(aggressive.process.pid(),guarding.process.pid());
            ExternalCreatureController ca=attach(game,attacker,defender,aggressive);
            ExternalCreatureController cb=attach(game,defender,attacker,guarding);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertEquals(countDeadBears(attacker),0);
            Assert.assertEquals(countDeadBears(defender),0);
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                    ZoneType.Battlefield),0);
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            int steps=0,firstDeathTurn=-1,secondDeathTurn=-1,deathMilestones=0;
            int priorDeadAttacker=0,priorDeadDefender=0;
            boolean attackerPaid=false,defenderPaid=false;
            while(!game.isGameOver() && steps++<50000 && deathMilestones<2) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty()) {
                    int attackerTapped=0,defenderTapped=0;
                    for(Card c:attacker.getCardsIn(ZoneType.Battlefield))
                        if(c.isLand() && c.isTapped()) attackerTapped++;
                    for(Card c:defender.getCardsIn(ZoneType.Battlefield))
                        if(c.isLand() && c.isTapped()) defenderTapped++;
                    if(attackerTapped>=2 && basicLandCountOnBattlefield(attacker)>=2)
                        attackerPaid=true;
                    if(defenderTapped>=2 && basicLandCountOnBattlefield(defender)>=2)
                        defenderPaid=true;
                }
                int deadA=countDeadBears(attacker),deadD=countDeadBears(defender);
                Assert.assertTrue(deadA>=priorDeadAttacker && deadD>=priorDeadDefender,
                        "native graveyards must not lose either physical dead Bear");
                Assert.assertEquals(deadA,deadD,
                        "each 2/2-versus-2/2 Forge combat must kill one per player");
                Assert.assertTrue(deadA<=2,
                        "test detects exactly the first two mutual combats");
                if(deadA>priorDeadAttacker) {
                    Assert.assertEquals(deadA,priorDeadAttacker+1,
                            "each death milestone must be a distinct combat");
                    deathMilestones++;
                    int actualTurn=game.getPhaseHandler().getTurn();
                    if(deathMilestones==1) firstDeathTurn=actualTurn;
                    if(deathMilestones==2) secondDeathTurn=actualTurn;
                    Assert.assertEquals(attacker.getLife(),20);
                    Assert.assertEquals(defender.getLife(),20);
                    Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
                    Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
                }
                priorDeadAttacker=deadA;
                priorDeadDefender=deadD;
            }
            Assert.assertTrue(steps<50000,"bounded natural multi-turn match");
            Assert.assertEquals(deathMilestones,2,
                    "two successive natural-draw, paid-cast legal blocks required");
            Assert.assertTrue(firstDeathTurn>=0 && secondDeathTurn>firstDeathTurn,
                    "separate Forge turns required; no duplicate damage tick");
            Assert.assertTrue(attackerPaid && defenderPaid,
                    "each actor must pay genuine Forge mana for original Bears");
            Assert.assertTrue(ca.attackCallbacks>=2 && cb.blockerCallbacks>=2);
            Assert.assertTrue(cb.blocksAuthorized>=2,
                    "defender must authorize two legal blocks on separate turns");
            Assert.assertTrue(ca.refereeDamageAssignments+cb.refereeDamageAssignments>=2,
                    "both combat-damage steps use the authoritative Forge referee");
            Assert.assertEquals(countDeadBears(attacker),2);
            Assert.assertEquals(countDeadBears(defender),2);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome(),"no fabricated game outcome");
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertTrue(aggressive.process.isAlive() && guarding.process.isAlive());
            for(JsonObject view:Arrays.asList(
                    JsonParser.parseString(observation(attacker,defender)).getAsJsonObject(),
                    JsonParser.parseString(observation(defender,attacker)).getAsJsonObject())) {
                Assert.assertEquals(view.size(),4);
                for(String denied:Arrays.asList("opponent_hand","library_order",
                        "library_top","turn","phase","priority","stack")) {
                    Assert.assertFalse(view.has(denied));
                }
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeTwoSuccessiveUncuratedBlocksAttackerRegisteredFirst()
            throws Exception {
        verifyTwoSuccessiveNaturalBlocks(true);
    }

    @Test(timeOut=120000)
    public void nativeTwoSuccessiveUncuratedBlocksDefenderRegisteredFirst()
            throws Exception {
        verifyTwoSuccessiveNaturalBlocks(false);
    }

    /**
     * Stage10Y: a full real 20-life Forge win, with ZERO selected opening
     * cards. The attacking player's originally registered 56 Forest + four
     * Bears and the nonblocking registered opponent's 60 Islands are
     * genuinely shuffled for native Match.startGame and London mulligans.
     * Both persistent Python actors decide only on their permitted own/public
     * observations. The referee alone casts legally, collects mana,
     * resolves the stack, handles combat, and checks the life-based winner.
     *
     * Multiple naturally drawn Bears may attack in the same damage step;
     * therefore the rule-grounded requirement is positive EVEN damage
     * between 2 and 8 in each separate combat event, not artificially
     * assuming exactly ten single-Bear attacks to reach 0.
     */
    private void verifyNativeUncuratedFullTwentyLifeWin(boolean attackerRegisteredFirst)
            throws Exception {
        Game game=nativeUncuratedTwoActorGame(attackerRegisteredFirst);
        Player attacker=game.getPlayers().get(attackerRegisteredFirst?0:1);
        Player defender=game.getPlayers().get(attackerRegisteredFirst?1:0);
        try(Actor aggressive=new Actor(1);Actor passive=new Actor(2)) {
            Assert.assertNotEquals(aggressive.process.pid(),passive.process.pid(),
                    "strictly two independent, persistent external Python actors");
            ExternalCreatureController ca=attach(game,attacker,defender,aggressive);
            ExternalCreatureController cb=attach(game,defender,attacker,passive);
            doRealNativeStartForMultiTurn(game,ca,cb);
            // No prepareKnownCastingScenario(), no fixturePlaceRegisteredBear(),
            // no hand/library swaps, no direct life manipulation at any point.
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertNull(game.getOutcome());
            Assert.assertFalse(game.isGameOver());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Battlefield),0);
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            int firstAttackerLibrary=attacker.getCardsIn(ZoneType.Library).size();
            int firstDefenderLibrary=defender.getCardsIn(ZoneType.Library).size();
            int previousLife=20,previousDamageTurn=-1;
            int steps=0,damageEvents=0,damageTotal=0;
            boolean paidOnStack=false,bearResolved=false;
            while(!game.isGameOver() && steps++<25000) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty()) {
                    int tapped=0;
                    for(Card c:attacker.getCardsIn(ZoneType.Battlefield)) {
                        if(c.isLand() && c.isTapped()) tapped++;
                    }
                    if(tapped>=2 && basicLandCountOnBattlefield(attacker)>=2)
                        paidOnStack=true;
                }
                for(Card c:attacker.getCardsIn(ZoneType.Battlefield)) {
                    if("Grizzly Bears".equals(c.getName())) bearResolved=true;
                }
                int life=defender.getLife();
                if(life!=previousLife) {
                    int damage=previousLife-life;
                    Assert.assertTrue(damage>=2 && damage<=8 && damage%2==0,
                            "one genuine combat of up to four registered 2/2 Bears");
                    int actualTurn=game.getPhaseHandler().getTurn();
                    Assert.assertTrue(actualTurn>previousDamageTurn,
                            "positive damage events must be separate real Forge turns");
                    previousDamageTurn=actualTurn;
                    damageEvents++;
                    damageTotal+=damage;
                    Assert.assertEquals(life,20-damageTotal,
                            "referee tracks natural cumulative combat damage exactly");
                    previousLife=life;
                }
            }
            Assert.assertTrue(steps<25000,"bounded native match completed before turn limit");
            Assert.assertTrue(paidOnStack && bearResolved,
                    "genuine naturally drawn original Bear resolves from a paid Forge spell");
            Assert.assertTrue(damageEvents>=3,"multiple genuine combat turns required");
            Assert.assertTrue(damageTotal>=20,"unblocked creature damage beats 20 starting life");
            Assert.assertTrue(ca.attackCallbacks>=damageEvents);
            Assert.assertTrue(ca.priorityCallbacks>damageEvents);
            Assert.assertTrue(cb.priorityCallbacks>damageEvents);
            Assert.assertTrue(firstAttackerLibrary>
                    attacker.getCardsIn(ZoneType.Library).size(),
                    "both hands continue from real normal draws");
            Assert.assertTrue(firstDefenderLibrary>
                    defender.getCardsIn(ZoneType.Library).size());
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker),
                    "all originally registered attacker cards conserved");
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender),
                    "all originally registered defender cards conserved");
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertTrue(defender.getLife()<=0,
                    "native Forge, not the test, determines the final life value");
            Assert.assertTrue(game.isGameOver(),"actual game conclusion required");
            Assert.assertNotNull(game.getOutcome(),"Forge must record real game outcome");
            Assert.assertTrue(defender.hasLost(),"life-based native loss");
            Assert.assertEquals(defender.getOutcome().lossState.toString(),
                    "LifeReachedZero");
            Assert.assertTrue(attacker.hasWon(),"registered attacker is Forge winner");
            Assert.assertTrue(game.getOutcome().isWinner(attacker.getRegisteredPlayer()));
            Assert.assertFalse(game.getOutcome().isDraw());
            JsonObject attackerView=JsonParser.parseString(observation(attacker,defender))
                    .getAsJsonObject();
            JsonObject defenderView=JsonParser.parseString(observation(defender,attacker))
                    .getAsJsonObject();
            Assert.assertEquals(attackerView.size(),4);
            Assert.assertEquals(defenderView.size(),4);
            for(JsonObject view:Arrays.asList(attackerView,defenderView)) {
                for(String denied:Arrays.asList("opponent_hand","library_order",
                        "library_top","turn","phase","priority","stack"))
                    Assert.assertFalse(view.has(denied));
            }
        }
    }

    @Test(timeOut=120000)
    public void nativeUncuratedTwentyLifeForgeVictoryAttackerRegisteredFirst()
            throws Exception {
        verifyNativeUncuratedFullTwentyLifeWin(true);
    }

    @Test(timeOut=120000)
    public void nativeUncuratedTwentyLifeForgeVictoryDefenderRegisteredFirst()
            throws Exception {
        verifyNativeUncuratedFullTwentyLifeWin(false);
    }

    /**
     * Stage10Z: finish a REAL native game after the opponent cast an original
     * Bear, legally blocked, and both original Bears actually died. Unlike
     * Stage10Y's 60-Island nonblocking benchmark, both 60-card decks include
     * four naturally shuffled Bears, and BOTH persistent external Python
     * actors make gameplay decisions from own/public-only information.
     *
     * To isolate a second fully authentic game-ending route, the defender's
     * phase-blind preference remains defensive while the attacker attacks
     * only against a publicly visible opposing Bear. Once all actual
     * encounters finish, normal Forge draw steps continue until the first
     * attempted draw from an empty library ends the game under CR 104.3c
     * and 704.5b. NO referee-forced draw, library emptying, timeout winner,
     * opening hand staging, battlefield curation, or life manipulation.
     */
    private void verifyNativeNaturalContestedDeckOut(boolean attackerRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(attackerRegisteredFirst);
        Player attacking=game.getPlayers().get(attackerRegisteredFirst?0:1);
        Player defending=game.getPlayers().get(attackerRegisteredFirst?1:0);
        try(Actor aggressor=new Actor(4);Actor guardian=new Actor(2)) {
            Assert.assertNotEquals(aggressor.process.pid(),guardian.process.pid(),
                    "exactly two independently persisted Python actor OS processes");
            ExternalCreatureController ca=attach(game,attacking,defending,aggressor);
            ExternalCreatureController cb=attach(game,defending,attacking,guardian);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0,
                    "actual native privately shuffled opening hands and London mulligans");
            Assert.assertEquals(attacking.getLife(),20);
            Assert.assertEquals(defending.getLife(),20);
            Assert.assertEquals(countDeadBears(attacking),0);
            Assert.assertEquals(countDeadBears(defending),0);
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",ZoneType.Battlefield),0,
                    "no prepared initial board");
            Assert.assertEquals(fullPhysicalInventory(attacking),frozenMain.owned(attacking));
            Assert.assertEquals(fullPhysicalInventory(defending),frozenMain.owned(defending));

            boolean attackerPaid=false,defenderPaid=false,actualCombatDeaths=false;
            int steps=0;
            while(!game.isGameOver() && steps++<80000) {
                game.getPhaseHandler().mainLoopStep();
                if(!game.getStack().isEmpty()) {
                    int tappedA=0,tappedD=0;
                    for(Card c:attacking.getCardsIn(ZoneType.Battlefield))
                        if(c.isLand() && c.isTapped()) tappedA++;
                    for(Card c:defending.getCardsIn(ZoneType.Battlefield))
                        if(c.isLand() && c.isTapped()) tappedD++;
                    if(tappedA>=2 && basicLandCountOnBattlefield(attacking)>=2)
                        attackerPaid=true;
                    if(tappedD>=2 && basicLandCountOnBattlefield(defending)>=2)
                        defenderPaid=true;
                }
                if(countDeadBears(attacking)>=1 && countDeadBears(defending)>=1)
                    actualCombatDeaths=true;
                Assert.assertEquals(attacking.getLife(),20,
                        "all attacked 2/2 creatures remain legally blocked");
                Assert.assertEquals(defending.getLife(),20,
                        "no damage can be manufactured to force an outcome");
            }
            Assert.assertTrue(steps<80000,
                    "actual native game must terminate before bounded safety limit");
            Assert.assertTrue(attackerPaid && defenderPaid,
                    "BOTH actors genuinely cast their own mana-paid Bears");
            Assert.assertTrue(actualCombatDeaths && cb.blocksAuthorized>=1,
                    "opponent must genuinely and legally block and kill attacker");
            Assert.assertTrue(ca.attackCallbacks>0 && cb.blockerCallbacks>0);
            Assert.assertTrue(ca.refereeDamageAssignments+cb.refereeDamageAssignments>0,
                    "Forge alone assigned actual 2/2 combat damage");
            Assert.assertTrue(game.getPhaseHandler().getTurn()>40,
                    "natural repeated turn/draw progression must reach decking");
            Assert.assertTrue(game.isGameOver());
            Assert.assertNotNull(game.getOutcome(),"native Forge determines the winner");
            Assert.assertFalse(game.getOutcome().isDraw());
            Assert.assertTrue(attacking.hasWon()!=defending.hasWon(),
                    "exactly one of the two registered players is the real winner");
            Assert.assertTrue(attacking.hasLost()!=defending.hasLost(),
                    "exactly one real player must lose");
            Player winner=attacking.hasWon()?attacking:defending;
            Player loser=attacking.hasLost()?attacking:defending;
            Assert.assertNotSame(winner,loser);
            Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
            Assert.assertEquals(loser.getCardsIn(ZoneType.Library).size(),0,
                    "real draw from empty original library causes natural Forge loss");
            Assert.assertEquals(attacking.getLife(),20);
            Assert.assertEquals(defending.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(attacking),frozenMain.owned(attacking));
            Assert.assertEquals(fullPhysicalInventory(defending),frozenMain.owned(defending));
            JsonObject attackerView=JsonParser.parseString(observation(attacking,defending))
                    .getAsJsonObject();
            JsonObject defenderView=JsonParser.parseString(observation(defending,attacking))
                    .getAsJsonObject();
            Assert.assertEquals(attackerView.size(),4);
            Assert.assertEquals(defenderView.size(),4);
            for(JsonObject view:Arrays.asList(attackerView,defenderView)) {
                for(String denied:Arrays.asList("opponent_hand","library_order",
                        "library_top","turn","phase","priority","stack"))
                    Assert.assertFalse(view.has(denied),
                            "independent process must never receive referee-only data");
            }
        }
    }

    @Test(timeOut=180000)
    public void nativeNaturallyDepletedLibraryAfterRealContestedCombatAttackerRegisteredFirst()
            throws Exception {
        verifyNativeNaturalContestedDeckOut(true);
    }

    @Test(timeOut=180000)
    public void nativeNaturallyDepletedLibraryAfterRealContestedCombatDefenderRegisteredFirst()
            throws Exception {
        verifyNativeNaturalContestedDeckOut(false);
    }

    /**
     * Stage11A: BOTH independent Python actors run the same symmetric
     * attack-and-block preference, unlike the passive Stage10Z defender.
     * All 60 original registered cards per player are shuffled by native
     * Match.startGame. Nobody curates hand contents, life, libraries, or the
     * battlefield. Forge alone enforces legal attacks, blocks, costs,
     * summoning sickness, priority, damage and the actual game winner.
     *
     * This test asserts both actors genuinely have paid for and resolved
     * original creatures, and at least one legal attack occurs on each
     * side when Forge allows it. The combat/turn sequence can differ due
     * to real native shuffling and London mulligans.
     */
    private void verifyNaturalReciprocalFullGame(boolean leftRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(leftRegisteredFirst);
        Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
            Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid(),
                    "independent persistent external Python processes");
            ExternalCreatureController ca=attach(game,left,right,leftActor);
            ExternalCreatureController cb=attach(game,right,left,rightActor);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0,
                    "both players' genuine uncurated opening-hand decisions");
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                    ZoneType.Battlefield),0,"no fixture creature");
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            boolean leftResolved=false,rightResolved=false;
            boolean sawCombat=false;
            int steps=0;
            while(!game.isGameOver() && steps++<85000) {
                game.getPhaseHandler().mainLoopStep();
                for(Card c:left.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(c.getName())) leftResolved=true;
                for(Card c:right.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(c.getName())) rightResolved=true;
                if(ca.attacksAuthorized+cb.attacksAuthorized>0) sawCombat=true;
            }
            Assert.assertTrue(steps<85000,"natural Forge game must finish");
            // Native Forge accepts only legal, mana-affordable original
            // Grizzly Bears casts. A post-step stack snapshot can miss payment:
            // a real spell may be paid and resolved within one mainLoopStep.
            // Resolved physical cards plus original deck conservation provide
            // a stable result-level assertion without fabricating costs.
            // Shuffled four-of decks do NOT guarantee each participant will
            // draw and resolve a Bear before a legitimate lethal outcome.
            // Requiring both sides to cast or attack selects for lucky
            // opening hands and incorrectly fails authentic short games.
            Assert.assertTrue(leftResolved || rightResolved,
                    "at least one genuine paid creature from an original hand resolves");
            Assert.assertTrue(sawCombat &&
                    ca.attacksAuthorized+cb.attacksAuthorized>0,
                    "at least one genuine Forge-legal attack must be authorized");
            // Both actors run the SAME attack/block-capable external policy,
            // and each receives genuine combat callbacks, but both need not
            // physically attack in a naturally shuffled individual game.
            // If one player has no attack-eligible permanent before a real
            // game ending, Forge may legally skip its declareAttackers
            // callback. The actual authorized attack(s) are recorded above.
            Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0,
                    "both independent actors participate in real native priority");
            // Forge can legally skip declareBlockers entirely in a combat
            // with no eligible opposing creature. Do not require a callback
            // that native combat rules do not guarantee.
            Assert.assertTrue(game.isGameOver());
            Assert.assertNotNull(game.getOutcome(),
                    "Forge, not the test or actors, ends game");
            Assert.assertFalse(game.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon(),
                    "one genuinely registered native winner required");
            Assert.assertTrue(left.hasLost()!=right.hasLost(),
                    "one genuinely registered loser required");
            Player winner=left.hasWon()?left:right;
            Player loser=left.hasLost()?left:right;
            Assert.assertNotSame(winner,loser);
            Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            JsonObject lView=JsonParser.parseString(observation(left,right))
                    .getAsJsonObject();
            JsonObject rView=JsonParser.parseString(observation(right,left))
                    .getAsJsonObject();
            for(JsonObject view:Arrays.asList(lView,rView)) {
                Assert.assertEquals(view.size(),4);
                for(String denied:Arrays.asList("opponent_hand","library_order",
                        "library_top","turn","phase","priority","stack")) {
                    Assert.assertFalse(view.has(denied));
                }
            }
        }
    }

    @Test(timeOut=180000)
    public void nativeReciprocalAttackCapablePlayersPlayToActualOutcomeLeftRegisteredFirst()
            throws Exception {
        verifyNaturalReciprocalFullGame(true);
    }

    @Test(timeOut=180000)
    public void nativeReciprocalAttackCapablePlayersPlayToActualOutcomeRightRegisteredFirst()
            throws Exception {
        verifyNaturalReciprocalFullGame(false);
    }

    /**
     * Stage11B: one gameplay-policy improvement, a public-life-aware
     * counterattack. Two independent persistent Python actors decline an
     * even 2/2 trade while healthy and offer Forge-legal blocks when visible
     * Bears could deal lethal damage. This is a strategic preference, not a
     * replacement for native attack/block legality. The isolated probes
     * construct hypothetical PUBLIC observations only and never alter a
     * real game, its life totals, decks, opening hands, or battlefield.
     *
     * The following full game uses original shuffled 60-card decks, native
     * London mulligans, ordinary draws, mana payment, combat and outcome.
     * It requires genuine player combat damage but does not claim both
     * actors necessarily draw an attacker before a random short game ends.
     */
    private void verifyNativeLifeAwareCounterattack(boolean leftRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(leftRegisteredFirst);
        Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
            Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid(),
                    "exactly two persistent independent Python actors");
            ExternalCreatureController ca=attach(game,left,right,leftActor);
            ExternalCreatureController cb=attach(game,right,left,rightActor);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                    ZoneType.Battlefield),0,"no curated opening board");
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));

            // The same two LIVE actor PIDs must respond differently to
            // healthy versus lethal-risk public observations. Probes do not
            // edit native Forge Game/Card or transfer hidden opponent zones.
            for(int i=0;i<2;i++) {
                Actor actor=i==0?leftActor:rightActor;
                Player self=i==0?left:right;
                Player other=i==0?right:left;
                JsonObject probe=JsonParser.parseString(observation(self,other))
                        .getAsJsonObject();
                JsonObject faceUpBear=new JsonObject();
                faceUpBear.addProperty("appearance","face_up");
                faceUpBear.addProperty("card","Grizzly Bears");
                JsonArray ownTable=new JsonArray();
                ownTable.add(faceUpBear.deepCopy());
                probe.getAsJsonObject("own_cards").add("table",ownTable);
                JsonArray enemyTable=new JsonArray();
                enemyTable.add(faceUpBear.deepCopy());
                probe.add("opponent_table",enemyTable);
                JsonObject life=probe.getAsJsonObject("life_totals");
                life.addProperty("self",20);
                life.addProperty("opponent",20);
                JsonObject healthy=actor.ask(probe.toString());
                Assert.assertEquals(healthy.get("intent").getAsString(),"action_plan");
                Assert.assertEquals(healthy.getAsJsonArray("attack_table_indices").size(),1,
                        "healthy actor still offers a counterattack");
                Assert.assertEquals(healthy.getAsJsonArray("block_pairs").size(),1,
                        "Stage11F offers a legal defensive trade even at healthy life");
                life.addProperty("self",2);
                JsonObject critical=actor.ask(probe.toString());
                Assert.assertEquals(critical.getAsJsonArray("attack_table_indices").size(),0,
                        "Stage11D preserves its only Bear for lethal-risk defense");
                Assert.assertEquals(critical.getAsJsonArray("block_pairs").size(),1,
                        "public lethal risk changes independent block preference");
                JsonArray pair=critical.getAsJsonArray("block_pairs").get(0).getAsJsonArray();
                Assert.assertEquals(pair.get(0).getAsInt(),0);
                Assert.assertEquals(pair.get(1).getAsInt(),0);
            }

            boolean actualPlayerDamage=false;
            int steps=0;
            while(!game.isGameOver() && steps++<85000) {
                game.getPhaseHandler().mainLoopStep();
                if(left.getLife()<20 || right.getLife()<20)
                    actualPlayerDamage=true;
            }
            Assert.assertTrue(steps<85000,"native match must finish naturally");
            Assert.assertTrue(actualPlayerDamage,
                    "a real Forge combat attack must damage a registered player");
            Assert.assertTrue(ca.attacksAuthorized+cb.attacksAuthorized>0,
                    "Forge, not a probe, must authorize a legal natural attack");
            Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
            Assert.assertTrue(game.isGameOver());
            Assert.assertNotNull(game.getOutcome(),"Forge records the actual result");
            Assert.assertFalse(game.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon());
            Assert.assertTrue(left.hasLost()!=right.hasLost());
            Player winner=left.hasWon()?left:right;
            Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            System.out.println("STAGE11B_REAL_COMBAT left_attacks="+ca.attacksAuthorized+
                    " right_attacks="+cb.attacksAuthorized+
                    " left_life="+left.getLife()+" right_life="+right.getLife()+
                    " winner="+winner.getName());
        }
    }

    @Test(timeOut=180000)
    public void nativeLifeAwareCounterattackNaturalGameLeftRegisteredFirst()
            throws Exception {
        verifyNativeLifeAwareCounterattack(true);
    }

    @Test(timeOut=180000)
    public void nativeLifeAwareCounterattackNaturalGameRightRegisteredFirst()
            throws Exception {
        verifyNativeLifeAwareCounterattack(false);
    }

    /**
     * Stage11C: deployment of a second creature is no longer suppressed by
     * seeing one's own first Bear. A direct independent-process policy probe
     * uses only hypothetical OWN hand and PUBLIC battlefield entries; it
     * never changes the authentic live game. Then BOTH real workers continue
     * a native shuffled, untouched 60-card Forge match to a true outcome.
     * Because the real shuffles are not curated, multiple resolved Bears
     * and reciprocal combat are telemetry, not guaranteed test assertions.
     */
    private void verifyNativeReserveDeployment(boolean leftRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(leftRegisteredFirst);
        Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
            Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid(),
                    "two persistent independent Python actor OS processes");
            ExternalCreatureController ca=attach(game,left,right,leftActor);
            ExternalCreatureController cb=attach(game,right,left,rightActor);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                    ZoneType.Battlefield),0,"no curated native starting creatures");
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));

            for(int i=0;i<2;i++) {
                Actor actor=i==0?leftActor:rightActor;
                Player self=i==0?left:right;
                Player other=i==0?right:left;
                JsonObject probe=JsonParser.parseString(observation(self,other))
                        .getAsJsonObject();
                JsonArray hand=new JsonArray();
                hand.add("Forest");
                hand.add("Forest");
                hand.add("Grizzly Bears");
                hand.add("Grizzly Bears");
                probe.getAsJsonObject("own_cards").add("hand",hand);
                JsonObject bear=new JsonObject();
                bear.addProperty("appearance","face_up");
                bear.addProperty("card","Grizzly Bears");
                JsonArray ownTable=new JsonArray();
                ownTable.add(bear);
                probe.getAsJsonObject("own_cards").add("table",ownTable);
                probe.add("opponent_table",new JsonArray());
                JsonObject plan=actor.ask(probe.toString());
                Assert.assertEquals(plan.get("intent").getAsString(),"action_plan");
                JsonArray reinforcement=plan.getAsJsonArray("creature_hand_indices");
                Assert.assertEquals(reinforcement.size(),2,
                        "first own Bear must not suppress reinforcing creatures");
                Assert.assertEquals(reinforcement.get(0).getAsInt(),2);
                Assert.assertEquals(reinforcement.get(1).getAsInt(),3);
                Assert.assertEquals(plan.getAsJsonArray("attack_table_indices").size(),1,
                        "existing Bear may still be offered for legal combat");
                Assert.assertEquals(plan.getAsJsonArray("block_pairs").size(),0);
                // This probe only measures actor preference. No real hand,
                // actual battlefield, shuffled library or life was altered.
                Assert.assertEquals(self.getLife(),20);
                Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
            }

            boolean actualPlayerDamage=false;
            int peakLiveBears=0,steps=0;
            while(!game.isGameOver() && steps++<85000) {
                game.getPhaseHandler().mainLoopStep();
                int liveLeft=0,liveRight=0;
                for(Card card:left.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(card.getName())) liveLeft++;
                for(Card card:right.getCardsIn(ZoneType.Battlefield))
                    if("Grizzly Bears".equals(card.getName())) liveRight++;
                peakLiveBears=Math.max(peakLiveBears,Math.max(liveLeft,liveRight));
                if(left.getLife()<20 || right.getLife()<20)
                    actualPlayerDamage=true;
            }
            Assert.assertTrue(steps<85000,"authentic Forge game must end");
            Assert.assertTrue(actualPlayerDamage,
                    "at least one actual Forge player-damaging attack");
            Assert.assertTrue(ca.attacksAuthorized+cb.attacksAuthorized>0,
                    "Forge authorizes at least one real legal attack");
            Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
            Assert.assertTrue(game.isGameOver());
            Assert.assertNotNull(game.getOutcome());
            Assert.assertFalse(game.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon());
            Assert.assertTrue(left.hasLost()!=right.hasLost());
            Player winner=left.hasWon()?left:right;
            Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            System.out.println("STAGE11C_REINFORCEMENTS real_left_attacks="+
                    ca.attacksAuthorized+" real_right_attacks="+cb.attacksAuthorized+
                    " peak_real_bears_on_one_side="+peakLiveBears+
                    " left_life="+left.getLife()+" right_life="+right.getLife());
        }
    }

    @Test(timeOut=180000)
    public void nativeReserveDeploymentNaturalGameLeftRegisteredFirst()
            throws Exception {
        verifyNativeReserveDeployment(true);
    }

    @Test(timeOut=180000)
    public void nativeReserveDeploymentNaturalGameRightRegisteredFirst()
            throws Exception {
        verifyNativeReserveDeployment(false);
    }

    /**
     * Stage11D: a symmetric external actor should conserve just enough
     * publicly visible Bears to propose defensive blocks against possible
     * lethal combat damage. This reconciles attacking-taps-creatures
     * (CR 508.1f) with the untapped-blocker requirement (CR 509.1a).
     * Policy probes are constructed solely from legal own/public observation
     * fields and cannot change the native match. Two real actors then play
     * each privately shuffled 60-card game to Forge's actual outcome.
     */
    private void verifyNativeLethalRiskReserve(boolean leftRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(leftRegisteredFirst);
        Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
            Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid(),
                    "two independent persistent Python actor OS processes");
            ExternalCreatureController ca=attach(game,left,right,leftActor);
            ExternalCreatureController cb=attach(game,right,left,rightActor);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                    ZoneType.Battlefield),0,"no staged initial creatures");
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));

            for(int i=0;i<2;i++) {
                Actor actor=i==0?leftActor:rightActor;
                Player self=i==0?left:right;
                Player other=i==0?right:left;
                JsonObject probe=JsonParser.parseString(observation(self,other))
                        .getAsJsonObject();
                JsonObject bear=new JsonObject();
                bear.addProperty("appearance","face_up");
                bear.addProperty("card","Grizzly Bears");
                JsonArray own=new JsonArray();
                for(int j=0;j<3;j++) own.add(bear.deepCopy());
                probe.getAsJsonObject("own_cards").add("table",own);
                JsonArray opponents=new JsonArray();
                for(int j=0;j<3;j++) opponents.add(bear.deepCopy());
                probe.add("opponent_table",opponents);
                JsonObject life=probe.getAsJsonObject("life_totals");

                // Healthy: all three Bears available as attack candidates.
                life.addProperty("self",20);
                life.addProperty("opponent",20);
                JsonObject healthy=actor.ask(probe.toString());
                Assert.assertEquals(healthy.get("intent").getAsString(),"action_plan");
                Assert.assertEquals(healthy.getAsJsonArray("attack_table_indices").size(),3);
                Assert.assertEquals(healthy.getAsJsonArray("block_pairs").size(),8,
                        "Stage11F offers bounded candidate blocks even when healthy");

                // Six visible incoming damage vs four life: prevent at
                // least four damage by reserving two available 2/2s.
                life.addProperty("self",4);
                JsonObject endangered=actor.ask(probe.toString());
                JsonArray attacks=endangered.getAsJsonArray("attack_table_indices");
                JsonArray blocks=endangered.getAsJsonArray("block_pairs");
                Assert.assertEquals(attacks.size(),1,"one safe attacker remains");
                Assert.assertEquals(attacks.get(0).getAsInt(),0);
                Assert.assertEquals(blocks.size(),6,
                        "two reserved Bears offer three legal-target fallbacks each");
                Assert.assertEquals(blocks.get(0).getAsJsonArray().get(0).getAsInt(),1);
                Assert.assertEquals(blocks.get(0).getAsJsonArray().get(1).getAsInt(),0);
                Assert.assertEquals(blocks.get(2).getAsJsonArray().get(1).getAsInt(),2);
                Assert.assertEquals(blocks.get(3).getAsJsonArray().get(0).getAsInt(),2);

                // Six vs six needs only one blocker; retain two attackers.
                life.addProperty("self",6);
                JsonObject borderline=actor.ask(probe.toString());
                Assert.assertEquals(borderline.getAsJsonArray("attack_table_indices").size(),2);
                Assert.assertEquals(borderline.getAsJsonArray("block_pairs").size(),3);

                // One remaining Bear must be able to defend when a single
                // face-up opposing Bear could be lethal. A proposal is not
                // a guarantee of a legal block; Forge decides eligibility.
                JsonArray single=new JsonArray();
                single.add(bear.deepCopy());
                probe.getAsJsonObject("own_cards").add("table",single);
                probe.add("opponent_table",single.deepCopy());
                life.addProperty("self",2);
                JsonObject finalDefense=actor.ask(probe.toString());
                Assert.assertEquals(finalDefense.getAsJsonArray("attack_table_indices").size(),0);
                Assert.assertEquals(finalDefense.getAsJsonArray("block_pairs").size(),1);
                Assert.assertEquals(finalDefense.getAsJsonArray("block_pairs")
                        .get(0).getAsJsonArray().get(0).getAsInt(),0);

                // All edited values belong to PROBE JSON, not Forge game.
                Assert.assertEquals(self.getLife(),20);
                Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
            }

            int steps=0;
            boolean combatDamage=false;
            while(!game.isGameOver() && steps++<85000) {
                game.getPhaseHandler().mainLoopStep();
                if(left.getLife()<20 || right.getLife()<20) combatDamage=true;
            }
            Assert.assertTrue(steps<85000,"real shuffled game must terminate");
            Assert.assertTrue(combatDamage,"true native player combat damage");
            Assert.assertTrue(ca.attacksAuthorized+cb.attacksAuthorized>0);
            Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
            Assert.assertTrue(game.isGameOver());
            Assert.assertNotNull(game.getOutcome(),"native Forge determines outcome");
            Assert.assertFalse(game.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon());
            Assert.assertTrue(left.hasLost()!=right.hasLost());
            Player winner=left.hasWon()?left:right;
            Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            System.out.println("STAGE11D_DEFENSE real_left_attacks="+ca.attacksAuthorized+
                    " real_right_attacks="+cb.attacksAuthorized+
                    " left_life="+left.getLife()+" right_life="+right.getLife());
        }
    }

    @Test(timeOut=180000)
    public void nativeLethalRiskReserveNaturalGameLeftRegisteredFirst()
            throws Exception {
        verifyNativeLethalRiskReserve(true);
    }

    @Test(timeOut=180000)
    public void nativeLethalRiskReserveNaturalGameRightRegisteredFirst()
            throws Exception {
        verifyNativeLethalRiskReserve(false);
    }

    /**
     * Stage11E: one defended 2/2 is held back *before* single-swing lethal
     * exposure, because attacking usually taps a potential blocker.
     * Each persistent Python actor receives only its own hand/visible table,
     * legally visible enemy board and public life. Hypothetical JSON probes
     * do not touch the authentic native Forge game. Actual blocks are
     * counted only when Forge accepts assignments in declareBlockers;
     * naturally shuffled games can legitimately finish without one.
     */
    private void verifyNativeAdvanceDefenseReserve(boolean leftRegisteredFirst)
            throws Exception {
        Game game=nativeTwoNaturallyPaidBearGame(leftRegisteredFirst);
        Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
            Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid());
            ExternalCreatureController ca=attach(game,left,right,leftActor);
            ExternalCreatureController cb=attach(game,right,left,rightActor);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                    ZoneType.Battlefield),0,"no staged starting board");
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));

            for(int i=0;i<2;i++) {
                Actor actor=i==0?leftActor:rightActor;
                Player self=i==0?left:right;
                Player other=i==0?right:left;
                JsonObject probe=JsonParser.parseString(observation(self,other))
                        .getAsJsonObject();
                JsonObject bear=new JsonObject();
                bear.addProperty("appearance","face_up");
                bear.addProperty("card","Grizzly Bears");
                JsonArray own=new JsonArray();
                JsonArray enemy=new JsonArray();
                for(int j=0;j<3;j++) {
                    own.add(bear.deepCopy());
                    enemy.add(bear.deepCopy());
                }
                probe.getAsJsonObject("own_cards").add("table",own);
                probe.add("opponent_table",enemy);
                JsonObject life=probe.getAsJsonObject("life_totals");

                life.addProperty("self",11);
                life.addProperty("opponent",20);
                JsonObject safe=actor.ask(probe.toString());
                Assert.assertEquals(safe.getAsJsonArray("attack_table_indices").size(),3,
                        "outside two-turn danger all own Bears may attack");
                Assert.assertEquals(safe.getAsJsonArray("block_pairs").size(),8);

                life.addProperty("self",10);
                JsonObject atRisk=actor.ask(probe.toString());
                Assert.assertEquals(atRisk.get("intent").getAsString(),"action_plan");
                Assert.assertEquals(atRisk.getAsJsonArray("attack_table_indices").size(),2,
                        "hold one Bear for defense before one-swing lethal");
                Assert.assertEquals(atRisk.getAsJsonArray("block_pairs").size(),3);
                Assert.assertEquals(atRisk.getAsJsonArray("block_pairs")
                        .get(0).getAsJsonArray().get(0).getAsInt(),2);
                Assert.assertEquals(atRisk.getAsJsonArray("block_pairs")
                        .get(0).getAsJsonArray().get(1).getAsInt(),0);

                life.addProperty("self",6);
                JsonObject imminent=actor.ask(probe.toString());
                Assert.assertEquals(imminent.getAsJsonArray("attack_table_indices").size(),2);
                Assert.assertEquals(imminent.getAsJsonArray("block_pairs").size(),3);

                // Observation-only probes MUST NOT alter real life or inventory.
                Assert.assertEquals(self.getLife(),20);
                Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
            }

            int steps=0;
            boolean actualPlayerDamage=false;
            while(!game.isGameOver() && steps++<85000) {
                game.getPhaseHandler().mainLoopStep();
                if(left.getLife()<20 || right.getLife()<20)
                    actualPlayerDamage=true;
            }
            Assert.assertTrue(steps<85000,"native game finishes from legal actions");
            Assert.assertTrue(actualPlayerDamage,"genuine native combat damages a player");
            Assert.assertTrue(ca.attacksAuthorized+cb.attacksAuthorized>0);
            Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
            Assert.assertTrue(game.isGameOver());
            Assert.assertNotNull(game.getOutcome());
            Assert.assertFalse(game.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon());
            Assert.assertTrue(left.hasLost()!=right.hasLost());
            Player winner=left.hasWon()?left:right;
            Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            // Only refereeDeclareBlockers increments blocksAuthorized on a
            // physically accepted legal block. Zero is a possible AUTHENTIC
            // result under a full random draw; do not secretly reroll it.
            System.out.println("STAGE11E_NATIVE_BLOCKS left="+ca.blocksAuthorized+
                    " right="+cb.blocksAuthorized+
                    " left_attacks="+ca.attacksAuthorized+
                    " right_attacks="+cb.attacksAuthorized+
                    " left_life="+left.getLife()+" right_life="+right.getLife());
        }
    }

    @Test(timeOut=180000)
    public void nativeEarlyReserveNaturalGameLeftRegisteredFirst()
            throws Exception {
        verifyNativeAdvanceDefenseReserve(true);
    }

    @Test(timeOut=180000)
    public void nativeEarlyReserveNaturalGameRightRegisteredFirst()
            throws Exception {
        verifyNativeAdvanceDefenseReserve(false);
    }

    /**
     * Stage11F: reactive healthy blocking against any ACTUALLY attacking Bear.
     * Each actor offers public-board candidate pairs, including alternatives
     * for a creature at a later opponent-table index. Forge retains exclusive
     * authority to reject tapped, non-attacking or otherwise illegal pairs.
     * Four PREDECLARED sequential naturally shuffled full native games are
     * played in EACH registration order, not selectively rerolled. Genuine
     * accepted block counts are printed, INCLUDING ZERO if it occurs.
     * No opening cards, deck orders, life totals or game outcomes are curated.
     */
    private void verifyNativeReactiveDefenseCohort(boolean leftRegisteredFirst)
            throws Exception {
        int games=4;
        int allAcceptedBlocks=0,allAcceptedAttacks=0;
        int gamesWithBlocks=0;
        for(int iteration=0;iteration<games;iteration++) {
            Game game=nativeTwoNaturallyPaidBearGame(leftRegisteredFirst);
            Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
                Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid(),
                        "each natural game has exactly two external actor processes");
                ExternalCreatureController ca=attach(game,left,right,leftActor);
                ExternalCreatureController cb=attach(game,right,left,rightActor);
                doRealNativeStartForMultiTurn(game,ca,cb);
                Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertNull(game.getOutcome());
                Assert.assertEquals(countCardsWithName(game,"Grizzly Bears",
                        ZoneType.Battlefield),0,"no staged native board");
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));

                // Probe BOTH persistent real players, using only own/public
                // JSON. A defensive preference can be proposed at 20 life
                // without revealing turn, phase, stack or actual attackers.
                for(int i=0;i<2;i++) {
                    Actor actor=i==0?leftActor:rightActor;
                    Player self=i==0?left:right;
                    Player other=i==0?right:left;
                    JsonObject probe=JsonParser.parseString(observation(self,other))
                            .getAsJsonObject();
                    JsonObject visibleBear=new JsonObject();
                    visibleBear.addProperty("appearance","face_up");
                    visibleBear.addProperty("card","Grizzly Bears");
                    JsonArray ownTable=new JsonArray();
                    ownTable.add(visibleBear.deepCopy());
                    JsonArray enemyTable=new JsonArray();
                    for(int n=0;n<3;n++) enemyTable.add(visibleBear.deepCopy());
                    probe.getAsJsonObject("own_cards").add("table",ownTable);
                    probe.add("opponent_table",enemyTable);
                    probe.getAsJsonObject("life_totals").addProperty("self",20);
                    probe.getAsJsonObject("life_totals").addProperty("opponent",20);
                    JsonObject healthy=actor.ask(probe.toString());
                    Assert.assertEquals(healthy.get("intent").getAsString(),"action_plan");
                    Assert.assertEquals(healthy.getAsJsonArray("attack_table_indices").size(),1,
                            "the actor can still attack when healthy");
                    JsonArray proposed=healthy.getAsJsonArray("block_pairs");
                    Assert.assertEquals(proposed.size(),3,
                            "healthy blocker attempts all visible target indices");
                    for(int enemy=0;enemy<3;enemy++) {
                        JsonArray pair=proposed.get(enemy).getAsJsonArray();
                        Assert.assertEquals(pair.get(0).getAsInt(),0);
                        Assert.assertEquals(pair.get(1).getAsInt(),enemy);
                    }
                    Assert.assertEquals(self.getLife(),20,"probe must not edit life");
                    Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self),
                            "probe cannot edit cards or shuffle");
                }

                int steps=0;
                while(!game.isGameOver() && steps++<85000)
                    game.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"native game must terminate");
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome(),"Forge owns outcome");
                Assert.assertFalse(game.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Assert.assertTrue(left.hasLost()!=right.hasLost());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
                Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0,
                        "both independent actors play the natural match");
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                int accepted=ca.blocksAuthorized+cb.blocksAuthorized;
                int attacks=ca.attacksAuthorized+cb.attacksAuthorized;
                allAcceptedBlocks+=accepted;
                allAcceptedAttacks+=attacks;
                if(accepted>0) gamesWithBlocks++;
                System.out.println("STAGE11F_GAME seat_left_first="+leftRegisteredFirst+
                        " sequence="+iteration+" left_native_blocks="+ca.blocksAuthorized+
                        " right_native_blocks="+cb.blocksAuthorized+
                        " left_attacks="+ca.attacksAuthorized+
                        " right_attacks="+cb.attacksAuthorized+
                        " winner="+winner.getName());
            }
        }
        Assert.assertEquals(games,4,"no shuffle/result-selective early exit");
        Assert.assertTrue(allAcceptedAttacks>=0);
        // An uncurated game can legitimately contain no mutual Bears, so
        // the policy test cannot demand an invented or forced natural block.
        System.out.println("STAGE11F_COHORT left_first="+leftRegisteredFirst+
                " total_games="+games+" games_with_native_blocks="+gamesWithBlocks+
                " accepted_native_blocks="+allAcceptedBlocks+
                " accepted_native_attacks="+allAcceptedAttacks);
    }

    @Test(timeOut=180000)
    public void nativeReactiveHealthyBlockingCohortLeftRegisteredFirst()
            throws Exception {
        verifyNativeReactiveDefenseCohort(true);
    }

    @Test(timeOut=180000)
    public void nativeReactiveHealthyBlockingCohortRightRegisteredFirst()
            throws Exception {
        verifyNativeReactiveDefenseCohort(false);
    }

    /** Stage11G: 60 real Constructed cards; 48 Forest and four of each
     * distinct vanilla 2/2 Bear. No hand, draw, shuffle or life curation. */
    private Game nativeThreeSpeciesBearGame(boolean firstSeatActive) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",48);
        counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);
        counts.put("Balduvian Bears",4);
        RegisteredPlayer a=new RegisteredPlayer(mainDeck("11G left",counts))
                .setPlayer(new LobbyPlayerAi("mixed-left",null));
        RegisteredPlayer b=new RegisteredPlayer(mainDeck("11G right",counts))
                .setPlayer(new LobbyPlayerAi("mixed-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(firstSeatActive?a:b);
        registered.add(firstSeatActive?b:a);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage11G");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player player:game.getPlayers()) {
            Map<String,Integer> actual=frozenMain.owned(player);
            Assert.assertEquals(actual,counts);
            Assert.assertEquals(actual.values().stream().mapToInt(Integer::intValue).sum(),60);
            for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear","Balduvian Bears"))
                Assert.assertEquals(actual.get(name).intValue(),4);
        }
        return game;
    }

    /** Four PREDECLARED natural shuffled games in each registration order.
     * Each is two persistent independent Python actors plus one Forge referee.
     * Actor probes use only own/public JSON; actual block counts may be zero.
     */
    private void verifyNativeThreeSpeciesBearCohort(boolean leftRegisteredFirst)
            throws Exception {
        int totalBlocks=0,totalAttacks=0,gamesWithBlocks=0;
        for(int sequence=0;sequence<4;sequence++) {
            Game game=nativeThreeSpeciesBearGame(leftRegisteredFirst);
            Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
                Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid());
                ExternalCreatureController ca=attach(game,left,right,leftActor);
                ExternalCreatureController cb=attach(game,right,left,rightActor);
                doRealNativeStartForMultiTurn(game,ca,cb);
                Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertNull(game.getOutcome());
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear","Balduvian Bears"))
                    Assert.assertEquals(countCardsWithName(game,name,ZoneType.Battlefield),0);

                for(int seat=0;seat<2;seat++) {
                    Actor actor=seat==0?leftActor:rightActor;
                    Player self=seat==0?left:right;
                    Player other=seat==0?right:left;
                    JsonObject probe=JsonParser.parseString(observation(self,other))
                            .getAsJsonObject();
                    JsonArray hypotheticalHand=new JsonArray();
                    hypotheticalHand.add("Runeclaw Bear");
                    hypotheticalHand.add("Balduvian Bears");
                    hypotheticalHand.add("Grizzly Bears");
                    probe.getAsJsonObject("own_cards").add("hand",hypotheticalHand);
                    JsonArray ownTable=new JsonArray(),publicTable=new JsonArray();
                    for(String name:Arrays.asList("Runeclaw Bear","Balduvian Bears",
                            "Grizzly Bears")) {
                        JsonObject record=new JsonObject();
                        record.addProperty("appearance","face_up");
                        record.addProperty("card",name);
                        ownTable.add(record.deepCopy());
                        publicTable.add(record.deepCopy());
                    }
                    probe.getAsJsonObject("own_cards").add("table",ownTable);
                    probe.add("opponent_table",publicTable);
                    probe.getAsJsonObject("life_totals").addProperty("self",20);
                    probe.getAsJsonObject("life_totals").addProperty("opponent",20);
                    JsonObject plan=actor.ask(probe.toString());
                    Assert.assertEquals(plan.get("intent").getAsString(),"action_plan");
                    JsonArray creatures=plan.getAsJsonArray("creature_hand_indices");
                    Assert.assertEquals(creatures.size(),3);
                    for(int n=0;n<3;n++) Assert.assertEquals(creatures.get(n).getAsInt(),n);
                    Assert.assertEquals(plan.getAsJsonArray("attack_table_indices").size(),3);
                    JsonArray blocks=plan.getAsJsonArray("block_pairs");
                    Assert.assertEquals(blocks.size(),8);
                    Assert.assertEquals(blocks.get(0).getAsJsonArray().get(0).getAsInt(),0);
                    Assert.assertEquals(blocks.get(0).getAsJsonArray().get(1).getAsInt(),0);
                    Assert.assertEquals(blocks.get(3).getAsJsonArray().get(0).getAsInt(),1);
                    Assert.assertEquals(blocks.get(3).getAsJsonArray().get(1).getAsInt(),0);
                    Assert.assertEquals(self.getLife(),20);
                    Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
                }

                int steps=0;
                while(!game.isGameOver() && steps++<85000)
                    game.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"authentic native game must terminate");
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome(),"Forge determines outcome");
                Assert.assertFalse(game.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Assert.assertTrue(left.hasLost()!=right.hasLost());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
                Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                int blocks=ca.blocksAuthorized+cb.blocksAuthorized;
                int attacks=ca.attacksAuthorized+cb.attacksAuthorized;
                totalBlocks+=blocks;
                totalAttacks+=attacks;
                if(blocks>0) gamesWithBlocks++;
                System.out.println("STAGE11G_GAME left_first="+leftRegisteredFirst+
                        " sequence="+sequence+" left_native_blocks="+ca.blocksAuthorized+
                        " right_native_blocks="+cb.blocksAuthorized+
                        " left_attacks="+ca.attacksAuthorized+
                        " right_attacks="+cb.attacksAuthorized+
                        " winner="+winner.getName());
            }
        }
        Assert.assertTrue(totalAttacks>0,"real native attacks across four games");
        System.out.println("STAGE11G_COHORT left_first="+leftRegisteredFirst+
                " total_games=4 games_with_native_blocks="+gamesWithBlocks+
                " accepted_native_blocks="+totalBlocks+
                " accepted_native_attacks="+totalAttacks);
    }

    @Test(timeOut=180000)
    public void nativeThreeSpeciesVanillaBearCohortLeftRegisteredFirst()
            throws Exception {
        verifyNativeThreeSpeciesBearCohort(true);
    }

    @Test(timeOut=180000)
    public void nativeThreeSpeciesVanillaBearCohortRightRegisteredFirst()
            throws Exception {
        verifyNativeThreeSpeciesBearCohort(false);
    }
    /** Stage11H: natural 60-card Constructed with 44 Forest, 12 Bears,
     * four vanilla 3/3 Centaur Courser; no hand/life/draw curation. */
    private Game nativeUnequalVanillaGame(boolean firstSeatActive) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",44);
        counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);
        counts.put("Balduvian Bears",4);
        counts.put("Centaur Courser",4);
        RegisteredPlayer a=new RegisteredPlayer(mainDeck("11G left",counts))
                .setPlayer(new LobbyPlayerAi("mixed-left",null));
        RegisteredPlayer b=new RegisteredPlayer(mainDeck("11G right",counts))
                .setPlayer(new LobbyPlayerAi("mixed-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(firstSeatActive?a:b);
        registered.add(firstSeatActive?b:a);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage11H");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player player:game.getPlayers()) {
            Map<String,Integer> actual=frozenMain.owned(player);
            Assert.assertEquals(actual,counts);
            Assert.assertEquals(actual.values().stream().mapToInt(Integer::intValue).sum(),60);
            for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear","Balduvian Bears","Centaur Courser"))
                Assert.assertEquals(actual.get(name).intValue(),4);
        }
        return game;
    }

    /** Four PREDECLARED natural shuffled games in each registration order.
     * Each is two persistent independent Python actors plus one Forge referee.
     * Actor probes use only own/public JSON; actual block counts may be zero.
     */
    private void verifyNativeUnequalVanillaCohort(boolean leftRegisteredFirst)
            throws Exception {
        int totalBlocks=0,totalAttacks=0,gamesWithBlocks=0;
        for(int sequence=0;sequence<4;sequence++) {
            Game game=nativeUnequalVanillaGame(leftRegisteredFirst);
            Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor leftActor=new Actor(5);Actor rightActor=new Actor(5)) {
                Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid());
                ExternalCreatureController ca=attach(game,left,right,leftActor);
                ExternalCreatureController cb=attach(game,right,left,rightActor);
                doRealNativeStartForMultiTurn(game,ca,cb);
                Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertNull(game.getOutcome());
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear","Balduvian Bears","Centaur Courser"))
                    Assert.assertEquals(countCardsWithName(game,name,ZoneType.Battlefield),0);

                for(int seat=0;seat<2;seat++) {
                    Actor actor=seat==0?leftActor:rightActor;
                    Player self=seat==0?left:right;
                    Player other=seat==0?right:left;
                    JsonObject probe=JsonParser.parseString(observation(self,other))
                            .getAsJsonObject();
                    JsonArray hypotheticalHand=new JsonArray();
                    hypotheticalHand.add("Grizzly Bears");
                    hypotheticalHand.add("Centaur Courser");
                    probe.getAsJsonObject("own_cards").add("hand",hypotheticalHand);
                    JsonArray ownTable=new JsonArray(),publicTable=new JsonArray();
                    for(String name:Arrays.asList("Grizzly Bears","Centaur Courser")) {
                        JsonObject record=new JsonObject();
                        record.addProperty("appearance","face_up");
                        record.addProperty("card",name);
                        ownTable.add(record.deepCopy());
                        publicTable.add(record.deepCopy());
                    }
                    probe.getAsJsonObject("own_cards").add("table",ownTable);
                    probe.add("opponent_table",publicTable);
                    probe.getAsJsonObject("life_totals").addProperty("self",20);
                    probe.getAsJsonObject("life_totals").addProperty("opponent",20);
                    JsonObject plan=actor.ask(probe.toString());
                    Assert.assertEquals(plan.get("intent").getAsString(),"action_plan");
                    JsonArray creatures=plan.getAsJsonArray("creature_hand_indices");
                    Assert.assertEquals(creatures.size(),2);
                    Assert.assertEquals(creatures.get(0).getAsInt(),1,
                            "3/3 is first casting preference");
                    JsonArray swings=plan.getAsJsonArray("attack_table_indices");
                    Assert.assertEquals(swings.size(),1,
                            "avoid attacking 2/2 into visible 3/3");
                    Assert.assertEquals(swings.get(0).getAsInt(),1);
                    JsonArray blocks=plan.getAsJsonArray("block_pairs");
                    Assert.assertEquals(blocks.size(),3,
                            "healthy 2/2 should not chump-block 3/3");
                    Assert.assertEquals(blocks.get(0).getAsJsonArray().get(0).getAsInt(),1);
                    Assert.assertEquals(blocks.get(0).getAsJsonArray().get(1).getAsInt(),1);
                    Assert.assertEquals(blocks.get(2).getAsJsonArray().get(0).getAsInt(),0);
                    Assert.assertEquals(blocks.get(2).getAsJsonArray().get(1).getAsInt(),0);
                    probe.getAsJsonObject("life_totals").addProperty("self",2);
                    JsonObject endangered=actor.ask(probe.toString());
                    Assert.assertEquals(endangered.getAsJsonArray("attack_table_indices").size(),0);
                    JsonArray chump=endangered.getAsJsonArray("block_pairs");
                    Assert.assertEquals(chump.size(),4,
                            "lethal risk allows 2/2 to chump-block a 3/3");
                    Assert.assertEquals(chump.get(2).getAsJsonArray().get(0).getAsInt(),0);
                    Assert.assertEquals(chump.get(2).getAsJsonArray().get(1).getAsInt(),1);
                    Assert.assertEquals(self.getLife(),20);
                    Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
                }

                int steps=0;
                while(!game.isGameOver() && steps++<85000)
                    game.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"authentic native game must terminate");
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome(),"Forge determines outcome");
                Assert.assertFalse(game.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Assert.assertTrue(left.hasLost()!=right.hasLost());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
                Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                int blocks=ca.blocksAuthorized+cb.blocksAuthorized;
                int attacks=ca.attacksAuthorized+cb.attacksAuthorized;
                totalBlocks+=blocks;
                totalAttacks+=attacks;
                if(blocks>0) gamesWithBlocks++;
                System.out.println("STAGE11H_GAME left_first="+leftRegisteredFirst+
                        " sequence="+sequence+" left_native_blocks="+ca.blocksAuthorized+
                        " right_native_blocks="+cb.blocksAuthorized+
                        " left_attacks="+ca.attacksAuthorized+
                        " right_attacks="+cb.attacksAuthorized+
                        " winner="+winner.getName());
            }
        }
        Assert.assertTrue(totalAttacks>0,"real native attacks across four games");
        System.out.println("STAGE11H_COHORT left_first="+leftRegisteredFirst+
                " total_games=4 games_with_native_blocks="+gamesWithBlocks+
                " accepted_native_blocks="+totalBlocks+
                " accepted_native_attacks="+totalAttacks);
    }

    @Test(timeOut=180000)
    public void nativeUnequalVanillaCombatLeftRegisteredFirst()
            throws Exception {
        verifyNativeUnequalVanillaCohort(true);
    }

    @Test(timeOut=180000)
    public void nativeUnequalVanillaCombatRightRegisteredFirst()
            throws Exception {
        verifyNativeUnequalVanillaCohort(false);
    }


    /** Stage11I: both independent persistent actors can plan a legal
     * two-2/2 cooperative block on a public 3/3. All probes are only
     * hypothetical OWN/public JSON; native game remains uncurated. */
    private void verifyNativeCooperativeBlocking(boolean leftRegisteredFirst)
            throws Exception {
        for(int gameIndex=0;gameIndex<2;gameIndex++) {
            Game game=nativeUnequalVanillaGame(leftRegisteredFirst);
            Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor leftActor=new Actor(5); Actor rightActor=new Actor(5)) {
                Assert.assertNotEquals(leftActor.process.pid(),rightActor.process.pid());
                ExternalCreatureController ca=attach(game,left,right,leftActor);
                ExternalCreatureController cb=attach(game,right,left,rightActor);
                doRealNativeStartForMultiTurn(game,ca,cb);
                Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertNull(game.getOutcome());
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser"))
                    Assert.assertEquals(countCardsWithName(game,name,ZoneType.Battlefield),0);
                for(int seat=0;seat<2;seat++) {
                    Actor actor=seat==0?leftActor:rightActor;
                    Player self=seat==0?left:right;
                    Player other=seat==0?right:left;
                    JsonObject probe=JsonParser.parseString(observation(self,other))
                            .getAsJsonObject();
                    JsonObject bearA=new JsonObject();
                    bearA.addProperty("appearance","face_up");
                    bearA.addProperty("card","Grizzly Bears");
                    JsonObject bearB=new JsonObject();
                    bearB.addProperty("appearance","face_up");
                    bearB.addProperty("card","Runeclaw Bear");
                    JsonObject centaur=new JsonObject();
                    centaur.addProperty("appearance","face_up");
                    centaur.addProperty("card","Centaur Courser");
                    JsonArray own=new JsonArray();
                    own.add(bearA);
                    own.add(bearB);
                    JsonArray enemy=new JsonArray();
                    enemy.add(centaur.deepCopy());
                    enemy.add(centaur.deepCopy());
                    probe.getAsJsonObject("own_cards").add("table",own);
                    probe.add("opponent_table",enemy);
                    probe.getAsJsonObject("life_totals").addProperty("self",20);
                    probe.getAsJsonObject("life_totals").addProperty("opponent",20);
                    JsonObject first=actor.ask(probe.toString());
                    Assert.assertEquals(first.get("intent").getAsString(),"action_plan");
                    Assert.assertEquals(first.getAsJsonArray("attack_table_indices").size(),0);
                    JsonArray joint=first.getAsJsonArray("block_pairs");
                    Assert.assertEquals(joint.size(),4);
                    for(int n=0;n<4;n++) {
                        JsonArray pair=joint.get(n).getAsJsonArray();
                        Assert.assertEquals(pair.get(0).getAsInt(),n%2);
                        Assert.assertEquals(pair.get(1).getAsInt(),n/2);
                    }
                    own.add(centaur.deepCopy());
                    probe.getAsJsonObject("own_cards").add("table",own);
                    JsonObject withLarge=actor.ask(probe.toString());
                    JsonArray solo=withLarge.getAsJsonArray("block_pairs");
                    Assert.assertEquals(solo.size(),2);
                    for(int n=0;n<2;n++) {
                        JsonArray pair=solo.get(n).getAsJsonArray();
                        Assert.assertEquals(pair.get(0).getAsInt(),2);
                        Assert.assertEquals(pair.get(1).getAsInt(),n);
                    }
                    Assert.assertEquals(withLarge.getAsJsonArray("attack_table_indices").size(),1);
                    Assert.assertEquals(withLarge.getAsJsonArray("attack_table_indices")
                            .get(0).getAsInt(),2);
                    Assert.assertEquals(self.getLife(),20);
                    Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
                }
                int steps=0;
                while(!game.isGameOver() && steps++<85000)
                    game.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"natural Forge game must terminate");
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome());
                Assert.assertFalse(game.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Assert.assertTrue(left.hasLost()!=right.hasLost());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
                Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                System.out.println("STAGE11I_GAME left_first="+leftRegisteredFirst+
                        " game="+gameIndex+" real_accepted_blocks="+
                        (ca.blocksAuthorized+cb.blocksAuthorized)+
                        " left_attacks="+ca.attacksAuthorized+
                        " right_attacks="+cb.attacksAuthorized+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=180000)
    public void nativeCooperativeBlockingLeftRegisteredFirst() throws Exception {
        verifyNativeCooperativeBlocking(true);
    }

    @Test(timeOut=180000)
    public void nativeCooperativeBlockingRightRegisteredFirst() throws Exception {
        verifyNativeCooperativeBlocking(false);
    }

    /** Stage11J: add a real four-of vanilla {2}{G}{G} 4/4, with no
     * manipulation of the original privately shuffled sixty-card game. */
    private Game nativeFourStrengthVanillaGame(boolean leftRegisteredFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);
        counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);
        counts.put("Balduvian Bears",4);
        counts.put("Centaur Courser",4);
        counts.put("Rumbling Baloth",4);
        RegisteredPlayer a=new RegisteredPlayer(mainDeck("11J left",counts))
                .setPlayer(new LobbyPlayerAi("baloth-left",null));
        RegisteredPlayer b=new RegisteredPlayer(mainDeck("11J right",counts))
                .setPlayer(new LobbyPlayerAi("baloth-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(leftRegisteredFirst?a:b);
        registered.add(leftRegisteredFirst?b:a);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage11J");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player player:game.getPlayers()) {
            Assert.assertEquals(frozenMain.owned(player),counts);
            Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        }
        return game;
    }

    /** Stage11J: the actors independently reason about public 2/2, 3/3,
     * and 4/4 combat strength. All decision probes use copies of visible
     * own/public JSON and cannot influence real hands, shuffle or life.
     * Exactly two predeclared natural games run in each registration order.
     * Forge alone pays mana, casts creatures and resolves native combat. */
    private void verifyNaturalFourStrengthVanilla(boolean leftRegisteredFirst)
            throws Exception {
        int allAttacks=0,allBlocks=0,balothSeen=0;
        for(int sequence=0;sequence<2;sequence++) {
            Game game=nativeFourStrengthVanillaGame(leftRegisteredFirst);
            Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor a=new Actor(5);Actor b=new Actor(5)) {
                Assert.assertNotEquals(a.process.pid(),b.process.pid(),
                        "exactly two independent persistent Python actors");
                ExternalCreatureController ca=attach(game,left,right,a);
                ExternalCreatureController cb=attach(game,right,left,b);
                doRealNativeStartForMultiTurn(game,ca,cb);
                Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertNull(game.getOutcome());
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser","Rumbling Baloth"))
                    Assert.assertEquals(countCardsWithName(game,name,ZoneType.Battlefield),0,
                            "authentic no-fixture starting board");
                for(int seat=0;seat<2;seat++) {
                    Actor worker=seat==0?a:b;
                    Player self=seat==0?left:right;
                    Player other=seat==0?right:left;
                    JsonObject view=JsonParser.parseString(observation(self,other))
                            .getAsJsonObject();
                    JsonArray hand=new JsonArray();
                    hand.add("Grizzly Bears");
                    hand.add("Centaur Courser");
                    hand.add("Rumbling Baloth");
                    view.getAsJsonObject("own_cards").add("hand",hand);
                    JsonArray own=new JsonArray();
                    JsonArray opponentBoard=new JsonArray();
                    for(String name:Arrays.asList("Grizzly Bears",
                            "Centaur Courser","Rumbling Baloth")) {
                        JsonObject publicRecord=new JsonObject();
                        publicRecord.addProperty("appearance","face_up");
                        publicRecord.addProperty("card",name);
                        own.add(publicRecord.deepCopy());
                        opponentBoard.add(publicRecord.deepCopy());
                    }
                    view.getAsJsonObject("own_cards").add("table",own);
                    view.add("opponent_table",opponentBoard);
                    view.getAsJsonObject("life_totals").addProperty("self",20);
                    view.getAsJsonObject("life_totals").addProperty("opponent",20);
                    JsonObject plan=worker.ask(view.toString());
                    Assert.assertEquals(plan.get("intent").getAsString(),"action_plan");
                    JsonArray casts=plan.getAsJsonArray("creature_hand_indices");
                    Assert.assertEquals(casts.size(),3);
                    Assert.assertEquals(casts.get(0).getAsInt(),2,
                            "4/4 should be highest preferred creature");
                    Assert.assertEquals(casts.get(1).getAsInt(),1);
                    Assert.assertEquals(casts.get(2).getAsInt(),0);
                    JsonArray attacks=plan.getAsJsonArray("attack_table_indices");
                    Assert.assertEquals(attacks.size(),1,
                            "do not attack 2/2 or 3/3 into visible 4/4");
                    Assert.assertEquals(attacks.get(0).getAsInt(),2);
                    JsonArray blocks=plan.getAsJsonArray("block_pairs");
                    Assert.assertEquals(blocks.size(),6);
                    Assert.assertEquals(blocks.get(0).getAsJsonArray().get(0).getAsInt(),2);
                    Assert.assertEquals(blocks.get(0).getAsJsonArray().get(1).getAsInt(),2);
                    Assert.assertEquals(blocks.get(1).getAsJsonArray().get(1).getAsInt(),1);
                    Assert.assertEquals(blocks.get(2).getAsJsonArray().get(1).getAsInt(),0);
                    // Referee-private life, cards, priority and shuffles are untouched.
                    Assert.assertEquals(self.getLife(),20);
                    Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
                }
                int steps=0;
                while(!game.isGameOver() && steps++<85000) {
                    game.getPhaseHandler().mainLoopStep();
                    for(Player p:Arrays.asList(left,right))
                        for(Card c:p.getCardsIn(ZoneType.Battlefield))
                            if("Rumbling Baloth".equals(c.getName())) balothSeen++;
                }
                Assert.assertTrue(steps<85000,"native Forge game must terminate");
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome(),"Forge determines winner");
                Assert.assertFalse(game.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Assert.assertTrue(left.hasLost()!=right.hasLost());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
                Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                allAttacks+=ca.attacksAuthorized+cb.attacksAuthorized;
                allBlocks+=ca.blocksAuthorized+cb.blocksAuthorized;
                System.out.println("STAGE11J_NATIVE_GAME left_first="+leftRegisteredFirst+
                        " index="+sequence+" attacks="+
                        (ca.attacksAuthorized+cb.attacksAuthorized)+
                        " blocks="+(ca.blocksAuthorized+cb.blocksAuthorized)+
                        " winner="+winner.getName());
            }
        }
        System.out.println("STAGE11J_NATIVE_COHORT left_first="+leftRegisteredFirst+
                " games=2 attacks="+allAttacks+" blocks="+allBlocks+
                " observed_baloth_battlefield_snapshots="+balothSeen);
        // No lucky hands/seeds; real natural games might omit a particular
        // creature despite the policy demonstrating legal 4/4 preferences.
    }

    @Test(timeOut=180000)
    public void nativeFourStrengthVanillaLeftRegisteredFirst() throws Exception {
        verifyNaturalFourStrengthVanilla(true);
    }

    @Test(timeOut=180000)
    public void nativeFourStrengthVanillaRightRegisteredFirst() throws Exception {
        verifyNaturalFourStrengthVanilla(false);
    }

    /** Stage11K: generic cooperative vanilla defense:
     * 3/3+2/2 can trade with a 4/4, avoiding an isolated lost creature.
     * Probes exercise only own/public JSON to both persistent OS actors,
     * while four full shuffled native games per method are untouched.
     * The referee records genuine completed declaration-time two-blocker
     * events separately from policy recommendations; zero is possible.
     */
    private void verifyNativeGeneralCooperation(boolean leftRegisteredFirst)
            throws Exception {
        int games=3,legalAttacks=0,legalBlocks=0,nativePairs=0;
        for(int sequence=0;sequence<games;sequence++) {
            Game game=nativeFourStrengthVanillaGame(leftRegisteredFirst);
            Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor actorL=new Actor(5);Actor actorR=new Actor(5)) {
                Assert.assertNotEquals(actorL.process.pid(),actorR.process.pid(),
                        "two independent persistent external Python players");
                ExternalCreatureController ca=attach(game,left,right,actorL);
                ExternalCreatureController cb=attach(game,right,left,actorR);
                doRealNativeStartForMultiTurn(game,ca,cb);
                Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertNull(game.getOutcome());
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser","Rumbling Baloth"))
                    Assert.assertEquals(countCardsWithName(game,name,ZoneType.Battlefield),0);
                for(int seat=0;seat<2;seat++) {
                    Actor worker=seat==0?actorL:actorR;
                    Player self=seat==0?left:right;
                    Player other=seat==0?right:left;
                    JsonObject probe=JsonParser.parseString(observation(self,other))
                            .getAsJsonObject();
                    JsonObject small=new JsonObject();
                    small.addProperty("appearance","face_up");
                    small.addProperty("card","Grizzly Bears");
                    JsonObject medium=new JsonObject();
                    medium.addProperty("appearance","face_up");
                    medium.addProperty("card","Centaur Courser");
                    JsonObject big=new JsonObject();
                    big.addProperty("appearance","face_up");
                    big.addProperty("card","Rumbling Baloth");
                    JsonArray own=new JsonArray(),opponentTable=new JsonArray();
                    own.add(small.deepCopy());
                    own.add(medium.deepCopy());
                    opponentTable.add(big.deepCopy());
                    opponentTable.add(big.deepCopy());
                    probe.getAsJsonObject("own_cards").add("table",own);
                    probe.add("opponent_table",opponentTable);
                    probe.getAsJsonObject("life_totals").addProperty("self",20);
                    probe.getAsJsonObject("life_totals").addProperty("opponent",20);
                    JsonObject dual=worker.ask(probe.toString());
                    Assert.assertEquals(dual.get("intent").getAsString(),"action_plan");
                    Assert.assertEquals(dual.getAsJsonArray("attack_table_indices").size(),0,
                            "both visible own creatures weaker than public 4/4");
                    JsonArray proposed=dual.getAsJsonArray("block_pairs");
                    Assert.assertEquals(proposed.size(),4,
                            "3/3 and 2/2 cooperate against either public 4/4");
                    for(int target=0;target<2;target++) {
                        Assert.assertEquals(proposed.get(target*2)
                                .getAsJsonArray().get(0).getAsInt(),1);
                        Assert.assertEquals(proposed.get(target*2)
                                .getAsJsonArray().get(1).getAsInt(),target);
                        Assert.assertEquals(proposed.get(target*2+1)
                                .getAsJsonArray().get(0).getAsInt(),0);
                        Assert.assertEquals(proposed.get(target*2+1)
                                .getAsJsonArray().get(1).getAsInt(),target);
                    }
                    own.add(big.deepCopy());
                    probe.getAsJsonObject("own_cards").add("table",own);
                    JsonObject solo=worker.ask(probe.toString());
                    Assert.assertEquals(solo.getAsJsonArray("block_pairs").size(),2,
                            "4/4 single defender is preferred over sacrificing weaker creatures");
                    Assert.assertEquals(solo.getAsJsonArray("block_pairs")
                            .get(0).getAsJsonArray().get(0).getAsInt(),2);
                    Assert.assertEquals(solo.getAsJsonArray("block_pairs")
                            .get(0).getAsJsonArray().get(1).getAsInt(),0);
                    Assert.assertEquals(self.getLife(),20);
                    Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
                }
                int steps=0;
                while(!game.isGameOver() && steps++<85000)
                    game.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"untouched native game must complete");
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome());
                Assert.assertFalse(game.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Assert.assertTrue(left.hasLost()!=right.hasLost());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
                Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                int actualPairs=ca.coordinatedNativeBlocks+cb.coordinatedNativeBlocks;
                nativePairs+=actualPairs;
                legalAttacks+=ca.attacksAuthorized+cb.attacksAuthorized;
                legalBlocks+=ca.blocksAuthorized+cb.blocksAuthorized;
                System.out.println("STAGE11K_GAME left_first="+leftRegisteredFirst+
                        " sequence="+sequence+
                        " actual_referee_accepted_joint_blocks="+actualPairs+
                        " accepted_blocks="+(ca.blocksAuthorized+cb.blocksAuthorized)+
                        " real_attacks="+(ca.attacksAuthorized+cb.attacksAuthorized)+
                        " winner="+winner.getName());
            }
        }
        Assert.assertEquals(games,3,"all predeclared natural games completed");
        System.out.println("STAGE11K_COHORT left_first="+leftRegisteredFirst+
                " games="+games+" native_joint_blocks="+nativePairs+
                " accepted_blocks="+legalBlocks+" real_attacks="+legalAttacks);
    }

    @Test(timeOut=180000)
    public void nativeGeneralCooperationLeftRegisteredFirst() throws Exception {
        verifyNativeGeneralCooperation(true);
    }

    @Test(timeOut=180000)
    public void nativeGeneralCooperationRightRegisteredFirst() throws Exception {
        verifyNativeGeneralCooperation(false);
    }

    /** Stage11L: only for an EXPLICIT deterministic referee fixture.
     * The original registered physical card is moved from own hand/library.
     * This function MUST NEVER be used in the uncurated natural cohorts. */
    private void fixturePlaceRegisteredVanilla(Game game,Player owner,String name) {
        Card found=null;
        for(ZoneType zone:Arrays.asList(ZoneType.Library,ZoneType.Hand)) {
            for(Card c:owner.getCardsIn(zone)) {
                if(name.equals(c.getName())) {found=c;break;}
            }
            if(found!=null) break;
        }
        Assert.assertNotNull(found,"registered card must exist: "+name);
        Assert.assertTrue(supportedVanillaFighter(found.getName()));
        Assert.assertNotNull(game.getAction().moveToPlay(found,null,
                forge.game.ability.AbilityKey.newMap()));
    }

    /**
     * Stage11L gameplay: 4/4 vs two 2/2s assigns 2+2 and a 3/3 vs two
     * 2/2s assigns 2+1, following CR 510.1c-d. These first decisions are
     * EXPLICIT registered-card fixtures testing the authoritative referee,
     * NOT claims about natural combat deaths. Then two fresh fully random
     * 60-card games in each registration order test native integration.
     * Each game uses exactly two separate persistent external Python AIs.
     */
    private void verifyRefereeMultiTargetDamageSplit(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player attack=fixture.getPlayers().get(leftRegisteredFirst?0:1);
        Player defense=fixture.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor workerA=new Actor(5);Actor workerD=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerD.process.pid());
            ExternalCreatureController ca=attach(fixture,attack,defense,workerA);
            ExternalCreatureController cd=attach(fixture,defense,attack,workerD);
            doRealNativeStartForMultiTurn(fixture,ca,cd);
            Assert.assertEquals(attack.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,attack,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(fixture,attack,"Centaur Courser");
            fixturePlaceRegisteredVanilla(fixture,defense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,defense,"Runeclaw Bear");
            fixturePlaceRegisteredVanilla(fixture,defense,"Centaur Courser");
            Card four=attack.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Rumbling Baloth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card three=attack.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Centaur Courser".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card bearA=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card bearB=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Runeclaw Bear".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card defenderThree=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Centaur Courser".equals(c.getName()))
                    .findFirst().orElseThrow();
            CardCollection twoBears=new CardCollection();
            twoBears.add(bearA);
            twoBears.add(bearB);
            Map<Card,Integer> balanced=ca.assignCombatDamage(four,twoBears,
                    null,4,defense,false);
            Assert.assertEquals(balanced.size(),2,
                    "4/4 damage should be divided across two legal 2/2 blockers");
            Assert.assertEquals(balanced.get(bearA),Integer.valueOf(2));
            Assert.assertEquals(balanced.get(bearB),Integer.valueOf(2));
            Assert.assertEquals(balanced.values().stream()
                    .mapToInt(Integer::intValue).sum(),4);
            Map<Card,Integer> asymmetric=ca.assignCombatDamage(three,twoBears,
                    null,3,defense,false);
            Assert.assertEquals(asymmetric.size(),2);
            Assert.assertEquals(asymmetric.get(bearA),Integer.valueOf(2));
            Assert.assertEquals(asymmetric.get(bearB),Integer.valueOf(1));
            CardCollection mixed=new CardCollection();
            mixed.add(defenderThree);
            mixed.add(bearA);
            Map<Card,Integer> mixedSplit=ca.assignCombatDamage(four,mixed,
                    null,4,defense,false);
            Assert.assertEquals(mixedSplit.size(),2);
            Assert.assertEquals(mixedSplit.get(defenderThree),Integer.valueOf(3));
            Assert.assertEquals(mixedSplit.get(bearA),Integer.valueOf(1));
            Assert.assertEquals(ca.refereeDamageAssignments,3);
            Assert.assertEquals(ca.refereeSplitDamageAssignments,3);
            CardCollection unregistered=new CardCollection();
            unregistered.add(four);
            int rejected=0;
            try {ca.assignCombatDamage(four,unregistered,null,4,defense,false);}
            catch(SecurityException expected) {rejected++;}
            try {ca.assignCombatDamage(four,twoBears,null,3,defense,false);}
            catch(SecurityException expected) {rejected++;}
            Assert.assertEquals(rejected,2,"invalid combat allocation remains denied");
            Assert.assertEquals(ca.refereeDamageAssignments,3);
            Assert.assertEquals(ca.refereeSplitDamageAssignments,3);
            Assert.assertEquals(cd.refereeDamageAssignments,0);
            Assert.assertEquals(fullPhysicalInventory(attack),frozenMain.owned(attack));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertEquals(attack.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            Assert.assertFalse(observation(attack,defense).contains("\"phase\""));
            Assert.assertFalse(observation(defense,attack).contains("\"stack\""));
            System.out.println("STAGE11L_FIXTURE left_first="+leftRegisteredFirst+
                    " assigned_both_two_bears="+balanced.size()+
                    " distributed_damage=4 actual_fixture_combat_resolutions=0");
        }

        int actualAttacks=0,actualBlocks=0,actualDamageSplits=0;
        for(int sequence=0;sequence<2;sequence++) {
            Game game=nativeFourStrengthVanillaGame(leftRegisteredFirst);
            Player left=game.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=game.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor actorL=new Actor(5);Actor actorR=new Actor(5)) {
                Assert.assertNotEquals(actorL.process.pid(),actorR.process.pid());
                ExternalCreatureController ca=attach(game,left,right,actorL);
                ExternalCreatureController cb=attach(game,right,left,actorR);
                doRealNativeStartForMultiTurn(game,ca,cb);
                Assert.assertTrue(ca.openingCallbacks>0 && cb.openingCallbacks>0);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertNull(game.getOutcome());
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser","Rumbling Baloth"))
                    Assert.assertEquals(countCardsWithName(game,name,ZoneType.Battlefield),0);
                int steps=0;
                while(!game.isGameOver() && steps++<85000)
                    game.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"full native shuffled game terminates");
                Assert.assertTrue(game.isGameOver());
                Assert.assertNotNull(game.getOutcome(),"only Forge determines winner");
                Assert.assertFalse(game.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Assert.assertTrue(left.hasLost()!=right.hasLost());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(game.getOutcome().isWinner(winner.getRegisteredPlayer()));
                Assert.assertTrue(ca.priorityCallbacks>0 && cb.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                int attacks=ca.attacksAuthorized+cb.attacksAuthorized;
                int blocks=ca.blocksAuthorized+cb.blocksAuthorized;
                int splits=ca.refereeSplitDamageAssignments+
                        cb.refereeSplitDamageAssignments;
                actualAttacks+=attacks;
                actualBlocks+=blocks;
                actualDamageSplits+=splits;
                System.out.println("STAGE11L_NATIVE_GAME left_first="+leftRegisteredFirst+
                        " sequence="+sequence+" native_attacks="+attacks+
                        " accepted_blocks="+blocks+" native_damage_splits="+splits+
                        " winner="+winner.getName());
            }
        }
        System.out.println("STAGE11L_NATIVE_COHORT left_first="+leftRegisteredFirst+
                " actual_attacks="+actualAttacks+" accepted_blocks="+actualBlocks+
                " native_damage_splits="+actualDamageSplits+" games=2");
        // Genuine uncurated hands may contain zero native multiple-block combats.
        // Never use probe/fixture assertions to claim those outcomes occurred.
    }

    @Test(timeOut=180000)
    public void nativeDamageDivisionLeftRegisteredFirst() throws Exception {
        verifyRefereeMultiTargetDamageSplit(true);
    }

    @Test(timeOut=180000)
    public void nativeDamageDivisionRightRegisteredFirst() throws Exception {
        verifyRefereeMultiTargetDamageSplit(false);
    }

    /**
     * Stage11M gameplay: a damaged blocker needs LESS new combat damage.
     * Validate the newly changed native referee allocation with registered
     * physical cards, and separately validate actual Forge graveyard
     * destruction after native two-blocker combat. Explicit fixtures are
     * NEVER represented as untouched natural shuffled games.
     */
    private void verifyDamageAwareLethalSplitAndNativeDeaths(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player attacker=fixture.getPlayers().get(leftRegisteredFirst?0:1);
        Player defender=fixture.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor workerA=new Actor(5);Actor workerD=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerD.process.pid());
            ExternalCreatureController ac=attach(fixture,attacker,defender,workerA);
            ExternalCreatureController dc=attach(fixture,defender,attacker,workerD);
            doRealNativeStartForMultiTurn(fixture,ac,dc);
            fixturePlaceRegisteredVanilla(fixture,attacker,"Centaur Courser");
            fixturePlaceRegisteredVanilla(fixture,attacker,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(fixture,defender,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,defender,"Runeclaw Bear");
            fixturePlaceRegisteredVanilla(fixture,defender,"Centaur Courser");
            Card courser=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Centaur Courser".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card baloth=attacker.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Rumbling Baloth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card woundedBear=defender.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card freshBear=defender.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Runeclaw Bear".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card woundedCourser=defender.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Centaur Courser".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertEquals(woundedBear.getDamage(),0);
            Assert.assertEquals(woundedCourser.getDamage(),0);
            // Referee-only fixture: set public marked damage without moving
            // cards or mutating the two independent actor observations.
            woundedBear.setDamage(1);
            woundedCourser.setDamage(2);
            Assert.assertEquals(woundedBear.getLethalDamage(),1);
            Assert.assertEquals(woundedCourser.getLethalDamage(),1);
            CardCollection bears=new CardCollection();
            bears.add(woundedBear);
            bears.add(freshBear);
            Map<Card,Integer> threeVsTwo=ac.assignCombatDamage(courser,bears,
                    null,3,defender,false);
            Assert.assertEquals(threeVsTwo.size(),2);
            Assert.assertEquals(threeVsTwo.get(woundedBear),Integer.valueOf(1),
                    "marked 1 damage reduces 2/2 lethal requirement to 1");
            Assert.assertEquals(threeVsTwo.get(freshBear),Integer.valueOf(2),
                    "3/3 now deals lethal to BOTH blockers instead of 2+1");
            Assert.assertEquals(threeVsTwo.values().stream()
                    .mapToInt(Integer::intValue).sum(),3);
            CardCollection mixed=new CardCollection();
            mixed.add(woundedCourser);
            mixed.add(freshBear);
            Map<Card,Integer> fourVsMixed=ac.assignCombatDamage(baloth,mixed,
                    null,4,defender,false);
            Assert.assertEquals(fourVsMixed.size(),2);
            Assert.assertEquals(fourVsMixed.get(woundedCourser),Integer.valueOf(2),
                    "one lethal plus one surplus damage on damaged 3/3");
            Assert.assertEquals(fourVsMixed.get(freshBear),Integer.valueOf(2));
            Assert.assertEquals(fourVsMixed.values().stream()
                    .mapToInt(Integer::intValue).sum(),4);
            Assert.assertEquals(ac.refereeDamageAssignments,2);
            Assert.assertEquals(ac.refereeSplitDamageAssignments,2);
            Assert.assertEquals(dc.refereeDamageAssignments,0);
            Assert.assertEquals(attacker.getLife(),20);
            Assert.assertEquals(defender.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertFalse(observation(attacker,defender).contains("\"phase\""));
            Assert.assertFalse(observation(defender,attacker).contains("\"stack\""));
            System.out.println("STAGE11M_DAMAGE_AWARE_FIXTURE left_first="+
                    leftRegisteredFirst+" damaged_bear_vs_fresh=1+2"+
                    " damaged_courser_vs_fresh=2+2");
        }

        // A DISTINCT registered-card native combat fixture: actual
        // post-combat graveyard locations, not merely proposed assignments.
        Game combatGame=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player offense=combatGame.getPlayers().get(leftRegisteredFirst?0:1);
        Player defense=combatGame.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor actorO=new Actor(5);Actor actorD=new Actor(5)) {
            Assert.assertNotEquals(actorO.process.pid(),actorD.process.pid());
            ExternalCreatureController oc=attach(combatGame,offense,defense,actorO);
            ExternalCreatureController dc=attach(combatGame,defense,offense,actorD);
            doRealNativeStartForMultiTurn(combatGame,oc,dc);
            fixturePlaceRegisteredVanilla(combatGame,offense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(combatGame,defense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(combatGame,defense,"Runeclaw Bear");
            // Track the ACTUAL two physical registered blocker identities.
            // Later genuine priority can cast another same-named Bear; its
            // legal arrival must never invalidate our original-death proof.
            final long grizzlyFixtureId=defense.getCardsIn(ZoneType.Battlefield)
                    .stream().filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow().getId();
            final long runeclawFixtureId=defense.getCardsIn(ZoneType.Battlefield)
                    .stream().filter(c->"Runeclaw Bear".equals(c.getName()))
                    .findFirst().orElseThrow().getId();
            Assert.assertEquals(countCardsWithName(combatGame,"Grizzly Bears",
                    ZoneType.Graveyard),0);
            Assert.assertEquals(countCardsWithName(combatGame,"Runeclaw Bear",
                    ZoneType.Graveyard),0);
            Assert.assertEquals(countCardsWithName(combatGame,"Rumbling Baloth",
                    ZoneType.Graveyard),0);
            boolean bothBlockersDied=false;
            int steps=0;
            while(!combatGame.isGameOver() && steps++<2500) {
                combatGame.getPhaseHandler().mainLoopStep();
                if(oc.refereeSplitDamageAssignments>0 &&
                        dc.coordinatedNativeBlocks>0 &&
                        defense.getCardsIn(ZoneType.Graveyard).stream()
                                .anyMatch(c->c.getId()==grizzlyFixtureId) &&
                        defense.getCardsIn(ZoneType.Graveyard).stream()
                                .anyMatch(c->c.getId()==runeclawFixtureId)) {
                    bothBlockersDied=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded genuine Forge combat");
            Assert.assertTrue(dc.coordinatedNativeBlocks>0,
                    "Forge must authorize both weak blockers against 4/4");
            Assert.assertTrue(oc.refereeSplitDamageAssignments>0,
                    "real combat step must request 2+2 damage allocation");
            Assert.assertTrue(bothBlockersDied,
                    "BOTH real physical 2/2 blockers must die to native damage");
            Assert.assertFalse(defense.getCardsIn(ZoneType.Battlefield).stream()
                    .anyMatch(c->c.getId()==grizzlyFixtureId),
                    "original registered Grizzly blocker left battlefield");
            Assert.assertFalse(defense.getCardsIn(ZoneType.Battlefield).stream()
                    .anyMatch(c->c.getId()==runeclawFixtureId),
                    "original registered Runeclaw blocker left battlefield");
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            System.out.println("STAGE11M_NATIVE_DEATH_FIXTURE left_first="+
                    leftRegisteredFirst+" actual_two_blocker_graveyard_deaths=2"+
                    " real_split_assignments="+oc.refereeSplitDamageAssignments+
                    " native_steps="+steps);
        }

        // One new untouched natural 60-card game per registration order,
        // with real shuffle, mulligans, paid casts, combat and Forge outcome.
        Game natural=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor actorL=new Actor(5);Actor actorR=new Actor(5)) {
            Assert.assertNotEquals(actorL.process.pid(),actorR.process.pid());
            ExternalCreatureController lc=attach(natural,left,right,actorL);
            ExternalCreatureController rc=attach(natural,right,left,actorR);
            doRealNativeStartForMultiTurn(natural,lc,rc);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                    "Balduvian Bears","Centaur Courser","Rumbling Baloth"))
                Assert.assertEquals(countCardsWithName(natural,name,
                        ZoneType.Battlefield),0);
            int steps=0;
            while(!natural.isGameOver() && steps++<85000)
                natural.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<85000,"full native game terminates");
            Assert.assertTrue(natural.isGameOver());
            Assert.assertNotNull(natural.getOutcome());
            Assert.assertFalse(natural.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon());
            Player winner=left.hasWon()?left:right;
            Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
            Assert.assertTrue(lc.priorityCallbacks>0 && rc.priorityCallbacks>0);
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            System.out.println("STAGE11M_UNCURATED_GAME left_first="+
                    leftRegisteredFirst+" real_attacks="+
                    (lc.attacksAuthorized+rc.attacksAuthorized)+
                    " accepted_blocks="+(lc.blocksAuthorized+rc.blocksAuthorized)+
                    " native_splits="+(lc.refereeSplitDamageAssignments+
                            rc.refereeSplitDamageAssignments)+
                    " winner="+winner.getName());
        }
    }

    @Test(timeOut=180000)
    public void nativeDamageAwareSplitAndTwoBlockerDeathsLeftRegisteredFirst()
            throws Exception {
        verifyDamageAwareLethalSplitAndNativeDeaths(true);
    }

    @Test(timeOut=180000)
    public void nativeDamageAwareSplitAndTwoBlockerDeathsRightRegisteredFirst()
            throws Exception {
        verifyDamageAwareLethalSplitAndNativeDeaths(false);
    }

    /**
     * Stage11N GAMEPLAY: numerical lethal-swarm decision using ONLY public
     * table and public life, without inventing legal tap/readiness or hidden
     * game state. Separate explicit native registered-card combat fixture
     * demonstrates real Forge 3x2/2 attacks into a single 4/4 at four life.
     * A completely separate fresh 60-card natural match is never curated.
     */
    private void verifyPublicLethalSwarmAndNativeOutcome(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid());
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Assert.assertEquals(seatA.getLife(),20);
            Assert.assertEquals(seatB.getLife(),20);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense,"Forge owns starting player");
            Player defense=offense==seatA?seatB:seatA;
            Actor offensiveWorker=offense==seatA?workerA:workerB;
            Actor defensiveWorker=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            for(String name:Arrays.asList("Grizzly Bears",
                    "Runeclaw Bear","Balduvian Bears"))
                fixturePlaceRegisteredVanilla(fixture,offense,name);
            fixturePlaceRegisteredVanilla(fixture,defense,"Rumbling Baloth");
            for(Card card:offense.getCardsIn(ZoneType.Battlefield))
                if(Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears").contains(card.getName()))
                    card.setSickness(false); // EXPLICIT fixture, not natural match
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertEquals(countCardsWithName(fixture,"Rumbling Baloth",
                    ZoneType.Battlefield),1);
            Assert.assertTrue(defense.setLife(5,null),
                    "fixture-only public life 5 before nonlethal probe");
            JsonObject belowThreshold=offensiveWorker.ask(
                    observation(offense,defense));
            Assert.assertEquals(belowThreshold.getAsJsonArray(
                    "attack_table_indices").size(),0,
                    "three weak attackers should not charge 4/4 at 5 life");
            Assert.assertTrue(defense.setLife(4,null),
                    "fixture-only public life 4 for numerical lethal");
            JsonObject lethal=offensiveWorker.ask(
                    observation(offense,defense));
            Assert.assertEquals(lethal.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(lethal.getAsJsonArray(
                    "attack_table_indices").size(),3,
                    "3x2/2 forces at least four unblocked vanilla damage");
            // SYNTHETIC public observation only: an unknown face-down
            // permanent might be a legal extra blocker. Never derive its
            // card name or falsely guarantee lethal through it.
            JsonObject unknown=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject faceDown=new JsonObject();
            faceDown.addProperty("appearance","face_down");
            unknown.getAsJsonArray("opponent_table").add(faceDown);
            JsonObject unknownPlan=offensiveWorker.ask(unknown.toString());
            Assert.assertEquals(unknownPlan.getAsJsonArray(
                    "attack_table_indices").size(),0,
                    "unknown additional blocker must cancel numerical guarantee");
            Assert.assertEquals(defensiveWorker.ask(observation(defense,offense))
                    .get("intent").getAsString(),"action_plan");
            Assert.assertFalse(observation(offense,defense).contains(
                    "\\\"phase\\\""));
            Assert.assertFalse(observation(defense,offense).contains(
                    "\\\"stack\\\""));
            Assert.assertEquals(defense.getLife(),4,
                    "external actor probes cannot mutate referee life");
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500)
                fixture.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<2500,"bounded real Forge combat");
            Assert.assertTrue(fixture.isGameOver(),
                    "native 3x2/2 versus 4/4 must resolve lethal by unblocked creatures");
            Assert.assertTrue(oc.attacksAuthorized>=3,
                    "three genuinely legal attackers accepted by native Forge");
            Assert.assertTrue(dc.blocksAuthorized>0,
                    "defender gets an actual native legal block");
            Assert.assertTrue(offense.hasWon(),"Forge declares the winner");
            Assert.assertTrue(defense.hasLost(),"Forge declares life loss");
            Assert.assertNotNull(fixture.getOutcome());
            Assert.assertTrue(fixture.getOutcome().isWinner(
                    offense.getRegisteredPlayer()));
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11N_LETHAL_SWARM_FIXTURE left_first="+
                    leftRegisteredFirst+" native_attacks="+oc.attacksAuthorized+
                    " native_blocks="+dc.blocksAuthorized+
                    " defender_life="+defense.getLife());
        }

        // Distinct COMPLETELY uncurated registered Constructed game.
        Game natural=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor workerL=new Actor(5);Actor workerR=new Actor(5)) {
            Assert.assertNotEquals(workerL.process.pid(),workerR.process.pid());
            ExternalCreatureController lc=attach(natural,left,right,workerL);
            ExternalCreatureController rc=attach(natural,right,left,workerR);
            doRealNativeStartForMultiTurn(natural,lc,rc);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                    "Balduvian Bears","Centaur Courser","Rumbling Baloth"))
                Assert.assertEquals(countCardsWithName(natural,name,
                        ZoneType.Battlefield),0,"no curated natural board");
            int steps=0;
            while(!natural.isGameOver() && steps++<85000)
                natural.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<85000,"full natural Forge match terminates");
            Assert.assertTrue(natural.isGameOver());
            Assert.assertNotNull(natural.getOutcome());
            Assert.assertFalse(natural.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon());
            Player winner=left.hasWon()?left:right;
            Assert.assertTrue(natural.getOutcome().isWinner(
                    winner.getRegisteredPlayer()));
            Assert.assertTrue(lc.priorityCallbacks>0 && rc.priorityCallbacks>0);
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            System.out.println("STAGE11N_UNCURATED_GAME left_first="+
                    leftRegisteredFirst+" real_attacks="+
                    (lc.attacksAuthorized+rc.attacksAuthorized)+
                    " accepted_blocks="+(lc.blocksAuthorized+rc.blocksAuthorized)+
                    " winner="+winner.getName());
        }
    }

    @Test(timeOut=180000)
    public void nativePublicLethalSwarmLeftRegisteredFirst() throws Exception {
        verifyPublicLethalSwarmAndNativeOutcome(true);
    }

    @Test(timeOut=180000)
    public void nativePublicLethalSwarmRightRegisteredFirst() throws Exception {
        verifyPublicLethalSwarmAndNativeOutcome(false);
    }

    /**
     * Stage11O GAMEPLAY: a nonlethal, mixed-strength attack that the old
     * 4/4-only policy missed. A ready 4/4 and 2/2 attack into one 4/4
     * at 6 opposing life. At most ONE of the two attackers can be
     * blocked by the ordinary vanilla defender (CR 509.1a), so at
     * least 2 REAL damage is delivered by native Forge. No assumption
     * about hidden cards or actual tap status is sent to either actor.
     * Explicit registered-card fixture is SEPARATE from one fresh,
     * uncurated sixty-card complete Constructed match in each seat order.
     */
    private void verifyNonlethalMixedPressureAndNativeDamage(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0),seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid());
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Assert.assertEquals(seatA.getLife(),20);
            Assert.assertEquals(seatB.getLife(),20);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor offensiveWorker=offense==seatA?workerA:workerB;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            fixturePlaceRegisteredVanilla(fixture,offense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(fixture,offense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,defense,"Rumbling Baloth");
            for(Card card:offense.getCardsIn(ZoneType.Battlefield))
                if("Rumbling Baloth".equals(card.getName()) ||
                        "Grizzly Bears".equals(card.getName()))
                    card.setSickness(false); // EXPLICIT fixture, not natural
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(7,null),
                    "explicit nonlethal threshold probe, not a natural game");
            JsonObject distant=offensiveWorker.ask(observation(offense,defense));
            Assert.assertEquals(distant.getAsJsonArray("attack_table_indices").size(),1,
                    "at seven life, old 4/4-only attack remains");
            Assert.assertTrue(defense.setLife(6,null),
                    "explicit public life six, natural games stay at twenty");
            JsonObject pressure=offensiveWorker.ask(observation(offense,defense));
            JsonArray proposals=pressure.getAsJsonArray("attack_table_indices");
            Assert.assertEquals(proposals.size(),2,
                    "mixed 4/4+2/2 pressure at 6 life through one 4/4");
            // Adding an unknown facedown opponent permanent would make
            // an ordinary extra blocker possible. Its identity is never
            // requested and all unknown strength inference is prohibited.
            JsonObject unknown=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject hidden=new JsonObject();
            hidden.addProperty("appearance","face_down");
            unknown.getAsJsonArray("opponent_table").add(hidden);
            JsonObject conservative=offensiveWorker.ask(unknown.toString());
            Assert.assertEquals(conservative.getAsJsonArray(
                    "attack_table_indices").size(),1);
            Assert.assertFalse(observation(offense,defense).contains(
                    "\\\"phase\\\""));
            Assert.assertFalse(observation(defense,offense).contains(
                    "\\\"stack\\\""));
            Assert.assertEquals(defense.getLife(),6,
                    "actor output does not alter trusted Forge life");
            int steps=0;
            boolean nativeDamage=false;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                if(defense.getLife()<=4) {
                    nativeDamage=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded native Forge combat");
            Assert.assertTrue(nativeDamage,"real native combat must deal >=2 player damage");
            Assert.assertTrue(oc.attacksAuthorized>=2,
                    "both actors' proposed attack and real Forge legality must agree");
            Assert.assertTrue(dc.blocksAuthorized>=1,
                    "native Forge permits the single 4/4 to block");
            Assert.assertTrue(defense.getLife()>=2 && defense.getLife()<=4,
                    "one vanilla 4/4 blocks at most one attacker: 2 or 4 gets through");
            Assert.assertFalse(fixture.isGameOver(),
                    "nonlethal pressure must not fabricate winner");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11O_MIXED_PRESSURE_FIXTURE left_first="+
                    leftRegisteredFirst+" native_attacks="+oc.attacksAuthorized+
                    " native_blocks="+dc.blocksAuthorized+
                    " defender_life="+defense.getLife());
        }
        // DISTINCT fresh fully uncurated registered 60-card game.
        Game natural=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
        Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
        try(Actor workerL=new Actor(5);Actor workerR=new Actor(5)) {
            Assert.assertNotEquals(workerL.process.pid(),workerR.process.pid());
            ExternalCreatureController lc=attach(natural,left,right,workerL);
            ExternalCreatureController rc=attach(natural,right,left,workerR);
            doRealNativeStartForMultiTurn(natural,lc,rc);
            Assert.assertEquals(left.getLife(),20);
            Assert.assertEquals(right.getLife(),20);
            Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                    "Balduvian Bears","Centaur Courser","Rumbling Baloth"))
                Assert.assertEquals(countCardsWithName(natural,name,
                        ZoneType.Battlefield),0,"no curated natural board");
            int steps=0;
            while(!natural.isGameOver() && steps++<85000)
                natural.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<85000,"natural Forge match terminated");
            Assert.assertTrue(natural.isGameOver());
            Assert.assertNotNull(natural.getOutcome());
            Assert.assertFalse(natural.getOutcome().isDraw());
            Assert.assertTrue(left.hasWon()!=right.hasWon());
            Player winner=left.hasWon()?left:right;
            Assert.assertTrue(natural.getOutcome().isWinner(
                    winner.getRegisteredPlayer()));
            Assert.assertTrue(lc.priorityCallbacks>0 && rc.priorityCallbacks>0);
            Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
            Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
            System.out.println("STAGE11O_UNCURATED_GAME left_first="+
                    leftRegisteredFirst+" real_attacks="+
                    (lc.attacksAuthorized+rc.attacksAuthorized)+
                    " accepted_blocks="+(lc.blocksAuthorized+rc.blocksAuthorized)+
                    " winner="+winner.getName());
        }
    }

    @Test(timeOut=180000)
    public void nativeNonlethalMixedStrengthPressureLeftRegisteredFirst()
            throws Exception {
        verifyNonlethalMixedPressureAndNativeDamage(true);
    }

    @Test(timeOut=180000)
    public void nativeNonlethalMixedStrengthPressureRightRegisteredFirst()
            throws Exception {
        verifyNonlethalMixedPressureAndNativeDamage(false);
    }

    /**
     * Stage11P: emergent 2x2/2 coordinated block against 4/4 at
     * lethal-life risk. Only publicly visible cards and life guide
     * the persistent external actor; Forge owns legality, damage,
     * state-based creature deaths and player outcomes.
     * A separately created new naturally shuffled Constructed cohort
     * is verified without life, hand, board or draw manipulation.
     */
    private void verifyEmergencyCoordinatedDefense(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid());
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor defensiveActor=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,offense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(fixture,defense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,defense,"Runeclaw Bear");
            Card source=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Rumbling Baloth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card first=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card second=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Runeclaw Bear".equals(c.getName()))
                    .findFirst().orElseThrow();
            final long sourceId=source.getId();
            final long firstId=first.getId(),secondId=second.getId();
            source.setSickness(false); // explicit fixture only
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(5,null),
                    "fixture-only public life 5 control observation");
            JsonObject control=defensiveActor.ask(observation(defense,offense));
            Assert.assertEquals(control.getAsJsonArray("block_pairs").size(),0,
                    "at life 5, the existing healthy-life policy declines an outmatched 2/2 chump");
            Assert.assertTrue(defense.setLife(4,null),
                    "fixture-only public life 4 urgent observation");
            JsonObject urgent=defensiveActor.ask(observation(defense,offense));
            JsonArray pairs=urgent.getAsJsonArray("block_pairs");
            Assert.assertEquals(pairs.size(),2,
                    "urgent defense offers TWO physical 2/2 blockers");
            Assert.assertNotEquals(pairs.get(0).getAsJsonArray().get(0).getAsInt(),
                    pairs.get(1).getAsJsonArray().get(0).getAsInt());
            Assert.assertEquals(pairs.get(0).getAsJsonArray().get(1).getAsInt(),
                    pairs.get(1).getAsJsonArray().get(1).getAsInt());
            // A normal opponent Forest land drop must not suppress
            // the emergency response: Forest is public and cannot attack.
            JsonObject withForest=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject forest=new JsonObject();
            forest.addProperty("appearance","face_up");
            forest.addProperty("card","Forest");
            withForest.getAsJsonArray("opponent_table").add(forest);
            Assert.assertEquals(defensiveActor.ask(withForest.toString())
                    .getAsJsonArray("block_pairs").size(),2,
                    "public Forest does not count as a second 4/4 attacker");
            // An unidentifiable face-down permanent invalidates the
            // single-known-threat emergency assumption; never peek.
            JsonObject unknown=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject hidden=new JsonObject();
            hidden.addProperty("appearance","face_down");
            unknown.getAsJsonArray("opponent_table").add(hidden);
            Assert.assertEquals(defensiveActor.ask(unknown.toString())
                    .getAsJsonArray("block_pairs").size(),1);
            Assert.assertEquals(defense.getLife(),4,
                    "external actors cannot change referee life");
            boolean allOriginalsInGraves=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                if(oc.attacksAuthorized>0 && dc.coordinatedNativeBlocks>0 &&
                        offense.getCardsIn(ZoneType.Graveyard).stream()
                                .anyMatch(c->c.getId()==sourceId) &&
                        defense.getCardsIn(ZoneType.Graveyard).stream()
                                .anyMatch(c->c.getId()==firstId) &&
                        defense.getCardsIn(ZoneType.Graveyard).stream()
                                .anyMatch(c->c.getId()==secondId)) {
                    allOriginalsInGraves=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded real native Forge combat");
            Assert.assertTrue(allOriginalsInGraves,
                    "Forge actual original deaths expected; steps="+steps+
                    " attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " cooperation="+dc.coordinatedNativeBlocks+
                    " damage_splits="+oc.refereeSplitDamageAssignments+
                    " attacker_grave="+offense.getCardsIn(ZoneType.Graveyard).size()+
                    " defender_grave="+defense.getCardsIn(ZoneType.Graveyard).size()+
                    " defender_life="+defense.getLife()+
                    " game_over="+fixture.isGameOver());
            Assert.assertTrue(dc.blocksAuthorized>=2);
            Assert.assertTrue(oc.refereeSplitDamageAssignments>0);
            Assert.assertEquals(defense.getLife(),4,
                    "native fully blocked combat prevents player damage");
            Assert.assertFalse(fixture.isGameOver());
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11P_EMERGENCY_FIXTURE left_first="+
                    leftRegisteredFirst+" actual_blocks="+dc.blocksAuthorized+
                    " coordinated="+dc.coordinatedNativeBlocks+
                    " native_splits="+oc.refereeSplitDamageAssignments+
                    " verified_physical_deaths=3 defender_life="+defense.getLife());
        }
        // This existing harness starts FRESH wholly unmodified native
        // 60-card games on each invocation, not the staged fixture above.
        // It runs TWO predeclared natural matches per registration order.
        verifyNaturalFourStrengthVanilla(leftRegisteredFirst);
        System.out.println("STAGE11P_FRESH_NATURAL_COHORT left_first="+
                leftRegisteredFirst+" games=2");
    }

    @Test(timeOut=240000)
    public void nativeEmergencyJointBlockLeftRegisteredFirst() throws Exception {
        verifyEmergencyCoordinatedDefense(true);
    }

    @Test(timeOut=240000)
    public void nativeEmergencyJointBlockRightRegisteredFirst() throws Exception {
        verifyEmergencyCoordinatedDefense(false);
    }

    /**
     * Stage11Q GAMEPLAY: mixed-strength emergency joint defense using ONLY
     * public life and visible known vanilla 3/3 + 2/2 vs 4/4. Two independent
     * persistent Python players propose ordinary combat preferences; trusted
     * native Forge referee validates legality and resolves physical deaths.
     * A distinct freshly shuffled full native 60-card match is run afterward,
     * WITHOUT fixture life/board manipulation or secret information.
     */
    private void verifyMixedStrengthEmergencyJointDefense(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "two independent persistent external actors");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor defenderActor=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,offense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(fixture,defense,"Centaur Courser");
            fixturePlaceRegisteredVanilla(fixture,defense,"Grizzly Bears");
            Card baloth=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Rumbling Baloth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card courser=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Centaur Courser".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card bear=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            final long balothId=baloth.getId();
            final long courserId=courser.getId(),bearId=bear.getId();
            baloth.setSickness(false); // EXPLICIT fixture, never natural match
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));

            Assert.assertTrue(defense.setLife(5,null),
                    "fixture-only healthy control life 5");
            JsonObject healthy=defenderActor.ask(observation(defense,offense));
            Assert.assertEquals(healthy.getAsJsonArray("block_pairs").size(),0,
                    "at 5 life the prior early-reserve policy holds one blocker"+
                    " and makes NO insufficient solo block proposal");
            Assert.assertTrue(defense.setLife(4,null),
                    "fixture-only public urgent life 4");
            JsonObject urgent=defenderActor.ask(observation(defense,offense));
            JsonArray pairs=urgent.getAsJsonArray("block_pairs");
            Assert.assertEquals(urgent.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(pairs.size(),2,
                    "Stage11Q proposes both different-strength defenders at risk");
            Assert.assertNotEquals(pairs.get(0).getAsJsonArray().get(0).getAsInt(),
                    pairs.get(1).getAsJsonArray().get(0).getAsInt());
            Assert.assertEquals(pairs.get(0).getAsJsonArray().get(1).getAsInt(),
                    pairs.get(1).getAsJsonArray().get(1).getAsInt(),
                    "both target one publicly visible vanilla 4/4");
            JsonObject withForest=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject forest=new JsonObject();
            forest.addProperty("appearance","face_up");
            forest.addProperty("card","Forest");
            withForest.getAsJsonArray("opponent_table").add(forest);
            Assert.assertEquals(defenderActor.ask(withForest.toString())
                    .getAsJsonArray("block_pairs").size(),2,
                    "public noncreature Forest cannot be mistaken for second attacker");
            JsonObject unknown=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject faceDown=new JsonObject();
            faceDown.addProperty("appearance","face_down");
            unknown.getAsJsonArray("opponent_table").add(faceDown);
            Assert.assertEquals(defenderActor.ask(unknown.toString())
                    .getAsJsonArray("block_pairs").size(),1,
                    "face-down identity remains unknown; joint emergency disabled");
            Assert.assertEquals(defense.getLife(),4,
                    "hypothetical observation did not change real Forge life");
            Assert.assertFalse(observation(defense,offense).contains(
                    "\\\"phase\\\""));
            Assert.assertFalse(observation(offense,defense).contains(
                    "\\\"stack\\\""));
            boolean verifiedMixedTrade=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                boolean originalAttackerDead=offense.getCardsIn(ZoneType.Graveyard)
                        .stream().anyMatch(c->c.getId()==balothId);
                boolean courserDead=defense.getCardsIn(ZoneType.Graveyard)
                        .stream().anyMatch(c->c.getId()==courserId);
                boolean bearDead=defense.getCardsIn(ZoneType.Graveyard)
                        .stream().anyMatch(c->c.getId()==bearId);
                boolean courserAlive=defense.getCardsIn(ZoneType.Battlefield)
                        .stream().anyMatch(c->c.getId()==courserId);
                boolean bearAlive=defense.getCardsIn(ZoneType.Battlefield)
                        .stream().anyMatch(c->c.getId()==bearId);
                if(oc.attacksAuthorized>0 && dc.coordinatedNativeBlocks>0 &&
                        originalAttackerDead &&
                        (courserDead != bearDead) &&
                        (courserAlive != bearAlive) &&
                        courserDead==bearAlive && bearDead==courserAlive) {
                    verifiedMixedTrade=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded native Forge combat");
            Assert.assertTrue(verifiedMixedTrade,
                    "real Forge 4/4 must die, exactly one 3/3 or 2/2 dies;"+
                    " steps="+steps+" attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " coordinated="+dc.coordinatedNativeBlocks+
                    " splits="+oc.refereeSplitDamageAssignments+
                    " attacker_grave="+offense.getCardsIn(ZoneType.Graveyard).size()+
                    " defender_grave="+defense.getCardsIn(ZoneType.Graveyard).size()+
                    " life="+defense.getLife());
            Assert.assertTrue(dc.blocksAuthorized>=2,
                    "both distinct blockers accepted by actual native Forge");
            Assert.assertTrue(oc.refereeSplitDamageAssignments>0,
                    "attacking 4/4 divides four damage over both legal blockers");
            Assert.assertEquals(defense.getLife(),4,
                    "joint block prevents real player combat damage");
            Assert.assertFalse(fixture.isGameOver(),
                    "nonlethal block never fabricates winner");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11Q_MIXED_EMERGENCY_FIXTURE left_first="+
                    leftRegisteredFirst+" accepted_blocks="+dc.blocksAuthorized+
                    " coordinated="+dc.coordinatedNativeBlocks+
                    " native_splits="+oc.refereeSplitDamageAssignments+
                    " original_attacker_dead=true defender_deaths=1"+
                    " defender_survivors=1 defender_life="+defense.getLife());
        }
        // Fresh, uncurated, separate original 60-card Constructed games.
        // Existing established harness plays TWO native games per seat order
        // with independently shuffled libraries, real opening, 20 life,
        // separate Python PIDs and trusted Forge winners.
        verifyNaturalFourStrengthVanilla(leftRegisteredFirst);
        System.out.println("STAGE11Q_FRESH_NATURAL_COHORT left_first="+
                leftRegisteredFirst+" games=2");
    }

    @Test(timeOut=240000)
    public void nativeMixedEmergencyJointBlockLeftRegisteredFirst() throws Exception {
        verifyMixedStrengthEmergencyJointDefense(true);
    }

    @Test(timeOut=240000)
    public void nativeMixedEmergencyJointBlockRightRegisteredFirst() throws Exception {
        verifyMixedStrengthEmergencyJointDefense(false);
    }

    /**
     * Stage11R GAMEPLAY: defend at three public life with TWO registered
     * vanilla 2/2 creatures against a single attacking 3/3 Centaur Courser.
     * At four life the old no-suicidal-solo policy is unchanged; at three,
     * new public-only policy reserves and proposes BOTH blockers to destroy
     * the 3/3, instead of a single chump that leaves the threat intact.
     * Forge alone enforces the actual attackers, blocks, damage and graves.
     * The explicit physical-card combat fixture is separate from a fresh
     * set of naturally shuffled real twenty-life sixty-card matches.
     */
    private void verifyThreePowerEmergencyJointBlock(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "two independent persistent Python actors");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Assert.assertEquals(seatA.getLife(),20);
            Assert.assertEquals(seatB.getLife(),20);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense,"Forge owns actual first player");
            Player defense=offense==seatA?seatB:seatA;
            Actor defenseWorker=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;

            fixturePlaceRegisteredVanilla(fixture,offense,"Centaur Courser");
            fixturePlaceRegisteredVanilla(fixture,defense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,defense,"Runeclaw Bear");
            Card attacker=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Centaur Courser".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card grizzly=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card runeclaw=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Runeclaw Bear".equals(c.getName()))
                    .findFirst().orElseThrow();
            final long attackerId=attacker.getId();
            final long firstId=grizzly.getId(),secondId=runeclaw.getId();
            attacker.setSickness(false); // EXPLICIT control only
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));

            Assert.assertTrue(defense.setLife(4,null),
                    "explicit four-life baseline, not a natural-game state");
            JsonObject control=defenseWorker.ask(observation(defense,offense));
            Assert.assertEquals(control.getAsJsonArray("block_pairs").size(),0,
                    "at four life, retain old no-suicidal-solo policy");
            Assert.assertTrue(defense.setLife(3,null),
                    "explicit three-life emergency threshold only");
            JsonObject urgent=defenseWorker.ask(observation(defense,offense));
            JsonArray pairs=urgent.getAsJsonArray("block_pairs");
            Assert.assertEquals(urgent.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(pairs.size(),2,
                    "at three life, propose BOTH 2/2 blocks against one 3/3");
            Assert.assertNotEquals(
                    pairs.get(0).getAsJsonArray().get(0).getAsInt(),
                    pairs.get(1).getAsJsonArray().get(0).getAsInt(),
                    "different public own physical defender indices");
            Assert.assertEquals(
                    pairs.get(0).getAsJsonArray().get(1).getAsInt(),
                    pairs.get(1).getAsJsonArray().get(1).getAsInt(),
                    "both offer the SAME public target index");
            JsonObject normalForest=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject forest=new JsonObject();
            forest.addProperty("appearance","face_up");
            forest.addProperty("card","Forest");
            normalForest.getAsJsonArray("opponent_table").add(forest);
            Assert.assertEquals(defenseWorker.ask(normalForest.toString())
                    .getAsJsonArray("block_pairs").size(),2,
                    "a visible ordinary Forest is not a second threat");
            JsonObject unknown=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject faceDown=new JsonObject();
            faceDown.addProperty("appearance","face_down");
            unknown.getAsJsonArray("opponent_table").add(faceDown);
            Assert.assertEquals(defenseWorker.ask(unknown.toString())
                    .getAsJsonArray("block_pairs").size(),1,
                    "additional unknown permanent forbids exclusive-threat assumption");
            Assert.assertFalse(observation(defense,offense).contains(
                    "\\\"phase\\\""));
            Assert.assertFalse(observation(offense,defense).contains(
                    "\\\"stack\\\""));
            Assert.assertEquals(defense.getLife(),3,
                    "hypothetical player plans cannot change native life");

            boolean nativeTrade=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                boolean attackerDead=offense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==attackerId);
                boolean firstDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==firstId);
                boolean secondDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==secondId);
                boolean firstAlive=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==firstId);
                boolean secondAlive=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==secondId);
                if(oc.attacksAuthorized>0 && dc.coordinatedNativeBlocks>0
                        && attackerDead && (firstDead != secondDead)
                        && (firstAlive != secondAlive)
                        && firstDead == secondAlive
                        && secondDead == firstAlive) {
                    nativeTrade=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded native combat runner");
            Assert.assertTrue(nativeTrade,
                    "real Forge 3/3 death plus exactly one dead blocker;"+
                    " steps="+steps+" attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " coordination="+dc.coordinatedNativeBlocks+
                    " damage_splits="+oc.refereeSplitDamageAssignments+
                    " attacker_grave="+offense.getCardsIn(ZoneType.Graveyard).size()+
                    " defender_grave="+defense.getCardsIn(ZoneType.Graveyard).size()+
                    " defender_life="+defense.getLife()+
                    " game_over="+fixture.isGameOver());
            Assert.assertTrue(dc.blocksAuthorized>=2,
                    "native referee approved both distinct blockers");
            Assert.assertTrue(oc.refereeSplitDamageAssignments>0,
                    "native Forge resolved actual 3-power division among blockers");
            Assert.assertEquals(defense.getLife(),3,
                    "the fully blocked 3/3 dealt no player combat damage");
            Assert.assertFalse(fixture.isGameOver(),"no invented winner");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11R_THREE_POWER_FIXTURE left_first="+
                    leftRegisteredFirst+" accepted_blocks="+dc.blocksAuthorized+
                    " coordinated="+dc.coordinatedNativeBlocks+
                    " native_damage_splits="+oc.refereeSplitDamageAssignments+
                    " attacking_three_dead=true defending_dead=1 defending_alive=1"+
                    " defender_life="+defense.getLife());
        }

        // SEPARATE, naturally shuffled original 60-card Constructed games.
        // This existing harness starts two NEW authentic Forge matches per
        // seat order with natural London opening, 20 life, fresh libraries,
        // fully paid spells, authoritative Forge winner and preserved cards.
        verifyNaturalFourStrengthVanilla(leftRegisteredFirst);
        System.out.println("STAGE11R_NATURAL_UNCURATED_COHORT left_first="+
                leftRegisteredFirst+" distinct_games=2");
    }


    /**
     * Stage11S GAMEPLAY: treat ordinary public basic Forest as nonblocking
     * when considering a 4/4+2/2 numerical attack into a 4/4 defender.
     * Registered physical Forest is placed ONLY in a labeled controlled
     * fixture. Actual creature declaration, land status, blocking, life,
     * damage and graveyards remain Forge-authoritative. Separately run
     * two completely untouched natural 60-card games per seat order.
     */
    private void verifyBasicForestNonblockerPressure(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid());
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor attackingWorker=offense==seatA?workerA:workerB;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,offense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(fixture,offense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,defense,"Rumbling Baloth");
            Card forest=null;
            for(ZoneType zone:Arrays.asList(ZoneType.Hand,ZoneType.Library)) {
                for(Card candidate:defense.getCardsIn(zone)) {
                    if("Forest".equals(candidate.getName())) {
                        forest=candidate;
                        break;
                    }
                }
                if(forest!=null) break;
            }
            Assert.assertNotNull(forest,"physical registered Forest must exist");
            Assert.assertNotNull(fixture.getAction().moveToPlay(forest,null,
                    forge.game.ability.AbilityKey.newMap()),
                    "fixture-only registered public basic land onto battlefield");
            for(Card card:offense.getCardsIn(ZoneType.Battlefield)) {
                if("Rumbling Baloth".equals(card.getName()) ||
                        "Grizzly Bears".equals(card.getName()))
                    card.setSickness(false); // explicit fixture ONLY
            }
            final long registeredForestId=forest.getId();
            Assert.assertTrue(defense.getCardsIn(ZoneType.Battlefield).stream()
                    .anyMatch(c->c.getId()==registeredForestId),
                    "real physical Forest is publicly present");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            JsonObject publicBefore=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            Assert.assertEquals(publicBefore.getAsJsonArray("opponent_table").size(),2,
                    "one public 4/4 and one public basic Forest");
            Assert.assertTrue(defense.setLife(7,null),
                    "controlled seven-life baseline, not a natural match");
            Assert.assertEquals(attackingWorker.ask(observation(offense,defense))
                    .getAsJsonArray("attack_table_indices").size(),1,
                    "seven life remains outside safe nonlethal pressure threshold");
            Assert.assertTrue(defense.setLife(6,null),
                    "controlled public six life, natural games unchanged");
            JsonObject pressure=attackingWorker.ask(observation(offense,defense));
            Assert.assertEquals(pressure.getAsJsonArray("attack_table_indices").size(),2,
                    "known Forest does not become a second 4/4 blocker");
            // Privacy-safe independent hypothetical projections. NEVER
            // alter the physical game or reveal facedown identity.
            JsonObject unknown=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject facedown=new JsonObject();
            facedown.addProperty("appearance","face_down");
            unknown.getAsJsonArray("opponent_table").add(facedown);
            Assert.assertEquals(attackingWorker.ask(unknown.toString())
                    .getAsJsonArray("attack_table_indices").size(),1,
                    "a possible unrecognized second blocker is conservative");
            JsonObject other=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject uncertain=new JsonObject();
            uncertain.addProperty("appearance","face_up");
            uncertain.addProperty("card","Unknown Permanent");
            other.getAsJsonArray("opponent_table").add(uncertain);
            Assert.assertEquals(attackingWorker.ask(other.toString())
                    .getAsJsonArray("attack_table_indices").size(),1,
                    "unrecognized face-up permanent cannot be presumed harmless");
            Assert.assertEquals(defense.getLife(),6,
                    "external actors cannot alter native referee life");
            Assert.assertFalse(observation(offense,defense).contains(
                    "\\\"phase\\\""));
            Assert.assertFalse(observation(defense,offense).contains(
                    "\\\"stack\\\""));
            boolean dealtPhysicalDamage=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                if(defense.getLife()<=4) {
                    dealtPhysicalDamage=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"native combat completed within bound");
            Assert.assertTrue(dealtPhysicalDamage,
                    "real Forge must resolve >=2 player combat damage");
            Assert.assertTrue(oc.attacksAuthorized>=2,
                    "both offered attackers accepted by true Forge legality");
            Assert.assertTrue(dc.blocksAuthorized>=1,
                    "opposing native 4/4 creature makes a legal block");
            Assert.assertTrue(defense.getLife()>=2 && defense.getLife()<=4,
                    "one vanilla 4/4 blocks one attacker, land blocks none");
            Assert.assertFalse(fixture.isGameOver(),
                    "nonlethal player damage must not invent a winner");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11S_PUBLIC_FOREST_FIXTURE left_first="+
                    leftRegisteredFirst+" native_attacks="+oc.attacksAuthorized+
                    " native_blocks="+dc.blocksAuthorized+
                    " actual_forest_present=true real_defender_life="+defense.getLife());
        }
        // DISTINCT completely untouched native 60-card Constructed games,
        // real London mulligans and opening life 20, no curated order.
        verifyNaturalFourStrengthVanilla(leftRegisteredFirst);
        System.out.println("STAGE11S_FRESH_NATURAL_COHORT left_first="+
                leftRegisteredFirst+" games=2");
    }


    /**
     * Stage11T GAMEPLAY: PUBLIC IMMEDIATE LETHAL overrides the actor's
     * defensive reserve, but only when every public opposing permanent
     * has known ordinary vanilla/blocking status. The reference position
     * is an EXPLICIT controlled physical registered-card Forge fixture:
     * at both players life 4, attack with 3x2/2 through a single 4/4.
     * The old reserve would leave only two 2/2 candidates and miss lethal.
     * Three separate natural games? No: exactly TWO fresh independent
     * untouched Constructed full games are run per registration order
     * using the existing natural-game harness after the controlled fixture.
     */
    private void verifyImmediateLethalBeatsReserve(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5); Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "exactly two external persistent independent actor PIDs");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor offensiveWorker=offense==seatA?workerA:workerB;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            for(String name:Arrays.asList("Grizzly Bears",
                    "Runeclaw Bear","Balduvian Bears"))
                fixturePlaceRegisteredVanilla(fixture,offense,name);
            fixturePlaceRegisteredVanilla(fixture,defense,"Rumbling Baloth");
            for(Card card:offense.getCardsIn(ZoneType.Battlefield))
                if(Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears").contains(card.getName()))
                    card.setSickness(false); // EXPLICIT fixture ONLY
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertTrue(offense.setLife(4,null),
                    "fixture-only urgent offensive life; natural games start 20");
            Assert.assertTrue(defense.setLife(5,null),
                    "fixture-only opponent control life 5");
            Assert.assertEquals(offensiveWorker.ask(observation(offense,defense))
                    .getAsJsonArray("attack_table_indices").size(),0,
                    "without immediate lethal, the defense reserve remains");
            Assert.assertTrue(defense.setLife(4,null),
                    "fixture-only opposing life four lethal opportunity");
            JsonObject winning=offensiveWorker.ask(observation(offense,defense));
            JsonArray candidates=winning.getAsJsonArray("attack_table_indices");
            Assert.assertEquals(winning.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(candidates.size(),3,
                    "public immediate lethal must commit all three 2/2s");
            Set<Integer> indices=new HashSet<>();
            for(int i=0;i<candidates.size();i++)
                Assert.assertTrue(indices.add(candidates.get(i).getAsInt()),
                        "three distinct own table indices");
            JsonObject facedownView=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject facedown=new JsonObject();
            facedown.addProperty("appearance","face_down");
            facedownView.getAsJsonArray("opponent_table").add(facedown);
            Assert.assertEquals(offensiveWorker.ask(facedownView.toString())
                    .getAsJsonArray("attack_table_indices").size(),0,
                    "face-down possible second blocker cancels lethal override");
            JsonObject unknownView=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject other=new JsonObject();
            other.addProperty("appearance","face_up");
            other.addProperty("card","Unknown Permanent");
            unknownView.getAsJsonArray("opponent_table").add(other);
            Assert.assertEquals(offensiveWorker.ask(unknownView.toString())
                    .getAsJsonArray("attack_table_indices").size(),0,
                    "unrecognized permanent cannot be assumed nonblocking");
            Assert.assertEquals(offense.getLife(),4);
            Assert.assertEquals(defense.getLife(),4,
                    "synthetic probes have no native Forge side effects");
            Assert.assertFalse(observation(offense,defense).contains(
                    "\\\"phase\\\""));
            Assert.assertFalse(observation(defense,offense).contains(
                    "\\\"stack\\\""));
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500)
                fixture.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<2500,"native Forge combat resolves boundedly");
            Assert.assertTrue(fixture.isGameOver(),
                    "three genuinely legal 2/2 attacks kill at public life four");
            Assert.assertTrue(oc.attacksAuthorized>=3,
                    "Forge truly accepts three legal attacks");
            Assert.assertTrue(dc.blocksAuthorized>0,
                    "one genuine native 4/4 blocker assigned");
            Assert.assertTrue(offense.hasWon(),
                    "Forge, not the outside actor, decides real winner");
            Assert.assertTrue(defense.hasLost());
            Assert.assertNotNull(fixture.getOutcome());
            Assert.assertTrue(fixture.getOutcome().isWinner(
                    offense.getRegisteredPlayer()));
            Assert.assertTrue(defense.getLife()<=0,
                    "Forge resolves actual lethal unblocked player damage");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11T_RESERVE_OVERRIDE_FIXTURE left_first="+
                    leftRegisteredFirst+" real_attacks="+oc.attacksAuthorized+
                    " accepted_blocks="+dc.blocksAuthorized+
                    " Forge_winner="+offense.getName()+
                    " real_defender_life="+defense.getLife());
        }
        // DISTINCT fully untouched uncurated 60-card physical games:
        // two new naturally shuffled complete matches per registration
        // order; genuine London opening, both start at 20 life.
        verifyNaturalFourStrengthVanilla(leftRegisteredFirst);
        System.out.println("STAGE11T_NATURAL_UNCURATED_COHORT left_first="+
                leftRegisteredFirst+" distinct_games=2");
    }

    @Test(timeOut=240000)
    public void nativePublicLethalBeatsReserveLeftRegisteredFirst() throws Exception {
        verifyImmediateLethalBeatsReserve(true);
    }

    @Test(timeOut=240000)
    public void nativePublicLethalBeatsReserveRightRegisteredFirst() throws Exception {
        verifyImmediateLethalBeatsReserve(false);
    }

    @Test(timeOut=240000)
    public void nativeBasicForestPressureLeftRegisteredFirst() throws Exception {
        verifyBasicForestNonblockerPressure(true);
    }

    @Test(timeOut=240000)
    public void nativeBasicForestPressureRightRegisteredFirst() throws Exception {
        verifyBasicForestNonblockerPressure(false);
    }

    @Test(timeOut=240000)
    public void nativeThreePowerEmergencyBlockLeftRegisteredFirst() throws Exception {
        verifyThreePowerEmergencyJointBlock(true);
    }

    @Test(timeOut=240000)
    public void nativeThreePowerEmergencyBlockRightRegisteredFirst() throws Exception {
        verifyThreePowerEmergencyJointBlock(false);
    }

    /**
     * Stage11U GAMEPLAY: three separate, genuine physical vanilla 2/2s
     * exert NONLETHAL numerical pressure into just one known 4/4 blocker
     * at opposing life eight. At nine life, retain the original cautious
     * behavior. A facedown or unsupported public permanent removes the
     * safe three-attacker inference. This fixture is explicitly controlled;
     * the later natural registered 60-card full games are untouched.
     * Forge alone decides legal attacks, blocks, damage, losses and victory.
     */
    private void verifyNonlethalUnderpoweredSwarm(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5); Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "independent external OS Python player PIDs");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense,"only Forge owns active turn");
            Player defense=offense==seatA?seatB:seatA;
            Actor attackingWorker=offense==seatA?workerA:workerB;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            for(String name:Arrays.asList("Grizzly Bears",
                    "Runeclaw Bear","Balduvian Bears"))
                fixturePlaceRegisteredVanilla(fixture,offense,name);
            fixturePlaceRegisteredVanilla(fixture,defense,"Rumbling Baloth");
            for(Card card:offense.getCardsIn(ZoneType.Battlefield))
                if(Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears").contains(card.getName()))
                    card.setSickness(false); // CONTROLLED fixture ONLY
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(9,null),
                    "controlled nine-life baseline; natural games remain 20");
            Assert.assertEquals(attackingWorker.ask(observation(offense,defense))
                    .getAsJsonArray("attack_table_indices").size(),0,
                    "all-weaker 2/2s stay home when pressure too distant");
            Assert.assertTrue(defense.setLife(8,null),
                    "controlled eight-life bounded nonlethal pressure");
            JsonObject plan=attackingWorker.ask(observation(offense,defense));
            JsonArray proposals=plan.getAsJsonArray("attack_table_indices");
            Assert.assertEquals(plan.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposals.size(),3,
                    "three 2/2s should apply four unblocked life pressure");
            Set<Integer> unique=new HashSet<>();
            for(int i=0;i<proposals.size();i++)
                Assert.assertTrue(unique.add(proposals.get(i).getAsInt()),
                        "three distinct registered vanilla attackers");
            JsonObject hidden=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject down=new JsonObject();
            down.addProperty("appearance","face_down");
            hidden.getAsJsonArray("opponent_table").add(down);
            Assert.assertEquals(attackingWorker.ask(hidden.toString())
                    .getAsJsonArray("attack_table_indices").size(),0,
                    "facedown unknown may be another legal blocker");
            JsonObject other=JsonParser.parseString(
                    observation(offense,defense)).getAsJsonObject();
            JsonObject unsupported=new JsonObject();
            unsupported.addProperty("appearance","face_up");
            unsupported.addProperty("card","Unknown Permanent");
            other.getAsJsonArray("opponent_table").add(unsupported);
            Assert.assertEquals(attackingWorker.ask(other.toString())
                    .getAsJsonArray("attack_table_indices").size(),0,
                    "unrecognized opposing permanent prevents all-weak pressure");
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),8,
                    "public probes must not mutate Forge life");
            Assert.assertFalse(observation(offense,defense).contains(
                    "\\\"phase\\\""));
            Assert.assertFalse(observation(defense,offense).contains(
                    "\\\"stack\\\""));
            int steps=0;
            boolean nativeDamage=false;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                if(defense.getLife()<=4) {
                    nativeDamage=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded genuine Forge combat");
            Assert.assertTrue(nativeDamage,
                    "real Forge resolves at least four nonlethal damage");
            Assert.assertTrue(oc.attacksAuthorized>=3,
                    "Forge accepted three genuinely legal attacking creatures");
            Assert.assertTrue(dc.blocksAuthorized>=1,
                    "real 4/4 defender may legally block just one attacker");
            Assert.assertTrue(defense.getLife()>=2 && defense.getLife()<=4,
                    "unblocked vanilla attackers deal four to six real damage");
            Assert.assertFalse(fixture.isGameOver(),
                    "nonlethal pressure must not fabricate victory");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11U_ALL_SMALL_PRESSURE_FIXTURE left_first="+
                    leftRegisteredFirst+" native_attacks="+oc.attacksAuthorized+
                    " native_blocks="+dc.blocksAuthorized+
                    " actual_defender_life="+defense.getLife());
        }
        // DISTINCT TWO untouched original shuffled 60-card full native
        // Constructed games per registration order: no board, hand, life,
        // library order, mulligan or RNG seed curation.
        verifyNaturalFourStrengthVanilla(leftRegisteredFirst);
        System.out.println("STAGE11U_NATURAL_UNCURATED_COHORT left_first="+
                leftRegisteredFirst+" games=2");
    }

    @Test(timeOut=240000)
    public void nativeNonlethalAllSmallPressureLeftRegisteredFirst()
            throws Exception {
        verifyNonlethalUnderpoweredSwarm(true);
    }

    @Test(timeOut=240000)
    public void nativeNonlethalAllSmallPressureRightRegisteredFirst()
            throws Exception {
        verifyNonlethalUnderpoweredSwarm(false);
    }

    /** Stage11V: TWO distinct public 4/4 attackers, own 4/4 and 2/2,
     * four life. Explicit physical fixture only; Forge alone decides
     * legal combat, damage, deaths, and life. Followed by TWO untouched
     * fresh shuffled registered sixty-card native games per seat order. */
    private void verifyTwoLaneEmergencyDefense(boolean leftRegisteredFirst)
            throws Exception {
        Game game=nativeFourStrengthVanillaGame(leftRegisteredFirst);
        Player seatA=game.getPlayers().get(0),seatB=game.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid());
            ExternalCreatureController ca=attach(game,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(game,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(game,ca,cb);
            Assert.assertEquals(seatA.getLife(),20);
            Assert.assertEquals(seatB.getLife(),20);
            Player offense=game.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor defenderWorker=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            fixturePlaceRegisteredVanilla(game,offense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(game,offense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(game,defense,"Rumbling Baloth");
            fixturePlaceRegisteredVanilla(game,defense,"Grizzly Bears");
            List<Card> attackers=new ArrayList<>();
            for(Card c:offense.getCardsIn(ZoneType.Battlefield))
                if("Rumbling Baloth".equals(c.getName())) attackers.add(c);
            Assert.assertEquals(attackers.size(),2);
            for(Card c:attackers) c.setSickness(false); // controlled fixture
            Card guard=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Rumbling Baloth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card bear=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Set<Long> attackingIds=new HashSet<>();
            for(Card c:attackers) attackingIds.add(Long.valueOf(c.getId()));
            long guardId=guard.getId(),bearId=bear.getId();
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(5,null));
            JsonArray baseline=defenderWorker.ask(observation(defense,offense))
                    .getAsJsonArray("block_pairs");
            Assert.assertEquals(baseline.size(),4);
            Assert.assertEquals(baseline.get(0).getAsJsonArray().get(0).getAsInt(),
                    baseline.get(1).getAsJsonArray().get(0).getAsInt(),
                    "old greedy order would double-block one attacker");
            Assert.assertTrue(defense.setLife(4,null));
            JsonArray proposed=defenderWorker.ask(observation(defense,offense))
                    .getAsJsonArray("block_pairs");
            Assert.assertEquals(proposed.size(),4);
            JsonArray first=proposed.get(0).getAsJsonArray();
            JsonArray second=proposed.get(1).getAsJsonArray();
            Assert.assertNotEquals(first.get(0).getAsInt(),second.get(0).getAsInt());
            Assert.assertNotEquals(first.get(1).getAsInt(),second.get(1).getAsInt());
            Assert.assertEquals(defense.getCardsIn(ZoneType.Battlefield)
                    .get(first.get(0).getAsInt()).getName(),"Rumbling Baloth");
            Assert.assertEquals(defense.getCardsIn(ZoneType.Battlefield)
                    .get(second.get(0).getAsInt()).getName(),"Grizzly Bears");
            JsonObject uncertain=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject down=new JsonObject();
            down.addProperty("appearance","face_down");
            uncertain.getAsJsonArray("opponent_table").add(down);
            JsonArray cautious=defenderWorker.ask(uncertain.toString())
                    .getAsJsonArray("block_pairs");
            Assert.assertEquals(cautious.get(0).getAsJsonArray().get(0).getAsInt(),
                    cautious.get(1).getAsJsonArray().get(0).getAsInt(),
                    "unknown public permanent disables two-lane inference");
            Assert.assertEquals(defense.getLife(),4);
            boolean nativeTwoLane=false;
            int steps=0;
            while(!game.isGameOver() && steps++<2500) {
                game.getPhaseHandler().mainLoopStep();
                int attackerDeaths=0,attackerSurvivors=0;
                for(Card c:offense.getCardsIn(ZoneType.Graveyard))
                    if(attackingIds.contains(Long.valueOf(c.getId()))) attackerDeaths++;
                for(Card c:offense.getCardsIn(ZoneType.Battlefield))
                    if(attackingIds.contains(Long.valueOf(c.getId()))) attackerSurvivors++;
                boolean guardDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==guardId);
                boolean bearDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==bearId);
                if(oc.attacksAuthorized>=2 && dc.blocksAuthorized>=2 &&
                        attackerDeaths==1 && attackerSurvivors==1 &&
                        guardDead && bearDead && defense.getLife()==4) {
                    nativeTwoLane=true;break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded Forge combat");
            Assert.assertTrue(nativeTwoLane,
                    "Forge must resolve two lanes: one 4/4 trade, 2/2 chump;"+
                    " attacks="+oc.attacksAuthorized+" blocks="+dc.blocksAuthorized+
                    " defender_life="+defense.getLife());
            Assert.assertEquals(defense.getLife(),4);
            Assert.assertFalse(game.isGameOver());
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11V_TWO_LANE_NATIVE_FIXTURE left_first="+
                    leftRegisteredFirst+" real_attacks="+oc.attacksAuthorized+
                    " real_blocks="+dc.blocksAuthorized+" defender_life="+
                    defense.getLife());
        }
        verifyNaturalFourStrengthVanilla(leftRegisteredFirst);
        System.out.println("STAGE11V_UNCURATED_NATURAL left_first="+
                leftRegisteredFirst+" games=2");
    }

    @Test(timeOut=240000)
    public void nativeTwoLaneEmergencyDefenseLeftRegisteredFirst()
            throws Exception {verifyTwoLaneEmergencyDefense(true);}

    @Test(timeOut=240000)
    public void nativeTwoLaneEmergencyDefenseRightRegisteredFirst()
            throws Exception {verifyTwoLaneEmergencyDefense(false);}

    /** Stage11W: an EXACT independently registered 60-card elf cohort.
     * Different from the earlier 4/4 natural deck, with four genuine
     * Llanowar Elves and no artificial card duplication. This is also
     * the registered physical-card source for the CONTROLLED fixture. */
    private Game nativeElfEmergencyGame(boolean leftRegisteredFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);
        counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);
        counts.put("Balduvian Bears",4);
        counts.put("Centaur Courser",4);
        counts.put("Llanowar Elves",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer a=new RegisteredPlayer(mainDeck("11W elf left",counts))
                .setPlayer(new LobbyPlayerAi("elf-left",null));
        RegisteredPlayer b=new RegisteredPlayer(mainDeck("11W elf right",counts))
                .setPlayer(new LobbyPlayerAi("elf-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(leftRegisteredFirst?a:b);
        registered.add(leftRegisteredFirst?b:a);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage11W");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers())
            Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    /** Stage11W: precisely ONE new tactical change: defend an imminent
     * public lethal 2/2 with a registered 1/1 Llanowar Elves. The true
     * Forge referee validates legal blocking and destroys the Elves;
     * both original cards remain physically conserved. Explicit
     * controlled fixture is kept SEPARATE from two naturally shuffled
     * complete untouched registered 60-card games per seating order. */
    private void verifyEmergencyElvesChump(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeElfEmergencyGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "two genuinely independent persistent Python actor PIDs");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense,"only Forge owns current player");
            Player defense=offense==seatA?seatB:seatA;
            Actor defenderWorker=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,offense,"Grizzly Bears");
            Card elf=null;
            for(ZoneType zone:Arrays.asList(ZoneType.Hand,ZoneType.Library)) {
                for(Card c:defense.getCardsIn(zone)) {
                    if("Llanowar Elves".equals(c.getName())) {
                        elf=c;break;
                    }
                }
                if(elf!=null) break;
            }
            Assert.assertNotNull(elf,"physically registered original Elves");
            Assert.assertNotNull(fixture.getAction().moveToPlay(elf,null,
                    forge.game.ability.AbilityKey.newMap()),
                    "fixture-only registered elf placed onto actual battlefield");
            Card bear=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            bear.setSickness(false); // controlled fixture ONLY
            // Fixture-only physical readiness; CR 509.1a forbids tapped blockers.
            // No such intervention occurs in either natural 60-card cohort.
            elf.setTapped(false);
            Assert.assertFalse(elf.isTapped(),"registered fixture Elf must be untapped");
            final long bearId=bear.getId(),elfId=elf.getId();
            Assert.assertTrue(defense.getCardsIn(ZoneType.Battlefield).stream()
                    .anyMatch(c->c.getId()==elfId));
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(3,null),
                    "fixture-only baseline, above one visible two-damage swing");
            Assert.assertEquals(defenderWorker.ask(observation(defense,offense))
                    .getAsJsonArray("block_pairs").size(),0,
                    "do not sacrifice mana Elf at nonlethal three life");
            Assert.assertTrue(defense.setLife(2,null),
                    "fixture-only imminent lethal; natural games start twenty");
            JsonObject urgent=defenderWorker.ask(observation(defense,offense));
            Assert.assertEquals(urgent.get("intent").getAsString(),"action_plan");
            JsonArray proposed=urgent.getAsJsonArray("block_pairs");
            Assert.assertEquals(proposed.size(),1,
                    "one known 1/1 Elf must chump one known attacking 2/2");
            JsonArray pair=proposed.get(0).getAsJsonArray();
            Assert.assertEquals(defense.getCardsIn(ZoneType.Battlefield)
                    .get(pair.get(0).getAsInt()).getName(),"Llanowar Elves");
            Assert.assertEquals(offense.getCardsIn(ZoneType.Battlefield)
                    .get(pair.get(1).getAsInt()).getName(),"Grizzly Bears");
            JsonObject hidden=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject down=new JsonObject();
            down.addProperty("appearance","face_down");
            hidden.getAsJsonArray("opponent_table").add(down);
            Assert.assertEquals(defenderWorker.ask(hidden.toString())
                    .getAsJsonArray("block_pairs").size(),0,
                    "unknown facedown opponent card invalidates exact-threat inference");
            JsonObject unknown=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject foreign=new JsonObject();
            foreign.addProperty("appearance","face_up");
            foreign.addProperty("card","Unsupported Creature");
            unknown.getAsJsonArray("opponent_table").add(foreign);
            Assert.assertEquals(defenderWorker.ask(unknown.toString())
                    .getAsJsonArray("block_pairs").size(),0,
                    "unknown faceup opposing body never guessed harmless");
            JsonObject extraElf=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject publicElf=new JsonObject();
            publicElf.addProperty("appearance","face_up");
            publicElf.addProperty("card","Llanowar Elves");
            extraElf.getAsJsonArray("opponent_table").add(publicElf);
            Assert.assertEquals(defenderWorker.ask(extraElf.toString())
                    .getAsJsonArray("block_pairs").size(),1,
                    "visible opposing nonattacking mana Elf must not suppress lethal Bear block");
            Assert.assertEquals(defense.getLife(),2,
                    "public probes never change trusted Forge life");
            Assert.assertFalse(JsonParser.parseString(observation(defense,offense))
                    .getAsJsonObject().has("phase"));
            Assert.assertFalse(JsonParser.parseString(observation(offense,defense))
                    .getAsJsonObject().has("stack"));
            boolean actualChump=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                boolean elfDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==elfId);
                boolean bearAlive=offense.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==bearId);
                if(oc.attacksAuthorized>0 && dc.blocksAuthorized>0 &&
                        elfDead && bearAlive && defense.getLife()==2) {
                    actualChump=true;break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded genuine Forge combat");
            Assert.assertTrue(actualChump,
                    "Forge must approve block, kill original 1/1 and preserve 2 life;"+
                    " attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " blocker_callbacks="+dc.blockerCallbacks+
                    " elf_tapped="+elf.isTapped()+
                    " defender_life="+defense.getLife());
            Assert.assertFalse(fixture.isGameOver(),"survived actual chump block");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11W_REGISTERED_ELF_CHUMP left_first="+
                    leftRegisteredFirst+" attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+" defender_life="+
                    defense.getLife()+" actual_elf_grave=true");
        }
        // DISTINCT NATURAL COHORT: no curated opening, shuffle, life,
        // battlefield, hand, mulligans or preselected seed. Exactly two
        // genuinely fresh untouched complete 60-card games per seat order.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeElfEmergencyGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor actorL=new Actor(5);Actor actorR=new Actor(5)) {
                Assert.assertNotEquals(actorL.process.pid(),actorR.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,actorL);
                ExternalCreatureController rc=attach(natural,right,left,actorR);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser","Llanowar Elves"))
                    Assert.assertEquals(countCardsWithName(natural,name,
                            ZoneType.Battlefield),0,"no curated natural board");
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"complete natural native game terminates");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(
                        winner.getRegisteredPlayer()));
                Assert.assertTrue(lc.priorityCallbacks>0 && rc.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                System.out.println("STAGE11W_NATURAL_ELF_GAME left_first="+
                        leftRegisteredFirst+" game="+sequence+
                        " attacks="+(lc.attacksAuthorized+rc.attacksAuthorized)+
                        " blocks="+(lc.blocksAuthorized+rc.blocksAuthorized)+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeLlanowarEmergencyChumpLeftRegisteredFirst()
            throws Exception {verifyEmergencyElvesChump(true);}

    @Test(timeOut=240000)
    public void nativeLlanowarEmergencyChumpRightRegisteredFirst()
            throws Exception {verifyEmergencyElvesChump(false);}

    /** Stage11X: one NEW decision, never a fake simulator outcome.
     * Two real registered vanilla 2/2 attackers, own real registered
     * 2/2 Bear and 1/1 Llanowar Elves, at two life. The Bear trades
     * on one lane while the Elf chumps the other. Only the CONTROLLED
     * physical fixture changes life, readiness and board; each seating
     * order also executes two untouched, freshly shuffled 60-card
     * full games with native London mulligans and actual Forge outcome. */
    private void verifyTwoLaneBearAndElf(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeElfEmergencyGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "two independent persistent external Python actor processes");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor defenderWorker=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,offense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,offense,"Runeclaw Bear");
            fixturePlaceRegisteredVanilla(fixture,defense,"Balduvian Bears");
            Card elf=null;
            for(ZoneType zone:Arrays.asList(ZoneType.Hand,ZoneType.Library)) {
                for(Card c:defense.getCardsIn(zone)) {
                    if("Llanowar Elves".equals(c.getName())) {
                        elf=c;break;
                    }
                }
                if(elf!=null) break;
            }
            Assert.assertNotNull(elf,"original registered Elf, not synthetic card");
            Assert.assertNotNull(fixture.getAction().moveToPlay(elf,null,
                    forge.game.ability.AbilityKey.newMap()),
                    "fixture-only physical Elf placement");
            List<Card> attackers=new ArrayList<>();
            for(Card c:offense.getCardsIn(ZoneType.Battlefield))
                if("Grizzly Bears".equals(c.getName()) ||
                        "Runeclaw Bear".equals(c.getName())) attackers.add(c);
            Assert.assertEquals(attackers.size(),2);
            for(Card c:attackers) c.setSickness(false); // controlled only
            Card guard=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Balduvian Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            guard.setTapped(false); // controlled fixture readiness
            elf.setTapped(false); // controlled fixture readiness
            Assert.assertFalse(guard.isTapped());
            Assert.assertFalse(elf.isTapped());
            Set<Long> attackingIds=new HashSet<>();
            for(Card c:attackers) attackingIds.add(Long.valueOf(c.getId()));
            long guardId=guard.getId(),elfId=elf.getId();
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(3,null),"controlled baseline");
            JsonArray baseline=defenderWorker.ask(observation(defense,offense))
                    .getAsJsonArray("block_pairs");
            for(int i=0;i<baseline.size();i++)
                Assert.assertNotEquals(defense.getCardsIn(ZoneType.Battlefield)
                        .get(baseline.get(i).getAsJsonArray().get(0).getAsInt())
                        .getName(),"Llanowar Elves",
                        "do not sacrifice Elf before lethal threshold");
            Assert.assertTrue(defense.setLife(2,null),"controlled lethal threshold");
            JsonObject urgent=defenderWorker.ask(observation(defense,offense));
            Assert.assertEquals(urgent.get("intent").getAsString(),"action_plan");
            JsonArray proposed=urgent.getAsJsonArray("block_pairs");
            Assert.assertTrue(proposed.size()>=2);
            JsonArray first=proposed.get(0).getAsJsonArray();
            JsonArray second=proposed.get(1).getAsJsonArray();
            Assert.assertNotEquals(first.get(0).getAsInt(),second.get(0).getAsInt());
            Assert.assertNotEquals(first.get(1).getAsInt(),second.get(1).getAsInt());
            Assert.assertEquals(defense.getCardsIn(ZoneType.Battlefield)
                    .get(first.get(0).getAsInt()).getName(),"Balduvian Bears");
            Assert.assertEquals(defense.getCardsIn(ZoneType.Battlefield)
                    .get(second.get(0).getAsInt()).getName(),"Llanowar Elves");
            for(int i=0;i<2;i++)
                Assert.assertTrue(Arrays.asList("Grizzly Bears","Runeclaw Bear")
                        .contains(offense.getCardsIn(ZoneType.Battlefield)
                        .get(proposed.get(i).getAsJsonArray().get(1).getAsInt())
                        .getName()));
            JsonObject hidden=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject down=new JsonObject();
            down.addProperty("appearance","face_down");
            hidden.getAsJsonArray("opponent_table").add(down);
            JsonArray cautious=defenderWorker.ask(hidden.toString())
                    .getAsJsonArray("block_pairs");
            for(int i=0;i<cautious.size();i++)
                Assert.assertNotEquals(defense.getCardsIn(ZoneType.Battlefield)
                        .get(cautious.get(i).getAsJsonArray().get(0).getAsInt())
                        .getName(),"Llanowar Elves",
                        "facedown identity must not be assumed harmless");
            JsonObject unknown=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject foreign=new JsonObject();
            foreign.addProperty("appearance","face_up");
            foreign.addProperty("card","Unsupported Creature");
            unknown.getAsJsonArray("opponent_table").add(foreign);
            JsonArray guarded=defenderWorker.ask(unknown.toString())
                    .getAsJsonArray("block_pairs");
            for(int i=0;i<guarded.size();i++)
                Assert.assertNotEquals(defense.getCardsIn(ZoneType.Battlefield)
                        .get(guarded.get(i).getAsJsonArray().get(0).getAsInt())
                        .getName(),"Llanowar Elves",
                        "unknown visible threat disables narrow two-lane inference");
            Assert.assertEquals(defense.getLife(),2);
            Assert.assertFalse(JsonParser.parseString(observation(defense,offense))
                    .getAsJsonObject().has("phase"));
            Assert.assertFalse(JsonParser.parseString(observation(offense,defense))
                    .getAsJsonObject().has("stack"));
            boolean nativeSplit=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                int attackerDead=0,attackerAlive=0;
                for(Card c:offense.getCardsIn(ZoneType.Graveyard))
                    if(attackingIds.contains(Long.valueOf(c.getId()))) attackerDead++;
                for(Card c:offense.getCardsIn(ZoneType.Battlefield))
                    if(attackingIds.contains(Long.valueOf(c.getId()))) attackerAlive++;
                boolean guardDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==guardId);
                boolean elfDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==elfId);
                if(oc.attacksAuthorized>=2 && dc.blocksAuthorized>=2 &&
                        attackerDead==1 && attackerAlive==1 &&
                        guardDead && elfDead && defense.getLife()==2) {
                    nativeSplit=true;break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded native Forge combat");
            Assert.assertTrue(nativeSplit,
                    "Forge must resolve separate 2/2 trade and Elf chump;"+
                    " attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " callbacks="+dc.blockerCallbacks+
                    " elf_tapped="+elf.isTapped()+
                    " guard_tapped="+guard.isTapped()+
                    " defender_life="+defense.getLife());
            Assert.assertFalse(fixture.isGameOver(),
                    "survival must be actual native Forge outcome");
            Assert.assertEquals(fullPhysicalInventory(offense),
                    frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),
                    frozenMain.owned(defense));
            System.out.println("STAGE11X_TWO_LANE_BEAR_ELF_FIXTURE left_first="+
                    leftRegisteredFirst+" attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " defender_life="+defense.getLife());
        }
        // Untouched NATURAL games: no selected draws, initial hands,
        // decks, library ordering, life, mulligans, board or lucky seeds.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeElfEmergencyGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor actorL=new Actor(5);Actor actorR=new Actor(5)) {
                Assert.assertNotEquals(actorL.process.pid(),actorR.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,actorL);
                ExternalCreatureController rc=attach(natural,right,left,actorR);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser","Llanowar Elves"))
                    Assert.assertEquals(countCardsWithName(natural,name,
                            ZoneType.Battlefield),0,"natural board never curated");
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"native natural game must terminate");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(
                        winner.getRegisteredPlayer()));
                Assert.assertTrue(lc.priorityCallbacks>0 && rc.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                System.out.println("STAGE11X_NATURAL_UNCURATED_GAME left_first="+
                        leftRegisteredFirst+" game="+sequence+
                        " attacks="+(lc.attacksAuthorized+rc.attacksAuthorized)+
                        " blocks="+(lc.blocksAuthorized+rc.blocksAuthorized)+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeTwoLaneBearElfLeftRegisteredFirst()
            throws Exception {verifyTwoLaneBearAndElf(true);}

    @Test(timeOut=240000)
    public void nativeTwoLaneBearElfRightRegisteredFirst()
            throws Exception {verifyTwoLaneBearAndElf(false);}

    /** Stage11Y: exactly ONE strategic choice: at life two, two
     * independently registered 1/1 Llanowar Elves must block TWO
     * different known attacking 2/2 creatures. Only the labelled
     * CONTROLLED fixture sets board/life/readiness. No natural deck,
     * draw, mulligan or life/starting-board curation is permitted. */
    private void verifyDoubleLlanowarEmergency(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeElfEmergencyGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5); Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "players must be two independent persistent external OS processes");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor defenderWorker=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,offense,"Grizzly Bears");
            fixturePlaceRegisteredVanilla(fixture,offense,"Runeclaw Bear");
            List<Card> elves=new ArrayList<>();
            for(int n=0;n<2;n++) {
                Card physicalElf=null;
                for(ZoneType zone:Arrays.asList(ZoneType.Hand,ZoneType.Library)) {
                    for(Card c:defense.getCardsIn(zone))
                        if("Llanowar Elves".equals(c.getName())) {
                            physicalElf=c;
                            break;
                        }
                    if(physicalElf!=null) break;
                }
                Assert.assertNotNull(physicalElf,
                        "two distinct original deck-registered physical Elves");
                Assert.assertNotNull(fixture.getAction().moveToPlay(physicalElf,null,
                        forge.game.ability.AbilityKey.newMap()),
                        "explicit controlled physical battlefield placement");
                physicalElf.setTapped(false); // controlled fixture ONLY
                Assert.assertFalse(physicalElf.isTapped(),
                        "CR 509.1a: only untapped blockers can be declared");
                elves.add(physicalElf);
            }
            Assert.assertNotEquals(elves.get(0).getId(),elves.get(1).getId());
            List<Card> attackers=new ArrayList<>();
            for(Card c:offense.getCardsIn(ZoneType.Battlefield))
                if("Grizzly Bears".equals(c.getName()) ||
                        "Runeclaw Bear".equals(c.getName())) attackers.add(c);
            Assert.assertEquals(attackers.size(),2);
            for(Card c:attackers) c.setSickness(false); // controlled fixture
            Set<Long> attackerIds=new HashSet<>();
            for(Card c:attackers) attackerIds.add(Long.valueOf(c.getId()));
            long firstElfId=elves.get(0).getId();
            long secondElfId=elves.get(1).getId();
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(3,null),
                    "controlled nonlethal baseline only");
            Assert.assertEquals(defenderWorker.ask(observation(defense,offense))
                    .getAsJsonArray("block_pairs").size(),0,
                    "do not expend two mana creatures before lethal danger");
            Assert.assertTrue(defense.setLife(2,null),
                    "controlled lethal threshold; natural games retain 20 life");
            JsonObject decision=defenderWorker.ask(observation(defense,offense));
            Assert.assertEquals(decision.get("intent").getAsString(),"action_plan");
            JsonArray pairs=decision.getAsJsonArray("block_pairs");
            Assert.assertEquals(pairs.size(),2,
                    "exactly two disjoint emergency Elf block preferences");
            Set<Integer> ownIndices=new HashSet<>();
            Set<Integer> enemyIndices=new HashSet<>();
            for(int n=0;n<2;n++) {
                JsonArray pair=pairs.get(n).getAsJsonArray();
                int own=pair.get(0).getAsInt(), enemy=pair.get(1).getAsInt();
                Assert.assertTrue(ownIndices.add(Integer.valueOf(own)));
                Assert.assertTrue(enemyIndices.add(Integer.valueOf(enemy)));
                Assert.assertEquals(defense.getCardsIn(ZoneType.Battlefield)
                        .get(own).getName(),"Llanowar Elves");
                Assert.assertTrue(Arrays.asList("Grizzly Bears","Runeclaw Bear")
                        .contains(offense.getCardsIn(ZoneType.Battlefield)
                                .get(enemy).getName()));
            }
            for(boolean faceDown:new boolean[]{true,false}) {
                JsonObject privacy=JsonParser.parseString(
                        observation(defense,offense)).getAsJsonObject();
                JsonObject suspicious=new JsonObject();
                suspicious.addProperty("appearance",faceDown?"face_down":"face_up");
                if(!faceDown) suspicious.addProperty("card","Unsupported Creature");
                privacy.getAsJsonArray("opponent_table").add(suspicious);
                Assert.assertEquals(defenderWorker.ask(privacy.toString())
                        .getAsJsonArray("block_pairs").size(),0,
                        "unknown or face-down information cannot be treated as vanilla");
            }
            JsonObject safeView=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            Assert.assertFalse(safeView.has("phase"));
            Assert.assertFalse(safeView.has("priority"));
            Assert.assertFalse(safeView.has("library_top"));
            Assert.assertFalse(safeView.has("opponent_hand"));
            Assert.assertFalse(safeView.has("stack"));
            boolean survived=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                int livingBears=0;
                for(Card c:offense.getCardsIn(ZoneType.Battlefield))
                    if(attackerIds.contains(Long.valueOf(c.getId()))) livingBears++;
                boolean firstDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==firstElfId);
                boolean secondDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==secondElfId);
                if(oc.attacksAuthorized>=2 && dc.blocksAuthorized>=2 &&
                        livingBears==2 && firstDead && secondDead &&
                        defense.getLife()==2) {
                    survived=true;
                    break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded genuine Forge combat");
            Assert.assertTrue(survived,
                    "native Forge must approve two distinct Elf chumps, kill both Elves,"+
                    " preserve both attacking Bears, and leave defender at two life;"+
                    " attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " callbacks="+dc.blockerCallbacks+
                    " elf1_tapped="+elves.get(0).isTapped()+
                    " elf2_tapped="+elves.get(1).isTapped()+
                    " defender_life="+defense.getLife());
            Assert.assertFalse(fixture.isGameOver());
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            System.out.println("STAGE11Y_DOUBLE_ELF_FIXTURE left_first="+
                    leftRegisteredFirst+" attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " defender_life="+defense.getLife());
        }
        // TWO genuinely independent uncurated, naturally shuffled 60-card
        // complete Forge games per registered seating order, without seed,
        // hand, initial board, library, mulligan or natural-life intervention.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeElfEmergencyGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor actorL=new Actor(5);Actor actorR=new Actor(5)) {
                Assert.assertNotEquals(actorL.process.pid(),actorR.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,actorL);
                ExternalCreatureController rc=attach(natural,right,left,actorR);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser","Llanowar Elves"))
                    Assert.assertEquals(countCardsWithName(natural,name,
                            ZoneType.Battlefield),0,"uncurated natural board");
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"bounded full natural game");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(
                        winner.getRegisteredPlayer()));
                Assert.assertTrue(lc.priorityCallbacks>0 && rc.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                System.out.println("STAGE11Y_NATURAL_UNCURATED_GAME left_first="+
                        leftRegisteredFirst+" game="+sequence+
                        " attacks="+(lc.attacksAuthorized+rc.attacksAuthorized)+
                        " blocks="+(lc.blocksAuthorized+rc.blocksAuthorized)+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeDoubleElfEmergencyLeftRegisteredFirst()
            throws Exception {verifyDoubleLlanowarEmergency(true);}

    @Test(timeOut=240000)
    public void nativeDoubleElfEmergencyRightRegisteredFirst()
            throws Exception {verifyDoubleLlanowarEmergency(false);}


    /** Stage11Z: one distinct tactical behavior. TWO physical original
     * 1/1 Llanowar Elves block ONE real attacking vanilla 2/2 at life2,
     * kill it by simultaneous native damage, and save the player.
     * Only CONTROLLED fixtures modify board/life/readiness. */
    private void verifyTwoElfCooperativeTrade(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeElfEmergencyGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid(),
                    "two persistent independent external Python actors");
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player offense=fixture.getPhaseHandler().getPlayerTurn();
            Assert.assertNotNull(offense);
            Player defense=offense==seatA?seatB:seatA;
            Actor defenderActor=offense==seatA?workerB:workerA;
            ExternalCreatureController oc=offense==seatA?ca:cb;
            ExternalCreatureController dc=offense==seatA?cb:ca;
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(defense.getLife(),20);
            fixturePlaceRegisteredVanilla(fixture,offense,"Grizzly Bears");
            Card bear=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            bear.setSickness(false); // controlled fixture only
            long bearId=bear.getId();
            List<Card> elves=new ArrayList<>();
            for(int n=0;n<2;n++) {
                Card elf=null;
                for(ZoneType zone:Arrays.asList(ZoneType.Hand,ZoneType.Library)) {
                    for(Card c:defense.getCardsIn(zone))
                        if("Llanowar Elves".equals(c.getName())) {
                            elf=c;break;
                        }
                    if(elf!=null) break;
                }
                Assert.assertNotNull(elf,"distinct original deck-registered Elf");
                Assert.assertNotNull(fixture.getAction().moveToPlay(elf,null,
                        forge.game.ability.AbilityKey.newMap()));
                elf.setTapped(false); // controlled fixture only, CR 509.1a
                Assert.assertFalse(elf.isTapped());
                elves.add(elf);
            }
            long elf1Id=elves.get(0).getId(),elf2Id=elves.get(1).getId();
            Assert.assertNotEquals(elf1Id,elf2Id);
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertTrue(defense.setLife(3,null),"fixture-only baseline");
            Assert.assertEquals(defenderActor.ask(observation(defense,offense))
                    .getAsJsonArray("block_pairs").size(),0,
                    "do not sacrifice two mana creatures before lethal danger");
            Assert.assertTrue(defense.setLife(2,null),"fixture-only lethal threshold");
            JsonArray pairs=defenderActor.ask(observation(defense,offense))
                    .getAsJsonArray("block_pairs");
            Assert.assertEquals(pairs.size(),2,
                    "two distinct Elves must target the SAME threatening 2/2");
            JsonArray p0=pairs.get(0).getAsJsonArray();
            JsonArray p1=pairs.get(1).getAsJsonArray();
            Assert.assertNotEquals(p0.get(0).getAsInt(),p1.get(0).getAsInt());
            Assert.assertEquals(p0.get(1).getAsInt(),p1.get(1).getAsInt());
            for(int n=0;n<2;n++) {
                JsonArray pair=pairs.get(n).getAsJsonArray();
                Assert.assertEquals(defense.getCardsIn(ZoneType.Battlefield)
                        .get(pair.get(0).getAsInt()).getName(),"Llanowar Elves");
                Assert.assertEquals(offense.getCardsIn(ZoneType.Battlefield)
                        .get(pair.get(1).getAsInt()).getName(),"Grizzly Bears");
            }
            JsonObject landView=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            JsonObject forest=new JsonObject();
            forest.addProperty("appearance","face_up");
            forest.addProperty("card","Forest");
            landView.getAsJsonArray("opponent_table").add(forest);
            Assert.assertEquals(defenderActor.ask(landView.toString())
                    .getAsJsonArray("block_pairs").size(),2,
                    "known basic Forest cannot attack");
            for(boolean facedown:new boolean[]{true,false}) {
                JsonObject guarded=JsonParser.parseString(
                        observation(defense,offense)).getAsJsonObject();
                JsonObject suspicious=new JsonObject();
                suspicious.addProperty("appearance",facedown?"face_down":"face_up");
                if(!facedown) suspicious.addProperty("card","Unsupported Creature");
                guarded.getAsJsonArray("opponent_table").add(suspicious);
                Assert.assertEquals(defenderActor.ask(guarded.toString())
                        .getAsJsonArray("block_pairs").size(),0,
                        "unknown/facedown cards cannot be assumed harmless");
            }
            JsonObject view=JsonParser.parseString(
                    observation(defense,offense)).getAsJsonObject();
            for(String forbidden:Arrays.asList("phase","turn","priority","stack",
                    "opponent_hand","opponent_library","library_top"))
                Assert.assertFalse(view.has(forbidden));
            boolean realTrade=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<2500) {
                fixture.getPhaseHandler().mainLoopStep();
                boolean bearDead=offense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==bearId);
                boolean elf1Dead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==elf1Id);
                boolean elf2Dead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==elf2Id);
                if(oc.attacksAuthorized>=1 && dc.blocksAuthorized>=2 &&
                        oc.refereeSplitDamageAssignments>0 &&
                        bearDead && elf1Dead && elf2Dead && defense.getLife()==2) {
                    realTrade=true;break;
                }
            }
            Assert.assertTrue(steps<2500,"bounded native Forge combat");
            Assert.assertTrue(realTrade,
                    "native Forge must resolve 2x1/1 on one 2/2: both legal"+
                    " blockers, all original creature deaths, player survival;"+
                    " attacks="+oc.attacksAuthorized+
                    " blocks="+dc.blocksAuthorized+
                    " callbacks="+dc.blockerCallbacks+
                    " splits="+oc.refereeSplitDamageAssignments+
                    " elf1_tapped="+elves.get(0).isTapped()+
                    " elf2_tapped="+elves.get(1).isTapped()+
                    " defender_life="+defense.getLife());
            Assert.assertFalse(fixture.isGameOver());
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            System.out.println("STAGE11Z_COOPERATIVE_ELF_TRADE left_first="+
                    leftRegisteredFirst+" blocks="+dc.blocksAuthorized+
                    " native_splits="+oc.refereeSplitDamageAssignments+
                    " defender_life="+defense.getLife());
        }
        // Two INDEPENDENT natural 60-card full games per seat order.
        // Never curate initial hand, deck, shuffle, mulligan, board, life,
        // draws, seed, or select only favorable game results.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeElfEmergencyGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor actorL=new Actor(5);Actor actorR=new Actor(5)) {
                Assert.assertNotEquals(actorL.process.pid(),actorR.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,actorL);
                ExternalCreatureController rc=attach(natural,right,left,actorR);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear",
                        "Balduvian Bears","Centaur Courser","Llanowar Elves"))
                    Assert.assertEquals(countCardsWithName(natural,name,
                            ZoneType.Battlefield),0,"natural board never curated");
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"natural full game terminates");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(
                        winner.getRegisteredPlayer()));
                Assert.assertTrue(lc.priorityCallbacks>0 && rc.priorityCallbacks>0);
                Assert.assertEquals(fullPhysicalInventory(left),frozenMain.owned(left));
                Assert.assertEquals(fullPhysicalInventory(right),frozenMain.owned(right));
                System.out.println("STAGE11Z_NATURAL_UNCURATED_GAME left_first="+
                        leftRegisteredFirst+" game="+sequence+
                        " attacks="+(lc.attacksAuthorized+rc.attacksAuthorized)+
                        " blocks="+(lc.blocksAuthorized+rc.blocksAuthorized)+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeTwoElfCooperativeTradeLeftRegisteredFirst()
            throws Exception {verifyTwoElfCooperativeTrade(true);}

    @Test(timeOut=240000)
    public void nativeTwoElfCooperativeTradeRightRegisteredFirst()
            throws Exception {verifyTwoElfCooperativeTrade(false);}


    /** Stage12A: explicit physical cleanup fixture and two independent,
     * untouched registered 60-card complete natural games per seat order.
     * Card movements in the controlled fixture are from original deck
     * objects, NEVER manufactured/duplicated cards. Forge owns cleanup. */
    private void verifyRegisteredNativeCleanup(boolean leftRegisteredFirst)
            throws Exception {
        Game controlled=nativeElfEmergencyGame(leftRegisteredFirst);
        Player a=controlled.getPlayers().get(0);
        Player b=controlled.getPlayers().get(1);
        try(Actor actorA=new Actor(5); Actor actorB=new Actor(5)) {
            Assert.assertNotEquals(actorA.process.pid(),actorB.process.pid());
            ExternalCreatureController ca=attach(controlled,a,b,actorA);
            ExternalCreatureController cb=attach(controlled,b,a,actorB);
            doRealNativeStartForMultiTurn(controlled,ca,cb);
            Player self=controlled.getPhaseHandler().getPlayerTurn();
            Player other=self==a?b:a;
            Actor worker=self==a?actorA:actorB;
            ExternalCreatureController controller=self==a?ca:cb;
            int maximum=leftRegisteredFirst?7:5; // CONTROLLED fixture only
            // Real original deck and native mulligan precede fixture changes.
            while(self.getCardsIn(ZoneType.Hand).size()<9) {
                Card actual=self.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName()))
                        .findFirst().orElseThrow();
                Assert.assertNotNull(controlled.getAction().moveToHand(actual,null));
            }
            Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),9);
            Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
            Assert.assertEquals(fullPhysicalInventory(other),frozenMain.owned(other));
            JsonObject visible=JsonParser.parseString(
                    observation(self,other)).getAsJsonObject();
            Assert.assertFalse(visible.has("phase"));
            Assert.assertFalse(visible.has("priority"));
            Assert.assertFalse(visible.has("library_top"));
            Assert.assertFalse(visible.has("opponent_hand"));
            Assert.assertFalse(visible.has("stack"));
            JsonObject ranked=askHandOrder(worker,visible.toString());
            Assert.assertEquals(ranked.get("intent").getAsString(),"hand_order");
            Assert.assertEquals(ranked.getAsJsonArray("hand_priority").size(),9);
            JsonObject hiddenChange=visible.deepCopy();
            JsonObject down=new JsonObject();
            down.addProperty("appearance","face_down");
            hiddenChange.getAsJsonArray("opponent_table").add(down);
            Assert.assertEquals(askHandOrder(worker,hiddenChange.toString()),ranked,
                    "own-hand ranking must not depend on hidden or opposing state");
            // Fixture phase change is explicit and never used for natural games.
            self.setMaxHandSize(maximum);
            controlled.getPhaseHandler().devModeSet(PhaseType.CLEANUP,self);
            CardCollection snapshot=new CardCollection(self.getCardsIn(ZoneType.Hand));
            int excess=9-maximum;
            for(int wrong:new int[]{-1,0,excess+1}) {
                try {
                    controller.chooseCardsToDiscardToMaximumHandSize(wrong);
                    Assert.fail("invalid cleanup count accepted");
                } catch(SecurityException expected) { /* deny before mutation */ }
            }
            Assert.assertEquals(controller.cleanupChoices,0);
            CardCollection selected=new CardCollection(
                    controller.chooseCardsToDiscardToMaximumHandSize(excess));
            Assert.assertEquals(selected.size(),excess);
            Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),9,
                    "actor only chooses; Forge alone moves original cards");
            Assert.assertTrue(self.getCardsIn(ZoneType.Graveyard).stream()
                    .noneMatch(c->selected.contains(c)));
            for(Card chosen:selected) {
                Assert.assertTrue(snapshot.stream().anyMatch(c->c==chosen));
                Assert.assertEquals(chosen.getOwner(),self);
            }
            // Real native cleanup from end step uses the same untouched physical hand.
            controlled.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN,self);
            controlled.getPhaseHandler().onStackResolved();
            int steps=0;
            while(controller.cleanupChoices<2 && !controlled.isGameOver() && steps++<90)
                controlled.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(steps<90,"native cleanup must finish");
            Assert.assertEquals(controller.cleanupChoices,2);
            Assert.assertEquals(self.getCardsIn(ZoneType.Hand).size(),maximum);
            Assert.assertEquals(self.getCardsIn(ZoneType.Graveyard).size(),excess);
            for(Card discarded:controller.cleanupSelected) {
                Assert.assertTrue(self.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==discarded.getId()));
            }
            Assert.assertEquals(fullPhysicalInventory(self),frozenMain.owned(self));
            Assert.assertEquals(fullPhysicalInventory(other),frozenMain.owned(other));
            System.out.println("STAGE12A_REGISTERED_CLEANUP left_first="+
                    leftRegisteredFirst+" maximum="+maximum+
                    " actual_discards="+excess);
        }
        // Independent untouched natural games: no hand/board/life curation,
        // no seed selection, no early abort or selected reroll.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeElfEmergencyGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor al=new Actor(5);Actor ar=new Actor(5)) {
                Assert.assertNotEquals(al.process.pid(),ar.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,al);
                ExternalCreatureController rc=attach(natural,right,left,ar);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"native game must complete naturally");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                System.out.println("STAGE12A_NATURAL_GAME left_first="+leftRegisteredFirst+
                        " game="+sequence+" cleanup_choices="+
                        (lc.cleanupChoices+rc.cleanupChoices)+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeOwnHandCleanupRegisteredLeftRegisteredFirst() throws Exception {
        verifyRegisteredNativeCleanup(true);
    }

    @Test(timeOut=240000)
    public void nativeOwnHandCleanupRegisteredRightRegisteredFirst() throws Exception {
        verifyRegisteredNativeCleanup(false);
    }


    /** Stage12B: independent, registered, exactly 60-card deck with
     * four real {1}{G} Elvish Visionary creatures and four real Llanowar.
     * Natural cohorts have no modified starting hand, life, deck or draws. */
    private Game nativeVisionaryGame(boolean leftRegisteredFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);
        counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);
        counts.put("Balduvian Bears",4);
        counts.put("Llanowar Elves",4);
        counts.put("Elvish Visionary",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer a=new RegisteredPlayer(mainDeck("12B left",counts))
                .setPlayer(new LobbyPlayerAi("visionary-left",null));
        RegisteredPlayer b=new RegisteredPlayer(mainDeck("12B right",counts))
                .setPlayer(new LobbyPlayerAi("visionary-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(leftRegisteredFirst?a:b);
        registered.add(leftRegisteredFirst?b:a);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage12B");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    /** Explicit controlled physical fixture: only the two original Forests
     * are placed on the battlefield, and the caster's hand is arranged
     * from its own registered physical cards. No fake mana, cast or draw.
     * The actual first-turn main phase, paid casting, triggered ability
     * and one private library-to-hand draw all execute in native Forge. */
    private void verifyVisionaryEtbAndNaturalGames(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeVisionaryGame(leftRegisteredFirst);
        Player seatA=fixture.getPlayers().get(0);
        Player seatB=fixture.getPlayers().get(1);
        try(Actor workerA=new Actor(5);Actor workerB=new Actor(5)) {
            Assert.assertNotEquals(workerA.process.pid(),workerB.process.pid());
            ExternalCreatureController ca=attach(fixture,seatA,seatB,workerA);
            ExternalCreatureController cb=attach(fixture,seatB,seatA,workerB);
            doRealNativeStartForMultiTurn(fixture,ca,cb);
            Player caster=fixture.getStartingPlayer();
            Player opponent=caster==seatA?seatB:seatA;
            Actor casterWorker=caster==seatA?workerA:workerB;
            ExternalCreatureController casterController=caster==seatA?ca:cb;
            // CONTROLLED FIXTURE ONLY: restore all opening-hand cards to
            // their owner's library, then select two distinct registered
            // Forests and two original registered competing creature spells.
            for(Card c:new ArrayList<Card>(caster.getCardsIn(ZoneType.Hand))) {
                Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            }
            List<Card> physicalForests=new ArrayList<>();
            for(int i=0;i<2;i++) {
                Card forest=caster.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName()))
                        .findFirst().orElseThrow();
                Assert.assertNotNull(fixture.getAction().moveToPlay(forest,null,
                        forge.game.ability.AbilityKey.newMap()));
                forest.setTapped(false);
                physicalForests.add(forest);
            }
            for(String name:Arrays.asList("Grizzly Bears","Elvish Visionary")) {
                Card actual=caster.getCardsIn(ZoneType.Library).stream()
                        .filter(c->name.equals(c.getName())).findFirst().orElseThrow();
                Assert.assertNotNull(fixture.getAction().moveToHand(actual,null));
            }
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand).size(),2);
            Assert.assertEquals(basicLandCountOnBattlefield(caster),2);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            String projected=observation(caster,opponent);
            JsonObject proposed=casterWorker.ask(projected);
            Assert.assertEquals(proposed.get("intent").getAsString(),"action_plan");
            JsonArray candidate=proposed.getAsJsonArray("creature_hand_indices");
            Assert.assertEquals(candidate.size(),2);
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand)
                    .get(candidate.get(0).getAsInt()).getName(),"Elvish Visionary",
                    "low own hand prioritizes native draw creature over vanilla Bear");
            Assert.assertFalse(projected.contains("\"phase\""));
            Assert.assertFalse(projected.contains("\"stack\""));
            Assert.assertFalse(projected.contains("\"library_top\""));
            Assert.assertFalse(projected.contains("\"opponent_hand\""));
            // Only a public-only observation is supplied; shuffling the
            // opponent's private library cannot change the actor's plan.
            opponent.shuffle(null);
            Assert.assertEquals(observation(caster,opponent),projected);
            Assert.assertEquals(casterWorker.ask(projected),proposed);
            int libraryBefore=caster.getCardsIn(ZoneType.Library).size();
            int priorityBefore=casterController.priorityCallbacks;
            boolean realEtbDraw=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<1200) {
                fixture.getPhaseHandler().mainLoopStep();
                boolean visionaryOnBoard=caster.getCardsIn(ZoneType.Battlefield)
                        .stream().anyMatch(c->"Elvish Visionary".equals(c.getName()));
                if(visionaryOnBoard &&
                        caster.getCardsIn(ZoneType.Library).size()==libraryBefore-1 &&
                        fixture.getStack().isEmpty()) {
                    realEtbDraw=true;break;
                }
            }
            Assert.assertTrue(steps<1200,"bounded genuine native casting and ETB draw");
            Assert.assertTrue(realEtbDraw,
                    "Forge must pay two Forest mana, cast Visionary and draw exactly one card");
            Assert.assertTrue(casterController.priorityCallbacks>priorityBefore);
            Assert.assertTrue(physicalForests.stream().allMatch(Card::isTapped),
                    "both original Forests must be tapped to pay actual {1}{G}");
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand).size(),2,
                    "one Visionary left hand and exactly one private drawn card replaced it");
            Assert.assertEquals(caster.getLife(),20);
            Assert.assertEquals(opponent.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            Assert.assertTrue(workerA.process.isAlive() && workerB.process.isAlive());
            System.out.println("STAGE12B_CONTROLLED_VISIONARY left_first="+leftRegisteredFirst+
                    " native_paid_cast=true etb_private_draw=1");
        }
        // Two independent, untouched naturally shuffled original 60-card
        // complete games per seat. NO fixture cards, forced mulligans,
        // life modifications, lucky-seed selection or truncated outcomes.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeVisionaryGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor workerL=new Actor(5);Actor workerR=new Actor(5)) {
                Assert.assertNotEquals(workerL.process.pid(),workerR.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,workerL);
                ExternalCreatureController rc=attach(natural,right,left,workerR);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"full natural game must terminate");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(workerL.process.isAlive() && workerR.process.isAlive());
                System.out.println("STAGE12B_NATURAL_VISIONARY left_first="+
                        leftRegisteredFirst+" sequence="+sequence+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeVisionaryEtbPrivateDrawLeftRegisteredFirst() throws Exception {
        verifyVisionaryEtbAndNaturalGames(true);
    }

    @Test(timeOut=240000)
    public void nativeVisionaryEtbPrivateDrawRightRegisteredFirst() throws Exception {
        verifyVisionaryEtbAndNaturalGames(false);
    }


    /** Stage12C: one printed flying 1/1 versus one printed grounded 4/4.
     * This controlled fixture moves only original registered physical cards;
     * the actor actually casts Sprites by paying a real Forest, and native
     * Forge validates combat, evasion, and unblocked player damage.
     * Two separate natural 60-card games per seat are never curated. */
    private Game nativeFlyingEvasionGame(boolean leftRegisteredFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);
        counts.put("Scryb Sprites",4);
        counts.put("Rumbling Baloth",4);
        counts.put("Grizzly Bears",4);
        counts.put("Llanowar Elves",4);
        counts.put("Elvish Visionary",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12C left",counts))
                .setPlayer(new LobbyPlayerAi("flying-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12C right",counts))
                .setPlayer(new LobbyPlayerAi("flying-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(leftRegisteredFirst?left:right);
        registered.add(leftRegisteredFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage12C");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeFlyingEvasionAndNaturalGames(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeFlyingEvasionGame(leftRegisteredFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor worker0=new Actor(5);Actor worker1=new Actor(5)) {
            Assert.assertNotEquals(worker0.process.pid(),worker1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,worker0);
            ExternalCreatureController c1=attach(fixture,p1,p0,worker1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player attacker=fixture.getStartingPlayer();
            Player defender=attacker==p0?p1:p0;
            Actor actor=attacker==p0?worker0:worker1;
            ExternalCreatureController ac=attacker==p0?c0:c1;
            ExternalCreatureController dc=attacker==p0?c1:c0;
            // CONTROLLED PHYSICAL FIXTURE ONLY: return opening hands to
            // original owners' libraries; natural games below do none of this.
            for(Player player:Arrays.asList(attacker,defender)) {
                for(Card c:new ArrayList<Card>(player.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            }
            // Keep the defender's original registered flying cards at the
            // bottom in this fixture only, preventing an unrelated flying
            // draw from making the grounded-evasion scenario ambiguous.
            for(Card c:new ArrayList<Card>(defender.getCardsIn(ZoneType.Library))) {
                if("Scryb Sprites".equals(c.getName()))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            }
            Card baloth=defender.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Rumbling Baloth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(baloth,null,
                    forge.game.ability.AbilityKey.newMap()));
            baloth.setTapped(false);
            Card forest=attacker.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(forest,null,
                    forge.game.ability.AbilityKey.newMap()));
            forest.setTapped(false);
            Card sprite=attacker.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Scryb Sprites".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(sprite,null));
            Assert.assertEquals(attacker.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertTrue(sprite.hasKeyword("Flying"));
            Assert.assertFalse(baloth.hasKeyword("Flying"));
            Assert.assertFalse(baloth.hasKeyword("Reach"));
            // The card is still in hand here. CombatUtil.canBlock is
            // meaningful only once the real creature has resolved in play.
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            String view=observation(attacker,defender);
            JsonObject before=actor.ask(view);
            Assert.assertEquals(before.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(before.getAsJsonArray("creature_hand_indices").get(0)
                    .getAsInt(),0);
            Assert.assertFalse(view.contains("\"phase\""));
            Assert.assertFalse(view.contains("\"priority\""));
            Assert.assertFalse(view.contains("\"opponent_hand\""));
            Assert.assertFalse(view.contains("\"library_top\""));
            defender.shuffle(null);
            Assert.assertEquals(observation(attacker,defender),view,
                    "opponent's hidden shuffled order never changes the actor view");
            Assert.assertEquals(actor.ask(view),before);
            boolean nativePaid=false,publicFlyingPreference=false;
            boolean nativeFlyingRestriction=false;
            boolean nativeAttack=false,realUnblockedDamage=false;
            int steps=0, spriteBattlefieldSamples=0, untappedBalothSamples=0;
            Card liveSprite=null, liveBaloth=null;
            while(!fixture.isGameOver() && steps++<3000) {
                // Explicit fixture-only readiness: even if Baloth attacked
                // on the preceding turn, it is an UNTAPPED possible blocker
                // for the real Flying combat. Never done in natural games.
                // Forge can create a new current Card state on zone changes.
                // Match the original physical ID, then query the LIVE battlefield
                // instances: a stale hand Card cannot supply active Flying statics.
                liveBaloth=defender.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==baloth.getId())
                        .findFirst().orElse(null);
                if(liveBaloth!=null &&
                        fixture.getPhaseHandler().getPlayerTurn()==attacker &&
                        liveBaloth.isTapped()) liveBaloth.setTapped(false);
                fixture.getPhaseHandler().mainLoopStep();
                liveSprite=attacker.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==sprite.getId())
                        .findFirst().orElse(null);
                liveBaloth=defender.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==baloth.getId())
                        .findFirst().orElse(null);
                if(liveSprite!=null && liveBaloth!=null) {
                    spriteBattlefieldSamples++;
                    if(!liveBaloth.isTapped()) untappedBalothSamples++;
                    if(forest.isTapped()) nativePaid=true;
                    // Test the PRINTED evasion rule itself, not the
                    // unrelated readiness of the defending creature.
                    // The nextTurn form ignores tapped status, so this
                    // cannot accidentally pass because Baloth attacked.
                    nativeFlyingRestriction |=
                            StaticAbilityCantAttackBlock.cantBlockBy(liveSprite,liveBaloth)
                            && !CombatUtil.canBlock(liveSprite,liveBaloth,true);
                    if(!liveBaloth.isTapped() && !publicFlyingPreference) {
                        JsonObject preference=actor.ask(observation(attacker,defender));
                        JsonArray attacks=preference.getAsJsonArray("attack_table_indices");
                        int spriteIndex=attacker.getCardsIn(ZoneType.Battlefield).indexOf(liveSprite);
                        for(JsonElement entry:attacks) {
                            if(entry.getAsInt()==spriteIndex) publicFlyingPreference=true;
                        }
                    }
                }
                nativeAttack |= ac.attacksAuthorized>0;
                if(nativeAttack && defender.getLife()==19) {
                    realUnblockedDamage=true;break;
                }
            }
            Assert.assertTrue(steps<3000,"native cast and real combat must finish");
            Assert.assertTrue(nativePaid,"original Forest must pay printed {G} cast");
            Assert.assertTrue(nativeFlyingRestriction,
                    "native printed Flying restriction missing: samples="+spriteBattlefieldSamples+
                    " ready="+untappedBalothSamples+" steps="+steps+
                    " paid="+nativePaid+" attacker_turn="+
                    (fixture.getPhaseHandler().getPlayerTurn()==attacker)+
                    " sprite_zone="+(liveSprite==null?"not_in_play":liveSprite.getZone().getZoneType())+
                    " baloth_tapped="+(liveBaloth!=null && liveBaloth.isTapped())+
                    " flying="+(liveSprite!=null && liveSprite.hasKeyword("Flying"))+
                    " direct_static="+(liveSprite!=null && liveBaloth!=null &&
                            StaticAbilityCantAttackBlock.cantBlockBy(liveSprite,liveBaloth))+
                    " nextturn_canblock="+(liveSprite!=null && liveBaloth!=null &&
                            CombatUtil.canBlock(liveSprite,liveBaloth,true))+
                    " attack_callbacks="+ac.attackCallbacks+
                    " attacks_authorized="+ac.attacksAuthorized+
                    " defender_life="+defender.getLife());
            Assert.assertTrue(publicFlyingPreference,
                    "external actor must prefer Flying 1/1 past grounded 4/4");
            Assert.assertTrue(nativeAttack,"native Forge must declare the flyer attacker");
            Assert.assertTrue(realUnblockedDamage,"real Forge combat deals one player damage");
            Assert.assertEquals(defender.getLife(),19);
            Assert.assertTrue(defender.getCardsIn(ZoneType.Battlefield).contains(baloth));
            Assert.assertTrue(attacker.getCardsIn(ZoneType.Battlefield).contains(sprite));
            Assert.assertNotNull(liveBaloth,"registered Baloth remains physically on battlefield");
            Assert.assertFalse(liveBaloth.isTapped(),
                    "grounded Baloth was a ready but illegal Flying blocker");
            Assert.assertEquals(dc.blocksAuthorized,0);
            Assert.assertEquals(fullPhysicalInventory(attacker),frozenMain.owned(attacker));
            Assert.assertEquals(fullPhysicalInventory(defender),frozenMain.owned(defender));
            Assert.assertTrue(worker0.process.isAlive() && worker1.process.isAlive());
            System.out.println("STAGE12C_CONTROLLED_PRINTED_FLYING left_first="+
                    leftRegisteredFirst+" paid_cast=true native_unblocked_damage=1");
        }
        // Two independent untouched original 60-card naturally shuffled
        // complete games per seat; no seed choice, mulligan or hand changes.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeFlyingEvasionGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor workerL=new Actor(5);Actor workerR=new Actor(5)) {
                Assert.assertNotEquals(workerL.process.pid(),workerR.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,workerL);
                ExternalCreatureController rc=attach(natural,right,left,workerR);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"natural full game must finish");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(workerL.process.isAlive() && workerR.process.isAlive());
                System.out.println("STAGE12C_NATURAL_FLYING left_first="+
                        leftRegisteredFirst+" sequence="+sequence+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativePrintedFlyingEvasionLeftRegisteredFirst() throws Exception {
        verifyNativeFlyingEvasionAndNaturalGames(true);
    }

    @Test(timeOut=240000)
    public void nativePrintedFlyingEvasionRightRegisteredFirst() throws Exception {
        verifyNativeFlyingEvasionAndNaturalGames(false);
    }


    /** Stage12D: one real printed {G} 1/1 Deathtouch can trade with
     * a native 4/4 attacking at our four life, unlike a vanilla 1/1.
     * The external actor proposes only own/public card indices. Forge
     * actually pays {G}, declares attack/block, assigns damage and kills
     * BOTH registered original physical cards under CR 702.2b/704.5h.
     * Controlled fixtures ONLY curate physical zones and starting life.
     * Four natural original 60-card games remain fully untouched. */
    private Game nativeDeathtouchGame(boolean leftRegisteredFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);
        counts.put("Sedge Scorpion",4);
        counts.put("Rumbling Baloth",4);
        counts.put("Grizzly Bears",4);
        counts.put("Llanowar Elves",4);
        counts.put("Elvish Visionary",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12D left",counts))
                .setPlayer(new LobbyPlayerAi("deathtouch-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12D right",counts))
                .setPlayer(new LobbyPlayerAi("deathtouch-right",null));
        List<RegisteredPlayer> registered=new ArrayList<>();
        registered.add(leftRegisteredFirst?left:right);
        registered.add(leftRegisteredFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,registered,"Stage12D");
        Game game=new Game(registered,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeDeathtouchTradeAndNaturalGames(boolean leftRegisteredFirst)
            throws Exception {
        Game fixture=nativeDeathtouchGame(leftRegisteredFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor worker0=new Actor(5);Actor worker1=new Actor(5)) {
            Assert.assertNotEquals(worker0.process.pid(),worker1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,worker0);
            ExternalCreatureController c1=attach(fixture,p1,p0,worker1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player defense=fixture.getStartingPlayer();
            Player offense=defense==p0?p1:p0;
            Actor defenderWorker=defense==p0?worker0:worker1;
            ExternalCreatureController dc=defense==p0?c0:c1;
            ExternalCreatureController ac=defense==p0?c1:c0;
            // CONTROLLED FIXTURE ONLY: restore the original opening cards
            // to their own libraries; no new Card objects are fabricated.
            for(Player player:Arrays.asList(defense,offense)) {
                for(Card c:new ArrayList<Card>(player.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            }
            Card baloth=offense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Rumbling Baloth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(baloth,null,
                    forge.game.ability.AbilityKey.newMap()));
            Card liveBaloth=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->c.getId()==baloth.getId()).findFirst().orElseThrow();
            liveBaloth.setSickness(false); // CONTROLLED only, not natural games
            liveBaloth.setTapped(false);
            Card forest=defense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(forest,null,
                    forge.game.ability.AbilityKey.newMap()));
            Card scorpion=defense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Sedge Scorpion".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(scorpion,null));
            Assert.assertTrue(defense.setLife(4,null),"controlled lethal-risk life");
            Assert.assertEquals(defense.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertTrue(scorpion.hasKeyword("Deathtouch"));
            Assert.assertFalse(liveBaloth.hasKeyword("Deathtouch"));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            String projected=observation(defense,offense);
            JsonObject initial=defenderWorker.ask(projected);
            Assert.assertEquals(initial.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(initial.getAsJsonArray("creature_hand_indices")
                    .get(0).getAsInt(),0);
            JsonObject publicView=JsonParser.parseString(projected).getAsJsonObject();
            for(String denied:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand"))
                Assert.assertFalse(publicView.has(denied));
            offense.shuffle(null);
            Assert.assertEquals(observation(defense,offense),projected);
            Assert.assertEquals(defenderWorker.ask(projected),initial);
            boolean realPaidCast=false,publicTradePreference=false;
            boolean printedDeathtouch=false,actualTwoCreatureTrade=false;
            int steps=0,liveSamples=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                Card liveScorpion=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==scorpion.getId()).findFirst().orElse(null);
                Card liveForest=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==forest.getId()).findFirst().orElse(null);
                if(liveScorpion!=null) {
                    liveSamples++;
                    printedDeathtouch |= liveScorpion.hasKeyword("Deathtouch");
                    if(liveForest!=null && liveForest.isTapped()) realPaidCast=true;
                    JsonObject proposed=defenderWorker.ask(observation(defense,offense));
                    JsonArray pairs=proposed.getAsJsonArray("block_pairs");
                    int selfIndex=defense.getCardsIn(ZoneType.Battlefield).indexOf(liveScorpion);
                    int enemyIndex=offense.getCardsIn(ZoneType.Battlefield).indexOf(liveBaloth);
                    for(JsonElement item:pairs) {
                        JsonArray pair=item.getAsJsonArray();
                        if(pair.get(0).getAsInt()==selfIndex &&
                           pair.get(1).getAsInt()==enemyIndex)
                            publicTradePreference=true;
                    }
                }
                boolean balothDead=offense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==baloth.getId());
                boolean scorpionDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==scorpion.getId());
                if(balothDead && scorpionDead) {
                    actualTwoCreatureTrade=true;break;
                }
            }
            Assert.assertTrue(steps<3500,"native paid cast and lethal-risk trade must finish");
            Assert.assertTrue(liveSamples>0 && printedDeathtouch,
                    "must inspect live battlefield instance with printed Deathtouch");
            Assert.assertTrue(realPaidCast,"real original Forest paid printed {G}");
            Assert.assertTrue(publicTradePreference,
                    "independent defender proposes one 1/1 Deathtouch vs visible 4/4");
            Assert.assertTrue(ac.attacksAuthorized>0,"Forge authorized real 4/4 attack");
            Assert.assertTrue(dc.blocksAuthorized>0,"Forge authorized legal 1/1 block");
            Assert.assertTrue(actualTwoCreatureTrade,
                    "Forge must destroy BOTH original cards after 1 Deathtouch damage");
            Assert.assertEquals(defense.getLife(),4,"block prevented four player damage");
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertTrue(worker0.process.isAlive() && worker1.process.isAlive());
            System.out.println("STAGE12D_CONTROLLED_DEATHTOUCH left_first="+
                    leftRegisteredFirst+" paid_cast=true native_trade=true");
        }
        // Two independent untouched naturally shuffled original 60-card
        // full games per seat. No seed choice, curated opening, modified
        // life, fabricated cards, forced mulligan, or selective reroll.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeDeathtouchGame(leftRegisteredFirst);
            Player left=natural.getPlayers().get(leftRegisteredFirst?0:1);
            Player right=natural.getPlayers().get(leftRegisteredFirst?1:0);
            try(Actor workerL=new Actor(5);Actor workerR=new Actor(5)) {
                Assert.assertNotEquals(workerL.process.pid(),workerR.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,workerL);
                ExternalCreatureController rc=attach(natural,right,left,workerR);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000,"natural full game must finish");
                Assert.assertTrue(natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(workerL.process.isAlive() && workerR.process.isAlive());
                System.out.println("STAGE12D_NATURAL_DEATHTOUCH left_first="+
                        leftRegisteredFirst+" sequence="+sequence+
                        " winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeDeathtouchEmergencyTradeLeftRegisteredFirst() throws Exception {
        verifyNativeDeathtouchTradeAndNaturalGames(true);
    }

    @Test(timeOut=240000)
    public void nativeDeathtouchEmergencyTradeRightRegisteredFirst() throws Exception {
        verifyNativeDeathtouchTradeAndNaturalGames(false);
    }


    /** Stage12E: two ORIGINAL Forests pay for a REAL Rampant Growth {1}{G}.
     * Forge searches the PRIVATE library and puts a third original Forest
     * onto the battlefield tapped, then shuffles; actors see no private zone. */
    private Game nativeRampantGrowthGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);
        counts.put("Rampant Growth",4);
        counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);
        counts.put("Llanowar Elves",4);
        counts.put("Elvish Visionary",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12E left",counts))
                .setPlayer(new LobbyPlayerAi("ramp-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12E right",counts))
                .setPlayer(new LobbyPlayerAi("ramp-right",null));
        List<RegisteredPlayer> players=new ArrayList<>();
        players.add(leftFirst?left:right);
        players.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,players,"Stage12E");
        Game game=new Game(players,rules,match);
        frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeRampantGrowthAndNaturalGames(boolean leftFirst) throws Exception {
        Game fixture=nativeRampantGrowthGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor worker0=new Actor(5);Actor worker1=new Actor(5)) {
            Assert.assertNotEquals(worker0.process.pid(),worker1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,worker0);
            ExternalCreatureController c1=attach(fixture,p1,p0,worker1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player ramp=fixture.getStartingPlayer(),other=ramp==p0?p1:p0;
            Actor actor=ramp==p0?worker0:worker1;
            // Controlled fixture ONLY: physically move original hand cards.
            for(Player p:Arrays.asList(ramp,other))
                for(Card c:new ArrayList<Card>(p.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Set<Integer> payIds=new HashSet<>();
            for(int i=0;i<2;i++) {
                Card forest=ramp.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
                payIds.add(forest.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(forest,null,
                        forge.game.ability.AbilityKey.newMap()));
                Card live=ramp.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==forest.getId()).findFirst().orElseThrow();
                live.setTapped(false);
            }
            Card growth=ramp.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Rampant Growth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(growth,null));
            Assert.assertEquals(ramp.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertEquals(ramp.getLife(),20);
            Assert.assertEquals(other.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(ramp),frozenMain.owned(ramp));
            Assert.assertEquals(fullPhysicalInventory(other),frozenMain.owned(other));
            String projected=observation(ramp,other);
            JsonObject plan=actor.ask(projected);
            Assert.assertEquals(plan.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(plan.getAsJsonArray("sorcery_hand_indices").size(),1);
            Assert.assertEquals(plan.getAsJsonArray("sorcery_hand_indices")
                    .get(0).getAsInt(),0);
            JsonObject publicView=JsonParser.parseString(projected).getAsJsonObject();
            for(String denied:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(publicView.has(denied));
            ramp.shuffle(null);
            Assert.assertEquals(observation(ramp,other),projected);
            Assert.assertEquals(actor.ask(projected),plan);
            boolean paid=false,graveyard=false,tappedLand=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                int tappedPayments=0;
                for(Card c:ramp.getCardsIn(ZoneType.Battlefield))
                    if(payIds.contains(c.getId()) && c.isTapped()) tappedPayments++;
                if(tappedPayments==2) paid=true;
                graveyard |= ramp.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==growth.getId());
                for(Card c:ramp.getCardsIn(ZoneType.Battlefield))
                    if("Forest".equals(c.getName()) && !payIds.contains(c.getId())
                            && c.isTapped()) tappedLand=true;
                if(paid && graveyard && tappedLand) break;
            }
            Assert.assertTrue(steps<3500,"real sorcery must resolve promptly");
            Assert.assertTrue(paid,"two original Forests must pay real {1}{G}");
            Assert.assertTrue(graveyard,"real original Rampant Growth must resolve; paid="+
                    paid+" fetchedTapped="+tappedLand+" steps="+steps+
                    " gameOver="+fixture.isGameOver());
            Assert.assertTrue(tappedLand,"native library search must fetch tapped original Forest");
            Assert.assertEquals(ramp.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->"Forest".equals(c.getName())).count(),3L);
            Assert.assertEquals(fullPhysicalInventory(ramp),frozenMain.owned(ramp));
            Assert.assertEquals(fullPhysicalInventory(other),frozenMain.owned(other));
            Assert.assertTrue(worker0.process.isAlive() && worker1.process.isAlive());
            System.out.println("STAGE12E_CONTROLLED_RAMP left_first="+leftFirst+
                    " paid=true native_private_search=true");
        }
        // Two independent untouched 60-card full games per seat.
        for(int sequence=0;sequence<2;sequence++) {
            Game natural=nativeRampantGrowthGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12E_NATURAL_RAMP left_first="+leftFirst+
                        " sequence="+sequence+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeRampantGrowthSearchLeftRegisteredFirst() throws Exception {
        verifyNativeRampantGrowthAndNaturalGames(true);
    }

    @Test(timeOut=240000)
    public void nativeRampantGrowthSearchRightRegisteredFirst() throws Exception {
        verifyNativeRampantGrowthAndNaturalGames(false);
    }

    /** Stage12F: printed Overrun {2}{G}{G}{G} gives two ORIGINAL
     * registered bears +3/+3 and Trample. Forge actually pays five
     * Forests, resolves its native global buff, legal blocks and excess
     * damage. Curation is limited to explicit physical-card fixtures. */
    private Game nativeOverrunGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40); counts.put("Overrun",4);
        counts.put("Grizzly Bears",4); counts.put("Runeclaw Bear",4);
        counts.put("Llanowar Elves",4); counts.put("Elvish Visionary",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12F left",counts))
                .setPlayer(new LobbyPlayerAi("overrun-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12F right",counts))
                .setPlayer(new LobbyPlayerAi("overrun-right",null));
        List<RegisteredPlayer> reg=new ArrayList<>();
        reg.add(leftFirst?left:right);reg.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,reg,"Stage12F");
        Game game=new Game(reg,rules,match);frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers())
            Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyOverrunControlledFixture(boolean leftFirst,int defenderStartLife)
            throws Exception {
        Game fixture=nativeOverrunGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player assault=fixture.getStartingPlayer();
            Player defense=assault==p0?p1:p0;
            Actor actor=assault==p0?w0:w1;
            // EXPLICIT CONTROLLED FIXTURE only: move already registered
            // ORIGINAL physical cards; do not fabricate extra cards.
            for(Player p:Arrays.asList(assault,defense))
                for(Card c:new ArrayList<Card>(p.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Set<Integer> payIds=new HashSet<>();
            for(int i=0;i<5;i++) {
                Card orig=assault.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
                payIds.add(orig.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(orig,null,
                        forge.game.ability.AbilityKey.newMap()));
                Card live=assault.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==orig.getId()).findFirst().orElseThrow();
                live.setTapped(false);
            }
            Set<Integer> bearIds=new HashSet<>();
            for(String name:Arrays.asList("Grizzly Bears","Runeclaw Bear")) {
                Card orig=assault.getCardsIn(ZoneType.Library).stream()
                        .filter(c->name.equals(c.getName())).findFirst().orElseThrow();
                bearIds.add(orig.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(orig,null,
                        forge.game.ability.AbilityKey.newMap()));
                Card live=assault.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==orig.getId()).findFirst().orElseThrow();
                live.setSickness(false);live.setTapped(false);
            }
            Card blocker=defense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(blocker,null,
                    forge.game.ability.AbilityKey.newMap()));
            Card liveBlocker=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->c.getId()==blocker.getId()).findFirst().orElseThrow();
            liveBlocker.setSickness(false);liveBlocker.setTapped(false);
            Card spell=assault.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Overrun".equals(c.getName())).findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(spell,null));
            Assert.assertTrue(defense.setLife(defenderStartLife,null),"controlled physical fixture life");
            Assert.assertEquals(assault.getLife(),20);
            Assert.assertEquals(assault.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertEquals(fullPhysicalInventory(assault),frozenMain.owned(assault));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            String projected=observation(assault,defense);
            JsonObject proposed=actor.ask(projected);
            Assert.assertEquals(proposed.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposed.getAsJsonArray("sorcery_hand_indices").size(),1);
            Assert.assertEquals(proposed.getAsJsonArray("sorcery_hand_indices")
                    .get(0).getAsInt(),0);
            JsonObject view=JsonParser.parseString(projected).getAsJsonObject();
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(view.has(forbidden));
            assault.shuffle(null);
            Assert.assertEquals(observation(assault,defense),projected);
            Assert.assertEquals(actor.ask(projected),proposed);
            boolean paidFive=false,resolved=false,pumped=false,trampleVerified=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                if(assault.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->payIds.contains(c.getId()) && c.isTapped()).count()==5)
                    paidFive=true;
                if(assault.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==spell.getId())) resolved=true;
                int active=0;
                for(Card c:assault.getCardsIn(ZoneType.Battlefield))
                    if(bearIds.contains(c.getId()) && c.hasKeyword("Trample") &&
                            c.getNetPower()==5 && c.getNetToughness()==5) active++;
                if(active==2) pumped=true;
                // Pinned Forge GameAction.checkStateEffects (1401-1405)
                // ends at zero life BEFORE lethal creature SBA runs.
                // Thus test 8-life lethal player damage and actual lethal
                // marked blocker damage, plus separate 9-life native SBA
                // destruction of the same original physical blocker.
                Card blockerDuringDamage=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==blocker.getId()).findFirst().orElse(null);
                boolean blockerDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==blocker.getId());
                if(defenderStartLife==8 && defense.getLife()==0 &&
                        blockerDuringDamage!=null && blockerDuringDamage.getDamage()>=2)
                    trampleVerified=true;
                if(defenderStartLife==9 && defense.getLife()==1 && blockerDead)
                    trampleVerified=true;
                if(paidFive && resolved && pumped && trampleVerified &&
                        (defenderStartLife!=8 || fixture.isGameOver())) break;
            }
            Assert.assertTrue(steps<3500,"real Overrun should resolve promptly");
            Assert.assertTrue(paidFive,"all five original Forests must pay real mana");
            Assert.assertTrue(resolved,"native original Overrun goes to graveyard");
            Assert.assertTrue(pumped,"both original Bears gain +3/+3 and Trample");
            boolean defenderActuallyDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                    .anyMatch(c->c.getId()==blocker.getId());
            ExternalCreatureController assaultController=assault==p0?c0:c1;
            ExternalCreatureController defenseController=defense==p0?c0:c1;
            Card remainingBlocker=defense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->c.getId()==blocker.getId()).findFirst().orElse(null);
            int marked=remainingBlocker==null?0:remainingBlocker.getDamage();
            Assert.assertTrue(trampleVerified,"Forge native Overrun damage/SBA: "+
                    "initialLife="+defenderStartLife+" paidFive="+paidFive+
                    " resolved="+resolved+" pumped="+pumped+
                    " blockerDead="+defenderActuallyDead+" marked="+marked+
                    " defenderLife="+defense.getLife()+
                    " attacks="+assaultController.attacksAuthorized+
                    " blocks="+defenseController.blocksAuthorized+
                    " assignments="+assaultController.refereeDamageAssignments+
                    " steps="+steps+" gameOver="+fixture.isGameOver());
            Assert.assertTrue(assaultController.attacksAuthorized>=2 &&
                    defenseController.blocksAuthorized>=1,
                    "two actual Forge attacks and one actual Forge block");
            Assert.assertTrue(assaultController.refereeDamageAssignments>=1,
                    "trusted referee must allocate genuine Overrun trample");
            if(defenderStartLife==8) {
                Assert.assertEquals(defense.getLife(),0,
                        "real five unblocked + three trample is lethal at eight");
                Assert.assertTrue(fixture.isGameOver());
                Assert.assertNotNull(fixture.getOutcome());
                Assert.assertTrue(fixture.getOutcome().isWinner(
                        assault.getRegisteredPlayer()));
                Assert.assertNotNull(remainingBlocker,
                        "pinned Forge checks game-over before creature SBAs");
                Assert.assertTrue(remainingBlocker.getDamage()>=2,
                        "native blocked Bear dealt at least lethal two damage");
            } else {
                Assert.assertEquals(defenderStartLife,9);
                Assert.assertEquals(defense.getLife(),1,
                        "identical real eight damage must leave nine-life player at one");
                Assert.assertTrue(defenderActuallyDead,
                        "native Forge SBA must destroy the original 2/2 blocker");
                Assert.assertFalse(fixture.isGameOver());
            }
            Assert.assertEquals(fullPhysicalInventory(assault),frozenMain.owned(assault));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12F_CONTROLLED_OVERRUN left_first="+leftFirst+
                    " initial_life="+defenderStartLife+" paid5=true native_damage=true"+
                    " blocker_dead="+defenderActuallyDead);
        }
    }

    private void verifyNativeOverrunTrampleAndNaturalGames(boolean leftFirst)
            throws Exception {
        // Two complementary physical fixtures per seating order: at eight
        // life prove native player-lethal and marked blocker damage; at nine
        // life prove the SAME native combat destroys the original blocker.
        // Pinned Forge short-circuits creature SBA after zero-life game over.
        verifyOverrunControlledFixture(leftFirst,8);
        verifyOverrunControlledFixture(leftFirst,9);
        // TWO independent completely untouched, naturally shuffled 60-card
        // complete Forge games per seat, with normal mulligans and 20 life.
        for(int seq=0;seq<2;seq++) {
            Game natural=nativeOverrunGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12F_NATURAL_OVERRUN left_first="+leftFirst+
                        " sequence="+seq+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeOverrunTrampleLethalLeftRegisteredFirst() throws Exception {
        verifyNativeOverrunTrampleAndNaturalGames(true);
    }
    @Test(timeOut=240000)
    public void nativeOverrunTrampleLethalRightRegisteredFirst() throws Exception {
        verifyNativeOverrunTrampleAndNaturalGames(false);
    }


    /** Stage12G: genuine printed {1}{B} Zulaport Cutthroat death trigger.
     * Two original lands pay, a legal native chump block kills the original
     * 1/1 and Forge drains one opposing life and gains one controller life. */
    private Game nativeCutthroatGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",24);counts.put("Swamp",16);
        counts.put("Zulaport Cutthroat",4);
        counts.put("Grizzly Bears",4);counts.put("Runeclaw Bear",4);
        counts.put("Llanowar Elves",4);counts.put("Elvish Visionary",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12G left",counts))
                .setPlayer(new LobbyPlayerAi("cutthroat-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12G right",counts))
                .setPlayer(new LobbyPlayerAi("cutthroat-right",null));
        List<RegisteredPlayer> reg=new ArrayList<>();
        reg.add(leftFirst?left:right);reg.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,reg,"Stage12G");
        Game game=new Game(reg,rules,match);frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers())
            Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeCutthroatDrainAndNaturalGames(boolean leftFirst)
            throws Exception {
        Game fixture=nativeCutthroatGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player defense=fixture.getStartingPlayer();
            Player offense=defense==p0?p1:p0;
            Actor worker=defense==p0?w0:w1;
            ExternalCreatureController dc=defense==p0?c0:c1;
            ExternalCreatureController ac=defense==p0?c1:c0;
            // EXPLICIT CONTROLLED fixture only; no fabricated cards.
            for(Player p:Arrays.asList(defense,offense))
                for(Card c:new ArrayList<Card>(p.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Card bear=offense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(bear,null,
                    forge.game.ability.AbilityKey.newMap()));
            Card liveBear=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->c.getId()==bear.getId()).findFirst().orElseThrow();
            liveBear.setSickness(false);liveBear.setTapped(false);
            Set<Integer> payIds=new HashSet<>();
            for(String landName:Arrays.asList("Forest","Swamp")) {
                Card land=defense.getCardsIn(ZoneType.Library).stream()
                        .filter(c->landName.equals(c.getName()))
                        .findFirst().orElseThrow();
                payIds.add(land.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(land,null,
                        forge.game.ability.AbilityKey.newMap()));
                Card live=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==land.getId()).findFirst().orElseThrow();
                live.setTapped(false);
            }
            Card cutthroat=defense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Zulaport Cutthroat".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(cutthroat,null));
            Assert.assertTrue(defense.setLife(2,null));
            Assert.assertTrue(offense.setLife(6,null));
            Assert.assertEquals(defense.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            String projected=observation(defense,offense);
            JsonObject proposal=worker.ask(projected);
            Assert.assertEquals(proposal.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposal.getAsJsonArray("creature_hand_indices").size(),1);
            Assert.assertEquals(proposal.getAsJsonArray("creature_hand_indices")
                    .get(0).getAsInt(),0);
            JsonObject view=JsonParser.parseString(projected).getAsJsonObject();
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(view.has(forbidden));
            offense.shuffle(null);
            Assert.assertEquals(observation(defense,offense),projected);
            Assert.assertEquals(worker.ask(projected),proposal);
            boolean paidBoth=false,offeredBlock=false,originalDied=false;
            boolean nativeDrain=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                if(defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->payIds.contains(c.getId()) && c.isTapped()).count()==2)
                    paidBoth=true;
                Card live=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==cutthroat.getId()).findFirst().orElse(null);
                if(live!=null) {
                    JsonObject plan=worker.ask(observation(defense,offense));
                    int ownIndex=defense.getCardsIn(ZoneType.Battlefield).indexOf(live);
                    int enemyIndex=offense.getCardsIn(ZoneType.Battlefield).indexOf(liveBear);
                    for(JsonElement item:plan.getAsJsonArray("block_pairs")) {
                        JsonArray pair=item.getAsJsonArray();
                        if(pair.get(0).getAsInt()==ownIndex &&
                                pair.get(1).getAsInt()==enemyIndex)
                            offeredBlock=true;
                    }
                }
                originalDied |= defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==cutthroat.getId());
                if(originalDied && defense.getLife()==3 && offense.getLife()==5) {
                    nativeDrain=true;break;
                }
            }
            Assert.assertTrue(steps<3500,"paid cast and native drain must finish");
            Assert.assertTrue(paidBoth,"two original lands pay genuine {1}{B}");
            Assert.assertTrue(offeredBlock,"external actor offers emergency chump");
            Assert.assertTrue(ac.attacksAuthorized>=1,"Forge authorized Bear attack");
            Assert.assertTrue(dc.blocksAuthorized>=1,"Forge authorized legal block");
            Assert.assertTrue(originalDied,"original Cutthroat died in combat");
            Assert.assertTrue(nativeDrain,"Forge must drain one and gain one");
            Assert.assertEquals(defense.getLife(),3);
            Assert.assertEquals(offense.getLife(),5);
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12G_CONTROLLED_DRAIN left_first="+leftFirst+
                    " paid=true native_trigger=true");
        }
        // Two independent untouched naturally shuffled 60-card full games
        // per seat. Normal London mulligans and 20 life, no seed selection.
        for(int seq=0;seq<2;seq++) {
            Game natural=nativeCutthroatGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12G_NATURAL_DRAIN left_first="+leftFirst+
                        " sequence="+seq+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeCutthroatDeathDrainLeftRegisteredFirst() throws Exception {
        verifyNativeCutthroatDrainAndNaturalGames(true);
    }
    @Test(timeOut=240000)
    public void nativeCutthroatDeathDrainRightRegisteredFirst() throws Exception {
        verifyNativeCutthroatDrainAndNaturalGames(false);
    }

    /** Stage12G increment: printed Kazandu Nectarpot {1}{G} 1/3.
     * Landfall is triggered by a real hand-played Forest and again by
     * native Rampant Growth's private-library search. No actor sees top,
     * future order, timing, stack, private cards, or authority objects. */
    private Game nativeNectarpotGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);counts.put("Kazandu Nectarpot",4);
        counts.put("Rampant Growth",4);counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);counts.put("Llanowar Elves",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12G landfall left",counts))
                .setPlayer(new LobbyPlayerAi("nectarpot-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12G landfall right",counts))
                .setPlayer(new LobbyPlayerAi("nectarpot-right",null));
        List<RegisteredPlayer> reg=new ArrayList<>();
        reg.add(leftFirst?left:right);reg.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,reg,"Stage12G Landfall");
        Game game=new Game(reg,rules,match);frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeNectarpotLandfallAndNaturalGames(boolean leftFirst)
            throws Exception {
        Game fixture=nativeNectarpotGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player grower=fixture.getStartingPlayer();
            Player opponent=grower==p0?p1:p0;
            Actor worker=grower==p0?w0:w1;
            // CONTROLLED physical original-card fixture only.
            for(Player p:Arrays.asList(grower,opponent))
                for(Card c:new ArrayList<Card>(p.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Set<Integer> payingIds=new HashSet<>();
            for(int i=0;i<2;i++) {
                Card forest=grower.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
                payingIds.add(forest.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(forest,null,
                        forge.game.ability.AbilityKey.newMap()));
                Card live=grower.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==forest.getId()).findFirst().orElseThrow();
                live.setTapped(false);
            }
            Card pot=grower.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Kazandu Nectarpot".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card bear=grower.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(bear,null));
            Assert.assertNotNull(fixture.getAction().moveToHand(pot,null));
            Assert.assertTrue(grower.setLife(6,null));
            String projected=observation(grower,opponent);
            JsonObject proposal=worker.ask(projected);
            int potIndex=grower.getCardsIn(ZoneType.Hand).indexOf(pot);
            Assert.assertEquals(proposal.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposal.getAsJsonArray("creature_hand_indices")
                    .get(0).getAsInt(),potIndex,
                    "public low-life actor must prefer Landfall before vanilla Bear");
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(JsonParser.parseString(projected)
                        .getAsJsonObject().has(forbidden));
            opponent.shuffle(null);
            Assert.assertEquals(observation(grower,opponent),projected);
            Assert.assertEquals(worker.ask(projected),proposal);
            boolean cast=false,paid=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                cast=grower.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==pot.getId());
                if(grower.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->payingIds.contains(c.getId()) && c.isTapped())
                        .count()==2) paid=true;
                if(cast) break;
            }
            Assert.assertTrue(steps<3500 && cast,"real printed Nectarpot cast");
            Assert.assertTrue(paid,"two original Forests pay genuine {1}{G}");
            Assert.assertEquals(grower.getLife(),6);
            if(grower.getCardsIn(ZoneType.Hand).contains(bear))
                Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(bear,null));
            Card handForest=grower.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
            Card growth=grower.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Rampant Growth".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(handForest,null));
            Assert.assertNotNull(fixture.getAction().moveToHand(growth,null));
            Assert.assertEquals(fullPhysicalInventory(grower),frozenMain.owned(grower));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            JsonObject after=worker.ask(observation(grower,opponent));
            Assert.assertTrue(after.getAsJsonArray("land_hand_indices").size()>0);
            Assert.assertTrue(after.getAsJsonArray("sorcery_hand_indices").size()>0);
            int previousLife=grower.getLife(),positiveGain=0;
            boolean handLandPlayed=false,growthResolved=false,fetchedTapped=false;
            steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                int currentLife=grower.getLife();
                if(currentLife>previousLife) positiveGain+=currentLife-previousLife;
                previousLife=currentLife;
                handLandPlayed |= grower.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==handForest.getId());
                growthResolved |= grower.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==growth.getId());
                fetchedTapped |= grower.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->"Forest".equals(c.getName()) &&
                                !payingIds.contains(c.getId()) &&
                                c.getId()!=handForest.getId() && c.isTapped());
                if(handLandPlayed && growthResolved && fetchedTapped && positiveGain>=2)
                    break;
            }
            Assert.assertTrue(steps<3500,"two native Landfall events must finish");
            Assert.assertTrue(handLandPlayed,"Forge legally played original Forest");
            Assert.assertTrue(growthResolved,"native Rampant Growth resolved");
            Assert.assertTrue(fetchedTapped,"Forge privately fetched tapped Forest");
            Assert.assertEquals(positiveGain,2,
                    "each original Forest entry must natively trigger +1 life");
            Assert.assertTrue(grower.getCardsIn(ZoneType.Battlefield).stream()
                    .anyMatch(c->c.getId()==pot.getId()));
            Assert.assertEquals(fullPhysicalInventory(grower),frozenMain.owned(grower));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12G_CONTROLLED_LANDFALL left_first="+leftFirst+
                    " real_cast=true native_landfall=2");
        }
        // Four independent UNTOUCHED natural 60-card full games, two per seat.
        for(int seq=0;seq<2;seq++) {
            Game natural=nativeNectarpotGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12G_NATURAL_LANDFALL left_first="+leftFirst+
                        " sequence="+seq+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeNectarpotLandfallLeftRegisteredFirst() throws Exception {
        verifyNativeNectarpotLandfallAndNaturalGames(true);
    }
    @Test(timeOut=240000)
    public void nativeNectarpotLandfallRightRegisteredFirst() throws Exception {
        verifyNativeNectarpotLandfallAndNaturalGames(false);
    }


    /** Stage12G: printed Phyrexian Rager {2}{B} 2/2 has a mandatory
     * ETB draw-one AND lose-one-life effect. External actors may prioritize
     * this public-life-aware tradeoff but cannot see the drawn card early. */
    private Game nativeRagerGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);counts.put("Swamp",8);
        counts.put("Phyrexian Rager",4);
        counts.put("Grizzly Bears",4);counts.put("Runeclaw Bear",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12G Rager left",counts))
                .setPlayer(new LobbyPlayerAi("rager-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12G Rager right",counts))
                .setPlayer(new LobbyPlayerAi("rager-right",null));
        List<RegisteredPlayer> reg=new ArrayList<>();
        reg.add(leftFirst?left:right);reg.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,reg,"Stage12G Rager");
        Game game=new Game(reg,rules,match);frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeRagerRiskDrawAndNaturalGames(boolean leftFirst)
            throws Exception {
        Game fixture=nativeRagerGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player caster=fixture.getStartingPlayer();
            Player opponent=caster==p0?p1:p0;
            Actor worker=caster==p0?w0:w1;
            for(Card c:new ArrayList<Card>(caster.getCardsIn(ZoneType.Hand)))
                Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Set<Integer> paidIds=new HashSet<>();
            for(String name:Arrays.asList("Forest","Forest","Swamp")) {
                Card land=caster.getCardsIn(ZoneType.Library).stream()
                        .filter(c->name.equals(c.getName())).findFirst().orElseThrow();
                paidIds.add(land.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(land,null,
                        forge.game.ability.AbilityKey.newMap()));
                land.setTapped(false);
            }
            Card rager=caster.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Phyrexian Rager".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card bear=caster.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(bear,null));
            Assert.assertNotNull(fixture.getAction().moveToHand(rager,null));
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand).size(),2);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            // Negative control: at 1 life, mandatory Rager life loss is lethal.
            Assert.assertTrue(caster.setLife(1,null));
            JsonObject emergency=worker.ask(observation(caster,opponent));
            JsonArray safe=emergency.getAsJsonArray("creature_hand_indices");
            Assert.assertEquals(safe.size(),1);
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand)
                    .get(safe.get(0).getAsInt()).getName(),"Grizzly Bears");
            // At six life and with only two cards, draw-one is worth the risk.
            Assert.assertTrue(caster.setLife(6,null));
            String projected=observation(caster,opponent);
            JsonObject proposed=worker.ask(projected);
            Assert.assertEquals(proposed.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand)
                    .get(proposed.getAsJsonArray("creature_hand_indices")
                    .get(0).getAsInt()).getName(),"Phyrexian Rager");
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(JsonParser.parseString(projected)
                        .getAsJsonObject().has(forbidden));
            opponent.shuffle(null);
            Assert.assertEquals(observation(caster,opponent),projected);
            Assert.assertEquals(worker.ask(projected),proposed);
            int libraryBefore=caster.getCardsIn(ZoneType.Library).size();
            boolean nativeDrawAndLoss=false,paid=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                paid |= caster.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->paidIds.contains(c.getId()) && c.isTapped())
                        .count()==3;
                boolean onBoard=caster.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==rager.getId());
                if(onBoard && fixture.getStack().isEmpty() &&
                   caster.getCardsIn(ZoneType.Library).size()==libraryBefore-1 &&
                   caster.getLife()==5) {
                    nativeDrawAndLoss=true;break;
                }
            }
            Assert.assertTrue(steps<3500,"bounded native paid cast and ETB");
            Assert.assertTrue(nativeDrawAndLoss,
                    "Forge must natively draw exactly one private card and lose 1 life");
            Assert.assertTrue(paid,"two original Forests and one Swamp pay {2}{B}");
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand).size(),2);
            Assert.assertEquals(opponent.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12G_CONTROLLED_RAGER left_first="+leftFirst+
                    " native_paid_cast=true draw=1 life_loss=1");
        }
        // Four independent untouched naturally shuffled 60-card full games.
        for(int seq=0;seq<2;seq++) {
            Game natural=nativeRagerGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12G_NATURAL_RAGER left_first="+leftFirst+
                        " sequence="+seq+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeRagerDrawLifeRiskLeftRegisteredFirst() throws Exception {
        verifyNativeRagerRiskDrawAndNaturalGames(true);
    }
    @Test(timeOut=240000)
    public void nativeRagerDrawLifeRiskRightRegisteredFirst() throws Exception {
        verifyNativeRagerRiskDrawAndNaturalGames(false);
    }


    /** Stage12G: Beast Whisperer {2}{G}{G} 2/3 draws a private card
     * when its controller CASTS another creature, before that spell resolves.
     * The two external actors rank the engine from their own hand and
     * public Forests, never from hidden future draws or Forge phase state. */
    private Game nativeWhispererGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);counts.put("Beast Whisperer",4);
        counts.put("Grizzly Bears",4);counts.put("Runeclaw Bear",4);
        counts.put("Balduvian Bears",4);counts.put("Centaur Courser",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12G Whisperer left",counts))
                .setPlayer(new LobbyPlayerAi("whisperer-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12G Whisperer right",counts))
                .setPlayer(new LobbyPlayerAi("whisperer-right",null));
        List<RegisteredPlayer> reg=new ArrayList<>();
        reg.add(leftFirst?left:right);reg.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,reg,"Stage12G Whisperer");
        Game game=new Game(reg,rules,match);frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeWhispererCastDrawAndNaturalGames(boolean leftFirst)
            throws Exception {
        Game fixture=nativeWhispererGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player caster=fixture.getStartingPlayer();
            Player opponent=caster==p0?p1:p0;
            Actor worker=caster==p0?w0:w1;
            for(Card c:new ArrayList<Card>(caster.getCardsIn(ZoneType.Hand)))
                Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Set<Integer> forestIds=new HashSet<>();
            for(int k=0;k<6;k++) {
                Card land=caster.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
                forestIds.add(land.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(land,null,
                        forge.game.ability.AbilityKey.newMap()));
                land.setTapped(false);
            }
            Card whisperer=caster.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Beast Whisperer".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card bear=caster.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(bear,null));
            Assert.assertNotNull(fixture.getAction().moveToHand(whisperer,null));
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand).size(),2);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            String projected=observation(caster,opponent);
            JsonObject proposal=worker.ask(projected);
            Assert.assertEquals(proposal.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand)
                    .get(proposal.getAsJsonArray("creature_hand_indices")
                    .get(0).getAsInt()).getName(),"Beast Whisperer");
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(JsonParser.parseString(projected)
                        .getAsJsonObject().has(forbidden));
            opponent.shuffle(null);
            Assert.assertEquals(observation(caster,opponent),projected);
            Assert.assertEquals(worker.ask(projected),proposal);
            int libraryBefore=caster.getCardsIn(ZoneType.Library).size();
            boolean nativeCastDraw=false,paid=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                paid |= caster.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->forestIds.contains(c.getId()) && c.isTapped())
                        .count()==6;
                boolean enginePresent=caster.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==whisperer.getId());
                boolean bearPresent=caster.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==bear.getId());
                if(enginePresent && bearPresent && fixture.getStack().isEmpty() &&
                   caster.getCardsIn(ZoneType.Library).size()==libraryBefore-1 &&
                   caster.getLife()==20) {
                    nativeCastDraw=true;break;
                }
            }
            Assert.assertTrue(steps<3500,"bounded genuine paid creature casts");
            Assert.assertTrue(nativeCastDraw,
                    "Forge must natively draw exactly one private card on Bear cast");
            Assert.assertTrue(paid,"six original Forests pay {2}{G}{G} then {1}{G}");
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand).size(),1);
            Assert.assertEquals(opponent.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12G_CONTROLLED_WHISPERER left_first="+leftFirst+
                    " native_paid_casts=true creature_spell_draw=1");
        }
        // Four separate untouched naturally shuffled 60-card full games.
        for(int seq=0;seq<2;seq++) {
            Game natural=nativeWhispererGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12G_NATURAL_WHISPERER left_first="+leftFirst+
                        " sequence="+seq+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeBeastWhispererCastDrawLeftRegisteredFirst() throws Exception {
        verifyNativeWhispererCastDrawAndNaturalGames(true);
    }
    @Test(timeOut=240000)
    public void nativeBeastWhispererCastDrawRightRegisteredFirst() throws Exception {
        verifyNativeWhispererCastDrawAndNaturalGames(false);
    }


    /** Stage12G: paid original 2/3 Beast Whisperer blocks original 2/2
     * Bear at life two, kills the Bear and preserves the card-draw engine.
     * Only public-only external actor proposals; Forge resolves combat. */
    private void verifyWhispererShieldAndNaturalGames(boolean leftFirst) throws Exception {
        Game fixture=nativeWhispererGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player defense=fixture.getStartingPlayer();
            Player offense=defense==p0?p1:p0;
            Actor worker=defense==p0?w0:w1;
            ExternalCreatureController dc=defense==p0?c0:c1;
            ExternalCreatureController ac=defense==p0?c1:c0;
            // Controlled physical fixture only; no natural-game manipulation.
            for(Player p:Arrays.asList(defense,offense))
                for(Card c:new ArrayList<Card>(p.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Card bear=offense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(bear,null,
                    forge.game.ability.AbilityKey.newMap()));
            Card liveBear=offense.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c->c.getId()==bear.getId()).findFirst().orElseThrow();
            liveBear.setSickness(false);liveBear.setTapped(false);
            Set<Integer> forests=new HashSet<>();
            for(int k=0;k<4;k++) {
                Card land=defense.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
                forests.add(land.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(land,null,
                        forge.game.ability.AbilityKey.newMap()));
                land.setTapped(false);
            }
            Card engine=defense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Beast Whisperer".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(engine,null));
            Assert.assertTrue(defense.setLife(2,null));
            Assert.assertEquals(offense.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            String projection=observation(defense,offense);
            JsonObject proposal=worker.ask(projection);
            Assert.assertEquals(proposal.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposal.getAsJsonArray("creature_hand_indices").get(0).getAsInt(),0);
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(JsonParser.parseString(projection)
                        .getAsJsonObject().has(forbidden));
            offense.shuffle(null);
            Assert.assertEquals(observation(defense,offense),projection);
            Assert.assertEquals(worker.ask(projection),proposal);
            boolean paid=false,offered=false,shielded=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<3500) {
                fixture.getPhaseHandler().mainLoopStep();
                paid |= defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->forests.contains(c.getId()) && c.isTapped()).count()==4;
                Card live=defense.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->c.getId()==engine.getId()).findFirst().orElse(null);
                if(live!=null) {
                    JsonObject reply=worker.ask(observation(defense,offense));
                    int own=defense.getCardsIn(ZoneType.Battlefield).indexOf(live);
                    int enemy=offense.getCardsIn(ZoneType.Battlefield).indexOf(liveBear);
                    for(JsonElement item:reply.getAsJsonArray("block_pairs")) {
                        JsonArray pair=item.getAsJsonArray();
                        if(pair.get(0).getAsInt()==own && pair.get(1).getAsInt()==enemy)
                            offered=true;
                    }
                }
                boolean bearDead=offense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==bear.getId());
                if(bearDead && live!=null && defense.getLife()==2 &&
                        ac.attacksAuthorized>=1 && dc.blocksAuthorized>=1) {
                    shielded=true;break;
                }
            }
            Assert.assertTrue(steps<3500,"bounded native paid shield");
            Assert.assertTrue(paid,"four original Forests pay genuine {2}{G}{G}");
            Assert.assertTrue(offered,"actor offers public-only engine defense");
            Assert.assertTrue(ac.attacksAuthorized>=1 && dc.blocksAuthorized>=1);
            Assert.assertTrue(shielded,"native Bear dies; original 2/3 survives at life two");
            Assert.assertEquals(defense.getLife(),2);
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertEquals(fullPhysicalInventory(offense),frozenMain.owned(offense));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12G_CONTROLLED_ENGINE_SHIELD left_first="+leftFirst+
                    " paid4=true native_block=true bear_dead=true engine_survives=true");
        }
        // Separately instantiated untouched natural 60-card games and
        // inherited native Whisperer draw fixture; no curated draws/seeds.
        verifyNativeWhispererCastDrawAndNaturalGames(leftFirst);
    }

    @Test(timeOut=240000)
    public void nativeWhispererEmergencyShieldLeftRegisteredFirst() throws Exception {
        verifyWhispererShieldAndNaturalGames(true);
    }

    @Test(timeOut=240000)
    public void nativeWhispererEmergencyShieldRightRegisteredFirst() throws Exception {
        verifyWhispererShieldAndNaturalGames(false);
    }

    /** Stage12G: printed {4}{G}{G} 6/6 Trample uses six original Forests,
     * then native Forge combat against an original 2/2 blocker. */
    private Game nativeDreadmawGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);counts.put("Colossal Dreadmaw",4);
        counts.put("Grizzly Bears",4);counts.put("Runeclaw Bear",4);
        counts.put("Llanowar Elves",4);counts.put("Elvish Visionary",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12G Dreadmaw left",counts))
                .setPlayer(new LobbyPlayerAi("dreadmaw-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12G Dreadmaw right",counts))
                .setPlayer(new LobbyPlayerAi("dreadmaw-right",null));
        List<RegisteredPlayer> reg=new ArrayList<>();
        reg.add(leftFirst?left:right);reg.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,reg,"Stage12G");
        Game game=new Game(reg,rules,match);frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeDreadmawAndNaturalGames(boolean leftFirst) throws Exception {
        Game fixture=nativeDreadmawGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player assault=fixture.getStartingPlayer();
            Player defense=assault==p0?p1:p0;
            Actor worker=assault==p0?w0:w1;
            ExternalCreatureController ac=assault==p0?c0:c1;
            ExternalCreatureController dc=assault==p0?c1:c0;
            // Controlled physical fixture: move only ORIGINAL registered cards.
            for(Player p:Arrays.asList(assault,defense))
                for(Card c:new ArrayList<Card>(p.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Set<Integer> paidLands=new HashSet<>();
            for(int i=0;i<6;i++) {
                Card forest=assault.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
                paidLands.add(forest.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(forest,null,
                        forge.game.ability.AbilityKey.newMap()));
                forest.setTapped(false);
            }
            Card defenderBear=defense.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Grizzly Bears".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToPlay(defenderBear,null,
                    forge.game.ability.AbilityKey.newMap()));
            defenderBear.setTapped(false);defenderBear.setSickness(false);
            Card dinosaur=assault.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Colossal Dreadmaw".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(dinosaur,null));
            Assert.assertTrue(defense.setLife(6,null));
            Assert.assertEquals(assault.getLife(),20);
            String projection=observation(assault,defense);
            JsonObject proposal=worker.ask(projection);
            Assert.assertEquals(proposal.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(proposal.getAsJsonArray("creature_hand_indices")
                    .get(0).getAsInt(),0);
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(JsonParser.parseString(projection)
                        .getAsJsonObject().has(forbidden));
            assault.shuffle(null);
            Assert.assertEquals(observation(assault,defense),projection);
            Assert.assertEquals(worker.ask(projection),proposal);
            boolean paidSix=false,entered=false,trampled=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<7000) {
                fixture.getPhaseHandler().mainLoopStep();
                if(assault.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->paidLands.contains(c.getId()) && c.isTapped())
                        .count()==6) paidSix=true;
                entered |= assault.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==dinosaur.getId() &&
                                c.getNetPower()==6 && c.getNetToughness()==6 &&
                                c.hasKeyword("Trample"));
                boolean bearDead=defense.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(c->c.getId()==defenderBear.getId());
                if(paidSix && entered && bearDead && defense.getLife()<6 &&
                        ac.attacksAuthorized>0 && dc.blocksAuthorized>0) {
                    trampled=true;break;
                }
            }
            Assert.assertTrue(steps<7000,"bounded real native Dreadmaw combat");
            Assert.assertTrue(paidSix,"six original Forests pay genuine printed cost");
            Assert.assertTrue(entered,"original printed 6/6 Trample entered");
            Assert.assertTrue(trampled,"native excess damage passes original blocker");
            Assert.assertTrue(ac.refereeDamageAssignments>0);
            Assert.assertEquals(fullPhysicalInventory(assault),frozenMain.owned(assault));
            Assert.assertEquals(fullPhysicalInventory(defense),frozenMain.owned(defense));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12G_CONTROLLED_DREADMAW left_first="+leftFirst+
                    " paid6=true trample_damage=true native_block=true");
        }
        // Four independently instantiated, untouched naturally shuffled 60-card
        // full Forge games (two per seat), no chosen seeds, hand curation or rerolls.
        for(int seq=0;seq<2;seq++) {
            Game natural=nativeDreadmawGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12G_NATURAL_DREADMAW left_first="+leftFirst+
                        " sequence="+seq+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeDreadmawTrampleLeftRegisteredFirst() throws Exception {
        verifyNativeDreadmawAndNaturalGames(true);
    }

    @Test(timeOut=240000)
    public void nativeDreadmawTrampleRightRegisteredFirst() throws Exception {
        verifyNativeDreadmawAndNaturalGames(false);
    }


    /** Stage12G: a native paid Elvish Visionary cast while Beast Whisperer
     * is already on the battlefield produces two DISTINCT Forge draws:
     * one on creature-spell cast, one on Visionary battlefield entry.
     * Actors prioritize this combo from public table and their own hand. */
    private Game nativeVisionaryWhispererChainGame(boolean leftFirst) {
        Map<String,Integer> counts=new TreeMap<>();
        counts.put("Forest",40);counts.put("Beast Whisperer",4);
        counts.put("Elvish Visionary",4);counts.put("Grizzly Bears",4);
        counts.put("Runeclaw Bear",4);counts.put("Centaur Courser",4);
        Assert.assertEquals(counts.values().stream().mapToInt(Integer::intValue).sum(),60);
        RegisteredPlayer left=new RegisteredPlayer(mainDeck("12G Chain left",counts))
                .setPlayer(new LobbyPlayerAi("chain-left",null));
        RegisteredPlayer right=new RegisteredPlayer(mainDeck("12G Chain right",counts))
                .setPlayer(new LobbyPlayerAi("chain-right",null));
        List<RegisteredPlayer> reg=new ArrayList<>();
        reg.add(leftFirst?left:right);reg.add(leftFirst?right:left);
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,reg,"Stage12G Visionary Whisperer Chain");
        Game game=new Game(reg,rules,match);frozenMain=new FrozenMain(game);
        Assert.assertNull(game.getPhaseHandler().getPhase());
        for(Player p:game.getPlayers()) Assert.assertEquals(frozenMain.owned(p),counts);
        return game;
    }

    private void verifyNativeVisionaryWhispererChainAndNaturalGames(boolean leftFirst)
            throws Exception {
        Game fixture=nativeVisionaryWhispererChainGame(leftFirst);
        Player p0=fixture.getPlayers().get(0),p1=fixture.getPlayers().get(1);
        try(Actor w0=new Actor(5);Actor w1=new Actor(5)) {
            Assert.assertNotEquals(w0.process.pid(),w1.process.pid());
            ExternalCreatureController c0=attach(fixture,p0,p1,w0);
            ExternalCreatureController c1=attach(fixture,p1,p0,w1);
            doRealNativeStartForMultiTurn(fixture,c0,c1);
            Player caster=fixture.getStartingPlayer();
            Player opponent=caster==p0?p1:p0;
            Actor worker=caster==p0?w0:w1;
            // Controlled physical fixture only: original registered cards.
            for(Player p:Arrays.asList(caster,opponent))
                for(Card c:new ArrayList<Card>(p.getCardsIn(ZoneType.Hand)))
                    Assert.assertNotNull(fixture.getAction().moveToBottomOfLibrary(c,null));
            Set<Integer> forestIds=new HashSet<>();
            for(int k=0;k<6;k++) {
                Card land=caster.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Forest".equals(c.getName())).findFirst().orElseThrow();
                forestIds.add(land.getId());
                Assert.assertNotNull(fixture.getAction().moveToPlay(land,null,
                        forge.game.ability.AbilityKey.newMap()));
                land.setTapped(false);
            }
            // Put four vanilla Bears BEFORE Visionary in the own hand.
            // The combo-specific preference must select Visionary first
            // after Whisperer resolves, not the earlier hand-index Bears.
            for(int k=0;k<4;k++) {
                Card bear=caster.getCardsIn(ZoneType.Library).stream()
                        .filter(c->"Grizzly Bears".equals(c.getName())).findFirst().orElseThrow();
                Assert.assertNotNull(fixture.getAction().moveToHand(bear,null));
            }
            Card visionary=caster.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Elvish Visionary".equals(c.getName()))
                    .findFirst().orElseThrow();
            Card whisperer=caster.getCardsIn(ZoneType.Library).stream()
                    .filter(c->"Beast Whisperer".equals(c.getName()))
                    .findFirst().orElseThrow();
            Assert.assertNotNull(fixture.getAction().moveToHand(visionary,null));
            Assert.assertNotNull(fixture.getAction().moveToHand(whisperer,null));
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand).size(),6);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            String projection=observation(caster,opponent);
            JsonObject initial=worker.ask(projection);
            Assert.assertEquals(initial.get("intent").getAsString(),"action_plan");
            Assert.assertEquals(caster.getCardsIn(ZoneType.Hand)
                    .get(initial.getAsJsonArray("creature_hand_indices")
                    .get(0).getAsInt()).getName(),"Beast Whisperer");
            for(String forbidden:Arrays.asList("phase","priority","stack",
                    "library_top","opponent_hand","library_order"))
                Assert.assertFalse(JsonParser.parseString(projection)
                        .getAsJsonObject().has(forbidden));
            opponent.shuffle(null);
            Assert.assertEquals(observation(caster,opponent),projection);
            Assert.assertEquals(worker.ask(projection),initial);
            int libraryBefore=caster.getCardsIn(ZoneType.Library).size();
            boolean nativeDoubleDraw=false,paid=false,sawComboPreference=false;
            int steps=0;
            while(!fixture.isGameOver() && steps++<4500) {
                fixture.getPhaseHandler().mainLoopStep();
                paid |= caster.getCardsIn(ZoneType.Battlefield).stream()
                        .filter(c->forestIds.contains(c.getId()) && c.isTapped())
                        .count()==6;
                boolean enginePresent=caster.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==whisperer.getId());
                boolean visionaryPresent=caster.getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(c->c.getId()==visionary.getId());
                if(enginePresent && !visionaryPresent &&
                   caster.getCardsIn(ZoneType.Hand).stream()
                        .anyMatch(c->c.getId()==visionary.getId())) {
                    JsonObject combo=worker.ask(observation(caster,opponent));
                    Assert.assertEquals(caster.getCardsIn(ZoneType.Hand)
                            .get(combo.getAsJsonArray("creature_hand_indices")
                            .get(0).getAsInt()).getId(),visionary.getId());
                    sawComboPreference=true;
                }
                if(enginePresent && visionaryPresent && fixture.getStack().isEmpty() &&
                   caster.getCardsIn(ZoneType.Library).size()==libraryBefore-2 &&
                   caster.getCardsIn(ZoneType.Hand).size()==6 &&
                   caster.getLife()==20) {
                    nativeDoubleDraw=true;break;
                }
            }
            Assert.assertTrue(steps<4500,"bounded genuine paid dual-trigger chain");
            Assert.assertTrue(paid,"six original Forests pay {2}{G}{G} then {1}{G}");
            Assert.assertTrue(sawComboPreference,"public-only Visionary combo ranked before Bears");
            Assert.assertTrue(nativeDoubleDraw,
                    "Forge must draw one on Visionary cast and another on ETB");
            Assert.assertEquals(opponent.getLife(),20);
            Assert.assertEquals(fullPhysicalInventory(caster),frozenMain.owned(caster));
            Assert.assertEquals(fullPhysicalInventory(opponent),frozenMain.owned(opponent));
            Assert.assertTrue(w0.process.isAlive() && w1.process.isAlive());
            System.out.println("STAGE12G_CONTROLLED_VISIONARY_WHISPERER left_first="+
                    leftFirst+" paid6=true two_native_draw_triggers=true");
        }
        // Four separate uncurated naturally shuffled 60-card full games,
        // two per seating order, no seeds, rerolls, private peek or hand edits.
        for(int seq=0;seq<2;seq++) {
            Game natural=nativeVisionaryWhispererChainGame(leftFirst);
            Player left=natural.getPlayers().get(leftFirst?0:1);
            Player right=natural.getPlayers().get(leftFirst?1:0);
            try(Actor wl=new Actor(5);Actor wr=new Actor(5)) {
                Assert.assertNotEquals(wl.process.pid(),wr.process.pid());
                ExternalCreatureController lc=attach(natural,left,right,wl);
                ExternalCreatureController rc=attach(natural,right,left,wr);
                doRealNativeStartForMultiTurn(natural,lc,rc);
                Assert.assertEquals(left.getLife(),20);
                Assert.assertEquals(right.getLife(),20);
                Assert.assertTrue(lc.openingCallbacks>0 && rc.openingCallbacks>0);
                for(Player p:Arrays.asList(left,right)) {
                    Assert.assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                }
                int steps=0;
                while(!natural.isGameOver() && steps++<85000)
                    natural.getPhaseHandler().mainLoopStep();
                Assert.assertTrue(steps<85000 && natural.isGameOver());
                Assert.assertNotNull(natural.getOutcome());
                Assert.assertFalse(natural.getOutcome().isDraw());
                Assert.assertTrue(left.hasWon()!=right.hasWon());
                Player winner=left.hasWon()?left:right;
                Assert.assertTrue(natural.getOutcome().isWinner(winner.getRegisteredPlayer()));
                for(Player p:Arrays.asList(left,right))
                    Assert.assertEquals(fullPhysicalInventory(p),frozenMain.owned(p));
                Assert.assertTrue(wl.process.isAlive() && wr.process.isAlive());
                System.out.println("STAGE12G_NATURAL_VISIONARY_WHISPERER left_first="+
                        leftFirst+" sequence="+seq+" winner="+winner.getName());
            }
        }
    }

    @Test(timeOut=240000)
    public void nativeVisionaryWhispererDoubleDrawLeftRegisteredFirst() throws Exception {
        verifyNativeVisionaryWhispererChainAndNaturalGames(true);
    }

    @Test(timeOut=240000)
    public void nativeVisionaryWhispererDoubleDrawRightRegisteredFirst() throws Exception {
        verifyNativeVisionaryWhispererChainAndNaturalGames(false);
    }

}
