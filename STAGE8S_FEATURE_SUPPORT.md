# Stage 8S — read-only feature support and uncertainty diagnostic

**Purpose:** Determine whether the failed Stage 8R target/effect extension is sparse across *independent development seed families* and how uncertain the observed difference is. This is a diagnostic explanation of an already-observed loss, **not** a new model, model selection, or a claim of improved gameplay.

## Mandatory quarantine and provenance

- Stage 8P reserved families **20261032 and 20261033 are permanently consumed**. Do not run or train on them again.
- Load only the pinned Stage 8L (run 37637623265; artifact 11491113213; SHA256 archive `4179dbe7bd26bcb745efd9d5da800342f13638a85dd6abd97f449bf3d29a1d7c`) and Stage 8N (run 37646582768; artifact 11495733848; SHA256 archive `36037df0152a57fc6117f5cf91dcaa4cb3fd26288e4b07d6ce2028cffe93062e`) **prior development** corpora.
- Existing Stage 8R corpus loader enforces 24 games, 331 *observed-action* rows, exactly 12 full seed families (two game orientations per family), public inputs only, action identity and legality, no reserved or proposed-future seeds.
- Proposed families 20261042–20261049 (future development) and 20261050–20261057 (future held-out) remain **unverified candidates, not authorized for gameplay**.
- Frozen Stage 8O checkpoint, Stage 8P protocol and Forge are unchanged. Neither this code nor the workflow can dispatch gameplay.

## Method frozen before this diagnostic runs

1. For every Stage 8Q `q_` target/effect feature, report activated **observed actions**, distinct games and distinct seed families. Count zero-support features and features active in fewer than 3 of the 12 development families. These are *descriptive support* measures, not empirical action-effect estimates.
2. Explicitly include `q_gain_life_to_self_player` and `q_pump_to_self_creature` alongside self-damage features. Legal beneficial self-target choices must never be hardcoded away. No attempted action or counterfactual is labeled from unexecuted gameplay.
3. Recompute Stage 8R and the frozen Stage 8O method using identical leave-one-*entire-family*-out folds, same pinned 24 games, and both orientations. Calculate per-family **8R − 8O** differences in game-normalized log loss and Brier score.
4. Bootstrap **entire paired families** 10,000 times (seed **81502026**) with replacement. Return percentile 2.5%–97.5% intervals and signs, with the smaller loss always better. Do not resample the 331 correlated action rows as independent games.
5. Check family-mean differences against Stage 8R aggregate differences to tolerance `1e-10`. Fail closed on missing/duplicated families, missing game orientation, nonfinite losses or seed contamination.
6. Perform byte-identical repeat runs under distinct Python hash seeds, archive source/corpus hashes and all results. CI success means diagnostic integrity, **not** a passing strength gate.

The confidence intervals are **exploratory retrospective descriptions on only 12 clusters**. They are not a precommitted prospective test, and must not be used to retroactively rescue the Stage 8R result or select new hyperparameters on held-out data.

## Decisions permitted

- If new features have thin family coverage, document it as a possible explanation; do **not** assume this establishes the cause.
- If the family interval crosses zero, say that the sign is uncertain. A noncrossing interval on these previously inspected development families still does **not** authorize controller promotion.
- Stage 8R's predeclared development gate already **FAILED**: log loss 8R 0.7220619231746436 versus 8O 0.7215996535719428; Brier 8R 0.26424775159871444 versus 8O 0.2640286471704821.
- A future gameplay study requires a separately reviewed and frozen Forge/deck/model/seed/paired-outcome protocol and robust one-shot seed guard. No current stage approves it.

**Review gate:** This Stage 8S branch is based on pending Stage 8R PR #15. Review and passing CI are prerequisites; do not merge into main or dispatch games without independent review.
