# Stage 8M — frozen outcome-policy results

Status: **training/reproducibility gate passed; model retired from reserved evaluation because the internal development diagnostic was worse than baseline**.

## Successful Stage 8M run

Workflow:

- `Forge Expert AI Stage 8M — Frozen Outcome Policy`
- run: `37645183725`
- commit: `e5c3f39964c6d67aa36c54f41197effac8c53090`

Artifact:

- name: `stage8m-frozen-outcome-policy`
- artifact ID: `11494167437`
- artifact digest:
  `sha256:17d0d2456404ccac61744828beaa869478d180936d091d4d00ee978f8f6e19c1`

Frozen model ID:

`4195f55ec4c06c4e7e8ea33c7d97cabce708ff7bfd4706b41d54b27a695c804d`

The checkpoint was exported twice under different Python hash seeds and matched
byte for byte.

## Training corpus

The model used only the successful Stage 8L public-only outcome corpus:

- 8 source games
- 115 trajectory rows
- exploration seed families `20261028-20261031`
- reserved seed families `20261032-20261033` absent
- 108 Forge behavior rows
- 7 return-boundary learned behavior rows
- no hidden-information model fields
- no search-score model fields
- no lifecycle anomalies
- Forge remained the rules referee

## Internal development diagnostic

Leave-one-complete-seed-family-out validation produced:

| Metric | Stage 8M model | Constant baseline |
|---|---:|---:|
| Game-weighted log loss | 1.3698972166 | 0.8958797346 |
| Game-weighted Brier score | 0.4808954736 | 0.3472222222 |
| Unique recommendation rate | 1.0000 | 0.0000 |
| Behavior agreement rate | 0.6466834700 | diagnostic only |

Lower log loss and Brier score are better. The Stage 8M model was materially
worse than the constant baseline on both outcome-prediction metrics.

This is not a rules/safety failure. It is a **development-quality failure**.

## Decision

Do **not** spend the reserved `20261032-20261033` evaluation families on this
model.

Those families remain untouched.

The model ID above remains frozen as the exact Stage 8M result; it will not be
silently retrained or redefined.

The likely limitation is data quantity and action diversity: only eight games
were available and only seven behavior rows differed from Forge. An
action-conditioned outcome estimator cannot learn reliable comparative action
value from such a narrow behavior distribution.

## Next justified stage

Stage 8N expands the development corpus without touching reserved evaluation
families.

It will use new development-only families `20261034-20261041`, both deck
orientations, and at most one deterministic public-only alternative action per
game at the already-proven Stage 8K return boundary.

The exploration choice is model-independent and uses only Forge's captured legal
candidate set and public audit metadata. Forge still decides legality, costs,
targets, modes, choices, priority, stack behavior, resolution and game result.

The goal is to obtain substantially more outcome-bearing action diversity before
training another outcome model.

No Stage 8M metric authorizes broader learned control or a playing-strength
claim.
