# Stage 10R — VERIFIED: two distinct Forge-native paid-creature combats

## CI evidence and software identity

- **Exact tested software commit:** `30ebda6cd7c46982ec8e266f3bf11f7e341e8645` on UNMERGED `expert-stage9a-forge-decision-pairs`.
- **Completed successful PUSH GitHub Actions run:** https://github.com/boubaca86/mtg-simulator/actions/runs/38194501517; head SHA matches exact tested software commit.
- **Inspected real job:** `114634094854` — Maven exit **0**, **BUILD SUCCESS**, genuine pinned Forge TestNG `forge.ai.Stage10RMultiCombatVictoryTest` **76 tests run, zero failures, zero errors, zero skips**.
- **Independent exact 76 TestNG method Surefire XML verifier:** `{"executed_methods":76,"full_magic_game":false,"gate":"PASS",...}`.
- **Stage10D/E IPC:** PASS, separate persistent actor processes, strict privacy allowlist; **Stage10H** four actor types strict JSON PASS with 56 malformed negatives.
- **Real pinned Card-Forge/forge release:** annotated `forge-2.0.15` peeled and tested exact SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- **Uploaded artifact:** `11722313045`, `stage10r-multi-combat-victory-38194501517-1`, **82,926 bytes**, with source hashes, Maven log, test XML and gate evidence.
- All **74** previous Stage10Q TestNG tests retained and passed. New Stage10R methods: 2. No AI training, deployment, promotion or merge.

## One increment: cumulative damage on TWO distinct genuine turns

Stage10R uses two independent persistent external Python actor OS processes and one authoritative Forge referee. It runs a real registered sixty-card Constructed match, native Match.startGame / mulligans and bounded native mainLoopStep turns. Actors receive only objective, own hand, frozen original deck name counts (no library order/top), own/opponent visible tables and Forge-maintained public life totals. Referee exclusively decides legal lands, paid casting, stack, priority, combat, damage and outcome.

This test deliberately isolates multi-round combat with 60-card legal **test-only** registered deck fixture of attacker **39 Forest, 20 Island, one Grizzly Bears** and opponent **60 Islands**, and a **test-only curated own hand** of two registered Forests plus the registered Bear. Critically NO attacking creature is artificially placed onto battlefield. A live external actor must propose and Forge must mana-pay the real Bear spell on the actual stack and resolve it; sickness, attack opportunities, damage and game end happen across natural turns.

- `nativeTwoSeparatePaidBearCombatsCauseCumulativeForgeVictory`: starting at referee-set **4** defender life, one legal 2/2 attacker connects for **4 → 2 → 0** on **two different real Forge turn numbers**. Next native state-based-action check yields Game.isGameOver, defender LifeReachedZero loss, registered correct winning actor.
- `nativeTwoSeparatePaidBearCombatsLeaveOneLifeWithoutFalseWinner`: starting at test-only **5** defender life, legal consecutive-turn damage produces **5 → 3 → 1**, remains in game without fabricated winner or GameOutcome.

Two real distinct damage events are asserted, with 2 exact damage each, cost-paid stack-to-board progression, original registered physical-card conservation, active actor PID independence, public current life, and no leaked secret observation fields.

## External research and limitations

- Wizards [Magic Comprehensive Rules effective September 25, 2026](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt), notably CR 400.2 hidden zones, 302.6 summoning sickness, 509–510 combat, 702.7 first strike, and 704.5a zero life loss.
- Cowling / Powley / Whitehouse (2012), *Information Set Monte Carlo Tree Search*, https://doi.org/10.1109/TCIAIG.2012.2200894, preserving restricted imperfect-information observations.
- Genuine functional two-turn cumulative damage is proven **only for these deliberately constrained registered-deck / opening-hand / low-life fixtures**. NOT evidence that arbitrary natural 20-life games, all cards, realistic strong opponents, or custom Ultimatus games are supported. Actors are separate PIDs but not an OS sandbox. Never use protected seeds 20261042–49 or 20261050–57.
- The documentation commit written after this verified software SHA is **not** itself an independently tested software commit.
