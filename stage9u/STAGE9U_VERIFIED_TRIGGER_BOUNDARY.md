# Stage 9U — Bounded mandatory/optional triggers and two-actor priority

**Status: VERIFIED (test-only canary; not merged or deployed)**

## Evidence and pinned engine

- Successful pinned Forge CI: https://github.com/boubaca86/mtg-simulator/actions/runs/38142728716
- Tested source commit: `97cf028745251f62e709fa7074d2049af3558f06`
- GitHub branch: `expert-stage9u-trigger-boundary` (separate review branch; NOT merged into main or the Stage9A feature branch)
- Upstream: `Card-Forge/forge` tag `forge-2.0.15`, SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`
- Actual Java Forge test: **31/31 TestNG methods passed; 0 failures, 0 errors, 0 skips; Maven BUILD SUCCESS**.
- Independent strict Surefire XML gate: **PASS**, exactly 23 inherited Stage9T gameplay/hidden-info tests plus eight Stage9U trigger tests, no missing/duplicate/extra/skipped cases.
- Evidence artifact: `stage9u-trigger-boundary-38142728716-1`, ID `11703930504`, SHA-256 `d1fa7ba2ea10198ce76c408fc4e252522fb0d2cf8519fdb8020ac64354784fd9`.
- Source: `forge_tests/Stage9UTriggerBoundaryTest.java`; verifier `forge_tests/verify_stage9u_surefire.py` and its unit tests; workflow `.github/workflows/stage9u-trigger-boundary.yml`.

## What is actually established

The existing test-only restricted controller inherits the prior Stage9T controls and runs with **two distinct persistent Python actors**. The actors receive only their own objective, registered starting card-name counts, own hand/visible table, opponent's visible table and both referee-owned life totals. They do not receive game phase, priority, stack, opponent hidden hand/decklist, library order/top, or true face-down identities. Forge remains the trusted referee.

The Stage9U scoped `TriggerController` checks a **strict, cost-free, target-free, single-effect GainLife 1** shape against the pinned card scripts and refuses unaudited variants. It permits **Soul Warden** mandatory life gain while refusing to consult the inherited AI for the decision, and refuses to auto-choose **Soul's Attendant** optional life gain. The optional triggered ability can be queued on the Forge stack, but the unsupported choice stops the disposable game at the decision point rather than silently accepting or declining it.

The eight new tests verify actual creature ETB generating genuine wrapped triggers, mana-paid casting, distinct AI process interactions, mandatory +1 life via Forge, source leaving battlefield, pending optional effects, cross-world hidden-info noninterference, rejected trigger disguise/cost/subability/foreign-controller mutations, rejected unknown ordering, and rejection of unsupported target/static setup.

### Correction of two earlier failures

Earlier exploratory runs `38142133751` and `38142153505` showed **29/31 tests** because two tests compared `priorityCallbacks` **only after** the newly queued trigger had already been observed. This missed a legitimate callback in the same transition: the count was sampled too late to prove who saw the stack.

Commit `97cf028745251f62e709fa7074d2049af3558f06` added separate **referee-only trigger-stack priority counters** in the `TriggerController.chooseSpellAbilityToPlay` test override. A counter increments only when a real `WrappedAbility` is on Forge's stack during that actor's actual priority callback, and the tests independently require **both actor-specific counters >0** before resolution/decision. No stack, priority or other privileged information was added to the actor's serialized observation. No assertions were removed to hide a priority defect. The subsequent complete 31/31 pinned CI and XML verification **passed**.

## Boundaries and next engineering priority

- **Not a general triggered-ability implementation.** This is a narrow audited two-card test canary. Any other trigger shape, optional choice, target, cost, additional ability, multiple-trigger order, static trigger or target setup must be validated separately.
- Optional decisions are not yet represented as actor instructions; unsupported options fail closed. A production game would need legal option projection, actor intent validation, correct stack handling and a controlled error/abort path.
- Other inherited privileged decision routes remain, including scry/surveil, discard, nonbasic cost choices, mana combinations, replacement chains and more.
- These processes are separated but not hardened as OS sandboxes. Known Stage9E hidden-exile permission alias/stale capability/re-exile identity/timestamp/hidden-original issues remain open.
- Not an unrestricted Magic match, strong ISMCTS player or user's Ultimatus custom negative-life/library exhaustion implementation.
- No main merge, deployment, training/policy promotion or protected evaluation seeds were used.

**Stage 9V candidate:** introduce an explicit bounded optional-trigger decision `accept/decline` only for Soul's Attendant. Keep mandatory Soul Warden automatic, require actor decisions through a limited and referee-validated observable capability (avoid revealing phase/stack), verify authentic Forge life/stack results and secret-world noninterference; research exact timing CR 603.5 and Forge `confirmTrigger` before implementation. Continue with pinned Forge exact named CI and independent XML gate, no merge until reviewed.

## Research anchors

- Official [Magic comprehensive rules](https://magic.wizards.com/en/rules), CR 117 (priority), CR 603 (triggered abilities, including 603.5 choice timing), CR 601 and 608.
- [Pinned Forge source](https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676), particularly `PlayerControllerAi.confirmTrigger`, `PhaseHandler.mainLoopStep`, `WrappedAbility`, and `GameStack`.
- Cowling, Powley and Whitehouse [Information Set MCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/), for later imperfect-information strategic actor policies.
