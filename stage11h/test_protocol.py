"""Real-process policy regressions; actual legality/damage is tested in Forge."""
import copy
import json
import os
from pathlib import Path
import selectors
import subprocess
import sys
import unittest


WORKER = Path(os.environ.get("STAGE11H_TEST_WORKER", str(
    Path(__file__).resolve().parents[1] / "stage11a/reciprocal_actor.py")))
SPECIES = ("Grizzly Bears", "Runeclaw Bear", "Balduvian Bears")


def card(name):
    return {"appearance": "face_up", "card": name}


def view(own=3, enemy=3, life=20):
    return {
        "objective": "Win the game under referee-enforced Magic rules",
        "own_cards": {"hand": ["Forest", "Forest", "Runeclaw Bear"],
                      "decklist_multiset": [["Forest", 48]] + [[n, 4] for n in SPECIES],
                      "table": [card(SPECIES[i % 3]) for i in range(own)]},
        "opponent_table": [card(SPECIES[i % 3]) for i in range(enemy)],
        "life_totals": {"self": life, "opponent": 20},
    }


class SpreadPolicyTests(unittest.TestCase):
    def worker(self):
        process = subprocess.Popen([sys.executable, "-u", str(WORKER)],
                                   stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        def close():
            if process.poll() is None:
                process.terminate()
            try:
                process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=3)
            for stream in (process.stdin, process.stdout, process.stderr):
                stream.close()
        self.addCleanup(close)
        return process

    def ask(self, process, observation):
        process.stdin.write(json.dumps(observation).encode() + b"\n")
        process.stdin.flush()
        with selectors.DefaultSelector() as ready:
            ready.register(process.stdout, selectors.EVENT_READ)
            self.assertTrue(ready.select(timeout=3), "worker did not answer")
        raw = process.stdout.readline()
        self.assertTrue(raw, "worker rejected valid input")
        return json.loads(raw)

    def test_distinct_first_targets_prevent_three_blockers_piling_on_one(self):
        plan = self.ask(self.worker(), view(life=1))
        self.assertEqual(plan["attack_table_indices"], [])
        self.assertEqual(plan["block_pairs"][:3], [[0, 0], [1, 1], [2, 2]])
        self.assertEqual(len(plan["block_pairs"]), 8)
        self.assertEqual(len({tuple(p) for p in plan["block_pairs"]}), 8)

    def test_eight_pair_cap_covers_eight_distinct_blockers_and_targets(self):
        pairs = self.ask(self.worker(), view(8, 8, 1))["block_pairs"]
        self.assertEqual(pairs, [[i, i] for i in range(8)])

    def test_two_reserved_blockers_get_distinct_preferences_then_fallbacks(self):
        plan = self.ask(self.worker(), view(life=4))
        self.assertEqual(plan["attack_table_indices"], [0])
        self.assertEqual(plan["block_pairs"][:2], [[1, 0], [2, 1]])
        self.assertEqual({tuple(p) for p in plan["block_pairs"]},
                         {(own, target) for own in (1, 2) for target in range(3)})

    def test_public_table_positions_preserved_and_unknown_cards_excluded(self):
        observation = view()
        observation["own_cards"]["table"] = [card("Forest"), card(SPECIES[0]),
            {"appearance": "face_down"}, card("Llanowar Elves"), card(SPECIES[1])]
        observation["opponent_table"] = [{"appearance": "face_down"},
            card("Island"), card(SPECIES[2]), card("Sedge Scorpion"), card(SPECIES[0])]
        pairs = self.ask(self.worker(), observation)["block_pairs"]
        self.assertEqual(pairs[:2], [[1, 2], [4, 4]])
        self.assertEqual({tuple(p) for p in pairs}, {(1, 2), (1, 4), (4, 2), (4, 4)})

    def test_single_blocker_still_offers_every_visible_target_fallback(self):
        pairs = self.ask(self.worker(), view(1, 3))["block_pairs"]
        self.assertEqual(pairs, [[0, 0], [0, 1], [0, 2]])

    def test_empty_sides_and_more_blockers_than_targets_are_bounded(self):
        p = self.worker()
        for own, enemy in ((0, 3), (3, 0), (0, 0)):
            self.assertEqual(self.ask(p, view(own, enemy))["block_pairs"], [])
        pairs = self.ask(p, view(12, 1))["block_pairs"]
        self.assertEqual(pairs, [[i, 0] for i in range(8)])

    def test_two_independent_workers_agree_and_opening_protocol_survives(self):
        a, b = self.worker(), self.worker()
        self.assertNotEqual(a.pid, b.pid)
        observation = view()
        self.assertEqual(self.ask(a, observation), self.ask(b, observation))
        for p in (a, b):
            opening = self.ask(p, {"request": "own_opening_hand", "observation": observation})
            self.assertEqual(opening["intent"], "opening_hand")
            self.assertTrue(opening["keep"])
            self.assertEqual(sorted(opening["bottom_priority"]), [0, 1, 2])
            self.assertIsNone(p.poll())

    def test_unapproved_readiness_combat_and_hidden_fields_are_rejected(self):
        for key in ("phase", "turn", "priority", "stack", "actual_attackers",
                    "opponent_hand", "library_order", "tapped", "summoning_sickness"):
            with self.subTest(key=key):
                malformed = copy.deepcopy(view())
                malformed[key] = []
                p = self.worker()
                out, _ = p.communicate(json.dumps(malformed).encode() + b"\n", timeout=3)
                self.assertNotEqual(p.returncode, 0)
                self.assertEqual(out, b"")


if __name__ == "__main__":
    unittest.main()
