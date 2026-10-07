import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from stage8l_outcome_trajectories import (
    EXPLORATION_SEEDS,
    collect_log,
    collect_logs,
    validate_model_input,
    validate_trajectory_row,
)


FORGE = "recipe=v2|ability=Forge Choice|targets=<none>|choices=<none>"
LEARNED = "recipe=v2|ability=Learned Choice|targets=[Public Target]|choices=<none>"


def safe_input():
    return {
        "public_state": {
            "phase": "MAIN1",
            "acting_player_name": "Ai(1)-ST Forge Full",
            "acting_life": 20,
            "opponent_life": 20,
            "turn": 3,
            "opponent_unknown_hand_count": 7,
            "own_library_count": 53,
            "opponent_library_count": 53,
            "own_hand": [],
            "own_battlefield": [],
            "opponent_battlefield": [],
            "own_graveyard": [],
            "opponent_graveyard": [],
            "exile_public": [],
            "stack_public": [],
            "own_hand_semantics": [],
            "own_battlefield_semantics": [],
            "opponent_battlefield_semantics": [],
            "own_graveyard_semantics": [],
            "opponent_graveyard_semantics": [],
            "exile_public_semantics": [],
            "stack_public_semantics": [],
        },
        "candidates": [
            {
                "action_identity": FORGE,
                "target_semantics_version": "forge-public-targets-v2",
                "target_public_semantics": [],
            },
            {
                "action_identity": LEARNED,
                "target_semantics_version": "forge-public-targets-v2",
                "target_public_semantics": [
                    "zone=opponent_battlefield|role=opponent|Public Target|mv=2|type=Creature|p=2|t=2"
                ],
            },
        ],
    }


def row(seed=20261028, learned=True):
    behavior = LEARNED if learned else FORGE
    return {
        "schema_version": "stage8l-outcome-trajectory-v1",
        "trajectory_id": f"fixture-{seed}:decision-4",
        "source_log": f"stage8l-controlled-st-first-{seed}.log",
        "source_log_sha256": "0" * 64,
        "trajectory_step": 2,
        "model_input": safe_input(),
        "behavior_action": behavior,
        "behavior_source": "learned-return-boundary" if learned else "forge",
        "behavior_action_targeted": learned,
        "labels": {
            "game_result": 1.0,
            "terminal_action_outcome": "resolved",
        },
        "audit": {
            "run_seed": seed,
            "decision_index": 4,
            "matchup_id": "st-vs-benchmark-red",
            "acting_player": 0,
            "acting_player_name": "Ai(1)-ST Forge Full",
            "turn": 3,
            "phase": "MAIN1",
            "forge_proposed_action": FORGE,
            "forge_referee": True,
            "return_boundary_verified": True,
            "controller_acceptance_verified": True,
            "terminal_binding_verified": True,
            "hidden_information_fields": 0,
            "promotion_allowed": False,
        },
    }


class Stage8LOutcomeTrajectoryTests(unittest.TestCase):
    def test_model_input_rejects_labels_scores_and_hidden_state(self):
        good = safe_input()
        self.assertIs(validate_model_input(good), good)

        leaked_label = safe_input()
        leaked_label["public_state"]["game_result"] = 1.0
        with self.assertRaises(ValueError):
            validate_model_input(leaked_label)

        leaked_score = safe_input()
        leaked_score["candidates"][0]["aggregate_score"] = 999
        with self.assertRaises(ValueError):
            validate_model_input(leaked_score)

        hidden = safe_input()
        hidden["public_state"]["opponent_hand"] = ["SECRET"]
        with self.assertRaises(ValueError):
            validate_model_input(hidden)

    def test_labels_and_behavior_are_separate_from_model_input(self):
        value = row()
        self.assertIs(validate_trajectory_row(value), value)
        self.assertNotIn("game_result", value["model_input"]["public_state"])
        self.assertNotIn("behavior_action", value["model_input"])
        self.assertNotIn("forge_proposed_action", value["model_input"])

    def test_behavior_must_be_in_forge_legal_candidate_set(self):
        value = row()
        value["behavior_action"] = "recipe=v2|ability=Invented|targets=<none>|choices=<none>"
        with self.assertRaises(ValueError):
            validate_trajectory_row(value)

    def test_lifecycle_anomaly_is_not_training_data(self):
        value = row()
        value["labels"]["terminal_action_outcome"] = "dispatch-failed"
        with self.assertRaises(ValueError):
            validate_trajectory_row(value)

    def test_collect_log_binds_real_control_path_and_game_outcome(self):
        learned = "recipe=v2|ability=Learned Untargeted|targets=<none>|choices=<none>"
        state = dict(safe_input()["public_state"])
        state.update({
            "schema_version": "stage7d-v1",
            "run_seed": 20261028,
            "decision_index": 4,
            "acting_player": 0,
            "infoset_sample_count": 3,
            "matchup_id": "st-vs-benchmark-red",
            "complete_action_identity": FORGE,
            "search_policy": "fixed-root-v1",
        })
        candidates = [
            {
                "action_identity": FORGE,
                "replay_valid_count": 3,
                "target_semantics_version": "forge-public-targets-v2",
                "target_public_semantics": [],
            },
            {
                "action_identity": learned,
                "replay_valid_count": 3,
                "target_semantics_version": "forge-public-targets-v2",
                "target_public_semantics": [],
            },
        ]
        capture = {
            "schema_version": "stage8-capture-v1",
            "decision_index": 4,
            "run_seed": 20261028,
            "matchup_id": "st-vs-benchmark-red",
            "public_state": state,
            "selected_action": FORGE,
            "candidates": candidates,
            "information_set_samples": 3,
        }
        stage7 = dict(state)
        arm = {
            "schema_version": "stage8k-return-boundary-armed-v1",
            "decision_index": 4,
            "requested_action": learned,
            "forge_selected_action": FORGE,
        }
        control = {
            "schema_version": "stage8k-return-boundary-selection-v1",
            "decision_index": 4,
            "requested_action": learned,
            "forge_selected_action": FORGE,
            "forge_search_unchanged": True,
            "forge_phase_deferral_unchanged": True,
        }
        returned = {
            "schema_version": "stage8e-returned-action-v1",
            "capture_decision_index": 4,
            "priority_return_index": 0,
            "action_identity": learned,
        }
        accepted = {
            "schema_version": "stage8f-controller-acceptance-v1",
            "capture_decision_index": 4,
            "priority_return_index": 0,
            "action_identity": learned,
            "dispatch_success": True,
        }
        terminal = {
            "schema_version": "stage8h-action-terminal-v1",
            "capture_decision_index": 4,
            "priority_return_index": 0,
            "action_identity": learned,
            "outcome": "resolved",
        }

        lines = [
            "EXPERT_STAGE7_DATA: " + json.dumps(stage7),
            "EXPERT_STAGE8_CAPTURE: " + json.dumps(capture),
            "EXPERT_STAGE8K_CONTROL_ARMED: " + json.dumps(arm),
            "EXPERT_STAGE8K_RETURN_BOUNDARY_SELECTION: " + json.dumps(control),
            "EXPERT_STAGE8_RETURNED_ACTION: " + json.dumps(returned),
            "EXPERT_STAGE8_CONTROLLER_ACCEPTANCE: " + json.dumps(accepted),
            "EXPERT_STAGE8H_ACTION_TERMINAL: " + json.dumps(terminal),
            "EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3",
            "Game Result: Game 1 ended in 10 ms. Ai(1)-ST Forge Full has won!",
        ]

        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "stage8l-controlled-st-first-20261028.log"
            path.write_text("\n".join(lines) + "\n")
            rows = collect_log(path)

        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0]["behavior_action"], learned)
        self.assertEqual(rows[0]["behavior_source"], "learned-return-boundary")
        self.assertEqual(rows[0]["labels"]["game_result"], 1.0)
        self.assertEqual(rows[0]["labels"]["terminal_action_outcome"], "resolved")
        self.assertNotIn("run_seed", rows[0]["model_input"]["public_state"])
        self.assertNotIn("aggregate_score", rows[0]["model_input"]["candidates"][0])

    def test_seed_manifest_keeps_future_evaluation_families_out(self):
        with tempfile.TemporaryDirectory() as td:
            paths = [Path(td) / f"game-{seed}.log" for seed in EXPLORATION_SEEDS]
            for path in paths:
                path.write_text("fixture")

            fake_rows = {
                str(path): [row(seed, learned=(seed == EXPLORATION_SEEDS[0]))]
                for path, seed in zip(paths, EXPLORATION_SEEDS)
            }

            def fake_collect(path):
                return fake_rows[str(path)]

            with patch("stage8l_outcome_trajectories.collect_log", side_effect=fake_collect):
                rows, manifest = collect_logs(paths)

            self.assertEqual(len(rows), len(EXPLORATION_SEEDS))
            self.assertEqual(
                manifest["exploration_seed_families"], list(EXPLORATION_SEEDS)
            )
            self.assertEqual(
                manifest["reserved_untouched_evaluation_seed_families"],
                [20261032, 20261033],
            )
            self.assertFalse(manifest["training_performed"])
            self.assertFalse(manifest["promotion_allowed"])
            self.assertFalse(manifest["broader_learned_control_allowed"])


if __name__ == "__main__":
    unittest.main()
