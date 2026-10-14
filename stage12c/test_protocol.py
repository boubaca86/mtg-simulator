"""Real-process London bottom preferences; no referee-private inputs."""
import copy
import unittest

from stage11y import test_protocol as worker_checks
from stage11y.test_protocol import request


class LondonBottomTests(unittest.TestCase):
    launch = worker_checks.OwnHandProtocolTests.launch
    ask = worker_checks.OwnHandProtocolTests.ask

    def opening(self, hand):
        payload = request(hand)
        payload["request"] = "own_opening_hand"
        return payload

    def assert_order(self, actor, hand, expected):
        result = self.ask(actor, self.opening(hand))
        self.assertEqual(set(result), {"intent", "keep", "bottom_priority"})
        self.assertEqual(result["intent"], "opening_hand")
        self.assertEqual(result["bottom_priority"], expected)
        self.assertEqual(sorted(expected), list(range(len(hand))))
        return result

    def test_surplus_lands_go_before_only_payable_bear(self):
        actor = self.launch()
        hand = ["Forest"] * 5 + ["Grizzly Bears", "Centaur Courser"]
        self.assert_order(actor, hand, [2, 3, 4, 6, 0, 1, 5])

    def test_two_independent_workers_preserve_duplicate_card_indices(self):
        a, b = self.launch(), self.launch()
        self.assertNotEqual(a.pid, b.pid)
        hand = ["Forest", "Forest", "Grizzly Bears", "Grizzly Bears",
                "Forest", "Forest", "Forest"]
        for actor in (a, b):
            self.assert_order(actor, hand, [4, 5, 6, 3, 0, 1, 2])

    def test_baloth_requires_two_actual_green_sources(self):
        actor = self.launch()
        hand = ["Forest", "Forest", "Island", "Mountain", "Swamp",
                "Rumbling Baloth", "Counterspell"]
        self.assert_order(actor, hand, [6, 4, 0, 1, 2, 3, 5])
        hand[1] = "Island"
        # No payable creature: explicit ordinary own-hand fallback.
        expected = self.ask(actor, request(hand))["hand_priority"]
        self.assert_order(actor, hand, expected)

    def test_cheaper_payable_creature_is_preserved_before_larger_one(self):
        actor = self.launch()
        hand = ["Forest"] * 4 + ["Rumbling Baloth", "Centaur Courser", "Llanowar Elves"]
        self.assert_order(actor, hand, [1, 2, 3, 4, 5, 0, 6])

    def test_each_affordable_creature_retains_required_basic_mana(self):
        actor = self.launch()
        for creature, cost, green in (("Llanowar Elves", 1, 1),
                                     ("Scryb Sprites", 1, 1),
                                     ("Sedge Scorpion", 1, 1),
                                     ("Grizzly Bears", 2, 1),
                                     ("Elvish Visionary", 2, 1),
                                     ("Runeclaw Bear", 2, 1),
                                     ("Balduvian Bears", 2, 1),
                                     ("Centaur Courser", 3, 1),
                                     ("Rumbling Baloth", 4, 2)):
            original = ["Forest"] * green + ["Island"] * (6-green) + [creature]
            # Every cyclic physical index layout, every safe referee prefix.
            for rotation in range(7):
                hand = original[rotation:] + original[:rotation]
                order = self.ask(actor, self.opening(hand))["bottom_priority"]
                self.assertEqual(sorted(order), list(range(7)))
                for count in range(7-cost):
                    kept = [name for i, name in enumerate(hand) if i not in order[:count]]
                    with self.subTest(creature=creature, rotation=rotation, count=count):
                        self.assertIn(creature, kept)
                        self.assertGreaterEqual(kept.count("Forest"), green)
                        self.assertGreaterEqual(sum(x in ("Forest", "Island") for x in kept), cost)

    def test_opening_choice_depends_only_on_own_hand(self):
        actor = self.launch()
        payload = self.opening(["Forest"] * 5 + ["Grizzly Bears", "Centaur Courser"])
        expected = self.ask(actor, payload)
        changed = copy.deepcopy(payload)
        changed["observation"]["opponent_table"] = [{"appearance": "face_down"}]
        changed["observation"]["life_totals"] = {"self": -20, "opponent": 999}
        changed["observation"]["own_cards"]["decklist_multiset"] = [["Island", 60]]
        changed["observation"]["own_cards"]["table"] = [{"appearance": "face_up", "card": "Rumbling Baloth"}]
        self.assertEqual(self.ask(actor, changed), expected)

    def test_opening_cleanup_and_combat_remain_separate_in_one_worker(self):
        actor = self.launch()
        hand = ["Forest"] * 5 + ["Grizzly Bears", "Centaur Courser"]
        cleanup = self.ask(actor, request(hand))
        plan = self.ask(actor, request(hand)["observation"])
        opening = self.ask(actor, self.opening(hand))
        self.assertNotEqual(cleanup["hand_priority"], opening["bottom_priority"])
        self.assertEqual(self.ask(actor, request(hand)), cleanup)
        self.assertEqual(self.ask(actor, request(hand)["observation"]), plan)

    def test_empty_unpayable_and_unsupported_hands_remain_permutations(self):
        actor = self.launch()
        for hand in ([], ["Island"] * 7, ["Grizzly Bears"] * 7,
                     ["Counterspell", "Shock", "Forest"], ["Forest"] * 128):
            expected = self.ask(actor, request(hand))["hand_priority"]
            self.assert_order(actor, hand, expected)


if __name__ == "__main__":
    unittest.main()
