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

    old = """    public static synchronized RootActionTable.ScoredAction select(long decisionIndex,
            List<RootActionTable.ScoredAction> candidates,
            RootActionTable.ScoredAction forgeSelected) {
        if (!enabled()) return forgeSelected;
"""
    new = """    public static RootActionTable.ScoredAction select(long decisionIndex,
            List<RootActionTable.ScoredAction> candidates,
            RootActionTable.ScoredAction forgeSelected) {
        return select(decisionIndex, "", candidates, forgeSelected);
    }

    public static synchronized RootActionTable.ScoredAction select(long decisionIndex,
            String actorName,
            List<RootActionTable.ScoredAction> candidates,
            RootActionTable.ScoredAction forgeSelected) {
        if (!enabled()) return forgeSelected;
"""
    text = replace_once(text, old, new, "Stage 8J actor-aware selector overload")

    old = """        if (!consumed.add(decisionIndex)) {
            throw new IllegalStateException("Stage 8I request consumed twice: " + decisionIndex);
        }

        String requested = requests.get(decisionIndex);
        if (requested == null) {
            throw new IllegalStateException("Missing Stage 8I request for decision " + decisionIndex);
        }

        LinkedHashMap<String, RootActionTable.ScoredAction> legal = new LinkedHashMap<>();
"""
    new = """        String requested = requests.get(decisionIndex);
        if (requested == null) {
            if (Boolean.getBoolean("forge.expert.stage8.control.allow_missing")) {
                return forgeSelected;
            }
            throw new IllegalStateException("Missing Stage 8I request for decision " + decisionIndex);
        }

        String actorPrefix = System.getProperty("forge.expert.stage8.control.actor_prefix", "");
        if (!actorPrefix.isEmpty()
                && (actorName == null || !actorName.startsWith(actorPrefix))) {
            throw new IllegalStateException("Stage 8J request reached a non-controlled actor");
        }
        if (!consumed.add(decisionIndex)) {
            throw new IllegalStateException("Stage 8I request consumed twice: " + decisionIndex);
        }

        LinkedHashMap<String, RootActionTable.ScoredAction> legal = new LinkedHashMap<>();
"""
    text = replace_once(text, old, new, "Stage 8J sparse request boundary")

    old = """        out.append("\"decision_index\":").append(decisionIndex).append(',');
        out.append("\"requested_action\":\"").append(jsonEscape(requested)).append("\",");
"""
    new = """        out.append("\"decision_index\":").append(decisionIndex).append(',');
        out.append("\"acting_player_name\":\"")
                .append(jsonEscape(actorName == null ? "" : actorName)).append("\",");
        out.append("\"requested_action\":\"").append(jsonEscape(requested)).append("\",");
"""
    text = replace_once(text, old, new, "Stage 8J actor audit field")

    path.write_text(text, encoding="utf-8")


def patch_picker(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    old = """                    bestAction = ExpertPolicyControlAdapter.select(
                            stage7DecisionIndex, stage8Candidates, bestAction);
"""
    new = """                    bestAction = ExpertPolicyControlAdapter.select(
                            stage7DecisionIndex, player.getLobbyPlayer().getName(),
                            stage8Candidates, bestAction);
"""
    text = replace_once(text, old, new, "Stage 8J actor-aware control call")
    path.write_text(text, encoding="utf-8")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--adapter", required=True, type=Path)
    p.add_argument("--spell-picker", required=True, type=Path)
    args = p.parse_args()
    patch_adapter(args.adapter)
    patch_picker(args.spell_picker)
    print("Patched Stage 8J sparse side-specific control into Forge")


if __name__ == "__main__":
    main()
