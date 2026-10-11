# Stage 9R — Fail-closed guard for four privileged inherited AI callbacks

**VERIFIED October 11, 2026** in independent pinned Forge GitHub Actions:
https://github.com/boubaca86/mtg-simulator/actions/runs/38137583259

- Feature commit tested: `0535c44fb55d17e54a729ef54b22aa531aa8cce7`.
- Upstream `Card-Forge/forge` tag `forge-2.0.15`, exact SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Genuine Forge Maven **BUILD SUCCESS**, exit `0`.
- **19/19 TestNG methods passed**, 0 errors, 0 failures, 0 skips.
- Independent strict `verify_stage9r_surefire.py` method-name/XML gate: **PASS**.
- Artifact: `stage9r-guarded-callbacks-38137583259-1`, ID `11701666310` (Maven log, XML, frozen source hashes and gate).
- Earlier candidate CI `38137453060` had Maven success 19/19 but a failed independent verifier due to incorrect class name; this was repaired and **not** counted as verified.

## Implementation

Source `forge_tests/Stage9RGuardedCallbackTest.java`, derived from independently verified Stage9Q genuine-registered-deck controller. Two distinct persistent Python actor processes receive only strict allowlisted actor projections. Referee and Forge alone retain game legality, real priority and turn, mana/casting, stack, attacks, damage and shared life.

Four inherited `PlayerControllerAi` callbacks are now explicitly overridden to **throw SecurityException instead of consulting Forge's full-state AI**:

1. `chooseCardsForEffect` (otherwise delegates to `brains.chooseCardsForEffect`);
2. `chooseCardsToDiscardToMaximumHandSize` (otherwise delegates to `brains.getCardsToDiscard`);
3. `chooseStartingHand` (otherwise uses `ComputerUtil.scoreHand`);
4. `confirmAction` (otherwise delegates to `getAi().confirmAction`).

An extra named TestNG method directly probes all four and requires all to fail closed, without changing life, stack or card ownership. Eighteen existing Stage9Q named Forge priority, real land, creature stack, attack, damage, genuine registered deck and secret noninterference tests remain green with these guards.

## What this does NOT establish

This is a **test-only, bounded guard of four callback families**, not a comprehensive privilege firewall. Other inherited callbacks still need independent security analysis and replacement or fail-closed guards. Known high-risk pathways in pinned `PlayerControllerAi` include `chooseSingleEntityForEffect`, `chooseEntitiesForEffect`, `chooseSingleSpellForEffect`, `choosePermanentsToSacrifice`, `choosePermanentsToDestroy`, `assignCombatDamage`, `specifyManaCombo`, target choices and other confirmation / cost-choice methods.

Fail-closed callbacks intentionally do not implement game functionality requiring that choice. Complex spells, opening-hand decisions, discards, attack taxes, blockers, cost rollback, triggered abilities, ability targets and novel status effects remain unsupported. The two Python workers run as different processes but do not have hardened independent OS filesystem, network and IPC sandboxing.

The unresolved Stage9E face-down exile issues (cross-pile aliases, stale permissions, exile object identity after re-exile, timestamps, hidden originals, real costs and hidden library-top selection) remain open. User's custom negative-life/Ultimatus game-end semantics are not represented by ordinary Forge validation. **Not a complete Magic match or strategically strong ISMCTS AI.**

## Next Stage9S

Research all non-priority `PlayerControllerAi` method families against pinned source and official rules, inventory policy decisions and their private-state dependencies, and implement one additional restricted actor decision path or fail-closed class with explicit tests. Prefer `chooseSingleEntityForEffect`, `chooseEntitiesForEffect` and target selection first, without widening actor observation. Retain frozen registered deck ownership as the sole initial decklist source. Do not merge, deploy, train, use reserved seeds or promote before independent evidence review.

Rules: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt
ISMCTS research: https://eprints.whiterose.ac.uk/id/eprint/75048/
