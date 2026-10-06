# Stage 8E — returned-action binding

Status: **passed CI returned-action binding validation**.

Stage 8D proved that the frozen public-only shadow model can score every captured
search proposal without labels, hidden information or gameplay control. The next
integration problem is that a Stage 8 capture is not necessarily an action Forge
actually returns: Forge may evaluate current-phase and later-phase probes, discard
a plan, or pass priority.

Stage 8E adds audit-only metadata that binds the exact fixed root recipe selected
by the Stage 6 information-set search to the `Plan.Decision` that survives into
Forge's real top-level plan. When `SpellAbilityPicker` returns that exact action
to Forge, a public-only `EXPERT_STAGE8_RETURNED_ACTION` event is emitted. Explicit
top-level passes emit `EXPERT_STAGE8_PRIORITY_PASS`.

## Safety boundary

This stage does **not** give the learned model an execution interface.

- Forge still creates the legal action set, chooses the action and executes rules.
- The patch changes no card text, costs, targets, modes, X values or choices.
- Hidden opponent hand/library identities are never serialized.
- Recursive simulated branches do not emit returned-action/pass events.
- A returned-action event is bound to the exact Stage 8 capture decision index and
  exact complete-action identity; drift fails closed.
- The shadow model reads only the existing Stage 8D public allowlist.
- `promotion_allowed=false`.

The boundary is intentionally named **returned action**, not executed/resolved
action. Returning a `SpellAbility` from `SpellAbilityPicker` proves that the
captured plan reached Forge's real action-return boundary. It does not yet prove
that all downstream mode/choice handling completed or that the action resolved.

## Audit

`stage8e_returned_action_audit.py` first runs the existing whole-log validator.
It then requires:

1. every Stage 8 capture to match a Stage 7 observation;
2. every returned action to reference a previously seen capture in the same game;
3. exact complete-action identity equality between capture and return;
4. turn, phase and acting player equality at the public boundary;
5. at most one returned action per capture;
6. contiguous top-level priority-return indices;
7. explicit separation of returned actions, passes, and unbound probe/deferred captures.

The frozen Stage 8D checkpoint may score each bound capture after the binding is
verified. Agreement is instrumentation evidence only, not a win-rate or promotion
claim.

## Validation corpus

The CI workflow uses already-observed seed `20261012`, four games in each deck
orientation (eight games total). This is intentional: Stage 8E changes
instrumentation, not the frozen model, so no untouched seed is consumed for a
playing-strength claim.

The next stage after this passes is a live sidecar/post-acceptance integration
that verifies downstream acceptance/completion while still preventing the learned
policy from controlling Forge.

## Verified result

Run `37453082844` at commit `eac94389b6b109eaeca434abb3d2208afbd5abd0`
passed all unit, Forge build, Stage 6 identity and Stage 8 typed-target regressions.

Across eight completed games:

- 178 Stage 8 search proposals were captured;
- 120 exact proposals reached the real `SpellAbilityPicker` return boundary;
- 58 captures were correctly separated as probe/deferred plans;
- 1,345 explicit priority passes were recorded;
- **0 capture-to-return identity mismatches** occurred;
- returned-action binding coverage was 67.42%.

The frozen Stage 8D model scored all 120 bound actions. Of the 60 rankable
(non-forced) returned actions, Forge's returned action was in the shadow model's
top set 46 times (76.67%). The model had a unique preference on 59 of those
rankable actions and agreed with Forge 45 times (76.27%). These are shadow
agreement measurements, not playing-strength or win-rate claims.

Artifact `11408271210`, SHA-256
`5b28710e01891c54fdbe8e63ed583796ad4c0f8c38833cb393febd6ac247f5a1`.

The next integration boundary is the AI controller's
`playChosenSpellAbility` acceptance path plus an independently running live
shadow sidecar. The learned model remains unable to choose or execute actions.
