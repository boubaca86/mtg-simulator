# Stage 9K — Genuine Forge priority/turn loop with two isolated actors

**VERIFIED LIVE in pinned Forge 2.0.15:** [GitHub Actions run 38134726473](https://github.com/boubaca86/mtg-simulator/actions/runs/38134726473)
completed **SUCCESS** at feature SHA `4b34d083df868c02209bfc0105c77872298bf760`.

Maven exit **0**, **BUILD SUCCESS**, TestNG **3 run / 0 failed / 0 errors / 0 skipped**, independently parsed Surefire XML gate **PASS**.

Evidence artifact `stage9k-forge-priority-loop-38134726473-1` (id `11700721746`), containing Maven log, test XML, gate JSON, pinned hashes.

## Executed code

- Test-only Java class `forge_tests/IsolatedForgePriorityLoopTest.java` installs separate `PlayerControllerAi` subclasses for Forge's **actual two players** with `Player.dangerouslySetController` in the isolated fixture.
- Each controller overrides `chooseSpellAbilityToPlay()` to send the actor only allowed data (objective, own hand, known own decklist multiset, own/visible opponent table cards, authoritative dual life totals); each response comes from an independent persistent `python3 -I -S -u` process running `stage9h/ai_actor.py`. Actor response is checked for process PID, byte-level observation SHA-256, allowed command (`pass` only), and life totals.
- The controller returns `null` for a genuine passed priority; `PhaseHandler.mainLoopStep()` retains all rules, state-based checks, stack, phase and turn logic. Neither actor receives turn/phase/priority/stack details.
- Case 1: first player passes, MAIN1 must remain open; second player passes, genuine Forge closes MAIN1 and advances. Two actual separate subprocess callbacks proved.
- Case 2: a bounded genuine Forge mainLoopStep sequence traverses later phases and **switches turn without test `devModeSet`**, then Forge `onPhaseBegin(DRAW)` itself draws from its next active player's authentic library into the corresponding hidden hand. No synthetic manual `DrawEffect` for this test.
- Case 3: modifying actual opponent Forge hand and library leaves actor serialized input unchanged, and no phase/priority/turn/stack or opponent concealed card names are transmitted.
- Initial setup uses Forge's preexisting `AITest.initAndCreateGame()` fixture, which initializes MAIN1 with `devModeSet` ONCE. Subsequent turn progression is natural Forge gameplay and **no devModeSet calls appear in Stage9K tests**.

## Limits and next work

- **Pass-only** external actor: Stage9I/9J's genuine land selection has not yet been integrated with this authentic priority callback; the earlier tests were referee-controlled fixtures.
- This does not prove complete legal spell casting, costs, targets, priority/stack responses to nonempty spells, combat AI, full game termination, adversarial information noninterference or secure operating-system sandboxing.
- No production patch, merge, evaluation seeds, ML training or promotion.
- Stage9L priority: enable a permitted `play_land` request in the real priority callback, map the index to a legal current Forge `SpellAbility` **inside the trusted referee** and return it to Forge's normal action pathway, verify one land-per-turn, no out-of-turn plays, next-turn reset and no hidden-zone leakage. Add strict pinned Forge CI. After that, extend to legal spells/mana/costs/targets, without broadening actor-visible fields.

Research consulted: [Wizards official MTG rules](https://magic.wizards.com/en/rules), September 25 2026 CR 117, 305, 500 and 504; [Card-Forge/forge tag forge-2.0.15](https://github.com/Card-Forge/forge/tree/forge-2.0.15) exact commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`; [Cowling, Powley & Whitehouse 2012 ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
