# Stage 9X — Two-actor registered-deck opening sequence and first priority

**VERIFIED**, October 11, 2026:
https://github.com/boubaca86/mtg-simulator/actions/runs/38147078537

Development branch `expert-stage9a-forge-decision-pairs` remains **unmerged**;
validated head `7961a01465037b838fbf9266e0a2731a372b4fa3`.
Upstream Forge tag `forge-2.0.15` pinned to
`4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.

- Maven BUILD SUCCESS / exit 0.
- **35 actual TestNG methods executed, 35 passed, 0 failures,
  0 errors, 0 skips**.
- Independent strict XML checker **PASS**,
  `full_magic_game:false`.
- Archived CI artifact `stage9x-opening-game-38147078537-1`,
  ID `11704908537`, with pinned source hashes,
  Maven output, per-test XML and gate result.
- Files `forge_tests/Stage9XOpeningGameSequenceTest.java`,
  `forge_tests/verify_stage9x_surefire.py`,
  `.github/workflows/stage9x-opening-game.yml`;
  actor policy reused `stage9w/opening_hand_actor.py`.

## Newly demonstrated

A test-only **trusted referee** builds actual Forge library cards
from each player's immutable registered 20-card starting deck,
then uses actual `Player.shuffle(null)` to shuffle privately.
The inherited `PlayerControllerAi.cheatShuffle` hook is overridden
to return the **already Forge-randomized** cards without
delegating to the privileged AI policy. The actor neither
chooses nor observes the actual hidden ordering.

Each real player draws seven cards through
`Player.drawCards(7)`. Two independent persistent Python
subprocess actors receive only the explicitly permitted
objective, own cards and frozen starting decklist multiset,
opponent's legally visible table cards, and both
referee-maintained current life totals.

The referee collects each player's keep/mulligan declaration
**before making changes to either hand**. The actor's
`own_opening_hand` request uses only the same strictly
allowlisted observation and returns a bounded keep decision
and own-hand bottom-card index priority. For a mulligan, the
referee uses Forge GameAction to return the real hand to its
own library, Forge `Player.shuffle`, real
`Player.drawCards(7)`, and Forge
`GameAction.moveToBottomOfLibrary` for the private
mulligan-count number of cards. Count is never sent to actor.
The current September 25, 2026 CR 103.5 specifies bottoming
after each re-draw, which this fixture applies.

A bounded sequence repeats until both players have kept,
then `PhaseHandler.mainLoopStep` invokes a genuine
separate-process priority decision. No test-only
`devModeSet` is used during the opening/mulligan or after
returning to priority; `AITest`-style Game fixture
**initializes MAIN1 upfront**, which is a limitation.

New successful TestNG cases:

1. `completeRefereePregameShufflesDrawsAndStartsRealPriorityForTwoActors`
   checks private Forge shuffling, draw seven, decisions,
   inventory conservation, final hand size
   `7 - mulligans`, active process callbacks and genuine
   first priority.
2. `twoPlayerPregameNeverTransfersHiddenMulliganOrShuffleInformation`
   confirms only four approved top-level observation keys,
   no mulligan counts, shuffle/library order/unknown top,
   turn/phase/priority/stack or opponent hidden-hand data,
   and that real opposing player shuffling does not change
   the other actor's observation.

All 33 previously verified Stage9W real Forge tests also
passed, including casting, mana, basic combat attacks,
registered counts, existing callback denials and own-hand
opening choice boundary.

## What this is NOT

- This is a bounded **test-fixture referee pregame implementation**,
  **not** complete production `Game.startGame` integration.
  Forge's real match initializer, true starting player
  election, pre-start triggers, draw-step exceptions,
  sideboarding and all possible opening-hand modifiers
  remain outside the test scope.
- The test fixture uses 20-card synthetic registered
  decks, not sanctioned Constructed deck minimum sizes.
  This establishes consistency/security, not format legality.
- The ref-moved real cards, and two separate worker
  processes, do not establish hardened OS sandboxing.
- Inherited full-information AI callbacks still exist
  outside the explicitly guarded families; no comprehensive
  secrecy proof is claimed.
- Actor strategy is a simple deterministic heuristic,
  not expert AI. General spells, arbitrary cost/target
  selection and full combat/blocking remain unfinished.
- Stock Forge life-loss game-over rules were used; the
  user's custom negative-life / highest-life-at-library-
  exhaustion conditions are not yet integrated.
- No main merge, live gameplay deployment, training,
  evaluation seeds or promotion was performed.

## Intermediate CI

First run `38146922926` executed all **35/35**
Maven tests green but failed an independent XML
checker due to a mismatched Java class name.
Corrected in commit `7961a01465037b838fbf9266e0a2731a372b4fa3`;
full rerun `38147078537` succeeded with both
Forge and evidence gate PASS.

## Next: Stage 9Y

Integrate the own-hand opening/mulligan projection into an
authentic `Game.startGame` initialization path and verify
the starting-player and first draw exception, while refusing
any non-approved actor observations. Harden two independent
AI processes beyond `python3 -I -S` with isolated
working directories, bounded I/O and trusted referee-only
pipes; validate no cross-actor filesystem/network/hidden
referee access. Continue auditing inherited
`PlayerControllerAi` callbacks, and expand blocked combat
and legal targeting only with explicit referee approval.

References:
- Magic 2026-09-25 Comprehensive Rules, 103.5:
  https://magic.wizards.com/en/rules
- Forge pinned upstream code:
  https://github.com/Card-Forge/forge/tree/forge-2.0.15
- ISMCTS:
  https://eprints.whiterose.ac.uk/id/eprint/75048/
