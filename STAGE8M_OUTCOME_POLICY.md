# Stage 8M — frozen public-only outcome policy

Status: **predeclared training stage; reserved evaluation seeds remain unopened**.

## Why this stage exists

Stage 8L passed its complete data-integrity gate and produced a public-only
trajectory corpus from eight controlled Forge games. Stage 8M turns that frozen
corpus into the first model whose objective is eventual game success rather than
imitation of Forge's search score.

This stage does **not** claim stronger play and does **not** expand learned
gameplay control.

Forge remains the sole rules referee and executor.

## Frozen Stage 8L source

Stage 8M may train only on the successful Stage 8L artifact from workflow run
`37637623265`:

- artifact: `stage8l-public-outcome-trajectories`
- artifact ID: `11491113213`
- artifact digest:
  `sha256:4179dbe7bd26bcb745efd9d5da800342f13638a85dd6abd97f449bf3d29a1d7c`
- games: **8**
- trajectory rows: **115**
- Forge behavior rows: **108**
- learned-return-boundary behavior rows: **7**
- targeted learned-return-boundary rows: **1**
- lifecycle anomalies: **0**
- failed dispatches: **0**

Training seed families are frozen as:

- `20261028`
- `20261029`
- `20261030`
- `20261031`

Reserved evaluation seed families remain:

- `20261032`
- `20261033`

Stage 8M must not run, inspect, tune against, or otherwise open either reserved
evaluation family.

## Learner boundary

The model receives only each Stage 8L row's validated `model_input`:

- public game state;
- own known hand;
- public battlefield, graveyard, exile and stack information;
- public counts for hidden zones;
- public card semantics already exposed legally by Forge;
- the exact legal complete-action candidates;
- public typed target semantics.

The learner must never receive as an inference feature:

- opponent hidden hand identities;
- either library's hidden card identities or order;
- sampled hidden-world identities;
- future draws;
- Forge search scores;
- Forge's selected action;
- the executed behavior action;
- game result;
- action terminal result;
- run seed;
- corpus seed;
- decision index.

`behavior_action` and `labels.game_result` are training supervision only.
Audit provenance remains audit-only.

## Frozen objective and representation

The Stage 8M model is a sparse logistic action-value estimator.

For the action actually executed in each Stage 8L row:

`Q(public state, legal action) -> probability of eventual game success`

The feature representation is the already audited Stage 8C public action and
typed-target semantic representation.

The model may score every legal candidate at inference, but it has no execution
interface.

Hyperparameters are frozen before reserved evaluation:

- objective: `game-outcome-logistic`
- epochs: **240**
- learning rate: **0.03**
- L2: **0.001**
- weighting: each source game has equal total training weight
- validation: leave one complete corpus-seed family out at a time
- selection: preserve all maximum-score ties
- no hyperparameter search
- no tuning on reserved seeds

Equal game weighting prevents games with more captured decisions from silently
dominating the objective.

## Internal validation

Internal validation is diagnostic only.

For each of the four Stage 8L exploration seed families:

1. hold out both games belonging to that seed family;
2. train on the other three seed families;
3. score only the observed behavior action's eventual-outcome probability;
4. report game-weighted log loss and Brier score;
5. compare with a constant outcome-probability baseline trained on the same
   training fold.

The model also reports recommendation coverage and agreement with the observed
behavior action, but those are not playing-strength measurements.

No internal metric can authorize promotion.

## Checkpoint contract

The frozen checkpoint records:

- Stage 8M schema and mode;
- model ID derived from canonical checkpoint content;
- exact Stage 8L dataset SHA-256;
- exact training and reserved seed families;
- fixed training configuration;
- source-code hashes;
- training row/game counts;
- deterministic sparse weights;
- `training_performed=true`;
- `promotion_allowed=false`;
- `broader_learned_control_allowed=false`.

Checkpoint loading fails closed on content, source, configuration, schema or
provenance drift.

The CI workflow exports the checkpoint twice under different Python hash seeds
and requires byte-identical outputs.

## Card and Forge semantics

Stage 8M changes no card gameplay semantics.

It does not modify:

- custom card definitions;
- mana costs or payments;
- X values;
- modes;
- targets;
- explicit choices;
- triggers or replacement effects;
- priority/pass semantics;
- stack ordering;
- resolution;
- game-result rules.

Every future evaluated action must still be a complete legal action exposed by
Forge, and Forge must remain the sole legality and execution referee.

## Stage 8M acceptance gate

Stage 8M passes only if:

1. all new and inherited public-input safety tests pass;
2. the exact successful Stage 8L artifact digest is verified;
3. the Stage 8L manifest still reports 8 games and 115 rows;
4. only exploration seeds `20261028-20261031` are present;
5. reserved seeds `20261032-20261033` are absent;
6. every source game contributes equal total training weight;
7. leave-one-seed-family-out validation has no same-game leakage;
8. checkpoint export is byte-reproducible across Python hash seeds;
9. checkpoint reload succeeds and remains immutable;
10. `promotion_allowed=false`;
11. `broader_learned_control_allowed=false`;
12. no Forge gameplay or reserved-seed evaluation is run in this stage.

## Next stage after a pass

After Stage 8M passes, freeze the resulting model ID permanently.

Only then may the project open reserved families `20261032-20261033` for one
predeclared evaluation stage. That evaluation must keep Forge as referee and must
not retune the model after seeing those results.

A later strength gate—not Stage 8M training loss—must determine whether learned
control can expand beyond the current bounded return-boundary intervention.
