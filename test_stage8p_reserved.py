"""Stage 8P public-only selection, seed isolation and one-shot gate regressions."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

from stage8d_shadow_policy import CAPTURE_PREFIX
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8p_reserved_planner import (
    MODEL_ID, SEEDS, _recommend, baseline_identity, choose, expected_plan,
)
from stage8p_reserved_gate import aggregate
from test_stage8d_shadow_policy import capture, policy


class Stage8PTests(unittest.TestCase):
    def setUp(self):
        self.validator = policy()
        self.model = {"model_id": MODEL_ID, "weights": {"action_cast": 4.0}}
        self.capture = capture()
        self.capture['matchup_id'] = 'fixture'
        first, second = self.capture["candidates"]
        second["action_identity"] = second["action_identity"].replace(
            "ability=Remove target creature", "ability=Cast creature")
        self.capture["selected_action"] = first["action_identity"]
        self.capture["public_state"]["complete_action_identity"] = first["action_identity"]

    def test_reserved_seeds_only(self):
        self.assertEqual(SEEDS, (20261032, 20261033))
        self.assertEqual(baseline_identity(Path("stage8p-baseline-red-first-20261032.log")),
                         ("red-first", 20261032))
        for name in ("stage8p-baseline-red-first-20261034.log",
                     "stage8p-baseline-st-first-20261031.log",
                     "stage8n-baseline-st-first-20261032.log"):
            with self.assertRaises(ValueError):
                baseline_identity(Path(name))

    def test_unique_public_recommendation_and_tie_abstention(self):
        chosen = _recommend(self.model, self.validator, self.capture)
        self.assertEqual(chosen["learned_action"],
                         self.capture["candidates"][1]["action_identity"])
        self.assertGreater(chosen["predicted_margin"], 0)
        self.assertIsNone(_recommend({"weights": {}}, self.validator, self.capture))
        reversed_capture = copy.deepcopy(self.capture)
        reversed_capture["candidates"].reverse()
        self.assertEqual(_recommend(self.model, self.validator, reversed_capture),
                         chosen)

    def test_hidden_information_is_rejected(self):
        changed = copy.deepcopy(self.capture)
        changed["public_state"]["opponent_hand"] = ["Secret Card"]
        with self.assertRaises(ValueError):
            _recommend(self.model, self.validator, changed)

    def test_cannot_select_before_real_return_boundary(self):
        with tempfile.TemporaryDirectory() as temp:
            base = Path(temp) / "stage8p-baseline-st-first-20261032.log"
            capture_line = CAPTURE_PREFIX + json.dumps(self.capture) + "\n"
            base.write_text(capture_line)
            self.assertIsNone(choose(self.model, self.validator, base))
            ret = {"capture_decision_index": self.capture["decision_index"],
                   "action_identity": self.capture["selected_action"]}
            base.write_text(capture_line + RETURN_PREFIX + json.dumps(ret) + "\n")
            chosen = choose(self.model, self.validator, base)
            self.assertIsNotNone(chosen)
            base.write_text(base.read_text() + "Game Result: Game 1 ended in 100 ms. Ai(1)-Example has won!\n")
            self.assertEqual(choose(self.model, self.validator, base), chosen)
            plan, raw = expected_plan(self.model, self.validator, base, Path("request.tsv"))
            self.assertFalse(plan["promotion_allowed"])
            self.assertFalse(plan["future_outcome_guided_selection"])
            self.assertEqual(len(raw.split(b"\t")), 3)

    def test_one_shot_gate_requires_all_four_pairs(self):
        pairs = []
        for seed in SEEDS:
            for orientation in ("st-first", "red-first"):
                pairs.append({
                    "seed_family": seed, "orientation": orientation,
                    "intervention_planned": True, "intervention_applied": True,
                    "intervention_targeted": False,
                    "baseline_side_score": 0.0,
                    "controlled_side_score": 1.0 if seed == SEEDS[0] else 0.0,
                    "paired_score_delta": 1.0 if seed == SEEDS[0] else 0.0,
                    "failed_dispatches": 0, "lifecycle_anomalies": 0,
                    "pre_intervention_drift": 0, "forge_referee": True,
                    "promotion_allowed": False,
                })
        report = aggregate(pairs)
        self.assertTrue(report["evaluation_gate_passed"])
        self.assertFalse(report["strength_claim_allowed"])
        with self.assertRaises(ValueError):
            aggregate(pairs[:3])
        changed = copy.deepcopy(pairs)
        changed[0]["failed_dispatches"] = 1
        self.assertFalse(aggregate(changed)["safety_gate_passed"])
        changed = copy.deepcopy(pairs)
        for p in changed:
            p["paired_score_delta"] = 0.0
        self.assertFalse(aggregate(changed)["primary_game_result_gate_passed"])


if __name__ == "__main__":
    unittest.main()
