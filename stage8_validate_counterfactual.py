#!/usr/bin/env python3
"""Fail-closed validator for Stage 8 counterfactual action-ranking JSONL.

This validates the learner-facing serialization only. Forge remains the rules
referee and producer of complete-action identities and aggregate labels.
"""
from __future__ import annotations

import argparse
import json
from collections import defaultdict
from pathlib import Path
from stage8c_target_features import validated_target_descriptors

FORBIDDEN_KEYS = {
    "opponent_hand_cards", "opponent_hand_identities", "opponent_library",
    "opponent_library_cards", "library_order", "future_draws", "hidden_cards",
    "hidden_world", "hidden_world_cards", "determinization_cards",
}
REQUIRED = {
    "schema_version", "decision_id", "corpus_seed", "game_group",
    "public_state", "selected_action", "candidates", "information_set_samples",
    "forge_version", "search_policy",
}


def walk_keys(value):
    if isinstance(value, dict):
        for key, child in value.items():
            yield key
            yield from walk_keys(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_keys(child)


def validate_row(row, line_no):
    missing = REQUIRED - set(row)
    assert not missing, f"line {line_no}: missing fields {sorted(missing)}"
    assert row["schema_version"] == "stage8a-v1", f"line {line_no}: wrong schema"
    assert row["forge_version"] == "2.0.15", f"line {line_no}: unpinned Forge"
    assert row["search_policy"] == "fixed-root-v1", f"line {line_no}: unsafe search policy"
    assert isinstance(row["information_set_samples"], int) and row["information_set_samples"] >= 2, \
        f"line {line_no}: information_set_samples must be an integer >=2"
    assert isinstance(row["public_state"], dict), f"line {line_no}: public_state must be an object"
    keys = set(walk_keys(row["public_state"]))
    leaked = keys & FORBIDDEN_KEYS
    assert not leaked, f"line {line_no}: forbidden learner fields {sorted(leaked)}"
    candidates = row["candidates"]
    assert isinstance(candidates, list) and len(candidates) >= 2, f"line {line_no}: ranking requires >=2 candidates"
    identities = []
    for c in candidates:
        assert isinstance(c, dict), f"line {line_no}: candidate must be an object"
        assert set(c) >= {"action_identity", "aggregate_score", "replay_valid_count"}, f"line {line_no}: incomplete candidate"
        assert isinstance(c["action_identity"], str) and c["action_identity"], f"line {line_no}: empty action identity"
        assert isinstance(c["aggregate_score"], int) and not isinstance(c["aggregate_score"], bool), f"line {line_no}: non-integer Forge score"
        assert isinstance(c["replay_valid_count"], int) and not isinstance(c["replay_valid_count"], bool), f"line {line_no}: invalid replay count"
        assert c["replay_valid_count"] == row["information_set_samples"], f"line {line_no}: partial-world candidate"
        validated_target_descriptors(c)
        identities.append(c["action_identity"])
    assert len(identities) == len(set(identities)), f"line {line_no}: duplicate complete action"
    assert isinstance(row["selected_action"], str) and row["selected_action"], f"line {line_no}: empty selected action"
    assert row["selected_action"] in set(identities), f"line {line_no}: selected action absent from candidate set"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jsonl", type=Path)
    args = ap.parse_args()
    rows = []
    for i, text in enumerate(args.jsonl.read_text(encoding="utf-8").splitlines(), 1):
        if not text.strip():
            continue
        row = json.loads(text)
        validate_row(row, i)
        rows.append(row)
    assert rows, "empty Stage 8 dataset"
    by_decision = defaultdict(list)
    for row in rows:
        by_decision[row["decision_id"]].append(row)
    duplicates = [k for k, v in by_decision.items() if len(v) != 1]
    assert not duplicates, f"duplicate decision ids: {duplicates[:5]}"
    print(f"Stage 8A counterfactual contract: PASS ({len(rows)} decisions)")


if __name__ == "__main__":
    main()
