from __future__ import annotations

import argparse
from pathlib import Path


ADAPTER_JAVA = r'''package forge.ai.simulation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 8I bounded complete-action control adapter.
 *
 * The adapter may select only an exact complete action already produced by the
 * fixed-root information-set search. It never creates an action, edits a target,
 * changes a cost/mode/X/choice, or bypasses Forge legality/rules handling.
 *
 * Stage 8I CI runs in require-forge-match mode: the external request must equal
 * Forge's own selected complete action. This proves the control path without
 * allowing the learned model to change gameplay.
 */
public final class ExpertPolicyControlAdapter {
    private ExpertPolicyControlAdapter() {}

    private static Map<Long, String> requests;
    private static String requestSha256;
    private static final Set<Long> consumed = new LinkedHashSet<>();

    private static boolean enabled() {
        return Boolean.getBoolean("forge.expert.stage8.control");
    }

    private static synchronized void load() {
        if (requests != null) return;
        if (!enabled()) throw new IllegalStateException("Stage 8I control is not enabled");
        if (!Boolean.getBoolean("forge.expert.stage7.dataset")
                || !Boolean.getBoolean("forge.expert.stage8.capture")) {
            throw new IllegalStateException("Stage 8I requires Stage 7/8 public capture");
        }
        String file = System.getProperty("forge.expert.stage8.control.file", "");
        if (file.isEmpty()) throw new IllegalStateException("Stage 8I request file is required");
        try {
            Path path = Path.of(file);
            byte[] raw = Files.readAllBytes(path);
            requestSha256 = hex(MessageDigest.getInstance("SHA-256").digest(raw));
            LinkedHashMap<Long, String> parsed = new LinkedHashMap<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\t", -1);
                if (parts.length != 2) throw new IllegalArgumentException("Invalid Stage 8I request line");
                long index = Long.parseLong(parts[0]);
                if (index < 0) throw new IllegalArgumentException("Negative Stage 8I decision index");
                String identity = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                if (identity.isEmpty()) throw new IllegalArgumentException("Empty Stage 8I action identity");
                if (parsed.put(index, identity) != null) {
                    throw new IllegalArgumentException("Duplicate Stage 8I decision index: " + index);
                }
            }
            if (parsed.isEmpty()) throw new IllegalArgumentException("Empty Stage 8I request file");
            requests = parsed;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load Stage 8I request file", ex);
        }
    }

    public static synchronized RootActionTable.ScoredAction select(long decisionIndex,
            List<RootActionTable.ScoredAction> candidates,
            RootActionTable.ScoredAction forgeSelected) {
        if (!enabled()) return forgeSelected;
        load();
        if (decisionIndex < 0 || forgeSelected == null || candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Invalid Stage 8I selection boundary");
        }
        if (!consumed.add(decisionIndex)) {
            throw new IllegalStateException("Stage 8I request consumed twice: " + decisionIndex);
        }

        String requested = requests.get(decisionIndex);
        if (requested == null) {
            throw new IllegalStateException("Missing Stage 8I request for decision " + decisionIndex);
        }

        LinkedHashMap<String, RootActionTable.ScoredAction> legal = new LinkedHashMap<>();
        for (RootActionTable.ScoredAction candidate : candidates) {
            String identity = candidate.action.completeActionIdentity();
            RootActionTable.ScoredAction prior = legal.putIfAbsent(identity, candidate);
            if (prior != null && (prior.score.value != candidate.score.value
                    || prior.score.availableValue != candidate.score.availableValue)) {
                throw new IllegalStateException("Duplicate Stage 8I candidate has inconsistent score");
            }
        }

        RootActionTable.ScoredAction chosen = legal.get(requested);
        if (chosen == null) {
            throw new IllegalStateException("Stage 8I requested action is not in the captured legal candidate set");
        }

        String forgeIdentity = forgeSelected.action.completeActionIdentity();
        boolean sameAsForge = requested.equals(forgeIdentity);
        boolean requireForgeMatch = Boolean.parseBoolean(
                System.getProperty("forge.expert.stage8.control.require_forge_match", "true"));
        if (requireForgeMatch && !sameAsForge) {
            throw new IllegalStateException("Stage 8I replay request differs from Forge selection");
        }

        StringBuilder out = new StringBuilder(1024);
        out.append('{');
        out.append("\"schema_version\":\"stage8i-control-selection-v1\",");
        out.append("\"decision_index\":").append(decisionIndex).append(',');
        out.append("\"requested_action\":\"").append(jsonEscape(requested)).append("\",");
        out.append("\"forge_selected_action\":\"").append(jsonEscape(forgeIdentity)).append("\",");
        out.append("\"requested_action_in_candidates\":true,");
        out.append("\"same_as_forge\":").append(sameAsForge).append(',');
        out.append("\"candidate_count\":").append(legal.size()).append(',');
        out.append("\"request_count\":").append(requests.size()).append(',');
        out.append("\"request_file_sha256\":\"").append(requestSha256).append("\",");
        out.append("\"forge_referee\":true,");
        out.append("\"promotion_allowed\":false");
        System.out.println("EXPERT_STAGE8I_CONTROL_SELECTION: " + out.append('}'));
        return chosen;
    }

    private static String hex(byte[] value) {
        StringBuilder out = new StringBuilder(value.length * 2);
        for (byte b : value) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
'''


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    old = """                controller.useFixedRootAction(bestAction);
"""
    new = """                if (Boolean.getBoolean("forge.expert.stage8.control")) {
                    bestAction = ExpertPolicyControlAdapter.select(
                            stage7DecisionIndex, stage8Candidates, bestAction);
                    // In Stage 8I require-forge-match mode this is identical to
                    // Forge's original score. Keep the selected action's score
                    // attached to the installed plan for audit correctness.
                    bestAverage = bestAction.score;
                }
                controller.useFixedRootAction(bestAction);
"""
    text = replace_once(text, old, new, "bounded control selection hook")
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--forge-ai-java", required=True, type=Path)
    parser.add_argument("--spell-picker", required=True, type=Path)
    args = parser.parse_args()

    target = args.forge_ai_java / "forge" / "ai" / "simulation" / "ExpertPolicyControlAdapter.java"
    if target.exists():
        raise RuntimeError(f"Refusing to overwrite existing Forge source: {target}")
    target.write_text(ADAPTER_JAVA, encoding="utf-8")
    patch_picker(args.spell_picker)
    print("Patched Stage 8I bounded complete-action control adapter into Forge")


if __name__ == "__main__":
    main()
