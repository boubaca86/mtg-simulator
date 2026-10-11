# Stage 9Y — Native Forge mulligans and two-player first-turn draw rule

**VERIFIED in real pinned Forge CI, October 11, 2026.**

- Successful workflow: https://github.com/boubaca86/mtg-simulator/actions/runs/38147836136
- Unmerged branch: `expert-stage9a-forge-decision-pairs`; verified head
  `2121dab076e9bb199a7d4cb8181cf2d352870802`.
- Upstream `Card-Forge/forge` release tag `forge-2.0.15`, exact
  checked commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Maven **BUILD SUCCESS**, exit 0; **38 actual TestNG methods executed,
  38 passed, 0 failed, 0 errors, 0 skipped**.
  Separate exact-method Surefire XML verifier: **PASS**.
  Evidence artifact `stage9y-native-opening-turn-38147836136-1`,
  ID `11705044410`; includes pinned hashes, per-method reports,
  Maven log, verifier gate output.
- Sources: `forge_tests/Stage9YNativeMulliganFirstTurnTest.java`,
  `forge_tests/verify_stage9y_surefire.py`,
  `.github/workflows/stage9y-native-opening-turn.yml`,
  unchanged actor `stage9w/opening_hand_actor.py`.

## What the 3 new real Forge tests demonstrate

1. **Native `MulliganService`, first player = forest actor.**
   The referee creates two real 20-card registered Forge decks, freezes
   own-only starting-deck multiplicities, loads real library cards,
   performs private `Player.shuffle`, draws seven with Forge, and
   delegates own-hand keep or bottom-card ranking decisions to two
   different persistent Python actor processes. The native Forge
   `MulliganService.perform()` selects the `LondonMulligan` implementation
   (the referee pins and restores the test-global choice). Forge owns
   all hand-to-library movement, shuffle, draw and bottom placement.
   Then actual `PhaseHandler.setupFirstTurn(forestActor, null)`
   starts a **real untap/upkeep/draw/MAIN1 sequence**, no test
   `devModeSet`: first player does not draw (rule 103.8a); second
   player draws one on their first turn.
2. **Native `MulliganService`, first player = island actor.**
   Repeats the genuine native phase/draw assertions with players'
   starting positions swapped, proving the first-turn skip is tied
   to the actual starting player, not the actor's identity.
3. **Hidden-information and inventory checks before first turn.**
   The registered-to-physical card counts remain exact after native
   mulligans and Forge shuffles; changing the opponent's concealed
   library order cannot change the other actor's allowlisted
   observation. The `PhaseHandler.phase` remains null through
   native mulligans. Both actors have independently executed real
   callback processes and only allowed observation fields.

The 35 earlier Stage 9X TestNG cases also passed under the new class,
including real priority, land/limited creature casting, basic combat,
registered deck counts, privileged-controller callback guards,
own-hand opening decisions and strict data noninterference.

## User-approved actor observation boundary — unchanged

Each AI receives only its objective, own legitimate cards/hand,
its own immutable registered starting decklist multiset **not ordered
library**, opponent's legally visible battlefield/table cards and
**both referee-owned current life totals**. The referee alone knows
starting player, mulligan counters, priority/phase, both private hands,
libraries/order, costs and all rules. No game phase, turn, stack,
priority, opponent hand, library top/order, internal Forge Card refs,
generic public snapshots or extra secrets are serialized to an AI.
The actor has no direct life mutation ability.

## Important limits

- **This is still a test-only native component integration, not
  `Match.startGame` or `GameAction.startGame` running end to end.**
  The referee explicitly prepares physical libraries/opening draws,
  chooses the starting player for a deterministic test, invokes native
  `MulliganService.perform`, then calls actual
  `PhaseHandler.setupFirstTurn` and `mainLoopStep`.
  Full production match-start orchestration (first-player election,
  opening hand effects, sideboarding, card-specific setup, match-end,
  restarts) remains incomplete.
- Registered fixtures contain **20 cards**, below normal Constructed
  deck minimum. These are narrowly controlled internal rules tests,
  not legal competitive decks or completed matches.
- Inherited `PlayerControllerAi` retains non-approved callbacks;
  unsupported ones are progressively fail-closed, but a general
  least-privilege replacement has not been demonstrated.
- Two separate Python processes are not yet hardened OS sandboxes
  against filesystem/network/debugger access or side channels.
- The policy is still heuristic, not expert. The user's custom
  game-end variant (life can be negative, winner higher life at
  library exhaustion) is not implemented in stock Forge.
- There was no training, evaluation seed use, main merge, code
  promotion or production deployment.

## Audit of first intermediate workflow

Run `38147695603` had all **38/38 Forge tests PASS** but the strict
evidence gate used an incorrect Java class name. The mismatch was fixed
at `2121dab076e9bb199a7d4cb8181cf2d352870802`.
The second run `38147836136` passed both the full Forge tests and
independent XML verification.

## Next milestone: Stage 9Z

Integrate an entirely native **`Match.startGame` /
`GameAction.startGame`** lifecycle, including an explicit safe
starting-player decision and the native pregame turn order, while
preserving the same hard AI observation allowlist. Use real game
constructor / match fixtures with 60-card decks where practical,
bounded lifetime (rather than an uncontrolled full-match loop),
and strict callback tracing. Avoid falsely attributing real
match-start behavior to the current component-level Stage9Y test.

In parallel, prioritize OS-level worker privilege isolation
(network/filesystem/I/O), outstanding hidden Card grant/cost/rollback
issues and complete arbitrary-card support only after a verified
least-privilege referee.

Sources:
- [2026-09-25 Magic CR 103, especially 103.5 and 103.8a](https://magic.wizards.com/en/rules).
- [Pinned Forge source](https://github.com/Card-Forge/forge/tree/forge-2.0.15).
- [Information Set MCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
