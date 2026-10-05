#!/usr/bin/env python3
"""Paired, context-sensitive offline ranking. No model controls Forge gameplay."""
from __future__ import annotations
import argparse
import hashlib
import json
import math
from collections import defaultdict
from pathlib import Path

import stage8_legacy_ranker as legacy
from stage7_dataset_validation import reject_hidden, validate_observation
from stage8_ranker_features import MODELS, canonical_action, features, public_context
from stage8_validate_counterfactual import FORBIDDEN_KEYS, validate_row, walk_keys

CONFIG = {'epochs': 20, 'learning_rate': .05, 'averaged_weights': True,
          'normalize_updates': True, 'selection': 'uniform-among-predicted-ties'}


def validate_rows(rows):
    if not rows:
        raise ValueError('empty ranking dataset')
    decisions = set()
    games = {}
    for i, row in enumerate(rows, 1):
        validate_row(row, i)
        reject_hidden(row)
        if FORBIDDEN_KEYS & set(walk_keys(row)):
            raise ValueError('forbidden hidden-information field in ranking dataset')
        decision, game, seed = row['decision_id'], row['game_group'], row['corpus_seed']
        if not isinstance(decision, str) or not decision or decision in decisions:
            raise ValueError('missing or duplicate decision_id')
        if not isinstance(game, str) or not game:
            raise ValueError('missing game_group')
        if type(seed) is not int:
            raise ValueError('corpus_seed must be an integer')
        if game in games and games[game] != seed:
            raise ValueError('one complete game crosses corpus seed families')
        games[game] = seed
        decisions.add(decision)
        validate_observation(row['public_state'])
        public_context(row['public_state'], 'public_semantics')
        for candidate in row['candidates']:
            canonical_action(candidate['action_identity'])


def dot(weights, vector):
    return sum(weights.get(key, 0.) * value for key, value in vector.items())


def train(rows, vectors, epochs=20, lr=.05):
    """Normalized averaged perceptron; equal-score optimal actions are interchangeable."""
    weights, totals, last = {}, {}, {}
    step = 0
    for _ in range(epochs):
        for row in sorted(rows, key=lambda r: r['decision_id']):
            step += 1
            xs = vectors[row['decision_id']]
            cs = row['candidates']
            predicted = max(range(len(cs)), key=lambda i: dot(weights, xs[i]))
            gold_score = max(c['aggregate_score'] for c in cs)
            if cs[predicted]['aggregate_score'] == gold_score:
                continue
            good = [i for i, c in enumerate(cs) if c['aggregate_score'] == gold_score]
            gold = max(good, key=lambda i: dot(weights, xs[i]))
            delta = dict(xs[gold])
            for key, value in xs[predicted].items():
                delta[key] = delta.get(key, 0.) - value
            norm = max(1., sum(v*v for v in delta.values()))
            for key, value in delta.items():
                if not value:
                    continue
                totals[key] = totals.get(key, 0.) + (step-last.get(key, 0))*weights.get(key, 0.)
                last[key] = step
                weights[key] = weights.get(key, 0.) + lr*value/norm
    if not step:
        raise ValueError('empty training fold')
    return {key: (totals.get(key, 0.) + (step-last[key])*value)/step
            for key, value in weights.items()}


def decision_metrics(row, predictions):
    """Expected performance for uniform tie-breaking, without peeking at labels."""
    cs = row['candidates']
    if len(predictions) != len(cs) or not all(math.isfinite(x) for x in predictions):
        raise ValueError('one finite prediction is required for each candidate')
    high = max(predictions)
    chosen = [i for i, p in enumerate(predictions) if math.isclose(p, high, rel_tol=1e-12, abs_tol=1e-12)]
    scores = [c['aggregate_score'] for c in cs]
    best, worst = max(scores), min(scores)
    raw_regret = sum(best-scores[i] for i in chosen)/len(chosen)
    pair_ok, pairs = 0., 0
    for i, a in enumerate(scores):
        for j, b in enumerate(scores):
            if a > b:
                pairs += 1
                if math.isclose(predictions[i], predictions[j], rel_tol=1e-12, abs_tol=1e-12):
                    pair_ok += .5
                elif predictions[i] > predictions[j]:
                    pair_ok += 1.
    return {'decision_id': row['decision_id'], 'game_group': row['game_group'],
            'top1': sum(scores[i] == best for i in chosen)/len(chosen),
            'regret': raw_regret, 'normalized_regret': raw_regret/(best-worst) if best > worst else 0.,
            'pair_ok': pair_ok, 'pairs': pairs, 'strict': best > worst,
            'model_tied': len(chosen) > 1, 'terminal_scale': any(abs(s) >= 1_000_000_000 for s in scores)}


def summarize(records):
    if not records:
        raise ValueError('empty evaluation fold')
    n = len(records)
    pairs = sum(r['pairs'] for r in records)
    strict = [r for r in records if r['strict']]
    return {'decisions': n, 'informative_decisions': len(strict),
            'top1_accuracy': sum(r['top1'] for r in records)/n,
            'informative_top1_accuracy': sum(r['top1'] for r in strict)/len(strict) if strict else None,
            'mean_regret': sum(r['regret'] for r in records)/n,
            'normalized_regret': sum(r['normalized_regret'] for r in records)/n,
            'pairwise_accuracy': sum(r['pair_ok'] for r in records)/pairs if pairs else None,
            'pairwise_comparisons': pairs,
            'prediction_tie_decisions': sum(r['model_tied'] for r in records),
            'terminal_scale_decisions': sum(r['terminal_scale'] for r in records)}


def aggregate(records):
    out = summarize(records)
    groups = defaultdict(list)
    for record in records:
        groups[record['game_group']].append(record)
    per_game = {game: summarize(rs) for game, rs in sorted(groups.items())}
    out['games'] = len(per_game)
    out['game_macro'] = {k: sum(m[k] for m in per_game.values())/len(per_game)
                         for k in ('top1_accuracy', 'normalized_regret')}
    out['per_game'] = per_game
    return out


def compare(rows):
    validate_rows(rows)
    seeds = sorted({r['corpus_seed'] for r in rows})
    if len(seeds) < 3:
        raise ValueError('ranking requires at least three independent corpus seeds')
    report = {}
    for model in ('uniform', 'legacy_action_hash', *MODELS):
        vectors = None if model in ('uniform', 'legacy_action_hash') else {
            r['decision_id']: [features(r, c, model) for c in r['candidates']] for r in rows}
        folds, all_records = [], []
        for seed in seeds:
            train_rows = [r for r in rows if r['corpus_seed'] != seed]
            holdout = [r for r in rows if r['corpus_seed'] == seed]
            if model == 'legacy_action_hash':
                weights = legacy.train(train_rows, epochs=CONFIG['epochs'], lr=CONFIG['learning_rate'])
            elif model != 'uniform':
                weights = train(train_rows, vectors, epochs=CONFIG['epochs'], lr=CONFIG['learning_rate'])
            records = []
            for row in holdout:
                if model == 'uniform':
                    predictions = [0.] * len(row['candidates'])
                elif model == 'legacy_action_hash':
                    predictions = [legacy.dot(weights, legacy.features(row, c)) for c in row['candidates']]
                else:
                    predictions = [dot(weights, x) for x in vectors[row['decision_id']]]
                records.append(decision_metrics(row, predictions))
            all_records.extend(records)
            folds.append({'holdout_seed': seed, 'train_seeds': [s for s in seeds if s != seed],
                          'train_decisions': len(train_rows), 'holdout': aggregate(records)})
        report[model] = {'folds': folds, 'holdout': aggregate(all_records)}
    # Assert pairing at the reporting boundary, not merely in the train/test splitter.
    games = set(report['uniform']['holdout']['per_game'])
    if any(set(m['holdout']['per_game']) != games for m in report.values()):
        raise ValueError('models were evaluated on different held-out games')
    selected_records = [decision_metrics(r, [float(c['action_identity'] == r['selected_action'])
                                            for c in r['candidates']]) for r in rows]
    selected_reference = aggregate(selected_records)
    # The search produced these labels. Its top-1/regret is a consistency reference,
    # not out-of-sample model evidence; the binary choice does not order alternatives.
    for summary in [selected_reference, *selected_reference['per_game'].values()]:
        summary.pop('pairwise_accuracy')
        summary.pop('pairwise_comparisons')
    return {'schema_version': 'stage8b-ranking-v3', 'method': 'leave-one-seed-out-context-action-interactions',
            'promotion_allowed': False, 'seed_count': len(seeds), 'config': CONFIG,
            'models': report, 'holdout': report['public_semantics']['holdout'],
            'forge_selected_reference': selected_reference,
            'limitations': [
                'Development corpus reused across feature design; fresh seed families are required before promotion.',
                'Labels imitate the existing fixed-root Forge evaluator, not independently proven optimal play.',
                'Captured proposals omit passing and include phase probes that may be deferred.',
                'Transient IDs are excluded from features; same-name targets can remain indistinguishable.',
                'Legacy raw regret is dominated by Forge terminal-score sentinels; normalized regret is also reported.'
            ]}


def coverage(rows, observations):
    rankable = len(rows)
    if observations is None:
        return {'rankable_decisions': rankable, 'captured_proposals': None, 'fraction': None}
    keys = [(r['game_group'], r['decision_index']) for r in observations]
    if len(keys) != len(set(keys)):
        raise ValueError('duplicate source observation')
    available = set(keys)
    matched = {(r['game_group'], r['public_state']['decision_index']) for r in rows}
    if len(matched) != len(rows) or not matched <= available:
        raise ValueError('ranking rows do not match the supplied source observations')
    return {'rankable_decisions': rankable, 'captured_proposals': len(observations),
            'fraction': rankable/len(observations),
            'rankable_games': len({r['game_group'] for r in rows}),
            'captured_games': len({r['game_group'] for r in observations}),
            'scope': 'captured selected search proposals, not all real priority decisions'}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('jsonl', type=Path)
    ap.add_argument('--output', type=Path, default=Path('stage8b-ranking.json'))
    ap.add_argument('--observations', type=Path)
    args = ap.parse_args()
    raw = args.jsonl.read_bytes()
    rows = [json.loads(x) for x in raw.splitlines() if x.strip()]
    result = compare(rows)
    observations = ([json.loads(x) for x in args.observations.read_text().splitlines() if x.strip()]
                    if args.observations else None)
    result['coverage'] = coverage(rows, observations)
    result['dataset_sha256'] = hashlib.sha256(raw).hexdigest()
    if args.observations:
        result['observations_sha256'] = hashlib.sha256(args.observations.read_bytes()).hexdigest()
    args.output.write_text(json.dumps(result, sort_keys=True, indent=2)+'\n')
    for name, model in result['models'].items():
        h = model['holdout']
        print(f"{name}: top1={h['top1_accuracy']:.4f} normalized_regret={h['normalized_regret']:.4f} pairwise={h['pairwise_accuracy']}")
    print('Offline only; promotion_allowed=false')


if __name__ == '__main__':
    main()
