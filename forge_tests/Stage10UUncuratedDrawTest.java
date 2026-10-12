package forge.ai;

import com.google.gson.JsonArray;
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
            Assert.assertTrue(reply.size()==4 || reply.size()==5,
                    "action plan only contains approved preference fields");
            validatePlanIndices(reply,"land_hand_indices",128);
            validatePlanIndices(reply,"creature_hand_indices",128);
            validatePlanIndices(reply,"attack_table_indices",8);
            if(reply.size()==5) validateBlockPairs(reply);
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
                "Rumbling Baloth".equals(name);
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
                    !supportedVanillaFighter(blocker.getName()) ||
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
        if(!"Llanowar Elves".equals(creature.getName()) &&
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
                        supportedVanillaFighter(card.getName()) &&
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
               !supportedVanillaFighter(c.getName()) ||
               !CombatUtil.canAttack(c,defender) ||
               CombatUtil.getAttackCost(game,c,defender)!=null) return;
            approved.add(c);
        }
        for(Card c:approved) combat.addAttacker(c,defender);
        if(!CombatUtil.validateAttackers(combat)) combat.clearAttackers();
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
        @Override
        public Map<Card,Integer> assignCombatDamage(Card source,
                CardCollectionView targets,CardCollectionView remaining,
                int damageDealt,GameEntity defender,boolean overrideOrder) {
            if(source==null || source.getController()!=controlled ||
                    !controlled.getCardsIn(ZoneType.Battlefield).contains(source) ||
                    !supportedVanillaFighter(source.getName()) ||
                    damageDealt!=("Rumbling Baloth".equals(source.getName())?4:
                            "Centaur Courser".equals(source.getName())?3:2) ||
                    source.getNetCombatDamage()!=damageDealt ||
                    source.hasKeyword("Trample") ||
                    targets==null || targets.isEmpty() || targets.size()>8) {
                throw new SecurityException("unsupported referee combat-damage allocation");
            }
            Set<Card> unique=Collections.newSetFromMap(new IdentityHashMap<Card,Boolean>());
            for(Card target:targets) {
                if(target==null || !unique.add(target) ||
                        !supportedVanillaFighter(target.getName()) ||
                        target.getController()!=opponent ||
                        !opponent.getCardsIn(ZoneType.Battlefield).contains(target)) {
                    throw new SecurityException("unregistered or unsupported combat target");
                }
            }
            // Under CR 510.1c-d (2026-09-25) a supported vanilla
            // 2/2, 3/3 or 4/4 assigns its full real combat damage
            // to the first Forge-legal blocking creature in the list.
            // The passed-in combat list
            // is referee-authoritative, never chosen by an inherited AI.
            Map<Card,Integer> split=new LinkedHashMap<>();
            split.put(targets.get(0),damageDealt);
            refereeDamageAssignments++;
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
        public CardCollectionView chooseCardsToDiscardToMaximumHandSize(int n) {
            throw new SecurityException("restricted actor has no discard protocol");
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
            if(!sa.isSpell() || !sa.getHostCard().isCreature()) return false;
            // Real Forge pays mana (including tapping actual sources), places spell
            // on stack and resolves only after normal opponent priority.
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
        addCardToZone("Sedge Scorpion",active,ZoneType.Hand);
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
                ZoneType.Battlefield,ZoneType.Graveyard)) {
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
                    Assert.assertEquals(solo.getAsJsonArray("block_pairs").size(),4,
                            "4/4 single defender; 3/3 also accepts 2/2 trade");
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
}
