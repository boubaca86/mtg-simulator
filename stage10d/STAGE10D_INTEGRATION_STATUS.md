# Stage 10D — Research-first bounded multi-block implementation (UNVERIFIED)

Research basis:
- Wizards of the Coast, Magic Comprehensive Rules effective 2026-09-25:
  https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt
  CR 509.1a-b (untapped creature, blocking restrictions), 509.1c (requirements),
  509.1d-e (block costs), and 510.1a-e/510.2 (assignment and simultaneous damage).
- Card-Forge/forge tag forge-2.0.15 **peeled SHA**
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`:
  `CombatUtil.canBlock`, `CombatUtil.validateBlocks`,
  `CombatUtil.getBlockCost`, and `PhaseHandler.declareBlockersTurnBasedAction`.
- Cowling / Powley / Whitehouse (2012), *Information Set Monte Carlo Tree
  Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/
  Keep all decisions restricted to the actual actor observation.

Architecture held: two separate persistent external Python actor processes;
the single trusted Forge referee alone knows and applies combat/priority/stack/
turn/mana/life and hidden order. Actors receive only objective, own hand/table,
own original registered deck-name/count multiset, visible opponent table, and
referee-owned current dual life totals. They never receive phase, turn,
priority, stack, hidden original, opponent private hand/deck, or library order.

Current branch `expert-stage9a-forge-decision-pairs` stays **UNMERGED**.

Current files and intent:
- `stage10d/multi_block_actor.py`: bounded one-to-one *candidate* pairs
  from visible own Grizzly Bears and visible opponent Grizzly Bears.
- `forge_tests/Stage10DMultiBlockRestrictionsTest.java`: 49 named
  TestNG methods (46 inherited from verified Stage10C plus three Stage10D
  restrictions: two native one-to-one blocks, flying, tapped/nonattacking).
- `forge_tests/verify_stage10d_surefire.py`: 49 exact-method strict
  independent XML gate, no failure/error/skip accepted.
- `.github/workflows/stage10d-multi-block-restrictions.yml`:
  exact pinned Forge SHA, genuine Maven TestNG, strict XML gate, logs/
  hashes/artifact, plus Python protocol canary.
- `stage10d/test_protocol.py`: execute two persistent independent workers,
  verify exact five-key action-plan, own-visible indexed pairs, hidden
  face-down filtering, own-opening-hand protocol, and rejection of
  privileged extra fields.

Safety refinement: `twoIndependentAttackersAreBlockedOneToOneAndAllFourBearsDie`
now requires **two blocks accepted in the same native declaration callback**.
A cumulative count spanning two combats is not proof of the intended behavior.

## Verification truth boundary

The last independently confirmed pinned Forge milestone is Stage10C
46/46 TestNG, zero failures/errors/skips, XML gate PASS:
https://github.com/boubaca86/mtg-simulator/actions/runs/38159095903

Stage10D GitHub source and workflow have been committed, but as of this
status document **the Stage10D 49/49 Maven run, independent Surefire
XML gate, source hashes, and artifact have NOT been inspected and
verified**. Do NOT claim Stage10D complete or `full_magic_game:true`.
GitHub connector's `fetch_commit_workflow_runs` returned no runs even for
the known-successful Stage10C push commit, so a zero result there is not
evidence that Stage10D CI failed to start.

Boundaries still open: no hardened OS sandbox; inherited PlayerControllerAi
callback audit incomplete; conditional block costs/mandatory/evasion cases
beyond these tests, multiple blockers per attacker and damage splits,
Stage9E stale exile capabilities/object identity/cost rollback/hidden top,
and user's nonstandard Ultimatus negative-life/library-exhaustion scoring.
No merge, deployment, training, protected evaluation seeds or promotion.
