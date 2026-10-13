"""Real worker-process tests of the new own-hand request contract."""
import copy
import json
import os
from pathlib import Path
import selectors
import subprocess
import sys
import unittest

from stage10h.test_protocol import invalid_messages, view

WORKER = Path(os.environ.get("STAGE11Y_TEST_WORKER", str(
    Path(__file__).resolve().parents[1] / "stage11a/reciprocal_actor.py")))


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
        self.assertEqual(set(plan), {"intent", "land_hand_indices", "creature_hand_indices", "attack_table_indices", "block_pairs"})
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

    def test_mixed_creatures_retain_four_preferred_basics_and_rank_smaller_first(self):
        actor = self.launch()
        hand = ["Forest"] * 6 + ["Grizzly Bears", "Centaur Courser", "Rumbling Baloth"]
        self.assertEqual(self.ask(actor, request(hand))["hand_priority"],
                         [4, 5, 6, 7, 8, 0, 1, 2, 3])
        for creature, keep in (("Grizzly Bears", 2), ("Centaur Courser", 3), ("Rumbling Baloth", 4)):
            hand = ["Forest"] * 5 + [creature]
            order = self.ask(actor, request(hand))["hand_priority"]
            with self.subTest(creature=creature):
                self.assertEqual(order[:5-keep], list(range(keep, 5)))
                self.assertEqual(order[-keep:], list(range(keep)))

    def test_cleanup_request_does_not_change_persistent_emergency_elf_policy(self):
        actor = self.launch()
        data = view()
        data["own_cards"]["table"] = [{"appearance": "face_up", "card": "Llanowar Elves"}]
        data["life_totals"] = {"self": 2, "opponent": 20}
        for names in (("Grizzly Bears",), ("Grizzly Bears", "Llanowar Elves")):
            data["opponent_table"] = [{"appearance": "face_up", "card": name}
                                      for name in names]
            with self.subTest(opposing_cards=names):
                before = self.ask(actor, data)
                self.assertEqual(before["block_pairs"], [[0, 0]])
                self.ask(actor, request())
                self.assertEqual(self.ask(actor, data), before)
        data["own_cards"]["table"].append({"appearance": "face_up", "card": "Grizzly Bears"})
        data["opponent_table"] = [{"appearance": "face_up", "card": name}
                                  for name in ("Grizzly Bears", "Runeclaw Bear")]
        before = self.ask(actor, data)
        self.assertEqual(before["block_pairs"][:2], [[1, 0], [0, 1]])
        self.ask(actor, request())
        self.assertEqual(self.ask(actor, data), before)

    def test_unknown_request_and_broken_wire_frames_are_rejected(self):
        payload = request()
        payload["request"] = "cleanup_phase_with_private_state"
        self.denied(payload)
        self.denied(b"\xff\n")
        for encoding in ("utf-16-be", "utf-32-be"):
            self.denied((json.dumps(request()) + "\n").encode(encoding))
        self.denied(json.dumps(request()).encode())
        self.denied(b" " * 16385 + b"\n")


if __name__ == "__main__":
    unittest.main()
