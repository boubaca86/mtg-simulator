import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from stage8d_shadow_policy import CAPTURE_PREFIX
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8h_lifecycle_audit import TERMINAL_PREFIX
from stage8k_return_boundary_intervention import ARM_PREFIX, CONTROL_PREFIX
from stage8n_exploration import (
    DEVELOPMENT_SEEDS,
    RESERVED_EVALUATION_SEEDS,
    EXPLORATION_RULE,
    PLAN_SCHEMA,
    _choose_from_capture,
    _request_digest_bytes,
    _selection_hash,
    aggregate,
    choose_exploration,
    compare_pair,
)
from test_stage8d_shadow_policy import capture, policy
from test_stage8e_returned_action_audit import returned
from test_stage8h_lifecycle_audit import accepted, terminal


UNTARGETED = (
    "recipe=v2|ability=Untargeted Choice|candidate=2/3|x=<none>|modes=<none>"
    "|targets=<none>|choices=<none>"
)


class Stage8NSelectionTests(unittest.TestCase):
    def test_development_and_reserved_seed_families_are_disjoint(self):
        self.assertEqual(DEVELOPMENT_SEEDS, tuple(range(20261034, 20261042)))
        self.assertEqual(RESERVED_EVALUATION_SEEDS, (20261032, 20261033))
        self.assertTrue(set(DEVELOPMENT_SEEDS).isdisjoint(RESERVED_EVALUATION_SEEDS))

    def fixture_capture(self):
        value = capture()
        value["decision_index"] = 0
        value["public_state"]["decision_index"] = 0
        value["selected_action"] = value["candidates"][0]["action_identity"]
        value["public_state"]["complete_action_identity"] = value["selected_action"]
        extra = copy.deepcopy(value["candidates"][0])
        extra["action_identity"] = UNTARGETED
        extra["target_public_semantics"] = []
        value["candidates"].append(extra)
        return value

    def test_even_seed_prefers_targeted_and_odd_seed_prefers_untargeted(self):
        value = self.fixture_capture()
        even = _choose_from_capture(value, 20261034)
        odd = _choose_from_capture(value, 20261035)
        self.assertTrue(even["exploration_action_targeted"])
        self.assertFalse(odd["exploration_action_targeted"])
        self.assertEqual(even["selection_pool"], "targeted-preferred")
        self.assertEqual(odd["selection_pool"], "untargeted-preferred")

    def test_selection_is_order_invariant_and_not_forge_action(self):
        value = self.fixture_capture()
        expected = _choose_from_capture(value, 20261035)
        reordered = copy.deepcopy(value)
        reordered["candidates"] = list(reversed(reordered["candidates"]))
        actual = _choose_from_capture(reordered, 20261035)
        self.assertEqual(expected["exploration_action"], actual["exploration_action"])
        self.assertEqual(expected["selection_digest"], actual["selection_digest"])
        self.assertNotEqual(expected["exploration_action"], value["selected_action"])

    def test_selection_hash_is_fixed_and_uses_no_outcome(self):
        first = _selection_hash(20261034, 7, UNTARGETED)
        second = _selection_hash(20261034, 7, UNTARGETED)
        self.assertEqual(first, second)
        self.assertEqual(len(first), 64)

    def test_choose_exploration_waits_for_real_return_boundary(self):
        value = self.fixture_capture()
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "baseline-20261034.log"
            path.write_text(CAPTURE_PREFIX + json.dumps(value) + "\n")
            self.assertIsNone(choose_exploration(policy(), path, "Ai(1)-", 20261034))

            ret = returned(0, 0, value["selected_action"])
            ret["acting_player_name"] = value["public_state"]["acting_player_name"]
            path.write_text(
                CAPTURE_PREFIX + json.dumps(value) + "\n"
                + RETURN_PREFIX + json.dumps(ret) + "\n"
            )
            chosen = choose_exploration(policy(), path, "Ai(1)-", 20261034)
            self.assertIsNotNone(chosen)
            self.assertNotEqual(chosen["exploration_action"], value["selected_action"])

    def test_request_is_exactly_bound_to_forge_and_exploration_actions(self):
        chosen = _choose_from_capture(self.fixture_capture(), 20261035)
        raw, digest = _request_digest_bytes(chosen)
        self.assertEqual(hashlib.sha256(raw).hexdigest(), digest)
        self.assertTrue(raw)
        self.assertIn(str(chosen["decision_index"]).encode(), raw)


class Stage8NFullAuditTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.seed = 20261034
        self.baseline = root / f"stage8n-baseline-st-first-{self.seed}.log"
        self.controlled = root / f"stage8n-controlled-st-first-{self.seed}.log"
        self.plan = root / f"stage8n-st-first-{self.seed}.plan.json"
        self.request = root / f"stage8n-st-first-{self.seed}.requests.tsv"
        self.policy = policy()

        self.capture = capture()
        self.capture.update(decision_index=0, matchup_id="fixture")
        self.capture["selected_action"] = self.capture["candidates"][0]["action_identity"]
        self.capture["public_state"].update(
            decision_index=0,
            acting_player=0,
            complete_action_identity=self.capture["selected_action"],
        )

        self.baseline.write_text("\n".join(self.lines()) + "\n")
        self.chosen = choose_exploration(
            self.policy, self.baseline, "Ai(1)-", self.seed
        )
        self.assertIsNotNone(self.chosen)
        raw, digest = _request_digest_bytes(self.chosen)
        self.request.write_bytes(raw)
        self.metadata = {
            "schema_version": PLAN_SCHEMA,
            "mode": "single-deterministic-public-exploration-intervention",
            "exploration_rule": EXPLORATION_RULE,
            "validation_model_id": self.policy.model_id,
            "corpus_seed": self.seed,
            "actor_prefix": "Ai(1)-",
            "intervention_planned": True,
            "intervention": self.chosen,
            "request_file_sha256": digest,
            "model_guided_selection": False,
            "outcome_guided_selection": False,
            "future_state_guided_selection": False,
            "hidden_information_fields": 0,
            "early_search_substitution_allowed": False,
            "forge_phase_deferral_unchanged": True,
            "forge_referee": True,
            "promotion_allowed": False,
            "broader_learned_control_allowed": False,
        }
        self.plan.write_text(json.dumps(self.metadata))
        self.controlled.write_text("\n".join(self.lines(self.chosen)) + "\n")

    @staticmethod
    def encoded(prefix, event):
        return prefix + json.dumps(event)

    def lines(self, intervention=None):
        c = self.capture
        state = c["public_state"]
        action = (
            c["selected_action"]
            if intervention is None
            else intervention["exploration_action"]
        )
        events = [
            "EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3"
        ]
        if intervention:
            events.append(self.encoded(ARM_PREFIX, {
                "schema_version": "stage8k-return-boundary-armed-v1",
                "decision_index": 0,
                "acting_player_name": state["acting_player_name"],
                "requested_action": action,
                "forge_selected_action": c["selected_action"],
                "requested_action_in_candidates": True,
                "early_substitution": False,
                "forge_referee": True,
                "promotion_allowed": False,
            }))
        events += [
            self.encoded("EXPERT_STAGE7_DATA: ", state),
            self.encoded(CAPTURE_PREFIX, c),
        ]
        if intervention:
            events.append(self.encoded(CONTROL_PREFIX, {
                "schema_version": "stage8k-return-boundary-selection-v1",
                "decision_index": 0,
                "acting_player_name": state["acting_player_name"],
                "requested_action": action,
                "forge_selected_action": c["selected_action"],
                "substitution_boundary": "post-forge-plan-pre-return",
                "forge_search_unchanged": True,
                "forge_phase_deferral_unchanged": True,
                "forge_referee": True,
                "promotion_allowed": False,
            }))
        ret = returned(0, 0, action)
        ret["acting_player_name"] = state["acting_player_name"]
        events += [
            self.encoded(RETURN_PREFIX, ret),
            self.encoded(
                ACCEPT_PREFIX,
                accepted(
                    idx=0,
                    priority=0,
                    action=action,
                    actor=state["acting_player_name"],
                    turn=4,
                ),
            ),
            self.encoded(
                TERMINAL_PREFIX,
                terminal(
                    idx=0,
                    priority=0,
                    action=action,
                    actor=state["acting_player_name"],
                    return_turn=4,
                    terminal_turn=4,
                ),
            ),
            "Game Result: Game 1 ended in 111 ms. Ai(1)-Example has won!",
        ]
        return events

    def compare(self):
        return compare_pair(
            self.policy,
            self.baseline,
            self.controlled,
            self.plan,
            self.request,
            "Ai(1)-",
            self.seed,
        )

    def test_valid_public_exploration_chain_passes(self):
        result = self.compare()
        self.assertTrue(result["intervention_applied"])
        self.assertFalse(result["model_guided_selection"])
        self.assertFalse(result["outcome_guided_selection"])
        self.assertEqual(result["failed_dispatches"], 0)
        report = aggregate(
            [result],
            minimum_interventions=1,
            minimum_targeted_interventions=1,
            minimum_untargeted_interventions=0,
        )
        self.assertFalse(report["safety_gate_passed"])
        # A one-game fixture cannot satisfy the full eight-family corpus gate.
        self.assertFalse(report["promotion_allowed"])

    def test_plan_cannot_claim_model_or_outcome_guidance(self):
        for key in ("model_guided_selection", "outcome_guided_selection", "future_state_guided_selection"):
            original = self.metadata[key]
            self.metadata[key] = True
            self.plan.write_text(json.dumps(self.metadata))
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.compare()
            self.metadata[key] = original

    def test_plan_and_request_are_recomputed_not_trusted(self):
        original = copy.deepcopy(self.metadata)
        self.metadata["intervention"]["exploration_action"] = self.metadata["intervention"]["forge_action"]
        self.plan.write_text(json.dumps(self.metadata))
        with self.assertRaises(ValueError):
            self.compare()
        self.metadata = original
        self.plan.write_text(json.dumps(self.metadata))

        self.request.write_bytes(self.request.read_bytes() + b"bad\n")
        with self.assertRaisesRegex(ValueError, "request bytes"):
            self.compare()

    def test_aggregate_requires_action_diversity_and_clean_execution(self):
        base = {
            "corpus_seed": None,
            "intervention_planned": True,
            "intervention_applied": True,
            "intervention_targeted": False,
            "lifecycle_anomalies": 0,
            "failed_dispatches": 0,
            "invalid_requests_accepted": 0,
            "pre_intervention_drift": 0,
            "controlled_side_baseline_score": 0.0,
            "controlled_side_result_score": 0.0,
            "forge_search_unchanged": True,
            "forge_phase_deferral_unchanged": True,
            "forge_referee": True,
            "model_guided_selection": False,
            "outcome_guided_selection": False,
        }
        rows = []
        for seed in DEVELOPMENT_SEEDS:
            for orientation in range(2):
                item = dict(base)
                item["corpus_seed"] = seed
                item["intervention_targeted"] = (seed % 2 == 0 and orientation == 0)
                rows.append(item)
        report = aggregate(
            rows,
            minimum_interventions=12,
            minimum_targeted_interventions=2,
            minimum_untargeted_interventions=6,
        )
        self.assertTrue(report["safety_gate_passed"])
        rows[0]["failed_dispatches"] = 1
        self.assertFalse(
            aggregate(rows, 12, 2, 6)["safety_gate_passed"]
        )


if __name__ == "__main__":
    unittest.main()
