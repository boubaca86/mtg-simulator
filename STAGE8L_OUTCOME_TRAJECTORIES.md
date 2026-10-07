# Stage 8L — public-only outcome trajectory collection

Status: **predeclared and isolated on a branch; do not merge or execute until Stage 8K passes**.

## Objective

Stage 8K proves whether one learned complete action can safely cross the final
Forge execution boundary without changing Forge search, phase deferral, legality,
targets, costs, modes, choices, controller dispatch, or terminal lifecycle.

Stage 8L is the next data stage. It does **not** grant broader learned control and
it does **not** claim stronger play.

Its purpose is to collect training trajectories whose objective is actual game
success rather than imitation of Forge's fixed-root search score.

Every trajectory row must bind:

1. one public Forge state;
2. Forge's public legal complete-action candidate set;
3. the action actually returned to Forge;
4. controller acceptance of that exact action;
5. the exact Forge terminal lifecycle event for that action;
6. the eventual game result from the acting player's perspective.

## Critical separation: model input vs labels

The learner-facing `model_input` is created only through the frozen Stage 8D
public-input allowlist.

It may contain:

- public game state;
- own hand;
- public battlefield/graveyard/exile/stack information;
- public counts for hidden zones;
- public typed target semantics;
- complete legal candidate identities.

It may **not** contain:

- opponent hidden hand identities;
- either library's card order or identities;
- sampled hidden-world identities;
- future draws;
- Forge aggregate search scores;
- frozen-model scores;
- Forge's selected action as an input feature;
- run seed or decision index as an input feature;
- controller/terminal outcomes as input features;
- eventual game result as an input feature.

Outcome information is stored only under `labels`.
Audit/provenance fields are stored only under `audit`.

This separation is fail-closed and unit tested.

## Behavior action

`behavior_action` is the exact complete action that crossed the Stage 8E real
SpellAbility return boundary.

It must already exist in Forge's captured legal candidate set.

The row records whether the executed behavior came from:

- `forge`; or
- `learned-return-boundary`.

A learned behavior is accepted into the corpus only when the Stage 8K arm and
post-Forge-plan control events prove that:

- the requested learned action was present in the live Forge candidate set;
- the expected Forge action matched the live Forge action;
- Forge search was unchanged;
- Forge phase deferral was unchanged;
- substitution occurred only at the post-Forge-plan / pre-return boundary.

## Lifecycle requirement

Every retained behavior action must have exact identity continuity across:

**capture candidate → returned action → controller acceptance → terminal lifecycle**

Failed dispatches and lifecycle anomaly outcomes are excluded by failing the
entire source log. They are not silently dropped row-by-row.

The source log is also quarantined by the existing whole-log validator if it
contains:

- timeout handling;
- Java exceptions or errors;
- OutOfMemoryError;
- strategy-fusion divergence;
- complete-action replay mismatch;
- incomplete information-set audit coverage.

## Forge authority

Forge 2.0.15 remains the sole rules and legality referee.

Stage 8L does not modify:

- custom card definitions or card text;
- mana costs or payment;
- announced X;
- modes;
- targets;
- explicit card choices;
- triggers or replacement effects;
- priority/pass semantics;
- stack ordering;
- resolution;
- game-result rules.

## Exploration families

Reserved for Stage 8L collection only:

- `20261028`
- `20261029`
- `20261030`
- `20261031`

Each family is run in both deck orientations with one game per JVM:

- `ST Forge Full.dck` vs `Benchmark Red Forge.dck`;
- `Benchmark Red Forge.dck` vs `ST Forge Full.dck`.

For each pair:

1. run a Forge-only baseline;
2. freeze at most one Stage 8K return-boundary learned intervention using the
   unchanged frozen Stage 8D checkpoint;
3. run the controlled game;
4. collect every clean returned action from the controlled game into the Stage 8L
   trajectory corpus.

Targeted learned actions remain allowed because Stage 8K is specifically the
safety gate for that execution boundary.

## Reserved untouched evaluation families

The following families are reserved and **must not be run during Stage 8L**:

- `20261032`
- `20261033`

They are reserved for the first frozen outcome-oriented learner evaluation.

Do not inspect their results, tune on them, or use them for training before the
next learner specification and hyperparameters are frozen.

## Stage 8L acceptance gate

Stage 8L passes as a data-collection stage only if:

1. Stage 8K has already passed its full safety gate;
2. all Stage 8L and inherited Python safety tests pass;
3. patched Forge 2.0.15 builds;
4. all inherited real-Forge complete-action, typed-target, bounded-control and
   return-boundary regressions pass;
5. all 8 Forge-only baselines complete cleanly;
6. all 8 controlled games complete cleanly;
7. every retained row has complete returned/accepted/terminal identity binding;
8. every retained behavior action exists in its captured Forge legal candidate set;
9. every learned row has Stage 8K arm/control provenance;
10. model input contains zero search-score fields;
11. model input contains zero outcome/terminal label fields;
12. model input contains zero hidden-information fields;
13. at least **4 learned-return-boundary behavior rows** are present across the
    8 controlled games;
14. at least **1 learned-return-boundary behavior row is targeted**;
15. reserved evaluation families `20261032–20261033` are absent from the corpus;
16. `training_performed=false`;
17. `promotion_allowed=false`;
18. `broader_learned_control_allowed=false`.

This gate certifies dataset integrity, not playing strength.

## Next stage after a pass

Freeze an outcome-oriented policy/value learner specification **before** opening
reserved evaluation seeds.

Train only on Stage 8L exploration trajectories. The model must consume only the
`model_input` fields and use `behavior_action` / `labels.game_result` as training
targets or credit-assignment signals.

Then evaluate exactly once on the reserved `20261032–20261033` families without
retuning on their outcomes.

Only a later predeclared strength gate can justify increasing learned control
beyond one intervention per game.
