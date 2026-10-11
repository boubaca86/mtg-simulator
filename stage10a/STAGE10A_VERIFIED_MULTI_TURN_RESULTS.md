# Stage 10A — Five and seven natural Forge turns after actual native match-start

**VERIFIED GREEN, October 11, 2026.**

- GitHub Actions run: https://github.com/boubaca86/mtg-simulator/actions/runs/38149408132
- Source branch: `expert-stage9a-forge-decision-pairs` **UNMERGED**; exact tested head:
  `bf8515dada8e4470e9a57c8a149acf62a92ca7d7`.
- Upstream Forge tag `forge-2.0.15`, exact pinned commit
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Maven **BUILD SUCCESS**, exit 0; **42 actual TestNG methods executed,
  42 passed, 0 failures, 0 errors, 0 skipped**.
- Strict independent exact-method Surefire XML verifier:
  **`gate:PASS`, `full_magic_game:false`**.
- Immutable CI artifact
  `stage10a-real-multiturn-38149408132-1`, ID `11706521741`,
  containing build logs, source hashes, actual per-method reports and gate.
- Source `forge_tests/Stage10ABoundedMultiTurnMatchTest.java`;
  `forge_tests/verify_stage10a_surefire.py`;
  `.github/workflows/stage10a-real-multiturn.yml`.
  Reuses unchanged process worker `stage9w/opening_hand_actor.py`.

## What's genuinely new

Two extra real pinned Forge TestNG methods, on top of all 40 Stage9Z
tests, start from the actual `Match.startGame` and
`GameAction.startGame` with two independently registered 60-card
Constructed decks. The referee's test-only first-turn hook aborts
**only the unlimited game loop**, after Forge has completed native
deck physical loading, private shuffling, first player selection,
own-hand-only London mulligans, new-game initialization and entry
into the native UNTAP phase. Two separate persistent restricted
Python workers remain attached for all post-start priority and
attacker decisions.

Next, the tests advance real
`PhaseHandler.mainLoopStep()` for **five** and **seven** separate
MAIN1 entries, respectively. No `devModeSet` calls, manually
injected draws or synthetic turn switches occur after true setup.
Every step asserts the real active player's
`getLandsPlayedThisTurn()` remains in `[0,1]`.
Five-turn coverage requires both actors to have completed
**two legal real land plays each**. Seven-turn coverage
checks three FIRST-player and three SECOND-player natural
draws according to the rule that only the starting player skips
its first draw. Both source and physical-zone decklists remain
reconciled; actor projections are checked for exclusion of
turn, phase, stack, priority, opponent hand, library top/order and
mulligan count. An opponent real `Player.shuffle`
does not change the other player's serialized view.

The tests measure bounded natural turns, not a
random-seed-based win-rate contest. No evaluation/training
seeds were used or consumed.

## Architectural security scope

Each actor observes **only** its objective, own hand/cards,
immutable own starting decklist card-name multiset, opponent
legally visible table cards, and two current referee-owned
life totals. The referee alone owns hidden cards/order,
priority, turn, mana payment, combat and game-rule enforcement.
The two independently executed worker processes do not hold
Forge `Game` or `Card` objects.

## Limits and next milestone

This is **not a complete Magic game or expert AI**.
The current narrow heuristic prioritizes playing a land
whenever any basic land remains in hand. After its legal land
drop it may keep requesting another land; the referee safely
rejects that proposal, but the actor can consequently
miss mana-paid creature casting in that turn. Prior test-only
creature/mana/stack/unblocked-combat paths remain covered by
the 40 regression tests but are **not claimed as verified
uninterrupted multi-turn combat after Match.startGame**.
Full proactive creature casting, legal attacking/blocking
plans and eventual match victory remain unfinished.

**Next Stage10B:** improve the *isolated* actor policy using only its
own hand, own observable table and process-local history
(not turn/phase/priority or opponent hidden state) so a
failed/used land attempt does not prevent legal creature casting.
Use genuine native match-start two-player multi-turn
tests that deterministically show **mana-paid creature casting,
priority/stack resolution, and later legal combat damage**
with a fully legal 60-card deck. Keep real Forge referee
authoritative; do not infer a win-rate target from asymmetric
or incomplete coverage. Before claiming OS-level isolation,
also harden two actor workers against file/network/process
or shared-state access and replace/audit inherited
`PlayerControllerAi` callbacks. Stage9E concealed identity,
stale authorization, re-exile 406.7 and cost rollback remain.
The user's custom Ultimatus negative-life and
library-exhaustion higher-life win rules are not stock Forge
and remain unimplemented.

No `main` merge, production deployment, reserved evaluation
seeds, training or model promotion.

Research:
- Official 2026 Magic rules: https://magic.wizards.com/en/rules
- 2026-09-25 CR 103.8a, 305, 505–506:
  https://blogs.magicjudges.org/rules/comprehensive-rules/
- Pinned Forge:
  https://github.com/Card-Forge/forge/tree/forge-2.0.15
