#!/usr/bin/env python3
"""Predeclared Stage 8 fresh-seed replication evaluator.

Train once on the frozen development corpus and evaluate untouched fresh seed
families. No model controls Forge gameplay.
"""
from __future__ import annotations
import argparse, hashlib, json
from collections import defaultdict
from pathlib import Path
import stage8_action_ranker as ranker
import stage8_legacy_ranker as legacy
from stage8_ranker_features import MODELS, features

DEV_SHA='106912cfd8800cad8abe65231abeca03578c4185205a2e11b19e87000cc8ee7e'
DEV_SEEDS={20261004,20261005,20261006,20261007}
FRESH_SEEDS={20261008,20261009,20261010,20261011}


def load(path):
    raw=path.read_bytes(); rows=[json.loads(x) for x in raw.splitlines() if x.strip()]
    ranker.validate_rows(rows); return raw,rows

def game_seed_macro(records, rows):
    seed_for={r['decision_id']:r['corpus_seed'] for r in rows}
    by_seed=defaultdict(list)
    for rec in records: by_seed[seed_for[rec['decision_id']]].append(rec)
    per={}
    for seed, rs in sorted(by_seed.items()):
        a=ranker.aggregate(rs)
        per[str(seed)]={'games':a['games'],'game_macro':a['game_macro'],'decision_weighted':{k:a[k] for k in ('top1_accuracy','normalized_regret','pairwise_accuracy')}}
    return per

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('development',type=Path); ap.add_argument('fresh',type=Path); ap.add_argument('--output',type=Path,default=Path('stage8-fresh-seed-report.json')); a=ap.parse_args()
    dev_raw,dev=load(a.development); fresh_raw,fresh=load(a.fresh)
    if hashlib.sha256(dev_raw).hexdigest()!=DEV_SHA: raise ValueError('development corpus hash differs from predeclared frozen artifact')
    if {r['corpus_seed'] for r in dev}!=DEV_SEEDS: raise ValueError('wrong development seed families')
    if {r['corpus_seed'] for r in fresh}!=FRESH_SEEDS: raise ValueError('fresh corpus does not contain exactly the four predeclared families')
    if {r['corpus_seed'] for r in dev}&{r['corpus_seed'] for r in fresh}: raise ValueError('training/fresh seed leakage')
    models={}; records_by_model={}
    for model in ('uniform','legacy_action_hash',*MODELS):
        if model=='legacy_action_hash': weights=legacy.train(dev,epochs=ranker.CONFIG['epochs'],lr=ranker.CONFIG['learning_rate'])
        elif model not in ('uniform',):
            dv={r['decision_id']:[features(r,c,model) for c in r['candidates']] for r in dev}; weights=ranker.train(dev,dv,epochs=ranker.CONFIG['epochs'],lr=ranker.CONFIG['learning_rate'])
        records=[]
        for row in fresh:
            if model=='uniform': pred=[0.]*len(row['candidates'])
            elif model=='legacy_action_hash': pred=[legacy.dot(weights,legacy.features(row,c)) for c in row['candidates']]
            else: pred=[ranker.dot(weights,features(row,c,model)) for c in row['candidates']]
            records.append(ranker.decision_metrics(row,pred))
        records_by_model[model]=records
        models[model]={'overall':ranker.aggregate(records),'per_seed':game_seed_macro(records,fresh)}
    pc=models['public_counts']; ao=models['action_only']; diffs=[]
    for seed in sorted(FRESH_SEEDS):
        p=pc['per_seed'][str(seed)]['game_macro']; q=ao['per_seed'][str(seed)]['game_macro']; diffs.append({'seed':seed,'regret_improvement':q['normalized_regret']-p['normalized_regret'],'top1_delta':p['top1_accuracy']-q['top1_accuracy']})
    mean_imp=sum(x['regret_improvement'] for x in diffs)/4
    mean_top=sum(x['top1_delta'] for x in diffs)/4
    lower=sum(x['regret_improvement']>0 for x in diffs)
    positive=mean_imp>=.01 and lower>=3 and mean_top>=-.02
    report={'schema_version':'stage8-fresh-seed-v1','promotion_allowed':False,'development_sha256':hashlib.sha256(dev_raw).hexdigest(),'fresh_sha256':hashlib.sha256(fresh_raw).hexdigest(),'config':ranker.CONFIG,'models':models,'primary_contrast':{'public_counts_vs_action_only':diffs,'seed_macro_regret_improvement':mean_imp,'families_with_lower_regret':lower,'seed_macro_top1_delta':mean_top,'positive_replication':positive},'limitations':['Offline imitation of Forge fixed-root labels only.','No learned model controls gameplay.','Passing and uncaptured priority windows remain outside proposal coverage.']}
    a.output.write_text(json.dumps(report,sort_keys=True,indent=2)+'\n')
    print(json.dumps(report['primary_contrast'],sort_keys=True))
    print('Offline only; promotion_allowed=false')
if __name__=='__main__': main()
