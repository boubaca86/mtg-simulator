# Stage 10N — VERIFIED one shared actor exchange I/O deadline

**VERIFIED source SHA:** `7e38222aadf29bc9d33eedf26d5108c9f4410798` (GitHub Actions run completed 2026-10-12 00:04 UTC / October 11 Toronto). The feature branch `expert-stage9a-forge-decision-pairs` remains **unmerged**.

## Independently inspected CI evidence

- [Stage10N GitHub Actions SUCCESS](https://github.com/boubaca86/mtg-simulator/actions/runs/38186476084), run ID `38186476084`, tested software SHA `7e38222aadf29bc9d33eedf26d5108c9f4410798`.
- Pinned upstream Card-Forge/forge annotated tag `forge-2.0.15`, peeled SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Actual Forge TestNG `forge.ai.Stage10NExchangeBudgetTest`: **67 executed, zero failures, zero errors, zero skips**.
- Maven exit **0**, **BUILD SUCCESS**.
- Independent exact 67-name Surefire XML method gate `forge_tests/verify_stage10n_surefire.py`: **PASS** with `executed_methods=67`.
- Stage10D and Stage10E two-independent-persistent-actor IPC privacy canaries: PASS.
- Stage10H four-worker strict inbound JSON actor canary: PASS (56 malformed negatives).
- [Immutable evidence artifact](https://github.com/boubaca86/mtg-simulator/actions/runs/38186476084/artifacts/11719577564), ID **11719577564**, size **78,397 bytes**, uploaded.
- CI job `114609765114`; its actual log explicitly reports all of the above with matching pinned source.

## One scoped improvement

- Trusted Forge test-only referee source: `forge_tests/Stage10NExchangeBudgetTest.java`; workflow: `.github/workflows/stage10n-exchange-deadline.yml`.
- Before this change Stage10M used independent **10-second outbound send** and **10-second inbound reply** waits; a slow send could therefore be followed by a fresh entire 10-second receive deadline.
- Stage10N records one monotonic `System.nanoTime()` expiration before sending the actor observation. It passes the **remaining budget** to the existing trusted referee-owned serialized writer operation and, after successful send, passes only the **remaining part of the same budget** to the inbound reply read. If nothing remains, it aborts the actor and fails closed. The configured combined I/O time budget is 10 seconds for each `ask` and `askOpening`, without a new player, AI process, referee, observation field, or permissive fallback.
- Genuine Forge `sharedExchangeDeadlineRejectsCombinedDelayWithoutStateMutation` uses a trusted test-only delayed writer and reader where each I/O phase fits a fresh independent allocation but both combined exceed the overall allowance. It verifies denial, the abort callback, bounded elapsed wait and unchanged Forge life, hand, library and stack.
- Genuine Forge `sharedExchangeDeadlineAcceptsPromptNormalAndOpeningHandResponses` verifies successful valid normal and own-opening-hand replies with modest delays during both send and read.
- All 65 inherited native Forge regressions remain required: real two-Python-process nonreading stdin backpressure, real silent/partial stdout hang recovery, JSON/UTF-8 framing, private-information guardrails, limited 2/2 bear combat damage, menace, first strike, legal priorities and mana-paid actions.

## Research

- Official WotC Magic Comprehensive Rules effective September 25, 2026: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt (CR 400.2 hidden zones; CR 509–510 blocks/damage; CR 702.7 first strike).
- Exact pinned Forge source https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676
- Cowling, Powley and Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Strict limits

- The **shared 10-second budget applies only to the paired actor I/O send and read**, not to Forge parsing, schema validation, game-rule logic, subprocess startup, or all subsequent turns of a match. Millisecond remaining-time conversion is rounded down to avoid extending the budget.
- New combined-delay tests use controlled referee-side streams; inherited genuine two-process tests cover normal legal Forge exchanges plus isolated stalls and backpressure. A future stage can prove the combined budget with an end-to-end delayed real Python worker.
- The architecture remains exactly two independent persistent Python actor OS processes and one authoritative Forge referee. Actors receive only their objective, own hand/legally visible own table, original frozen deck name/count multiset without order/top, legally visible opponent battlefield, and both referee-owned life totals. Never hidden opponent zones, face-down originals, turn/phase/priority/stack, raw Forge Card/Game or shared private files. Separate PIDs are NOT hardened OS sandboxing.
- This remains a narrow test adapter, **not** a complete competitive AI, production security proof, full Magic game, or implemented Ultimatus winner engine. Stage8P learned3 vs baseline4 remains FAILED.
- **No** main merge, deploy, training, promotion or protected evaluation seeds 20261042–49 / 20261050–57.

**Evidence semantics:** This report is a documentation-only commit made after testing the exact source SHA. It does not imply a new validated code revision.
