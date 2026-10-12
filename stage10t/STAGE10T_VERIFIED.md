# Stage 10T — VERIFIED: two genuine mana-paid creatures and a legal block

## Proven successful push CI

- **Exact tested SOFTWARE commit:** `baf52b9d0952c0531ccfc990c7d560691cf2e7f0` on unmerged branch `expert-stage9a-forge-decision-pairs`.
- **Actual successful push CI:** https://github.com/boubaca86/mtg-simulator/actions/runs/38234044658 — `conclusion=success`; SHA matches tested software.
- **Independently inspected job:** `114760218503`.
- **Forge pinned release:** `forge-2.0.15`, exact peeled commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` checked in CI.
- **Maven exit:** `0`, **BUILD SUCCESS**.
- **Real TestNG** `forge.ai.Stage10TTwoPaidCombatantsTest`: **80 executed, 0 failures, 0 errors, 0 skipped**. All 78 inherited Stage10S tests included and passed.
- **Independent exact test names Surefire XML gate:** `{"executed_methods":80,"full_magic_game":false,"gate":"PASS",...}`.
- **Stage10D/E:** independent two-PID actor IPC and privacy PASS; **Stage10H:** strict JSON four actor types, 56 malformed negatives PASS.
- **Evidence artifact:** ID `11739822015`; `stage10t-two-paid-combatants-38234044658-1`, 84,173 bytes, includes saved Maven log, XML, hashes and gate output.

## What new functionality was exercised

Two new independent genuine Forge TestNG methods:
1. `nativeBothManaPaidCombatantsBlockWhenAttackerRegisteredFirst`.
2. `nativeBothManaPaidCombatantsBlockWhenDefenderRegisteredFirst`.

Each runs real `Match.startGame` for two **60-card legal registered Constructed** lists with separate persistent external Python actor OS processes attached to one Forge authoritative referee. Native shuffles/mulligans, private hands, legal lands, true mana-paid casting and stack resolution, summoning sickness, actual attacks and validated blocks, combat damage, and real zone movements are exercised. Both **Grizzly Bears** are originally registered physical cards **in their OWN private hands** and must be paid for by real Forests and resolved on the Forge stack before combat. The defender actor must propose a legal blocking pair through the restricted IPC. Both Bears die in genuine 2/2 versus 2/2 combat and enter their owners' graveyards. Both native starting life totals remain 20, and no winner is fabricated. The two methods swap player registration positions to check role symmetry.

**Test-only limits:** both original own hands are curated from their respective already registered lists after real native opening (2 Forests and 1 Grizzly Bears per actor); no actor sees opponent private hand or library, and no battlefield creature is preseeded. The attacker list contains four Llanowar Elves as a frozen deck-composition signal for the approved simple attack preference; no omniscient decision-maker. This establishes a more realistic opponent than Stage10S's 60-Island nonblocking opponent but does NOT establish uncurated naturally sampled opening hands, arbitrary card support, full real-world MTG/Ultimatus matches or AI skill. Separate PIDs are not a hardened OS sandbox.

**First Stage10T CI run failed**: https://github.com/boubaca86/mtg-simulator/actions/runs/38233710210 — 78 inherited passed, two new cases failed only the **test-only inventory helper** because it omitted real graveyard cards after both Bears died. Replaced post-combat `countInventory` with inherited `fullPhysicalInventory` (library+hand+battlefield+graveyard, respecting ownership), never patched Forge behavior. Exact subsequent tested software SHA `baf52b9d...` passed 80/80.

## Research and boundaries

- [WotC comprehensive rules](https://magic.wizards.com/en/rules), current revision as of 2026-09-25: legal hidden/public card information, creature play/tap/sickness, blocks, combat damage and state-based rules. Relevant CR 302.6, 400.2, 509–510; 104.3b and 119.6 for loss at zero life. Avoid obsolete citation that maps 704.5a to zero life.
- Cowling/Powley/Whitehouse (2012), [Information Set Monte Carlo Tree Search](https://doi.org/10.1109/TCIAIG.2012.2200894): model each actor's bounded information set, not a privileged copied Forge Game.
- **No training, deployment, promotion or merge.** Protected Stage8P evaluation seeds 20261042–49 and 20261050–57 not used.

This documentation follow-up is NOT an independently tested software SHA.
