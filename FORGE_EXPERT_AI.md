# Expert MTG AI — Forge Architecture

The target is **expert-human-level decision quality while Forge remains the rules referee**.

## Current architecture

- Forge 2.0.15: rules engine, zones, stack, priority, legality, combat, triggers, replacement effects.
- Custom S.T cards: Forge scripts generated from this repository.
- Cloud execution: GitHub Actions.
- Existing balance lab: Forge Default heuristic AI.

## Stage 3 — search AI enabled in headless Forge — PASSED

Forge already contains two simulation options in its AI module:

- `AIOption.USE_HYBRID_SIMULATION`
- `AIOption.USE_FULL_SIMULATION`

Normal headless `sim` mode does not expose these options. `patch_forge_expert_cli.py` patches Forge's `SimulateMatch` command so each player can be assigned one of:

- `default`
- `hybrid`
- `full`

The Stage 3 workflow successfully built patched Forge and completed all four benchmark arms using the real full-simulation mode. The initial 10-game-per-arm smoke result was:

- default/default: S.T 7–3
- full/full: S.T 7–3
- full/default: S.T 8–2
- default/full: S.T 7–3

This sample is intentionally too small for a strength claim; its purpose was to prove the search mode works in cloud simulations.

## Stage 4 — information boundary — PASSED

Forge full simulation copies the real `Game`, including hidden zones. Even though its board evaluator uses opponent hand count rather than opponent hand identities, search should not inherit the actual unknown hand or actual future library order.

`patch_forge_information_set.py` adds a determinization boundary inside `GameSimulator` when Forge is launched with:

```text
-Dforge.expert.infoset=true
```

For each search copy it:

- preserves all public game state
- preserves the acting player's own hidden information
- preserves cards the acting player is explicitly allowed to look at
- preserves opponent hand and library sizes
- pools unknown opponent hand and library cards
- resamples the unknown hand and future library order
- uses a local deterministic RNG seed built only from public/count state, so it does not consume the real game's RNG stream

The Stage 4 workflow completed both validation arms with no crashes or timeouts:

- raw full simulation: S.T 7–3
- information-set full simulation: S.T 7–3

The 10-game arms are only an engineering validation; they are not a strength or balance claim.

## Stage 5 — multi-sample information-set search — IMPLEMENTED / VALIDATION RUNNING

`patch_forge_infoset_ensemble.py` upgrades the root decision from one hidden-world determinization to an ensemble of plausible hidden worlds.

For each root decision it:

1. generates legal candidate spells/abilities through Forge
2. generates multiple opponent hand/library determinizations consistent with known information
3. evaluates each root candidate in every sampled world
4. allows Forge Full Simulation to model responses/lookahead inside each sampled world
5. averages candidate values across the sampled worlds
6. selects the action with the best mean value
7. retains only the chosen root action, then re-searches at the next real priority window instead of following a multi-step plan tailored to one sampled hidden world

Stage 5 also keeps a sampled world internally consistent during recursive lookahead instead of re-determinizing at each recursive node.

The initial workflow compares one-sample and three-sample search in both mixed-strength directions:

- full-search S.T vs default Benchmark
- default S.T vs full-search Benchmark

This is **root-sampled information-set search**, not yet full IS-MCTS.

## Stage 6 — expert value model

Replace hand-written board scores with a learned evaluator that estimates match win probability from a legal-information state.

Training records should include:

- state features
- legal actions
- chosen action
- eventual game result
- matchup/archetype
- play/draw
- mulligan decisions

Self-play can then improve the evaluator continuously.

## Stage 7 — specialist decision modules

Add dedicated search/evaluation for:

- London mulligans
- attack subsets and block assignments
- stack/priority responses
- X-cost choices
- modal spells
- sacrifice/discard choices
- sideboarding
- tutor/search decisions

## Stage 8 — expert validation

Do not call the system expert-human until it passes external validation:

- head-to-head against strong human pilots on known decks
- tactical puzzle suites with known expert lines
- mirror matches where stronger search consistently defeats Forge Default
- no hidden-information leakage audits
- reproducible decision traces

## Balance-testing rule

Until Stage 8 is reached, card-balance reports must name the AI used. Forge Default, Forge Full Simulation, single-determinization information-set search, and multi-sample information-set search are separate measurements and should not be treated as equivalent to expert-human balance results.
