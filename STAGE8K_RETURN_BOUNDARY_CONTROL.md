# Stage 8K — learned control at the final Forge return boundary

Status: **predeclared engineering stage; not yet promoted or executed**.

Stage 8K exists because deeper inspection of the original Stage 8J failure changed
the diagnosis.

## Corrected Stage 8J failure diagnosis

The original Stage 8J run selected a learned complete action:

- **Lightning Strike**
- target: **Servitor**
- capture decision index: **7**

That action was present in Forge's exact captured legal candidate set. However,
the missing lifecycle event did **not** prove that target rebinding failed.

The controlled log shows what actually happened:

1. the early Stage 8J adapter replaced Forge's root search winner with Lightning
   Strike;
2. the learned Lightning Strike candidate had a lower fixed-root score than
   Forge's original Lightning Elemental choice;
3. after that early substitution, Forge's existing phase-bloom heuristic compared
   the modified current-phase plan with a later after-blockers plan;
4. Forge therefore chose to **wait** instead of returning an action in MAIN1;
5. the same Lightning Strike targeting Servitor was later returned and accepted
   by Forge at COMBAT_DECLARE_BLOCKERS.

Therefore the first real integration defect was a **control-boundary defect**:
Stage 8J altered Forge's search result *before* Forge finished its own
current-vs-later-phase decision. The target itself was not shown to be invalid.

The original failure remains valid evidence. This stage does not relabel it as a
pass.

## Objective

Prove that one learned alternative can replace Forge's chosen action only after
Forge has committed to returning an action, while preserving:

- Forge's search;
- Forge's current-vs-later-phase decision;
- Forge legality;
- exact complete-action semantics;
- targets, modes, X and explicit choices;
- hidden-information restrictions;
- controller dispatch;
- terminal lifecycle tracking.

The intended boundary is:

**Forge search → Forge phase/defer decision → learned substitution → real
SpellAbility return → controller acceptance → Forge terminal outcome**

The learned policy must never modify the earlier two Forge stages.

## Frozen policy

Use the exact frozen Stage 8D public-only checkpoint:

`6c4dd27f364aaa6c9e9100f88eb323eaa419dab0a49845ee0f748b9998746288`

No Stage 8J or Stage 8K outcome may retrain it before this experiment.

Learner inputs remain limited to the existing public-information allowlist and
typed public target semantics. Opponent hidden cards, future draws, sampled
hidden-world identities, Forge terminal outcomes and search scores are not model
inputs.

## Fresh seed families

Reserved untouched families:

- `20261024`
- `20261025`
- `20261026`
- `20261027`

Each seed is run in both deck orientations, one game per JVM:

- `ST Forge Full.dck` vs `Benchmark Red Forge.dck`;
- `Benchmark Red Forge.dck` vs `ST Forge Full.dck`.

Total: **8 Forge-only baselines + 8 controlled games**.

Once any of these baselines is run, the family is observed and may not be reused
as untouched evidence for a changed Stage 8K protocol.

## Intervention selection

Only the public player name beginning with `Ai(1)-` is controlled.

From each Forge-only baseline, choose the first capture that:

1. actually reached the Stage 8E SpellAbility return boundary;
2. belongs to the controlled actor;
3. has a unique frozen-model recommendation;
4. recommends an action different from Forge;
5. contains that recommendation in Forge's exact live complete-action candidate
   set;
6. replayed the candidate successfully in every information-set sample.

Unlike the amended Stage 8J target-free experiment, Stage 8K permits targeted
actions. At least **one targeted learned intervention** is required for the
Stage 8K gate to pass.

## Request format

Each request contains exactly three fields:

1. `decision_index`;
2. expected Forge complete-action identity from the frozen baseline;
3. requested learned complete-action identity.

The two identities are URL-safe base64 encoded only for transport.

The request contains no hidden information and no search score.

## Two-step live control

### 1. Arm during Forge search

When the matching decision index is encountered, the adapter must verify:

- controlled actor matches;
- expected Forge action matches the live Forge action;
- requested learned action is present in the exact live candidate set;
- requested action differs from Forge.

The request is then **armed only**.

The adapter must return Forge's own action to the search code.

An audit event records:

`early_substitution=false`

### 2. Apply after Forge finalizes its plan

Only after `createNewPlan` has completed Forge's current-vs-later-phase logic may
the adapter inspect the final Forge plan.

The substitution is permitted only if:

- the final plan carries the same capture decision index;
- its complete action identity still equals the predeclared Forge identity;
- the armed learned candidate still exists;
- there is exactly one root action in the bounded information-set plan.

The learned candidate is copied into a new one-action plan without editing its
ability, targets, modes, X, choices or cost information.

The original Forge plan is not mutated.

## Target handling

Stage 8K does not invent, parse or reconstruct a target from learner text.

For a targeted learned action, the candidate already contains Forge's executable
target recipe captured by the fixed-root search. The normal
`getPlannedSpellAbility` path must replay that exact recipe against the live
state.

The existing exact target replay check remains active.

If target replay fails, the stage fails. It must not choose another target or
silently fall back to Forge.

## Forge authority

Forge 2.0.15 remains the sole rules referee.

Stage 8K must not change:

- custom card definitions;
- card text;
- mana costs;
- payment;
- announced X;
- modes;
- targets;
- explicit card choices;
- priority/pass rules;
- triggers;
- replacement effects;
- stack ordering;
- resolution;
- game-result rules.

## Predeclared safety gate

A Stage 8K pass requires:

1. all earlier Python safety regressions pass;
2. Stage 8K Python tests pass;
3. patched Forge 2.0.15 builds;
4. existing complete-action, typed-target, sparse-control and lifecycle Java
   regressions pass;
5. the Stage 8K Java regression proves that search-time arming does not substitute
   the action;
6. all 16 fresh games complete;
7. at least **4** paired games contain a learned intervention;
8. at least **1** applied intervention is targeted;
9. every request matches the expected live Forge action and exact live candidate
   set;
10. zero gameplay drift occurs before the intervention;
11. the controlled capture at the intervention remains byte-equivalent to the
    Forge-only baseline capture;
12. the returned action equals the learned requested identity;
13. controller acceptance preserves that exact identity;
14. one clean Forge terminal lifecycle outcome is observed for every accepted
    action;
15. zero dispatch failures or lifecycle anomalies occur;
16. Forge search and phase deferral are recorded as unchanged;
17. `promotion_allowed=false` and
    `broader_learned_control_allowed=false`.

Match-result changes are descriptive only. Stage 8K is an integration/safety
gate, not a playing-strength promotion.

## Next stage after a pass

After Stage 8K proves the final execution boundary, the next justified direction
is outcome-bearing exploration.

Use the bounded return-boundary controller to collect public-state/action
trajectories with actual game outcomes. Train and evaluate an outcome-oriented
policy/value signal on isolated seed families before granting more than one
learned intervention per game.

Forge continues to generate legal actions and referee every game.
