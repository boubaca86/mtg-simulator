# Stage 8C — typed target repair

Status: implemented; real-Forge regression, legal-information, deterministic
capture and the repaired 32-game ranking comparison passed.

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

The full Forge build and the real-object regression passed at
`c3add5b439c9907cea3c4b7e1f45c9e694584564`. The fixture uses initialized Forge
AI players, printed card rules and language services; it does not mock the target
resolver. Both original Stage 6 replay/aggregation regressions also passed.

| Check | CI run | Result |
| --- | --- | --- |
| Stage 8 contract and Python target regressions | [37398118547](https://github.com/boubaca86/mtg-simulator/actions/runs/37398118547) | Pass |
| Legal extractor boundary and Forge AI module build | [37398118513](https://github.com/boubaca86/mtg-simulator/actions/runs/37398118513) | Pass |
| Independent seeded live reproducibility | [37398118299](https://github.com/boubaca86/mtg-simulator/actions/runs/37398118299) | Pass; 85 observations, byte-identical across two runs |
| Full Forge build, real target regression and repaired capture | [37399134353](https://github.com/boubaca86/mtg-simulator/actions/runs/37399134353) | Pass; 32 games, 750 proposals, 518 rankable decisions |

The repaired corpus contains 2,035 versioned candidates: 512 player targets,
1,126 public battlefield targets and 397 untargeted candidates. Every player
target has an explicit role and no card characteristics; none is unresolved.

Removing only the two new typed-target fields reproduces the original Stage 8B
candidate corpus byte for byte, SHA-256
`106912cfd8800cad8abe65231abeca03578c4185205a2e11b19e87000cc8ee7e`.
The 750 observations and all six existing model reports also remain identical.
Thus the extra capture fields did not alter any recorded action or search score.
The complete target report reproduced locally. Evidence is in
`results/forge_expert_ai_stage8/typed-target-development-v2.json`.

The target model improves development top-choice agreement from 81.08% to 82.63%
and normalized regret from 0.09218 to 0.08209; see
`STAGE8C_DEVELOPMENT_RESULTS.md`. The earlier unversioned captures are not a valid
baseline. The subsequent predeclared confirmation is documented separately in
`STAGE8C_FRESH_SEED_RESULTS.md`.

## Scope

This fixes training-data correctness; it does not establish improved playing
strength. The existing target model pools descriptors, so preserving order in
capture does not yet make all of its features order-sensitive. Ability-dependent
target preferences and exact independent spell-instance distinctions on the stack
remain future representation work. Passing and deferred phase probes still need
execution-aware coverage. Learned gameplay remains disabled.
