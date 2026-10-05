# Stage 6/7 integrity repair — 2026-10-05 UTC

The earlier Stage 7B `0.400665` holdout log-loss is **invalid as model evidence**.
The earlier Stage 6 zero-divergence audit is also **not evidence of complete-action consistency**.
Neither result supports an expert-strength claim or promotion of a learned model.

## Reproduced findings

The saved [Stage 7B run](https://github.com/boubaca86/mtg-simulator/actions/runs/37241369454)
contains artifact `11318401398`. Its original JSONL has 796 rows from 24 games.
`results/integrity_20261005/data_audit.json` records file hashes and per-log findings.

1. **Wrong player indexing.** Pinned Forge `SimulateMatch.java` names seats `Ai(1)`
   and `Ai(2)`, while `Game.java` starts in-game player IDs at zero. The old labeler
   compared these directly. Of 338 observations in the 16 benchmark games whose
   logs contain no timeout, **111 labels change**. The full-search side's terminal
   record was **5 wins / 11 losses**, not 0 / 16. This is a retrospective outcome
   correction, not a clean strength benchmark.
2. **Timeouts accepted as results.** Five mirror games printed `Stopping slow match
   as draw` even though later terminal lines reported a winner. All four mirror
   source logs are quarantined in their entirety: interrupted execution can also
   contaminate later games in a process. No timeout is silently relabeled as a draw.
3. **Root choices absent from the audit.** Forge stores target, mode and card-choice
   decisions in child nodes, and merges them only in `getBestPlan()`. The old audit
   walked to the oldest node and serialized it before merging. The saved corpus
   contains **288 observations whose ability text mentions a target while the
   recorded target identity is `<none>`**. The source code establishes why.
4. **Schema drift.** The Stage 7A live check still required combined public zones
   after the extractor switched to own/opponent zones. Its actual failure was
   `AssertionError: {'graveyard_public', 'battlefield_public'}`. The count baseline
   also silently counted these missing combined zones as zero.
5. **Stack text treated as cards.** The extractor returned a string such as
   `[]==[]==[]`; feature code counted its characters and hashed them as identities.
   New capture exports a list of visible host-card names in stack order. All
   face-down objects are conservatively redacted, including in exile.

## Repairs and validation

- Winner display seats are explicitly converted to zero-based IDs. New rows also
  include the public player name, corrected label version and terminal winner ID.
- Labeling fails on timeouts, exceptions, missing results, incomplete world audit
  coverage, or any reported strategy-fusion divergence. Each expected game must
  have decisions and audit coverage.
- The Java root audit copies the first spell and merges only its child choices,
  stopping before the next spell. It does not call the mutating `getBestPlan()`.
  A regression executable tests the real patched Forge classes for target/mode
  divergence, X, explicit choices, source index, future-action isolation and repeat
  reads. This is a diagnostic repair, not complete-action search enforcement.
- Shared data checks reject obsolete labels, winner/actor inconsistencies, nested
  forbidden hidden fields, unverified opponent-known-card lists and malformed stacks.
- The paired comparison fixes its split before fitting. All observations from a
  complete game and all related games sharing a corpus seed stay together. Both
  partitions must contain real wins and losses; failure does not trigger seed shopping.
- Total counts, perspective-separated counts, and visible-identity hashes use the
  same dataset, split, optimizer and hyperparameters. A constant training-prior
  predictor is included. Reports contain game-weighted metrics, the exact split,
  dataset hash and weights. Promotion into Forge is always disabled.
- The fixed two-orientation benchmark retains all four original seeds. Mirror
  calibration is deferred because it was introduced to address artificial label
  imbalance and has a documented timeout problem. No model is trained on repaired
  historical data: the old action audit did not establish its information boundary.

## What is still open

Stage 6 still evaluates independently optimized world actions and reconstructs a
representative plan. Logging a disagreement is not a mechanism that forces a
single action across worlds. Exact complete-action replay/aggregation, stable
object identity for ambiguous same-name choices, and adversarial live positions
remain prerequisites for promoting a learned evaluator into actual play.

The extractor is a limited representation: visible names, zones, counts and life.
Hashing card names does not provide rules understanding, combat state, mana planning
or expert tactical judgment. Offline predictive loss is not a playing-strength test.

## Reproduce the historical audit

Download and unpack artifact `11318401398`, then run:

```sh
python stage7_reaudit_saved_logs.py /path/to/unpacked/artifact results/integrity_20261005/data_audit.json
python -m unittest -v test_stage7_label_outcomes.py test_stage7_compare_evaluators.py test_stage7_semantic_value_model.py
```

The updated GitHub workflows build pinned Forge and run the Java regression, live
reproducibility check and conditional offline comparison. A failed scientific gate
preserves its artifacts and blocks training. Its failure must not be called progress
in playing strength.
