import json
import unittest

from stage8f_acceptance_audit import ACCEPT_PREFIX, audit_acceptance_lines
from stage8e_returned_action_audit import RETURN_PREFIX


ACTION = "recipe=v2|ability=A|candidate=0/1|x=<none>|modes=<none>|targets=<none>|choices=<none>"


def line(prefix, value):
    return prefix + json.dumps(value, sort_keys=True)


def returned(idx=4, priority=0, action=ACTION, turn=3, phase="MAIN1", actor="Expert"):
    return {
        "schema_version": "stage8e-returned-action-v1",
        "priority_return_index": priority,
        "capture_decision_index": idx,
        "turn": turn,
        "phase": phase,
        "acting_player_name": actor,
        "action_identity": action,
        "boundary": "spell-ability-return",
        "resolved_or_completed": False,
        "promotion_allowed": False,
    }


def accepted(idx=4, priority=0, action=ACTION, success=True, turn=3, phase="MAIN1", actor="Expert"):
    return {
        "schema_version": "stage8f-controller-acceptance-v1",
        "capture_decision_index": idx,
        "priority_return_index": priority,
        "turn": turn,
        "phase": phase,
        "acting_player_name": actor,
        "action_identity": action,
        "dispatch_success": success,
        "controller_return_value": True,
        "land_ability": False,
        "skip_after_dispatch": not success,
        "return_context_matches": True,
        "boundary": "player-controller-dispatch",
        "resolved_or_completed": False,
        "promotion_allowed": False,
    }


BASE = {
    "returned_actions": 1,
    "games_completed": 1,
}


class Stage8FAcceptanceAuditTests(unittest.TestCase):
    def test_exact_return_binds_to_successful_dispatch(self):
        lines = [
            line(RETURN_PREFIX, returned()),
            line(ACCEPT_PREFIX, accepted()),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        report = audit_acceptance_lines(lines, "synthetic.log", BASE)
        self.assertEqual(report["controller_acceptance_events"], 1)
        self.assertEqual(report["successful_dispatches"], 1)
        self.assertEqual(report["failed_dispatches"], 0)
        self.assertEqual(report["dispatch_success_fraction"], 1.0)
        self.assertFalse(report["resolution_verified"])
        self.assertFalse(report["promotion_allowed"])

    def test_dispatch_failure_is_measured_not_hidden(self):
        lines = [
            line(RETURN_PREFIX, returned()),
            line(ACCEPT_PREFIX, accepted(success=False)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        report = audit_acceptance_lines(lines, "synthetic.log", BASE)
        self.assertEqual(report["failed_dispatches"], 1)
        self.assertEqual(report["successful_dispatches"], 0)
        self.assertEqual(len(report["failed_dispatch_details"]), 1)

    def test_missing_acceptance_fails_closed(self):
        lines = [
            line(RETURN_PREFIX, returned()),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "missing controller acceptance"):
            audit_acceptance_lines(lines, "synthetic.log", BASE)

    def test_acceptance_before_return_fails_closed(self):
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            line(RETURN_PREFIX, returned()),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "unknown/future"):
            audit_acceptance_lines(lines, "synthetic.log", BASE)

    def test_identity_drift_fails_closed(self):
        lines = [
            line(RETURN_PREFIX, returned()),
            line(ACCEPT_PREFIX, accepted(action=ACTION + "|wrong")),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "identity drift"):
            audit_acceptance_lines(lines, "synthetic.log", BASE)

    def test_priority_index_drift_fails_closed(self):
        lines = [
            line(RETURN_PREFIX, returned()),
            line(ACCEPT_PREFIX, accepted(priority=1)),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "priority-return index drift"):
            audit_acceptance_lines(lines, "synthetic.log", BASE)

    def test_context_drift_fails_closed(self):
        lines = [
            line(RETURN_PREFIX, returned()),
            line(ACCEPT_PREFIX, accepted(phase="MAIN2")),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "public context drift"):
            audit_acceptance_lines(lines, "synthetic.log", BASE)

    def test_controller_return_semantics_must_remain_true(self):
        event = accepted()
        event["controller_return_value"] = False
        lines = [
            line(RETURN_PREFIX, returned()),
            line(ACCEPT_PREFIX, event),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "controller return semantics"):
            audit_acceptance_lines(lines, "synthetic.log", BASE)

    def test_acceptance_cannot_cross_game_boundary(self):
        lines = [
            line(RETURN_PREFIX, returned()),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
            line(ACCEPT_PREFIX, accepted()),
            "Game Result: Game 2 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "crossed a game boundary"):
            audit_acceptance_lines(lines, "synthetic.log", BASE)


if __name__ == "__main__":
    unittest.main()
