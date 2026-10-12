# Stage 10X — VERIFIED: two successive naturally drawn paid blocking combats

## Exact inspected push-CI evidence

- **Tested software SHA**: `81592305537f7a324573dd4ad3aaadd3d2782c09`, unmerged `expert-stage9a-forge-decision-pairs`.
- **Successful exact-software-SHA PUSH run**: https://github.com/boubaca86/mtg-simulator/actions/runs/38239051908, completed SUCCESS, attempt 1.
- **Inspected job**: `114776820950` returned Maven exit `0`, `BUILD SUCCESS`, and genuine pinned `forge.ai.Stage10UUncuratedDrawTest` TestNG **88 tests run, 0 failures, 0 errors, 0 skipped**, including all 86 inherited Stage10W methods.
- **Independent exact-name Surefire XML method gate**: `{"executed_methods":88,"full_magic_game":false,"gate":"PASS",...}`; all 88 distinct method names match, none skipped/failed.
- **Pinned Forge release**: annotated `Card-Forge/forge` tag `forge-2.0.15` peeled to exact `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` as confirmed by Actions checkout/verification.
- **Privacy/protocol regressions**: Stage10D two persistent actor IPC PASS, Stage10E actor privacy and defense PASS, Stage10H four actor types and 56 malformed negative JSON requests PASS.
- **Evidence artifact**: https://github.com/boubaca86/mtg-simulator/actions/runs/38239051908/artifacts/11742296038 ; `stage10x-repeat-natural-block-38239051908-1`, **86,432 bytes**, contains Maven output, individual Surefire XML, source hashes, per-step evidence and independent gate.
- No branch merge, promotion, model training, deployment, or use of protected Stage8P evaluation seeds. This documentation-only follow-up commit does NOT itself constitute a separately tested software SHA.

## One genuine gameplay increment

Added exactly two Forge-native TestNG methods:
1. `nativeTwoSuccessiveUncuratedBlocksAttackerRegisteredFirst`.
2. `nativeTwoSuccessiveUncuratedBlocksDefenderRegisteredFirst`.

Both 60-card decks have 56 Forest and four Grizzly Bears; first-turn setup, shuffled opening hands, London mulligans, subsequent private draws and regular untap/priority/stack are genuine Forge. Exactly two independent persistent external Python actor OS processes participate, with the attacking actor `stage10v/visible_opponent_actor.py` and blocking defender `stage10d/multi_block_actor.py`; a third trusted Forge referee determines all legal actions and state changes. The AI gets only objective, own hand, publicly visible own/opponent table, frozen own deck multiset, and public life totals — no opponent hidden hand or library, ordered deck, turn/phase/priority/stack, raw Forge objects, or private shared files.

The actors independently cast and pay real mana for an initial Bear, legally attack/block, and BOTH Bears die to native damage into their owners' graveyards. The SAME two processes, decks, life totals and match continue; each draws or uses a second original physical registered Bear, pays for it, and exchanges a SECOND separately legally blocked combat on a LATER Forge turn. Each side ends with two different Bears in the real graveyard; both players stay at 20 life, preserved card inventories include all registered real-card zones, and no fake winner is assigned. Both player registration orders executed and passed under the exact 88-test XML gate.

## Limits and research

This verifies multiple real combat exchanges in a bounded native test, NOT a complete skilled competitive match, autonomous expert search, arbitrary cards, Ultimatus custom rules, or hardened OS sandbox. Next concrete priority: Stage10Y true native full-game natural draw/paid-casting/combat end condition or a meaningful strategic action beyond vanilla 2/2 battles, still without hidden-info leaks or test-fixture cards.

- [Official Magic rules](https://magic.wizards.com/en/rules) and September 25, 2026 Comprehensive Rules (CR 103; 302.6; 400.2; 509–510; 104.3b and 119.6), also https://blogs.magicjudges.org/rules/comprehensive-rules/.
- Cowling, Powley & Whitehouse (2012), Information Set Monte Carlo Tree Search, DOI https://doi.org/10.1109/TCIAIG.2012.2200894.
- Never run protected seed ranges `20261042–20261049` or `20261050–20261057`.
