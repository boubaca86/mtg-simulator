package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Regression for Stage 8K: arm during search, substitute only after Forge finalizes its plan. */
public final class Stage8KReturnBoundaryRegression {
    private static RootActionTable.ScoredAction candidate(
            List<SpellAbility> abilities, int index, int scoreValue) {
        Score score = new Score(scoreValue);
        Plan.Decision decision = new Plan.Decision(
                score, null, new Plan.SpellAbilityRef(abilities, index));
        return new RootActionTable.ScoredAction(decision, score);
    }

    private static Path request(
            long index, String expectedForge, String requested) throws Exception {
        Path file = Files.createTempFile("stage8k-control-", ".tsv");
        String expectedEncoded = Base64.getUrlEncoder().encodeToString(
                expectedForge.getBytes(StandardCharsets.UTF_8));
        String requestedEncoded = Base64.getUrlEncoder().encodeToString(
                requested.getBytes(StandardCharsets.UTF_8));
        Files.writeString(
                file,
                index + "\t" + expectedEncoded + "\t" + requestedEncoded + "\n",
                StandardCharsets.UTF_8);
        return file;
    }

    private static List<SpellAbility> abilities() {
        Card a = new Card(101, null);
        a.setName("Forge Root");
        Card b = new Card(102, null);
        b.setName("Learned Root");
        return List.of(
                new SpellAbility.EmptySa(a, null),
                new SpellAbility.EmptySa(b, null));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("mode required");

        List<SpellAbility> abilities = abilities();
        RootActionTable.ScoredAction forge = candidate(abilities, 0, 20);
        RootActionTable.ScoredAction learned = candidate(abilities, 1, 15);
        String forgeIdentity = forge.action.completeActionIdentity();
        String learnedIdentity = learned.action.completeActionIdentity();

        String expected = "wrong-forge".equals(args[0])
                ? forgeIdentity + "-drift"
                : forgeIdentity;
        long requestIndex = "missing".equals(args[0]) ? 6L : 5L;
        Path file = request(requestIndex, expected, learnedIdentity);

        System.setProperty("forge.expert.stage8.control", "true");
        System.setProperty("forge.expert.stage8.control.return_boundary", "true");
        System.setProperty("forge.expert.stage8.control.file", file.toString());
        System.setProperty("forge.expert.stage8.control.require_forge_match", "false");
        System.setProperty("forge.expert.stage8.control.allow_missing", "true");
        System.setProperty("forge.expert.stage8.control.actor_prefix", "Ai(1)-");
        System.setProperty("forge.expert.stage7.dataset", "true");
        System.setProperty("forge.expert.stage8.capture", "true");

        try {
            if ("wrong-forge".equals(args[0])) {
                try {
                    ExpertPolicyControlAdapter.select(
                            5L, "Ai(1)-Fixture", List.of(forge, learned), forge);
                    throw new AssertionError("Stage 8K accepted Forge-selection drift");
                } catch (IllegalStateException expectedFailure) {
                    if (!expectedFailure.getMessage().contains("predeclared baseline")) {
                        throw expectedFailure;
                    }
                    System.out.println("Stage 8K expected-Forge mismatch rejected fail-closed.");
                }
                return;
            }

            RootActionTable.ScoredAction duringSearch =
                    ExpertPolicyControlAdapter.select(
                            5L, "Ai(1)-Fixture", List.of(forge, learned), forge);
            if (duringSearch != forge) {
                throw new AssertionError("Stage 8K substituted before Forge finalized its plan");
            }

            forge.action.expertCaptureDecisionIndex = 5L;
            ArrayList<Plan.Decision> sequence = new ArrayList<>();
            sequence.add(forge.action);
            Plan forgePlan = new Plan(sequence, forge.score);
            Plan finalPlan = ExpertPolicyControlAdapter.applyAfterForgePlan(forgePlan);

            if ("missing".equals(args[0])) {
                if (finalPlan != forgePlan) {
                    throw new AssertionError("missing Stage 8K request changed the Forge plan");
                }
                System.out.println("Stage 8K missing request remains Forge-controlled.");
                return;
            }

            if (!"valid".equals(args[0])) {
                throw new IllegalArgumentException("unknown mode");
            }
            String finalIdentity = finalPlan.getDecisions().get(0).completeActionIdentity();
            if (!learnedIdentity.equals(finalIdentity)) {
                throw new AssertionError("Stage 8K did not install the requested legal action");
            }
            if (!forgeIdentity.equals(forgePlan.getDecisions().get(0).completeActionIdentity())) {
                throw new AssertionError("Stage 8K mutated the original Forge plan");
            }
            if (finalPlan.getDecisions().get(0).expertCaptureDecisionIndex != 5L) {
                throw new AssertionError("Stage 8K lost capture binding");
            }
            System.out.println(
                    "Stage 8K valid request armed during search and applied only at return boundary.");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
