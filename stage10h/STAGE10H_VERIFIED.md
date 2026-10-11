# Stage 10H — VERIFIED strict actor JSON inbound boundary

**Status: VERIFIED** on source commit `25873fb48d3286deb05cb7ecd611619fd096f54f` (2026-10-11). The feature branch `expert-stage9a-forge-decision-pairs` is still **UNMERGED**.

## Independent evidence

- GitHub Actions Stage10H run: https://github.com/boubaca86/mtg-simulator/actions/runs/38182880607
- Tested source SHA: `25873fb48d3286deb05cb7ecd611619fd096f54f`
- Pin: Card-Forge/forge tag `forge-2.0.15`, annotated tag peeled to `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Four actor scripts: `stage9w/opening_hand_actor.py`, `stage10b/action_plan_actor.py`, `stage10d/multi_block_actor.py`, `stage10e/menace_defense_actor.py`.
- `stage10h/test_protocol.py`: **PASS**: 2 persistent separate OS PIDs per actor-type positive test, valid normal and own-opening-hand messages, **56 malformed negative requests** (14 each × 4 workers), no output on rejection.
- Stage10D existing actor IPC protocol canary: PASS.
- Stage10E existing actor IPC privacy canary: PASS.
- Pinned genuine Forge TestNG `forge.ai.Stage10GRefereeDamageTest`: **55 executed, 0 failures, 0 errors, 0 skipped**.
- Maven: exit **0**, **BUILD SUCCESS**.
- Independent exact 55-name Surefire XML method gate `forge_tests/verify_stage10g_surefire.py`: **PASS** (`executed_methods=55`).
- Immutable uploaded evidence artifact: [stage10h-strict-json-38182880607-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38182880607/artifacts/11717713358), ID **11717713358**, size 73,975 bytes.
- Same-source separate Stage10G regression: [success on SHA 25873fb](https://github.com/boubaca86/mtg-simulator/actions/runs/38182880631), artifact ID **11717979569**.

## Scope

The four existing test-only independent Python workers now parse every inbound JSON message with `json.loads(..., object_pairs_hook=..., parse_constant=..., parse_float=...)`. Decoded duplicate member names are rejected at any nesting depth, including Unicode-escaped collisions and opening-hand request envelopes. `NaN`, `Infinity`, `-Infinity`, and non-finite float overflow like `1e999` are rejected. Valid JSON continues through the unchanged exact projection allowlist, with the existing **16,384-byte** message bound. Malformed input fails closed with **no JSON response**.

The test suite additionally exposed an unrelated inherited Stage10G fixture nondeterminism: a registered Grizzly Bears physical card may reside in its owner's own hand after the native opening draw rather than in the library. The trusted test-only fixture now searches the owner's library then own hand, preserving registered whole-card inventory and never disclosing it to the opponent actor. This was required for repeatable whole-suite verification.

## Sources consulted

- Official WotC Magic Comprehensive Rules effective 2026-09-25: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt (CR 400.2 hidden zones; 509-510 combat; 702.7 first strike).
- Pinned Forge source: https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676
- Cowling, Powley & Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Architecture and limits

Exactly **two independent persistent Python actor processes** and **one authoritative Forge referee**; actors get only objective, own hand and visible battlefield, frozen main-deck name/count multiset without order, legally visible opponent battlefield, and referee-owned dual life totals. No hidden opponent cards or library/order/top, face-down originals, turn/phase/priority/stack, raw Forge Card/Game objects, or shared private files. Forge alone owns priority, rules, mana/costs, stack, draw/shuffle, combat and life.

Separate PIDs are **not** a hardened OS sandbox. This is an incremental test-only protocol hardening stage, not a complete MTG AI, hardened deployment, or a proof for all inherited callbacks, complex combat, cost rollback, Stage9E exile aliases, or Ultimatus completion. No training, promotion, deployment, main merge, or protected evaluation seed use (20261042-49, 20261050-57). Stage8P learned3 vs baseline4 remains FAILED.

## Failure history

Initial Stage10H run `38182695057` failed because the new workflow misnamed environment variables required by inherited Stage10G Java tests. A later Stage10G run exposed an intermittent library-only fixture assumption after native opening draw. Both were corrected before the **final fully green same-source Stage10H and Stage10G runs** above. Do not use failed attempts as verification evidence.
