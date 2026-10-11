# Stage 9S — Ten guarded non-priority Forge AI choice callbacks

**Verified independently against actual pinned Forge 2.0.15 on October 11, 2026.**

## Proven CI outcome

- Successful run: https://github.com/boubaca86/mtg-simulator/actions/runs/38138746803
- Tested feature SHA: `e1e865880e59a35e4c4c6ae9a999a5ad94e1a8a6`
- Pinned Forge version: tag `forge-2.0.15` exact commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Maven `BUILD SUCCESS`, exit `0`. **21/21 actual TestNG methods passed, 0 failed, 0 errored, 0 skipped.**
- Independently parsed strict Surefire XML required-method gate: **PASS**, `executed_methods:21`.
- Archived evidence artifact: `stage9s-guarded-choices-38138746803-1`, ID `11701223759`, with Maven log, individual XML, feature/pinned upstream hashes and gate output.
- One earlier disposable CI run `38138738023` was superseded by the corrected verification gate; count only the successful pinned run.

## What was added

Test-only scoped controller `forge_tests/Stage9SGuardedChoiceTest.java` extends the real registered-main-deck and two-subprocess Forge action canary from Stage 9Q/9R. A strict actor has no access to any Forge `Game`, `Card`, hidden library order, generic public state, turn/phase/priority or opponent hand. The actual Forge referee alone continues to make legality, land/priority, mana/cost, stack, combat and life decisions.

Stage 9R previously blocked these **four** inherited privileged AI choices until approved actor protocols exist:

1. `chooseCardsForEffect`
2. `chooseCardsToDiscardToMaximumHandSize`
3. `chooseStartingHand`
4. `confirmAction`

Stage 9S **adds six more fail-closed overrides** (throwing `SecurityException` before inherited `PlayerControllerAi` can inspect privileged state):

5. `chooseSingleEntityForEffect`
6. `chooseEntitiesForEffect`
7. `chooseSingleSpellForEffect`
8. `chooseSpellAbilitiesForEffect`
9. `choosePermanentsToSacrifice`
10. `choosePermanentsToDestroy`

Two new adversarial TestNG methods individually exercise the additional six choices and assert denied callbacks do not mutate life, hand/table, stack, actor priority or attack callback counters. All nineteen Stage 9R tests (genuine PhaseHandler land, paid creature stack, real combat, actual registered-deck multiset and earlier guard regression coverage) still pass. Worker `stage9o/combat_actor.py` is reused; CI at `.github/workflows/stage9s-guarded-choices.yml`; independent gate `forge_tests/verify_stage9s_surefire.py`.

**Fail-closed does NOT mean generally supported Magic.** Real card effects that require these choices can now be rejected by the test-only controller until an explicit bounded actor intent and referee validation are implemented; no silent privileged decision fallback is allowed for these ten methods. A single stage cannot prove full secrecy.

## Remaining risks and next scoped work

- Many other inherited Forge `PlayerControllerAi` callbacks still involve state-dependent internal AI code, such as `confirmBidAction`, `confirmStaticApplication`, `confirmPayment`, `confirmReplacementEffect`, optional triggers, scry/surveil, delve, opponent discard, non-vanilla damage assignment and mana combinations. Must audit and explicitly authorize, referee-handle, or fail closed individually.
- Stage 9T: add a carefully scoped additional guard test for confirmation/payment/replacement choices; preserve all 21 Stage 9S tests and independent exact-count evidence, then progressively implement legitimate actor protocols for mandatory game effects.
- Two actor Python processes are distinct, not hardened OS sandboxes. File access and process/network isolation still need proof.
- Previously identified Stage9E permission/stale grant, cross-pile alias, re-exile identity/timestamps, concealed-original, true cost rollback, hidden-top issues remain unsolved.
- User's custom Ultimatus negative-life/library-exhaustion rules, unrestricted spells/targets/combat/blocking and strategic ISMCTS are **not** implemented or verified by these canaries.
- Feature branch `expert-stage9a-forge-decision-pairs` remains unmerged. No deployment, training, evaluation seeds or promotion.

## Research anchors

- Official [Comprehensive Rules effective September 25, 2026](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt): sections 103, 117, 305, 401-402, 406, 508-510, 601-608, and effects/choices.
- Pinned [Card-Forge source](https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676), especially `PlayerControllerAi`, `PlayerController`, `PhaseHandler.mainLoopStep` and `CombatUtil`.
- Cowling, Powley and Whitehouse [ISMCTS (2012)](https://eprints.whiterose.ac.uk/id/eprint/75048/) establishes information-set rather than omniscient game-state reasoning as eventual strategy framework.
