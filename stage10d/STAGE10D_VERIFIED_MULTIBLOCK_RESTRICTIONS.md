# Stage 10D — Multiple blocking assignments, flying evasion and tapped-blocker restrictions

**VERIFIED GREEN on October 11, 2026**:
https://github.com/boubaca86/mtg-simulator/actions/runs/38160611609

- Tested branch `expert-stage9a-forge-decision-pairs`, commit `f911b3f8b609c1f76a1b4639c89b22357b4f85f1`, UNMERGED.
- Exact pinned upstream `Card-Forge/forge` release `forge-2.0.15`, SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Real Forge Maven **BUILD SUCCESS**, exit 0. **49 TestNG methods executed, 49 passed, zero failures/errors/skips**.
- Strict independent XML gate **PASS**, `full_magic_game:false`.
- Artifact `stage10d-multiblock-restrictions-38160611609-1` ID `11710585423`, includes source hashes, pinned hash, Surefire XML, Maven logs and gate.
- Files: `stage10d/multi_block_actor.py`,
  `forge_tests/Stage10DMultiBlockRestrictionsTest.java`,
  `forge_tests/verify_stage10d_surefire.py`,
  `.github/workflows/stage10d-multiblock-restrictions.yml`.

## Verified new behavior

The separate test-only AI policy now ranks **one-to-one** legal-block candidates rather than blindly trying every defender against a single attacker. Its `block_pairs` action preferences refer only to own face-up battlefield Grizzly Bears and opponent face-up battlefield Grizzly Bears, both already permitted information. The actor still has no access to turn, phase, priority, current combat attacker list, stack, hidden identity, library order, arbitrary other zones or authoritative Forge Card objects. The trusted Java referee translates these proposals into actual current-blocking decisions only after `Combat.isAttacking`, ownership, identity, `CombatUtil.canBlock`, blocking cost checks and `CombatUtil.validateBlocks`. Invalid assignments do not become approved blocks.

Three new actual Forge test methods passed alongside the 46 Stage10C and earlier regression tests:

1. `twoIndependentAttackersAreBlockedOneToOneAndAllFourBearsDie`: following bounded native 60-card registered Match.startGame, the trusted referee deliberately stages **two authentic registered Grizzly Bears per side** from each player's physical deck. Natural Forge turns and attacker/blocker declarations produce **two distinct legal blocks**; full actual combat kills all four 2/2 Bears and prevents damage to the defending player. Both deck inventories, including graveyards, remain equal to registered counts.
2. `flyingAttackerCannotBeBlockedByNonflyingBearDuringRealForgeCombat`: one actual registered attacking Bear is given Flying via a genuine Forge changed-keyword effect in the **test fixture** (not a distinct naturally drawn card). Native `CombatUtil.canBlock` rejects a normal ground Bear. Real combat then produces unblocked player damage, with zero illegal blocker assignments. Forge may correctly skip calling `declareBlockers` entirely when nobody can legally block.
3. `tappedAndNonattackingBlockPairsAreRejectedByActualForgeLegality`: the test sets an authentic registered Bear tapped and confirms `CombatUtil.canBlock` rejects it; an untapped Bear is eligible in principle. Before any real attack declaration, the referee rejects a speculative block proposal rather than trusting visible opponent-table identity as proof of an attacker. No life or card inventory is changed.

The two workers remain distinct persistent Python subprocesses, with unchanged four-field game observation. These are narrow rules and security integration checks, **not independently sampled full Magic games**.

## Failed first candidate and resolution

Run `38160445369` executed 49 tests with 48 passes, one failure. The flying case successfully showed actual unblocked damage but incorrectly required a defender blocker callback. Forge legitimately omits that callback if there is no legal blocker. We removed **only that unsupported assertion**, kept the observed damage / no-illegal-block checks and reran everything. The second run `38160611609` is fully green with independently verified exact executed-method XML.

## Limitations and next priority

- The policy is a simple deterministic preference plan for Grizzly Bears and a few basic lands/creatures, not an expert strategic agent or complete Magic engine.
- Controlled staging of battlefield creatures and changed Flying keyword are trusted test fixtures. No win-rate evidence or natural shuffled matchup claim.
- A tapped-blocker legality probe here directly tests a real Forge card and `CombatUtil`, **not an entire natural turn in which a blocker becomes tapped midcombat**.
- No general evasion rules beyond actual Forge checks, multi-block damage assignment strategy, optional mana blocking costs, mandatory blocking or targeted spells are supported by the actor yet.
- AI processes remain separate but not OS-level hardened (filesystem/network/debugger); the test controller still inherits privileged Forge AI callbacks. Stage9E hidden-object permissions/aliases/re-exile 406.7/cost rollback/RNG remain unresolved.
- User's custom negative-life, library-exhaustion and highest-life-wins format remains unimplemented. **No complete match claim**.
- No main merge, production deployment, training or reserved evaluation seeds.

**Stage 10E recommendation:** prioritize a more comprehensive blocking legality canary with native actual Flying/Reach/Menace printed cards and real multi-block assignment/cost handling, and a rigorous least-privilege/process-isolation audit. Then extend to fully native bounded game-end and ultimately the user-specific scoring conditions. All new actions must be referee-validated against current Forge state while the actor observation remains unchanged.

Research: [Official Magic Comprehensive Rules](https://magic.wizards.com/en/rules) (509.1a–d, flying 702.9, menace 702.111), [Forge 2.0.15 pinned](https://github.com/Card-Forge/forge/tree/forge-2.0.15), [ISMCTS research](https://eprints.whiterose.ac.uk/id/eprint/75048/).
