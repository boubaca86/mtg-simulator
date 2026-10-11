# Stage 9Y — First-turn and mulligan integration research

Status: RESEARCH ONLY; no new Forge test has passed.

The 2026-09-25 Magic Comprehensive Rules CR 103.5 requires starting-player-first mulligan declarations, all declarations before mulligans happen, draw a fresh starting-size hand, and bottom cards equal to counted mulligans. CR 103.8a requires the starting player in a two-player game to skip the first draw step.

Pinned upstream Forge 2.0.15 SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` implements its actual opening via `Match.startGame -> GameAction.startGame -> MulliganService.perform -> PhaseHandler.startFirstTurn`. `PhaseHandler.setupFirstTurn` is a public setup path that requires a null phase; its `isSkippingPhase(DRAW)` checks turn 1 and exactly two players. The existing Stage9X fixture uses `devModeSet(MAIN1)` up front and therefore cannot itself verify this first-turn draw exception. Forge's `LondonMulligan` and `AbstractMulligan` handle counted bottom cards in the pinned version. Avoid substituting a simulated skip for actual PhaseHandler behavior.

Privacy contract remains: no turn, phase, priority, stack, mulligan count, shuffle order, library top, opponent hidden hand/deck or referee internals in actor observations. Two distinct persistent actor processes and referee-only actual life, deck ownership and moves.

The next dynamic test must use a *fresh* Game without `devModeSet`, exercise `setupFirstTurn` and real `mainLoopStep`, prove first player draws zero and second draws one, and independently run all 35 inherited Stage9X tests. Do not label this Stage9Y verified without pinned CI and strict XML evidence.

Research: https://magic.wizards.com/en/rules ; https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676 ; https://eprints.whiterose.ac.uk/id/eprint/75048/ .
