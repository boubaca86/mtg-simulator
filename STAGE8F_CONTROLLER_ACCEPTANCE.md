# Stage 8F — controller-acceptance binding

Status: **passed CI controller-acceptance validation**.

Stage 8E established which Stage 8 search proposals actually survive planning and
are returned by `SpellAbilityPicker`. Stage 8F moves one boundary farther:
it verifies whether each exact returned `SpellAbility` reaches
`PlayerControllerAi.playChosenSpellAbility` and whether Forge's own dispatch
path reports success.

## Why this stage exists

A returned action is still not automatically an accepted play. Between the
simulation planner and the game engine, Forge may need to:

- re-check a land ability;
- apply deferred targeting;
- apply extra keyword costs;
- choose modes;
- pay costs;
- move a spell to the stack;
- reject a now-invalid play.

The expert-AI project must measure that boundary rather than silently assuming
every returned proposal became a real play.

## Safety and rules boundary

Stage 8F remains audit-only.

- Forge 2.0.15 still creates legal actions and executes every game rule.
- The learned shadow model still has no action-selection or execution interface.
- No custom card text, cost, mode, target, X value or gameplay semantic is changed.
- The existing Forge 2.0.15 `PlayerControllerAi.playChosenSpellAbility` return
  behavior is preserved exactly: it continues to return `true` as before.
- The internal boolean returned by `ComputerUtil.handlePlayingSpellAbility`
  is observed, not substituted into controller behavior.
- Returned actions are associated with dispatch using Java object identity and a
  consume-once audit bridge. Default/non-full-simulation AI actions are ignored
  because they have no Stage 8 capture.
- The bridge contains only existing complete-action identity plus public
  turn/phase/actor metadata. It does not inspect opponent hidden zones.
- `promotion_allowed=false`.

## Acceptance event

For each Stage 8E returned action that reaches the AI controller, Forge emits
`EXPERT_STAGE8_CONTROLLER_ACCEPTANCE` with:

- the exact Stage 8 capture decision index;
- the Stage 8E priority-return index;
- exact complete-action identity;
- public turn, phase and acting player;
- Forge dispatch success/failure;
- whether the action was a land ability;
- whether Forge marked the action skipped after dispatch;
- proof that the return and dispatch public contexts still match.

The audit requires a one-to-one mapping between returned actions and controller
acceptance events. Identity, priority index or public-context drift fails closed.

## Validation protocol

The CI run reuses observed seed `20261012`, four games in each deck orientation.
This does not consume fresh strength-test seeds because Stage 8F is an integration
test, not a model comparison.

A clean integration pass requires:

1. all existing Stage 6 complete-action and Stage 8C target-boundary regressions pass;
2. all Stage 8E return bindings remain valid;
3. every Stage 8E returned action has exactly one Stage 8F acceptance event;
4. zero identity/context/index mismatches;
5. the controller return semantics remain unchanged;
6. zero Forge dispatch failures in the validation games.

If a dispatch failure is observed, it is retained as evidence and treated as a
real search-to-execution mismatch to diagnose rather than being hidden or
re-labeled as success.

Even a clean Stage 8F pass does **not** prove stack resolution. The next boundary
would trace accepted actions to stack/no-stack completion or resolution without
giving the learned policy control.


## Verified result

Run `37454222152` at commit `373857b7d5456c8366d3ea9b6cda866e39ff3a69`
passed the unit tests, Forge 2.0.15 build, Stage 6 complete-action regression,
Stage 8C typed-target regression, return binding, and controller-dispatch audit.

Across eight completed observed-seed games:

- 120 Stage 8E returned expert actions were seen;
- all **120/120** produced exactly one controller-acceptance event;
- all **120/120** Forge dispatches reported success;
- failed dispatches: **0**;
- missing acceptance events: **0**;
- identity/context/index binding mismatches: **0**;
- Forge's original `playChosenSpellAbility` return behavior remained unchanged.

Artifact `11409400859`, SHA-256
`6cd790a4fb5c7609713665deab90dec648f49e2733099f1bd9237216f47a8842`.

This is integration evidence, not a new playing-strength result.
`promotion_allowed=false` remains in force.

The next justified integration step is a genuinely live, one-way shadow sidecar:
it should consume the same public Stage 8 capture stream while Forge is playing,
emit recommendations immediately, have no command path back into Forge, and
reproduce those recommendations exactly in an offline post-game audit.
