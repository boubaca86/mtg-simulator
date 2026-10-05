#!/usr/bin/env python3
import json
from stage8_serialize_counterfactual import canonical_line, normalize
from stage8_validate_counterfactual import validate_row


def sample():
    return {
        "decision_id": "seed-17:g0:d4",
        "corpus_seed": 17,
        "game_group": "seed-17",
        "public_state": {"life": [18, 14], "phase": "MAIN1", "visible": ["Island"]},
        "selected_action": "spell=A|target=opp",
        "information_set_samples": 3,
        "candidates": [
            {"action_identity": "spell=A|target=opp", "aggregate_score": 12, "replay_valid_count": 3},
            {"action_identity": "pass", "aggregate_score": 7, "replay_valid_count": 3},
        ],
    }


row = normalize(sample())
validate_row(row, 1)
assert canonical_line(row) == canonical_line(normalize(sample()))
assert [c["action_identity"] for c in row["candidates"]] == ["spell=A|target=opp", "pass"]
assert row["forge_version"] == "2.0.15"
assert row["search_policy"] == "fixed-root-v1"

# Input key order cannot perturb bytes.
s = sample()
s = dict(reversed(list(s.items())))
assert canonical_line(normalize(s)) == canonical_line(row)

# Partial-world evidence must fail before it reaches learner JSONL.
bad = sample()
bad["candidates"][1]["replay_valid_count"] = 2
try:
    normalize(bad)
    raise AssertionError("partial-world candidate accepted")
except ValueError as exc:
    assert "partial-world" in str(exc)

# Serializer must not silently reinterpret malformed Forge labels.
bad = sample()
bad["candidates"][0]["aggregate_score"] = 1.5
try:
    normalize(bad)
    raise AssertionError("non-integer Forge score accepted")
except ValueError:
    pass

print("Stage 8A deterministic serializer: PASS")
