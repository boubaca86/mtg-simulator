# Stage 10F — First Strike in Native Forge Combat (VERIFIED)

**Verified October 11, 2026**, specifically for the narrow scenarios below.
Feature branch `expert-stage9a-forge-decision-pairs` remains **UNMERGED**.
No deployment, model training, promotion, or protected seed use.

## Reproducible pinned Forge CI evidence

- **Tested feature commit:** `56986d64d78d8243a00d1176a7c28156792d1cef`
- **Stage 10F CI:** https://github.com/boubaca86/mtg-simulator/actions/runs/38172395130
- **Pinned engine:** `Card-Forge/forge` tag `forge-2.0.15`, peeled SHA
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`
- Maven `exit=0`, `BUILD SUCCESS`
- **53/53 genuine pinned Forge TestNG methods**, zero failures,
  zero errors, zero skipped
- **Independent exact 53-method Surefire XML gate: PASS**,
  `full_magic_game:false`
- Both persistent two-actor Stage 10D and Stage 10E observation/IPC
  canaries: PASS
- Evidence archive: `stage10f-first-strike-38172395130-1`,
  GitHub Actions **artifact ID 11714726171** (source hash manifest,
  pinned Forge SHA, original per-method XML, Maven log, gate, protocol logs).

## Confirmed regressions on the identical feature commit

| Stage | CI run | TestNG | XML gate | Artifact ID |
| --- | --- | --- | --- | --- |
| 10D (two independent legal blocks, evasion) | [38172395142](https://github.com/boubaca86/mtg-simulator/actions/runs/38172395142) | **49/49** | PASS | 11714825938 |
| 10E (menace) | [38172395160](https://github.com/boubaca86/mtg-simulator/actions/runs/38172395160) | **51/51** | PASS | 11714596306 |
| 10F (first strike) | [38172395130](https://github.com/boubaca86/mtg-simulator/actions/runs/38172395130) | **53/53** | PASS | 11714726171 |

Each separate Maven build succeeded, with 0 failures/errors/skips.
These tests share the new actor behavior, exact initial feature SHA and
independent two-worker canaries as applicable; do not add these test
counts together as distinct scenarios.

## New real-Forge tests

- `firstStrikeAttackerKillsOrdinaryBlockerWithoutTakingDamage`: 2/2
  attacking registered Grizzly Bears receives First Strike from the
  **trusted referee-only fixture**. A normal 2/2 blocker legally blocks,
  then dies in native first-strike damage. The attacking Bear survives
  with both players' life totals unchanged.
- `firstStrikeBlockerKillsNormalAttackerAndSurvives`: reverse the keyword
  assignment. The first-striking 2/2 blocker survives native combat; the
  normal attacking 2/2 Bear dies, with both life totals unchanged.
- Each starts genuine native Forge registered two-player 60-card matches,
  uses separate persistent Python AI actor PIDs, bounded real native
  turns/attacker/blocker callbacks, verifies exact registered physical
  inventory, and rejects unapproved observation fields.

## Flake correction and regression

Original Stage 10F workflow run
https://github.com/boubaca86/mtg-simulator/actions/runs/38172148464
failed **one inherited menace test**, while both newly added first-strike
tests passed. Review found that the simple attacking actor could cast
a second Bear from hand while its first Bear was already on the table;
this could invalidate a defender's phase-agnostic single-visible-target
menace preference. `stage10d/multi_block_actor.py` now suppresses
additional creature-cast preferences **when a visible own Bear exists**,
using *only* its own allowlisted battlefield view. The independent
`stage10d/test_protocol.py` also asserts this invariant. Both changes
were committed to the feature branch, and all three current CI stages
passed on `56986d64...`. This is a limited strategic policy—not the
general solution to hidden or changing opponent battlefield options.

## Rules and upstream research

- Wizards of the Coast, Comprehensive Rules **effective Sep 25, 2026**:
  https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt
  - Rules 509.1a–e (legal blockers), 510.2 (simultaneous damage in a
    given step), 510.4 (two damage steps), and 702.7 (first strike).
- `Card-Forge/forge` `forge-2.0.15` pinned implementation of
  `Combat.assignCombatDamage(boolean)`,
  `Combat.assignAttackersDamage`,
  `Combat.assignBlockersDamage`, `Combat.dealAssignedDamage`,
  `PhaseHandler` first-strike/regular damage steps,
  `Card.addChangedCardKeywords`.
- Cowling, Powley and Whitehouse (2012), *Information Set Monte Carlo
  Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Architecture and important unverified boundaries

The two independent Python actor OS processes receive **only** objective,
own visible hand and battlefield, immutable actual registered original
deck-name/count multiset (not draw order/top), visible opponent table
and Forge-authoritative life totals. They do not receive opponent hidden
hand/deck, face-down originals, phase/turn/priority/stack/hidden order or
raw Card/Game. The single trusted Forge referee handles game state,
legality, timing, shuffle, combat and life.

**Not a full playable, independent master AI or OS security sandbox.**
The inherited Java `PlayerControllerAi.assignCombatDamage` still
delegates to `ComputerUtilCombat.distributeAIDamage` rather than a fully
audited limited referee-driven policy. This is a concrete remaining
privileged callback surface to address separately. Many complex spells,
targeting, cost rollback, multiple damage-assignment choices, spells on
stack, hidden-object/exile alias rules, full end-to-end game adjudication,
and the user's custom Ultimatus negative life/library exhaustion remain
untested or unsupported.

Do **not** merge into main or claim readiness for training/production.
