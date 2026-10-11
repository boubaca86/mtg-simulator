package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameStage;
import forge.game.Match;
import forge.deck.Deck;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.card.Card;
import forge.game.cost.CostPartMana;
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
 * Test-only real Forge referee + two separate, persistent Python actor processes.
 * Only allowlisted JSON is transferred; actors never hold Forge Game or Card.
 * Actors request a basic-land play, a named vanilla/mana creature cast, or pass.
 * The referee alone decides legality, mana payment, priority and stack resolution.
 */
public final class IsolatedForgeRegisteredPriorityTest extends AITest {


    /** Snapshot directly from each authoritative RegisteredPlayer main deck. */
    private static final class RegisteredSnapshot {
        final Map<Player,Map<String,Integer>> frozen=new IdentityHashMap<>();
        RegisteredSnapshot(Game game) {
            for(Player player:game.getRegisteredPlayers()) {
                Map<String,Integer> counts=new TreeMap<>();
                if(player.getRegisteredPlayer()==null ||
                   player.getRegisteredPlayer().getDeck()==null ||
                   player.getRegisteredPlayer().getDeck().getMain()==null)
                    throw new IllegalStateException("no trusted registered deck");
                for(Map.Entry<PaperCard,Integer> e:
                        player.getRegisteredPlayer().getDeck().getMain()) {
                    if(e.getKey()==null || e.getValue()==null || e.getValue()<=0)
                        throw new IllegalStateException("invalid registered pool");
                    counts.merge(e.getKey().getName(),e.getValue(),Integer::sum);
                }
                if(counts.isEmpty()) throw new IllegalStateException("empty registered deck");
                frozen.put(player,Collections.unmodifiableMap(counts));
            }
            if(frozen.size()!=2) throw new IllegalStateException("requires two registered players");
        }
        Map<String,Integer> owned(Player self) {
            Map<String,Integer> result=frozen.get(self);
            if(result==null) throw new IllegalArgumentException("unregistered actor");
            return result;
        }
    }

    private static PaperCard known(String name) {
        PaperCard c=FModel.getMagicDb().getCommonCards().getCard(name);
        if(c==null) throw new IllegalStateException("missing card "+name);
        return c;
    }

    private static Game registeredGame() {
        Deck deckA=new Deck("Stage9Q A");
        deckA.getMain().add(known("Forest"),4);
        deckA.getMain().add(known("Island"),3);
        deckA.getMain().add(known("Grizzly Bears"),2);
        deckA.getMain().add(known("Llanowar Elves"),1);
        deckA.getMain().add(known("Shock"),1);
        Deck deckB=new Deck("Stage9Q B");
        deckB.getMain().add(known("Forest"),4);
        deckB.getMain().add(known("Island"),5);
        deckB.getMain().add(known("Grizzly Bears"),2);
        deckB.getMain().add(known("Counterspell"),1);
        List<RegisteredPlayer> players=new ArrayList<>();
        players.add(new RegisteredPlayer(deckA).setPlayer(new LobbyPlayerAi("actorA",null)));
        players.add(new RegisteredPlayer(deckB).setPlayer(new LobbyPlayerAi("actorB",null)));
        GameRules rules=new GameRules(GameType.Constructed);
        Match match=new Match(rules,players,"Stage9Q");
        Game game=new Game(players,rules,match);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1,game.getPlayers().get(1));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    /** Seed exactly the registered main deck, choosing only initial locations. */
    private static void seedRegistered(Player p, Map<String,Integer> hand,
            Map<String,Integer> battlefield) {
        for(Map.Entry<PaperCard,Integer> e:p.getRegisteredPlayer().getDeck().getMain()) {
            String name=e.getKey().getName();
            int handCount=hand.getOrDefault(name,0);
            int fieldCount=battlefield.getOrDefault(name,0);
            if(handCount<0 || fieldCount<0 || handCount+fieldCount>e.getValue())
                throw new IllegalArgumentException("unregistered fixture card "+name);
            for(int i=0;i<e.getValue();i++) {
                Card card=Card.fromPaperCard(e.getKey(),p);
                card.setGameTimestamp(p.getGame().getNextTimestamp());
                ZoneType target=i<handCount?ZoneType.Hand:
                    i<handCount+fieldCount?ZoneType.Battlefield:ZoneType.Library;
                p.getZone(target).add(card);
                if(target==ZoneType.Battlefield && card.isCreature())
                    card.setSickness(false); // fixture already under control
            }
        }
    }

    private static Map<String,Integer> actualOwnedPool(Player p) {
        Map<String,Integer> counts=new TreeMap<>();
        for(ZoneType zone:Arrays.asList(ZoneType.Library,ZoneType.Hand,
                ZoneType.Battlefield,ZoneType.Graveyard,ZoneType.Exile,ZoneType.Stack)) {
            for(Card c:p.getCardsIn(zone)) {
                if(c.getOwner()==p && !c.isToken())counts.merge(c.getName(),1,Integer::sum);
            }
        }
        return counts;
    }

    private static final class Actor implements AutoCloseable {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;

        Actor() throws Exception {
            String script = System.getenv("STAGE9Q_WORKER");
            Assert.assertNotNull(script, "CI must define STAGE9Q_WORKER");
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
            validateReply(reply);
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
    private static String observation(Player self, Player other, Map<String,Integer> decklist) {
        JsonObject result = new JsonObject();
        result.addProperty("objective","Win the game under referee-enforced Magic rules");
        JsonObject own = new JsonObject();
        JsonArray hand = new JsonArray();
        for (Card card : self.getCardsIn(ZoneType.Hand)) {
            hand.add(card.isFaceDown() ? "FACE_DOWN_UNKNOWN" : card.getName());
        }
        own.add("hand",hand);
        JsonArray deck = new JsonArray();
        for (Map.Entry<String,Integer> e : decklist.entrySet()) {
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
           !"Grizzly Bears".equals(creature.getName())) return null;
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
               !"Grizzly Bears".equals(c.getName()) ||
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
    private static final class ExternalCreatureController extends PlayerControllerAi {
        final Actor subprocess;
        final Player controlled;
        final Player opponent;
        final Map<String,Integer> ownDecklist;
        int priorityCallbacks = 0;
        ExternalCreatureController(Game game,Player self,Player other,Actor worker,RegisteredSnapshot snapshot) {
            super(game,self,self.getController().getLobbyPlayer());
            subprocess=worker;
            controlled=self;
            opponent=other;
            ownDecklist=snapshot.owned(self);
        }
        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            try {
                String view=observation(controlled,opponent,ownDecklist);
                JsonObject response=subprocess.ask(view);
                priorityCallbacks++;
                SpellAbility requested=refereeAuthorizeLand(controlled,response);
                if (requested==null) requested=refereeAuthorizeCreature(controlled,response);
                if (requested!=null) return Collections.singletonList(requested);
                return null; // pass, or attempted illegal land; Forge advances priority
            } catch(Exception ex) {
                throw new IllegalStateException("Restricted actor priority IPC failed",ex);
            }
        }
        int attackCallbacks=0;
        @Override
        public void declareAttackers(Player attacker,Combat combat) {
            try {
                attackCallbacks++;
                JsonObject proposed=subprocess.ask(observation(controlled,opponent,ownDecklist));
                refereeDeclareAttackers(controlled,opponent,combat,proposed);
            } catch(Exception ex) {
                throw new IllegalStateException("Actor attack IPC failed",ex);
            }
        }
        @Override
        public void declareBlockers(Player defender,Combat combat) {
            // This canary has no blockers. Never run inherited full-state AI.
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

    private static ExternalCreatureController attach(Game game,Player self,Player opponent,Actor actor,RegisteredSnapshot snapshot) {
        ExternalCreatureController controller=new ExternalCreatureController(game,self,opponent,actor,snapshot);
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
    public void trueRegisteredDecksStayImmutableAcrossRealDrawAndShuffles() throws Exception {
        Game game=registeredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        seedRegistered(a,Collections.emptyMap(),Collections.emptyMap());
        seedRegistered(b,Collections.emptyMap(),Collections.emptyMap());
        RegisteredSnapshot snap=new RegisteredSnapshot(game);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,a,b,aa,snap);
            ExternalCreatureController cb=attach(game,b,a,bb,snap);
            Assert.assertNotEquals(aa.process.pid(),bb.process.pid());
            Assert.assertEquals(ca.ownDecklist.get("Forest"),Integer.valueOf(4));
            Assert.assertEquals(cb.ownDecklist.get("Island"),Integer.valueOf(5));
            Assert.assertEquals(actualOwnedPool(a),ca.ownDecklist);
            Assert.assertEquals(actualOwnedPool(b),cb.ownDecklist);
            String beforeA=observation(a,b,ca.ownDecklist);
            String beforeB=observation(b,a,cb.ownDecklist);
            for(int i=0;i<10;i++) {
                a.getZone(ZoneType.Library).shuffle();
                b.getZone(ZoneType.Library).shuffle();
                Assert.assertEquals(observation(a,b,ca.ownDecklist),beforeA);
                Assert.assertEquals(observation(b,a,cb.ownDecklist),beforeB);
            }
            a.drawCard();
            Assert.assertNotEquals(observation(a,b,ca.ownDecklist),beforeA);
            Assert.assertEquals(observation(b,a,cb.ownDecklist),beforeB);
            Assert.assertEquals(actualOwnedPool(a),ca.ownDecklist);
            Assert.assertEquals(actualOwnedPool(b),cb.ownDecklist);
            Assert.assertEquals(ca.ownDecklist,snap.owned(a));
        }
    }

    @Test(timeOut=120000)
    public void actualPriorityLandActionUsesRegisteredDeckWithoutHandCreatedDecklist() throws Exception {
        Game game=registeredGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedRegistered(active,Collections.singletonMap("Forest",1),Collections.emptyMap());
        seedRegistered(other,Collections.singletonMap("Island",1),Collections.emptyMap());
        RegisteredSnapshot snap=new RegisteredSnapshot(game);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,active,other,aa,snap);
            attach(game,other,active,bb,snap);
            Assert.assertEquals(actualOwnedPool(active),ca.ownDecklist);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(active.getLandsPlayedThisTurn(),1);
            Assert.assertEquals(actualOwnedPool(active),ca.ownDecklist);
            Assert.assertEquals(ca.ownDecklist.get("Forest"),Integer.valueOf(4));
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),0);
            Assert.assertTrue(active.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c->
                "Forest".equals(c.getName())));
        }
    }

    @Test(timeOut=120000)
    public void paidCreatureCastingUsesFrozenRegisteredDeckThroughRealStack() throws Exception {
        Game game=registeredGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedRegistered(active,Collections.singletonMap("Grizzly Bears",1),
            Collections.singletonMap("Forest",2));
        seedRegistered(other,Collections.emptyMap(),Collections.emptyMap());
        RegisteredSnapshot snap=new RegisteredSnapshot(game);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,active,other,aa,snap);
            attach(game,other,active,bb,snap);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertFalse(game.getStack().isEmpty(),"real paid spell stays on stack");
            Assert.assertEquals(active.getCardsIn(ZoneType.Hand).size(),0);
            Assert.assertEquals(actualOwnedPool(active),ca.ownDecklist);
            game.getPhaseHandler().mainLoopStep();
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertTrue(active.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c->
                "Grizzly Bears".equals(c.getName())));
            Assert.assertEquals(actualOwnedPool(active),ca.ownDecklist);
        }
    }

    @Test(timeOut=120000)
    public void unblockedCombatFromRegisteredDeckRunsGenuineForgeDamage() throws Exception {
        Game game=registeredGame();
        Player active=game.getPlayers().get(1),other=game.getPlayers().get(0);
        seedRegistered(active,Collections.emptyMap(),
            Collections.singletonMap("Grizzly Bears",1));
        seedRegistered(other,Collections.emptyMap(),Collections.emptyMap());
        RegisteredSnapshot snap=new RegisteredSnapshot(game);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,active,other,aa,snap);
            attach(game,other,active,bb,snap);
            int steps=0;
            while(!game.isGameOver() && other.getLife()==20 && steps++<120)
                game.getPhaseHandler().mainLoopStep();
            Assert.assertEquals(other.getLife(),18);
            Assert.assertTrue(ca.attackCallbacks>0);
            Assert.assertEquals(actualOwnedPool(active),ca.ownDecklist);
        }
    }

    @Test(timeOut=120000)
    public void actorSnapshotRemainsFrozenAfterRegisteredDeckMutationAndRejectsForgery() throws Exception {
        Game game=registeredGame();
        Player a=game.getPlayers().get(0),b=game.getPlayers().get(1);
        seedRegistered(a,Collections.emptyMap(),Collections.emptyMap());
        seedRegistered(b,Collections.emptyMap(),Collections.emptyMap());
        RegisteredSnapshot snap=new RegisteredSnapshot(game);
        try(Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,a,b,aa,snap);
            attach(game,b,a,bb,snap);
            String before=observation(a,b,ca.ownDecklist);
            try {
                ca.ownDecklist.put("FORGED",999);
                Assert.fail("mutable registration");
            } catch(UnsupportedOperationException expected) {
                Assert.assertNotNull(expected);
            }
            b.getRegisteredPlayer().getDeck().getMain().add(known("Lightning Bolt"),4);
            Assert.assertEquals(observation(a,b,ca.ownDecklist),before);
            Assert.assertFalse(observation(a,b,ca.ownDecklist).contains("Lightning Bolt"));
        }
    }

}
