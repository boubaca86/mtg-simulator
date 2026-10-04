#!/usr/bin/env python3
"""Stage 7B transparent value-model baseline.

Consumes Stage 7 legal-information JSONL only. No Forge/card rules are changed.
Rows are split by complete game_group before fitting so decisions from one game can
never straddle training and validation. run_seed remains provenance, not a split key.
"""
from __future__ import annotations
import argparse, json, math, random
from pathlib import Path

FORBIDDEN = {'opponent_hand','opponent_hand_cards','own_library','opponent_library','library_order','future_draws'}


def load(path):
    rows=[json.loads(x) for x in Path(path).read_text().splitlines() if x.strip()]
    if not rows: raise ValueError('empty dataset')
    for r in rows:
        bad=FORBIDDEN & r.keys()
        if bad: raise ValueError(f'hidden-information leakage: {sorted(bad)}')
        if not r.get('complete_action_identity'): raise ValueError('missing complete action identity')
        if not r.get('game_group'): raise ValueError('missing game_group; split must be by complete Forge game')
    return rows


def features(r):
    return [1.0,
            float(r.get('acting_life',0)), float(r.get('opponent_life',0)),
            float(len(r.get('own_hand',[]))),
            float(r.get('opponent_unknown_hand_count',0)),
            float(r.get('own_library_count',0)), float(r.get('opponent_library_count',0)),
            float(len(r.get('battlefield_public',[]))), float(len(r.get('graveyard_public',[]))),
            float(len(r.get('exile_public',[]))), float(len(r.get('stack_public',[]))),
            float(r.get('turn',0))]


def sigmoid(z):
    z=max(-40.0,min(40.0,z)); return 1/(1+math.exp(-z))


def dot(a,b): return sum(x*y for x,y in zip(a,b))


def fit(xs,ys,epochs=1500,lr=.002,l2=1e-4):
    w=[0.0]*len(xs[0])
    for _ in range(epochs):
        g=[0.0]*len(w)
        for x,y in zip(xs,ys):
            e=sigmoid(dot(w,x))-y
            for j,v in enumerate(x): g[j]+=e*v
        n=len(xs)
        for j in range(len(w)):
            reg=0 if j==0 else l2*w[j]
            w[j]-=lr*(g[j]/n+reg)
    return w


def logloss(w,xs,ys):
    if not xs: return None
    s=0.0
    for x,y in zip(xs,ys):
        p=min(1-1e-9,max(1e-9,sigmoid(dot(w,x))))
        s-=y*math.log(p)+(1-y)*math.log(1-p)
    return s/len(xs)


def main():
    ap=argparse.ArgumentParser(); ap.add_argument('jsonl'); ap.add_argument('--holdout',type=float,default=.25); ap.add_argument('--seed',type=int,default=7)
    a=ap.parse_args(); rows=load(a.jsonl)
    label='game_result'
    if any(label not in r for r in rows):
        raise SystemExit('Stage 7B BLOCKED: dataset lacks game_result labels; capture eventual Forge game outcomes before training.')
    for r in rows:
        y=float(r[label])
        if not 0.0 <= y <= 1.0:
            raise SystemExit(f'Stage 7B BLOCKED: invalid game_result {y}; expected 0 loss, 0.5 draw, or 1 win.')
    groups=sorted({str(r['game_group']) for r in rows}); random.Random(a.seed).shuffle(groups)
    n=max(1,int(round(len(groups)*a.holdout))) if len(groups)>1 else 0
    held=set(groups[:n]); train=[r for r in rows if str(r['game_group']) not in held]; test=[r for r in rows if str(r['game_group']) in held]
    if not train or not test: raise SystemExit('Stage 7B BLOCKED: need at least two complete games for leakage-safe holdout.')
    if {r['game_group'] for r in train} & {r['game_group'] for r in test}: raise SystemExit('Stage 7B BLOCKED: same-game train/holdout leakage')
    y=lambda r: float(r[label])
    tx=[features(r) for r in train]; ty=[y(r) for r in train]; vx=[features(r) for r in test]; vy=[y(r) for r in test]
    w=fit(tx,ty)
    names=['bias','acting_life','opponent_life','own_hand_count','opponent_unknown_hand_count','own_library_count','opponent_library_count','battlefield_public_count','graveyard_public_count','exile_public_count','stack_public_count','turn']
    print(json.dumps({'schema':'stage7b-linear-v2-game-holdout','train_rows':len(train),'holdout_rows':len(test),'train_game_groups':len(set(str(r['game_group']) for r in train)),'holdout_game_groups':len(held),'train_logloss':logloss(w,tx,ty),'holdout_logloss':logloss(w,vx,vy),'weights':dict(zip(names,w))},sort_keys=True))

if __name__=='__main__': main()
