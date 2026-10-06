# Stage 8F — controller-acceptance result

Status: **passed**.

Workflow run `37454222152` at commit
`373857b7d5456c8366d3ea9b6cda866e39ff3a69` passed the predeclared
integration checks.

Artifact `11409400859`:
SHA-256 `6cd790a4fb5c7609713665deab90dec648f49e2733099f1bd9237216f47a8842`.

## Observed integration result

Across eight games using already-observed seed `20261012`:

- 120 Stage 8E actions reached the real `SpellAbilityPicker` return boundary.
- 120/120 produced a matching Stage 8F controller-acceptance event.
- 120/120 reported successful Forge dispatch.
- 0 returned actions were missing a controller-acceptance event.
- 0 capture/action/context/index binding mismatches occurred.
- 0 Forge dispatch failures occurred.
- Forge 2.0.15 controller return semantics were preserved exactly.
- `promotion_allowed=false`.
- Stack resolution is not claimed by this result.

This is integration evidence, not fresh playing-strength evidence. The learned
shadow model still cannot control gameplay. Forge remains the legality/rules
referee.

A follow-up reporting-only fix at commit
`96b4a8425a9e5d028f0412b74b3f0d4dc19f4294` preserves the already-computed
Stage 8E shadow agreement aggregate inside the Stage 8F report; it does not
change instrumentation or gameplay.

## Next justified boundary

Stage 8G should trace successfully dispatched captured actions to a terminal
public lifecycle outcome:

- synchronous no-stack completion for land actions;
- resolved on the stack;
- fizzled on resolution; or
- removed/countered before resolution.

That stage must remain observation-only and must not let the learned model alter
Forge's action selection, costs, targets or resolution.
