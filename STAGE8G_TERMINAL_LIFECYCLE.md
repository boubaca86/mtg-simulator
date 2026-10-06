# Stage 8G — terminal action lifecycle

Status: **implemented; CI validation required**.

Stage 8F proved that every captured action returned by the full-simulation planner
reached `PlayerControllerAi` and successfully passed Forge's real dispatch path
in the validation corpus. Stage 8G follows those same accepted actions one step
farther until Forge reaches a terminal lifecycle outcome.

## Terminal outcomes

A tracked action may terminate as:

- `resolved` — a stack action completed resolution normally;
- `fizzled` — Forge resolved the stack entry as fizzled;
- `removed-before-resolution` — the tracked stack entry was removed/countered
  before normal resolution;
- `no-stack-completed` — a tracked land action resolved synchronously through
  Forge's existing land-action path.

Two anomaly outcomes are also instrumented and must fail the clean validation:

- `dispatch-failed`;
- `nonland-success-without-stack-binding`.

## Object-identity chain

The lifecycle audit never reconstructs an action from text.

1. Stage 8E binds the exact winning complete-action recipe to the exact
   `SpellAbility` object returned by `SpellAbilityPicker`.
2. Stage 8F verifies that object reaches the real AI controller dispatch.
3. Stage 8G registers the returned object in a game-layer audit registry.
4. On successful `ComputerUtil.handlePlayingSpellAbility`, the registry moves
   the record from the original object to the final stack `SpellAbility`
   object after Forge has applied its normal transformations.
5. `MagicStack` consumes that exact stack-object record when Forge resolves,
   fizzles or removes the entry.

The registry uses Java object identity and consume-once mappings. It does not
modify stack ordering, legality or game objects.

## Safety boundary

- Forge 2.0.15 remains the sole rules referee.
- The learned model remains read-only and cannot select or execute an action.
- No custom-card rules, costs, targets, choices, modes, X values or card text are
  altered.
- Existing Forge controller return behavior is unchanged.
- No opponent hidden-hand or hidden-library identity is inspected or serialized.
- Lifecycle records contain only existing complete-action identity plus public
  turn/phase/actor metadata.
- `promotion_allowed=false`.

## Validation protocol

The CI run reuses observed seed `20261012`, four games in each deck orientation.
This is an integration test, not new playing-strength evidence.

A clean Stage 8G pass is predeclared to require:

1. all Stage 8F, Stage 8E, Stage 8D and typed-target audit regressions pass;
2. Forge 2.0.15 builds with the lifecycle instrumentation;
3. Stage 6 complete-action and Stage 8C typed-target Forge regressions pass;
4. all Stage 8F successful dispatches have exactly one Stage 8G terminal event;
5. terminal lifecycle coverage is 100%;
6. `pending_at_game_end == 0`;
7. zero terminal identity/index/context mismatches;
8. zero lifecycle anomalies.

If the observed corpus contains a legitimate action still pending when the game
ends, this first validation will fail rather than silently relaxing the criterion.
That case must be inspected and documented before any revised protocol.

A clean pass verifies the capture → return → controller dispatch → Forge terminal
lifecycle chain. It still does not authorize learned-policy control or establish
win-rate strength.
