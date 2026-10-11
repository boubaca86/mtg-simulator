# Stage 10L — VERIFIED real unresponsive Python actor timeout recovery

**VERIFIED** on exact software source SHA `9cc45e6b586e6d3b16526270a7d5e205195c8b5a`, October 11, 2026. The development branch `expert-stage9a-forge-decision-pairs` is **unmerged**.

## Independently checked CI evidence

- GitHub Actions SUCCESS: https://github.com/boubaca86/mtg-simulator/actions/runs/38185544377
- Pinned upstream `Card-Forge/forge` annotated tag `forge-2.0.15` peeled commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Genuine pinned Forge TestNG `forge.ai.Stage10LRealActorDeadlineTest`: **63 executed, zero failures, zero errors, zero skips**.
- Maven exit **0**, **BUILD SUCCESS**.
- Independent exact 63-name Surefire XML verification: `forge_tests/verify_stage10l_surefire.py`: **PASS**, `executed_methods=63`.
- Stage10D persistent two-process actor IPC canary PASS; Stage10E persistent actor privacy canary PASS; Stage10H four-worker strict inbound JSON canary PASS (56 malformed negative cases).
- Immutable uploaded CI artifact: [stage10l-real-actor-timeout-38185544377-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38185544377/artifacts/11719233704), ID **11719233704**, **77,152 bytes**.
- CI job ID `114606955938`; log explicitly reports all of the above results, same source SHA.

## Source change and genuine real-process tests

- Test-only trusted Java Forge referee: `forge_tests/Stage10LRealActorDeadlineTest.java`.
- Test-only malicious Python worker: `stage10l/adversarial_actor.py`.
- Workflow: `.github/workflows/stage10l-real-actor-timeout.yml`.
- `Actor` helper can launch a controlled test-only adversarial script via an explicit path/mode, without changing the legal actor path or widening the projection. Its timeout flag permits orderly test fixture teardown after an expected forced Python process termination.
- `realSilentPythonActorTimesOutAndOtherActorContinues`: EXACTLY TWO independent live Python actor OS processes are launched simultaneously, one permanently silent and one valid. The stalled process is terminated on the trusted referee's **10-second** stdout deadline, while the other actor stays alive and can return an unchanged valid decision. Forge-owned life, hand/library counts and stack remain unchanged.
- `realPartialPythonOpeningReplyTimesOutAndOtherActorContinues`: the adversary returns otherwise-valid reply JSON but deliberately omits the terminating newline, during the distinct own-opening-hand request path. The referee denies the request by timeout, terminates only that worker and allows the other legal actor to continue. Hidden information and referee authority remain isolated.
- Existing Stage10K simulated pipe timeout, Stage10J strict UTF-8, Stage10I strict Java inbound actor reply JSON, Stage10H strict Python inbound observation JSON and Stage10D/E/F/G game, combat and privacy tests are inherited into the same exact 63-method run.

## Research grounding

- Official Magic: The Gathering Comprehensive Rules, September 25, 2026: https://magic.wizards.com/en/rules, relevant hidden information CR 400.2, combat CR 509–510 and first strike CR 702.7.
- Exact pinned Forge source https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676 ; inherited `PlayerControllerAi.assignCombatDamage` uses `ComputerUtilCombat.distributeAIDamage`, and existing restricted referee code bypasses it.
- Cowling, Powley & Whitehouse, *Information Set Monte Carlo Tree Search*, IEEE TCIAIG 2012: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Remaining limitations

- Timeout is enforced on **actor stdout reads**; synchronous Python actor `writer.write/flush` on the referee's process stdin is NOT yet protected by an elapsed-time deadline. That is an independent future design risk.
- This is a small test-only Forge game adapter, **not** a complete MTG AI, hardened OS sandbox or production system. Each adversarial test has exactly two independent Python actor OS processes and one authoritative Forge referee. A small referee-owned thread for bounded I/O is not an AI actor.
- Actor observations are unchanged: own objective, hand, visible own table, immutable starting main-deck name/count multiset without order/top, visible opponent table and both current Forge-maintained life totals; no opponent private hand/library/order/top, original face-down identities, phase/turn/priority/stack, raw Forge Card/Game references, or shared private files.
- No main merge, deployment, training, promotion, or protected evaluation seeds 20261042–49 / 20261050–57. Stage8P learned3 vs baseline4 FAILED.

**Evidence semantics:** This report is a documentation-only commit after the verified software SHA; it is not another tested software revision.
