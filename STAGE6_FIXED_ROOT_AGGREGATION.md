# Fixed root-action aggregation — 2026-10-05

Status: implemented; real-Forge regression and live recapture pending.

The 32-game replication run [37286004175](https://github.com/boubaca86/mtg-simulator/actions/runs/37286004175)
failed on `st-first`, seed `20261005`. Its log records two divergences involving
Whirlwind S.T's targeted damage ability, at turn 11. The first two seed/orientation
groups completed, but the full corpus was correctly rejected before model fitting.
Do not reuse the partial artifact as the replication result.

## Search change

This supersedes the concurrent emergency rejection in `de88239`: full-world
coverage remains mandatory, and disagreements are resolved by comparing fixed
recipes instead of rejecting the entire ability.

Stage 5 averaged each sampled world's independently optimized ability score and
then optimized that ability again in one representative world to create a plan.
This could mix different targets in one average and execute a different choice.

The new Stage 6 patch records every successful root recipe enumerated by Forge's
existing choice iterator, immediately before its linked choice nodes are removed.
It includes results below the original position score. Root target-effect caching
is bypassed during collection because a cached score does not retain the recipe.

`RootActionTable` groups scores by the existing `recipe=v2` identity: candidate
source index, ability, X, modes, targets, and recorded card choices. A recipe is
eligible only if it has a successful evaluation in **all three worlds**. Its score
is the arithmetic mean of those three evaluations. Missing/failed evaluations
exclude that recipe; repeated evaluations in one world never add sample coverage.
The downside-risk coefficient is explicitly **zero** in this version; no risk
aversion or playing-strength claim is implied.

The winning detached root snapshot is installed directly with its averaged score.
There is no representative-world re-optimization and no sampled future sequence
in the executable plan. Target replay checks both legality and the concrete target
description (which includes Forge card IDs); mismatches quarantine the whole log.

`EXPERT_INFOSET_ACTIONS_RECONCILED` describes differences among world preferences
before aggregation. It is expected and does not invalidate a corpus. The
strategy-fusion invariant and replay-mismatch diagnostics remain fatal to dataset
admission. Fresh rows carry `search_policy=fixed-root-v1`; the paired comparison
rejects older policies instead of silently mixing corpora.

## Validation

- Apply the complete patch chain to pristine pinned Forge `2.0.15` source.
- Run the expanded Java regression against the actual built Forge jar. It checks
  adversarial world preferences, same-name/different-ID targets, below-baseline
  worlds, failed/missing worlds, choice-node timing, child X, exact plan installation,
  and mean-score preservation.
- Repeat the previously failing seed `20261005` for the live action gate and twice
  for byte-identical dataset capture.
- Recapture all 32 games on the predeclared seed families `20261004`–`20261007`,
  in both deck orientations, without changing split, model settings, or card rules.
- Enforce the documented paired performance inequalities: semantics must beat
  perspective counts and be no worse than visible identity, with eight held-out games.

## Remaining limits

This enforces consistency for the root recipes represented by Forge's current
iterator and `Plan`. It does not prove information-set consistency of deeper
rollout policies, completeness of Forge's target pruning, or coverage of every
Magic choice (cost-payment/sacrifice ordering, divided allocations, and duplicate
same-name explicit card choices need further representation work). Choices after
new hidden information is revealed also need a contingent-policy treatment.
Root recipe availability is conservatively intersected across sampled worlds;
this is not a general proof of legal-information-only action enumeration.

Stage 6 remains experimental beyond this boundary. The learned evaluator remains
offline with `promotion_allowed=false`; a clean root regression or better outcome
prediction does not establish master-level play.
