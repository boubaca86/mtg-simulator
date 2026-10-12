# Stage 10W — VERIFIED: natural private draws and real 2/2 versus 2/2 blocked combat

## Exact independent push-CI evidence

- **Verified tested software SHA**: `fa13f07937fb43f31c5ac8a94722bcb6e9896aee` on unmerged `expert-stage9a-forge-decision-pairs`.
- **Exact SHA successful PUSH run**: https://github.com/boubaca86/mtg-simulator/actions/runs/38238137938, status `completed`, conclusion `success`, attempt 1.
- **Inspected job** `114773791328`: Maven exit `0`, `BUILD SUCCESS`, genuine `forge.ai.Stage10UUncuratedDrawTest` TestNG `86` executed, `0` failures, `0` errors, `0` skipped.
- **Separate exact-86-method Surefire XML gate**: `{"executed_methods": 86, "full_magic_game": false, "gate": "PASS"}`. No missing/duplicate methods.
- **Pinned Forge** annotated `forge-2.0.15` tag peeled to `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`, checked in successful Actions logs.
- **Stage10D and 10E**: separate persistent actor-process IPC and hidden-information tests PASS. **Stage10H**: four actor types and 56 strict malformed JSON negative cases PASS.
- **Evidence artifact**: https://github.com/boubaca86/mtg-simulator/actions/runs/38238137938/artifacts/11740669753 ; `stage10w-natural-block-combat-38238137938-1`, 85,838 bytes, Maven logs, per-test Surefire XML, source hashes, independent gate.
- No merge, trained model, deployment, promotion or protected evaluation seeds. This docs-only follow-up is not separately software-tested.

## Genuine functional improvement

The new methods are:
1. `nativeUncuratedPaidBearsContestedCombatAttackerRegisteredFirst`
2. `nativeUncuratedPaidBearsContestedCombatDefenderRegisteredFirst`

They inherit the earlier 84 tests and use the real Forge `Match.startGame` London mulligans, private shuffled natural draws, and two actual 60-card Constructed decks (each 56 Forest + 4 Grizzly Bears). Each side uses a separate persistent external Python process, and Forge alone judges actions/costs/priority/stack/combat. No selected opening hand, hand manipulation, board preseed, life adjustment or inspection of opponent private state.

The attacking actor uses the existing `stage10v/visible_opponent_actor.py` and proposes an attack only once the opponent's *publicly visible* Bear appears. The other independent actor uses the existing passive-attacking but blocking-capable `stage10d/multi_block_actor.py`. Each side genuinely pays for and resolves its own Bear from native draws before the attack. Forge checks a legal defender block and actual simultaneous 2/2 damage: both real creatures die into their respective graveyards, both life totals stay 20, and no game outcome is invented. Both registration orders passed; original full physical card inventory is conserved. The action observation retains exactly four authorized fields (objective, own cards, public opponent table, publicly maintained life totals) without hidden hand/deck/order, phase, turn, stack or priority. Two process IDs are not a hardened OS security sandbox.

## Limits and next step

This is deliberately *one bounded naturally drawn creature battle*, not a complete competitive match, advanced opponent strategy, arbitrary spell/card support, or Ultimatus engine. Next functional priority: Stage10X naturally drawn repeatable multi-combat/full match with nontrivial strategic interaction, while preserving two independently restricted actors, one authoritative Forge referee, the four-copy rule and an exact tested-SHA CI+Surefire evidence gate.

## Research

- Official Wizards 2026-09-25 Comprehensive Rules: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt ; CR 100.2a, 103, 302.6, 400.2, 509-510, 104.3b, 119.6, 702.7.
- Cowling, Powley & Whitehouse (2012), Information Set Monte Carlo Tree Search: https://doi.org/10.1109/TCIAIG.2012.2200894.
- Do not use protected Stage8P evaluation seed ranges 20261042-20261049 or 20261050-20261057.
