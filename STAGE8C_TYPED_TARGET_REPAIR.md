# Stage 8C — typed target repair

Status: implemented and locally checked; real-Forge/live verification pending.

## Reproduced failure

The first Stage 8C resolver searched the displayed target string with
`\((\d+)\)`. A player-only target such as `[Ai(2)-Benchmark Red Forge]` therefore
produced card ID `2`. Depending on the visible zones, it could be represented as
an unrelated card or an unresolved card target. Numbers in other display names
could cause the same problem. The old set and alphabetical sorting also discarded
target ordering and repeated target occurrences.

The previous quick contract workflow had Stage 8C path triggers but did not run
the Stage 8C tests. Running them exposed a separate test defect: looking for the
substring `12` in rendered feature vectors also matched legitimate values such
as `0.125`. The test now changes physical IDs and verifies unchanged features.

## Repair

Forge's `PossibleTargetSelector` snapshots each selected object's **type and ID**
alongside its unchanged executable target recipe. `MultiTargetSelector` retains
those immutable references in target order. Neither display strings nor card or
player names are parsed to determine the target's type or identity.

`LegalDecisionFeatures` resolves the references against the actual root:

- Player references use only the public player roster and emit
  `zone=player|role=self/opponent|<player>` (public for another teammate).
- Card references use the acting player's hand and public battlefield, graveyard,
  exile and stack zones. Opponent hand and both libraries are never searched.
- Spell references use the public stack, never a fallback to the host card on
  the battlefield. The spell and card namespaces remain distinct.
- Unavailable targets are opaque. Face-down cards export no name, type, mana
  value or power/toughness. Descriptors are derived from the actual root, never
  from sampled-world card characteristics.

Order and repeated references survive the descriptor array. Internal references
contain no scores, names or sampled characteristics. Existing exact action
identities, target selection, game rules and all card definitions are unchanged.

Every new candidate carries `target_semantics_version=forge-public-targets-v2`.
The serializer, validator and Stage 8C model reject old/unversioned target arrays,
unknown attributes, duplicate attributes, nonfinite values and card facts attached
to opaque/player targets. Original Stage 8B rows without target arrays still work.
Do not add the new version marker to old rows: they require a new Forge capture.

## Verification

- 44 local unit tests and all three Stage 8 validator/serializer/parser scripts
  passed. The quick contract workflow now runs the target tests explicitly.
- The patch matches the pinned Forge selectors, and the static restricted-zone
  check still permits exactly three size-only accesses: opponent hand and each
  player's library.
- A real-Forge Java regression is now run before the full benchmark. It includes
  player/card ID collisions, punctuation in names, repeated/ordered targets,
  immutable snapshots, same-name creatures with different stats, simulated-copy
  isolation, hidden-zone traps, face-down opacity and spell/card separation.

Full Forge compilation, that Java regression and a corrected benchmark capture
must pass before the repaired target model's results are recorded.

## Scope

This fixes training-data correctness; it does not establish improved playing
strength. The existing target model pools descriptors, so preserving order in
capture does not yet make all of its features order-sensitive. Ability-dependent
target preferences and exact independent spell-instance distinctions on the stack
remain future representation work. Passing and deferred phase probes still need
execution-aware coverage. Learned gameplay remains disabled.
