package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.game.Game;
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
public final class IsolatedForgeCombatAttackTest extends AITest {
    private static final List<String> DECK_A = Arrays.asList("Forest","Forest","Shock");
    private static final List<String> DECK_B = Arrays.asList("Island","Counterspell","Grizzly Bears");

    private static final class Actor implements AutoCloseable {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;

        Actor() throws Exception {
            String script = System.getenv("STAGE9N_WORKER");
            Assert.assertNotNull(script, "CI must define STAGE9N_WORKER");
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
        List<Card> table=self.getCardsIn(ZoneType.Battlefield);
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
        final List<String> ownDecklist;
        int priorityCallbacks = 0;
        ExternalCreatureController(Game game,Player self,Player other,Actor worker,List<String> decklist) {
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

    private static ExternalCreatureController attach(Game game,Player self,Player opponent,Actor actor,List<String> deck) {
        ExternalCreatureController controller=new ExternalCreatureController(game,self,opponent,actor,deck);
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
            ExternalCreatureController ca=attach(game,p0,p1,a,DECK_A);
            ExternalCreatureController cb=attach(game,p1,p0,b,DECK_B);
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
            ExternalCreatureController ca=attach(game,p0,p1,a,DECK_A);
            ExternalCreatureController cb=attach(game,p1,p0,b,DECK_B);
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
            ExternalCreatureController cs=attach(game,self,other,selfActor,DECK_A);
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
    @Test(timeOut=120000)
    public void realPriorityLoopAcceptsLandAndRejectsOutOfTurnProposal() throws Exception {
        Game game=initAndCreateGame();
        Player active=game.getPlayers().get(1), other=game.getPlayers().get(0);
        seedPrivateLibraries(active,other);
        addCardToZone("Forest",active,ZoneType.Hand);
        addCardToZone("Island",other,ZoneType.Hand);
        try (Actor aa=new Actor();Actor bb=new Actor()) {
            ExternalCreatureController ca=attach(game,active,other,aa,DECK_A);
            ExternalCreatureController cb=attach(game,other,active,bb,DECK_B);
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
            ExternalCreatureController ca=attach(game,active,other,aa,DECK_A);
            ExternalCreatureController cb=attach(game,other,active,bb,DECK_B);
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
        String aBefore=observation(active,other,DECK_A);
        addCardToZone("Counterspell",other,ZoneType.Hand);
        addCardToZone("Shock",other,ZoneType.Library);
        Assert.assertEquals(observation(active,other,DECK_A),aBefore);
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
            ExternalCreatureController ca=attach(game,active,other,aa,DECK_A);
            ExternalCreatureController cb=attach(game,other,active,bb,DECK_B);
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
            attach(game,active,other,aa,DECK_A);
            attach(game,other,active,bb,DECK_B);
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
            attach(game,active,other,aa,DECK_A);
            attach(game,other,active,bb,DECK_B);
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
            attach(game,active,other,aa,DECK_A);
            attach(game,other,active,bb,DECK_B);
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
        String baseline=observation(active,other,DECK_A);
        addCardToZone("Grizzly Bears",other,ZoneType.Hand);
        addCardToZone("Llanowar Elves",other,ZoneType.Library);
        Assert.assertEquals(observation(active,other,DECK_A),baseline,
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
            ExternalCreatureController ca=attach(game,active,defender,a,DECK_A);
            ExternalCreatureController cb=attach(game,defender,active,b,DECK_B);
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
            attach(game,active,defender,a,DECK_A);
            attach(game,defender,active,b,DECK_B);
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
            attach(game,active,defender,a,DECK_A);
            attach(game,defender,active,b,DECK_B);
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
            ExternalCreatureController ca=attach(game,active,defender,a,DECK_A);
            attach(game,defender,active,b,DECK_B);
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
        String baseline=observation(active,opponent,DECK_A);
        addCardToZone("Counterspell",opponent,ZoneType.Hand);
        addCardToZone("Shock",opponent,ZoneType.Library);
        Assert.assertEquals(observation(active,opponent,DECK_A),baseline);
    }

}
