# Stage 9J — Genuine Forge draw → isolated AI land decision

**Verified:** GitHub Actions [run 38134184504](https://github.com/boubaca86/mtg-simulator/actions/runs/38134184504) on
`expert-stage9a-forge-decision-pairs`, head `a58728cf7698d7b211232c422addfde6d05783ed`,
**SUCCESS**. Pinned Forge tag `forge-2.0.15` resolves to commit
`4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
Maven exit 0 and **BUILD SUCCESS**. Strict Surefire XML verifier PASS,
exactly **five TestNG methods executed, five passed, zero failures, zero errors,
zero skipped**. Evidence artifact:
`stage9j-forge-draw-and-land-38134184504-1` (ID `11700332202`).

## What ran

1. Real Forge `Game` and two different Python AI worker processes.
2. Workers receive only allowed data: objective, own hand, own decklist
   multiset (not order), visible battlefield cards and authoritative
   self/opponent life totals. They receive neither turn/phase information
   nor opponent hand, library order or private hidden card identities.
3. Before drawing, neither actor has a basic land in hand and both
   choose `pass`. The trusted referee invokes actual pinned Forge
   `DrawEffect.resolve` to move Forest into Player 1's hand and Island
   into Player 2's hand from real Forge libraries.
4. Each worker now chooses `play_land` using only its newly observed
   own hand. Actual `Player.canPlayLand` and `Player.playLand` accept
   legal plays, including phase/turn limits, and reject the out-of-turn
   attempt. The test *fixture* changes active player with
   `devModeSet` for Player 2; no full real turn loop is claimed.
5. The other actor's observation remains byte-for-byte identical when
   an opponent actually draws. The actor cannot directly request a
   `draw_card` action. Other original Stage9I land/forged-command
   regression tests remain green.

## Nonclaims and next blockers

- Test-only isolated processes use separate PIDs, but no hardened
  filesystem, network or debugger sandbox.
- No genuine complete turn/priority/stack progression, target selection,
  mana payment, spell casting, combat, game win/lose termination or
  strategic expert policy has been demonstrated.
- The current test's `DrawEffect.resolve` is invoked by the trusted
  referee directly in a fixture, not triggered by Forge's automatic
  draw-step progression.
- Stage9A visibility-only production patch unapproved; Stage9E hidden
  alias/re-exile/stale grant/cost/rollback issues still open.
- No training, evaluation games, reserved seeds, merges or deployments
  performed.

## Next executable milestone: Stage 9K

Integrate the actual pinned Forge `PhaseHandler.mainLoopStep` priority
and turn progression with separate AI process decision callbacks,
without widening actor observations. Magic CR 117 and 500 require
consecutive passes from both players, stack resolution and phase end.
Maintain referee-only phase/turn/priority, and enforce legal actions in
Forge. Run a bounded, test-only, non-reserved two-player fixture first.
Only claim full games after runtime evidence.

Research:
- [Official Magic rules](https://magic.wizards.com/en/rules),
  effective September 25 2026, notably 117, 305, 500, 504.
- [Pinned Forge 2.0.15 source](https://github.com/Card-Forge/forge/tree/forge-2.0.15).
- [Cowling, Powley, Whitehouse 2012, ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
