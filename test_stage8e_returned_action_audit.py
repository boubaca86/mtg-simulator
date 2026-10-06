import json
import unittest

from stage8e_returned_action_audit import (
    CAPTURE_PREFIX, PASS_PREFIX, RETURN_PREFIX, audit_lines
)


def line(prefix, value):
    return prefix + json.dumps(value, sort_keys=True)


def capture(idx, action="recipe=v2|ability=A|candidate=0/2|x=<none>|modes=<none>|targets=<none>|choices=<none>"):
    other = "recipe=v2|ability=B|candidate=1/2|x=<none>|modes=<none>|targets=<none>|choices=<none>"
    return {
        "schema_version": "stage8-capture-v1",
        "decision_index": idx,
        "run_seed": 10,
        "matchup_id": "test",
        "public_state": {
            "schema_version": "stage7d-v1",
            "decision_index": idx,
            "turn": 4,
            "phase": "MAIN1",
            "acting_player_name": "Expert",
        },
        "selected_action": action,
        "candidates": [
            {"action_identity": action},
            {"action_identity": other},
        ],
        "information_set_samples": 3,
    }


def returned(priority, idx, action=None):
    action = action or capture(idx)["selected_action"]
    return {
        "schema_version": "stage8e-returned-action-v1",
        "priority_return_index": priority,
        "capture_decision_index": idx,
        "turn": 4,
        "phase": "MAIN1",
        "acting_player_name": "Expert",
        "action_identity": action,
        "boundary": "spell-ability-return",
        "resolved_or_completed": False,
        "promotion_allowed": False,
    }


def passed(priority):
    return {
        "schema_version": "stage8e-priority-pass-v1",
        "priority_return_index": priority,
        "turn": 4,
        "phase": "MAIN1",
        "acting_player_name": "Expert",
        "reason": "no-action-returned",
        "boundary": "spell-ability-return",
        "promotion_allowed": False,
    }


class Stage8EReturnedActionAuditTests(unittest.TestCase):
    def test_binds_return_and_leaves_probe_unbound(self):
        lines = [
            line(CAPTURE_PREFIX, capture(0)),
            line(RETURN_PREFIX, returned(0, 0)),
            line(CAPTURE_PREFIX, capture(1)),
            line(PASS_PREFIX, passed(1)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        report = audit_lines(lines, [0, 1], "synthetic.log")
        self.assertEqual(report["returned_actions"], 1)
        self.assertEqual(report["priority_passes"], 1)
        self.assertEqual(report["unbound_probe_or_deferred_captures"], 1)
        self.assertFalse(report["resolution_verified"])
        self.assertFalse(report["promotion_allowed"])

    def test_rejects_action_identity_drift(self):
        wrong = "recipe=v2|ability=WRONG|candidate=0/1|x=<none>|modes=<none>|targets=<none>|choices=<none>"
        lines = [
            line(CAPTURE_PREFIX, capture(0)),
            line(RETURN_PREFIX, returned(0, 0, wrong)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "differs"):
            audit_lines(lines, [0], "synthetic.log")

    def test_rejects_duplicate_binding(self):
        lines = [
            line(CAPTURE_PREFIX, capture(0)),
            line(RETURN_PREFIX, returned(0, 0)),
            line(RETURN_PREFIX, returned(1, 0)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "multiply-bound"):
            audit_lines(lines, [0], "synthetic.log")

    def test_rejects_noncontiguous_priority_indices(self):
        lines = [
            line(CAPTURE_PREFIX, capture(0)),
            line(RETURN_PREFIX, returned(1, 0)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "contiguous"):
            audit_lines(lines, [0], "synthetic.log")

    def test_rejects_capture_not_seen_by_stage7_validator(self):
        lines = [
            line(CAPTURE_PREFIX, capture(0)),
            line(RETURN_PREFIX, returned(0, 0)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "capture mismatch"):
            audit_lines(lines, [], "synthetic.log")

    def test_rejects_cross_game_binding(self):
        lines = [
            line(CAPTURE_PREFIX, capture(0)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
            line(RETURN_PREFIX, returned(0, 0)),
            "Game Result: Game 2 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "crossed"):
            audit_lines(lines, [0], "synthetic.log")


if __name__ == "__main__":
    unittest.main()
