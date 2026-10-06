import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from stage8j_single_intervention import (
    _safe_signature,
    _target_free_identity,
    _side_score,
    _winner,
    _write_request,
    aggregate,
)


def candidate(identity):
    return {
        "action_identity": identity,
        "aggregate_score": 123,
        "replay_valid_count": 3,
        "target_semantics_version": "forge-public-targets-v2",
        "target_public_semantics": [],
    }


def capture(selected="recipe=v2|ability=A"):
    return {
        "decision_index": 4,
        "run_seed": 20261016,
        "matchup_id": "test",
        "public_state": {
            "schema_version": "stage7d-v1",
            "turn": 3,
            "phase": "MAIN1",
            "acting_player_name": "Ai(1)-ST Forge Full",
            "complete_action_identity": selected,
        },
        "selected_action": selected,
        "candidates": [
            candidate("recipe=v2|ability=A"),
            candidate("recipe=v2|ability=B"),
        ],
        "information_set_samples": 3,
    }


class Stage8JSingleInterventionTests(unittest.TestCase):
    def test_target_free_identity_requires_explicit_none_target_field(self):
        self.assertTrue(_target_free_identity(
            "recipe=v2|ability=Servitor|targets=<none>|choices=<none>"
        ))
        self.assertFalse(_target_free_identity(
            "recipe=v2|ability=Lightning Strike|targets=[Servitor (117)]|choices=<none>"
        ))
        self.assertFalse(_target_free_identity(
            "recipe=v2|ability=Malformed Without Target Field|choices=<none>"
        ))

    def test_safe_signature_ignores_only_selected_action_metadata_and_scores(self):
        left = capture("recipe=v2|ability=A")
        right = json.loads(json.dumps(left))
        right["selected_action"] = "recipe=v2|ability=B"
        right["public_state"]["complete_action_identity"] = "recipe=v2|ability=B"
        right["candidates"][0]["aggregate_score"] = -999
        right["candidates"][1]["aggregate_score"] = 777
        self.assertEqual(_safe_signature(left), _safe_signature(right))

    def test_safe_signature_detects_public_or_candidate_drift(self):
        base = capture()
        changed = json.loads(json.dumps(base))
        changed["public_state"]["turn"] = 4
        self.assertNotEqual(_safe_signature(base), _safe_signature(changed))
        changed = json.loads(json.dumps(base))
        changed["candidates"][1]["target_public_semantics"] = [
            "zone=opponent_battlefield|role=opponent|Goblin|mv=1|type=Creature|p=2|t=2"
        ]
        self.assertNotEqual(_safe_signature(base), _safe_signature(changed))

    def test_request_contains_only_index_and_encoded_identity(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "request.tsv"
            identity = "recipe=v2|ability=Alpha|candidate=0/2|x=<none>"
            digest = _write_request(path, 7, identity)
            raw = path.read_bytes()
            self.assertEqual(hashlib.sha256(raw).hexdigest(), digest)
            self.assertEqual(len(raw.decode().strip().split("\t")), 2)
            self.assertNotIn("aggregate_score", raw.decode())

    def test_winner_and_side_score(self):
        with tempfile.TemporaryDirectory() as td:
            log = Path(td) / "game.log"
            log.write_text(
                "Game Result: Game 1 ended in 1234 ms. Ai(1)-ST Forge Full has won!\n"
            )
            winner = _winner(log)
            self.assertTrue(winner.startswith("Ai(1)-"))
            self.assertEqual(_side_score(winner, "Ai(1)-"), 1.0)
            self.assertEqual(_side_score(winner, "Ai(2)-"), 0.0)

    def test_safety_gate_is_independent_of_outcome_delta(self):
        good = {
            "intervention_planned": True,
            "intervention_applied": True,
            "lifecycle_anomalies": 0,
            "invalid_requests_accepted": 0,
            "pre_intervention_drift": 0,
            "controlled_side_baseline_score": 1.0,
            "controlled_side_result_score": 0.0,
        }
        report = aggregate([dict(good) for _ in range(4)], 4)
        self.assertTrue(report["safety_gate_passed"])
        self.assertEqual(report["totals"]["controlled_side_score_delta"], -4.0)
        self.assertFalse(report["promotion_allowed"])
        self.assertFalse(report["broader_learned_control_allowed"])
        self.assertFalse(report["targeted_learned_actions_allowed"])

    def test_safety_gate_fails_for_too_few_or_anomalous_interventions(self):
        good = {
            "intervention_planned": True,
            "intervention_applied": True,
            "lifecycle_anomalies": 0,
            "invalid_requests_accepted": 0,
            "pre_intervention_drift": 0,
            "controlled_side_baseline_score": 0.0,
            "controlled_side_result_score": 1.0,
        }
        self.assertFalse(aggregate([dict(good) for _ in range(3)], 4)["safety_gate_passed"])
        bad = [dict(good) for _ in range(4)]
        bad[2]["lifecycle_anomalies"] = 1
        self.assertFalse(aggregate(bad, 4)["safety_gate_passed"])


if __name__ == "__main__":
    unittest.main()
