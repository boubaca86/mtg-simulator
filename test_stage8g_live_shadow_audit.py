import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import stage8g_live_shadow_audit as audit


class FakePolicy:
    model_id = "model-test"


CAPTURE = {
    "schema_version": "stage8-capture-v1",
    "decision_index": 7,
    "public_state": {},
    "candidates": [],
}


def recommendation(decision_index=7):
    return {
        "schema_version": "stage8d-shadow-recommendation-v1",
        "model_id": "model-test",
        "mode": "shadow-only",
        "promotion_allowed": False,
        "status": "ranked",
        "decision_index": decision_index,
    }


FORGE_REPORT = {
    "games_completed": 1,
    "returned_actions": 1,
    "controller_acceptance_events": 1,
    "successful_dispatches": 1,
    "failed_dispatches": 0,
    "return_boundary": {"returned_actions": 1},
}


class Stage8GLiveShadowAuditTests(unittest.TestCase):
    def pair_files(self, live):
        temp = tempfile.TemporaryDirectory()
        root = Path(temp.name)
        log = root / "forge.log"
        side = root / "shadow.jsonl"
        log.write_text(
            audit.CAPTURE_PREFIX + json.dumps(CAPTURE, sort_keys=True) +
            "\nGame Result: Game 1 ended in 1 ms. Expert has won!\n"
        )
        side.write_text("\n".join(json.dumps(row, sort_keys=True) for row in live) + "\n")
        self.addCleanup(temp.cleanup)
        return log, side

    @patch("stage8g_live_shadow_audit.audit_stage8f_log", return_value=FORGE_REPORT)
    @patch("stage8g_live_shadow_audit.observe_capture", return_value=recommendation())
    def test_exact_live_replay_passes(self, _, __):
        log, side = self.pair_files([recommendation()])
        report = audit.audit_pair(log, side, 1, FakePolicy())
        self.assertEqual(report["captured_proposals"], 1)
        self.assertEqual(report["live_recommendations"], 1)
        self.assertEqual(report["live_offline_mismatches"], 0)
        self.assertFalse(report["sidecar_control_channel"])
        self.assertFalse(report["promotion_allowed"])

    @patch("stage8g_live_shadow_audit.audit_stage8f_log", return_value=FORGE_REPORT)
    @patch("stage8g_live_shadow_audit.observe_capture", return_value=recommendation())
    def test_live_offline_drift_fails_closed(self, _, __):
        wrong = recommendation()
        wrong["status"] = "forced"
        log, side = self.pair_files([wrong])
        with self.assertRaisesRegex(ValueError, "live/offline recommendation drift"):
            audit.audit_pair(log, side, 1, FakePolicy())

    def test_rejected_live_recommendation_fails_closed(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        path = Path(temp.name) / "shadow.jsonl"
        row = recommendation()
        row["status"] = "rejected"
        row["reason"] = "bad input"
        path.write_text(json.dumps(row) + "\n")
        with self.assertRaisesRegex(ValueError, "rejected capture"):
            audit.live_rows(path)

    @patch("stage8g_live_shadow_audit.audit_stage8f_log", return_value=FORGE_REPORT)
    @patch("stage8g_live_shadow_audit.observe_capture")
    def test_sidecar_count_mismatch_fails_closed(self, observe, _):
        observe.return_value = recommendation()
        log, side = self.pair_files([recommendation(), recommendation()])
        with self.assertRaisesRegex(ValueError, "count mismatch"):
            audit.audit_pair(log, side, 1, FakePolicy())

    def test_aggregate_requires_full_live_coverage(self):
        report = {
            "games_completed": 1,
            "captured_proposals": 2,
            "live_recommendations": 1,
            "live_rejections": 0,
            "live_offline_mismatches": 0,
            "returned_actions": 1,
            "controller_acceptance_events": 1,
            "successful_dispatches": 1,
            "failed_dispatches": 0,
        }
        with self.assertRaisesRegex(ValueError, "coverage"):
            audit.aggregate([report], FakePolicy())


if __name__ == "__main__":
    unittest.main()
