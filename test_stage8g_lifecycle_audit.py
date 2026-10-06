import json
import unittest

from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8g_lifecycle_audit import TERMINAL_PREFIX, audit_lifecycle_lines


ACTION = "recipe=v2|ability=A|candidate=0/1|x=<none>|modes=<none>|targets=<none>|choices=<none>"


def line(prefix, value):
    return prefix + json.dumps(value, sort_keys=True)


def accepted(idx=3, priority=8, action=ACTION, turn=5, phase="MAIN1", actor="Expert"):
    return {
        "schema_version": "stage8f-controller-acceptance-v1",
        "capture_decision_index": idx,
        "priority_return_index": priority,
        "turn": turn,
        "phase": phase,
        "acting_player_name": actor,
        "action_identity": action,
        "dispatch_success": True,
        "controller_return_value": True,
        "land_ability": False,
        "skip_after_dispatch": False,
        "return_context_matches": True,
        "boundary": "player-controller-dispatch",
        "resolved_or_completed": False,
        "promotion_allowed": False,
    }


def terminal(outcome="resolved", idx=3, priority=8, action=ACTION, actor="Expert",
             return_turn=5, return_phase="MAIN1"):
    stack_based = outcome in {"resolved", "fizzled", "removed-before-resolution"}
    if outcome == "resolved":
        fizzled = False
    elif outcome == "fizzled":
        fizzled = True
    elif outcome == "no-stack-completed":
        fizzled = False
    else:
        fizzled = None
    return {
        "schema_version": "stage8g-action-terminal-v1",
        "capture_decision_index": idx,
        "priority_return_index": priority,
        "action_identity": action,
        "acting_player_name": actor,
        "return_turn": return_turn,
        "return_phase": return_phase,
        "terminal_turn": 5,
        "terminal_phase": "MAIN1",
        "outcome": outcome,
        "stack_based": stack_based,
        "fizzled": fizzled,
        "boundary": "forge-action-terminal",
        "promotion_allowed": False,
    }


BASE = {"successful_dispatches": 1, "games_completed": 1}


class Stage8GLifecycleAuditTests(unittest.TestCase):
    def test_resolved_terminal_binds_exactly(self):
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            line(TERMINAL_PREFIX, terminal("resolved")),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        report = audit_lifecycle_lines(lines, "synthetic.log", BASE)
        self.assertEqual(report["terminal_events"], 1)
        self.assertEqual(report["pending_at_game_end"], 0)
        self.assertEqual(report["terminal_outcome_counts"], {"resolved": 1})
        self.assertEqual(report["terminal_coverage_fraction"], 1.0)

    def test_fizzled_and_removed_semantics(self):
        for outcome in ("fizzled", "removed-before-resolution", "no-stack-completed"):
            with self.subTest(outcome=outcome):
                lines = [
                    line(ACCEPT_PREFIX, accepted()),
                    line(TERMINAL_PREFIX, terminal(outcome)),
                    "Game Result: Game 1 ended in 1 ms. Expert has won!",
                ]
                report = audit_lifecycle_lines(lines, "synthetic.log", BASE)
                self.assertEqual(report["terminal_outcome_counts"], {outcome: 1})

    def test_pending_at_game_end_is_visible(self):
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        report = audit_lifecycle_lines(lines, "synthetic.log", BASE)
        self.assertEqual(report["terminal_events"], 0)
        self.assertEqual(report["pending_at_game_end"], 1)
        self.assertEqual(report["pending_capture_decision_indices"], [3])

    def test_terminal_before_acceptance_fails_closed(self):
        lines = [
            line(TERMINAL_PREFIX, terminal()),
            line(ACCEPT_PREFIX, accepted()),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "unknown/future"):
            audit_lifecycle_lines(lines, "synthetic.log", BASE)

    def test_identity_drift_fails_closed(self):
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            line(TERMINAL_PREFIX, terminal(action=ACTION + "|drift")),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "identity drift"):
            audit_lifecycle_lines(lines, "synthetic.log", BASE)

    def test_return_context_drift_fails_closed(self):
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            line(TERMINAL_PREFIX, terminal(return_phase="MAIN2")),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "return context drift"):
            audit_lifecycle_lines(lines, "synthetic.log", BASE)

    def test_duplicate_terminal_fails_closed(self):
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            line(TERMINAL_PREFIX, terminal()),
            line(TERMINAL_PREFIX, terminal()),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "duplicate"):
            audit_lifecycle_lines(lines, "synthetic.log", BASE)

    def test_bad_terminal_semantics_fail_closed(self):
        event = terminal("resolved")
        event["fizzled"] = True
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            line(TERMINAL_PREFIX, event),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        with self.assertRaisesRegex(ValueError, "resolved terminal semantics mismatch"):
            audit_lifecycle_lines(lines, "synthetic.log", BASE)

    def test_lifecycle_anomaly_is_counted(self):
        lines = [
            line(ACCEPT_PREFIX, accepted()),
            line(TERMINAL_PREFIX, terminal("nonland-success-without-stack-binding")),
            "Game Result: Game 1 ended in 1 ms. Expert has won!",
        ]
        report = audit_lifecycle_lines(lines, "synthetic.log", BASE)
        self.assertEqual(report["lifecycle_anomalies"], 1)


if __name__ == "__main__":
    unittest.main()
