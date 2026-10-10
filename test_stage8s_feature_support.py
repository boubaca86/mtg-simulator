"""Stage 8S synthetic read-only regressions; no gameplay or held-out seeds."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

import stage8o_outcome_policy as old
import stage8s_feature_support as s
from test_stage8o_outcome_policy import make_rows, write_rows


def toy_samples():
    rows = []
    for seed in old.TRAIN_SEEDS:
        for orientation in ("a", "b"):
            features = {
                "q_gain_life_to_self_player": 0.25 if orientation == "a" else 0.0,
                "q_pump_to_self_creature": 0.25 if orientation == "b" else 0.0,
                "q_damage_to_self_creature": 0.0,
                "q_fixed_damage_to_self_creature": 0.0,
                "q_damage_to_opponent_player": 0.25 if orientation == "b" else 0.0,
            }
            rows.append({
                "family": seed,
                "game": (seed, orientation),
                "features": features,
                "outcome": 1.0 if orientation == "a" else 0.0,
            })
    return rows


def toy_folds(diff=0.02):
    fresh, baseline = [], []
    for seed in old.TRAIN_SEEDS:
        baseline.append({
            "holdout_seed": seed, "held_games": 2, "train_games": 22,
            "model": {"logloss": 0.69, "brier": 0.25},
        })
        fresh.append({
            "held_seed": seed, "held_games": 2, "train_games": 22,
            "model": {"logloss": 0.69 + diff, "brier": 0.25 + diff},
        })
    return fresh, baseline


class Stage8SSafetyTests(unittest.TestCase):
    def test_beneficial_self_target_features_are_not_excluded(self):
        rows = toy_samples()
        report = s.feature_support(rows, sorted(rows[0]["features"]))
        self.assertEqual(report["by_feature"]["q_gain_life_to_self_player"]["active_games"], 12)
        self.assertEqual(report["by_feature"]["q_pump_to_self_creature"]["active_families"], 12)
        self.assertEqual(report["by_feature"]["q_damage_to_self_creature"]["active_games"], 0)
        self.assertTrue(report["beneficial_self_target_options_always_remain_legal"])
        self.assertEqual(report["number_never_activated"], 2)

    def test_support_fails_on_missing_or_foreign_seed_family(self):
        rows = toy_samples()
        with self.assertRaisesRegex(ValueError, "seed families"):
            s.feature_support(rows[:-2], list(rows[0]["features"]))
        poisoned = copy.deepcopy(rows)
        poisoned[0]["family"] = 20261032
        with self.assertRaisesRegex(ValueError, "seed families"):
            s.feature_support(poisoned, list(rows[0]["features"]))
        poisoned = copy.deepcopy(rows)
        poisoned[0]["features"]["q_damage_to_self_creature"] = float("nan")
        with self.assertRaisesRegex(ValueError, "nonfinite"):
            s.feature_support(poisoned, list(rows[0]["features"]))

    def test_family_bootstrap_is_deterministic_and_keeps_orientations_together(self):
        new, base = toy_folds(0.02)
        paired = s._paired_folds(new, base)
        self.assertEqual(len(paired), 12)
        a = s.family_bootstrap(paired, resamples=1000, seed=4)
        b = s.family_bootstrap(paired, resamples=1000, seed=4)
        self.assertEqual(a, b)
        self.assertAlmostEqual(a["logloss"]["mean_paired_delta"], .02)
        self.assertGreater(a["logloss"]["percentile_95_low"], 0)
        self.assertFalse(a["brier"]["interval_crosses_zero"])

    def test_missing_family_or_single_orientation_blocks_analysis(self):
        new, base = toy_folds()
        with self.assertRaisesRegex(ValueError, "exact development"):
            s._paired_folds(new[:-1], base)
        new, base = toy_folds()
        base[0]["held_games"] = 1
        with self.assertRaisesRegex(ValueError, "orientations"):
            s._paired_folds(new, base)

    def test_real_loader_contract_on_synthetic_development_games(self):
        early, later = make_rows()
        with tempfile.TemporaryDirectory() as td:
            a, b = Path(td) / "l.jsonl", Path(td) / "n.jsonl"
            write_rows(a, early)
            write_rows(b, later)
            report = s.export(a, b)
        self.assertEqual(report["training_games"], 24)
        self.assertEqual(report["observed_action_rows"], 331)
        self.assertEqual(report["consumed_reserved_seed_families_not_used"], [20261032, 20261033])
        self.assertEqual(len(report["paired_seed_family_differences"]), 12)
        self.assertFalse(report["promotion_allowed"])
        self.assertFalse(report["new_gameplay_executed"])
        self.assertFalse(report["new_training_or_model_selection"])
        self.assertFalse(report["strength_claim_allowed"])
        self.assertTrue(report["future_proposed_seeds_unverified_and_not_used"])
        self.assertTrue(report["feature_support"]["number_of_q_features"] > 4)


if __name__ == "__main__":
    unittest.main()
