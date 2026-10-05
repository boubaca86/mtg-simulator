#!/usr/bin/env python3
"""Paired offline comparison. No model is promoted into Forge move selection.

All models share rows, split, optimizer and predeclared hyperparameters. Related
games using the same corpus seed are kept together as well as whole games.
"""
import argparse
import hashlib
import json
import math
import random
from collections import defaultdict
from pathlib import Path

import stage7_semantic_value_model as semantic
import stage7_value_baseline as baseline
from stage7_dataset_validation import validate_labeled_rows


def split_rows(rows, seed=7, holdout=.25):
    if not 0 < holdout < 1:
        raise ValueError('holdout must be between zero and one')
    by_game = defaultdict(set)
    for row in rows:
        if 'corpus_seed' not in row:
            raise ValueError('missing corpus_seed: related games must stay in one split')
        by_game[str(row['game_group'])].add(str(row['corpus_seed']))
    if any(len(v) != 1 for v in by_game.values()):
        raise ValueError('one game has multiple corpus seeds')
    groups = sorted({str(r['corpus_seed']) for r in rows})
    if len(groups) < 4:
        raise ValueError('need at least four independent corpus seed groups')
    random.Random(seed).shuffle(groups)
    n = max(1, min(len(groups) - 1, round(len(groups) * holdout)))
    held = set(groups[:n])
    train = [r for r in rows if str(r['corpus_seed']) not in held]
    test = [r for r in rows if str(r['corpus_seed']) in held]
    for name, part in [('training', train), ('holdout', test)]:
        if not {0., 1.} <= {r['game_result'] for r in part}:
            raise ValueError(f'{name} needs both actual wins and losses; do not seed-shop')
    if {r['game_group'] for r in train} & {r['game_group'] for r in test}:
        raise ValueError('same-game leakage')
    return train, test


def metrics(rows, predictions):
    per_game = defaultdict(list)
    for row, p in zip(rows, predictions):
        p = min(1 - 1e-9, max(1e-9, p))
        y = row['game_result']
        per_game[row['game_group']].append((-(y * math.log(p) + (1 - y) * math.log(1 - p)), (p - y) ** 2))
    game_scores = {g: {'logloss': sum(v[0] for v in vals) / len(vals),
                       'brier': sum(v[1] for v in vals) / len(vals)} for g, vals in sorted(per_game.items())}
    return {'row_logloss': sum(-r['game_result'] * math.log(min(1-1e-9, max(1e-9, p)))
                              -(1-r['game_result']) * math.log(1-min(1-1e-9, max(1e-9, p)))
                              for r, p in zip(rows, predictions)) / len(rows),
            'game_mean_logloss': sum(v['logloss'] for v in game_scores.values()) / len(game_scores),
            'game_mean_brier': sum(v['brier'] for v in game_scores.values()) / len(game_scores),
            'per_game': game_scores}


def compare(rows, *, seed=7, holdout=.25, hash_dim=256, epochs=1800):
    validate_labeled_rows(rows, current_schema=True)
    if hash_dim < 1:
        raise ValueError('hash dimension must be positive')
    train, test = split_rows(rows, seed, holdout)
    ys = [r['game_result'] for r in train]
    extractors = {'combined_counts': baseline.features,
                  'perspective_counts': lambda r: semantic.features(r, 0),
                  'visible_identity_hash': lambda r: semantic.features(r, hash_dim)}
    models, weights = {}, {}
    prior = sum(ys) / len(ys)
    models['training_prior'] = {'holdout': metrics(test, [prior] * len(test))}
    for name, features in extractors.items():
        tx = [features(r) for r in train]
        vx = [features(r) for r in test]
        w = semantic.fit(tx, ys, epochs=epochs, lr=.002, l2=3e-4)
        weights[name] = w
        predict = lambda xs: [semantic.sigmoid(semantic.dot(w, x)) for x in xs]
        models[name] = {'feature_count': len(w), 'train': metrics(train, predict(tx)),
                        'holdout': metrics(test, predict(vx))}
    describe = lambda part: {'rows': len(part), 'games': sorted({r['game_group'] for r in part}),
                             'corpus_seeds': sorted({r['corpus_seed'] for r in part}),
                             'outcomes': {str(y): sum(r['game_result'] == y for r in part) for y in (0., .5, 1.)}}
    identity_loss = models['visible_identity_hash']['holdout']['game_mean_logloss']
    deltas = {name: identity_loss - models[name]['holdout']['game_mean_logloss']
              for name in ('combined_counts', 'perspective_counts', 'training_prior')}
    return {'schema': 'stage7c-paired-offline-v1', 'train': describe(train), 'holdout': describe(test),
            'split_seed': seed, 'holdout_fraction': holdout,
            'optimizer': {'epochs': epochs, 'lr': .002, 'l2': 3e-4}, 'hash_dim': hash_dim,
            'models': models, 'weights': weights, 'identity_minus_baseline_logloss': deltas,
            'promotion_allowed': False,
            'limitations': ['Small offline corpus; outcome prediction is not playing strength.',
                            'Card identity hashing does not encode card rules or tactical reasoning.',
                            'Stage 6 remains diagnostic; complete-action replay/aggregation is unfinished.',
                            'The historical 0.400665 result used incorrect winner labels and is invalid.']}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('jsonl', type=Path)
    ap.add_argument('--output', type=Path, required=True)
    ap.add_argument('--seed', type=int, default=7)
    ap.add_argument('--holdout', type=float, default=.25)
    ap.add_argument('--hash-dim', type=int, default=256)
    args = ap.parse_args()
    raw = args.jsonl.read_bytes()
    rows = [json.loads(x) for x in raw.splitlines() if x.strip()]
    report = compare(rows, seed=args.seed, holdout=args.holdout, hash_dim=args.hash_dim)
    report['dataset_sha256'] = hashlib.sha256(raw).hexdigest()
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + '\n')
    print(json.dumps({k: v for k, v in report.items() if k not in ('models', 'weights')}, sort_keys=True))
    for name, model in report['models'].items():
        print(f"{name}: holdout game-mean logloss={model['holdout']['game_mean_logloss']:.6f}")


if __name__ == '__main__':
    main()
