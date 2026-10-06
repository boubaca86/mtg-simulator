# Stage 8J — single learned-action intervention protocol

Status: **predeclared; fresh-seed execution not yet started**.

Stage 8I proved that an external policy-control transport can request only an
already-captured complete Forge-legal action, fail closed on invalid identities,
and reproduce Forge's original choices without gameplay drift.

Stage 8J is the first deliberately different learned action allowed into a real
Forge game. The scope is intentionally narrow: **at most one learned action per
game**. This is a safety/integration experiment, not a strength promotion.

## Frozen policy

The policy is the exact frozen Stage 8D public-only checkpoint:

- model id: \`6c4dd27f364aaa6c9e9100f88eb323eaa419dab0a49845ee0f748b9998746288\`;
- development data only from seed families \`20261004–20261007\`;
- no Stage 8C confirmation rows, Stage 8I rows or Stage 8J outcomes may retrain
  the model before this experiment;
- inference receives only the existing public allowlist and typed public target
  semantics;
- search scores, terminal labels, future draws and hidden opponent identities
  are forbidden inputs.

## Fresh seed families

Predeclared untouched families:

- \`20261016\`
- \`20261017\`
- \`20261018\`
- \`20261019\`

For each family, run both deck orientations:

- \`ST Forge Full.dck\` first vs \`Benchmark Red Forge.dck\`;
- \`Benchmark Red Forge.dck\` first vs \`ST Forge Full.dck\`.

Each orientation/seed is run as its own JVM with exactly **one game** so that
decision indices and the single-intervention budget cannot cross game boundaries.

Total paired experiments: **8 baseline games + 8 controlled games**.

Once the first baseline is run, these seeds are observed and may never again be
described as untouched evidence for a changed policy or changed Stage 8J gate.

## Controlled side and intervention recipe

Only the Forge player whose public name begins with \`Ai(1)-\` is eligible for
Stage 8J control. The opposite side remains completely Forge-controlled.

From the baseline game, inspect returned-action captures in chronological order.
Choose the **first** decision satisfying all of these conditions:

1. the capture reached the real \`SpellAbilityPicker\` returned-action boundary;
2. the acting player is \`Ai(1)-\`;
3. the frozen model has one unique preferred action;
4. that preferred action differs from Forge's baseline selected action;
5. that preferred action is already in the exact complete candidate set captured
   by Forge;
6. every candidate replayed successfully in all three information-set worlds.

The external request file contains only:

- \`decision_index\`;
- \`complete_action_identity\`.

No search score, outcome, seed label, hidden card identity or sampled hidden-world
identity is included.

If a game has no eligible intervention, it receives no control request and runs
Forge-only in the controlled pass.

## Live fail-closed boundary

At the requested decision, the controlled run must reproduce the baseline public
state and complete candidate set before the request may be applied.

The adapter may select only an exact \`RootActionTable.ScoredAction\` present in
that live Forge candidate set. It cannot construct or mutate an action.

The adapter is configured with:

- \`require_forge_match=false\` — the one predeclared learned action may differ;
- \`allow_missing=true\` — every non-requested decision falls through to Forge;
- \`actor_prefix=Ai(1)-\` — a request reaching another player is an error.

Pass/defer decisions remain Forge-controlled. The model cannot directly request a
pass in Stage 8J.

## Forge authority and semantic lock

Forge 2.0.15 remains the sole legality and rules referee.

Stage 8J must not change:

- custom-card text;
- mana costs or payment;
- announced X;
- modes;
- targets;
- explicit card choices;
- priority/pass semantics;
- stack ordering;
- replacement effects;
- triggers;
- resolution;
- game-result rules.

The selected learned action proceeds through the same Stage 8E returned-action,
Stage 8F controller-acceptance and Stage 8H terminal-lifecycle chain as every
Forge-selected action.

## Predeclared safety gate

A Stage 8J safety pass requires all of the following:

1. all Stage 8C–8J Python regressions pass;
2. patched Forge 2.0.15 builds;
3. Stage 6 complete-action, Stage 8C typed-target and Stage 8I fail-closed Java
   regressions pass;
4. all 16 games complete without timeout, exception, strategy-fusion failure or
   replay failure;
5. at least **4 of the 8** paired games contain an eligible learned intervention;
6. every planned intervention is applied exactly once;
7. zero intervention requests are accepted outside the live captured legal set;
8. zero control events occur for the wrong actor;
9. the controlled run is identical to its baseline at every captured decision
   before the intervention;
10. the intervention-time public state and complete candidate descriptors match
    the baseline exactly;
11. every applied learned action reaches returned-action, controller-acceptance
    and one clean terminal lifecycle outcome;
12. zero dispatch failures, lifecycle anomalies or pending tracked actions occur;
13. Forge remains the rules referee;
14. \`promotion_allowed=false\` and
    \`broader_learned_control_allowed=false\`.

## Outcome measurement

For the controlled \`Ai(1)-\` side, record paired baseline and controlled game
scores:

- win = 1;
- draw = 0.5;
- loss = 0.

The paired score delta is **descriptive only**. It is not a Stage 8J pass/fail
criterion and must not be used to move the gate after the result is known.

Reason: the frozen Stage 8D model was trained to approximate Forge's fixed-root
counterfactual scores, not to maximize independently observed match win rate.
Stage 8J establishes whether a genuinely different model-requested legal action
can traverse the full live Forge lifecycle safely.

## Next stage if the safety gate passes

Do **not** immediately enable unrestricted learned control.

The next justified direction is outcome-bearing exploration: use the bounded,
audited intervention mechanism to collect public-state/action trajectories with
actual match outcomes, then train and evaluate a value/policy signal whose target
is game success rather than imitation of Forge's fixed-root score. Training and
evaluation must remain seed-family isolated and hidden-information safe.
