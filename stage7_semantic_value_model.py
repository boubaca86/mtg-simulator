#!/usr/bin/env python3
"""Stage 7C semantic value baseline.

Uses only Stage-7 legal observations. Public/own-known card names are hashed into
fixed-dimensional zone-aware features; unknown opponent hand/library identities
are never consumed. Splits are by complete Forge game_group.
"""
from __future__ import annotations
import argparse, hashlib, json, math, random
from pathlib import Path
from stage7_dataset_validation import stack_items, validate_labeled_rows

FORBIDDEN={'opponent_hand','opponent_hand_cards','own_library','opponent_library','library_order','future_draws'}
ZONES=('own_hand','opponent_known_cards','own_battlefield','opponent_battlefield','own_graveyard','opponent_graveyard','exile_public','stack_public')
LEGACY=('battlefield_public','graveyard_public')


def load(path):
    rows=[json.loads(x) for x in Path(path).read_text().splitlines() if x.strip()]
    if not rows: raise ValueError('empty dataset')
    return validate_labeled_rows(rows)


def _bucket(zone,name,dim):
    raw=(zone+'\0'+str(name)).encode('utf-8')
    return int.from_bytes(hashlib.sha256(raw).digest()[:8],'big') % dim


def features(r,dim):
    if dim < 0: raise ValueError('hash dimension must be nonnegative')
    # Transparent scalar core retained for direct comparison with Stage 7B.
    x=[1.0,float(r.get('acting_life',0)),float(r.get('opponent_life',0)),
       float(len(r.get('own_hand',[]))),float(r.get('opponent_unknown_hand_count',0)),
       float(r.get('own_library_count',0)),float(r.get('opponent_library_count',0)),
       float(len(r.get('own_battlefield',r.get('battlefield_public',[])))),
       float(len(r.get('opponent_battlefield',[]))),
       float(len(r.get('own_graveyard',r.get('graveyard_public',[])))),
       float(len(r.get('opponent_graveyard',[]))),float(len(r.get('exile_public',[]))),
       float(len(stack_items(r))),float(r.get('turn',0))]
    if dim == 0: return x  # controller-aware count ablation
    h=[0.0]*dim
    for zone in ZONES:
        items = stack_items(r) if zone == 'stack_public' else r.get(zone,[])
        for name in items: h[_bucket(zone,name,dim)]+=1.0
    # Old datasets may only have combined public zones; preserve them without
    # pretending controller identity is known.
    for zone in LEGACY:
        if zone in r:
            for name in r.get(zone,[]): h[_bucket('legacy_'+zone,name,dim)]+=1.0
    return x+h


def sigmoid(z):
    z=max(-40,min(40,z)); return 1/(1+math.exp(-z))
def dot(a,b): return sum(x*y for x,y in zip(a,b))
def fit(xs,ys,epochs=1800,lr=.002,l2=3e-4):
    w=[0.0]*len(xs[0])
    for _ in range(epochs):
        g=[0.0]*len(w)
        for x,y in zip(xs,ys):
            e=sigmoid(dot(w,x))-y
            for j,v in enumerate(x): g[j]+=e*v
        n=len(xs)
        for j in range(len(w)):
            w[j]-=lr*(g[j]/n+(0 if j==0 else l2*w[j]))
    return w
def loss(w,xs,ys):
    s=0.0
    for x,y in zip(xs,ys):
        p=min(1-1e-9,max(1e-9,sigmoid(dot(w,x))))
        s-=y*math.log(p)+(1-y)*math.log(1-p)
    return s/len(xs)


def main():
    ap=argparse.ArgumentParser(); ap.add_argument('jsonl'); ap.add_argument('--holdout',type=float,default=.25); ap.add_argument('--seed',type=int,default=7); ap.add_argument('--hash-dim',type=int,default=256)
    a=ap.parse_args(); rows=load(a.jsonl)
    groups=sorted({str(r['game_group']) for r in rows}); random.Random(a.seed).shuffle(groups)
    n=max(1,int(round(len(groups)*a.holdout))) if len(groups)>1 else 0; held=set(groups[:n])
    train=[r for r in rows if str(r['game_group']) not in held]; test=[r for r in rows if str(r['game_group']) in held]
    if not train or not test: raise SystemExit('Stage 7C BLOCKED: need at least two complete games')
    if {r['game_group'] for r in train}&{r['game_group'] for r in test}: raise SystemExit('Stage 7C BLOCKED: same-game leakage')
    ys=lambda rs:[float(r['game_result']) for r in rs]
    tx=[features(r,a.hash_dim) for r in train]; vx=[features(r,a.hash_dim) for r in test]
    w=fit(tx,ys(train))
    print(json.dumps({'schema':'stage7c-semantic-hash-v1','hash_dim':a.hash_dim,'train_rows':len(train),'holdout_rows':len(test),'train_game_groups':len(set(str(r['game_group']) for r in train)),'holdout_game_groups':len(held),'train_logloss':loss(w,tx,ys(train)),'holdout_logloss':loss(w,vx,ys(test))},sort_keys=True))
if __name__=='__main__': main()
