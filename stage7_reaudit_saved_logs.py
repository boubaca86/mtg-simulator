#!/usr/bin/env python3
"""Forensic report only: old action-audit data remains quarantined from training."""
import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

from stage7_label_outcomes import label_log


def audit(directory):
    original_path = directory / 'stage7b-labeled.jsonl'
    original = [json.loads(x) for x in original_path.read_text().splitlines() if x.strip()]
    old = {(r['game_group'], r['decision_index']): r for r in original}
    sources, corrected, changed = [], [], 0
    for path in sorted(directory.glob('stage7b-*.log')):
        content = path.read_text()
        source = {'name': path.name, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                  'terminal_lines': [x for x in content.splitlines() if x.startswith('Game Result:')],
                  'timeout_count': content.count('Stopping slow match as draw')}
        try:
            rows = label_log(path, expected_games=2)
        except ValueError as exc:
            source['label_repair_status'] = 'quarantined'
            source['reason'] = str(exc)
        else:
            source['label_repair_status'] = 'terminal_labels_reconstructed_only'
            source['rows'] = len(rows)
            source['corrected_row_outcomes'] = dict(Counter(str(r['game_result']) for r in rows))
            games = {r['game_group']: r['game_result'] for r in rows}
            source['acting_player_0_game_outcomes'] = dict(Counter(str(v) for v in games.values()))
            source['changed_row_labels'] = sum(old[(r['game_group'], r['decision_index'])]['game_result'] != r['game_result'] for r in rows)
            changed += source['changed_row_labels']
            corrected.extend(rows)
        sources.append(source)
    benchmark_games = {r['game_group']: r['game_result'] for r in corrected}
    target_missing = [r for r in original if 'target ' in r['complete_action_identity'].lower()
                      and '|targets=<none>' in r['complete_action_identity']]
    return {'source_run': 37241369454, 'source_artifact': 11318401398,
            'original_dataset_sha256': hashlib.sha256(original_path.read_bytes()).hexdigest(),
            'original_rows': len(original), 'original_games': len({r['game_group'] for r in original}),
            'original_row_outcomes': dict(Counter(str(r['game_result']) for r in original)),
            'timeout_events': sum(s['timeout_count'] for s in sources),
            'quarantined_source_logs': sum(s['label_repair_status'] == 'quarantined' for s in sources),
            'reconstructed_terminal_rows': len(corrected), 'changed_row_labels': changed,
            'benchmark_game_outcomes_for_full_actor_0': dict(Counter(str(v) for v in benchmark_games.values())),
            'target_word_actions_with_missing_target_identity': len(target_missing),
            'training_eligible': False,
            'reason': 'All original observations predate the corrected root-choice audit; terminal-label repair alone cannot make them information-set clean.',
            'sources': sources}


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('directory', type=Path)
    ap.add_argument('output', type=Path)
    args = ap.parse_args()
    report = audit(args.directory)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
    print(json.dumps({k: v for k, v in report.items() if k != 'sources'}, indent=2, sort_keys=True))
