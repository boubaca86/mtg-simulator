# Stage 8H — terminal action lifecycle

Status: **corrected implementation; CI validation required**.

Stage 8G proved that the frozen Stage 8D public-only model can run as a one-way
live shadow sidecar beside a real Forge match with exact offline/live replay and
no control channel into Forge.

Stage 8H follows each controller-accepted Stage 8 action farther through Forge
until it reaches a terminal lifecycle outcome.

## Why the first lifecycle pilot failed

The first lifecycle pilot was temporarily named Stage 8G and ran as workflow
`37455625769`. Forge built, all existing action/target safety regressions passed,
and all eight games completed. The final whole-log audit correctly quarantined
the run because Forge had printed:

`java.lang.IllegalStateException: Stage 8G SpellAbility already registered`

The first real occurrence was a repeated activation of the same
`Whirlwind S.T` ability.

That was an instrumentation identity bug. Forge legitimately reuses a
`SpellAbility` object for later activations, while each activation placed on the
real stack receives a distinct `SpellAbilityStackInstance`. Treating reusable
`SpellAbility` identity as terminal stack identity was therefore incorrect.

No card rule, custom-card text, learned model, target choice, cost, game result or
promotion threshold was changed in response.

## Corrected identity chain

Stage 8H uses two identity levels:

1. **Returned-action queue.** The exact `SpellAbility` returned by the
   full-simulation planner temporarily owns an ordered queue of audit records.
   Reusing that Java object for a later activation is legal.
2. **Exact stack-instance identity.** As soon as Forge creates the real
   `SpellAbilityStackInstance`, exactly one returned record moves to that unique
   stack instance. From that point until terminal handling, the reusable source
   ability is irrelevant to audit identity.

Forge may copy an activated ability before putting it on the stack and may
transform a spell before stack insertion. The audit handles both cases without
parsing names or IDs:

- transformed spell objects receive an identity-only temporary alias;
- copied activated abilities use Forge's existing `getOriginalAbility()`
  reference;
- the alias/returned record is consumed when the exact stack instance is created.

This fixes the pilot defect without suppressing duplicate checks at the actual
stack-instance boundary.

## Terminal outcomes

Every controller-accepted action is expected to terminate as exactly one of:

- `resolved` — a stack entry completed resolution normally;
- `fizzled` — Forge resolved the stack entry as fizzled;
- `removed-before-resolution` — the exact tracked stack instance was removed
  before normal resolution;
- `no-stack-completed` — a tracked land action completed synchronously.

Two explicit anomaly outcomes are retained and fail the clean gate:

- `dispatch-failed`;
- `nonland-success-without-stack-binding`.

A controller-accepted action still pending at game end is reported, never silently
counted as resolved.

## Safety boundary

- Forge 2.0.15 remains the sole rules referee.
- The frozen learned model remains read-only and cannot choose or execute actions.
- No custom-card text, mana cost, target, mode, X value, card choice, stack order
  or resolution semantic is altered.
- Forge's existing `PlayerControllerAi.playChosenSpellAbility` return behavior
  remains unchanged.
- No opponent hidden-hand or hidden-library identity is inspected or serialized.
- Search scores, future draws and outcome labels are not model inputs.
- `promotion_allowed=false`.

## Validation protocol

The CI validation deliberately reuses observed seed `20261012`, four games in
each deck orientation. This is integration validation, not fresh playing-strength
evidence.

A clean Stage 8H pass requires:

1. all Stage 8H, Stage 8F, Stage 8E, Stage 8D and typed-target Python regressions pass;
2. patched Forge 2.0.15 builds;
3. Stage 6 complete-action and Stage 8C target-boundary Forge regressions pass;
4. all eight games complete without timeout, exception, strategy-fusion failure
   or replay failure;
5. every controller-acceptance event receives exactly one terminal event;
6. terminal coverage is 100%;
7. zero actions remain pending at game end;
8. zero lifecycle identity/index/context mismatches;
9. zero lifecycle anomaly outcomes;
10. the frozen shadow agreement aggregate remains available for audit.

If the corpus contains a legitimate action still pending when a game ends, the
predeclared gate fails and that case must be documented before any protocol
revision. The threshold is not relaxed after observing the result.

A pass verifies the full capture → returned action → controller dispatch → exact
Forge terminal-lifecycle chain. It still does not authorize learned-policy
control or establish a win-rate improvement.
