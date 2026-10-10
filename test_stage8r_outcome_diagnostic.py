"""Stage 8R offline-only regressions; all rows synthetic, no Forge gameplay."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

import stage8o_outcome_policy as old
import stage8r_outcome_diagnostic as q
from test_stage8o_outcome_policy import make_rows, write_rows


class Stage8ROutcomeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.early, cls.later = make_rows()

    def _write(self, folder, early=None, later=None):
        a, b = Path(folder) / "8l.jsonl", Path(folder) / "8n.jsonl"
        write_rows(a, self.early if early is None else early)
        write_rows(b, self.later if later is None else later)
        return a, b

    def test_nonreserved_dataset_and_public_only_fixed_features(self):
        with tempfile.TemporaryDirectory() as td:
            records, hashes = q.read_training(*self._write(td))
        self.assertEqual(len(records), 331)
        self.assertEqual(len(hashes), 2)
        data, names = q.observations(records)
        self.assertEqual(len(data), 331)
        self.assertEqual(len({r["game"] for r in data}), 24)
        self.assertIn("q_damage_to_self_creature", names)
        self.assertIn("q_damage_to_opponent_player", names)
        self.assertTrue(set(q.CONSUMED_RESERVED_SEEDS).isdisjoint(
            {r["family"] for r in data}))
        self.assertTrue(all(abs(r["weight"]) < 1 and r["outcome"] in (0, .5, 1)
                            for r in data))

    def test_reservation_and_hidden_information_are_blocked(self):
        with tempfile.TemporaryDirectory() as td:
            early = copy.deepcopy(self.early)
            early[0]["audit"]["corpus_seed"] = 20261032
            with self.assertRaises(ValueError):
                q.read_training(*self._write(td, early=early))
            early = copy.deepcopy(self.early)
            early[0]["model_input"]["public_state"]["opponent_hand"] = ["Secret"]
            with self.assertRaises(ValueError):
                q.read_training(*self._write(td, early=early))

    def test_synthetic_grouped_diagnostic_and_checkpoint_integrity(self):
        with tempfile.TemporaryDirectory() as td:
            checkpoint, report = q.export(*self._write(td))
            cv = report["grouped_development_comparison"]
            self.assertEqual(len(cv["folds"]), 12)
            self.assertEqual({v["held_seed"] for v in cv["folds"]}, set(old.TRAIN_SEEDS))
            self.assertTrue(all(v["held_games"] == 2 and v["train_games"] == 22
                                for v in cv["folds"]))
            self.assertEqual(cv["stage8q_representation_improved_both_metrics"],
                             cv["model"]["logloss"] < cv["stage8o_model"]["logloss"] - 1e-6
                             and cv["model"]["brier"] < cv["stage8o_model"]["brier"] - 1e-6
                             and cv["model"]["logloss"] < cv["constant_baseline"]["logloss"] - 1e-6
                             and cv["model"]["brier"] < cv["constant_baseline"]["brier"] - 1e-6)
            self.assertFalse(report["promotion_allowed"])
            self.assertFalse(report["broader_learned_control_allowed"])
            self.assertFalse(report["strength_claim_allowed"])
            self.assertFalse(report["new_gameplay_executed"])
            self.assertTrue(report["proposed_fresh_seed_families_unverified"])
            self.assertEqual(checkpoint["consumed_reserved_seeds"], [20261032, 20261033])
            out = Path(td) / "model.json"
            out.write_text(old.canonical(checkpoint) + "\n")
            self.assertEqual(q.load(out)["model_id"], checkpoint["model_id"])
            altered = copy.deepcopy(checkpoint)
            altered["weights"]["bias"] += 0.5
            out.write_text(old.canonical(altered))
            with self.assertRaisesRegex(ValueError, "hash"):
                q.load(out)
            altered = copy.deepcopy(checkpoint)
            altered["future_proposed_holdout_seeds"] = [20261032]
            altered["model_id"] = old.digest({k: v for k,v in altered.items()
                                               if k != "model_id"})
            out.write_text(old.canonical(altered))
            with self.assertRaisesRegex(ValueError, "provenance"):
                q.load(out)

    def test_grouping_enforces_seed_family_and_game_disjointness(self):
        with tempfile.TemporaryDirectory() as td:
            rec, _ = q.read_training(*self._write(td))
        data, names = q.observations(rec)
        inconsistent = copy.deepcopy(data)
        # Poison an actual sample so it shares a game ID with an earlier fold.
        wrong_seed = inconsistent[0]["family"]
        other = next(x for x in inconsistent if x["family"] != wrong_seed)
        other["game"] = inconsistent[0]["game"]
        with self.assertRaisesRegex(ValueError, "leakage"):
            q.grouped_comparison(rec, inconsistent, names)

    def test_training_does_not_modify_frozen_stage8o_or_observations(self):
        with tempfile.TemporaryDirectory() as td:
            rec, _ = q.read_training(*self._write(td))
        data, names = q.observations(rec)
        previous = copy.deepcopy(data)
        result = q.fit(data, names)
        self.assertEqual(data, previous)
        self.assertEqual(set(result), set(names))
        self.assertNotIn("future_draw", result)
        self.assertFalse(set(q.PROPOSED_FRESH_DEV_SEEDS) & set(old.TRAIN_SEEDS))
        self.assertFalse(set(q.PROPOSED_FRESH_HOLDOUT_SEEDS) & set(old.TRAIN_SEEDS))


if __name__ == "__main__":
    unittest.main()
