# Stage 7D — Public Card Semantics

Status: **first paired research gate passed; live promotion remains disabled**.

## Result

The repaired Stage 7B live-labeled workflow run `37269454156` completed successfully on 2026-10-05 using pinned Forge `2.0.15`.

Corpus:

- 16 complete Forge games
- 338 legal decision states
- 12 training games / 248 states
- 4 seed-isolated holdout games / 90 states
- three information-set worlds per decision
- complete-action audit passed

Paired holdout log-loss on the exact same split:

| Representation | Holdout log-loss |
| --- | ---: |
| Combined counts | 0.850289 |
| Perspective-aware counts | 0.594673 |
| Stage 7C visible card identity | 0.441409 |
| **Stage 7D public card semantics** | **0.399694** |

Stage 7D improves holdout log-loss by `0.041715` versus Stage 7C identity hashing, approximately **9.45% relative**.

This is evidence that Forge-derived public characteristics (zone/controller context, mana value, type, and visible/current creature power/toughness) contain useful signal beyond card-name identity alone. It is **not** evidence that the evaluator is ready to choose live actions.

## Integrity constraints

The result is only valid while all of the following remain true:

1. Forge remains the rules referee and source of legal actions/gameplay semantics.
2. Opponent hand identities and both library identities/order remain unavailable to the learner.
3. Face-down objects remain opaque.
4. Train/holdout separation is by seed family / complete game, never by individual decision row.
5. All compared representations use the exact same held-out games.
6. No custom-card rules are modified to improve evaluator performance.
7. Learned evaluation remains offline until an explicit promotion gate is satisfied.

## Next gate: Stage 7D-R replication

The current holdout is only four games, so sampling noise is still a serious threat. The next justified step is **replication, not live promotion**.

Run the existing live-labeled Stage 7B workflow with a larger corpus, preserving the same pinned Forge revision, extractor schema, anti-cheating checks, deterministic seed-family split, and paired evaluator comparison. Target at least **32 complete games** and at least **8 seed-isolated holdout games** before considering the Stage 7D representation replicated.

Replication passes only if:

- every rules/information-security/complete-action gate passes;
- Stage 7D public semantics beats perspective-aware counts on holdout;
- Stage 7D public semantics is no worse than Stage 7C identity on holdout;
- results are reported on identical held-out games for every representation;
- no training or tuning decision uses holdout labels.

Even after replication, `promotion_allowed` must remain false. The following stage should test **counterfactual action ranking**: evaluate Forge-legal candidate actions from the same information set and verify ranking quality on held-out decisions before the learned model can influence live move selection.

## Why this order

Outcome prediction and action selection are different problems. A model can predict winners well while still choosing bad moves. Replication establishes that Stage 7D's representation improvement is real; counterfactual action-ranking tests whether that representation can support stronger play without bypassing Forge or leaking hidden information.
