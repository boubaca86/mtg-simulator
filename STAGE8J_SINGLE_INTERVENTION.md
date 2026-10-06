# Stage 8J — target-free single learned-action intervention protocol

Status: **target-free rerun passed; original failure retained and diagnosis corrected**.

## Original Stage 8J failure and corrected diagnosis

The original fresh-seed run (GitHub Actions run `37473357477`) completed the
Forge build, regressions, all baseline games, and all controlled games. It failed
at the final lifecycle audit.

At capture decision 7, the learned policy requested **Lightning Strike** targeting
**Servitor** instead of Forge's **Lightning Elemental**. The requested action was
present in Forge's exact captured legal candidate set.

Deeper log inspection showed that the missing lifecycle was **not proof of a
target-rebinding failure**. Stage 8J substituted the learned candidate too early,
while Forge was still comparing the current-phase plan against its existing
after-blockers phase-bloom plan. That changed the score entering Forge's
act-now-versus-wait decision, and Forge emitted a priority pass instead of
returning the capture-7 action.

The same **Lightning Strike → Servitor** complete action later crossed Forge's
real returned-action boundary at `COMBAT_DECLARE_BLOCKERS`. Therefore the first
proven integration defect was the **control boundary**: learned substitution
occurred before Forge had finished deciding whether to act now or defer.

The original failure remains a real failed safety result. The audit is not
weakened, and the observed seed families `20261016–20261019` are not reused as
fresh evidence.

The amended Stage 8J question was deliberately narrower: can the frozen learned
policy safely replace one Forge choice when the learned replacement has **no
target at all**?

## Prior stage

Stage 8I proved that an external policy-control transport can request only an
already-captured complete Forge-legal action, fail closed on invalid identities,
and reproduce Forge's original choices without gameplay drift.

Stage 8J remains intentionally narrow: **at most one learned action per game**,
and the learned replacement must have `targets=<none>`. This is a
safety/integration experiment, not a strength promotion.

## Frozen policy

The policy is the exact frozen Stage 8D public-only checkpoint:

- model id: `6c4dd27f364aaa6c9e9100f88eb323eaa419dab0a49845ee0f748b9998746288`;
- development data only from seed families `20261004–20261007`;
- no Stage 8C confirmation rows, Stage 8I rows, original Stage 8J outcomes, or
  amended Stage 8J outcomes may retrain the model before this experiment;
- inference receives only the existing public allowlist and typed public target
  semantics;
- search scores, terminal labels, future draws, and hidden opponent identities
  are forbidden inputs.

## Fresh seed families for the amended run

Predeclared untouched families:

- `20261020`
- `20261021`
- `20261022`
- `20261023`

For each family, run both deck orientations:

- `ST Forge Full.dck` first vs `Benchmark Red Forge.dck`;
- `Benchmark Red Forge.dck` first vs `ST Forge Full.dck`.

Each orientation/seed runs in its own JVM with exactly **one game** so decision
indices and the single-intervention budget cannot cross game boundaries.

Total paired experiments: **8 baseline games + 8 controlled games**.

The original `20261016–20261019` families are already observed and remain
historical failure evidence only. Once the first amended baseline runs, the new
`20261020–20261023` families are also observed and may never again be described
as untouched evidence for a changed policy or changed Stage 8J gate.

## Controlled side and intervention recipe

Only the Forge player whose public name begins with `Ai(1)-` is eligible for
Stage 8J control. The opposite side remains completely Forge-controlled.

From each baseline game, inspect returned-action captures in chronological order.
Choose the **first** decision satisfying all of these conditions:

1. the capture reached the real `SpellAbilityPicker` returned-action boundary;
2. the acting player is `Ai(1)-`;
3. the frozen model has one unique preferred action;
4. that preferred action differs from Forge's baseline selected action;
5. the preferred learned action's complete identity explicitly contains
   `targets=<none>`;
6. that preferred action is already in the exact complete candidate set captured
   by Forge;
7. every candidate replayed successfully in all three information-set worlds.

The external request file contains only:

- `decision_index`;
- `complete_action_identity`.

No search score, outcome, seed label, hidden card identity, or sampled
hidden-world identity is included.

If a game has no eligible target-free intervention, it receives no control
request and runs Forge-only in the controlled pass.

## Live fail-closed boundary

At the requested decision, the controlled run must reproduce the baseline public
state and complete candidate set before the request may be applied.

The adapter may select only an exact `RootActionTable.ScoredAction` present in
that live Forge candidate set. It cannot construct or mutate an action.

The adapter is configured with:

- `require_forge_match=false` — the one predeclared learned action may differ;
- `allow_missing=true` — every non-requested decision falls through to Forge;
- `actor_prefix=Ai(1)-` — a request reaching another player is an error.

Pass/defer decisions remain Forge-controlled. The model cannot directly request
a pass in Stage 8J. Targeted learned actions are rejected by the Stage 8J planner;
the original target-bearing failure is not hidden or converted into a pass.

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
Stage 8F controller-acceptance, and Stage 8H terminal-lifecycle chain as every
Forge-selected action.

## Predeclared safety gate

A Stage 8J safety pass requires all of the following:

1. all Stage 8C–8J Python regressions pass;
2. patched Forge 2.0.15 builds;
3. Stage 6 complete-action, Stage 8C typed-target, and Stage 8I fail-closed Java
   regressions pass;
4. all 16 games complete without timeout, exception, strategy-fusion failure, or
   replay failure;
5. at least **4 of the 8** paired games contain an eligible learned intervention;
6. every planned intervention is applied exactly once;
7. zero intervention requests are accepted outside the live captured legal set;
8. zero control events occur for the wrong actor;
9. the controlled run is identical to its baseline at every captured decision
   before the intervention;
10. the intervention-time public state and complete candidate descriptors match
    the baseline exactly;
11. every planned learned action explicitly has `targets=<none>`;
12. every applied learned action reaches returned-action, controller-acceptance,
    and one clean terminal lifecycle outcome;
13. zero dispatch failures, lifecycle anomalies, or pending tracked actions occur;
14. Forge remains the rules referee;
15. `promotion_allowed=false`,
    `broader_learned_control_allowed=false`, and
    `targeted_learned_actions_allowed=false`.

## Outcome measurement

For the controlled `Ai(1)-` side, record paired baseline and controlled game
scores:

- win = 1;
- draw = 0.5;
- loss = 0.

The paired score delta is **descriptive only**. It is not a Stage 8J pass/fail
criterion and must not be used to move the gate after the result is known.

Reason: the frozen Stage 8D model was trained to approximate Forge's fixed-root
counterfactual scores, not to maximize independently observed match win rate.
Stage 8J establishes whether a genuinely different model-requested legal,
target-free action can traverse the full live Forge lifecycle safely.

## Target-free rerun result

The amended target-free run used untouched seed families
`20261020–20261023`, both deck orientations, one game per JVM.

GitHub Actions run: `37520372008`

Artifact: `stage8j-target-free-single-learned-intervention`
(`11439104210`)

Result:

- 8 paired experiments;
- 7 interventions planned;
- 7 interventions applied;
- 0 lifecycle anomalies;
- 0 invalid requests accepted;
- 0 pre-intervention drift;
- controlled-side baseline score: 4.0;
- controlled-side result score: 4.0;
- score delta: 0.0;
- **safety gate passed**.

This establishes that bounded learned substitutions can safely traverse the live
Forge lifecycle when the substitution does not perturb the unresolved
act-now-versus-defer boundary.

It is still not a playing-strength promotion. `promotion_allowed=false` and
`broader_learned_control_allowed=false`.

## Next stage after the safety pass

Do **not** immediately enable unrestricted learned control.

The next justified stage is **Stage 8K: return-boundary learned control**. Forge
must first finish its normal search and current-vs-later-phase decision. Only
after Forge has committed to returning an action may the learned controller
replace that action with another exact complete action already present in Forge's
live legal candidate set.

Stage 8K may include targeted actions, but targeting is not reconstructed by the
learner. The exact Forge candidate recipe must replay normally through Forge.
This isolates the real control-boundary defect found in the original Stage 8J
failure while preserving Forge's legality, timing and rules authority.

Only after that boundary is proven should the project broaden learned control
and collect outcome-bearing public-state/action trajectories for an
outcome-oriented policy/value signal. Training and evaluation must remain
seed-family isolated and hidden-information safe.
