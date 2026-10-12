# Stage 10V — VERIFIED: both independent uncurated players pay for and resolve Bears

## Exact inspected evidence

- **Tested software SHA**: `9766fabb4ffb31026f903d2986016c2c99f7e4cd`, unmerged `expert-stage9a-forge-decision-pairs`.
- **Successful exact-SHA PUSH run**: https://github.com/boubaca86/mtg-simulator/actions/runs/38237108252, completed SUCCESS.
- **CI job**: `114770379898`; inspected real job logs. Maven exit **0**, **BUILD SUCCESS**.
- **Pin**: Card-Forge/forge `forge-2.0.15` annotated tag peeled exact `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`, verified in Actions checkout.
- **Genuine Forge TestNG** `forge.ai.Stage10UUncuratedDrawTest` **84 tests run, 0 failures, 0 errors, 0 skips**.
- **Separate exact 84-name Surefire XML verifier**: `{"executed_methods":84,"full_magic_game":false,"gate":"PASS",...}`.
- **Privacy**: Stage10D and 10E two-independent-PID protocol checks PASS. Stage10H strict JSON four actor types, 56 malformed negative cases PASS.
- **Evidence artifact** `11741152458`, `stage10v-natural-both-casts-38237108252-1`, **85,270 bytes**, includes Maven logs, XML, source hashes, verification gate and run SHA.
- Inherited all 82 Stage10U tests and added precisely 2 genuine native Forge TestNG methods:
  1. `nativeBothUncuratedPlayersPayAndResolveBearsLeftRegisteredFirst`
  2. `nativeBothUncuratedPlayersPayAndResolveBearsRightRegisteredFirst`

## Bounded functional progression

Both independently registered sixty-card Constructed decks contain **56 Forest and four Grizzly Bears**. Their native `Match.startGame` genuinely shuffles/starts, uses native London mulligan decisions and draws, and both separate persistent external Python actor processes receive only their own private hand and legal/public observations. No test reorders a library, chooses a starting hand, adds cards to hand/battlefield, or changes native starting 20 life totals. **Each player** must propose and pay for a Grizzly Bears from a genuinely drawn own hand using real Forest mana through genuine Forge casting, priority, stack and resolution. Each player's registered inventory is conserved over library, hand, battlefield and graveyard. The tests exercise BOTH player registration orders, not specific lucky opening shuffles. An authoritative Forge referee alone enforces legality and game state. The independent actors deliberately refrain from attacks so that each side has a reproducible opportunity to cast and resolve a creature before combat.

The Python file `stage10v/visible_opponent_actor.py` was added experimentally but **was not wired into this Stage10V verification workflow**; its behavior is **not** claimed as CI-verified. Future Stage10W can independently exercise that actor, adding actual uncurated-versus-uncurated legal combat.

**NOT proven**: completed realistic contests, legally blocked native combat from the newly uncurated setup, skilled opponent play, arbitrary Magic/Ultimatus cards, or an OS-hardened sandbox. Two distinct Python PIDs are information-limited processes, not complete isolation.

## Relevant research

- Official [Wizards 2026-09-25 Comprehensive Rules TXT](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt): CR 100.2a (minimum 60, four non-basic copies), CR 103 (starting/starting hands), CR 400 (zones/public vs hidden), CR 302.6 (summoning sickness), CR 509 and 510 (blocking/combat).
- Cowling/Powley/Whitehouse (2012), [Information Set Monte Carlo Tree Search](https://doi.org/10.1109/TCIAIG.2012.2200894): preserve uncertainty and independent actor private information sets.
- No model training, deployment, evaluation-seed access, promotion or merge. Never use protected seeds 20261042–49 or 20261050–57.

Documentation-only follow-up SHA is not itself a separately verified software SHA.
