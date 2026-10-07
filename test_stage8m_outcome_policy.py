import copy
import json
import tempfile
import unittest
from pathlib import Path

import stage8m_outcome_policy as outcome
from test_stage8l_outcome_trajectories import row as stage8l_row


def corpus():
    rows = []
    for seed in outcome.TRAINING_SEEDS:
        for orientation_index, orientation in enumerate(("st-first", "red-first")):
            game = f"stage8l-controlled-{orientation}-{seed}.log"
            for decision in range(2):
                value = stage8l_row(seed, learned=(decision == 1))
                value["trajectory_id"] = f"{game}:decision-{decision}"
                value["source_log"] = game
                value["trajectory_step"] = decision
                value["audit"]["corpus_seed"] = seed
                value["audit"]["run_seed"] = seed + decision + orientation_index
                value["audit"]["decision_index"] = decision
                value["model_input"]["public_state"]["turn"] = 2 + decision
                value["model_input"]["public_state"]["acting_life"] = 20 - orientation_index - decision
                value["model_input"]["public_state"]["opponent_life"] = 18 + decision
                value["labels"]["game_result"] = float((seed + orientation_index) % 2 == 0)
                rows.append(value)
    return rows


def write_rows(path: Path, rows):
    path.write_text("".join(outcome.canonical_json(value) + "\n" for value in rows))


class Stage8MOutcomePolicyTests(unittest.TestCase):
    def test_training_corpus_is_public_only_and_reserved_seeds_are_rejected(self):
        rows = corpus()
        self.assertIs(outcome.validate_rows(rows), rows)

        bad = copy.deepcopy(rows)
        bad[0]["audit"]["corpus_seed"] = outcome.RESERVED_SEEDS[0]
        with self.assertRaisesRegex(ValueError, "reserved"):
            outcome.validate_rows(bad)

        leaked = copy.deepcopy(rows)
        leaked[0]["model_input"]["public_state"]["game_result"] = 1.0
        with self.assertRaises(ValueError):
            outcome.validate_rows(leaked)

    def test_each_source_game_has_equal_total_training_weight(self):
        rows = corpus()
        weights = outcome.game_weights(rows)
        totals = {}
        for value in rows:
            game = value["source_log"]
            totals[game] = totals.get(game, 0.0) + weights[game]
        self.assertEqual(len(totals), 8)
        for total in totals.values():
            self.assertAlmostEqual(total, 1.0)

    def test_cross_validation_holds_out_whole_seed_families(self):
        rows = corpus()
        report = outcome.cross_validate(rows)
        self.assertEqual(len(report["folds"]), 4)
        self.assertFalse(report["strength_claim_allowed"])
        for fold in report["folds"]:
            self.assertNotIn(fold["holdout_seed"], fold["train_seeds"])
            self.assertEqual(len(fold["train_seeds"]), 3)
            self.assertEqual(fold["model"]["games"], 2)
            self.assertEqual(fold["constant_baseline"]["games"], 2)

    def test_outcome_labels_change_the_frozen_model_but_never_enter_inference_input(self):
        rows = corpus()
        flipped = copy.deepcopy(rows)
        for value in flipped:
            value["labels"]["game_result"] = 1.0 - value["labels"]["game_result"]

        with tempfile.TemporaryDirectory() as folder:
            first = Path(folder) / "first.jsonl"
            second = Path(folder) / "second.jsonl"
            write_rows(first, rows)
            write_rows(second, flipped)
            model_a, _ = outcome.export_checkpoint(first)
            model_b, _ = outcome.export_checkpoint(second)

        self.assertNotEqual(model_a["dataset_sha256"], model_b["dataset_sha256"])
        self.assertNotEqual(model_a["weights"], model_b["weights"])
        prediction_input = rows[0]["model_input"]
        self.assertNotIn("labels", prediction_input)
        self.assertNotIn("game_result", json.dumps(prediction_input))

    def test_export_is_deterministic_and_checkpoint_is_immutable(self):
        rows = corpus()
        with tempfile.TemporaryDirectory() as folder:
            dataset = Path(folder) / "rows.jsonl"
            checkpoint_path = Path(folder) / "model.json"
            write_rows(dataset, rows)
            first, report_a = outcome.export_checkpoint(dataset)
            second, report_b = outcome.export_checkpoint(dataset)
            self.assertEqual(first, second)
            self.assertEqual(report_a, report_b)
            checkpoint_path.write_text(outcome.canonical_json(first) + "\n")
            policy = outcome.load_checkpoint(checkpoint_path)

        self.assertEqual(policy.model_id, first["model_id"])
        self.assertFalse(first["promotion_allowed"])
        self.assertFalse(first["broader_learned_control_allowed"])
        self.assertTrue(first["training_performed"])
        with self.assertRaises(TypeError):
            policy.weights[("x",)] = 1.0

    def test_checkpoint_tampering_and_source_drift_fail_closed(self):
        rows = corpus()
        with tempfile.TemporaryDirectory() as folder:
            dataset = Path(folder) / "rows.jsonl"
            checkpoint_path = Path(folder) / "model.json"
            write_rows(dataset, rows)
            payload, _ = outcome.export_checkpoint(dataset)
            checkpoint_path.write_text(outcome.canonical_json(payload) + "\n")
            outcome.load_checkpoint(checkpoint_path)

            payload["weights"][0][1] += 1.0
            checkpoint_path.write_text(outcome.canonical_json(payload) + "\n")
            with self.assertRaisesRegex(ValueError, "content hash"):
                outcome.load_checkpoint(checkpoint_path)

    def test_prediction_ranks_only_the_supplied_legal_candidates(self):
        rows = corpus()
        weights = outcome.fit(rows)
        prediction = outcome.score_input(weights, rows[0]["model_input"])
        legal = {
            candidate["action_identity"]
            for candidate in rows[0]["model_input"]["candidates"]
        }
        self.assertEqual(
            {item["action_identity"] for item in prediction["candidate_scores"]},
            legal,
        )
        self.assertTrue(set(prediction["top_actions"]) <= legal)
        self.assertNotIn("game_result", json.dumps(prediction))
        self.assertNotIn("run_seed", json.dumps(prediction))


if __name__ == "__main__":
    unittest.main()
