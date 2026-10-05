#!/usr/bin/env python3
"""Paired offline evaluator comparison; never promotes a model into Forge play."""
import argparse, hashlib, json, math, random
from collections import defaultdict
from pathlib import Path
import stage7_semantic_value_model as semantic
import stage7_value_baseline as baseline
from stage7_dataset_validation import validate_labeled_rows

def split_rows(rows,seed=7,holdout=.25):
    if not 0<holdout<1: raise ValueError('holdout must be between zero and one')
    by_game=defaultdict(set)
    for r in rows:
        if 'corpus_seed' not in r: raise ValueError('missing corpus_seed: related games must stay in one split')
        by_game[str(r['game_group'])].add(str(r['corpus_seed']))
    if any(len(v)!=1 for v in by_game.values()): raise ValueError('one game has multiple corpus seeds')
    groups=sorted({str(r['corpus_seed']) for r in rows})
    if len(groups)<4: raise ValueError('need at least four independent corpus seed groups')
    random.Random(seed).shuffle(groups); n=max(1,min(len(groups)-1,round(len(groups)*holdout))); held=set(groups[:n])
    train=[r for r in rows if str(r['corpus_seed']) not in held]; test=[r for r in rows if str(r['corpus_seed']) in held]
    for name,part in [('training',train),('holdout',test)]:
        if not {0.,1.}<={r['game_result'] for r in part}: raise ValueError(f'{name} needs both actual wins and losses; do not seed-shop')
    if {r['game_group'] for r in train}&{r['game_group'] for r in test}: raise ValueError('same-game leakage')
    return train,test

def metrics(rows,predictions):
    per=defaultdict(list)
    for r,p in zip(rows,predictions):
        p=min(1-1e-9,max(1e-9,p)); y=r['game_result']; per[r['game_group']].append((-(y*math.log(p)+(1-y)*math.log(1-p)),(p-y)**2))
    gs={g:{'logloss':sum(x[0] for x in v)/len(v),'brier':sum(x[1] for x in v)/len(v)} for g,v in sorted(per.items())}
    return {'game_mean_logloss':sum(v['logloss'] for v in gs.values())/len(gs),'game_mean_brier':sum(v['brier'] for v in gs.values())/len(gs),'per_game':gs}

def compare(rows,seed=7,holdout=.25,hash_dim=256,epochs=1800):
    validate_labeled_rows(rows,current_schema=True); train,test=split_rows(rows,seed,holdout); ys=[r['game_result'] for r in train]
    ex={'combined_counts':baseline.features,'perspective_counts':lambda r:semantic.features(r,0),'visible_identity_hash':lambda r:semantic.features(r,hash_dim)}
    if all(r.get('schema_version')=='stage7d-v1' for r in rows): ex['public_card_semantics']=lambda r:semantic.features(r,hash_dim,True)
    models={}; weights={}; prior=sum(ys)/len(ys); models['training_prior']={'holdout':metrics(test,[prior]*len(test))}
    for name,f in ex.items():
        tx=[f(r) for r in train]; vx=[f(r) for r in test]; w=semantic.fit(tx,ys,epochs=epochs,lr=.002,l2=3e-4); weights[name]=w
        pred=lambda xs:[semantic.sigmoid(semantic.dot(w,x)) for x in xs]
        models[name]={'feature_count':len(w),'train':metrics(train,pred(tx)),'holdout':metrics(test,pred(vx))}
    identity=models['visible_identity_hash']['holdout']['game_mean_logloss']; semantic_loss=models.get('public_card_semantics',models['visible_identity_hash'])['holdout']['game_mean_logloss']
    return {'schema':'stage7d-paired-offline-v1' if 'public_card_semantics' in models else 'stage7c-paired-offline-v1','models':models,'weights':weights,'semantic_minus_identity_logloss':semantic_loss-identity,'promotion_allowed':False,'limitations':['Offline outcome prediction is not playing strength.','Public characteristics do not encode oracle-text interactions or tactical sequencing.','No model enters Forge move selection without a separate action-quality gate.']}

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('jsonl',type=Path); ap.add_argument('--output',type=Path,required=True); ap.add_argument('--seed',type=int,default=7); ap.add_argument('--holdout',type=float,default=.25); ap.add_argument('--hash-dim',type=int,default=256); a=ap.parse_args()
    raw=a.jsonl.read_bytes(); rows=[json.loads(x) for x in raw.splitlines() if x.strip()]; report=compare(rows,a.seed,a.holdout,a.hash_dim); report['dataset_sha256']=hashlib.sha256(raw).hexdigest(); a.output.write_text(json.dumps(report,sort_keys=True,indent=2)+'\n')
    for name,m in report['models'].items(): print(f"{name}: holdout game-mean logloss={m['holdout']['game_mean_logloss']:.6f}")
if __name__=='__main__': main()
