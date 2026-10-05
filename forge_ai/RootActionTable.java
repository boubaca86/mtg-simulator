package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Scores of the root recipes actually enumerated in ONE sampled world.
 * Continuations may still differ; this enforces consistency only at the root.
 */
public final class RootActionTable {
    public static final class ScoredAction {
        public final Plan.Decision action;
        public final Score score;

        ScoredAction(Plan.Decision action, Score score) {
            this.action = action;
            this.score = score;
        }
    }

    public static final class Result {
        public final ScoredAction best;
        public final int completeActions;
        public final int distinctActions;
        public final boolean differentWorldPreferences;

        Result(ScoredAction best, int completeActions, int distinctActions, boolean differentWorldPreferences) {
            this.best = best;
            this.completeActions = completeActions;
            this.distinctActions = distinctActions;
            this.differentWorldPreferences = differentWorldPreferences;
        }
    }

    private final Map<String, ScoredAction> actions = new LinkedHashMap<>();

    public void record(Plan.Decision action, Score score) {
        if (action == null || action.saRef == null || action.prevDecision != null) {
            throw new IllegalArgumentException("Expected one detached root action");
        }
        if (score.value == Integer.MIN_VALUE) return; // failed simulation is not evidence
        String identity = action.completeActionIdentity();
        ScoredAction old = actions.get(identity);
        if (old == null || score.value > old.score.value) {
            actions.put(identity, new ScoredAction(action, score));
        }
    }

    private ScoredAction best() {
        ScoredAction best = null;
        for (ScoredAction entry : actions.values()) {
            if (best == null || entry.score.value > best.score.value) best = entry;
        }
        return best;
    }

    /** Average the SAME recipe over every required world, then choose its maximum.
     * Never average per-world maxima, or divide by just the worlds where an action exists.
     * Linked insertion order gives reproducible tie-breaking without hidden-state sorting.
     */
    public static Result aggregate(List<RootActionTable> worlds, int expectedWorlds) {
        if (expectedWorlds < 1 || worlds.size() != expectedWorlds || worlds.contains(null)) {
            throw new IllegalArgumentException("Incomplete information-set world batch");
        }
        Set<String> identities = new LinkedHashSet<>();
        String firstPreference = null;
        boolean differentPreferences = false;
        for (RootActionTable world : worlds) {
            identities.addAll(world.actions.keySet());
            ScoredAction preferred = world.best();
            if (preferred != null) {
                String identity = preferred.action.completeActionIdentity();
                if (firstPreference == null) firstPreference = identity;
                else if (!firstPreference.equals(identity)) differentPreferences = true;
            }
        }
        ScoredAction best = null;
        int complete = 0;
        for (String identity : identities) {
            long value = 0L;
            long available = 0L;
            Plan.Decision action = null;
            int covered = 0;
            for (RootActionTable world : worlds) {
                ScoredAction entry = world.actions.get(identity);
                if (entry == null) break;
                if (action == null) action = entry.action;
                value += entry.score.value;
                available += entry.score.availableValue;
                covered++;
            }
            if (covered != expectedWorlds) continue;
            complete++;
            Score mean = new Score((int) (value / expectedWorlds), (int) (available / expectedWorlds));
            if (best == null || mean.value > best.score.value) best = new ScoredAction(action, mean);
        }
        return new Result(best, complete, identities.size(), differentPreferences);
    }
}
