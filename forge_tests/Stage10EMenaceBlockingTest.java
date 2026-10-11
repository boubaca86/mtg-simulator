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
public class Stage10EMenaceBlockingTest extends AITest {
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

    protected static final class Actor implements AutoCloseable {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;

        Actor() throws Exception {
            this(false);
        }
        Actor(boolean usePlan) throws Exception {
            this(usePlan ? 1 : 0);
        }
        Actor(int mode) throws Exception {
            String script=System.getenv(mode==3 ? "STAGE10E_MENACE_WORKER" :
                    mode==2 ? "STAGE10E_DEFENSE_WORKER" :
                    mode==1 ? "STAGE10E_LEGACY_PLAN_WORKER" : "STAGE10E_WORKER");
            Assert.assertNotNull(script, "CI must define STAGE10E_WORKER");
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

        /**
         * Opening-hand decision request is a message KIND, not an additional
         * game observation. The nested observation has the exact Stage9V
         * allowlist and no mulligan count/turn/opponent hidden information.
         */
        JsonObject askOpening(String observation) throws Exception {
            Assert.assertTrue(observation.length() < 16000);
            JsonObject envelope=new JsonObject();
            envelope.addProperty("request","own_opening_hand");
            envelope.add("observation",JsonParser.parseString(observation));
            String request=envelope.toString();
            Assert.assertTrue(request.length() < 16384);
            writer.write(request);
            writer.newLine();
            writer.flush();
            String line=reader.readLine();
            Assert.assertNotNull(line,"actor died during mulligan choice");
            Assert.assertTrue(line.length() < 2048);
            JsonObject reply=JsonParser.parseString(line).getAsJsonObject();
            int count=JsonParser.parseString(observation).getAsJsonObject()
                    .getAsJsonObject("own_cards").getAsJsonArray("hand").size();
            validateOpeningReply(reply,count);
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
    protected String observation(Player self, Player other) {
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

    private static void assertReplyRejected(String payload) {
        JsonObject reply = JsonParser.parseString(payload).getAsJsonObject();
        try {
            validateReply(reply);
        } catch (AssertionError expected) {
            return;
        }
        Assert.fail("FORGED_PAYLOAD_ACCEPTED: " + payload);
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
                    !"Grizzly Bears".equals(blocker.getName()) ||
                    !"Grizzly Bears".equals(attacker.getName()) ||
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
        if("action_plan".equals(reply.get("intent").getAsString())) {
            JsonArray candidates=reply.getAsJsonArray("attack_table_indices");
            if(candidates.size()==0) return;
            JsonArray authorized=new JsonArray();
            for(int i=0;i<candidates.size();i++) {
                int n=candidates.get(i).getAsInt();
                if(n<0 || n>=self.getCardsIn(ZoneType.Battlefield).size()) continue;
                Card card=self.getCardsIn(ZoneType.Battlefield).get(n);
                if(card.getController()==self && !card.isFaceDown() &&
                        "Grizzly Bears".equals(card.getName()) &&
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
    protected class ExternalCreatureController extends PlayerControllerAi {
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
        @Override
        public void declareAttackers(Player attacker,Combat combat) {
            try {
                attackCallbacks++;
                JsonObject proposed=subprocess.ask(observation(controlled,opponent));
                refereeDeclareAttackers(controlled,opponent,combat,proposed);
            } catch(Exception ex) {
                throw new IllegalStateException("Actor attack IPC failed",ex);
            }
        }
        int blockerCallbacks=0;
        int blocksAuthorized=0;
        @Override
        public void declareBlockers(Player defender,Combat combat) {
            try {
                blockerCallbacks++;
                JsonObject proposed=subprocess.ask(observation(controlled,opponent));
                blocksAuthorized+=refereeDeclareBlockers(controlled,opponent,combat,proposed);
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

    protected ExternalCreatureController attach(Game game,Player self,Player opponent,Actor actor) {
        ExternalCreatureController controller=new ExternalCreatureController(game,self,opponent,actor);
        self.dangerouslySetController(controller);
        return controller;
    }

    protected static void seedPrivateLibraries(Player a,Player b) {
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
            assertReplyRejected(candidate);
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
            assertReplyRejected(candidate);
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
            assertReplyRejected(payload);
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
        Card found=null;
        for(Card card:player.getCardsIn(ZoneType.Library)) {
            if("Grizzly Bears".equals(card.getName())) {found=card;break;}
        }
        Assert.assertNotNull(found,"must exist in registered 60-card library");
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
                if(dc.blocksAuthorized>=2) bothBlocks=true;
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

}
