import base64
import hashlib
import tempfile
import unittest
from pathlib import Path

from stage8k_return_boundary_intervention import (
    _is_targeted,
    _write_request,
    aggregate,
)


class Stage8KReturnBoundaryTests(unittest.TestCase):
    def test_targeted_identity_detection(self):
        self.assertFalse(_is_targeted(
            "recipe=v2|ability=Servitor|targets=<none>|choices=<none>"
        ))
        self.assertTrue(_is_targeted(
            "recipe=v2|ability=Lightning Strike|targets=[Servitor (117)]|choices=<none>"
        ))
        with self.assertRaises(ValueError):
            _is_targeted("recipe=v2|ability=Malformed|choices=<none>")

    def test_request_binds_expected_forge_and_learned_identities(self):
        forge = "recipe=v2|ability=Forge Choice|targets=<none>|choices=<none>"
        learned = "recipe=v2|ability=Learned Choice|targets=[Target (9)]|choices=<none>"
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "request.tsv"
            digest = _write_request(path, 7, forge, learned)
            raw = path.read_bytes()
            self.assertEqual(hashlib.sha256(raw).hexdigest(), digest)
            parts = raw.decode().strip().split("\t")
            self.assertEqual(len(parts), 3)
            self.assertEqual(parts[0], "7")
            self.assertEqual(
                base64.urlsafe_b64decode(parts[1]).decode(), forge
            )
            self.assertEqual(
                base64.urlsafe_b64decode(parts[2]).decode(), learned
            )

    def test_gate_requires_targeted_return_boundary_coverage(self):
        good = {
            "intervention_planned": True,
            "intervention_applied": True,
            "intervention_targeted": True,
            "lifecycle_anomalies": 0,
            "invalid_requests_accepted": 0,
            "pre_intervention_drift": 0,
            "forge_search_unchanged": True,
            "forge_phase_deferral_unchanged": True,
            "controlled_side_baseline_score": 0.0,
            "controlled_side_result_score": 1.0,
        }
        report = aggregate([dict(good) for _ in range(4)], 4, 1)
        self.assertTrue(report["safety_gate_passed"])
        self.assertEqual(report["totals"]["targeted_interventions_applied"], 4)
        self.assertFalse(report["promotion_allowed"])
        self.assertFalse(report["broader_learned_control_allowed"])

        untargeted = [dict(good, intervention_targeted=False) for _ in range(4)]
        self.assertFalse(aggregate(untargeted, 4, 1)["safety_gate_passed"])

    def test_gate_fails_if_forge_phase_decision_was_changed(self):
        good = {
            "intervention_planned": True,
            "intervention_applied": True,
            "intervention_targeted": True,
            "lifecycle_anomalies": 0,
            "invalid_requests_accepted": 0,
            "pre_intervention_drift": 0,
            "forge_search_unchanged": True,
            "forge_phase_deferral_unchanged": True,
            "controlled_side_baseline_score": 1.0,
            "controlled_side_result_score": 1.0,
        }
        rows = [dict(good) for _ in range(4)]
        rows[2]["forge_phase_deferral_unchanged"] = False
        self.assertFalse(aggregate(rows, 4, 1)["safety_gate_passed"])


if __name__ == "__main__":
    unittest.main()
