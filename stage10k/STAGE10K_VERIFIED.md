# Stage 10K — VERIFIED bounded referee-owned actor response deadline

**VERIFIED on exact tested software source SHA:** `eb7c3de589997e9f121875cc2fda237841d10a59`, 2026-10-11. Unmerged feature branch: `expert-stage9a-forge-decision-pairs`.

## CI evidence (actual inspected logs)

- GitHub Actions SUCCESS: https://github.com/boubaca86/mtg-simulator/actions/runs/38185124830
- Pinned Card-Forge/forge annotated tag `forge-2.0.15` peeled SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Genuine Forge TestNG `forge.ai.Stage10KReplyDeadlineTest`: **61 tests executed, 0 failures, 0 errors, 0 skipped**.
- Maven exit **0**, `BUILD SUCCESS`.
- Independent exact 61-name Surefire XML verification `forge_tests/verify_stage10k_surefire.py`: **PASS**, `executed_methods=61`.
- Stage10D and Stage10E actor IPC privacy canaries: PASS.
- Stage10H strict inbound-JSON IPC canary: PASS (56 malformed negatives, separate persistent Python actor PIDs).
- Uploaded GitHub Actions artifact: [stage10k-reply-deadline-38185124830-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38185124830/artifacts/11719043271), ID **11719043271**, **76,124 bytes**.

## Precise scope and mechanism

In `forge_tests/Stage10KReplyDeadlineTest.java`, the **test-only trusted Java Forge referee** uses one reusable daemon I/O thread per existing independent persistent Python actor to execute the previously strict length-bounded, UTF-8-validating, newline-terminated actor reply read. The referee caller applies `Future.get(10000, MILLISECONDS)`. On timeout or interruption, the referee destroys the offending Python worker, cancels the pending read and throws `SecurityException`; it does not grant that worker a game decision. Threads run inside the existing trusted referee process; they are **not additional AI processes or referees**. Actor reply schema/observation projections stay unchanged.

Two added genuine pinned Forge tests:
- `refereeDeadlineDeniesSilentActorAndKeepsGameUnchanged`: simulates an open but silent actor stdout with a pipe, bounds the elapsed wait, aborts its pipe, checks reader cleanup, life totals, physical library count, and empty stack unchanged.
- `refereeDeadlineAcceptsPromptFramesAndDeniesPartialUnfinishedReply`: accepts two separate newline-delimited legitimate replies using one reusable reader executor; then simulates an actor that sends complete JSON but never terminates its line and verifies a bounded deadline denial.

The inherited Stage10J strict UTF-8 checks; Stage10I strict JSON reply keys/nonfinite/trailing data; Stage10H strict actor input JSON; Stage10G referee-controlled limited 2/2 Bear damage; Stage10D/E/F actions/menace/first strike/privacy tests all remain covered by the exact 61-TestNG-method regression class.

## Research

- Official WotC Comprehensive Rules, effective 2026-09-25: https://magic.wizards.com/en/rules (CR 400.2; CR 509–510 combat and 702.7 first strike).
- Pinned Forge source: https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676
- Cowling, Powley & Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## What this does NOT prove

- Timeout failure is verified via **synthetic pipe read tests** and full normal two-process Forge regression. A real end-to-end hung Python actor still needs an adversarial subprocess canary; do not conflate a simulated stall with a hardened sandbox.
- The deadline measures bounded actor **stdout waiting**, not cost of all referee game logic or subprocess writes. Existing synchronous `writer.flush()` has no deadline.
- The two independent persistent OS PIDs are NOT a hardened security sandbox. This test-only scenario is not a full Magic AI; complex decisions, inherited callbacks, cost rollback, true target handling, library exhaustion, Ultimatus and full winner logic remain out of scope.
- No `main` merge, deployment, training, promotion or protected evaluation seeds 20261042–49 / 20261050–57. Stage8P learned3 versus baseline4 remains FAILED.

This report is **docs-only** and is committed after the tested software commit; it does not replace the exact verified run SHA.
