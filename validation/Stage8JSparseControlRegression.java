package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/** Fail-closed regression for Stage 8J sparse, side-specific control. */
public final class Stage8JSparseControlRegression {
    private static RootActionTable.ScoredAction candidate() {
        Score score = new Score(10);
        Plan.Decision decision = new Plan.Decision(
                score, null, (Plan.SpellAbilityRef) null);
        return new RootActionTable.ScoredAction(decision, score);
    }

    private static Path request(long index, String identity) throws Exception {
        Path file = Files.createTempFile("stage8j-control-", ".tsv");
        String encoded = Base64.getUrlEncoder().encodeToString(
                identity.getBytes(StandardCharsets.UTF_8));
        Files.writeString(file, index + "\t" + encoded + "\n",
                StandardCharsets.UTF_8);
        return file;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("mode required");
        RootActionTable.ScoredAction forge = candidate();
        String identity = forge.action.completeActionIdentity();
        Path file = request(5L, identity);

        System.setProperty("forge.expert.stage8.control", "true");
        System.setProperty("forge.expert.stage8.control.file", file.toString());
        System.setProperty("forge.expert.stage8.control.require_forge_match", "false");
        System.setProperty("forge.expert.stage8.control.allow_missing", "true");
        System.setProperty("forge.expert.stage8.control.actor_prefix", "Ai(1)-");
        System.setProperty("forge.expert.stage7.dataset", "true");
        System.setProperty("forge.expert.stage8.capture", "true");

        try {
            if ("missing".equals(args[0])) {
                RootActionTable.ScoredAction selected =
                        ExpertPolicyControlAdapter.select(
                                4L, "Ai(2)-Forge", List.of(forge), forge);
                if (selected != forge) {
                    throw new AssertionError("missing sparse request did not fall through to Forge");
                }
                System.out.println("Stage 8J missing request safely remains Forge-controlled.");
            } else if ("valid".equals(args[0])) {
                RootActionTable.ScoredAction selected =
                        ExpertPolicyControlAdapter.select(
                                5L, "Ai(1)-Learned", List.of(forge), forge);
                if (selected != forge) {
                    throw new AssertionError("valid request did not return captured candidate");
                }
                System.out.println("Stage 8J valid controlled-side request accepted.");
            } else if ("wrong-actor".equals(args[0])) {
                try {
                    ExpertPolicyControlAdapter.select(
                            5L, "Ai(2)-Forge", List.of(forge), forge);
                    throw new AssertionError("wrong actor request was accepted");
                } catch (IllegalStateException expected) {
                    if (!expected.getMessage().contains("non-controlled actor")) {
                        throw expected;
                    }
                    System.out.println("Stage 8J wrong-actor request rejected fail-closed.");
                }
            } else {
                throw new IllegalArgumentException("unknown mode");
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
