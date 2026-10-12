# Stage 10O — VERIFIED real two-process delayed actor exchange tests

**VERIFIED** on exact tested source SHA `45486bd58299e01d0e3fa17f065961736d78f0d6`, October 11, 2026 Toronto (October 12 UTC). Branch `expert-stage9a-forge-decision-pairs` remains **UNMERGED**.

## Independently inspected GitHub CI

- [Successful Stage 10O GitHub Actions run](https://github.com/boubaca86/mtg-simulator/actions/runs/38186900226), source commit `45486bd58299e01d0e3fa17f065961736d78f0d6`.
- Pinned `Card-Forge/forge` annotated tag `forge-2.0.15`, peeled commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Genuine Forge TestNG class `forge.ai.Stage10ORealExchangeBudgetTest`: **70/70 executed; zero failures, zero errors, zero skips**.
- Maven exit **0**, **BUILD SUCCESS**.
- Independent exact **70-method-name** Surefire XML gate `forge_tests/verify_stage10o_surefire.py`: **PASS**, `executed_methods=70`.
- Stage10D and Stage10E independent persistent two-actor IPC privacy canaries: PASS.
- Stage10H four-worker strict inbound JSON canary: PASS (56 malformed negative requests).
- CI job ID `114611061706` logs explicitly state all results.
- Evidence: [stage10o-real-exchange-deadline-38186900226-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38186900226/artifacts/11720206558), artifact ID **11720206558**, **80,382 bytes**.

## Scope of the increment

The test-only Python worker `stage10o/controlled_delay_actor.py` reads only the exact allowed own/public observation shape or legitimate own-opening-hand envelope, checks strict JSON member uniqueness and numeric finite parsing, and sends either a valid pass or a full own-hand-index permutation. It supports a deliberate 1.4-second reply delay (`slow`) or short 40ms delay (`prompt`). It does NOT learn about opponent private information, Forge Game/Card objects, phase, priority, stack, library order, or hidden card identities.

The genuine Forge class `forge_tests/Stage10ORealExchangeBudgetTest.java` inherits all 67 pinned Stage10N tests and introduces three methods, each with **exactly two actual, separate, persistent Python actor OS processes and one authoritative Forge referee**:

1. `realDelayedPythonNormalReplyDeniedBySharedBudgetAndPeerSurvives`: a real slow worker exceeds a **650ms test-only** shared I/O deadline for a regular observation and is terminated without legal action. Independent opponent worker remains responsive, game life, hands, libraries and stack unchanged.
2. `realDelayedPythonOpeningReplyDeniedBySharedBudgetAndPeerSurvives`: the same real-process timeout proof for a real own-opening-hand envelope, with identical privacy and no-state-mutation checks.
3. `realPromptPythonWorkerAnswersBothRequestsWithinSharedBudget`: a prompt persistent worker successfully returns three normal and three own-opening-hand decisions within a **3-second test-only** budget for each exchange, while the independent opponent remains responsive.

The **production-path test adapter default remains the existing Stage10N combined 10-second send+read I/O deadline**. The shortened timeouts are injected only into the new tests. The referee still owns all Magic rules, mana costs, priority, combat, shuffle/draw and life.

## Grounding

- Official WotC Magic Comprehensive Rules: https://magic.wizards.com/en/rules (effective Sept 25, 2026; CR 400.2, 509–510, 702.7).
- Pinned Forge source https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676 ; inherited PlayerControllerAi combat damage uses ComputerUtilCombat and the restricted Forge referee adapter avoids privileged AI fallback.
- Cowling, Powley and Whitehouse (2012), *Information Set Monte Carlo Tree Search*, https://eprints.whiterose.ac.uk/id/eprint/75048/.

## Strict limits

- Real Python worker tests prove the shared deadline with normal **prompt or slow reply** scenarios. The send stage is typically fast in these tests. Stage10N's combined slow-send+slow-read proof used controlled Java streams; Stage10M's real non-reading Python worker test proved outbound pipe backpressure separately. This does **not** prove a slow physical stdin send and slow physical stdout read combined in one live-worker exchange.
- Only send+read I/O is bounded by this deadline, NOT JSON parse time, all referee computation, process startup, or an entire Magic match clock.
- Exactly two independent Python actor processes and one authoritative Forge referee; separate OS PIDs are **not** a hardened sandbox. This is narrow test-only game behavior, **not** a full Magic AI, complete winner engine, all cards or Ultimatus system.
- The Stage8P learned3 versus baseline4 comparison remains **FAILED**. No `main` merge, training, deployment, model promotion or protected evaluation seeds `20261042–49` / `20261050–57`.

This is a **documentation-only commit after testing source SHA**. It does not imply the docs commit received a new source regression run.
