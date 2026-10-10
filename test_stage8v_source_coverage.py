import unittest
from stage8v_source_coverage import validate_event, summarize


def event():
    return {
        "schema_version": "stage8v-ai-filtered-top-level-v1",
        "source": "SpellAbilityPicker.getCandidateSpellsAndAbilities",
        "complete_legal_enumeration": False,
        "target_combinations_enumerated": False,
        "priority_pass_enumerated": False,
        "selected_status": "proposed_before_execution",
        "run_seed": 12,
        "decision_index": 7,
        "top_level_candidate_count": 4,
        "proposed_candidate_index": 2,
    }


class CoverageTests(unittest.TestCase):
    def test_valid(self):
        self.assertEqual(summarize([event()])["decisions"], 1)

    def test_nonexhaustive(self):
        e = event()
        e["complete_legal_enumeration"] = True
        with self.assertRaises(ValueError):
            validate_event(e)

    def test_not_executed(self):
        e = event()
        e["selected_status"] = "executed"
        with self.assertRaises(ValueError):
            validate_event(e)

    def test_no_unknown_fields(self):
        e = event()
        e["opponent_hand"] = []
        with self.assertRaises(ValueError):
            validate_event(e)

    def test_invalid_index(self):
        e = event()
        e["proposed_candidate_index"] = 4
        with self.assertRaises(ValueError):
            validate_event(e)

    def test_no_empty_success(self):
        with self.assertRaises(ValueError):
            summarize([])


if __name__ == "__main__":
    unittest.main()
