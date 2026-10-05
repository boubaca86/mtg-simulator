package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;
import java.lang.reflect.Field;
import java.util.List;

/** Executed against the real patched Forge jar, without changing game rules. */
public final class Stage6RootIdentityRegression {
    private static final Score SCORE = new Score(0);

    private static Card card(int id, String name) {
        Card card = new Card(id, null);
        card.setName(name);
        return card;
    }

    private static Plan.SpellAbilityRef ability(int index) {
        SpellAbility a = new SpellAbility.EmptySa(card(1, "Fixture"), null);
        return new Plan.SpellAbilityRef(List.of(a, a), index);
    }

    private static MultiTargetSelector.Targets target(final String description) {
        return new MultiTargetSelector.Targets() {
            @Override public String toString() { return description; }
        };
    }

    private static SimulationController controller(Plan.Decision tail) throws Exception {
        SimulationController c = new SimulationController(SCORE);
        Field f = SimulationController.class.getDeclaredField("bestSequence");
        f.setAccessible(true);
        f.set(c, tail);
        return c;
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static Plan.Decision recipe(String target) {
        Plan.Decision root = new Plan.Decision(SCORE, null, ability(0));
        root.targets = target(target);
        return root;
    }

    private static void testFixedActionAggregation() {
        Plan.Decision a = recipe("same-name creature (101)");
        Plan.Decision b = recipe("same-name creature (102)");
        Plan.Decision missing = recipe("available in only two worlds");
        RootActionTable w0 = new RootActionTable();
        RootActionTable w1 = new RootActionTable();
        RootActionTable w2 = new RootActionTable();
        // World 0 wants A, worlds 1/2 want B. Mean(max) is 73, but no recipe
        // achieves it. The correct result is B at 60, with available value 50.
        w0.record(a, new Score(100));
        w1.record(a, new Score(-10));
        w2.record(a, new Score(0));
        for (RootActionTable world : List.of(w0, w1, w2)) world.record(b, new Score(60, 50));
        w0.record(missing, new Score(1000));
        w1.record(missing, new Score(1000));
        w2.record(missing, new Score(Integer.MIN_VALUE));
        // Repeated evaluation in one world must not count as another sample.
        w0.record(missing, new Score(1001));
        RootActionTable.Result result = RootActionTable.aggregate(List.of(w0, w1, w2), 3);
        require(result.best.action.completeActionIdentity().equals(b.completeActionIdentity()),
                "selected a per-world optimum or fused same-name targets");
        require(result.best.score.value == 60 && result.best.score.availableValue == 50,
                "averaged world optima, dropped a poor world, or used a representative score");
        require(result.completeActions == 2 && result.distinctActions == 3,
                "missing or failed world contributed to an aggregate");
        require(result.differentWorldPreferences, "adversarial disagreement was not exercised");

        SimulationController execution = new SimulationController(SCORE, 0);
        execution.useFixedRootAction(result.best);
        Plan plan = execution.getBestPlan();
        require(plan.getDecisions().size() == 1, "installed a sampled future line");
        require(plan.selectNextDecision().completeActionIdentity().equals(b.completeActionIdentity()),
                "executable action differs from averaged action");
        require(plan.getFinalScore().value == 60, "phase comparison uses a single-world score");

        require(RootActionTable.aggregate(List.of(w0, w1, new RootActionTable()), 3).best == null,
                "partial-world averaging was allowed");
        boolean rejected = false;
        try { RootActionTable.aggregate(List.of(w0, w1), 3); }
        catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "incomplete world batch was allowed");
        rejected = false;
        try { RootActionTable.aggregate(java.util.Arrays.asList(w0, null, w2), 3); }
        catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "null world was allowed");
    }

    private static void testCollectionBeforeUnwind() {
        Score initial = new Score(10);
        SimulationController c = new SimulationController(initial);
        c.collectRootActions();
        SpellAbility sa = new SpellAbility.EmptySa(card(1, "Fixture"), null);
        c.evaluateSpellAbility(List.of(sa), 0);
        c.evaluateChosenModes(new int[] {1}, "mode");
        c.getLastDecision().xMana = 7; // announceX may run after modes are chosen
        c.evaluateTargetChoices(sa, target("public target (22)"));
        c.evaluateCardChoice(card(3, "Chosen card"));
        c.recordRootAction(new Score(5)); // lower than original, still essential evidence
        for (int i = 0; i < 4; i++) c.doneEvaluating(new Score(5));
        require(c.getBestRootActionIdentity() == null, "fixture should not improve the original score");
        RootActionTable.ScoredAction recorded = RootActionTable.aggregate(List.of(c.getRootActionTable()), 1).best;
        require(recorded != null && recorded.score.value == 5, "lost a below-baseline world's action");
        String identity = recorded.action.completeActionIdentity();
        require(identity.contains("|targets=public target (22)") && identity.contains("|modes=[1]")
                && identity.contains("|x=7") && identity.contains("Chosen card"),
                "collection happened after choice nodes were unwound, or dropped child X");
        require(recorded.action.prevDecision == null, "snapshot is still linked to simulation nodes");
        recorded.action.modes[0] = 1;
        SimulationController execution = new SimulationController(initial, 0);
        execution.useFixedRootAction(recorded);
        recorded.action.modes[0] = 9;
        require(execution.getBestRootActionIdentity().equals(identity), "installed recipe aliases mutable modes");
    }

    public static void main(String[] args) throws Exception {
        // PhaseType initializes localized labels when the first ability is made.
        // The standalone regression does not boot FModel, so initialize this
        // required Forge service explicitly before constructing fixture abilities.
        Localizer.getInstance().initialize("en-US", "forge-src/forge-gui/res/languages");
        testFixedActionAggregation();
        testCollectionBeforeUnwind();
        Plan.Decision root = new Plan.Decision(SCORE, null, ability(0));
        root.xMana = 4;
        Plan.Decision modes = new Plan.Decision(SCORE, root, new int[] {1, 2}, "fixture modes");
        Plan.Decision targetA = new Plan.Decision(SCORE, modes, target("target-A"));
        Plan.Decision choice = new Plan.Decision(SCORE, targetA, card(2, "Chosen card"));
        SimulationController c = controller(choice);
        String identity = c.getBestRootActionIdentity();
        require(identity.contains("|targets=target-A"), "root audit dropped the target child node");
        require(identity.contains("|modes=[1, 2]"), "root audit dropped modes");
        require(identity.contains("|x=4"), "root audit dropped X");
        require(identity.contains("Chosen card"), "root audit dropped explicit choice");
        require(identity.equals(c.getBestRootActionIdentity()), "repeated reads changed identity");
        require(root.targets == null && root.modes == null && root.choices == null,
                "auditing mutated Forge's executable plan");

        Plan.Decision targetB = new Plan.Decision(SCORE, modes, target("target-B"));
        require(!controller(targetA).getBestRootActionIdentity().equals(
                controller(targetB).getBestRootActionIdentity()), "different targets fused");
        Plan.Decision otherModes = new Plan.Decision(SCORE, root, new int[] {0}, "other mode");
        require(!controller(modes).getBestRootActionIdentity().equals(
                controller(otherModes).getBestRootActionIdentity()), "different modes fused");

        Plan.Decision future = new Plan.Decision(SCORE, choice, ability(1));
        Plan.Decision futureTarget = new Plan.Decision(SCORE, future, target("future-only"));
        require(identity.equals(controller(futureTarget).getBestRootActionIdentity()),
                "future rollout spell contaminated root identity");
        Plan.Decision otherSource = new Plan.Decision(SCORE, null, ability(1));
        require(!controller(root).getBestRootActionIdentity().equals(
                controller(otherSource).getBestRootActionIdentity()), "identical-text sources fused");

        Plan plan = c.getBestPlan();
        require(plan.getDecisions().get(0).choices.size() == 1,
                "audit duplicated a choice before plan construction");
        require(new SimulationController(SCORE).getBestRootActionIdentity() == null,
                "no plan should have no identity");
        System.out.println("Stage 6 root identity regression: PASS (targets, modes, X, choices, source, isolation, immutability)");
        System.out.println("Stage 6 fixed-root aggregation: PASS (world disagreement, poor worlds, missing worlds, exact plan, mean score)");
    }
}
