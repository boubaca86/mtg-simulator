# Stage 10G — restricted combat-damage assignment audit (research gate)

Status: **design reviewed; implementation and CI NOT verified**. Feature branch remains unmerged.

## Authoritative references

- Wizards of the Coast Comprehensive Rules effective September 25, 2026:
  https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt
  - CR 510.1c: a blocked attacker assigns all damage to its single blocker; for two or more blockers its controller chooses the division.
  - CR 510.1d: a blocker assigns all damage to its single attacker; for two or more attackers its controller chooses the division.
  - CR 510.2: assigned combat damage is dealt simultaneously without intervening priority.
  - CR 510.4: first strike changes which creatures assign damage in each damage step.
  - CR 702.111b: menace requires at least two blockers.
- Exact pinned Card-Forge/forge tag forge-2.0.15 peeled SHA:
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
  - `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java`, method `assignCombatDamage` delegates to `ComputerUtilCombat.distributeAIDamage`.
  - `forge-ai/src/main/java/forge/ai/ComputerUtilCombat.java` uses live Game/Combat internals and AI heuristics.
  - `forge-game/src/main/java/forge/game/combat/Combat.java` invokes controller `assignCombatDamage` for blocked attackers and blockers.
- Cowling/Powley/Whitehouse (2012), Information Set Monte Carlo Tree Search:
  https://eprints.whiterose.ac.uk/id/eprint/75048/

## Narrow Stage 10G acceptance criteria

1. Override `ExternalCreatureController.assignCombatDamage` in the **trusted referee adapter** in the test-only native Forge game. Never invoke `super.assignCombatDamage` or `ComputerUtilCombat.distributeAIDamage`.
2. For a registered 2/2 Grizzly Bears source and exactly one or two opposing registered 2/2 Bears, allocate the full 2 damage to the first legal opposing Bear. Under the current CR 510.1c–d, all damage on one of two blockers is legal; no obsolete damage assignment ordering is required.
3. Reject unsupported sources, target counts, or damage amounts before any state change, without consulting inherited AI.
4. Do not transmit damage allocation, Forge internals, priority, phase, stack, hidden hand, hidden library order, or private card identities to either of the **two independent persistent external actor processes**. One Forge referee alone controls state and rules.
5. Add at least two new real pinned Forge TestNG methods: exact one-to-one allocation and two-blocker allocation plus explicit unsupported-case rejection. Inherited native one-to-one, menace two-blocker and first-strike tests must assert the new referee callback actually ran.
6. Require exact independent Surefire XML method gate, zero failures/errors/skips, Maven BUILD SUCCESS, Stage10D/E actor canaries, source hashes, and a downloadable CI evidence artifact before marking verified.

## Limitation

This is not a full combat allocator, hidden-information security sandbox, or permission to merge. Trample, banding, replacement effects, complex damage allocation, and non-Bear creatures remain unsupported until independently reviewed.
