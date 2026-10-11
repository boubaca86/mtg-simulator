# Stage 10M — VERIFIED trusted Forge referee -> actor stdin write deadline

**Status: VERIFIED** on exact tested source commit `ede410f3f511c9bdca30dfd549d7a68cd8118eb9`, October 11, 2026. The feature branch `expert-stage9a-forge-decision-pairs` remains **unmerged**.

## Immutable CI evidence

- [Successful Stage10M GitHub Actions run](https://github.com/boubaca86/mtg-simulator/actions/runs/38185915001)
- Test workflow: `.github/workflows/stage10m-stdin-write-timeout.yml`.
- Pinned `Card-Forge/forge` annotated tag `forge-2.0.15`, peeled commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Real pinned Forge TestNG `forge.ai.Stage10MActorWriteDeadlineTest`: **65 executed, zero failures, zero errors, zero skips**.
- Maven process exit **0**, **BUILD SUCCESS**.
- Independent exact 65-method Surefire XML verification `forge_tests/verify_stage10m_surefire.py`: **PASS** (`executed_methods=65`).
- Stage10D and Stage10E two-independent-persistent-Python-actor privacy IPC tests: PASS.
- Stage10H strict observation JSON canary: PASS across four actor types (56 malformed cases).
- Uploaded immutable artifact [stage10m-stdin-write-timeout-38185915001-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38185915001/artifacts/11719536944), ID **11719536944**, **77,807 bytes**.
- CI job ID `114608074630`; inspected log explicitly confirms all above on source SHA shown.

## Engineering change and 2 genuine adversarial tests

- `forge_tests/Stage10MActorWriteDeadlineTest.java` keeps the **single trusted Java Forge referee** and exactly **two external independent persistent Python AI actor OS processes**. A per-actor trusted referee-owned daemon I/O thread (not another actor or referee) is reused serially for outbound `writer.write/newLine/flush` and inbound bounded reply reads.
- Outbound requests must be newline-free, UTF-8 encoded, and **under 16,384 bytes**; each entire asynchronous write/flush is capped at **10 seconds**. On a stalled or interrupted send, the referee destroys the stalled actor, cancels the pending operation, and rejects the request. A failed partially written frame is **never retried as a legal action**.
- Test-only `stage10l/adversarial_actor.py` gained a `noread` mode: an actual Python child keeps its stdin pipe open but never drains it. This is a real OS pipe backpressure test, not just a simulated stream.
- `realNonreadingPythonActorCannotBlockNormalRefereeSends`: repeatedly sends copies of the same **allowlisted own/public observation** (each individually under 16KiB) to that adversarial process to fill the operating-system pipe. Once a write blocks, verifies a bounded write-deadline rejection, targeted actor termination, the independent healthy actor's continued responses, and no change in Forge life/hand/library/stack.
- `realNonreadingPythonActorCannotBlockOpeningHandRefereeSends`: same live two-PID adversarial test with the permitted `own_opening_hand` envelope. No phase, stack, turn, opponent private zones, face-down original identities, or raw Card/Game objects are transmitted.
- Both new methods plus all 63 inherited real pinned Forge combat, limited referee damage, first strike, menace, private-observation, strict inbound JSON, strict actor replies, UTF-8, timeout, and IPC tests were required by the independent exact method-name gate.

## Research reviewed

- Official WotC Comprehensive Rules effective September 25, 2026: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt (CR 400.2, 509–510, 702.7).
- Pinned Forge implementation source: https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676
- Cowling, Powley, Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Limits and future work

- In real gameplay, the referee sends one legal observation and then waits for an actor reply. A single short message typically fits the OS pipe; the Stage10M adversarial tests deliberately repeat legal-shaped messages to trigger actual OS backpressure. This verifies write boundedness, **not** that ordinary gameplay normally exhibits repeated writes without replies.
- Input validation is unchanged. Exactly two independent actor PIDs and one authoritative Forge referee. Actors receive only own objective, own hand/face-up own table, frozen starting main-deck name/count multiset (never order/top), legally visible opponent table, and current referee-owned life totals. Forge alone decides all rules, priority, mana/costs, draw/shuffle, combat, damage, life.
- Two separate Python OS PIDs do **not** constitute a hardened sandbox. This is test-only Forge coverage of narrow supported actions, not a general Magic AI, production deployment, guaranteed complete game/winner engine, all inherited callbacks, or Ultimatus completion.
- The outgoing 10-second deadline and incoming 10-second deadline are **two separate waits**, not one shared per-exchange 10-second budget; a future stage may impose one overall end-to-end exchange budget. Cost/state rollback and timeout routing for every possible Forge callback still require investigation.
- No merge to `main`, deployment, promotion, training, or use of protected evaluation seeds `20261042–49` and `20261050–57`. Stage8P learned3 versus baseline4 remains FAILED.

This report is a **documentation-only commit** made after CI validated the exact software source SHA above.
