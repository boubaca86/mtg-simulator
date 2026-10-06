#!/usr/bin/env python3
"""Audit shadow predictions on complete source logs against the frozen experiment."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

import stage8_action_ranker as base
from stage7_label_outcomes import label_log
from stage8_label_counterfactual import parse_log
from stage8_serialize_counterfactual import normalize, canonical_line
from stage8d_shadow_policy import CAPTURE_PREFIX, canonical_json, load_checkpoint, observe_capture


def replay(checkpoint, capture_dir, output_dir):
    policy = load_checkpoint(checkpoint)
    reference = json.loads((capture_dir / 'stage8c-fresh-seed-report.json').read_text())
    raw = (capture_dir / 'stage8c-fresh-counterfactual.jsonl').read_bytes()
    if hashlib.sha256(raw).hexdigest() != reference['fresh_sha256']:
        raise ValueError('fresh reference corpus hash mismatch')
    rows = [json.loads(line) for line in raw.splitlines() if line.strip()]
    base.validate_rows(rows)
    ranked = {(r['game_group'], r['public_state']['decision_index']): r for r in rows}
    if len(ranked) != len(rows):
        raise ValueError('duplicate ranking proposal')
    recommendations, metrics, observations, regenerated, files = [], [], [], [], []
    for seed in (20261012, 20261013, 20261014, 20261015):
        for orientation in ('st-first', 'red-first'):
            path = capture_dir / f'stage8c-fresh-{orientation}-{seed}.log'
            # Audit the WHOLE log before accepting any of its recommendations.
            observed = label_log(path, expected_games=4)
            observations.extend(observed)
            regenerated.extend(normalize(r) for r in parse_log(path, 4, corpus_seed=seed))
            observation_keys = {(r['game_group'], r['decision_index']) for r in observed}
            matched = set()
            game = 1
            source_sha = hashlib.sha256(path.read_bytes()).hexdigest()
            for line in path.read_text().splitlines():
                if line.startswith('Game Result: Game '):
                    game += 1
                if not line.startswith(CAPTURE_PREFIX):
                    continue
                capture = json.loads(line[len(CAPTURE_PREFIX):])
                result = observe_capture(policy, capture)
                group = f'{path.stem}:game-{game}'
                key = (group, result['decision_index'])
                if key in matched or key not in observation_keys:
                    raise ValueError('shadow proposal does not match unique source observation')
                matched.add(key)
                result.update(source_log=path.name, game_group=group,
                              source_validation='whole-log audit passed')
                recommendations.append(result)
                if key in ranked:
                    r = ranked[key]
                    predictions = {c['action_identity']: c['model_score'] for c in result['candidate_scores']}
                    metrics.append(base.decision_metrics(r, [predictions[c['action_identity']] for c in r['candidates']]))
            if matched != observation_keys:
                raise ValueError('some captured proposals have no shadow recommendation')
            if hashlib.sha256(path.read_bytes()).hexdigest() != source_sha:
                raise ValueError('source log changed during shadow replay')
            files.append({'file': path.name, 'sha256': source_sha, 'proposals': len(matched)})
    if ''.join(canonical_line(r)+'\n' for r in regenerated).encode() != raw:
        raise ValueError('source logs do not reproduce reference corpus')
    aggregate = base.aggregate(metrics)
    if aggregate != reference['models']['target_semantics']['overall']:
        raise ValueError('exported inference differs from the frozen Python model')
    report = {
        'schema_version': 'stage8d-shadow-replay-v1', 'mode': 'shadow-only',
        'promotion_allowed': False, 'model_id': policy.model_id,
        'fresh_sha256': reference['fresh_sha256'], 'coverage': base.coverage(rows, observations),
        'proposals_scored': len(recommendations),
        'proposal_status_counts': dict(sorted(Counter(r['status'] for r in recommendations).items())),
        'forge_proposal_in_top_set_count': sum(r['forge_proposal_in_top_set'] for r in recommendations),
        'frozen_model_metrics_reproduced_exactly': True, 'source_logs_unchanged': True,
        'rankable_metrics': {k: v for k, v in aggregate.items() if k != 'per_game'},
        'source_logs': files,
        'limitations': ['Captured search proposals are not verified executed moves.',
                        'This replays the now-observed confirmation corpus; it is not new strength evidence.',
                        'Streaming recommendations remain provisional until whole-log audit.',
                        'Forge alone chooses and executes gameplay actions.'],
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / 'recommendations.jsonl').write_text(''.join(canonical_json(r)+'\n' for r in recommendations))
    (output_dir / 'report.json').write_text(json.dumps(report, sort_keys=True, indent=2)+'\n')
    return report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('checkpoint', type=Path)
    parser.add_argument('capture_dir', type=Path)
    parser.add_argument('--output-dir', required=True, type=Path)
    args = parser.parse_args()
    report = replay(args.checkpoint, args.capture_dir, args.output_dir)
    print(canonical_json({k: report[k] for k in ('model_id', 'proposals_scored', 'proposal_status_counts',
                                               'frozen_model_metrics_reproduced_exactly')}))


if __name__ == '__main__':
    main()
