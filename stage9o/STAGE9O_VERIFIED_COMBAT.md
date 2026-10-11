# Stage 9O — Genuine Forge combat attack with isolated actors

**VERIFIED in independent pinned Forge CI on October 11, 2026.**

- Successful run: https://github.com/boubaca86/mtg-simulator/actions/runs/38136373646
- Tested feature commit: `8dd86e5f14a4d9e0a562a406bf082f4e8fb75e99`
- Pinned Forge tag: `forge-2.0.15`, exact commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- GitHub evidence artifact: `stage9o-forge-combat-attack-38136373646-1`, id `11700964167` (Maven output, Surefire XML, input hashes, independent gate).
- Maven BUILD SUCCESS, exit 0. **16 TestNG methods passed; 0 failures, 0 errors, 0 skips.** Independent `verify_stage9o_surefire.py` gate: **PASS (exact 16 required methods)**.
- An earlier disposable candidate CI run `38136324080` failed at test compilation because `Player.getCardsIn` returns `CardCollectionView`, not `java.util.List<Card>`. It was corrected before the verified run; do not count the failed run as passing.

## What actually ran

Two distinct, persistent external Python actor processes (`stage9o/combat_actor.py`) communicate over separate stdin/stdout streams with test-only Forge Java controllers (`forge_tests/IsolatedForgeCombatAttackTest.java`). Each receives a strict projection: own objective, own hand, a fixed known decklist multiset, own face-up/facedown appearance on battlefield, publicly visible opponent table, and two referee-owned life totals. No turn/phase, priority, stack, real hidden hand/library contents or library order is provided.

The actor may request `{"intent":"attack","table_indices":[...]}` using indices into its **own projected battlefield**. The trusted Java referee alone knows the actual PhaseHandler and Combat instances, translates bounded canonical indices into genuine Forge Card references, checks that the request comes during the attacker's real `COMBAT_DECLARE_ATTACKERS` step, checks controller/identity and `CombatUtil.canAttack`, disallows attack costs in this scoped canary, performs `Combat.addAttacker`, then checks `CombatUtil.validateAttackers`. Invalid composite requests are rejected atomically without tapping or damage. Only the allowlisted vanilla creature `Grizzly Bears` is eligible in this stage.

Natural `PhaseHandler.mainLoopStep` exercises the actual declare-attackers step. Forge taps normal attackers, progresses priority and combat, and applies real damage to opponent `Player.getLife()`. Positive checks: one ready unblocked 2/2 Bear causes life **20 -> 18**, two ready Bears cause **20 -> 16**. Negatives: tapped or summoning-sick Bears cannot attack, mixed legal/illegal requests are rejected atomically, malformed and forged replies are rejected, requests outside real attack timing cannot declare attackers, and changes in opponent private hand/library leave the observation unchanged. All 11 Stage9M regression checks are preserved under the new controller.

## Limits and safety

This is **not a complete Magic game, comprehensive hidden-state firewall or general combat policy**. It currently refuses creatures beyond Grizzly Bears, attack taxes, unusual static attack requirements and other complexities rather than claiming universal correctness. Blocking choices remain a deliberately empty callback only for zero-blocker fixtures. Inherited `PlayerControllerAi` may still handle other non-priority callbacks with full Forge state. The Python processes are separate but lack strong OS filesystem/network isolation.

The fixture's hard-coded `decklist_multiset` does **not** always equal the synthetic Forge library/hand contents. This is an open correctness gap, not a verified invariant. Future work should use a referee-registered starting decklist multiset (without order), test consistency across draw/cast/discard/shuffle, and independently audit non-priority callbacks before expanding unrestricted combat.

Stage9E cross-pile alias, stale permission, re-exile identity, timestamp and concealed originals, complex costs/rollback and hidden library-top risks remain unresolved. No production deployment, PR merge, policy promotion or protected evaluation seeds were used.

## Research before implementation

- [Magic Comprehensive Rules, effective Sept 25 2026](https://magic.wizards.com/en/rules): CR 117, 305, 508, 509, 510, 601.
- Pinned upstream Forge methods: `PhaseHandler.mainLoopStep`, `declareAttackersTurnBasedAction`, `PlayerControllerAi.declareAttackers`, `Combat.initConstraints`, `Combat.addAttacker`, `CombatUtil.canAttack/validateAttackers`, `Combat.assignCombatDamage/dealAssignedDamage`.
- Cowling/Powley/Whitehouse (2012), [Information Set Monte Carlo Tree Search](https://eprints.whiterose.ac.uk/id/eprint/75048/). This stage does not yet implement ISMCTS.

## Next bounded stage

**Stage 9P:** replace hard-coded decklist multiset fixtures with a referee-registered, truthful starting multiset; prove that its card counts stay fixed, own legal draws change only visible hand, shuffled/drawn library order never leaks, and opponent hidden cards do not affect observation. Audit/remove inherited privileged non-priority choices incrementally. Afterward add a restricted actor-requested block/no-block interface with Forge-authoritative validation, combat damage, and precise CI.

Branch `expert-stage9a-forge-decision-pairs` remains intentionally unmerged.
