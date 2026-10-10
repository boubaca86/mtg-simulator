import hashlib
import json
import unittest
from stage8w_log_pairing import audit
from stage8w_provenance_manifest import session_id


def fixture():
    a = {"run_seed": 41, "decision_index": 0, "matchup_id": "st-vs-benchmark-red"}
    b = {"schema_version": "stage8v-ai-filtered-top-level-v1",
         "complete_legal_enumeration": False, "target_combinations_enumerated": False,
         "priority_pass_enumerated": False, "selected_status": "proposed_before_execution",
         "run_seed": 41, "decision_index": 0,
         "top_level_candidate_count": 3, "proposed_candidate_index": 1}
    log = ("EXPERT_STAGE7_DATA: " + json.dumps(a) + "\n"
           + "EXPERT_STAGE8V_TOP_LEVEL: " + json.dumps(b) + "\n").encode()
    m = {"github_run_id": 1, "github_run_attempt": 1, "job_id": "game",
         "game_ordinal": 0, "game_count": 1, "orientation": "st-first",
         "matchup_id": "st-vs-benchmark-red", "run_seed": 41,
         "log_sha256": hashlib.sha256(log).hexdigest()}
    return log, m


class Tests(unittest.TestCase):
    def test_valid(self):
        log, m = fixture()
        self.assertEqual(audit(log, m)["decisions"], 1)

    def test_distinct_attempts(self):
        _, m = fixture()
        first = session_id(m)
        m["github_run_attempt"] = 2
        self.assertNotEqual(first, session_id(m))

    def test_reject_multiple_games(self):
        log, m = fixture()
        m["game_count"] = 2
        with self.assertRaises(ValueError):
            audit(log, m)

    def test_reject_tamper(self):
        log, m = fixture()
        with self.assertRaises(ValueError):
            audit(log + b"x", m)

    def test_reject_wrong_matchup(self):
        log, m = fixture()
        m["matchup_id"] = "wrong"
        with self.assertRaises(ValueError):
            audit(log, m)


if __name__ == "__main__":
    unittest.main()
