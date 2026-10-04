# Expert MTG AI — Forge Architecture

The target is **expert-human-level decision quality while Forge remains the rules referee**.

## Current architecture

- Forge 2.0.15: rules engine, zones, stack, priority, legality, combat, triggers, replacement effects.
- Custom S.T cards: Forge scripts generated from this repository.
- Cloud execution: GitHub Actions.
- Existing balance lab: Forge Default heuristic AI.

## Stage 3 — search AI enabled in headless Forge — PASSED

Forge already contains `AIOption.USE_HYBRID_SIMULATION` and `AIOption.USE_FULL_SIMULATION`. `patch_forge_expert_cli.py` exposes default/hybrid/full per player in headless simulations. The cloud smoke test passed. Its small sample was an engineering validation, not a strength claim.

## Stage 4 — information boundary — PASSED

`patch_forge_information_set.py` prevents a simulation copy from simply inheriting the real unknown opponent hand and future library order. It preserves public state, acting-player knowledge, known/revealed hidden cards, and zone sizes, while resampling unknown opponent hand/library cards with a local deterministic RNG derived without hidden card identities. Stage 4 completed both 10-game validation arms without crashes or timeouts.

## Stage 5 — multi-sample information-set search — ENGINEERING PASS; STRENGTH CLAIM QUARANTINED

`patch_forge_infoset_ensemble.py` evaluates each root spell/ability across multiple plausible hidden worlds. The 20-games-per-arm validation completed successfully and persisted its results:

- single-sample Full S.T vs Default: 12–8 (60%)
- three-sample Full S.T vs Default: 16–4 (80%)
- Default S.T vs single-sample Full Benchmark: 12–8 (60%)
- Default S.T vs three-sample Full Benchmark: 17–3 (85%)
- zero draws and zero timeouts

These results establish that multi-world search runs stably and that the directional signal is worth investigating. They **do not establish expert strength**.

### Stage 5 audit finding: strategy fusion

A post-validation information-set audit found an important remaining leak. `evaluateSa(...)` constructs a fresh `SpellAbilityChoicesIterator` inside each determinized world. Consequently, although the root spell/ability identity is held constant across samples, Forge can independently optimize modes, targets, X values, or card choices in each hidden world before their scores are averaged.

That means the aggregate can score an impossible policy: one concrete target in hidden world A and another concrete target in hidden world B, even though the real player must choose one action without knowing which hidden world is real. This is the imperfect-information search problem known as **strategy fusion**.

Therefore the Stage 5 win-rate numbers are quarantined from expert-strength claims. They remain valid engineering/stability measurements only.

See `STAGE6_INFORMATION_SET_AUDIT.md` for the action-consistency requirement.

## Stage 6 — complete-action information-set search — CURRENT PRIORITY

Before adding a learned value model, search must be information-set correct at the complete-action level.

A candidate root action must include the spell/ability plus all decisions that must be fixed using current legal information: selected modes, announced X, public targets, and relevant public sacrifice/discard/other choices. The **same complete action** must then be evaluated in every hidden-world determinization. Per-world re-optimization of those decisions is forbidden.

Validation requires an adversarial fixture where different hidden worlds would tempt the perfect-information simulator toward different public targets. The information-set AI must select one target by aggregate value and use that same target in every sample.

Only after this passes should we train or integrate an expert value model; otherwise self-play risks learning labels contaminated by hidden-world strategy fusion.

## Stage 7 — expert value model

Replace hand-written board scores with a learned evaluator estimating match win probability from legal-information state. Training records should include state features, legal actions, chosen action, eventual result, matchup/archetype, play/draw, and mulligan decisions. Training data must come from information-set-clean search.

## Stage 8 — specialist decision modules

Add dedicated search/evaluation for London mulligans, attack subsets and block assignments, stack/priority responses, X-cost choices, modal spells, sacrifice/discard choices, sideboarding, and tutor/search decisions.

## Stage 9 — expert validation

Do not call the system expert-human until it passes external validation: strong human pilots on known decks, tactical puzzle suites with known expert lines, mirror matches where stronger search consistently defeats Forge Default, hidden-information leakage audits, and reproducible decision traces.

## Balance-testing rule

Until expert validation is reached, card-balance reports must name the AI used. Forge Default, Forge Full Simulation, single-determinization information-set search, and experimental multi-sample search are separate measurements and must not be treated as equivalent to expert-human balance results.
