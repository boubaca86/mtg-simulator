# Stage 10E — Real Menace one-block rejection and lawful two-creature block

**Verified GREEN in pinned real Forge CI on October 11, 2026.**

- GitHub Actions: https://github.com/boubaca86/mtg-simulator/actions/runs/38162832100
- Tested unmerged feature SHA: `9c86143abe44218b4f72d756dd1613e1b8c0ef70`
- Pinned upstream `Card-Forge/forge` tag `forge-2.0.15`, peeled SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`
- Actual Maven **BUILD SUCCESS**, exit 0, **51 actual TestNG methods executed; 51 passed, 0 failures, 0 errors, 0 skipped**
- Independent strict Surefire XML evidence gate **PASS**, explicit `full_magic_game:false`
- Evidence artifact `stage10e-menace-blocking-38162832100-1`, ID `11711220613`, source SHA256 and method-by-method results
- Worker `stage10e/menace_defense_actor.py`
- Real Forge integration `forge_tests/Stage10EMenaceBlockingTest.java`
- Strict verifier `forge_tests/verify_stage10e_surefire.py`
- Pinned CI workflow `.github/workflows/stage10e-menace-blocking.yml`

## Verified functionality

Magic Comprehensive Rules effective September 25, 2026, **702.111b**: a creature with Menace cannot be blocked except by **two or more creatures**, subject to the normal declared-block rules in CR 509.

A new test-only Python defender-worker can propose two different **own visible battlefield indices** against a single **opponent visible battlefield index** as a phase-independent preference. The actor still receives exactly the user-approved view: objective, own hand and other own cards, immutable OWN registered decklist name/count multiset (not library order), legitimately visible opponent cards on the table, and both current referee-owned life totals. It is not sent turn, phase, stack, priority, the specific attackers, hidden identities, opponent private zones or referee internals. The Java referee checks current native Forge `Combat`, legality/ownership, `CombatUtil.canBlock`, block costs, and whole declaration validity; it alone assigns genuine `Combat.addBlocker` and authorizes resolution.

Two additional real pinned Forge TestNG methods passed:

1. `menaceOneProposedBlockerIsIllegalAndCombatDamageHitsPlayer`: the authoritative test referee gives one registered Grizzly Bears a real Forge `Menace` keyword effect and places just one real registered Bear under the defender's control. One candidate blocker is insufficient. No block is authorized; real Forge combat deals two damage to the defending player. Original card inventories reconcile.
2. `menaceTwoBlockersAreLegalAndPreventPlayerCombatDamage`: the attacker has one real registered 2/2 Bear with Menace and the defender has two other registered 2/2 Bears. Both distinct defender proposals for the same visible attacker pass Forge legality; the attacker is blocked, dies under genuine combat damage, one defending Bear also dies, and the defending player's life remains unchanged. Inventory reconciles.

All prior **49** Stage10D methods passed, including native sixty-card `Match.startGame`, exact frozen owned deck lists, separate subprocess decisions, land, mana-paid casting, correct draws and turn limits, ordinary combat, two-vs-two blocks, flying evasion, tapped blockers, and strict actor JSON validation.

## Limitations and security caveats

- The registered physical Bears are deliberately moved from their owners' libraries to the battlefield by the **trusted test referee** to create deterministic combat encounters. These are **not unbiased draws** or full self-running matches.
- Stage 10E restricts AI-visible **proposals** and native block legality but does **not** replace every inherited `PlayerControllerAi` non-priority callback. In particular, ordering blockers and distributing combat damage can still inherit Forge's built-in AI policy; this remains a potential information-boundary weakness. No comprehensive secrecy proof is claimed.
- Two separate Python subprocesses are not isolated OS sandboxes: filesystem, network, process-inspection and IPC hardening remain outstanding.
- The working policy is narrow, not expert, and does not cover arbitrary card text, all combat rules, costs, targeting or full custom-format endgame.
- User's custom higher-life-wins-at-library-exhaustion / life-below-zero rules remain unimplemented. Stock Forge game-over semantics are used.
- No merge to `main`, policy training, reserved evaluation seeds, promotion or production deployment.

## Next recommended milestone — Stage 10F

**Prioritize least-privilege combat ordering and damage assignment**. Audit `PlayerControllerAi.orderBlockers`, `orderBlocker`, `orderAttackers`, `assignCombatDamage`, and `reveal` for access to hidden zones. Where callbacks are necessary, substitute referee-mediated deterministic logic based on **only visible combat objects**, never unrestricted `ComputerUtilCombat`, and test genuine two-block and Menace resolution again. Then harden OS process isolation and tighten actor IPC.

Research: [Magic CR effective 2026-09-25](https://blogs.magicjudges.org/rules/comprehensive-rules/), especially 509 and 702.111; [Forge pinned source](https://github.com/Card-Forge/forge/tree/forge-2.0.15); [ISMCTS imperfect-information search](https://eprints.whiterose.ac.uk/id/eprint/75048/).
