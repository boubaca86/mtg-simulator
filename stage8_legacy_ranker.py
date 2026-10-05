#!/usr/bin/env python3
"""Frozen Stage 8B v2 baseline for paired comparisons.

Learns only from Stage 8A learner-facing rows. Forge remains the rules referee;
this model never controls gameplay. Evaluation is leave-one-corpus-seed-out so
every reported decision is scored by a model that never saw its seed family.
"""
from __future__ import annotations
import hashlib


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
    # Complete action identity is legal/public at the decision boundary. Keep the
    # exact recipe as the baseline feature; later stages may add separately audited
    # public action semantics, but must never weaken the exact replay identity.
    add('action='+candidate['action_identity'])
    return f


def dot(w,f): return sum(w.get(i,0.0)*v for i,v in f.items())
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


