# Stage 11A — VERIFIED: symmetric, independent attack-and-block-capable agents play a natural game to a real Forge outcome

## Exact independently inspected push CI

- **Tested software SHA**: `51e585af369e860bc052282256b88c8b3ef33555`, **unmerged** `expert-stage9a-forge-decision-pairs`.
- **Successful exact SHA PUSH run** https://github.com/boubaca86/mtg-simulator/actions/runs/38243663082, completed SUCCESS attempt 1.
- **Inspected job** `114792139206`: Forge Maven exit **0**, **BUILD SUCCESS**, genuine pinned `forge.ai.Stage10UUncuratedDrawTest` TestNG **94 tests run, zero failures, zero errors, zero skipped**.
- **Independent exact name Surefire XML gate**: `{"executed_methods":94,"full_magic_game":false,"gate":"PASS",...}`; all 92 inherited Stage10Z methods plus two Stage11A tests.
- **Pin**: Card-Forge/forge annotated tag `forge-2.0.15` peeled `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`, checked in passing Actions checkout.
- **Stage10D and 10E two persistent Python actor PID/privacy canaries** PASS. **Stage10H strict JSON** four actor types and 56 malformed negatives PASS.
- **Evidence artifact** https://github.com/boubaca86/mtg-simulator/actions/runs/38243663082/artifacts/11743379364 ; `stage11a-reciprocal-38243663082-1` **88,144 bytes**, containing Maven log, per-test XML, source hashes, protocol logs and independent method gate.
- No merge, protected evaluation seeds, training, deployment or promotion. This docs-only follow-up SHA is not independently tested software.

## Exactly one gameplay improvement

New `stage11a/reciprocal_actor.py` runs as a separate persistent external Python process for **BOTH players** with symmetric action plans: genuine own-hand lands/Grizzly Bears casts, own face-up Bear attack candidates without needing an opposing Bear, and own-Bear-to-public-opponent-Bear block preferences. **Only the referee** decides whether those actions are legal in the current phase/turn, handles mana/stack/priority, block legality and damage, and records the outcome.

New genuine TestNG methods:
1. `nativeReciprocalAttackCapablePlayersPlayToActualOutcomeLeftRegisteredFirst`
2. `nativeReciprocalAttackCapablePlayersPlayToActualOutcomeRightRegisteredFirst`

Each test uses two separate persistent external Python actors, native `Match.startGame`, legal original 60-card Constructed decks (56 Forest + 4 Grizzly Bears each), untouched randomly shuffled private opening hands and native London mulligans, normal 20 life and draws, no card/life/battlefield/library fixtures, genuine at least one Forge-authorized native 2/2 attack, and a real Forge-recorded single winner/loser. Test preserves both registered physical card inventories and ensures both players have true native priority decisions. Both registration orders passed.

**Important precision:** Genuine random shuffles permit a native game to END BEFORE one player draws and resolves a creature, even though both independently run the same attacking/blocking-capable policy. Forge may also skip attack/block callbacks when no legal eligible creature exists. The tests therefore DO NOT claim both players actually declared attacks or successfully blocked in each individual Stage11A run; they establish reciprocal **capability/policy**, at least one real authorized attack, and full game termination. Stage10Z separately tests real defender casting and legal blocking across an entire native game. The controls remain independent OS processes with information filtering, not hardened process isolation, expert planning, arbitrary Magic or Ultimatus.

## Next step: Stage 11B

Add a non-flaky, honest test showing **actual attack declarations by both actors during the same naturally shuffled game**, or another functional strategic decision beyond basic Bears with real Forge validation. Avoid staged/curated opening hands, fabricated life changes, random rerolls selecting favorable test outcomes, privileged actor information or claims of full competitive intelligence. Continue incrementally with authentic pinned Forge TestNG, independent exact-name Surefire gate, successful exact software SHA CI, privacy checks, and evidence artifacts.

## Rules and research

- Official WotC Comprehensive Rules https://magic.wizards.com/en/rules ; 2026-09-25 TXT https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt ; particularly 100.2a, 103, 302.6, 400.2, 509–510, 704.5a/b.
- Cowling/Powley/Whitehouse 2012 *Information Set Monte Carlo Tree Search*, DOI https://doi.org/10.1109/TCIAIG.2012.2200894.
- Protected Stage8P evaluation seed ranges 20261042–20261049 and 20261050–20261057 are forbidden.
