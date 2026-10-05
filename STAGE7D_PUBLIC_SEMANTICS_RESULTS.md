# Stage 7D — Public Card Semantics

Status: **replicated offline; live promotion remains disabled**.

## Stage 7D-R fixed-root replication

Workflow run `37293614107` completed successfully on 2026-10-05 using pinned Forge `2.0.15` after the fixed-root information-set repair.

Corpus:

- 32 complete Forge games
- four independent corpus seed groups (`20261004` through `20261007`)
- both deck orientations for every seed group
- 750 legal decision states total (75 + 83 + 85 + 75 + 110 + 98 + 123 + 101)
- three information-set worlds per decision
- 8 seed-isolated holdout games
- complete root-action identity regression passed for targets, modes, X, choices, source, isolation and immutability
- fixed-root aggregation regression passed for world disagreement, poor worlds, missing worlds, exact-plan replay and mean score

Paired holdout game-mean log-loss on the exact same held-out games:

| Representation | Holdout log-loss |
| --- | ---: |
| Training prior | 0.694776 |
| Combined counts | 0.928861 |
| Perspective-aware counts | 0.557899 |
| Stage 7C visible card identity | 0.487562 |
| **Stage 7D public card semantics** | **0.485651** |

Stage 7D beats Stage 7C by `0.001910729` log-loss (about **0.39% relative**) and beats perspective-aware counts by `0.072248` (about **12.95% relative**). The improvement over visible identity is much smaller than the earlier 16-game estimate, so the correct conclusion is modest: public Forge-visible semantics add reproducible signal, but the effect is small on this benchmark.

The workflow's replication gates all passed: at least 32 complete games, at least eight seed-isolated holdout games, identical holdouts for every evaluator, public semantics better than perspective counts, public semantics no worse than visible identity, fixed-root search policy only, and `promotion_allowed == false`.

## Historical small-corpus result

The earlier repaired 16-game run `37269454156` reported 0.441409 for visible identity and 0.399694 for public semantics. That larger apparent gain should no longer be used as the primary estimate; the 32-game fixed-root replication above supersedes it.

The intervening 32-game run `37286004175` was invalidated when its action audit detected root-action divergence across hidden worlds. That failure led to the fixed-root information-set repair. It is not included as evidence for evaluator quality.

## Integrity constraints

The result is valid only while all of the following remain true:

1. Forge remains the rules referee and sole source of legal actions/gameplay semantics.
2. Opponent hand identities and both library identities/order remain unavailable to the learner.
3. Face-down objects remain opaque.
4. Train/holdout separation is by corpus seed family / complete game, never individual decision row.
5. Every compared representation uses the exact same held-out games.
6. No custom-card rules are modified to improve evaluator performance.
7. Learned evaluation remains offline until a separate action-quality gate passes.
8. A root action is compared across determinizations only when its complete executable identity (source, targets, modes, X and choices) is the same legal action.

## Next gate: Stage 8 — counterfactual action ranking

Outcome prediction is not playing strength. Stage 8 must therefore capture **multiple Forge-legal complete root actions from the same public information set**, evaluate the same complete action across the legal hidden-world samples, and test whether an offline learner ranks actions in a way that predicts superior Forge-refereed continuations on held-out seed families.

Stage 8 must fail closed when a complete action cannot be replayed legally in every required information-set world. It must never substitute a different target/mode/X/choice in another world and call that the same action. Candidate labels must come from Forge-refereed continuation outcomes or another explicitly documented Forge-derived target, not from silently changing card rules or using hidden cards as features.

Initial Stage 8 remains **offline only**. No learned action ranker may influence live move selection until it beats a reproducible Forge-search baseline on held-out decisions and passes the information-set/action-identity audits.
