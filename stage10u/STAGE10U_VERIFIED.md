# Stage 10U — VERIFIED: fully uncurated private opening hands to genuine paid spell

## Exact independently checked push CI evidence

- **Verified software SHA:** `cbd279839dccff224a4ae519b82c5389988bc7ce` on **UNMERGED** `expert-stage9a-forge-decision-pairs`.
- **Successful exact-software-SHA GitHub Actions push:** https://github.com/boubaca86/mtg-simulator/actions/runs/38235608518, `status=completed`, `conclusion=success`, run attempt 1.
- **Inspected real job:** `114765410510`.
- **Pinned real Forge release:** `forge-2.0.15` annotated tag peeled to exact checked-out commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- **Maven exit:** `0`, **BUILD SUCCESS**.
- **Native Forge TestNG suite:** `forge.ai.Stage10UUncuratedDrawTest` — **82 tests run, 0 failures, 0 errors, 0 skips**; all **80** inherited Stage10T tests passed.
- **Separate exact-name Surefire XML verifier:** `{"executed_methods":82,"full_magic_game":false,"gate":"PASS",...}`.
- **Stage10D and Stage10E:** independent persistent two-PID actor protocol/private observations PASS; **Stage10H:** four actor types and 56 strict malformed-JSON negative cases PASS.
- **Evidence artifact:** ID `11740074598`, name `stage10u-uncurated-draw-38235608518-1`, **84,606 bytes**, saved Maven output, individual Surefire XML results, pinned SHA/source hashes and independent gate result.
- No model training, promotion, merge or deployment.

## What Stage 10U improves

Two newly exercised real Forge TestNG methods:
1. `nativeUncuratedOpeningDrawAndPaidBearWhenAttackerRegisteredFirst`.
2. `nativeUncuratedOpeningDrawAndPaidBearWhenDefenderRegisteredFirst`.

The two separate persistent external Python actor OS processes each receive **only** the permitted public/own information-set projection from the single authoritative Forge referee. Real native `Match.startGame` performs library shuffles, London mulligans, own opening hands and later draws. **Neither player's hand is edited or curated** after opening, and no creature is preseeded on the battlefield. Both start with ordinary 20 life. The registered Constructed lists are attacker **56 Forest + 4 Grizzly Bears** and noninteractive opponent **60 Island** (four-copy creature constraint respected; basic land copies unrestricted). Each test swaps the registration order.

Within a bounded real mainLoopStep sequence, an attacker must receive an original physical registered Bear via their **naturally dealt opening hand or normal subsequent draws**, propose and make a real mana-paid play using Forest lands on the actual Forge stack, and resolve the Bear onto the battlefield. The test does **not** guarantee which turn the Bear appears, and does not pretend the actor can observe library order. All registered cards remain conserved across library, hand, battlefield, and graveyard, actor PIDs stay separate/alive, privacy field allowlist remains exactly four fields, and no fabricated winner is created.

**Limitations:** This tests first real creature casting with genuinely uncurated hands against an intentionally passive nonblocking 60-Island opponent. It does **not** prove arbitrary naturally shuffled competitive full games, arbitrary card legality, completed matches from these specific starts, trained expert AI, unrestricted custom Ultimatus, or a hardened sandbox. The fixed deck composition itself is a test fixture even though hand selection is not.

## Rules and research

- [Official Wizards Magic Comprehensive Rules](https://magic.wizards.com/en/rules), specific September 25 2026 edition reviewed for opening hand/mulligan, public/hidden information, mana-paid spells and native turn progression. Relevant CR 103, 302.6, 400.2, 509–510. Zero life loss described under 104.3b / 119.6, not obsolete 704.5a.
- Cowling / Powley / Whitehouse (2012), [Information Set Monte Carlo Tree Search](https://doi.org/10.1109/TCIAIG.2012.2200894): restricted independent actor information sets rather than hidden referee state.
- Never use protected Stage8P evaluation seeds `20261042–20261049` and `20261050–20261057`.

This documentation-only follow-up SHA does **not** itself have a new validated software test run.
