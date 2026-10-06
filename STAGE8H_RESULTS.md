# Stage 8H — terminal lifecycle result

Status: **passed**.

Workflow run `37457313304` at commit
`431544ef22d70c8f196e8f44c8b9b6f65826a156` passed the corrected terminal
lifecycle gate.

Artifact `11410845867`:
SHA-256 `1f04f7bddcd0d7ebc4586231f336a2f7a6edb344643f04f7d2a05ddafbe933b0`.

## Result

Across eight integration games using already-observed seed `20261012`:

- 120 actions reached the real SpellAbilityPicker return boundary;
- 120/120 reached PlayerControllerAi;
- 120/120 dispatched successfully;
- 120/120 received exactly one Forge terminal lifecycle event;
- 69 resolved through the stack;
- 51 completed through the synchronous no-stack land path;
- 0 dispatch failures;
- 0 lifecycle anomalies;
- 0 identity/index/context mismatches;
- 0 actions remained pending at game end;
- terminal coverage was 100%.

The frozen Stage 8D model remained read-only. On the 60 returned actions with
multiple candidates, its top set contained Forge's returned action for 46/60
(76.67%). It had 59 unique-preference cases, agreeing with Forge on 45/59
(76.27%).

## Pilot defect and correction

The earlier lifecycle pilot `37455625769` failed because it keyed stack
lifecycle state by reusable `SpellAbility` object identity. Forge legitimately
reuses an activated-ability object across activations. The corrected Stage 8H
implementation queues returned records by reusable ability only until Forge
creates the exact `SpellAbilityStackInstance`, then tracks terminal handling by
that unique stack-instance identity.

The correction changed only audit identity plumbing. It did not alter cards,
targets, costs, modes, X values, action choice, stack order or resolution.

## Meaning

The project now has an audited chain from:

`public information-set search -> complete action -> returned action -> controller dispatch -> exact Forge terminal outcome`

Forge 2.0.15 remains the rules referee and `promotion_allowed=false`.

The next justified stage is a bounded policy-control adapter. Its first gate must
prove that an external policy-selection path can request only an already captured
complete Forge-legal action, fail closed on invalid requests, and reproduce the
current Forge-selected action without gameplay drift before the frozen learned
model is ever allowed to choose a different action.
