# Stage 8N — development-only public action exploration corpus

Status: **predeclared development-data stage; reserved evaluation seeds remain untouched**.

## Why Stage 8N is necessary

Stage 8M successfully produced a deterministic, content-addressed outcome model,
but its internal leave-one-seed-family-out outcome diagnostic was worse than a
constant predictor:

- model log loss: **1.3698972166**
- constant-baseline log loss: **0.8958797346**
- model Brier score: **0.4808954736**
- constant-baseline Brier score: **0.3472222222**

The correct response is not to spend the reserved evaluation families on a model
that is already weak on development diagnostics.

Stage 8N therefore expands the development corpus while preserving the untouched
reserved families `20261032-20261033`.

## Development-only seed families

Stage 8N uses exactly:

- `20261034`
- `20261035`
- `20261036`
- `20261037`
- `20261038`
- `20261039`
- `20261040`
- `20261041`

Each family runs both deck orientations:

1. `ST Forge Full.dck` vs `Benchmark Red Forge.dck`
2. `Benchmark Red Forge.dck` vs `ST Forge Full.dck`

This yields **16 controlled development games** plus matching Forge-only
baselines.

Reserved families:

- `20261032`
- `20261033`

remain forbidden in Stage 8N.

## Exploration policy

Stage 8N deliberately does **not** use the Stage 8M model to choose exploration
actions.

The exploration rule is frozen as:

`first-returned-public-alternative-hash-v1`

For each baseline game:

1. Forge runs normally.
2. Stage 8N observes only validated Stage 8 capture/return events.
3. It waits until an action reaches the real Stage 8E return boundary.
4. It considers only alternatives that:
   - are already present in Forge's captured legal complete-action set;
   - differ from Forge's selected action;
   - have `replay_valid_count == information_set_samples`.
5. At most one action is selected for exploration.
6. Even corpus seeds prefer a targeted alternative when one exists.
7. Odd corpus seeds prefer an untargeted alternative when one exists.
8. Inside the eligible pool, selection is deterministic by SHA-256 of:
   - the fixed exploration rule;
   - corpus seed;
   - decision index;
   - complete action identity.
9. If the preferred targeted/untargeted class does not exist, Stage 8N falls
   back to any replay-valid public alternative.
10. If no legal alternative exists, no intervention is made.

The action choice is therefore:

- not model-guided;
- not outcome-guided;
- not future-state-guided;
- not hidden-information-guided;
- reproducible.

## Critical temporal boundary

The planner scans events chronologically and makes its choice only when the
corresponding real returned-action event is reached.

It does not select an earlier action by looking ahead at later game states or the
game result.

The replacement request is still applied only at the proven Stage 8K boundary:

**post-Forge-plan / pre-return**

Forge search and phase deferral must remain unchanged.

## Forge authority

Forge 2.0.15 remains the sole rules and legality referee.

Stage 8N does not alter:

- custom card text;
- costs;
- X values;
- modes;
- targets;
- explicit choices;
- triggers;
- replacement effects;
- priority or passing;
- stack ordering;
- resolution;
- game-result rules.

The exploration action must already be one of Forge's captured legal complete
actions.

## Why model-independent exploration

Stage 8L contained only seven behavior actions that differed from Forge across
eight games.

That is too little action diversity for a reliable action-conditioned outcome
model.

Using the weak Stage 8M model to collect the next corpus would risk reinforcing
its own errors. Stage 8N instead broadens the behavior distribution with a
predeclared public-only exploration rule.

## Required intervention diversity

Across the 16 games, the Stage 8N safety/data gate requires at least:

- **12 total applied interventions**
- **2 targeted interventions**
- **6 untargeted interventions**

These are data-coverage requirements, not playing-strength requirements.

If the corpus cannot achieve these counts without weakening legality or audit
requirements, the stage fails.

## Trajectory labeling

Stage 8N reuses the strict Stage 8L lifecycle collector.

Any returned action that differs from Forge is explicitly labeled:

`exploration-return-boundary`

It is **not** called a learned action.

Every retained row still requires exact identity continuity across:

**captured candidate → returned action → controller acceptance → terminal lifecycle**

The learner-facing `model_input` remains the same public-only allowlist.

Search scores, outcomes, seeds, Forge's proposal, hidden identities and audit
metadata remain outside model input.

## Stage 8N acceptance gate

Stage 8N passes only if:

1. all Stage 8N and inherited Python safety tests pass;
2. pinned Forge 2.0.15 builds;
3. inherited real-Forge complete-action, target and control regressions pass;
4. all 16 Forge-only baselines complete cleanly;
5. all 16 controlled games complete cleanly;
6. exploration plans are recomputed from the fixed public rule rather than
   trusted from files;
7. request bytes exactly match those recomputed plans;
8. Forge captures are unchanged before the substitution boundary;
9. Forge search is unchanged;
10. Forge phase deferral is unchanged;
11. controller dispatch has zero failures;
12. terminal lifecycle coverage is complete;
13. lifecycle anomalies are zero;
14. no invalid request is accepted;
15. at least 12 interventions are applied;
16. at least 2 applied interventions are targeted;
17. at least 6 applied interventions are untargeted;
18. development families are exactly `20261034-20261041`;
19. reserved families `20261032-20261033` are absent;
20. model input contains zero hidden-information fields;
21. exploration is recorded as model-guided=false and outcome-guided=false;
22. `training_performed=false`;
23. `promotion_allowed=false`;
24. `broader_learned_control_allowed=false`;
25. `strength_claim_allowed=false`.

## Next stage after a pass

Combine the successful Stage 8L and Stage 8N development corpora.

Before touching reserved evaluation families, predeclare a new outcome learner
that explicitly addresses the Stage 8M failure—for example with stronger
regularization, lower-capacity action features, and grouped validation across
complete game/seed families.

Only a learner that improves materially over simple development baselines should
be considered for the one-shot reserved evaluation.

Even then, Forge remains the rules referee and broader learned control remains a
separate later gate.
