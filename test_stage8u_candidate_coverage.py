import copy
import unittest
from stage8u_candidate_coverage import SCHEMA, validate_event, coverage, report_sha256


def sample():
    return {
        "schema_version": SCHEMA, "run_seed": 999, "decision_index": 0,
        "legal_candidates": [
            {"identity": "a", "effect": "gain_life", "targets": [{"role": "self", "kind": "player"}]},
            {"identity": "b", "effect": "pump", "targets": [{"role": "self", "kind": "card"}]},
            {"identity": "c", "effect": "damage", "targets": [{"role": "opponent", "kind": "player"}]},
            {"identity": "pass", "effect": "none", "targets": []},
        ], "selected_identity": "a", "legal_candidate_count": 4,
    }


class CoverageContractTests(unittest.TestCase):
    def test_available_not_confused_with_selected(self):
        x = coverage([sample()])
        self.assertEqual(x["available"]["pump_to_self_card"], 1)
        self.assertNotIn("pump_to_self_card", x["selected"])
        self.assertEqual(x["selected"]["gain_life_to_self_player"], 1)

    def test_self_target_is_not_removed(self):
        event = sample()
        event["selected_identity"] = "b"
        self.assertEqual(coverage([event])["selected"]["pump_to_self_card"], 1)

    def test_no_fabricated_counterfactual(self):
        event = sample()
        event["legal_candidates"][0]["outcome"] = "win"
        with self.assertRaises(ValueError):
            validate_event(event)

    def test_hidden_info_rejected(self):
        event = sample()
        event["opponent_hand"] = ["Secret"]
        with self.assertRaises(ValueError):
            validate_event(event)

    def test_duplicate_identity_rejected(self):
        event = sample()
        event["legal_candidates"][1]["identity"] = "a"
        with self.assertRaises(ValueError):
            validate_event(event)

    def test_selected_must_be_legal(self):
        event = sample()
        event["selected_identity"] = "not legal"
        with self.assertRaises(ValueError):
            validate_event(event)

    def test_candidate_count_must_match(self):
        event = sample()
        event["legal_candidate_count"] = 5
        with self.assertRaises(ValueError):
            validate_event(event)

    def test_duplicate_decisions_rejected(self):
        with self.assertRaises(ValueError):
            coverage([sample(), copy.deepcopy(sample())])

    def test_deterministic_digest(self):
        self.assertEqual(report_sha256([sample()]), report_sha256([sample()]))


if __name__ == "__main__":
    unittest.main()
