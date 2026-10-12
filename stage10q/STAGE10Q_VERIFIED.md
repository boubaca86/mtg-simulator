# Stage 10Q — VERIFIED: Native paid creature to genuine Forge outcome

## Software and independent CI evidence

- **Exact tested software SHA:** `8a020236ad3aff8ac6ab2d32cb03c7b115bcb13d`.
- **Unmerged branch:** `expert-stage9a-forge-decision-pairs`.
- **Successful push run:** https://github.com/boubaca86/mtg-simulator/actions/runs/38192653484 — `conclusion=success`, `status=completed`, software `head_sha=8a020236...`, attempt 1.
- **Inspected GitHub Actions job:** `114628531634`.
- **Pinned Forge:** annotated release tag `forge-2.0.15` peeled to exact `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`, checked out and SHA validated in CI.
- **Maven exit:** `0`; **BUILD SUCCESS**.
- **Authentic Forge TestNG:** `forge.ai.Stage10QNativePaidVictoryTest` — **74 tests, 0 failures, 0 errors, 0 skipped**.
- **Independent exact-method Surefire XML gate:** `{"executed_methods":74,"full_magic_game":false,"gate":"PASS",...}`.
- **Privacy checks:** Stage10D two independent actors PASS; Stage10E two independent actors PASS; Stage10H strict JSON canaries with **56 malformed negatives** PASS.
- **Actions artifact ID:** `11722685245`; `stage10q-native-paid-victory-38192653484-1`; **82,180 bytes** (CI evidence: original source hashes, per-method XML, Maven log, gate).
- Inherited all 72 Stage10P genuine Forge TestNG tests.

## Exactly one bounded functional increment

Two new tests in `forge_tests/Stage10QNativePaidVictoryTest.java`:

1. `nativeRegisteredPaidCreatureCastThenNaturalCombatTriggersForgeWin`: two persistent external Python preference-plan actors are attached to **one** authoritative real Forge referee. The real `Match.startGame` creates the registered 60-card game, performs native mulligans and sets up the first turn. A **trusted test-only registered-own-hand arrangement** ensures two Forests plus Grizzly Bears are available, with no preseeded Bears on the battlefield. A trusted **test-only** opponent starting life of **2** allows bounded coverage. Through natural `mainLoopStep` calls, Forge alone enforces legal land play, actual mana cost, stack, creature resolution, summoning sickness, combat damage, and state-based actions, resulting in **Game.isGameOver=true**, genuine **LifeReachedZero** loss and correct winner in GameOutcome.
2. `nativeRegisteredPaidCreatureCastThenNonlethalCombatContinues`: same native setup/casting/combat, but defender starts at test-only **3** life. Forge leaves 1 after damage and **does not** create GameOutcome or a game-over result.

The initial Stage10Q CI run https://github.com/boubaca86/mtg-simulator/actions/runs/38192475439 on software `12c57004083c5d879408db416a2a59b56b7f1f76` was **FAILED 73/74**, at a harness assertion that demanded `isGameOver` immediately after life reached 0 but before the next native state-based-action checkpoint. This was NOT marked verified. Fixed on software `8a020236...`: continue legitimate Forge main-loop priority/state-based-action processing, never call `Game.setGameOver` directly. The exact later successful push run above independently verifies 74/74.

## Rules / epistemic scope

- Official [2026-09-25 MTG Comprehensive Rules](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt): CR 400.2 hidden vs public information; 302.6 sickness; 509–510 legal blocks and combat damage; 702.7 first strike; 704.5a game loss at 0 life.
- Cowling/Powley/Whitehouse (2012), [Information Set Monte Carlo Tree Search](https://doi.org/10.1109/TCIAIG.2012.2200894): distinct imperfect-information actor views are maintained; no opponent hidden library, hand or future draws sent to actors.
- **NOT** proof of full naturally played 20-life games, arbitrary card legality, trained expert AI, custom Ultimatus play, or secure sandboxing. The low starting life and assured registered own-hand selection are **test-only referee fixtures**. Two persistent Python PIDs alone are not sandbox isolation.
- No protected Stage8P seeds used. No model training, deployment, promotion, merge or changed production policies.

The later verification Markdown commit is docs-only and must never be represented as a separately tested software SHA.
