# Stage 10P — Verified native lethal/nonlethal Forge combat outcome

## Result — PASS (one bounded functional increment)

- **Exact tested software commit:** `fc5ef3bab84eddfee0e81969f1adde6c29706a62`.
- **Branch:** `expert-stage9a-forge-decision-pairs` (UNMERGED).
- **Workflow run:** https://github.com/boubaca86/mtg-simulator/actions/runs/38191702439
- **Inspected successful job:** `114625635013` (`real-delayed-exchange-and-pinned-forge`).
- **Pinned Forge annotated tag:** `forge-2.0.15`, peeled commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`. CI checkout and exact SHA verification passed.
- **Maven:** exit **0**, **BUILD SUCCESS**.
- **Actual pinned Forge TestNG:** `Stage10PRefereeGameOverTest` — **72 run, 0 failures, 0 errors, 0 skips**.
- **Independent Surefire XML exact-name gate:** `{"executed_methods":72,"full_magic_game":false,"gate":"PASS", ...}` (not only a CI green badge).
- **Two-actor information/IPC canaries:** Stage 10D PASS, Stage 10E PASS; Stage 10H strict JSON PASS with 56 malformed negatives.
- **Actions artifact:** ID `11722120130`, `stage10p-native-game-over-38191702439-1`, **81,350 bytes**, retained build log, method XML, hashes and gate output.
- **Previous verified Stage 10O:** 70/70 tests; retained as inherited tests rather than replaced.

## What Stage 10P genuinely adds

Two genuinely exercised Forge-native tests, preserving exactly **two independent persistent external Python actor OS processes** and **one trusted Forge referee**:

1. `nativeLethalUnblockedCombatRecordsForgeWinnerAndLoser`: a test-only two-life starting fixture and real unblocked Grizzly Bears combat, proposed by the external actor and validated by Forge, deal two damage. Forge state-based actions produce **`Game.isGameOver()`**, a **`LifeReachedZero`** defender loss, and winner identity in **`GameOutcome`**.
2. `nativeNonlethalUnblockedCombatDoesNotInventGameOver`: same actor-controlled unblocked combat from three life leaves one life. Actual Forge says no game over and no outcome; public life projection reflects one and still hides the opponent's hand and phase.

Stage 10P extends the genuine Stage10O pinned-TestNG referee fixture, with all 70 inherited tests and a new exact 72-method XML gate. The referee retains sole authority for legal attacks, combat, damage, life, state-based checks and game outcome. No trained AI changes, no promotion, deployment or merge.

## Research-first decisions and constraints

- Official Wizards Comprehensive Rules effective **2026-09-25**: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt . Relevant CR **400.2** (public/hidden zones), **509–510** (blocking and combat damage), **702.7** (first strike), **704.5a** (zero life loses).
- Cowling, Powley, Whitehouse, *Information Set Monte Carlo Tree Search* (2012), DOI https://doi.org/10.1109/TCIAIG.2012.2200894 . Keep actors within individual information sets; never disclose private opponent cards, ordered libraries or privileged referee state.
- The frozen original registered deck names/counts remain unchanged and may be shown only to their owner; real hidden order/top may not.
- These tests use deliberately configured **test-only life and battlefield fixtures** and deliberately restricted card choices. This **does not** prove a full naturally played Magic/Ultimatus match, full legal move coverage, or a hardened OS sandbox. Two separate PIDs alone are not an OS security boundary.
- Protected evaluation seeds `20261042–20261049` and `20261050–20261057` were not used.

## Verification scope

CI reports success specifically on tested commit `fc5ef3...`. This documentation-only follow-up commit does **not** constitute a newly tested software build. Keep the Stage10P test/CI artifacts pinned to the exact software SHA above.
