# Stage 9I — Two isolated actors execute referee-validated Forge land plays

**Status: verified in real pinned Forge CI, October 11, 2026.**

- Source: `expert-stage9a-forge-decision-pairs`, test-run commit
  `822871ab216a55f3a8fd19a58fcf9d487c40e5f5`.
- CI: https://github.com/boubaca86/mtg-simulator/actions/runs/38133762452
- Pinned `Card-Forge/forge` tag `forge-2.0.15`, exact peeled commit
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Maven `BUILD SUCCESS`, **3 executed TestNG methods, 0 failures,
  0 errors, 0 skips**, independent Surefire gate `PASS`.
- Evidence artifact `stage9i-forge-land-actions-38133762452-1`,
  id `11699714600`; includes pinned hashes, Maven log and TestNG XML.

## What happened in actual Forge

The trusted Java referee built a `Game` with two genuine `Player`
objects. Two **different persistent Python actor processes** were launched,
each receiving an allowlisted observation containing only:
its objective, own hand and decklist multiset (not library draw order),
own table cards, visible opponent battlefield cards, and referee-maintained
life totals. No general game-state snapshot, turn, phase, priority, unknown
original identity or opponent hidden-hand data is sent.

The isolated actor policy uses its OWN hand names only, requests
`{"intent":"play_land","hand_index":N}` for a basic land if present, or
`{"intent":"pass"}` otherwise. It has no reference to `Game`, `Player`
or `Card`; this policy is intentionally simple, not an expert AI.

The referee strictly checks message keys, index bounds, actual game
active player, source card still in the actor's actual hand,
`Card.hasPlayableLandFace()` and `Player.canPlayLand(...)`; ONLY after
approval does it invoke actual `Player.playLand(...)`.

Live executed evidence:
1. Player 1's isolated worker selects a **Forest**, and real Forge moves
   it from hand to battlefield, registering one land play this turn.
2. Player 2's separate worker selects **Island**, Forge refuses an
   out-of-turn action, then the referee test changes active player via
   `PhaseHandler.devModeSet` and Forge accepts Island onto battlefield.
3. A second Forest request during the same turn is refused by real
   `Player.canPlayLand`, proving normal once-per-turn limit.
4. Private opponent hand/library changes leave observing actor's
   restricted view unchanged; forged life/extra-data/illegal-index
   intents are rejected.

## Explicit limits

- **This is not a completed Magic match.** Turn transitions are made
  with a TEST FIXTURE `devModeSet`, not the full real priority/stack/turn
  loop. Actors do not cast spells, select targets, pay costs, combat,
  respond at instant speed or execute a strategic search policy.
- Both Python processes still use the same host OS. Process IDs and IPC
  are distinct, but hardened filesystem/memory/network isolation remains
  an outstanding requirement.
- The simple actor has only one proactive real action, a basic-land
  request. The referee decides legality and advances real game state.
- Authorization for observations from arbitrary effects and temporary
  look permissions needs separate reviewed tests. No extra public-zone
  or opponent-hand knowledge has been granted.
- The Stage9A unreviewed visibility-only patch is NOT deployed or merged.

## Next concrete step

Implement a referee-managed **real priority and turn loop** for two
separate actor processes. Add strictly referee-validated legal action
submissions for spells, costs, targets and stack responses, with no
hidden-info leakage, before calling this a complete playable
AI-vs-AI Magic system. Test full matches only with explicitly
permitted non-reserved fixtures and independent CI review.

Neither Stage9H nor Stage9I used reserved evaluation seeds, trained
policies, merged branches or deployed production code.
