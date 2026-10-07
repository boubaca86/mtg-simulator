# Stage 8O — combined conservative outcome development results

Status: **Stage 8O development and reproducibility gates passed**. No gameplay
promotion or human-level-strength claim is authorized.

## Verified run

- Workflow: `Forge Expert AI Stage 8O — Combined Outcome Training`
- GitHub Actions run: `37693639263`
- Commit: `464522b19a7757b3b121bc5ffe006ff6f24ead02`
- Conclusion: success
- Artifact: `stage8o-conservative-outcome-dev`
- Artifact ID: `11514257724`
- Artifact ZIP digest: `sha256:69b9364b458a75087cf3c6fe76ca7c0ffb8e2590b6dca2851c02c0ce924cdc66`
- Frozen model ID: `90bbbd616e2509c3c85c318e5aded17cfe6e7592ad625bad2a1467136a596ba3`

The source artifact digests for Stage 8L and Stage 8N were checked before
training. The checkpoint and report were generated twice with differing
Python hash seeds; byte-for-byte comparisons passed. Checkpoint integrity,
source-hash, and fixed-configuration checks passed. The new and inherited
public-only safety regressions passed.

## Observational training data

- Stage 8L: 8 full games, 115 executed-action trajectories
- Stage 8N: 16 full games, 216 executed-action trajectories
- Combined: **24 full games, 331 executed-action trajectories**
- Twelve development seed families: 20261028–20261031 and 20261034–20261041
- Reserved evaluation families **20261032–20261033 have not been opened**
- All behavior actions were selected from Forge's captured legal candidates.
- The model trained on observed action outcomes only, without fabricating
  counterfactual outcomes for unexecuted actions.

## Predeclared grouped-development diagnostic

Leave-one-entire-seed-family-out, game-weighted result:

| Metric (lower is better) | Stage 8O model | Constant baseline |
|---|---:|---:|
| Log loss | 0.7215996536 | 0.7290127806 |
| Brier score | 0.2640286472 | 0.2677341598 |

The model improved both metrics by the predeclared margin. This is a **modest
development improvement**, not evidence that selecting the model's preferred
actions increases win rate. The outcomes are observational and can be confounded
by the rest of each game. Neither the size nor the diversity of this corpus
supports a strong human-level-play claim.

## Decision and next legitimate boundary

Stage 8O's development-only quality gate passed. This permits **predeclaring**
a one-shot evaluation stage on the reserved 20261032 and 20261033 seed families,
with complete games, both deck orientations, exact Forge referee and
return/acceptance/terminal identity audits, and a precommitted primary
game-result criterion.

The reserved families must remain untouched until the evaluation method and
pass/fail criteria are committed. The frozen Stage 8O model must not be
retuned after evaluation. Any score improvement without a safe end-to-end Forge
gameplay evaluation is **not** authority to increase learned gameplay control.

No card definitions, mana costs, targeting, mode choices, payment semantics,
triggers, stack/priority rules, resolution semantics or gameplay code were
modified during Stage 8O.

All Stage 8O checkpoints retain `promotion_allowed=false` and
`broader_learned_control_allowed=false`.
