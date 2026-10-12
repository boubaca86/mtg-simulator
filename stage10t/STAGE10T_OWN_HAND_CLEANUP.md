# Stage 10T: own-hand cleanup and an uncurated supported-deck game

Based on verified Stage 10S PR #26 at
`4b950d46ca1d42b179c4319a0a191346e493490d`. This is a stacked change; neither
PR may merge without successful tests and independent review.

## Capability

The restricted controller previously stopped whenever native cleanup required
a discard. A new external worker can rank its own hand. Its request contains
the existing four-field observation inside an `own_hand_order` message envelope;
it receives no cleanup phase, required discard count, maximum hand size, turn,
priority, stack, card object identifiers, or private opponent/library data.
This envelope identifies a preference request, as the existing opening-hand
envelope does. It is not a new game-observation field or a timing side-channel
mitigation. Process isolation remains unhardened.

The reply is exactly `intent: hand_order` and `hand_priority`, a complete bounded
permutation of current own-hand indices. The simple test policy prefers to
discard unsupported spells and surplus lands, retaining two basic lands
(prefer Forest), before discarding useful creatures. It is not an expert policy.

The trusted referee checks active-player cleanup, the exact Forge-computed
excess over maximum hand size, canonical unique indices, and unchanged own-hand
object bindings and observation across IPC. It takes the required prefix of the
preference order. **Only native Forge discards the cards.** There is no inherited
AI fallback. Frames, strict JSON parsing and a shared send/read deadline use the
already-tested Stage 10S helpers.

Eight visibility/finality changes permit test inheritance from Stage 10S without
copying its 4,300-line harness. Its original controller still rejects cleanup;
the new controller is attached only in the new tests. Evidence helpers accept
an explicit class/baseline verifier while preserving their original defaults.

## Required evidence

- All **84** named Forge tests: 78 inherited Stage 10S tests and six new cases.
- Two independent live workers give identical own-hand preferences across
  hidden-world changes; asking for a preference itself changes no cards.
- Rejection of forged/duplicate/noninteger/out-of-range indices, wrong turn,
  phase or count, unlimited-hand-size requests, and stale/foreign card bindings.
- A controlled nine-card fixture progresses naturally from end step through
  cleanup. Forge moves the actor's two selected cards to the graveyard, leaves
  seven cards in hand, conserves inventory and advances to the opponent's turn.
- A separate game uses native registered decks, shuffle, opening draw,
  mulligans, turns and outcome with **no prepared opening hand, manual life
  changes, seeded battlefield, library reordering or selected random seed**.
  The bounded game must finish with the correct native winner and either
  combat loss or ordinary empty-library draw loss; it does not assume which.
- Eight real-process worker tests, five new XML verifier tests and all thirteen
  Stage 10S evidence tests. Existing Stage 10D/E/H privacy/JSON canaries remain.
- The inherited repaired rejection assertions are mutation-tested through
  their declaring Stage 10S class: exactly three intended assertion failures,
  separated from the passing 84-method baseline. Missing execution cannot pass.

Exact Forge SHA: `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`, Java 21.
CI archives XML, logs, source/commit hashes and gate results. Verified run links
and artifact digest are recorded in the pull request after completion.

## Rules and limits

Audited pinned Forge `PhaseHandler` cleanup: it computes the required count,
calls `chooseCardsToDiscardToMaximumHandSize`, discards returned cards, then
handles triggers. The inherited AI callback otherwise calls `brains.getCardsToDiscard`.
The [official rules](https://magic.wizards.com/en/rules), effective September 25,
2026, specify cleanup discards in 514.1 and empty-library draw loss in 704.5b.

The uncurated game still uses the existing deliberately narrow sixty-card
fixtures: one Bear with 39 Forests/20 Islands against 60 Islands. Removing hand
preparation does not prove strong play or arbitrary card/deck support. A native
deck-out is a valid integration result, not evidence of combat superiority.
The earlier 78 tests retain their documented curated fixtures. The nine-card
cleanup test is also explicitly arranged; it is not a naturally drawn-hand claim.

No custom card/deck changes, training, promotion or reserved seed games are
included. Consumed seeds 20261032/20261033 and reserved/candidate families
20261042–20261057 remain untouched. This validates ordinary Magic and does not
implement Ultimatus's negative-life/library-exhaustion scoring.
