#!/usr/bin/env python3
"""Frozen, read-only recommendations for Forge's public search proposals.

Training uses the pinned development corpus. Inference receives only an explicit
public-input projection, never search scores, selected actions or terminal labels.
This process has no interface for choosing or executing a Forge action.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from types import MappingProxyType

import stage8_action_ranker as base
from stage7_dataset_validation import reject_hidden
from stage8_ranker_features import SCALES, ZONES, canonical_action, public_context
from stage8_validate_counterfactual import FORBIDDEN_KEYS, walk_keys
from stage8c_target_features import TARGET_SEMANTICS_VERSION, features, validated_target_descriptors

DEVELOPMENT_SHA256 = '1173a323a329460d69abfd2c8cab71816430759830b26c927874cc389ff42903'
DEVELOPMENT_SEEDS = [20261004, 20261005, 20261006, 20261007]
MODEL_SCHEMA = 'stage8d-shadow-model-v1'
CAPTURE_PREFIX = 'EXPERT_STAGE8_CAPTURE: '
SOURCE_FILES = ('stage8_action_ranker.py', 'stage8_ranker_features.py', 'stage8c_target_features.py')
STATE_FIELDS = ('phase', 'acting_player_name', *SCALES, *ZONES,
                *(zone + '_semantics' for zone in ZONES))
CANDIDATE_FIELDS = ('action_identity', 'target_semantics_version', 'target_public_semantics')


def canonical_json(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), allow_nan=False)


def fingerprint(value):
    return hashlib.sha256(canonical_json(value).encode()).hexdigest()


def source_hashes():
    return {name: hashlib.sha256(Path(__file__).with_name(name).read_bytes()).hexdigest()
            for name in SOURCE_FILES}


def reject_private(value):
    reject_hidden(value)
    if FORBIDDEN_KEYS & set(walk_keys(value)):
        raise ValueError('hidden-information field at shadow boundary')


def public_input(state, candidates):
    """Copy the scoring allowlist; incidental audit/label fields cannot enter it."""
    if not isinstance(state, dict) or not isinstance(candidates, list) or not candidates:
        raise ValueError('shadow scoring requires a public state and nonempty candidate list')
    reject_private(state)
    reject_private(candidates)
    if set(STATE_FIELDS) - state.keys():
        raise ValueError('incomplete public state; do not silently fill missing features')
    if state.get('opponent_known_cards'):
        raise ValueError('unverified opponent-hand knowledge')
    if not all(isinstance(state[k], str) and state[k] for k in ('phase', 'acting_player_name')):
        raise ValueError('invalid phase or public actor name')
    safe_state = {}
    for key in STATE_FIELDS:
        value = state[key]
        if key in ZONES or key.endswith('_semantics'):
            if not isinstance(value, list) or not all(isinstance(v, str) for v in value):
                raise ValueError('public zones must be lists of descriptors')
            value = list(value)
        safe_state[key] = value
    public_context(safe_state, 'public_semantics')
    safe_candidates = []
    seen = set()
    for candidate in candidates:
        if not isinstance(candidate, dict) or set(CANDIDATE_FIELDS) - candidate.keys():
            raise ValueError('incomplete typed candidate')
        identity = candidate['action_identity']
        if not isinstance(identity, str) or identity in seen:
            raise ValueError('invalid or duplicate complete action')
        canonical_action(identity, safe_state['acting_player_name'])
        validated_target_descriptors(candidate, required=True)
        seen.add(identity)
        safe_candidates.append({
            'action_identity': identity,
            'target_semantics_version': candidate['target_semantics_version'],
            'target_public_semantics': list(candidate['target_public_semantics']),
        })
    return {'public_state': safe_state, 'candidates': safe_candidates}


@dataclass(frozen=True)
class ShadowPolicy:
    model_id: str
    weights: MappingProxyType

    def predict(self, state, candidates):
        safe = public_input(state, candidates)
        scored = []
        for candidate in safe['candidates']:
            score = base.dot(self.weights, features(safe, candidate))
            if not math.isfinite(score):
                raise ValueError('nonfinite model prediction')
            scored.append({'action_identity': candidate['action_identity'], 'model_score': score})
        scored.sort(key=lambda c: c['action_identity'])
        high = max(c['model_score'] for c in scored)
        top = [c['action_identity'] for c in scored
               if math.isclose(c['model_score'], high, rel_tol=1e-12, abs_tol=1e-12)]
        # Keep every model tie. An arbitrary raw-ID or candidate-order tie-break
        # must not look like a confident recommendation.
        return {
            'schema_version': 'stage8d-shadow-recommendation-v1',
            'model_id': self.model_id, 'mode': 'shadow-only', 'promotion_allowed': False,
            'scope': 'captured search proposal; execution not verified',
            'status': 'forced' if len(scored) == 1 else ('tied' if len(top) > 1 else 'ranked'),
            'recommendation': top[0] if len(top) == 1 else None,
            'top_actions': top, 'candidate_scores': scored,
        }


def export_checkpoint(development: Path):
    raw = development.read_bytes()
    if hashlib.sha256(raw).hexdigest() != DEVELOPMENT_SHA256:
        raise ValueError('development corpus differs from the frozen Stage 8C artifact')
    rows = [json.loads(line) for line in raw.splitlines() if line.strip()]
    base.validate_rows(rows)
    if sorted({r['corpus_seed'] for r in rows}) != DEVELOPMENT_SEEDS:
        raise ValueError('wrong development seed families')
    vectors = {}
    for row in rows:
        safe = public_input(row['public_state'], row['candidates'])
        vectors[row['decision_id']] = [features(safe, c) for c in safe['candidates']]
    weights = base.train(rows, vectors, epochs=base.CONFIG['epochs'], lr=base.CONFIG['learning_rate'])
    payload = {
        'schema_version': MODEL_SCHEMA, 'mode': 'shadow-only', 'promotion_allowed': False,
        'target_semantics_version': TARGET_SEMANTICS_VERSION,
        'development_sha256': DEVELOPMENT_SHA256, 'training_seeds': DEVELOPMENT_SEEDS,
        'training_decisions': len(rows), 'config': base.CONFIG, 'source_sha256': source_hashes(),
        'weights': [[list(key), value] for key, value in sorted(weights.items())],
    }
    payload['model_id'] = fingerprint(payload)
    return payload


def load_checkpoint(path: Path):
    payload = json.loads(path.read_text())
    model_id = payload.pop('model_id', None)
    if fingerprint(payload) != model_id:
        raise ValueError('checkpoint content hash mismatch')
    if (payload.get('schema_version') != MODEL_SCHEMA or payload.get('mode') != 'shadow-only'
            or payload.get('promotion_allowed') is not False
            or payload.get('target_semantics_version') != TARGET_SEMANTICS_VERSION):
        raise ValueError('incompatible shadow checkpoint')
    if (payload.get('source_sha256') != source_hashes() or payload.get('config') != base.CONFIG
            or payload.get('development_sha256') != DEVELOPMENT_SHA256
            or payload.get('training_seeds') != DEVELOPMENT_SEEDS
            or payload.get('training_decisions') != 518):
        raise ValueError('checkpoint source or training provenance mismatch')
    weights = {}
    for key, value in payload['weights']:
        if (not isinstance(key, list) or len(key) not in (2, 3)
                or not all(isinstance(k, str) for k in key)
                or type(value) not in (int, float) or not math.isfinite(value)):
            raise ValueError('invalid checkpoint weight')
        if tuple(key) in weights:
            raise ValueError('duplicate checkpoint weight')
        weights[tuple(key)] = value
    if not weights:
        raise ValueError('empty checkpoint')
    return ShadowPolicy(model_id, MappingProxyType(weights))


def observe_capture(policy, capture):
    """Audit validation precedes prediction; Forge's choice is compared afterward."""
    reject_private(capture)
    if (capture.get('schema_version') != 'stage8-capture-v1'
            or type(capture.get('information_set_samples')) is not int
            or capture['information_set_samples'] != 3):
        raise ValueError('wrong capture schema or information-set sample count')
    state = capture.get('public_state', {})
    if state.get('schema_version') != 'stage7d-v1' or state.get('search_policy') != 'fixed-root-v1':
        raise ValueError('unverified public-state producer')
    if (type(capture.get('decision_index')) is not int or capture['decision_index'] < 0
            or type(capture.get('run_seed')) is not int
            or state.get('decision_index') != capture['decision_index']
            or type(state.get('infoset_sample_count')) is not int
            or state['infoset_sample_count'] != 3):
        raise ValueError('capture and public-state audit metadata disagree')
    candidates = capture.get('candidates', [])
    if not isinstance(candidates, list) or not candidates or any(
            not isinstance(c, dict) or type(c.get('replay_valid_count')) is not int
            or c['replay_valid_count'] != 3 for c in candidates):
        raise ValueError('partial-world or empty candidate set')
    result = policy.predict(state, candidates)
    selected = capture.get('selected_action')
    if selected not in {c['action_identity'] for c in candidates}:
        raise ValueError('Forge proposal is absent from the captured candidates')
    result.update({
        'decision_index': capture['decision_index'],
        'run_seed': capture.get('run_seed'),
        'forge_proposed_action': selected,
        'forge_proposal_in_top_set': selected in result['top_actions'],
        'source_validation': 'pending whole-log audit',
    })
    return result


def observe_stream(policy, source, output):
    counts = {'recommendations': 0, 'rejected': 0}
    for line in source:
        if not line.startswith(CAPTURE_PREFIX):
            continue
        try:
            record = observe_capture(policy, json.loads(line[len(CAPTURE_PREFIX):]))
            counts['recommendations'] += 1
        except (ValueError, TypeError, KeyError, AttributeError, AssertionError) as exc:
            counts['rejected'] += 1
            record = {'schema_version': 'stage8d-shadow-recommendation-v1',
                      'status': 'rejected', 'mode': 'shadow-only', 'promotion_allowed': False,
                      'model_id': policy.model_id, 'reason': str(exc)}
        output.write(canonical_json(record) + '\n')
        output.flush()
    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    export = sub.add_parser('export')
    export.add_argument('development', type=Path)
    export.add_argument('--output', required=True, type=Path)
    observe = sub.add_parser('observe')
    observe.add_argument('checkpoint', type=Path)
    args = parser.parse_args()
    if args.command == 'export':
        if args.output.resolve() == args.development.resolve():
            raise ValueError('checkpoint output must not overwrite the source corpus')
        checkpoint = export_checkpoint(args.development)
        args.output.write_text(canonical_json(checkpoint) + '\n')
        print('Frozen shadow checkpoint: ' + checkpoint['model_id'])
    else:
        counts = observe_stream(load_checkpoint(args.checkpoint), sys.stdin, sys.stdout)
        print(canonical_json(counts), file=sys.stderr)
        if counts['rejected'] or not counts['recommendations']:
            raise SystemExit(1)


if __name__ == '__main__':
    main()
