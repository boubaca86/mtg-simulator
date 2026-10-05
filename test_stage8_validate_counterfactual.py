#!/usr/bin/env python3
"""Regression tests for the Stage 8 learner-boundary contract."""
from copy import deepcopy

from stage8_validate_counterfactual import validate_row


def valid_row():
    return {
        "schema_version": "stage8a-v1",
        "decision_id": "seed-101-game-0-decision-3",
        "corpus_seed": 101,
        "game_group": "seed-101-game-0",
        "public_state": {
            "turn": 4,
            "active_player": 0,
            "life": [17, 14],
            "hand_size": [4, 3],
            "library_size": [47, 45],
            "battlefield": {"self": [], "opponent": []},
            "graveyard": {"self": [], "opponent": []},
            "exile": {"self": [], "opponent": []},
        },
        "selected_action": "spell:42|target:player:1|mode:0|x:0",
        "candidates": [
            {"action_identity": "spell:42|target:player:1|mode:0|x:0", "aggregate_score": 83, "replay_valid_count": 3},
            {"action_identity": "pass-priority", "aggregate_score": 61, "replay_valid_count": 3},
        ],
        "information_set_samples": 3,
        "forge_version": "2.0.15",
        "search_policy": "fixed-root-v1",
    }


def must_fail(mutator, needle):
    row = valid_row()
    mutator(row)
    try:
        validate_row(row, 1)
    except AssertionError as exc:
        assert needle in str(exc), (needle, str(exc))
    else:
        raise AssertionError(f"expected fail-closed rejection containing {needle!r}")


def main():
    validate_row(valid_row(), 1)

    must_fail(lambda r: r["public_state"].update({"opponent_hand_cards": ["Black Lotus"]}), "forbidden learner fields")
    must_fail(lambda r: r["public_state"].update({"hidden_world": {"opponent": ["Counterspell"]}}), "forbidden learner fields")
    must_fail(lambda r: r.update({"forge_version": "latest"}), "unpinned Forge")
    must_fail(lambda r: r.update({"search_policy": "per-world-best"}), "unsafe search policy")
    must_fail(lambda r: r.update({"information_set_samples": 1}), "information_set_samples")
    must_fail(lambda r: r["candidates"].pop(), "ranking requires >=2 candidates")
    must_fail(lambda r: r["candidates"][1].update({"replay_valid_count": 2}), "partial-world candidate")
    must_fail(lambda r: r["candidates"][1].update({"action_identity": r["candidates"][0]["action_identity"]}), "duplicate complete action")
    must_fail(lambda r: r.update({"selected_action": "not-in-candidate-set"}), "selected action absent")

    # Hidden information may not be smuggled in under a nested public-state object.
    def nested_leak(r):
        r["public_state"]["visible_card"] = {"metadata": {"future_draws": ["Ancestral Recall"]}}
    must_fail(nested_leak, "forbidden learner fields")

    print("Stage 8A counterfactual validator regression: PASS")


if __name__ == "__main__":
    main()
