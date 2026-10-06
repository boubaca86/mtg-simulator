package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/** Regression for Stage 8I's fail-closed complete-action boundary. */
public final class Stage8IBoundedControlRegression {
    public static void main(String[] args) throws Exception {
        boolean invalid = args.length > 0 && "invalid".equals(args[0]);

        Score score = new Score(10);
        Plan.Decision decision = new Plan.Decision(
                score, null, (Plan.SpellAbilityRef) null);
        RootActionTable.ScoredAction candidate =
                new RootActionTable.ScoredAction(decision, score);
        String identity = decision.completeActionIdentity();
        String request = invalid ? identity + "|not-captured" : identity;

        Path file = Files.createTempFile("stage8i-control-", ".tsv");
        String encoded = Base64.getUrlEncoder().encodeToString(
                request.getBytes(StandardCharsets.UTF_8));
        Files.writeString(file, "0\t" + encoded + "\n", StandardCharsets.UTF_8);

        System.setProperty("forge.expert.stage8.control", "true");
        System.setProperty("forge.expert.stage8.control.file", file.toString());
        System.setProperty("forge.expert.stage8.control.require_forge_match", "true");
        System.setProperty("forge.expert.stage7.dataset", "true");
        System.setProperty("forge.expert.stage8.capture", "true");

        try {
            RootActionTable.ScoredAction selected = ExpertPolicyControlAdapter.select(
                    0L, List.of(candidate), candidate);
            if (invalid) {
                throw new AssertionError("Stage 8I accepted an uncaptured action");
            }
            if (selected != candidate) {
                throw new AssertionError("Stage 8I did not return the exact captured candidate");
            }
            System.out.println("Stage 8I valid replay request accepted exactly.");
        } catch (IllegalStateException expected) {
            if (!invalid) throw expected;
            if (!expected.getMessage().contains("not in the captured legal candidate set")) {
                throw expected;
            }
            System.out.println("Stage 8I invalid request rejected fail-closed.");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
