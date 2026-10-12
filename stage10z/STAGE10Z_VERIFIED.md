# Stage 10Z — VERIFIED: both independent native actors contest the board and play to genuine library-depletion game result

## Exact independently inspected push CI evidence

- **Verified tested software SHA:** `416db5e7e33ed565e502014b2cc03018b57944ce`, on **UNMERGED** `expert-stage9a-forge-decision-pairs`.
- **Successful exact-SHA PUSH run:** https://github.com/boubaca86/mtg-simulator/actions/runs/38241310843, completed SUCCESS, attempt 1.
- **Inspected real job:** `114784330257`. Maven exit **0**, **BUILD SUCCESS**, genuine pinned Forge `forge.ai.Stage10UUncuratedDrawTest` **92 TestNG tests run, zero failures, zero errors, zero skips**.
- **Independent exact-name Surefire XML gate:** `{"executed_methods":92,"full_magic_game":false,"gate":"PASS",...}` — all 90 inherited Stage10Y methods plus both Stage10Z methods.
- **Forge pin:** Card-Forge/forge annotated tag `forge-2.0.15` peeled to checked-out commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` in CI.
- **Actor IPC and privacy canaries:** Stage10D PASS, Stage10E PASS; Stage10H strict JSON PASS with four actor types and 56 malformed negatives.
- **Evidence artifact:** https://github.com/boubaca86/mtg-simulator/actions/runs/38241310843/artifacts/11742184154 ; `stage10z-contested-deckout-38241310843-1`, **87,409 bytes**, including Maven logs, individual Surefire XML, source hashes and independent gate outcome.
- No merge, train, deploy, promotion, or protected Stage8P seeds. The follow-up documentation commit is not separately software-tested.

## One concrete functional increment: contested full game with authentic non-life loss

New pinned real Forge methods:
1. `nativeNaturallyDepletedLibraryAfterRealContestedCombatAttackerRegisteredFirst`.
2. `nativeNaturallyDepletedLibraryAfterRealContestedCombatDefenderRegisteredFirst`.

Both genuinely registered 60-card Constructed decks have **56 Forest + 4 Grizzly Bears** and normal 20 starting life; native `Match.startGame` shuffles and applies London mulligans with **no test-prepared hand or battlefield cards**. Exactly two independent persistent external Python actor processes participate. The attacker `stage10v/visible_opponent_actor.py` commits only after a face-up opposing Bear is publicly visible, and the defender `stage10d/multi_block_actor.py` can make Forge-legal blocks. Both sides cast real naturally drawn Bears and pay their own genuine mana through Forge priority/stack; an attacking Bear is legally blocked, and real simultaneous combat damage sends physical Bears to their actual graveyards.

The exact same live native game and actor processes continue through Forge's normal turns and draws without artificial library depletion or life changes. Eventually a player attempts to draw from an empty real registered library and Forge alone records an actual **draw-from-empty-library loss**, recognizes a single winner and records a complete game outcome. Both life totals remain 20. Original entire real-card inventories remain conserved; neither actor received the other's hidden hand/deck or library order/top, referee turn/phase/priority/stack or raw Game/Card references.

This exercises a game-completion rule DIFFERENT from Stage10Y's 20-to-zero combat loss while showing genuine opponent casting and blocking. It is still a deliberately simple two-player environment with a defensive/nonattacking opponent, NOT proof of a skilled or general-purpose competitive AI.

## Next objective and rules research

Priority Stage 11A: complete natural 20-life games in which the opponent also makes **attacking decisions**, and/or games won through real combat damage after meaningful opposing blocks, while preserving two external persistent players, one Forge referee and private information firewall. Avoid repeatedly extending just the IPC protocol.

- Wizards official Comprehensive Rules effective September 25, 2026, https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt . Relevant CR 100.2a, 103, 104.3c, 302.6, 400.2, 509-510 and especially **704.5b** for drawing from an empty library.
- Cowling/Powley/Whitehouse (2012), *Information Set Monte Carlo Tree Search*, https://doi.org/10.1109/TCIAIG.2012.2200894 .
- Never run protected Stage8P evaluation seeds 20261042–20261049 or 20261050–20261057.
