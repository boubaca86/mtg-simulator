"""Behavioral regressions for contextual ranking and honest paired evaluation."""
import math
import os
import subprocess
import sys
import unittest
from copy import deepcopy

from stage8_action_ranker import aggregate, compare, coverage, decision_metrics, validate_rows
from stage8_ranker_features import MODELS, ZONES, canonical_action, features


def recipe(name, index=0, target='<none>', x='<none>', modes='<none>', choices='<none>'):
    return (f'recipe=v2|ability={name}|candidate={index}/2|x={x}|modes={modes}'
            f'|targets={target}|choices={choices}')


def row(seed=101, decision=0, life=2):
    actions = [recipe('Recover'), recipe('Pressure', 1)]
    scores = [20, 10] if life < 20 else [10, 20]
    state = {'acting_life': life, 'opponent_life': 20, 'turn': 4, 'phase': 'MAIN1',
             'acting_player_name': 'Ai(1)-Example', 'decision_index': decision,
             'complete_action_identity': actions[scores.index(max(scores))]}
    for zone in ZONES:
        state[zone] = []
        state[zone+'_semantics'] = []
    return {'schema_version': 'stage8a-v1', 'forge_version': '2.0.15',
            'search_policy': 'fixed-root-v1', 'information_set_samples': 3,
            'decision_id': f'{seed}:{decision}', 'game_group': f'{seed}:game',
            'corpus_seed': seed, 'public_state': state,
            'selected_action': state['complete_action_identity'],
            'candidates': [{'action_identity': a, 'aggregate_score': s, 'replay_valid_count': 3}
                           for a, s in zip(actions, scores)]}


class FeatureBoundaryTests(unittest.TestCase):
    def test_context_can_change_relative_candidate_features(self):
        low, high = row(), row(life=38)
        def difference(r, model):
            a, b = [features(r, c, model) for c in r['candidates']]
            return {k: a.get(k, 0)-b.get(k, 0) for k in a.keys() | b.keys()}
        self.assertEqual(difference(low, 'action_only'), difference(high, 'action_only'))
        for model in MODELS[1:]:
            self.assertNotEqual(difference(low, model), difference(high, model))

    def test_labels_selected_action_and_provenance_are_not_features(self):
        original = row()
        changed = deepcopy(original)
        changed.update(decision_id='different', game_group='different', corpus_seed=999,
                       selected_action=changed['candidates'][1]['action_identity'])
        changed['public_state'].update(complete_action_identity='different chosen root',
                                      game_result=0, terminal_winner_player=1,
                                      run_seed=999, decision_index=999, matchup_id='other',
                                      acting_player_name='Ai(2)-Another deck', acting_player=1)
        for candidate in changed['candidates']:
            candidate['aggregate_score'] *= -100
        for model in MODELS:
            for before, after in zip(original['candidates'], changed['candidates']):
                self.assertEqual(features(original, before, model), features(changed, after, model))

    def test_physical_ids_and_candidate_positions_do_not_change_features(self):
        a = recipe('Bolt', target='[Goblin (12)]')
        b = recipe('Bolt', index=1, target='[Goblin (999)]')
        self.assertEqual(canonical_action(a), canonical_action(b))
        self.assertNotEqual(canonical_action(a), canonical_action(recipe('Bolt', target='[Giant (12)]')))
        self.assertNotEqual(canonical_action(a), canonical_action(recipe('Bolt', x='4', target='[Goblin (12)]')))
        self.assertNotEqual(canonical_action(a), canonical_action(recipe('Bolt', modes='(1)', target='[Goblin (12)]')))
        self.assertNotEqual(canonical_action(recipe('Bolt', choices='1')),
                            canonical_action(recipe('Bolt', choices='2')))

    def test_targets_follow_player_role_not_deck_identity(self):
        a = canonical_action(recipe('Bolt', target='[Ai(2)-Red]'), 'Ai(1)-Blue')
        b = canonical_action(recipe('Bolt', target='[Ai(1)-Blue]'), 'Ai(2)-Red')
        own = canonical_action(recipe('Bolt', target='[Ai(1)-Blue]'), 'Ai(1)-Blue')
        self.assertEqual(a, b)
        self.assertNotEqual(a, own)

    def test_malformed_and_old_action_recipes_fail(self):
        for identity in ('pass', 'ability=Recover', 'recipe=v2|ability=Recover',
                         recipe('Recover')+'|x=4'):
            with self.subTest(identity=identity), self.assertRaises(ValueError):
                canonical_action(identity)


class EvaluationTests(unittest.TestCase):
    def test_prediction_ties_are_uniform_and_not_label_broken(self):
        r = row()
        metric = decision_metrics(r, [0, 0])
        self.assertEqual(metric['top1'], .5)
        self.assertEqual(metric['normalized_regret'], .5)
        self.assertEqual(metric['pair_ok'], .5)
        r['candidates'].reverse()
        self.assertEqual(metric, decision_metrics(r, [0, 0]))

    def test_every_equal_quality_action_is_accepted(self):
        r = row()
        r['candidates'][1]['aggregate_score'] = 20
        for predictions in ([0, 1], [1, 0], [0, 0]):
            metric = decision_metrics(r, predictions)
            self.assertEqual(metric['top1'], 1)
            self.assertEqual(metric['regret'], 0)
            summary = aggregate([metric])
            self.assertIsNone(summary['pairwise_accuracy'])
            self.assertEqual(summary['pairwise_comparisons'], 0)

    def test_terminal_sentinels_have_bounded_normalized_regret(self):
        r = row()
        r['candidates'][0]['aggregate_score'] = 2_147_483_647
        r['candidates'][1]['aggregate_score'] = -2_147_483_648
        metric = decision_metrics(r, [0, 1])
        self.assertEqual(metric['normalized_regret'], 1)
        self.assertEqual(metric['regret'], 4_294_967_295)
        self.assertTrue(metric['terminal_scale'])

    def test_nonfinite_predictions_fail(self):
        for predictions in ([math.nan, 0], [0, math.inf], [0]):
            with self.assertRaises(ValueError):
                decision_metrics(row(), predictions)

    def test_context_learns_opposite_preferences_on_unseen_families(self):
        rows = [row(seed, i, life) for seed in (101, 202, 303) for i, life in enumerate((2, 38))]
        before = deepcopy(rows)
        result = compare(rows)
        self.assertEqual(result['models']['action_only']['holdout']['top1_accuracy'], .5)
        for model in MODELS[1:]:
            self.assertEqual(result['models'][model]['holdout']['top1_accuracy'], 1.)
        for model in result['models'].values():
            self.assertEqual(model['holdout']['decisions'], len(rows))
            for fold in model['folds']:
                self.assertNotIn(fold['holdout_seed'], fold['train_seeds'])
                self.assertEqual(len(fold['train_seeds']), 2)
        self.assertEqual(rows, before)
        self.assertEqual(result, compare(rows))
        self.assertFalse(result['promotion_allowed'])

    def test_report_is_identical_across_python_hash_seeds(self):
        script = '''import json
from test_stage8_action_ranker import row, recipe
from stage8_action_ranker import compare
rows = [row(s, i, life) for s in (101, 202, 303) for i, life in enumerate((2, 38))]
for r in rows:
    for i, c in enumerate(r['candidates']):
        c['action_identity'] = recipe('Gain three life from a spell' if i == 0 else 'Deal damage to an enemy')
    r['selected_action'] = r['candidates'][int(r['public_state']['acting_life'] > 20)]['action_identity']
print(json.dumps(compare(rows), sort_keys=True))
'''
        outputs = [subprocess.check_output([sys.executable, '-c', script],
                    env=dict(os.environ, PYTHONHASHSEED=seed)) for seed in ('1', '7')]
        self.assertEqual(outputs[0], outputs[1])


class DatasetBoundaryTests(unittest.TestCase):
    def test_duplicate_decisions_and_cross_seed_games_fail(self):
        with self.assertRaisesRegex(ValueError, 'duplicate decision'):
            validate_rows([row(), row()])
        a, b = row(), row(seed=202)
        b['game_group'] = a['game_group']
        with self.assertRaisesRegex(ValueError, 'crosses corpus seed'):
            validate_rows([a, b])

    def test_nested_hidden_fields_fail_anywhere_in_record(self):
        for key in ('opponent_hand', 'opponent_hand_identities', 'own_library', 'future_draws'):
            r = row()
            r['metadata'] = {'nested': [{key: ['secret']}]}
            with self.subTest(key=key), self.assertRaises(ValueError):
                validate_rows([r])

    def test_nonfinite_public_state_and_unverified_known_cards_fail(self):
        for value in (math.nan, math.inf, True):
            r = row()
            r['public_state']['acting_life'] = value
            with self.assertRaises(ValueError):
                validate_rows([r])
        r = row()
        r['public_state']['own_hand_semantics'] = ['Goblin|mv=nan|type=Creature']
        with self.assertRaises(ValueError):
            validate_rows([r])
        r = row()
        r['public_state']['opponent_known_cards'] = ['unverified']
        with self.assertRaises(ValueError):
            validate_rows([r])

    def test_partial_world_candidates_fail_before_training(self):
        r = row()
        r['candidates'][0]['replay_valid_count'] = 2
        with self.assertRaisesRegex(AssertionError, 'partial-world'):
            compare([r])

    def test_at_least_three_seeds_required(self):
        with self.assertRaisesRegex(ValueError, 'three independent'):
            compare([row(), row(seed=202)])

    def test_coverage_uses_captured_proposals_and_checks_matching(self):
        r = row()
        observations = [{'game_group': r['game_group'], 'decision_index': i} for i in (0, 1)]
        result = coverage([r], observations)
        self.assertEqual(result['fraction'], .5)
        self.assertEqual(result['captured_proposals'], 2)
        self.assertIsNone(coverage([r], None)['fraction'])
        with self.assertRaisesRegex(ValueError, 'duplicate source'):
            coverage([r], observations*2)
        with self.assertRaisesRegex(ValueError, 'do not match'):
            coverage([r], observations[1:])


if __name__ == '__main__':
    unittest.main()
