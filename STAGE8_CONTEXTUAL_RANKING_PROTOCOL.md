# Stage 8B v3 — contextual ranking protocol

Status: offline development experiment; no learned gameplay control.

## Problem and fixed comparison

The v2 linear model adds public-state features to one exact-action hash. Within a
decision, the state contribution is identical for every candidate and cancels in
both ranking and perceptron updates. Its relative preferences therefore cannot
depend on the current position. Exact recipes also contain transient object IDs
and candidate indexes that should not be learned as reusable card semantics.

V3 multiplies each action feature by public context features. The complete Forge
recipe in the dataset and executor remains unchanged. Learning features omit
candidate indexes and physical object IDs, normalize player targets to self or
opponent, and preserve card names, ability text, X, modes and recorded choices.
Same-name physical targets may still alias; no exact public object mapping is
invented. Numeric mode/choice values are preserved.

These settings and representations were fixed before the first v3 corpus run:

- 20 deterministic epochs; learning rate 0.05.
- Multiclass top-choice perceptron with squared-norm-normalized updates and
  averaged weights. Any maximum-score candidate is an acceptable training target.
- Leave one complete corpus seed family out. Fit only on the other families.
- Compare uniform choice, the frozen v2 hash model, canonical action only,
  public counts, visible card identity, and public card semantics.
- All four new learned representations use the same trainer. The frozen v2
  control retains its original trainer as well as its original features.
- Public counts include life, turn, phase, unknown hand/library counts and public
  zone sizes. Identity adds visible card names. Semantics also adds public mana
  value, power/toughness/type summaries and source ability tokens/characteristics.
- No winner, selected root, outcome, seed, game ID, decision index, deck name or
  candidate score is a learner feature. Actor name is used only to resolve target
  role. Unknown opponent cards and library order remain forbidden.

The development corpus is the artifact from successful Forge run
[37351285061](https://github.com/boubaca86/mtg-simulator/actions/runs/37351285061)
at `234c545758405de3863600df4ef391a8bd0d8ce3`:

| Item | Value |
| --- | --- |
| Forge / search | 2.0.15 / fixed-root-v1, three sampled worlds |
| Corpus seeds | 20261004–20261007, both deck orientations |
| Completed games / captured proposals | 32 / 750 |
| Rankable proposals / represented games | 518 / 31 |
| Candidates / decisions with a strict score difference | 2,035 / 488 |
| Counterfactual JSONL SHA-256 | `106912cfd8800cad8abe65231abeca03578c4185205a2e11b19e87000cc8ee7e` |
| Observation JSONL SHA-256 | `9630d9d0ba24e449a17e9760b051c8adfde285c23e272ad2090fd2ee67df60b6` |

The corpus has already informed feature design. Seed isolation prevents direct
training leakage, but these are development estimates, not an untouched final
test. Fresh predeclared seed families are required before any promotion decision.

## Metrics

Evaluation uses uniform expected choice among model-score ties (relative/absolute
tolerance 1e-12), without consulting candidate labels. A choice is correct when
its Forge score equals the maximum; an equally good alternative is not an error.
Pairwise accuracy considers only strict Forge-score pairs and gives predicted
ties half credit. Decisions where every label is equal are reported separately.

Normalized regret is `(best - chosen) / (best - worst)` per decision, then averaged.
It is zero if all labels tie and otherwise lies between zero and one. Raw regret
is retained but can be dominated by Forge's integer terminal-score sentinels.
Report decision-weighted, per-seed and per-game metrics, plus a game-macro summary.

The existing Forge-selected candidate is reported as a top-choice consistency
reference. Since the same search supplies the labels, its agreement is circular
and is not evidence that a learned policy is stronger than Forge.

Coverage uses **captured selected search proposals** as its denominator. These
include phase probes that may be deferred and omit passing and other priority
windows. They are not complete executed-action trajectories. One of the 32 games
has no rankable proposal, which must remain visible in the coverage report.

## Acceptance and reproduction

The implementation gate is correctness: context-sensitive synthetic choices,
label/provenance exclusion, seed isolation, deterministic fitting, tie handling,
coverage matching, and rejection of hidden or partial-world data. CI success
does not imply a performance improvement or authorize live model selection.
`promotion_allowed` remains `false`, including if all learned baselines lose.

Run against the unmodified downloaded artifact:

```sh
python -m unittest test_stage8_action_ranker
python stage8_action_ranker.py stage8a-counterfactual.jsonl \
  --observations stage7b-labeled.jsonl --output stage8b-ranking-v3.json
```

The report records the input hashes and fixed training configuration. A later
fresh-corpus or live-shadow experiment must predeclare its seed families and
evaluation criteria before inspecting outcomes. These labels imitate the current
Forge search; independent continuation quality and playing strength remain open.
