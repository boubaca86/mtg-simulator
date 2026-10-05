import copy
import unittest

import stage7_compare_evaluators as compare
import stage7_semantic_value_model as semantic
import stage7_value_baseline as baseline
from stage7_label_outcomes import LABEL_VERSION
from stage7_dataset_validation import validate_labeled_rows


def fixture():
    rows = []
    for seed in range(4):
        for game in range(2):
            for actor in range(2):
                rows.append({'schema_version': 'stage7c-v3', 'corpus_seed': seed, 'run_seed': seed,
                             'decision_index': actor, 'acting_player': actor,
                             'acting_player_name': f'Ai({actor + 1})-Same Deck',
                             'game_group': f'seed-{seed}:game-{game}', 'game_result': float(actor == game),
                             'terminal_winner_player': game, 'label_version': LABEL_VERSION,
                             'complete_action_identity': 'recipe=v2|ability=test', 'infoset_sample_count': 3,
                             'acting_life': 10, 'opponent_life': 10, 'turn': 4, 'phase': 'MAIN1',
                             'own_hand': ['Bolt'], 'opponent_unknown_hand_count': 1,
                             'own_library_count': 30, 'opponent_library_count': 30,
                             'own_battlefield': ['Bear'], 'opponent_battlefield': ['Goblin'],
                             'own_graveyard': ['Mountain'], 'opponent_graveyard': [],
                             'exile_public': [], 'stack_public': [], 'matchup_id': 'fixture'})
    return rows


class ComparisonTest(unittest.TestCase):
    def test_counts_survive_schema_migration(self):
        current = fixture()[0]
        legacy = dict(current, battlefield_public=['Bear', 'Goblin'], graveyard_public=['Mountain'])
        self.assertEqual(baseline.features(current), baseline.features(legacy))

    def test_stack_is_not_a_character_sequence(self):
        r = fixture()[0]
        self.assertEqual(semantic.features(r, 8), semantic.features(dict(r, stack_public='[]==[]==[]'), 8))
        with self.assertRaisesRegex(ValueError, 'stack_public'):
            semantic.features(dict(r, stack_public='Secret raw stack'), 8)

    def test_split_keeps_seed_families_and_games_together(self):
        train, test = compare.split_rows(fixture())
        self.assertFalse({r['game_group'] for r in train} & {r['game_group'] for r in test})
        self.assertFalse({r['corpus_seed'] for r in train} & {r['corpus_seed'] for r in test})
        self.assertEqual((train, test), compare.split_rows(fixture()))

    def test_bad_labels_and_nested_secrets_are_rejected(self):
        for mutation in ({'label_version': 'old'}, {'game_result': 0.},
                         {'metadata': {'opponent_hand_cards': ['Secret']}},
                         {'opponent_known_cards': ['Unverified']}):
            rows = fixture()
            rows[0].update(mutation)
            with self.assertRaises(ValueError):
                validate_labeled_rows(rows)

    def test_old_schema_and_false_single_class_benchmarks_fail(self):
        rows = fixture()
        rows[0]['schema_version'] = 'stage7a-v1'
        with self.assertRaises(ValueError):
            compare.compare(rows, epochs=1)
        rows = [r for r in fixture() if r['game_result'] == 0]
        with self.assertRaisesRegex(ValueError, 'both actual wins and losses'):
            compare.split_rows(rows)

    def test_paired_models_are_reproducible_and_never_promoted(self):
        rows = fixture()
        _, holdout = compare.split_rows(rows)
        a = compare.compare(rows, epochs=2, hash_dim=8)
        b = compare.compare(copy.deepcopy(rows), epochs=2, hash_dim=8)
        self.assertEqual(a, b)
        self.assertFalse(a['promotion_allowed'])
        self.assertEqual(set(a['models']), {'combined_counts', 'perspective_counts', 'visible_identity_hash', 'training_prior'})
        games = {r['game_group'] for r in holdout}
        for model in a['models'].values():
            self.assertEqual(set(model['holdout']['per_game']), games)


if __name__ == '__main__':
    unittest.main()
