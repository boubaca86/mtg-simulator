package forge.ai;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test-only trigger boundary, inheriting all 23 corrected Stage 9T regressions.
 * A single audited life-gain trigger can enter the real Forge stack without AI
 * preparation. Mandatory gain resolves through Forge; an optional choice stops
 * the disposable game explicitly. This is not a general trigger interpreter.
 */
public class Stage9UTriggerBoundaryTest extends Stage9TGuardedConfirmTest {
    private static final String OPTIONAL = "stage9u: optional trigger choice unsupported";
    private static final String SHAPE = "stage9u: unaudited trigger shape";
    private static final String SETUP = "stage9u: trigger target setup unsupported";
    private static final String ORDER = "stage9u: simultaneous trigger ordering unsupported";
    private static final String STATIC = "stage9u: static trigger execution unsupported";

    protected class TriggerController extends ExternalCreatureController {
        int confirmations;
        int stackedTriggers;
        int resolvedMandatoryEffects;
        int triggerPriorityCallbacks;

        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            // Referee-only diagnostic: actor itself never receives the stack.
            if (!getGame().getStack().isEmpty()
                    && getGame().getStack().peekAbility() instanceof WrappedAbility) {
                triggerPriorityCallbacks++;
            }
            return super.chooseSpellAbilityToPlay();
        }

        TriggerController(Game game, Player self, Player other, Actor worker) {
            super(game, self, other, worker);
        }

        /** Validate trusted referee objects before taking any automatic action.
         * Names alone are insufficient: costs, targets, chains, parameters and
         * optional status must also match the two pinned card scripts exactly. */
        protected SpellAbility checkedEffect(SpellAbility effect) {
            if (!(effect instanceof AbilitySub) || effect.isWrapper()
                    || effect.getActivatingPlayer() != controlled
                    || effect.getHostCard().getGame() != getGame()
                    || effect.getApi() != ApiType.GainLife || effect.isCopied()
                    || effect.isReplacementAbility() || effect.usesTargeting()
                    || effect.getPayCosts() != Cost.Zero || effect.getParent() != null
                    || effect.getSubAbility() != null
                    || !effect.getAdditionalAbilities().isEmpty()
                    || !effect.getAdditionalAbilityLists().isEmpty()
                    || !effect.getMapParams().equals(Map.of(
                        "DB", "GainLife", "Defined", "You", "LifeAmount", "1"))) {
                throw new SecurityException(SHAPE);
            }
            String name = effect.getHostCard().getName();
            boolean optional = "Soul's Attendant".equals(name);
            if (!optional && !"Soul Warden".equals(name)) {
                throw new SecurityException(SHAPE);
            }
            Trigger trigger = effect.getTrigger();
            Map<String,String> expected = new java.util.HashMap<>(Map.of(
                "Mode", "ChangesZone", "Origin", "Any", "Destination", "Battlefield",
                "ValidCard", "Creature.Other", "TriggerZones", "Battlefield",
                "Execute", "TrigGainLife", "TriggerDescription",
                optional ? "Whenever another creature enters, you may gain 1 life."
                         : "Whenever another creature enters, you gain 1 life."));
            if (optional) expected.put("OptionalDecider", "You");
            if (trigger == null || trigger.isStatic()
                    || trigger.getMode() != TriggerType.ChangesZone
                    || trigger.getHostCard() != effect.getHostCard()
                    || !trigger.getMapParams().equals(expected)
                    || effect.isOptionalTrigger() != optional) {
                throw new SecurityException(SHAPE);
            }
            return effect;
        }

        protected WrappedAbility checkedWrapper(SpellAbility ability) {
            if (!(ability instanceof WrappedAbility)) throw new SecurityException(SHAPE);
            WrappedAbility wrapper = (WrappedAbility) ability;
            SpellAbility effect = checkedEffect(wrapper.getWrappedAbility());
            if (wrapper.getTrigger() != effect.getTrigger()
                    || wrapper.getDecider() != (effect.isOptionalTrigger() ? controlled : null)) {
                throw new SecurityException(SHAPE);
            }
            return wrapper;
        }

        @Override
        public boolean confirmTrigger(WrappedAbility wrapper) {
            checkedWrapper(wrapper);
            confirmations++;
            if (!wrapper.isMandatory()) throw new SecurityException(OPTIONAL);
            return true; // No choice exists; never consult brains.doTrigger.
        }

        @Override
        public void playSpellAbilityNoStack(SpellAbility effect, boolean canSetupTargets) {
            if (canSetupTargets) throw new SecurityException(SETUP);
            checkedEffect(effect);
            if (!effect.isMandatory()) throw new SecurityException(OPTIONAL);
            // Audited DB GainLife has no cost or actor choice. In particular,
            // do not call ComputerUtil.playNoStack: it constructs AiCostDecision.
            AbilityUtils.resolve(effect);
            resolvedMandatoryEffects++;
        }

        @Override
        public List<SpellAbility> orderSimultaneousSa(List<SpellAbility> abilities) {
            if (abilities == null || abilities.size() > 1) throw new SecurityException(ORDER);
            if (abilities.isEmpty()) return Collections.emptyList();
            return Collections.singletonList(checkedWrapper(abilities.get(0)));
        }

        @Override
        public void orderAndPlaySimultaneousSa(List<SpellAbility> abilities) {
            for (SpellAbility ability : orderSimultaneousSa(abilities)) {
                // A single target-free trigger has no ordering/setup decision.
                // Optional effects still go on the stack, per CR 603.5.
                getGame().getStack().add(ability);
                stackedTriggers++;
            }
        }

        @Override
        public boolean playTrigger(Card host, WrappedAbility wrapper, boolean mandatory) {
            throw new SecurityException(STATIC);
        }

        @Override
        public boolean chooseTargetsFor(SpellAbility ability) {
            throw new SecurityException(SETUP);
        }
    }

    @Override
    protected TriggerController attach(Game game, Player self, Player opponent, Actor actor) {
        TriggerController controller = new TriggerController(game, self, opponent, actor);
        self.dangerouslySetController(controller);
        return controller;
    }

    /** Direct-callback fixtures use the real, unmodified pinned card SVar. */
    protected WrappedAbility lifeTrigger(Card source, Player self) {
        Assert.assertEquals(source.getTriggers().size(), 1);
        Trigger trigger = source.getTriggers().get(0);
        SpellAbility effect = AbilityFactory.getAbility(source.getSVar("TrigGainLife"), source);
        effect.setActivatingPlayer(self);
        boolean optional = trigger.hasParam("OptionalDecider");
        effect.setOptionalTrigger(optional);
        return new WrappedAbility(trigger, effect, optional ? self : null);
    }

    protected static void denied(String message, Runnable action) {
        try {
            action.run();
        } catch (SecurityException expected) {
            Assert.assertEquals(expected.getMessage(), message);
            return;
        }
        // Outside the catch: an absent guard must fail this test.
        Assert.fail("TRIGGER_GUARD_MISSING: " + message);
    }

    protected WrappedAbility advanceToTrigger(Game game, TriggerController controller) {
        for (int steps = 0; steps < 40 && !game.isGameOver(); steps++) {
            game.getPhaseHandler().mainLoopStep();
            if (!game.getStack().isEmpty()
                    && game.getStack().peekAbility() instanceof WrappedAbility) {
                Assert.assertEquals(controller.stackedTriggers, 1);
                return (WrappedAbility) game.getStack().peekAbility();
            }
        }
        throw new AssertionError("real creature ETB did not put its life trigger on the stack");
    }

    private void finishMandatory(Game game, Player self, TriggerController controller) {
        for (int steps = 0; steps < 40 && self.getLife() == 20; steps++) {
            game.getPhaseHandler().mainLoopStep();
        }
        Assert.assertEquals(self.getLife(), 21);
        Assert.assertEquals(controller.resolvedMandatoryEffects, 1);
        Assert.assertEquals(controller.confirmations, 0,
                "mandatory wrapper bypasses optional confirmation in pinned Forge");
        Assert.assertTrue(game.getStack().isEmpty());
    }

    @Test(timeOut=120000)
    public void mandatoryLifeTriggerUsesRealStackAndBothActorsPriority() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        seedPrivateLibraries(self, other);
        addCardToZone("Soul Warden", self, ZoneType.Battlefield);
        addCardToZone("Llanowar Elves", self, ZoneType.Hand);
        Card forest = addCardToZone("Forest", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            Assert.assertNotEquals(a.process.pid(), b.process.pid());
            TriggerController ca = attach(game, self, other, a);
            TriggerController cb = attach(game, other, self, b);
            WrappedAbility wrapper = advanceToTrigger(game, ca);
            Assert.assertTrue(wrapper.isMandatory());
            Assert.assertEquals(self.getLife(), 20, "ETB does not resolve its trigger immediately");
            Assert.assertTrue(forest.isTapped(), "creature was genuinely paid for");
            Assert.assertEquals(countCardsWithName(game, "Llanowar Elves"), 1);
            finishMandatory(game, self, ca);
            Assert.assertTrue(ca.triggerPriorityCallbacks > 0 && cb.triggerPriorityCallbacks > 0,
                    "both external actors must each receive real priority with trigger on stack"
                    + " actorA=" + ca.triggerPriorityCallbacks + " actorB=" + cb.triggerPriorityCallbacks
                    + " totalA=" + ca.priorityCallbacks + " totalB=" + cb.priorityCallbacks);
            Assert.assertEquals(other.getLife(), 20);
        }
    }

    @Test(timeOut=120000)
    public void optionalLifeTriggerStacksThenStopsAtResolution() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        seedPrivateLibraries(self, other);
        addCardToZone("Soul's Attendant", self, ZoneType.Battlefield);
        addCardToZone("Llanowar Elves", self, ZoneType.Hand);
        addCardToZone("Forest", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            TriggerController ca = attach(game, self, other, a);
            TriggerController cb = attach(game, other, self, b);
            WrappedAbility wrapper = advanceToTrigger(game, ca);
            Assert.assertTrue(wrapper.isOptionalTrigger());
            Assert.assertSame(wrapper.getDecider(), self);
            Assert.assertEquals(ca.confirmations, 0, "no optional decision while stacking");
            Assert.assertEquals(self.getLife(), 20);
            denied(OPTIONAL, () -> {
                for (int steps = 0; steps < 40; steps++) game.getPhaseHandler().mainLoopStep();
            });
            Assert.assertTrue(ca.triggerPriorityCallbacks > 0 && cb.triggerPriorityCallbacks > 0,
                    "optional trigger must reach both actors before its resolution choice"
                    + " actorA=" + ca.triggerPriorityCallbacks + " actorB=" + cb.triggerPriorityCallbacks);
            Assert.assertEquals(ca.confirmations, 1);
            Assert.assertEquals(ca.resolvedMandatoryEffects, 0);
            Assert.assertEquals(self.getLife(), 20);
            Assert.assertEquals(other.getLife(), 20);
            // The unsupported game is aborted, never resumed as a declined option.
        }
    }

    @Test(timeOut=120000)
    public void mandatoryLifeTriggerSurvivesSourceLeavingBattlefield() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        seedPrivateLibraries(self, other);
        Card warden = addCardToZone("Soul Warden", self, ZoneType.Battlefield);
        addCardToZone("Llanowar Elves", self, ZoneType.Hand);
        addCardToZone("Forest", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            TriggerController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            advanceToTrigger(game, ca);
            game.getAction().moveToGraveyard(warden, null);
            Assert.assertEquals(countCardsWithName(game, "Soul Warden"), 0);
            finishMandatory(game, self, ca);
        }
    }

    @Test(timeOut=120000)
    public void triggerDecisionsAndMandatoryEffectsIgnoreOpponentSecrets() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        seedPrivateLibraries(self, other);
        Card warden = addCardToZone("Soul Warden", self, ZoneType.Battlefield);
        Card attendant = addCardToZone("Soul's Attendant", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            TriggerController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            String before = observation(self, other);
            var reply = a.ask(before);
            Assert.assertTrue(ca.confirmTrigger(lifeTrigger(warden, self)));
            denied(OPTIONAL, () -> ca.confirmTrigger(lifeTrigger(attendant, self)));
            lifeTrigger(warden, self).resolve();
            Assert.assertEquals(self.getLife(), 21);
            String publicAfter = observation(self, other);
            addCardToZone("Counterspell", other, ZoneType.Hand);
            addCardToZone("Shock", other, ZoneType.Library);
            Assert.assertEquals(observation(self, other), publicAfter);
            Assert.assertEquals(a.ask(publicAfter), a.ask(observation(self, other)));
            Assert.assertEquals(reply.get("intent"), a.ask(publicAfter).get("intent"));
            Assert.assertTrue(ca.confirmTrigger(lifeTrigger(warden, self)));
            denied(OPTIONAL, () -> ca.confirmTrigger(lifeTrigger(attendant, self)));
            lifeTrigger(warden, self).resolve();
            Assert.assertEquals(self.getLife(), 22, "same compulsory +1 in either secret world");
            Assert.assertEquals(ca.resolvedMandatoryEffects, 2);
            Assert.assertEquals(other.getLife(), 20);
        }
    }

    @Test(timeOut=120000)
    public void optionalTriggerCannotBeRelabeledOrExecutedWithoutChoice() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        Card attendant = addCardToZone("Soul's Attendant", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            TriggerController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            SpellAbility effect = lifeTrigger(attendant, self).getWrappedAbility();
            denied(OPTIONAL, () -> ca.playSpellAbilityNoStack(effect, false));
            WrappedAbility missingDecider = new WrappedAbility(effect.getTrigger(), effect, null);
            denied(SHAPE, () -> ca.confirmTrigger(missingDecider));
            effect.setOptionalTrigger(false);
            denied(SHAPE, () -> ca.playSpellAbilityNoStack(effect, false));
            denied(SHAPE, () -> ca.confirmTrigger(missingDecider));
            Assert.assertEquals(self.getLife(), 20);
            Assert.assertEquals(ca.resolvedMandatoryEffects, 0);
            Assert.assertTrue(game.getStack().isEmpty());
        }
    }

    @Test(timeOut=120000)
    public void noStackTriggerGuardRejectsCostsChainsAndForeignControllers() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        seedPrivateLibraries(self, other);
        Card warden = addCardToZone("Soul Warden", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            TriggerController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            int library = self.getCardsIn(ZoneType.Library).size();
            SpellAbility withCost = lifeTrigger(warden, self).getWrappedAbility();
            withCost.setPayCosts(new Cost("2", true));
            SpellAbility withChain = lifeTrigger(warden, self).getWrappedAbility();
            withChain.setSubAbility((AbilitySub) AbilityFactory.getAbility(
                    "DB$ Draw | Defined$ You | NumCards$ 1", warden));
            SpellAbility foreign = lifeTrigger(warden, self).getWrappedAbility();
            foreign.setActivatingPlayer(other);
            SpellAbility changedAmount = lifeTrigger(warden, self).getWrappedAbility();
            changedAmount.getMapParams().put("LifeAmount", "2");
            SpellAbility additional = lifeTrigger(warden, self).getWrappedAbility();
            additional.setAdditionalAbility("Unexpected", AbilityFactory.getAbility(
                    "DB$ Draw | Defined$ You | NumCards$ 1", warden));
            for (SpellAbility effect : Arrays.asList(withCost, withChain, foreign, changedAmount,
                    additional, lifeTrigger(warden, self), null)) {
                denied(SHAPE, () -> ca.playSpellAbilityNoStack(effect, false));
            }
            denied(SETUP, () -> ca.playSpellAbilityNoStack(
                    lifeTrigger(warden, self).getWrappedAbility(), true));
            Assert.assertEquals(self.getLife(), 20);
            Assert.assertEquals(other.getLife(), 20);
            Assert.assertEquals(self.getCardsIn(ZoneType.Library).size(), library);
            Assert.assertTrue(self.getCardsIn(ZoneType.Hand).isEmpty());
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(ca.resolvedMandatoryEffects, 0);
        }
    }

    @Test(timeOut=120000)
    public void multipleTriggerOrderingStopsBeforeAnyStackMutation() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        Card warden = addCardToZone("Soul Warden", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            TriggerController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            List<SpellAbility> two = Arrays.asList(lifeTrigger(warden, self), lifeTrigger(warden, self));
            denied(ORDER, () -> ca.orderSimultaneousSa(two));
            denied(ORDER, () -> ca.orderAndPlaySimultaneousSa(two));
            denied(ORDER, () -> ca.orderAndPlaySimultaneousSa(null));
            denied(SHAPE, () -> ca.orderAndPlaySimultaneousSa(Collections.singletonList(null)));
            Assert.assertTrue(ca.orderSimultaneousSa(Collections.emptyList()).isEmpty());
            ca.orderAndPlaySimultaneousSa(Collections.emptyList());
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertEquals(self.getLife(), 20);
            Assert.assertEquals(ca.stackedTriggers, 0);
        }
    }

    @Test(timeOut=120000)
    public void staticTriggerAndTargetSetupCannotReachInheritedAi() throws Exception {
        Game game = initAndCreateGame();
        Player self = game.getPlayers().get(1), other = game.getPlayers().get(0);
        Card warden = addCardToZone("Soul Warden", self, ZoneType.Battlefield);
        try (Actor a = new Actor(); Actor b = new Actor()) {
            TriggerController ca = attach(game, self, other, a);
            attach(game, other, self, b);
            WrappedAbility wrapper = lifeTrigger(warden, self);
            SpellAbility targeted = AbilityFactory.getAbility(
                    "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 1", warden);
            targeted.setActivatingPlayer(self);
            denied(STATIC, () -> ca.playTrigger(warden, wrapper, true));
            denied(STATIC, () -> ca.playTrigger(warden, wrapper, false));
            denied(SETUP, () -> ca.chooseTargetsFor(targeted));
            denied(SHAPE, () -> ca.playSpellAbilityNoStack(targeted, false));
            Assert.assertTrue(targeted.getTargets().isEmpty());
            Assert.assertEquals(self.getLife(), 20);
            Assert.assertEquals(other.getLife(), 20);
            Assert.assertTrue(game.getStack().isEmpty());
        }
    }
}
