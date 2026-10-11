# Stage 10G — VERIFIED referee-owned restricted combat damage

Status: **VERIFIED on source commit `eda38de358a0c6bdc001577875fabb8d297dbbca`** (2026-10-11). The branch remains **unmerged**.

## Independently observed proof
- GitHub Actions run: https://github.com/boubaca86/mtg-simulator/actions/runs/38180407706
- Workflow: `.github/workflows/stage10g-referee-damage.yml`
- Pinned upstream Card-Forge/forge tag `forge-2.0.15`, peeled commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Real pinned Forge TestNG `forge.ai.Stage10GRefereeDamageTest`: **55 executed, 0 failures, 0 errors, 0 skipped**.
- Maven process exit **0**, `BUILD SUCCESS`.
- Independent exact 55-name Surefire XML gate `forge_tests/verify_stage10g_surefire.py`: **PASS**, `executed_methods=55`.
- Stage10D two independent persistent actor IPC canary **PASS**.
- Stage10E two independent persistent menace actor IPC canary **PASS**.
- GitHub Actions evidence artifact: [stage10g-referee-damage-38180407706-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38180407706/artifacts/11717660275), artifact ID **11717660275**, 73,617 bytes, uploaded successfully.
- The full CI job completed with conclusion **success** at the exact source commit above.

## Scope
- Adds a **trusted Forge referee adapter override** of `ExternalCreatureController.assignCombatDamage` in `forge_tests/Stage10GRefereeDamageTest.java`; does not call `super.assignCombatDamage` or `ComputerUtilCombat.distributeAIDamage`.
- Supports native 2-damage `Grizzly Bears` source with one or two legally presented opposing Bears, assigning 2 to the first target (per CR 510.1c–d).
- Unsupported source, player, amount, non-Bear or more than two targets causes `SecurityException` before mapping damage; no inherited AI fallback.
- Two additional methods check one-to-one referee allocation and two-target allocation plus unsupported-case rejection; existing first-strike, menace, two-blocker and privacy tests remain covered.
- Existing two persistent separate Python actor processes receive only the previously allowlisted observation. A *single* Forge referee still exclusively handles game state, priority, damage and life. Separate PIDs do not constitute sandbox isolation.

## Sources reviewed
- Official WotC Comprehensive Rules effective September 25, 2026: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt (CR 509–510, 510.1c–d, 510.4, 702.7, hidden zones).
- Card-Forge pinned source: https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676
- Cowling, Powley & Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Explicit limits and no-go areas
- This is not a general Magic AI, not complete combat allocation, and not a hardened OS sandbox. No claim of complex trample/banding/deathtouch/replacement/cross-controlling combat cases, arbitrary spells, or custom Ultimatus winner rules.
- No `main` merge, no deploy, no training/promotion, and **no use** of protected evaluation seeds `20261042–49` or `20261050–57`.
- The documentation commit containing this report occurs **after** the tested software commit; only this Markdown proof is added, so it is not a claim that a later software SHA was CI tested.
- Next suggested Stage10H: strict fail-closed JSON parsing for duplicate keys and NaN/Infinity across all test actors with positive and negative IPC canaries. Do not mark verified without fresh exact source commit, tests and CI artifact.
