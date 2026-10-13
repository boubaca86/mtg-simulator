# Stage 11Y: own-hand cleanup in the current combat actors

Base: Stage 11X commit `cdd669506ceadef5c5a5a37643c6c9c4ced69953`,
[PR #31](https://github.com/boubaca86/mtg-simulator/pull/31).
This integrates the cleanup protocol prototyped separately in
[PR #27](https://github.com/boubaca86/mtg-simulator/pull/27) with the current
mixed-creature reciprocal actor, including the current Elf-defense policy. Neither parent
work nor this change is merged by this stage. Exact CI evidence is recorded
in the Stage 11Y pull request after the runs complete.

## Behavior

Previously, the current controller rejected every maximum-hand-size discard
callback. The reciprocal actor now supports `own_hand_order`, returning a full
permutation of indices into its own current hand. No count, maximum hand size,
phase, turn, priority, stack or opponent hidden cards are sent with the request.
The existing observation fields are unchanged.

The simple ranking discards unsupported spells first, then surplus basic lands,
then smaller supported creatures. It retains preferred basics (Forest first)
for the largest supported creature in its hand, up to four. It uses only the
own-hand names; it is a bounded heuristic, not optimal discard strategy.
Normal opening-hand, casting, attack and block requests remain available in the
same persistent process.

Forge alone decides when cleanup occurs, which player is active, the applicable
maximum, and the exact positive excess. The adapter checks those facts, validates
the complete permutation, binds indices to an unchanged physical hand snapshot,
and returns the first required cards. Native Forge performs the discards, clears
marked damage and advances the turn. Other actor modes remain denied.

The worker now bounds reads before buffering a whole input line, requires a
UTF-8 newline-terminated frame of at most 16 KiB, and checks hand (128 cards), table,
deck-multiset, name, objective and life-value bounds. Duplicate JSON fields,
nonfinite numbers and unapproved information fields remain rejected. The referee
uses its existing shared I/O deadline and strict reply parser.

## Verification

The exact-method gate requires 148 Forge methods: 140 inherited plus eight new:

1. Two independent workers agree on own-hand rankings, invariant under changes
   to the opponent's secret hand/library and the actual own-library contents.
2. Malformed, duplicate, out-of-range, noninteger and extra-field replies fail.
3. Wrong phase, turn, count and unlimited-hand callbacks fail before actor choice;
   a valid callback selects cards without moving them.
4. Stale or foreign physical hand bindings fail before discarding anything.
5. Native cleanup discards from nine to seven, clears marked damage, and resumes
   the next turn with preserved physical-card inventories.
6. A separate native fixture with maximum five discards four, demonstrating that
   the actor neither needs nor supplies the hand-size rule.
7. One uncurated registered 60-card mixed-creature game, left registered first.
8. One separate uncurated game with the registration order reversed.

The cleanup fixtures explicitly arrange cards and the starting end step; they
are not natural-opening games. The two natural games use real shuffled openings,
London mulligans, 20 life, paid casts and native outcomes. Their actual cleanup
counts, including zero, are reported without retries or favorable seed selection.

Twenty-three Python checks cover the new request, mixed-creature ranking, continued
Elf-defense behavior, framing, independent processes, input rejection and
strict evidence gates. The inherited disposable-validator mutation check remains
required. These policy/evidence checks are separate from native Forge tests.

## Limits and references

This handles ordinary maximum-hand-size cleanup for owned cards in the controlled
hand. Arbitrary effect-driven discards, unsupported discard triggers, complete
Magic coverage and custom Ultimatus scoring are not implemented. The separate
processes still share an execution environment; they are not hardened OS isolation.
No protected evaluation seeds, training, strength promotion or merge are performed.
Successful tests and independent review remain prerequisites for merging.
The PR is stacked on PR #31 so neither parent changes nor this feature need
to be merged before review. The opening and combat implementation body is
identical to the parent; only input validation and own-hand ordering are added.

- [Official rules](https://magic.wizards.com/en/rules),
  [2026-09-25 text](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt),
  rules 514.1 and 514.2.
- [Pinned Forge PhaseHandler](https://github.com/Card-Forge/forge/blob/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676/forge-game/src/main/java/forge/game/phase/PhaseHandler.java),
  cleanup callback and native discard/damage removal.
