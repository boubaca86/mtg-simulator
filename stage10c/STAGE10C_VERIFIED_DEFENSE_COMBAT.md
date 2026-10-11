# Stage 10C — Referee-validated two-way combat and restricted blocker preferences

**VERIFIED October 11, 2026** in real pinned Forge:
https://github.com/boubaca86/mtg-simulator/actions/runs/38159095903

- Unmerged feature branch `expert-stage9a-forge-decision-pairs`; tested head `e83ec5378630c5e21a2ee6664ed31e2f1877e384`.
- Upstream `Card-Forge/forge` tag `forge-2.0.15` checked exact SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Maven **BUILD SUCCESS**, exit 0; **46 actual TestNG methods executed, 46 passed, 0 failures/errors/skips**.
- Independent exact-method Surefire XML evidence gate: **PASS**, explicitly `full_magic_game:false`.
- Archived artifact `stage10c-referee-blocking-38159095903-1`, ID `11709583605`, including source hashes, XML and Maven log.
- Files: `stage10c/defense_plan_actor.py`, `forge_tests/Stage10CDefenseCombatTest.java`, `forge_tests/verify_stage10c_surefire.py`, `.github/workflows/stage10c-referee-blocking.yml`.

## New functionality

The second test-only AI policy makes a **phase-independent** preference plan using strictly its already-permitted observation of its own cards, its own immutable registered deck-name count multiset, legally visible opponent battlefield/table cards, and the two current referee-owned life totals. No combat attacker list, turn/phase/priority, stack, hidden card identities/order, internal Forge objects or arbitrary other public zones are transmitted.

The AI may propose `block_pairs: [[own_battlefield_index,opponent_visible_battlefield_index],...]` in addition to the previously supported land/creature/attacker candidates. It does not determine actual block legality or move any cards. One test-only own-deck-dependent policy has an attacking profile; the other maintains creatures defensively. This is explicitly a narrow heuristic, NOT general expert strategy.

The trusted Forge referee requires exact JSON keys, canonical nonnegative indices, unique bounded pair entries, current controlled and face-up **Grizzly Bears** cards in both players' current actual battlefields, and a real attacker already declared in Forge `Combat` before applying `CombatUtil.canBlock(attacker,blocker,combat)` and any block-cost guard. It calls genuine `Combat.addBlocker` and checks `CombatUtil.validateBlocks`, reverting disallowed assignments. The actor has no access to the referee's private `Combat` object.

The defensive controller's `declareBlockers` callback consults a **separate persistent Python subprocess**, not the inherited omniscient `PlayerControllerAi`. Previous attacker/caster/land/referee guards are preserved. The earlier 44 Forge regression tests remain green, with legacy Stage10B action plans accepted while the new blocker-enabled plan uses one extra candidate field.

## New real Forge test outcomes

1. `independentDefenderBlocksRealAttackerAndForgeDestroysBothBears`:
   native 60-card `Match.startGame` and own-hand-only London mulligans run with two independent process-backed AIs. The **trusted referee deliberately moves one physical registered Grizzly Bears from each library to its owner's battlefield** to make an unambiguous defensive encounter (not a natural-shuffle probability claim). After this controlled initial fixture, only genuine `PhaseHandler.mainLoopStep` advances the game. The attacking-profile AI proposes a legal attack; the defending AI proposes a legal block; Forge resolves actual 2/2 creature-versus-creature damage, moving **both Bears into their owners' graveyards** while leaving both life totals unchanged. Inventory counts including graveyards reconcile exactly to each frozen registered 60-card deck.
2. `blockerPreferenceRejectsForgedPairsAndDoesNotRevealCombatState`:
   duplicate pairs, negative/fractional card indices and extra spoofed phase/life commands are rejected. The authorized two-AI observation retains its exact four-field allowlist and never includes turn/phase/stack/combat/priority.

This follows Magic CR **509.1a–c**: defenders select creatures they control to block real attackers, obeying restrictions/requirements. Research: [Wizards Comprehensive Rules](https://magic.wizards.com/en/rules); [Forge tag 2.0.15](https://github.com/Card-Forge/forge/tree/forge-2.0.15); [ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).

## Remaining limitations and recommended Stage 10D

- **Not a complete AI-versus-AI match.** The 60-card real native match fixture deliberately stops the unlimited Forge main game loop and advances bounded phases. No general target selection, strategic opponent modeling, planeswalker/multiplayer block logic, optional block costs, multi-block assignment, general card/ability support or complete victory/negative-life custom format.
- **Controlled battlefield setup** after match start, not unbiased random natural draws. No policy training or reserved evaluation seeds used.
- Two Python worker processes are distinct but not hardened OS sandboxes. Many inherited PlayerControllerAi callbacks and Stage9E concealed-object/stale authorization/re-exile 406.7/cost rollback/RNG problems remain open. Do not claim comprehensive information-flow security.
- **Stage 10D proposal**: expand controlled real multi-creature blocking (two attackers / multiple block options, illegal tapped/flying/mandatory constraints) with strictly own-table/opponent-visible-table proposals and test legal restrictions/cost choices. Alternatively prioritize OS sandboxing and inherited AI callback inventory to improve confidentiality before arbitrary cards. Maintain exact actor view constraints, pinned Forge CI, independently verified XML, and avoid claims of finished games.

No main merge, production deployment, policy training, reserved evaluation seeds or unreviewed patch promotion.
