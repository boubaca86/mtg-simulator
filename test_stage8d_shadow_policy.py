import copy
import io
import json
import tempfile
import unittest
from pathlib import Path
from types import MappingProxyType

import stage8d_shadow_policy as shadow
from test_stage8c_target_features import row


def policy(weights=None):
    return shadow.ShadowPolicy('test-model', MappingProxyType(
        weights if weights is not None else {('target', 'target_p'): 1.0}))


def capture():
    r = row()
    r['schema_version'] = 'stage8-capture-v1'
    r['decision_index'] = 1
    r['run_seed'] = 101
    r['public_state'].update(schema_version='stage7d-v1', search_policy='fixed-root-v1',
                             infoset_sample_count=3, decision_index=1)
    return r


class ShadowPolicyTests(unittest.TestCase):
    def test_same_name_targets_use_current_public_statistics(self):
        r = row()
        result = policy().predict(r['public_state'], r['candidates'])
        self.assertEqual(result['recommendation'], r['candidates'][1]['action_identity'])
        self.assertEqual(result['mode'], 'shadow-only')
        self.assertFalse(result['promotion_allowed'])

    def test_labels_selected_move_and_provenance_cannot_change_prediction(self):
        r = row()
        expected = policy().predict(r['public_state'], r['candidates'])
        for c in r['candidates']:
            c.pop('aggregate_score')
            c.pop('replay_valid_count')
            c['selected_action'] = 'unavailable'
            c['game_result'] = -1000
        r['public_state'].update(complete_action_identity='different', game_result=0,
                                 decision_index=987, run_seed=789, terminal_winner_player=1)
        self.assertEqual(expected, policy().predict(r['public_state'], r['candidates']))
        safe = shadow.public_input(r['public_state'], r['candidates'])
        self.assertNotIn('complete_action_identity', safe['public_state'])
        self.assertNotIn('selected_action', safe['candidates'][0])

    def test_reordering_is_invariant_and_ties_do_not_pick_by_id(self):
        r = row()
        tied = policy({})
        result = tied.predict(r['public_state'], r['candidates'])
        self.assertEqual(result, tied.predict(r['public_state'], list(reversed(r['candidates']))))
        self.assertEqual(result['status'], 'tied')
        self.assertIsNone(result['recommendation'])
        self.assertEqual(len(result['top_actions']), 2)

    def test_prediction_does_not_mutate_public_input_or_model(self):
        r = row()
        before = copy.deepcopy(r)
        p = policy()
        p.predict(r['public_state'], r['candidates'])
        self.assertEqual(r, before)
        with self.assertRaises(TypeError):
            p.weights[('target', 'target_p')] = 999

    def test_hidden_fields_incomplete_states_and_obsolete_targets_rejected(self):
        for mutation in (
            lambda r: r['public_state'].update(opponent_hand=['Secret']),
            lambda r: r['candidates'][0].update(hidden_world_cards=['Secret']),
            lambda r: r['public_state'].pop('opponent_life'),
            lambda r: r['candidates'][0].pop('target_semantics_version'),
            lambda r: r['candidates'].append(copy.deepcopy(r['candidates'][0])),
            lambda r: r['public_state'].update(acting_life=float('nan')),
        ):
            r = row()
            mutation(r)
            with self.subTest(mutation=mutation), self.assertRaises(ValueError):
                policy().predict(r['public_state'], r['candidates'])

    def test_capture_requires_all_worlds_but_not_search_score_labels(self):
        c = capture()
        for candidate in c['candidates']:
            candidate.pop('aggregate_score')
        result = shadow.observe_capture(policy(), c)
        self.assertTrue(result['forge_proposal_in_top_set'])
        c['candidates'][0]['replay_valid_count'] = 2
        with self.assertRaisesRegex(ValueError, 'partial-world'):
            shadow.observe_capture(policy(), c)
        c['candidates'][0]['replay_valid_count'] = 3
        c['public_state']['decision_index'] = 999
        with self.assertRaisesRegex(ValueError, 'metadata disagree'):
            shadow.observe_capture(policy(), c)

    def test_stream_emits_before_future_lines_and_continues_after_rejection(self):
        output = io.StringIO()
        c = capture()
        original = copy.deepcopy(c)

        def lines():
            yield shadow.CAPTURE_PREFIX + json.dumps(c)
            self.assertEqual(len(output.getvalue().splitlines()), 1)
            bad = copy.deepcopy(c)
            bad['information_set_samples'] = 1
            yield shadow.CAPTURE_PREFIX + json.dumps(bad)
            yield 'Game Result: ignored by the proposal-only scorer\n'
            yield shadow.CAPTURE_PREFIX + json.dumps(c)

        self.assertEqual(shadow.observe_stream(policy(), lines(), output),
                         {'recommendations': 2, 'rejected': 1})
        self.assertEqual(c, original)
        records = [json.loads(line) for line in output.getvalue().splitlines()]
        self.assertEqual(records[1]['status'], 'rejected')
        self.assertNotIn('recommendation', records[1])

    def test_forced_proposals_are_identified_separately(self):
        c = capture()
        c['candidates'] = c['candidates'][1:]
        result = shadow.observe_capture(policy(), c)
        self.assertEqual(result['status'], 'forced')
        self.assertEqual(result['source_validation'], 'pending whole-log audit')

    def test_checkpoint_rejects_content_and_source_drift(self):
        payload = {
            'schema_version': shadow.MODEL_SCHEMA, 'mode': 'shadow-only',
            'promotion_allowed': False, 'target_semantics_version': shadow.TARGET_SEMANTICS_VERSION,
            'development_sha256': shadow.DEVELOPMENT_SHA256,
            'training_seeds': shadow.DEVELOPMENT_SEEDS, 'training_decisions': 518,
            'config': shadow.base.CONFIG, 'source_sha256': shadow.source_hashes(),
            'weights': [[['target', 'target_p'], 1.0]],
        }
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder) / 'model.json'
            payload['model_id'] = shadow.fingerprint(payload)
            p.write_text(json.dumps(payload))
            shadow.load_checkpoint(p)
            payload['weights'][0][1] = 2.0
            p.write_text(json.dumps(payload))
            with self.assertRaisesRegex(ValueError, 'content hash'):
                shadow.load_checkpoint(p)
            payload['source_sha256']['stage8c_target_features.py'] = 'changed'
            payload.pop('model_id')
            payload['model_id'] = shadow.fingerprint(payload)
            p.write_text(json.dumps(payload))
            with self.assertRaisesRegex(ValueError, 'provenance mismatch'):
                shadow.load_checkpoint(p)

    def test_training_rejects_an_unpinned_corpus(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder) / 'fresh.jsonl'
            p.write_text(json.dumps(row()))
            with self.assertRaisesRegex(ValueError, 'frozen Stage 8C artifact'):
                shadow.export_checkpoint(p)


if __name__ == '__main__':
    unittest.main()
