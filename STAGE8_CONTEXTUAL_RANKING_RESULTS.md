# Stage 8B v3 — contextual action-ranking results

Status: development comparison completed; **promotion_allowed=false**.

The new public-count model agrees with a maximum-scoring Forge candidate on
83.30% of held-out decisions, compared with 52.58% for the frozen original ranker
under the same corrected evaluation. Most of this gain is already present in the
canonical action-only control (81.85%). This supports fixing transient action
identities and the trainer; it does not attribute the entire gain to board context.

## Evidence and method

Source: successful 32-game Forge workflow
[37351285061](https://github.com/boubaca86/mtg-simulator/actions/runs/37351285061),
commit `234c545758405de3863600df4ef391a8bd0d8ce3`.
The fixed protocol and input hashes are in `STAGE8_CONTEXTUAL_RANKING_PROTOCOL.md`.
Machine-readable metrics are in
`results/forge_expert_ai_stage8/contextual-ranking-v3.json`.

Each decision is tested exactly once, with its entire seed family excluded from
training. All six models use the same 518 decisions, 2,035 candidates, 4,134 strict
candidate pairs, four seed families and 31 represented games. Of the 518
decisions, 488 have at least one strict label difference and 30 have all labels
tied. Candidate coverage is **518/750 captured proposals (69.07%)** across 32 source
games; one game has no rankable proposal. It is not coverage of all real moves.

## Paired comparison

Top-choice accuracy accepts any maximum-score candidate. Model ties use uniform
expected choice. Lower regret is better. Game-macro metrics weight each represented
game equally; decision-weighted metrics give longer trajectories more weight.

| Model | Top-choice accuracy | Pairwise accuracy | Normalized regret | Game-macro accuracy | Game-macro regret |
| --- | ---: | ---: | ---: | ---: | ---: |
| Uniform | 37.77% | 50.00% | 0.45993 | 39.57% | 0.44885 |
| Frozen v2 action hash | 52.58% | 57.57% | 0.31971 | 53.25% | 0.34427 |
| Canonical action only | 81.85% | **75.27%** | 0.10093 | 82.74% | 0.11431 |
| Public counts × action | **83.30%** | 73.34% | **0.08481** | 84.62% | 0.09515 |
| Visible identity × action | 79.25% | 70.44% | 0.10691 | 81.47% | 0.11673 |
| Public semantics × action | 81.08% | 70.91% | 0.09218 | **85.26%** | **0.08704** |

Compared with action only, public counts improves decision accuracy by 1.45
percentage points and reduces normalized regret by about 16%. Its pairwise
accuracy decreases. Public semantics improves game-macro accuracy/regret but does
not dominate public counts or action only. Therefore no single representation is
an across-metric winner, and the richer representation is not promoted.

Normalized regret by held-out seed family:

| Held-out seed | Decisions | Action only | Public counts | Public semantics |
| --- | ---: | ---: | ---: | ---: |
| 20261004 | 104 | 0.13108 | 0.12788 | 0.09893 |
| 20261005 | 99 | 0.07450 | 0.07415 | 0.06369 |
| 20261006 | 152 | 0.07449 | 0.07461 | 0.08632 |
| 20261007 | 163 | 0.12240 | 0.07332 | 0.11064 |

Most of the count model's aggregate regret gain comes from seed 20261007. Four
seed families are too few to present this as a robust general playing-strength
improvement. The original model's published 48.46% top-choice figure used different
tie rules; 52.58% above reevaluates its unchanged predictions with the shared v3
metrics. These numbers must not be compared as if the old training had improved.

Thirty-two decisions contain terminal-scale scores. Raw mean regret is retained
in the JSON report but should not be interpreted as a stable card-value scale.
Forge's selected proposal has 100% top-choice agreement and zero regret by
construction: the search itself produced the candidate labels.

## Verification and next gate

Seventeen new behavioral regressions pass, including learning opposite preferences
from different public positions, ignoring outcomes/chosen-action/provenance fields,
removing transient IDs, handling label/model ties, deterministic fitting across
Python hash seeds, seed
separation, bounded regret, proposal coverage and hidden/partial-world rejection.
Nineteen existing Stage 7 unit tests and all three Stage 8 contract/serializer/parser
scripts also pass locally. Both Stage 8 contract CI and the full Forge workflow
now exercise the revised ranker and report proposal coverage.
The complete 518-decision report reproduced byte for byte in a second process
with an explicitly different Python hash seed.

CI evidence for implementation commit `71e6ab6b403cd889c60bb5743a31931e25366ff5`:

| Check | Run | Result |
| --- | --- | --- |
| Stage 8 contract, serializer, parser and 17 ranker regressions | [37357437020](https://github.com/boubaca86/mtg-simulator/actions/runs/37357437020) | Pass |
| Stage 7 legal-information contract | [37357437096](https://github.com/boubaca86/mtg-simulator/actions/runs/37357437096) | 5 adversarial/deterministic tests passed |
| Full Forge build, real action-identity regression, capture and model comparison | [37357436865](https://github.com/boubaca86/mtg-simulator/actions/runs/37357436865) | Pass; 32 complete games, 750 proposals, 518 rankable decisions |

The independent full rerun produced byte-identical observation JSONL,
counterfactual candidate JSONL, and the complete v3 ranking report. Report SHA-256:
`76c70d585f29c9b82da0e8d7ad85f53c17fd1864560fd3cc774b7f2113ec66b7`.
Artifact `11365658640` has ZIP SHA-256
`e4f6845da82e763ad98ff0f7b1110783a44c31f6473774e914f5aa2a18da560e`.
This repeats the same development seed families; it is reproducibility evidence,
not the fresh-seed test.

This corpus has already informed feature design. No hyperparameter or feature
sweep followed these results, and no learned model controls Forge gameplay.
The fresh-seed comparison specified in `STAGE8_FRESH_SEED_PROTOCOL.md` is complete;
it did not meet the predeclared positive-replication threshold. See
`STAGE8_FRESH_SEED_RESULTS.md` for the complete result and timeout deviation.
Stronger execution/target-state capture and independent
continuation or shadow evaluation remain later work. The current results measure
imitation of Forge's fixed-root scores,
not wins against an opponent or master-level play.
