# Stage 10I — VERIFIED bounded, strict untrusted actor-reply parsing

**Status: VERIFIED**, tested software source commit `56dddf3dcd250bc0b8a84ba741d92d50ad80d146` on 2026-10-11. Development branch `expert-stage9a-forge-decision-pairs` remains **unmerged**.

## Reproducible CI evidence

- Stage10I GitHub Actions: https://github.com/boubaca86/mtg-simulator/actions/runs/38184005742
- Run conclusion: **SUCCESS**, run source SHA: `56dddf3dcd250bc0b8a84ba741d92d50ad80d146`.
- Exact upstream Card-Forge/forge `forge-2.0.15` annotated tag peeled SHA: `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Genuine Forge TestNG class: `forge.ai.Stage10IRefereeReplyTest`, **57 executed, 0 failures, 0 errors, 0 skipped** (55 inherited Stage10G regressions and two additional referee-reply tests).
- Maven exit code **0**, `BUILD SUCCESS`.
- Independent strict XML method-name gate `forge_tests/verify_stage10i_surefire.py`: **PASS**, `executed_methods=57`.
- Stage10D persistent actor IPC canary: PASS.
- Stage10E persistent actor IPC privacy canary: PASS.
- Stage10H strict inbound actor JSON canary: PASS (56 malformed requests across four actor-script types; independent persistent actor PIDs per positive test).
- Uploaded evidence: [stage10i-referee-replies-38184005742-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38184005742/artifacts/11718219308), ID **11718219308**, **74,724 bytes**, successful upload.
- CI logs explicitly report 57/57, zero failures/errors/skips, Maven success, and exact XML PASS.

## Scoped implementation

- Source: `forge_tests/Stage10IRefereeReplyTest.java` (test-only trusted referee), workflow `.github/workflows/stage10i-referee-replies.yml`.
- The trusted Java Forge referee now reads the external actor reply with a bounded character-by-character reader before newline, instead of unbounded `readLine()`, retaining the original <2048 reply constraint.
- `parseStrictActorReply` checks raw UTF-8 size and uses a strict Gson streaming `JsonReader` pre-scan at all nesting depths to reject **duplicate decoded JSON member names**, malformed/trailing JSON, unapproved tokens, non-finite numbers and unbounded nesting. Only after that scan does the existing Gson parse and existing strict action/opening-hand schema validation run.
- Positive tests cover valid normal action-plan/pass replies and a valid opening-hand reply.
- Negative tests cover duplicate/escaped names, nested duplicates, NaN, Infinity, -Infinity, 1e999, trailing data, invalid root, and overlong replies.
- No new actor fields, private-game references, permission, referee, subprocess, AI planning or play mode. Forge alone decides authoritative game actions, legal combat, priorities, life, shuffle and mana.

## Research grounding

- Official WotC September 25, 2026 Comprehensive Rules: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt (CR 400.2 hidden zones, 509–510 combat and 702.7 first strike).
- Pinned Forge source: https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676
- Cowling, Powley & Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Explicit limits

Architecture remains exactly **two independent persistent Python actor processes** and **one authoritative Forge referee**. Actor-visible information remains limited to own objective, own hand and face-up battlefield, original frozen main-deck card-name/count multiset without ordering, legally visible opponent battlefield and referee-owned current dual life totals. No opponent private hand/library/top/order, face-down original identity, turn/phase/priority/stack, raw Forge Card/Game refs or shared private files.

These OS processes are **not a hardened security sandbox**. This stage verifies a narrow supported game and test-only referee path, not general Magic card handling, all callback families, complete damage assignment, full win/loss, or Ultimatus. Stage8P learned3 versus baseline4 remains failed. No `main` merge, deployment, promotion, training, or protected evaluation seeds 20261042–49 / 20261050–57 were used.

## Verification semantics

This file is a **documentation-only commit after the tested source commit**. It is not an additional tested software revision. Only the exact run/source/artifact above establishes Stage10I verified status.
