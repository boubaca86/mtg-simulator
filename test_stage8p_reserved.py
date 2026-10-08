"""Stage 8P public-only selection, seed isolation and one-shot gate regressions."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

from stage8d_shadow_policy import CAPTURE_PREFIX
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8h_lifecycle_audit import TERMINAL_PREFIX
from stage8k_return_boundary_intervention import ARM_PREFIX, CONTROL_PREFIX
from stage8p_reserved_audit import compare_pair
from stage8p_reserved_planner import (
    MODEL_ID, SEEDS, _recommend, baseline_identity, choose, expected_plan, plan,
)
from stage8p_reserved_gate import aggregate
from test_stage8d_shadow_policy import capture, policy
from test_stage8e_returned_action_audit import returned
from test_stage8h_lifecycle_audit import accepted, terminal


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
        self.assertEqual(_recommend(self.model, self.validator, reversed_capture)['learned_action'],
                         chosen['learned_action'])

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


class Stage8PPairAuditTests(unittest.TestCase):
    """Exercise real planner/auditors together; all game records are synthetic.

    Reserved filenames only exercise the contract. No reserved Forge games,
    frozen evaluation outputs, or mocked audit functions are used here.
    """

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.validator = policy()
        self.model = {"model_id": MODEL_ID, "weights": {"action_cast": 4.0}}

    @staticmethod
    def encode(prefix, event):
        return prefix + json.dumps(event)

    def game_lines(self, cap, chosen=None, winner="Ai(1)-Example"):
        state = cap["public_state"]
        idx = cap["decision_index"]
        action = cap["selected_action"] if chosen is None else chosen["learned_action"]
        lines = ["EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3"]
        if chosen:
            # Real Forge may arm before it emits the unchanged search capture.
            lines.append(self.encode(ARM_PREFIX, {
                "schema_version": "stage8k-return-boundary-armed-v1",
                "decision_index": idx, "acting_player_name": state["acting_player_name"],
                "requested_action": action, "forge_selected_action": cap["selected_action"],
                "requested_action_in_candidates": True, "early_substitution": False,
                "forge_referee": True, "promotion_allowed": False,
            }))
        lines += [self.encode("EXPERT_STAGE7_DATA: ", state), self.encode(CAPTURE_PREFIX, cap)]
        if chosen:
            lines.append(self.encode(CONTROL_PREFIX, {
                "schema_version": "stage8k-return-boundary-selection-v1",
                "decision_index": idx, "acting_player_name": state["acting_player_name"],
                "requested_action": action, "forge_selected_action": cap["selected_action"],
                "substitution_boundary": "post-forge-plan-pre-return",
                "forge_search_unchanged": True, "forge_phase_deferral_unchanged": True,
                "forge_referee": True, "promotion_allowed": False,
            }))
        ret = returned(0, idx, action)
        ret["acting_player_name"] = state["acting_player_name"]
        lines += [
            self.encode(RETURN_PREFIX, ret),
            self.encode(ACCEPT_PREFIX, accepted(idx=idx, priority=0, action=action,
                actor=state["acting_player_name"], turn=4)),
            self.encode(TERMINAL_PREFIX, terminal(idx=idx, priority=0, action=action,
                actor=state["acting_player_name"], return_turn=4, terminal_turn=4)),
            f"Game Result: Game 1 ended in 100 ms. {winner} has won!",
        ]
        return lines

    def write_pair(self, seed=20261032, orientation="st-first", *, targeted=True,
                   baseline_winner="Ai(2)-Opponent", controlled_winner="Ai(1)-Example"):
        stem = f"{orientation}-{seed}"
        paths = (self.root / f"stage8p-baseline-{stem}.log",
                 self.root / f"stage8p-controlled-{stem}.log",
                 self.root / f"stage8p-{stem}.plan.json",
                 self.root / f"stage8p-{stem}.requests.tsv")
        baseline, controlled, metadata, request = paths
        cap = capture()
        cap.update(decision_index=0, matchup_id="synthetic-" + orientation)
        first, second = cap["candidates"]
        second["action_identity"] = second["action_identity"].replace(
            "ability=Remove target creature", "ability=Cast creature")
        if not targeted:
            second["action_identity"] = second["action_identity"].replace(
                "targets=[Goblin (99)]", "targets=<none>")
            second["target_public_semantics"] = []
        cap["selected_action"] = first["action_identity"]
        cap["public_state"].update(decision_index=0, acting_player=0,
                                    complete_action_identity=first["action_identity"])
        baseline.write_text("\n".join(self.game_lines(cap, winner=baseline_winner)) + "\n")
        planned = plan(self.model, self.validator, baseline, request, metadata)
        controlled.write_text("\n".join(self.game_lines(
            cap, planned["intervention"], controlled_winner)) + "\n")
        return paths

    def compare(self, paths):
        return compare_pair(self.model, self.validator, *paths)

    def change_event(self, path, prefix, **updates):
        lines = path.read_text().splitlines()
        for i, line in enumerate(lines):
            if line.startswith(prefix):
                event = json.loads(line[len(prefix):])
                event.update(updates)
                lines[i] = self.encode(prefix, event)
        path.write_text("\n".join(lines) + "\n")

    def test_four_complete_pairs_allow_exact_learned_substitutions(self):
        pairs = []
        for seed in SEEDS:
            for orientation in ("st-first", "red-first"):
                won = seed == SEEDS[0] and orientation == "st-first"
                paths = self.write_pair(seed, orientation, targeted=orientation == "st-first",
                    controlled_winner="Ai(1)-Example" if won else "Ai(2)-Opponent")
                pairs.append(self.compare(paths))
        report = aggregate(pairs)
        self.assertTrue(report["safety_gate_passed"])
        self.assertTrue(report["evaluation_gate_passed"])
        self.assertEqual(report["totals"]["interventions_applied"], 4)
        self.assertEqual(report["totals"]["targeted_interventions_applied"], 2)
        self.assertEqual(report["totals"]["paired_score_delta"], 1.0)
        self.assertFalse(report["promotion_allowed"])
        self.assertFalse(report["broader_learned_control_allowed"])
        self.assertFalse(report["strength_claim_allowed"])

    def test_safe_interventions_without_game_gain_fail_result_gate(self):
        pairs = [self.compare(self.write_pair(seed, orientation,
                    controlled_winner="Ai(2)-Opponent"))
                 for seed in SEEDS for orientation in ("st-first", "red-first")]
        report = aggregate(pairs)
        self.assertTrue(report["safety_gate_passed"])
        self.assertEqual(report["totals"]["paired_score_delta"], 0.0)
        self.assertFalse(report["primary_game_result_gate_passed"])
        self.assertFalse(report["evaluation_gate_passed"])

    def test_tied_model_abstains_and_requires_identical_result(self):
        self.model["weights"] = {}
        paths = self.write_pair(controlled_winner="Ai(2)-Opponent")
        self.assertEqual(paths[3].read_bytes(), b"")
        pair = self.compare(paths)
        self.assertFalse(pair["intervention_planned"])
        self.assertFalse(pair["intervention_applied"])
        self.assertEqual(pair["paired_score_delta"], 0.0)
        paths[1].write_text(paths[1].read_text().replace(
            "Ai(2)-Opponent has won!", "Ai(1)-Example has won!"))
        with self.assertRaisesRegex(ValueError, "no-intervention game changed"):
            self.compare(paths)

    def test_baseline_still_rejects_a_non_forge_return(self):
        paths = self.write_pair()
        chosen = json.loads(paths[2].read_text())["intervention"]
        self.change_event(paths[0], RETURN_PREFIX, action_identity=chosen["learned_action"])
        with self.assertRaisesRegex(ValueError, "differs from Forge captured proposal"):
            plan(self.model, self.validator, paths[0], paths[3], paths[2])

    def test_controlled_failed_dispatch_is_rejected(self):
        paths = self.write_pair()
        self.change_event(paths[1], ACCEPT_PREFIX, dispatch_success=False, skip_after_dispatch=True)
        self.change_event(paths[1], TERMINAL_PREFIX, outcome="dispatch-failed",
                          stack_based=False, fizzled=None)
        with self.assertRaisesRegex(ValueError, "failed dispatch"):
            self.compare(paths)

    def test_failed_dispatch_cannot_claim_successful_resolution(self):
        paths = self.write_pair()
        self.change_event(paths[1], ACCEPT_PREFIX, dispatch_success=False, skip_after_dispatch=True)
        with self.assertRaisesRegex(ValueError, "resolved action disagrees with controller dispatch"):
            self.compare(paths)

    def test_controlled_missing_terminal_is_rejected(self):
        paths = self.write_pair()
        paths[1].write_text("\n".join(line for line in paths[1].read_text().splitlines()
                                     if not line.startswith(TERMINAL_PREFIX)) + "\n")
        with self.assertRaisesRegex(ValueError, "lack terminal coverage"):
            self.compare(paths)

    def test_controlled_action_identity_drift_is_rejected(self):
        for prefix in (RETURN_PREFIX, ACCEPT_PREFIX, TERMINAL_PREFIX):
            with self.subTest(boundary=prefix):
                paths = self.write_pair()
                original = json.loads(paths[2].read_text())["intervention"]["forge_action"]
                self.change_event(paths[1], prefix, action_identity=original)
                with self.assertRaisesRegex(ValueError, "identity drift|binding drift"):
                    self.compare(paths)

    def test_unplanned_substitution_is_rejected(self):
        paths = self.write_pair()
        self.change_event(paths[1], CONTROL_PREFIX, requested_action="unplanned-action")
        with self.assertRaisesRegex(ValueError, "differs from predeclared intervention"):
            self.compare(paths)

    def test_pre_intervention_public_state_drift_is_rejected(self):
        paths = self.write_pair()
        cap = next(json.loads(line[len(CAPTURE_PREFIX):])
                   for line in paths[1].read_text().splitlines() if line.startswith(CAPTURE_PREFIX))
        cap["public_state"]["acting_life"] -= 1
        self.change_event(paths[1], CAPTURE_PREFIX, public_state=cap["public_state"])
        self.change_event(paths[1], "EXPERT_STAGE7_DATA: ", acting_life=cap["public_state"]["acting_life"])
        with self.assertRaisesRegex(ValueError, "drifted before intervention"):
            self.compare(paths)

    def test_altered_plan_and_request_are_rejected(self):
        for kind in ("plan", "request"):
            with self.subTest(kind=kind):
                paths = self.write_pair()
                if kind == "plan":
                    payload = json.loads(paths[2].read_text())
                    payload["intervention"]["predicted_margin"] += 0.01
                    paths[2].write_text(json.dumps(payload))
                else:
                    paths[3].write_bytes(paths[3].read_bytes() + b"\n")
                with self.assertRaisesRegex(ValueError, "differs from frozen public rule"):
                    self.compare(paths)


if __name__ == "__main__":
    unittest.main()
