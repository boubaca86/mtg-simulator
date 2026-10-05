from __future__ import annotations

import json
import tempfile
from pathlib import Path

from stage8_label_counterfactual import parse_log


def capture(decision_index: int, selected: str = "A") -> str:
    row = {
        "schema_version": "stage8-capture-v1",
        "decision_index": decision_index,
        "run_seed": 123,
        "matchup_id": "test",
        "public_state": {"own_hand": ["Island"], "opponent_unknown_hand_count": 2},
        "selected_action": selected,
        "candidates": [
            {"action_identity": "A", "aggregate_score": 10, "replay_valid_count": 3},
            {"action_identity": "B", "aggregate_score": 5, "replay_valid_count": 3},
        ],
        "information_set_samples": 3,
    }
    return "EXPERT_STAGE8_CAPTURE: " + json.dumps(row, separators=(",", ":"))


def write_log(lines: list[str]) -> Path:
    f = tempfile.NamedTemporaryFile("w", suffix=".log", delete=False, encoding="utf-8")
    with f:
        f.write("\n".join(lines) + "\n")
    return Path(f.name)


def test_groups_only_at_terminal_boundary() -> None:
    path = write_log([
        "EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3",
        capture(0),
        capture(1),
        "Game Result: Game 1 Ai(1)-x has won!",
        "EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3",
        capture(2),
        "Game Result: Game 2 ended in a Draw!",
    ])
    rows = parse_log(path, expected_games=2, samples=3, corpus_seed=20261004)
    assert len(rows) == 3
    assert rows[0]["game_group"].endswith(":game-1")
    assert rows[1]["game_group"].endswith(":game-1")
    assert rows[2]["game_group"].endswith(":game-2")
    assert rows[0]["decision_id"].endswith(":0")
    assert rows[0]["corpus_seed"] == 20261004
    assert "run_seed" not in rows[0]


def test_quarantines_replay_mismatch() -> None:
    path = write_log([
        "EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3",
        capture(0),
        "EXPERT_INFOSET_ACTION_REPLAY_MISMATCH: bad",
        "Game Result: Game 1 Ai(1)-x has won!",
    ])
    try:
        parse_log(path, expected_games=1)
    except ValueError as exc:
        assert "replay mismatch" in str(exc)
    else:
        raise AssertionError("replay mismatch must quarantine the source log")


def test_quarantines_incomplete_audit() -> None:
    path = write_log([
        "EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=2 samples=3",
        capture(0),
        "Game Result: Game 1 Ai(1)-x has won!",
    ])
    try:
        parse_log(path, expected_games=1)
    except ValueError as exc:
        assert "incomplete hidden-world" in str(exc)
    else:
        raise AssertionError("incomplete audit must quarantine the source log")


if __name__ == "__main__":
    test_groups_only_at_terminal_boundary()
    test_quarantines_replay_mismatch()
    test_quarantines_incomplete_audit()
    print("Stage 8A live capture labeler: PASS")
