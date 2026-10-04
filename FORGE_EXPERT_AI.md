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

## Stage 6 — complete-action information-set search — LIVE REGRESSION GATE PASSED

Complete root-action identity now covers the spell/ability plus selected modes, announced X, targets, and relevant choices. The live Forge audit observes those identities independently in every hidden-world sample and fails CI if the exercised worlds select different complete actions.

The strengthened reproducible corpus completed 20/20 games, audited 825 complete root candidates across three hidden-world samples each, and observed 0 strategy-fusion divergences. The match result was 16–4 for Full information-set S.T against the Default benchmark.

This is a meaningful engineering gate, **not a universal proof** that no Magic position can expose another information-set bug. Stage 6 therefore remains permanently enabled as a regression gate during later expert-AI work. The earlier Stage 5 strength numbers remain historical engineering measurements rather than expert-strength evidence.

## Stage 7 — expert value model — CURRENT PRIORITY

Stage 7A begins with the data contract and leakage tests, not with a neural network. See `STAGE7_EXPERT_VALUE_MODEL.md`.

Training records must contain only information legally available to the acting player plus complete legal actions and outcome/search labels. Real unknown opponent hand identities and future library identities/order are forbidden model inputs. Determinized worlds may contribute only aggregated values; sampled hidden identities must not escape into features.

After deterministic extraction and leakage tests pass, Stage 7B may train a simple transparent baseline evaluator estimating game/match win probability. Learned-vs-baseline evaluation must use held-out seeds/positions and preserve the Stage 6 complete-action gate.

## Stage 8 — specialist decision modules

Add dedicated search/evaluation for London mulligans, attack subsets and block assignments, stack/priority responses, X-cost choices, modal spells, sacrifice/discard choices, sideboarding, and tutor/search decisions.

## Stage 9 — expert validation

Do not call the system expert-human until it passes external validation: strong human pilots on known decks, tactical puzzle suites with known expert lines, mirror matches where stronger search consistently defeats Forge Default, hidden-information leakage audits, and reproducible decision traces.

## Balance-testing rule

Until expert validation is reached, card-balance reports must name the AI used. Forge Default, Forge Full Simulation, single-determinization information-set search, and experimental multi-sample search are separate measurements and must not be treated as equivalent to expert-human balance results.
