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

    public static void main(String[] args) throws Exception {
        // PhaseType initializes localized labels when the first ability is made.
        // The standalone regression does not boot FModel, so initialize this
        // required Forge service explicitly before constructing fixture abilities.
        Localizer.getInstance().initialize("en-US", "forge-src/forge-gui/res/languages");
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
    }
}
