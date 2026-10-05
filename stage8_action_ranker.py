#!/usr/bin/env python3
"""Stage 8B: deterministic offline action-ranking baseline.

Learns only from Stage 8A learner-facing rows. Forge remains the rules referee;
this model never controls gameplay. Splits are by whole corpus seed so no game
or decision from a held-out seed family can leak into training.
"""
from __future__ import annotations
import argparse, hashlib, json, math
from pathlib import Path


def bucket(text: str, n: int) -> int:
    return int.from_bytes(hashlib.sha256(text.encode()).digest()[:8], 'big') % n


def features(row: dict, candidate: dict, n: int = 257) -> dict[int, float]:
    s=row['public_state']; f={0:1.0}
    def add(k,v=1.0):
        i=1+bucket(k,n-1); f[i]=f.get(i,0.0)+v
    add('phase='+str(s.get('phase',''))); add('turn', float(s.get('turn',0))/20.0)
    for key in ('acting_life','opponent_life','opponent_unknown_hand_count','own_library_count','opponent_library_count'):
        if isinstance(s.get(key),(int,float)): add(key,float(s[key])/20.0)
    for key in ('own_hand_semantics','own_battlefield_semantics','opponent_battlefield_semantics','own_graveyard_semantics','opponent_graveyard_semantics','exile_public_semantics','stack_public_semantics'):
        for x in s.get(key,[]): add(key+'='+str(x))
    # Complete action identity is legal/public at the decision boundary. Hashing
    # preserves exact targets/modes/X/choices without parsing card rules.
    add('action='+candidate['action_identity'])
    return f


def dot(w,f): return sum(w.get(i,0.0)*v for i,v in f.items())

def split(rows):
    seeds=sorted({r['corpus_seed'] for r in rows}, key=str)
    hold={s for s in seeds if bucket(str(s),4)==0}
    if not hold and seeds: hold={seeds[-1]}
    train=[r for r in rows if r['corpus_seed'] not in hold]; test=[r for r in rows if r['corpus_seed'] in hold]
    if not train or not test: raise ValueError('need nonempty seed-isolated train and holdout')
    return train,test,hold


def best(cands): return max(cands,key=lambda c:(c['aggregate_score'],c['action_identity']))

def train(rows, epochs=20, lr=.05):
    w={}
    for _ in range(epochs):
        for r in sorted(rows,key=lambda x:x['decision_id']):
            gold=best(r['candidates']); pred=max(r['candidates'],key=lambda c:(dot(w,features(r,c)),c['action_identity']))
            if pred['action_identity'] != gold['action_identity']:
                for i,v in features(r,gold).items(): w[i]=w.get(i,0)+lr*v
                for i,v in features(r,pred).items(): w[i]=w.get(i,0)-lr*v
    return w


def evaluate(rows,w):
    correct=0; pair_ok=pair_n=0; regret=0.0
    for r in rows:
        cs=r['candidates']; gold=best(cs); pred=max(cs,key=lambda c:(dot(w,features(r,c)),c['action_identity']))
        correct += pred['action_identity']==gold['action_identity']; regret += gold['aggregate_score']-pred['aggregate_score']
        for a in cs:
            for b in cs:
                if a['aggregate_score']>b['aggregate_score']:
                    pair_n+=1; pair_ok += dot(w,features(r,a))>dot(w,features(r,b))
    return {'decisions':len(rows),'top1_accuracy':correct/len(rows),'mean_regret':regret/len(rows),'pairwise_accuracy':pair_ok/pair_n if pair_n else None,'pairwise_comparisons':pair_n}


def main():
    ap=argparse.ArgumentParser(); ap.add_argument('jsonl',type=Path); ap.add_argument('--output',type=Path,default=Path('stage8b-ranking.json')); a=ap.parse_args()
    rows=[json.loads(x) for x in a.jsonl.read_text().splitlines() if x.strip()]
    train_rows,test_rows,hold=split(rows); w=train(train_rows)
    result={'schema_version':'stage8b-ranking-v1','method':'deterministic-pairwise-perceptron-public-state-plus-complete-action','promotion_allowed':False,'holdout_seeds':sorted(hold,key=str),'train':evaluate(train_rows,w),'holdout':evaluate(test_rows,w)}
    a.output.write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
    print(json.dumps(result,sort_keys=True))
if __name__=='__main__': main()
