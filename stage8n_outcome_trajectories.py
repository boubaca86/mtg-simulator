#!/usr/bin/env python3
"""Stage 8N: collect public-only outcome trajectories from exploration games.

This module reuses Stage 8L's strict log/lifecycle collector, then explicitly
relabels controlled alternatives as exploration-return-boundary behavior.
Nothing about the action is called "learned" because Stage 8N selection is
model-independent by design.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path

from stage8h_lifecycle_audit import ALLOWED_OUTCOMES, ANOMALY_OUTCOMES
from stage8l_outcome_trajectories import collect_log as collect_stage8l_log
from stage8l_outcome_trajectories import validate_model_input
from stage8n_exploration import (
    DEVELOPMENT_SEEDS,
    RESERVED_EVALUATION_SEEDS,
    EXPLORATION_RULE,
)

SCHEMA_VERSION = "stage8n-outcome-trajectory-v1"
MANIFEST_VERSION = "stage8n-outcome-trajectory-manifest-v1"


def canonical_json(value) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False)


def _corpus_seed_from_path(path: Path) -> int:
    match = re.search(r"(\d{8})(?=\.log$)", path.name)
    if not match:
        raise ValueError("Stage 8N source log filename lacks corpus seed")
    return int(match.group(1))


def _convert_row(row: dict) -> dict:
    value = dict(row)
    value["audit"] = dict(row["audit"])
    value["schema_version"] = SCHEMA_VERSION
    if value["behavior_source"] == "learned-return-boundary":
        value["behavior_source"] = "exploration-return-boundary"
    elif value["behavior_source"] != "forge":
        raise ValueError("unexpected inherited Stage 8N behavior source")
    value["audit"]["exploration_rule"] = EXPLORATION_RULE
    return value


def validate_row(row: dict) -> dict:
    if row.get("schema_version") != SCHEMA_VERSION:
        raise ValueError("wrong Stage 8N row schema")
    model_input = validate_model_input(row.get("model_input", {}))
    legal = {candidate.get("action_identity") for candidate in model_input["candidates"]}
    if row.get("behavior_action") not in legal:
        raise ValueError("Stage 8N behavior action is not a captured legal candidate")
    if row.get("behavior_source") not in {"forge", "exploration-return-boundary"}:
        raise ValueError("unknown Stage 8N behavior source")
    labels = row.get("labels")
    if not isinstance(labels, dict) or set(labels) != {
        "game_result",
        "terminal_action_outcome",
    }:
        raise ValueError("Stage 8N labels must remain separate from model input")
    if labels["game_result"] not in (0.0, 0.5, 1.0):
        raise ValueError("invalid Stage 8N game result")
    if labels["terminal_action_outcome"] not in ALLOWED_OUTCOMES:
        raise ValueError("invalid Stage 8N terminal outcome")
    if labels["terminal_action_outcome"] in ANOMALY_OUTCOMES:
        raise ValueError("Stage 8N refuses anomalous action lifecycles")

    audit = row.get("audit")
    if not isinstance(audit, dict):
        raise ValueError("Stage 8N audit metadata missing")
    seed = audit.get("corpus_seed")
    if type(seed) is not int or seed not in DEVELOPMENT_SEEDS:
        raise ValueError("Stage 8N row uses an undeclared development seed")
    if seed in RESERVED_EVALUATION_SEEDS:
        raise ValueError("reserved Stage 8N evaluation seed entered the corpus")
    if (
        audit.get("exploration_rule") != EXPLORATION_RULE
        or audit.get("forge_referee") is not True
        or audit.get("return_boundary_verified") is not True
        or audit.get("controller_acceptance_verified") is not True
        or audit.get("terminal_binding_verified") is not True
        or audit.get("hidden_information_fields") != 0
        or audit.get("promotion_allowed") is not False
    ):
        raise ValueError("Stage 8N safety provenance drift")
    forge_action = audit.get("forge_proposed_action")
    if row["behavior_source"] == "forge":
        if row["behavior_action"] != forge_action:
            raise ValueError("Stage 8N Forge row disagrees with Forge proposal")
    else:
        if row["behavior_action"] == forge_action:
            raise ValueError("Stage 8N exploration row did not differ from Forge")
    return row


def collect_logs(paths: list[Path]) -> tuple[list[dict], dict]:
    if not paths:
        raise ValueError("Stage 8N requires controlled game logs")
    rows = []
    seen_ids = set()
    seen_logs = set()
    games_by_seed = Counter()

    for path in paths:
        if path.name in seen_logs:
            raise ValueError("duplicate Stage 8N source log")
        seen_logs.add(path.name)
        seed = _corpus_seed_from_path(path)
        if seed not in DEVELOPMENT_SEEDS:
            raise ValueError(f"unexpected Stage 8N development seed family: {seed}")
        if seed in RESERVED_EVALUATION_SEEDS:
            raise ValueError("reserved Stage 8N evaluation seed entered collection")
        games_by_seed[seed] += 1

        inherited = collect_stage8l_log(path)
        for source_row in inherited:
            row = _convert_row(source_row)
            row["audit"]["corpus_seed"] = seed
            validate_row(row)
            if row["trajectory_id"] in seen_ids:
                raise ValueError("duplicate Stage 8N trajectory ID")
            seen_ids.add(row["trajectory_id"])
            rows.append(row)

    if sorted(games_by_seed) != list(DEVELOPMENT_SEEDS):
        raise ValueError("Stage 8N corpus does not cover every development seed")
    if any(games_by_seed[seed] != 2 for seed in DEVELOPMENT_SEEDS):
        raise ValueError("Stage 8N requires exactly two games per development seed")

    behavior = Counter(row["behavior_source"] for row in rows)
    terminal = Counter(row["labels"]["terminal_action_outcome"] for row in rows)
    outcomes = Counter(str(row["labels"]["game_result"]) for row in rows)
    targeted_exploration = sum(
        row["behavior_source"] == "exploration-return-boundary"
        and row["behavior_action_targeted"]
        for row in rows
    )
    manifest = {
        "schema_version": MANIFEST_VERSION,
        "mode": "development-public-exploration-outcome-trajectories",
        "development_seed_families": list(DEVELOPMENT_SEEDS),
        "reserved_untouched_evaluation_seed_families": list(RESERVED_EVALUATION_SEEDS),
        "games": len(paths),
        "trajectory_rows": len(rows),
        "behavior_source_counts": dict(sorted(behavior.items())),
        "game_result_label_counts": dict(sorted(outcomes.items())),
        "terminal_outcome_counts": dict(sorted(terminal.items())),
        "exploration_behavior_rows": behavior.get("exploration-return-boundary", 0),
        "targeted_exploration_behavior_rows": targeted_exploration,
        "model_input_contains_search_scores": False,
        "model_input_contains_terminal_outcomes": False,
        "model_input_contains_hidden_information": False,
        "exploration_model_guided": False,
        "exploration_outcome_guided": False,
        "forge_referee": True,
        "training_performed": False,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "strength_claim_allowed": False,
        "next_stage": (
            "Combine this development corpus with Stage 8L, predeclare the next "
            "outcome learner, and require grouped development validation before "
            "considering the still-untouched reserved evaluation families."
        ),
    }
    return rows, manifest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("logs", nargs="+", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    args = parser.parse_args()

    rows, manifest = collect_logs(args.logs)
    args.output.write_text(
        "".join(canonical_json(row) + "\n" for row in rows),
        encoding="utf-8",
    )
    args.manifest.write_text(
        json.dumps(manifest, sort_keys=True, indent=2) + "\n",
        encoding="utf-8",
    )
    print(canonical_json({
        "games": manifest["games"],
        "trajectory_rows": manifest["trajectory_rows"],
        "exploration_behavior_rows": manifest["exploration_behavior_rows"],
        "targeted_exploration_behavior_rows": manifest["targeted_exploration_behavior_rows"],
        "promotion_allowed": manifest["promotion_allowed"],
    }))


if __name__ == "__main__":
    main()
