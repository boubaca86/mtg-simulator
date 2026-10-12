"""Actual-worker checks for bounded printed-power combat preferences."""
import json
import unittest

from stage11h import test_protocol as shared


def view():
    result = shared.view()
    result['own_cards']['decklist_multiset'] = [
        ['Forest', 48], ['Grizzly Bears', 4], ['Runeclaw Bear', 4], ['Centaur Courser', 4]]
    return result


class CourserPolicyTests(unittest.TestCase):
    worker = shared.SpreadPolicyTests.worker
    ask = shared.SpreadPolicyTests.ask

    def test_courser_is_cast_candidate_before_cheaper_creature_fallbacks(self):
        observation = view()
        observation['own_cards']['hand'] = [
            'Forest', 'Grizzly Bears', 'Centaur Courser', 'Llanowar Elves']
        plan = self.ask(self.worker(), observation)
        self.assertEqual(plan['land_hand_indices'], [0])
        self.assertEqual(plan['creature_hand_indices'], [2, 1, 3])
        self.assertEqual(set(plan), {'intent', 'land_hand_indices',
                         'creature_hand_indices', 'attack_table_indices', 'block_pairs'})

    def test_own_visible_courser_can_attack_and_block(self):
        observation = view()
        observation['own_cards']['table'] = [shared.card('Centaur Courser')]
        observation['opponent_table'] = [shared.card('Runeclaw Bear')]
        plan = self.ask(self.worker(), observation)
        self.assertEqual(plan['attack_table_indices'], [0])
        self.assertEqual(plan['block_pairs'], [[0, 0]])

    def test_larger_visible_threats_get_distinct_first_block_preferences(self):
        observation = view()
        observation['opponent_table'] = [shared.card(name) for name in
            ('Grizzly Bears', 'Centaur Courser', 'Centaur Courser')]
        observation['life_totals']['self'] = 5
        plan = self.ask(self.worker(), observation)
        # Eight incoming printed damage: one 3-power block leaves lethal five.
        self.assertEqual(plan['attack_table_indices'], [0])
        self.assertEqual(plan['block_pairs'][:2], [[1, 1], [2, 2]])

    def test_mixed_damage_reserves_change_at_actual_three_power_thresholds(self):
        observation = view()
        observation['opponent_table'] = [shared.card(name) for name in
            ('Centaur Courser', 'Centaur Courser', 'Grizzly Bears')]
        p = self.worker()
        for life, attackers in ((20, 3), (6, 2), (5, 1), (3, 1), (2, 0), (1, 0)):
            with self.subTest(life=life):
                observation['life_totals']['self'] = life
                plan = self.ask(p, observation)
                self.assertEqual(len(plan['attack_table_indices']), attackers)

    def test_face_down_and_unsupported_creatures_do_not_acquire_known_stats(self):
        observation = view()
        observation['own_cards']['table'] = [
            {'appearance': 'face_down'}, shared.card('Centaur Courser'),
            shared.card('Sedge Scorpion')]
        observation['opponent_table'] = [shared.card('Sedge Scorpion'),
            {'appearance': 'face_down'}, shared.card('Centaur Courser')]
        plan = self.ask(self.worker(), observation)
        self.assertEqual(plan['attack_table_indices'], [1])
        self.assertEqual(plan['block_pairs'], [[1, 2]])

    def test_mixed_eight_pair_limit_preserves_distinct_target_coverage(self):
        observation = view()
        observation['own_cards']['table'] = [shared.card('Centaur Courser') for _ in range(8)]
        observation['opponent_table'] = [shared.card(
            'Grizzly Bears' if i % 2 == 0 else 'Centaur Courser') for i in range(8)]
        observation['life_totals']['self'] = 1
        pairs = self.ask(self.worker(), observation)['block_pairs']
        self.assertEqual(len(pairs), 8)
        self.assertEqual([pair[0] for pair in pairs], list(range(8)))
        self.assertEqual([pair[1] for pair in pairs], [1, 3, 5, 7, 0, 2, 4, 6])

    def test_two_independent_workers_keep_courser_opening_and_action_protocols(self):
        observation = view()
        observation['own_cards']['hand'] = ['Forest', 'Forest', 'Centaur Courser']
        a, b = self.worker(), self.worker()
        self.assertNotEqual(a.pid, b.pid)
        replies = []
        for p in (a, b):
            opening = self.ask(p, {'request': 'own_opening_hand', 'observation': observation})
            self.assertTrue(opening['keep'])
            self.assertEqual(sorted(opening['bottom_priority']), [0, 1, 2])
            replies.append(self.ask(p, observation))
        self.assertEqual(replies[0], replies[1])
        self.assertEqual(replies[0]['creature_hand_indices'], [2])

    def test_courser_does_not_allow_new_referee_stat_fields(self):
        for field in ('power', 'toughness', 'actual_attackers', 'tapped', 'phase'):
            with self.subTest(field=field):
                observation = view()
                record = shared.card('Centaur Courser')
                record[field] = 3
                observation['opponent_table'] = [record]
                process = self.worker()
                out, _ = process.communicate(json.dumps(observation).encode() + b'\n', timeout=3)
                self.assertNotEqual(process.returncode, 0)
                self.assertEqual(out, b'')


if __name__ == '__main__':
    unittest.main()
