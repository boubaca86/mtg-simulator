import base64
import json
import tempfile
import unittest
from pathlib import Path

from stage8i_control_replay import (
    CAPTURE_PREFIX, aggregate, extract_requests, load_requests
)


def capture(idx, selected=None):
    selected = selected or (
        f"recipe=v2|ability=Action{idx}|candidate=0/1|x=<none>|"
        "modes=<none>|targets=<none>|choices=<none>"
    )
    return {
        "schema_version": "stage8-capture-v1",
        "decision_index": idx,
        "selected_action": selected,
        "candidates": [{"action_identity": selected}],
    }


class Stage8IControlReplayTests(unittest.TestCase):
    def test_extract_round_trip_contains_only_index_and_identity(self):
        with tempfile.TemporaryDirectory() as td:
            source = Path(td) / "baseline.log"
            request = Path(td) / "requests.tsv"
            rows = [capture(0), capture(1)]
            source.write_text(
                "".join(CAPTURE_PREFIX + json.dumps(row) + "\n" for row in rows)
            )
            meta = extract_requests(source, request)
            loaded, digest = load_requests(request)
            self.assertEqual(loaded, {
                0: rows[0]["selected_action"],
                1: rows[1]["selected_action"],
            })
            self.assertEqual(meta["request_file_sha256"], digest)
            self.assertEqual(meta["hidden_information_fields"], 0)
            for line in request.read_text().splitlines():
                idx, encoded = line.split("\t")
                self.assertTrue(idx.isdigit())
                identity = base64.urlsafe_b64decode(encoded.encode()).decode()
                self.assertIn("recipe=v2|", identity)

    def test_extract_rejects_noncontiguous_indices(self):
        with tempfile.TemporaryDirectory() as td:
            source = Path(td) / "baseline.log"
            request = Path(td) / "requests.tsv"
            source.write_text(
                CAPTURE_PREFIX + json.dumps(capture(1)) + "\n"
            )
            with self.assertRaisesRegex(ValueError, "contiguous"):
                extract_requests(source, request)

    def test_extract_rejects_selected_action_outside_candidates(self):
        with tempfile.TemporaryDirectory() as td:
            source = Path(td) / "baseline.log"
            request = Path(td) / "requests.tsv"
            row = capture(0)
            row["candidates"] = [{"action_identity": "different"}]
            source.write_text(CAPTURE_PREFIX + json.dumps(row) + "\n")
            with self.assertRaisesRegex(ValueError, "not in captured candidates"):
                extract_requests(source, request)

    def test_load_rejects_duplicate_indices(self):
        with tempfile.TemporaryDirectory() as td:
            request = Path(td) / "requests.tsv"
            encoded = base64.urlsafe_b64encode(b"recipe=v2|ability=A").decode()
            request.write_text(f"0\t{encoded}\n0\t{encoded}\n")
            with self.assertRaisesRegex(ValueError, "duplicate"):
                load_requests(request)

    def test_gate_requires_exact_request_coverage(self):
        pair = {
            "requests": 3,
            "control_events": 3,
            "captures_reproduced_exactly": 3,
            "returned_actions_reproduced_exactly": 2,
            "controller_acceptances_reproduced_exactly": 2,
            "terminal_events_reproduced_exactly": 2,
            "game_results_reproduced_exactly": 1,
            "invalid_or_uncaptured_requests_accepted": 0,
            "different_from_forge_requests_accepted": 0,
        }
        self.assertTrue(aggregate([pair])["gate_passed"])
        bad = dict(pair)
        bad["control_events"] = 2
        self.assertFalse(aggregate([bad])["gate_passed"])


if __name__ == "__main__":
    unittest.main()
