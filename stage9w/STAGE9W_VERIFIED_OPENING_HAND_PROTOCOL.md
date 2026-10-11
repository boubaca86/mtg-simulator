# Stage 9W — Referee-mediated own-hand opening decision protocol

**VERIFIED pinned Forge 2.0.15, October 11, 2026:** [GitHub Actions run 38146405663](https://github.com/boubaca86/mtg-simulator/actions/runs/38146405663), source SHA `69676a31c323beba77d697e2e31251bde2dd79fd`, unmerged development branch `expert-stage9a-forge-decision-pairs`.

Actual Java/TestNG and independent Surefire XML verifier both **PASS**:
**33 executed, 33 passed, 0 failed, 0 errors, 0 skipped**; Maven BUILD SUCCESS exit 0.
Immutable runner artifact: `stage9w-opening-hand-protocol-38146405663-1`, ID `11705235985`.
Pinned upstream `Card-Forge/forge` tag `forge-2.0.15` peeled
SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.

Sources:
- `stage9w/opening_hand_actor.py`: single persistent restricted external policy that supports both existing combat/priority decisions and a new opening-hand request, with no additional observation/game fields.
- `forge_tests/Stage9WOpeningHandProtocolTest.java`: same two independently persistent process-backed Forge player-controller callbacks as Stage9V, plus gated `mulliganKeepHand` and `tuckCardsViaMulligan` callbacks and four new real Forge tests.
- `forge_tests/verify_stage9w_surefire.py`: independent strict 33-name executed/unskipped XML gate.
- `.github/workflows/stage9w-opening-hand-protocol.yml`: read-only pinned Forge CI and source-hash evidence.

## Narrow protocol and security contract

Actor observations are **unchanged**:
`objective`; `own_cards.hand`, frozen `own_cards.decklist_multiset`
and own visible table cards; legally visible `opponent_table`; and
both authoritative `life_totals`. No shuffle order, unknown library
top, opponent hand/deck, turn, priority, stack, other public zones,
actual `Card` references, or referee internals.

The new `own_opening_hand` message requests an action type; this is
NOT an additional game-state observation field. It encloses the
exact same allowed observation object. One persistent worker for
each player returns only
`{intent:'opening_hand',keep:boolean,bottom_priority:[permutation of own hand indices]}`.

The trusted referee validates an exact schema, proper boolean, complete
unique canonical nonnegative own-hand index permutation, and size bounds.
The actual count to bottom is retained by the referee, never taken from
the AI. Forge/Java maps positions only to the *current owned* hand
`Card` objects and returns `CardCollectionView` for normal
Forge card movements; AI never passes actual `Card` objects or
moves life/library cards itself. Stale or borrowed hand collections,
out-of-bounds counts, invalid/duplicate indexes, forged fields or
noncanonical numeric values fail closed.

Both callbacks are **disabled by default**: they still throw
`SecurityException` exactly like Stage9V until the trusted referee
explicitly enables a narrow own-hand opening fixture. All earlier
inherited full-information callback protections remain active.

## Test results

Four new actual Forge tests (29 previous Stage9V tests still green):

1. Different persistent subprocess players independently choose
   keep/mulligan from their respective seven-card visible own hands
   without seeing each other's hand.
2. Referee controls the bottom-card count and selects only actual
   current owned hand cards; the controller's decision alone does
   not change hand, stack, life or library.
3. Duplicate/out-of-range/fractional/forged JSON card positions
   and life-change commands are rejected before any state mutation.
4. Across changing opponent hidden hands and ordered libraries,
   actor observations remain byte-identical and the own-opening
   decision stays identical; no unauthorized IPC calls.

## Explicit remaining work

- This is a **real process-backed callback integration canary**, not
  a fully executed opening-hand/mulligan game initialization. The
  tests manually supply seven-card hands; they do not demonstrate
  authentic shuffle, draw seven, iterative London mulligans,
  simultaneous selection, moving the chosen cards to bottom and
  natural first-turn transition.
- The opening policy (keep with 2–5 basic lands, otherwise mulligan)
  is deliberately simple; not an expert AI strategy. It uses only
  literal names already present in its own hand and no global card
  database or opponent cards.
- Forge `PlayerControllerAi` inheritance still contains other
  data-sensitive, unreviewed callbacks. Process separation is not
  yet hardened OS network/filesystem/debugger isolation.
- Unresolved concealed-object grants/aliasing, re-exile identity,
  full cost rollback, random choice, and the user's custom
  negative-life/library-exhaustion rules remain outstanding.
- No production code deployment, main merge, policy training,
  reserved evaluation seeds or promotion.

## Next (Stage9X)

Integrate this same validated opening-hand protocol into a complete
Forge pregame fixture: load registered physical decks, referee-only
shuffle, draw seven to each player, perform iterative legal London
mulligan decisions, enforce bottom counts and zones, then use genuine
Forge first-turn priority/draw/land/creature callbacks. Use separate
private-process projections throughout and independently verify
real Forge rules with a strict CI gate. Avoid giving the actor
mulligan counters, opponent actions, exact library positions
or other extra information without user approval. Continue auditing
inherited non-priority callbacks and hardening OS isolation.

Research: [Wizards official rules](https://magic.wizards.com/en/rules)
CR 103.5; [Forge pinned source](https://github.com/Card-Forge/forge/tree/forge-2.0.15);
[Cowling et al. 2012, ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
