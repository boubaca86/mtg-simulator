import copy
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from stage8n_exploration import DEVELOPMENT_SEEDS, RESERVED_EVALUATION_SEEDS
from stage8n_outcome_trajectories import (
    collect_logs,
    validate_row,
)
from test_stage8l_outcome_trajectories import row as stage8l_row


def fake_stage8l_rows(path: Path):
    seed = int(path.stem.rsplit("-", 1)[-1])
    orientation = "red-first" if "red-first" in path.name else "st-first"

    forge = stage8l_row(seed, learned=False)
    forge["source_log"] = path.name
    forge["trajectory_id"] = f"{path.stem}:decision-0"
    forge["trajectory_step"] = 0
    forge["audit"]["decision_index"] = 0

    exploration = stage8l_row(seed, learned=True)
    exploration["source_log"] = path.name
    exploration["trajectory_id"] = f"{path.stem}:decision-1"
    exploration["trajectory_step"] = 1
    exploration["audit"]["decision_index"] = 1
    exploration["audit"]["run_seed"] = seed + (1 if orientation == "st-first" else 2)
    return [forge, exploration]


class Stage8NOutcomeTrajectoryTests(unittest.TestCase):
    def paths(self, root: Path):
        return [
            root / f"stage8n-controlled-{orientation}-{seed}.log"
            for seed in DEVELOPMENT_SEEDS
            for orientation in ("st-first", "red-first")
        ]

    def test_collection_relabels_exploration_without_calling_it_learned(self):
        with tempfile.TemporaryDirectory() as td:
            paths = self.paths(Path(td))
            with patch(
                "stage8n_outcome_trajectories.collect_stage8l_log",
                side_effect=fake_stage8l_rows,
            ):
                rows, manifest = collect_logs(paths)

        self.assertEqual(manifest["games"], 16)
        self.assertEqual(manifest["trajectory_rows"], 32)
        self.assertEqual(
            manifest["behavior_source_counts"],
            {"exploration-return-boundary": 16, "forge": 16},
        )
        self.assertEqual(manifest["exploration_behavior_rows"], 16)
        self.assertFalse(manifest["exploration_model_guided"])
        self.assertFalse(manifest["exploration_outcome_guided"])
        self.assertFalse(manifest["promotion_allowed"])
        self.assertEqual(
            sorted({row["audit"]["corpus_seed"] for row in rows}),
            list(DEVELOPMENT_SEEDS),
        )
        self.assertTrue(all(
            row["behavior_source"] != "learned-return-boundary"
            for row in rows
        ))

    def test_reserved_seed_is_rejected_before_collection(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / f"stage8n-controlled-st-first-{RESERVED_EVALUATION_SEEDS[0]}.log"
            with patch(
                "stage8n_outcome_trajectories.collect_stage8l_log",
                return_value=[],
            ):
                with self.assertRaises(ValueError):
                    collect_logs([path])

    def test_model_input_leakage_still_fails_closed(self):
        source = fake_stage8l_rows(
            Path(f"stage8n-controlled-st-first-{DEVELOPMENT_SEEDS[0]}.log")
        )[0]
        source["schema_version"] = "stage8n-outcome-trajectory-v1"
        source["audit"]["corpus_seed"] = DEVELOPMENT_SEEDS[0]
        source["audit"]["exploration_rule"] = "first-returned-public-alternative-hash-v1"
        source["model_input"]["public_state"]["game_result"] = 1.0
        with self.assertRaises(ValueError):
            validate_row(source)

    def test_exploration_row_must_actually_differ_from_forge(self):
        source = fake_stage8l_rows(
            Path(f"stage8n-controlled-st-first-{DEVELOPMENT_SEEDS[0]}.log")
        )[1]
        source["schema_version"] = "stage8n-outcome-trajectory-v1"
        source["behavior_source"] = "exploration-return-boundary"
        source["behavior_action"] = source["audit"]["forge_proposed_action"]
        source["audit"]["corpus_seed"] = DEVELOPMENT_SEEDS[0]
        source["audit"]["exploration_rule"] = "first-returned-public-alternative-hash-v1"
        with self.assertRaisesRegex(ValueError, "did not differ"):
            validate_row(source)

    def test_one_game_cannot_cross_seed_families(self):
        source = fake_stage8l_rows(
            Path(f"stage8n-controlled-st-first-{DEVELOPMENT_SEEDS[0]}.log")
        )[0]
        first = copy.deepcopy(source)
        first["schema_version"] = "stage8n-outcome-trajectory-v1"
        first["audit"]["corpus_seed"] = DEVELOPMENT_SEEDS[0]
        first["audit"]["exploration_rule"] = "first-returned-public-alternative-hash-v1"
        self.assertIs(validate_row(first), first)


if __name__ == "__main__":
    unittest.main()
