from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch_adapter(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        """    private static String requestSha256;
    private static final Set<Long> consumed = new LinkedHashSet<>();
""",
        """    private static String requestSha256;
    private static final Set<Long> consumed = new LinkedHashSet<>();
    private static final Map<Long, String> expectedForgeRequests = new LinkedHashMap<>();
    private static final Map<Long, RootActionTable.ScoredAction> armedRequests = new LinkedHashMap<>();
    private static final Map<Long, String> armedForgeIdentities = new LinkedHashMap<>();
    private static final Map<Long, String> armedActors = new LinkedHashMap<>();

    private static boolean returnBoundaryEnabled() {
        return Boolean.getBoolean("forge.expert.stage8.control.return_boundary");
    }
""",
        "Stage 8K adapter state",
    )

    text = replace_once(
        text,
        """                String[] parts = line.split("\\t", -1);
                if (parts.length != 2) throw new IllegalArgumentException("Invalid Stage 8I request line");
                long index = Long.parseLong(parts[0]);
                if (index < 0) throw new IllegalArgumentException("Negative Stage 8I decision index");
                String identity = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                if (identity.isEmpty()) throw new IllegalArgumentException("Empty Stage 8I action identity");
                if (parsed.put(index, identity) != null) {
""",
        """                String[] parts = line.split("\\t", -1);
                int expectedFields = returnBoundaryEnabled() ? 3 : 2;
                if (parts.length != expectedFields) {
                    throw new IllegalArgumentException("Invalid Stage 8 control request line");
                }
                long index = Long.parseLong(parts[0]);
                if (index < 0) throw new IllegalArgumentException("Negative Stage 8I decision index");
                int requestedField = returnBoundaryEnabled() ? 2 : 1;
                String identity = new String(Base64.getUrlDecoder().decode(parts[requestedField]), StandardCharsets.UTF_8);
                if (identity.isEmpty()) throw new IllegalArgumentException("Empty Stage 8I action identity");
                if (returnBoundaryEnabled()) {
                    String expectedForge = new String(
                            Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    if (expectedForge.isEmpty()) {
                        throw new IllegalArgumentException("Empty Stage 8K expected Forge identity");
                    }
                    expectedForgeRequests.put(index, expectedForge);
                }
                if (parsed.put(index, identity) != null) {
""",
        "Stage 8K request format",
    )

    marker = """        boolean requireForgeMatch = Boolean.parseBoolean(
                System.getProperty("forge.expert.stage8.control.require_forge_match", "true"));
        if (requireForgeMatch && !sameAsForge) {
            throw new IllegalStateException("Stage 8I replay request differs from Forge selection");
        }

        StringBuilder out = new StringBuilder(1024);
"""
    replacement = """        boolean requireForgeMatch = Boolean.parseBoolean(
                System.getProperty("forge.expert.stage8.control.require_forge_match", "true"));
        if (requireForgeMatch && !sameAsForge) {
            throw new IllegalStateException("Stage 8I replay request differs from Forge selection");
        }

        if (returnBoundaryEnabled()) {
            String expectedForge = expectedForgeRequests.get(decisionIndex);
            if (expectedForge == null || !expectedForge.equals(forgeIdentity)) {
                throw new IllegalStateException(
                        "Stage 8K live Forge selection differs from predeclared baseline");
            }
            if (sameAsForge) {
                throw new IllegalStateException(
                        "Stage 8K intervention must differ from Forge at the armed decision");
            }
            armedRequests.put(decisionIndex, chosen);
            armedForgeIdentities.put(decisionIndex, forgeIdentity);
            armedActors.put(decisionIndex, actorName == null ? "" : actorName);

            StringBuilder armedOut = new StringBuilder(1024);
            armedOut.append('{');
            armedOut.append("\"schema_version\":\"stage8k-return-boundary-armed-v1\",");
            armedOut.append("\"decision_index\":").append(decisionIndex).append(',');
            armedOut.append("\"acting_player_name\":\"")
                    .append(jsonEscape(actorName == null ? "" : actorName)).append("\",");
            armedOut.append("\"requested_action\":\"").append(jsonEscape(requested)).append("\",");
            armedOut.append("\"forge_selected_action\":\"")
                    .append(jsonEscape(forgeIdentity)).append("\",");
            armedOut.append("\"requested_action_in_candidates\":true,");
            armedOut.append("\"early_substitution\":false,");
            armedOut.append("\"forge_referee\":true,");
            armedOut.append("\"promotion_allowed\":false");
            System.out.println("EXPERT_STAGE8K_CONTROL_ARMED: " + armedOut.append('}'));
            return forgeSelected;
        }

        StringBuilder out = new StringBuilder(1024);
"""
    text = replace_once(text, marker, replacement, "Stage 8K arm-before-return boundary")

    insert_before = """    private static String hex(byte[] value) {
"""
    methods = r'''    public static synchronized Plan applyAfterForgePlan(Plan forgePlan) {
        if (!enabled() || !returnBoundaryEnabled() || forgePlan == null
                || forgePlan.getDecisions().isEmpty()) {
            return forgePlan;
        }
        if (forgePlan.getDecisions().size() != 1) {
            throw new IllegalStateException("Stage 8K requires a one-action Forge root plan");
        }

        Plan.Decision forgeRoot = forgePlan.getDecisions().get(0);
        long decisionIndex = forgeRoot.expertCaptureDecisionIndex;
        RootActionTable.ScoredAction chosen = armedRequests.remove(decisionIndex);
        if (chosen == null) {
            return forgePlan;
        }
        String expectedForge = armedForgeIdentities.remove(decisionIndex);
        String actorName = armedActors.remove(decisionIndex);
        String liveForge = forgeRoot.completeActionIdentity();
        if (expectedForge == null || !expectedForge.equals(liveForge)) {
            throw new IllegalStateException(
                    "Stage 8K final Forge plan differs from armed Forge action");
        }

        Plan.Decision replacement = cloneDecision(chosen.action, decisionIndex);
        java.util.ArrayList<Plan.Decision> sequence = new java.util.ArrayList<>();
        sequence.add(replacement);
        Plan result = new Plan(sequence, chosen.score);

        String requested = replacement.completeActionIdentity();
        StringBuilder out = new StringBuilder(1024);
        out.append('{');
        out.append(""schema_version":"stage8k-return-boundary-selection-v1",");
        out.append(""decision_index":").append(decisionIndex).append(',');
        out.append(""acting_player_name":"")
                .append(jsonEscape(actorName == null ? "" : actorName)).append("",");
        out.append(""requested_action":"").append(jsonEscape(requested)).append("",");
        out.append(""forge_selected_action":"").append(jsonEscape(liveForge)).append("",");
        out.append(""substitution_boundary":"post-forge-plan-pre-return",");
        out.append(""forge_search_unchanged":true,");
        out.append(""forge_phase_deferral_unchanged":true,");
        out.append(""forge_referee":true,");
        out.append(""promotion_allowed":false");
        System.out.println("EXPERT_STAGE8K_RETURN_BOUNDARY_SELECTION: " + out.append('}'));
        return result;
    }

    private static Plan.Decision cloneDecision(Plan.Decision source, long decisionIndex) {
        if (source == null || source.saRef == null) {
            throw new IllegalStateException("Stage 8K requested action lacks a root SpellAbility");
        }
        Plan.Decision copy = new Plan.Decision(source.initialScore, null, source.saRef);
        copy.xMana = source.xMana;
        copy.targets = source.targets;
        copy.modes = source.modes == null ? null : source.modes.clone();
        copy.modesStr = source.modesStr;
        copy.choices = source.choices == null
                ? null : new java.util.ArrayList<>(source.choices);
        copy.expertCaptureDecisionIndex = decisionIndex;
        return copy;
    }

'''
    text = replace_once(
        text,
        insert_before,
        methods + insert_before,
        "Stage 8K return-boundary apply method",
    )

    path.write_text(text, encoding="utf-8")


def patch_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        """        printPlan(bestPlan, "Current phase (" + currentPhase + ")");
        plan = bestPlan;
""",
        """        printPlan(bestPlan, "Current phase (" + currentPhase + ")");
        plan = bestPlan;
        if (Boolean.getBoolean("forge.expert.stage8.control.return_boundary")) {
            // Apply learned control only after Forge has completed its own
            // current-vs-later-phase decision. This prevents an intervention
            // from changing Forge's phase-deferral heuristic before the true
            // SpellAbility return boundary.
            plan = ExpertPolicyControlAdapter.applyAfterForgePlan(plan);
        }
""",
        "Stage 8K post-Forge-plan substitution hook",
    )
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--adapter", required=True, type=Path)
    parser.add_argument("--spell-picker", required=True, type=Path)
    args = parser.parse_args()
    patch_adapter(args.adapter)
    patch_picker(args.spell_picker)
    print("Patched Stage 8K return-boundary learned control into Forge")


if __name__ == "__main__":
    main()
