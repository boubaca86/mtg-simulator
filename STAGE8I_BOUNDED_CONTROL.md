# Stage 8I — bounded complete-action control replay

Status: **implementation ready for CI validation**.

Stage 8H established a complete audited chain from public information-set search
through exact Forge terminal handling. Stage 8I introduces the first policy-control
adapter, but deliberately does **not** permit the learned model to change gameplay.

## Objective

Prove that an external request path can reference one exact complete action that
Forge already enumerated as legal, without inventing or mutating gameplay state.

The Stage 8I gate uses a two-pass replay:

1. Baseline pass: Forge chooses normally and emits Stage 8 captures.
2. An external request file is generated from only:
   - \`decision_index\`
   - \`complete_action_identity\`
3. Controlled pass: the bounded adapter reads that file and requests the same
   complete actions on the same seed.
4. The controlled run must reproduce the baseline capture, returned-action,
   controller-acceptance, terminal-lifecycle and game-result sequences exactly.

The request file contains no search score, terminal label, future draw,
opponent-hidden hand identity, opponent-library identity or sampled hidden-world
identity.

## Forge authority

Forge 2.0.15 remains the sole legality and rules referee.

The adapter receives the already-aggregated complete candidate set. It may return
only an exact \`RootActionTable.ScoredAction\` already present in that set. It cannot:

- construct a new SpellAbility;
- alter a mana cost;
- change X;
- change mode choices;
- change targets;
- change explicit card choices;
- bypass normal controller dispatch;
- alter stack ordering or resolution;
- inspect opponent hidden cards.

An identity absent from the captured candidate set raises an exception. There is
no fallback to a guessed action.

## Stage 8I replay lock

For this stage, \`forge.expert.stage8.control.require_forge_match=true\`.

Therefore even a different **legal** captured action is rejected if it differs
from Forge's original selected action. This is intentional: Stage 8I validates
the control transport and safety boundary without changing gameplay.

\`promotion_allowed=false\` throughout.

## Validation

CI uses already-observed seed \`20261012\`, four games in each deck orientation,
twice:

- one baseline pass;
- one externally requested replay pass.

A clean pass requires:

- all existing Stage 8C–8H safety/audit regressions pass;
- patched Forge 2.0.15 builds;
- Stage 6 complete-action and Stage 8C typed-target Forge regressions pass;
- a direct Java regression accepts an exact captured identity and rejects an
  uncaptured identity fail-closed;
- every baseline capture produces exactly one external request;
- every external request is consumed exactly once;
- every request is found in the current captured candidate set;
- every request equals Forge's baseline selection;
- baseline and controlled Stage 8 capture payloads are byte-semantically equal;
- returned-action sequences match exactly;
- controller-acceptance sequences match exactly;
- terminal-lifecycle sequences match exactly;
- normalized game results match exactly;
- both baseline and controlled logs independently pass the Stage 8H whole-log
  lifecycle audit;
- no custom-card rule or gameplay semantic changes.

This stage is integration/safety evidence, not new playing-strength evidence.

## Next justified step if Stage 8I passes

Stage 8J may predeclare a **very narrow learned-policy intervention** on fresh,
disjoint seeds. The frozen public-only model may choose a different action only
when:

- the action is already in Forge's complete legal candidate set;
- the model has a unique preference;
- all hidden-information and three-world replay contracts pass;
- Forge still executes and resolves the selected action;
- pass/defer behavior remains Forge-controlled until represented explicitly;
- safety rollback to Forge is fail-closed and auditable.

Stage 8J must measure actual game outcomes against a matched Forge-only baseline
before any broader control is considered.
