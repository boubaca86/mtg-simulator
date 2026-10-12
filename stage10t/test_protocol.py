"""Real worker-process tests of the new own-hand request contract."""
import copy
import json
from pathlib import Path
import selectors
import subprocess
import sys
import unittest

from stage10h.test_protocol import invalid_messages, view

WORKER = Path(__file__).with_name("own_hand_actor.py")


def request(hand=None):
    data = view()
    data["own_cards"]["hand"] = hand if hand is not None else [
        "Counterspell", "Shock", "Forest", "Forest", "Grizzly Bears",
        "Forest", "Island", "Mountain", "Grizzly Bears"]
    return {"request": "own_hand_order", "observation": data}


class OwnHandProtocolTests(unittest.TestCase):
    def launch(self):
        actor = subprocess.Popen([sys.executable, "-I", "-S", "-u", str(WORKER)],
                                 stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                 stderr=subprocess.PIPE, text=True, env={})
        def close():
            actor.stdin.close()
            try:
                actor.wait(timeout=3)
            except subprocess.TimeoutExpired:
                actor.kill()
                actor.wait(timeout=3)
            actor.stdout.close()
            actor.stderr.close()
        self.addCleanup(close)
        return actor

    def ask(self, actor, payload):
        actor.stdin.write(json.dumps(payload, separators=(",", ":")) + "\n")
        actor.stdin.flush()
        with selectors.DefaultSelector() as ready:
            ready.register(actor.stdout, selectors.EVENT_READ)
            self.assertTrue(ready.select(5), "actor did not answer within test deadline")
        line = actor.stdout.readline()
        self.assertTrue(line)
        return json.loads(line)

    def denied(self, payload):
        raw = payload if isinstance(payload, bytes) else (
            payload if isinstance(payload, str) else json.dumps(payload)).encode() + b"\n"
        proc = subprocess.run([sys.executable, "-I", "-S", "-u", str(WORKER)],
                              input=raw, capture_output=True, timeout=5, env={})
        self.assertNotEqual(proc.returncode, 0)
        self.assertEqual(proc.stdout, b"")

    def test_two_independent_workers_return_only_complete_own_hand_permutation(self):
        a, b = self.launch(), self.launch()
        self.assertNotEqual(a.pid, b.pid)
        expected = {"intent": "hand_order", "hand_priority": [0, 1, 5, 6, 7, 4, 8, 2, 3]}
        for actor in (a, b):
            self.assertEqual(self.ask(actor, request()), expected)
            self.assertEqual(self.ask(actor, request()), expected)

    def test_ordinary_plan_and_opening_hand_remain_compatible(self):
        actor = self.launch()
        plan = self.ask(actor, view())
        self.assertEqual(set(plan), {"intent", "land_hand_indices", "creature_hand_indices", "attack_table_indices"})
        self.assertEqual(plan["intent"], "action_plan")
        opening = self.ask(actor, {"request": "own_opening_hand", "observation": view()})
        self.assertEqual(set(opening), {"intent", "keep", "bottom_priority"})
        self.assertEqual(opening["intent"], "opening_hand")

    def test_order_depends_only_on_own_hand(self):
        actor = self.launch()
        before = self.ask(actor, request())
        changed = request()
        changed["observation"]["opponent_table"] = [{"appearance": "face_down"}]
        changed["observation"]["life_totals"] = {"self": -4, "opponent": 100}
        changed["observation"]["own_cards"]["decklist_multiset"] = [["Island", 60]]
        self.assertEqual(self.ask(actor, changed), before)

    def test_empty_and_largest_hand_are_bounded(self):
        actor = self.launch()
        for hand in ([], ["Forest"] * 128):
            answer = self.ask(actor, request(hand))
            self.assertEqual(sorted(answer["hand_priority"]), list(range(len(hand))))
        self.denied(request(["Forest"] * 129))

    def test_shared_strict_json_negatives(self):
        for label, raw, _ in invalid_messages():
            with self.subTest(label=label):
                self.denied(raw)

    def test_request_never_accepts_phase_count_or_hidden_metadata(self):
        for key in ("phase", "count", "turn", "stack", "priority", "opponent_hand", "library_order"):
            for level in ("request", "observation"):
                payload = request()
                target = payload if level == "request" else payload["observation"]
                target[key] = 2
                with self.subTest(key=key, level=level):
                    self.denied(payload)

    def test_malformed_nested_values_fail_closed(self):
        changes = [
            ("hand", [False]), ("hand", "Forest"),
            ("decklist_multiset", [["Forest", True]]),
            ("decklist_multiset", [["Forest", 2], ["Forest", 1]]),
            ("table", [{"appearance": "face_down", "card": "Counterspell"}]),
            ("table", [{"appearance": "face_up", "card": "Forest", "id": 7}]),
        ]
        for key, value in changes:
            payload = request()
            payload["observation"]["own_cards"][key] = copy.deepcopy(value)
            with self.subTest(key=key, value=value):
                self.denied(payload)
        payload = request()
        payload["observation"]["life_totals"]["self"] = True
        self.denied(payload)

    def test_unknown_request_and_broken_wire_frames_are_rejected(self):
        payload = request()
        payload["request"] = "cleanup_phase_with_private_state"
        self.denied(payload)
        self.denied(b"\xff\n")
        self.denied(json.dumps(request()).encode())
        self.denied(b" " * 16385 + b"\n")


if __name__ == "__main__":
    unittest.main()
