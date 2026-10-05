# Stage 8 — Counterfactual Action Ranking

Status: **candidate capture and contextual offline ranking implemented; no live promotion**.

The initial linear ranker's state terms canceled within each decision. The v3
comparison repairs this using action/context interactions and reports simpler
baselines under consistent tie-aware metrics. See
`STAGE8_CONTEXTUAL_RANKING_PROTOCOL.md` for the fixed experiment and information
boundary, and `STAGE8_CONTEXTUAL_RANKING_RESULTS.md` for the measured results.

Stage 7D-R established that public Forge-visible semantics add a small reproducible outcome-prediction signal. Stage 8 changes the question from "who is winning?" to "which legal action is better?"

## Objective

For one decision state, compare multiple **complete Forge-legal root actions** under the same information set and determine whether a learned ranker can predict Forge-refereed action quality on unseen seed families.

Forge remains the rules referee. Stage 8 does not implement Magic rules, reinterpret custom cards, or manufacture legal actions outside Forge.

## Required action identity

A candidate is the same action across information-set worlds only when its complete executable recipe matches, including at minimum:

- source object / ability identity
- targets and target ordering where relevant
- selected modes
- X value
- explicit choices already represented by the Stage 6 root identity

If the recipe is unavailable or illegal in a required world, that candidate is **not cross-world comparable** and must be rejected from the paired information-set label. Never replace it with that world's preferred target or mode.

## Information boundary

Features available to the ranker may contain only information legally available to the acting player at the root decision. In particular:

- opponent hand identities are forbidden unless publicly revealed by game rules;
- library identities/order and future draws are forbidden;
- face-down objects remain opaque;
- hidden-world samples may be used internally by the Forge search/referee, but hidden identities may not enter learner features;
- labels derived from hidden-world continuation must be aggregated before training and must not expose per-world hidden identities.

## Stage 8A — candidate capture

Extend the Forge instrumentation to emit, for each captured root decision:

1. a stable decision/information-set identifier;
2. the legal public-state feature vector already used by Stage 7D;
3. at least two distinct complete root-action identities when Forge exposes them;
4. for each retained candidate, its Forge-derived fixed-root aggregate score over the same required information-set samples;
5. sample count, replay-valid count and an explicit rejection reason for non-comparable candidates;
6. the root proposal selected by existing Forge search, for baseline comparison.

Do not capture only the winning candidate. A ranking dataset requires alternatives from the same decision.

The current hook captures selected search proposals, including phase probes that
may be deferred. It does not record every priority window or an executed-action
trajectory. Coverage is measured against captured proposals and reports games
without a rankable proposal explicitly.

## Stage 8B — reproducibility and anti-cheating contract

Before model training, a fixed seeded capture must prove:

- byte-identical candidate identities and aggregate labels across repeated runs;
- every retained candidate replays as the exact same complete action in every required world;
- no forbidden hidden-information fields occur in serialized learner inputs;
- candidate alternatives share exactly one root public information set;
- no candidate is relabeled by switching targets, modes, X or choices;
- Forge version and patch set are pinned and recorded.

Any violation fails closed.

## Stage 8C — paired offline ranking

Split by complete corpus seed family, never by candidate row or decision. Train only on training families. Evaluate on unseen families with ranking metrics such as:

- pairwise accuracy against Forge-derived aggregate ordering;
- top-1 agreement with the strongest fixed-root Forge candidate;
- normalized regret: `(best - selected) / (best - worst)` within a decision, or
  zero when all candidate scores tie; retain raw regret separately;
- coverage: fraction of decisions with at least two safely comparable candidates.

Report coverage beside accuracy so the system cannot appear strong merely by abstaining on difficult decisions.

At minimum compare:

1. existing Forge fixed-root choice / score ordering;
2. a simple public-count ranker;
3. Stage 7C visible-identity features;
4. Stage 7D public-semantic features.

All models must use the same held-out decisions and candidate sets.
The Forge choice is a consistency reference: the same search supplies the labels,
so perfect agreement is not independent evidence of playing strength. Uniform and
canonical action-only controls distinguish contextual value from memorized action
preferences. Accept any tied-best label and use uniform expected model tie-breaking.

## Promotion gate

`promotion_allowed` remains `false` through Stage 8A-C.

A later live-shadow gate may be considered only if the learned ranker, on a sufficiently large unseen corpus:

- improves ranking/regret reproducibly over simpler public baselines;
- has useful candidate coverage;
- passes all hidden-information and complete-action audits;
- does not alter custom card gameplay semantics;
- can be run in shadow mode without changing Forge's selected move.

Only after shadow validation should a separate experiment consider allowing the ranker to influence move selection.

## Why this is the next stage

The Stage 7D-R semantic advantage over visible identity is real but small (`0.485651` vs `0.487562` holdout game-mean log-loss). More winner-prediction tuning is therefore lower value than testing the capability we actually need: choosing among legal actions. Stage 8 converts the project from a state critic toward a player while keeping legality, hidden information and card semantics anchored in Forge.
