# Stage 10J — VERIFIED strict UTF-8 actor reply framing

**Status: VERIFIED** on exact tested source commit `6693974a5b7a5fbfe0f86a7a1ccbada7fa96636f` (October 11, 2026). Feature branch remains **unmerged**.

## Independent evidence

- GitHub Actions SUCCESS: https://github.com/boubaca86/mtg-simulator/actions/runs/38184692274
- Pinned upstream Forge release: annotated tag `forge-2.0.15`, peeled to commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Genuine pinned Forge TestNG class `forge.ai.Stage10JReplyFramingTest`: **59 executed, 0 failures, 0 errors, 0 skipped**.
- Maven exit **0**, **BUILD SUCCESS**.
- Independent strict exact 59-method Surefire XML verification `forge_tests/verify_stage10j_surefire.py`: **PASS**, `executed_methods=59`.
- Stage10D and Stage10E persistent two-actor IPC canaries: PASS.
- Stage10H strict JSON input canary (56 malformed requests across four actor types): PASS.
- Immutable CI evidence artifact [stage10j-reply-framing-38184692274-1](https://github.com/boubaca86/mtg-simulator/actions/runs/38184692274/artifacts/11718509619), ID **11718509619**, **75,440 bytes**.
- The same successful source commit produced the workflow log with all these results.

## Scope

`forge_tests/Stage10JReplyFramingTest.java` is test-only trusted Forge referee code, built from Stage10I without changing the external actor's observation or legal decision schema. Its existing bounded reply reader now gets text through a strict UTF-8 `CharsetDecoder` configured with `CodingErrorAction.REPORT`, rather than accepting replacement characters for malformed or unmappable bytes.

Two added genuine TestNG methods:
1. `refereeRejectsTruncatedUnterminatedAndOverlongReplyFrames` — terminated streams with no LF, truncated JSON, overlong reply frames must be denied, while two valid independently terminated frames are handled separately.
2. `refereeRejectsMalformedUtf8AndReadsExactMultibyteFrames` — malformed/incomplete UTF-8 and encoded surrogate sequences are rejected. Valid multibyte UTF-8 survives the parser exactly, but an unexpected additional field still fails the authoritative reply schema.

Both actor processes remain separate persistent Python PIDs; the sole Forge referee still controls actual decisions, legal actions, mana/costs, priority/stack, draw/shuffle, combat/damage/life. Actor views remain limited to their own objective, own hand and own face-up battlefield, frozen original main-deck name/count multiset without order, legally visible opponent battlefield, and two referee-owned life totals. No opponent private zones or raw Card/Game reference.

## Research

- Official WotC rules (2026-09-25): https://magic.wizards.com/en/rules (CR 400.2 hidden zones; 509–510 combat; 702.7 first strike).
- Pinned Card-Forge `forge-2.0.15` at commit: https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676
- Cowling, Powley & Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Explicit remaining limitations

- A persistent actor that stalls without emitting a newline can still block a synchronous read: this stage only enforces bounded/truncated EOF behavior and strict UTF-8 decoding, **not an elapsed-time read timeout**. The next research-first milestone can focus on a bounded response deadline that fails closed without increasing actor count or granting privileged state.
- Separate actor PIDs are NOT an OS sandbox; not a production hardened or complete Magic simulator.
- Inherited callbacks beyond the validated narrow supported set, complex damage/cost rollback, Stage9E exile aliases, Ultimatus win rules and full-game completion remain outside this proof.
- No protected evaluation seeds (20261042–49 or 20261050–57), main merge, deployment, training, or promotion. Stage8P learned3 vs baseline4 remains FAILED.

This verification document is committed **after** the successfully tested software SHA; it does not claim another software run for a docs-only commit.
