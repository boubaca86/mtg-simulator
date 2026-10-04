# Expert MTG AI — Forge Architecture

The target is **expert-human-level decision quality while Forge remains the rules referee**.

## Current architecture

- Forge 2.0.15: rules engine, zones, stack, priority, legality, combat, triggers, replacement effects.
- Custom S.T cards: Forge scripts generated from this repository.
- Cloud execution: GitHub Actions.
- Existing balance lab: Forge Default heuristic AI.

## Stage 3 — search AI enabled in headless Forge

Forge already contains two simulation options in its AI module:

- `AIOption.USE_HYBRID_SIMULATION`
- `AIOption.USE_FULL_SIMULATION`

Normal headless `sim` mode does not expose these options. `patch_forge_expert_cli.py` patches Forge's `SimulateMatch` command so each player can be assigned one of:

- `default`
- `hybrid`
- `full`

Example after patching:

```text
java -jar forge.jar sim \
  -d "ST Forge Full.dck" "Benchmark Red Forge.dck" \
  -x full full \
  -n 20 -s 20261004 -q
```

The workflow `Forge Expert AI Stage 3 — Full Simulation` builds Forge from the official `forge-2.0.15` tag with this small CLI patch and runs four arms:

1. default S.T vs default Benchmark
2. full-simulation S.T vs full-simulation Benchmark
3. full-simulation S.T vs default Benchmark
4. default S.T vs full-simulation Benchmark

The mixed arms are an initial strength check for Forge's search mode.

## Why Stage 3 is not the final expert AI

Forge's full-simulation mode is substantially closer to search-based play than the normal heuristic AI, but it is not a trained expert agent and should not be described as proven expert-human strength.

The final architecture should add the following layers.

### Stage 4 — strict information boundary

Create an immutable `AIView` that contains only information the acting player is entitled to know:

- own hand identities
- public battlefield, graveyard, stack, exile information
- opponent hand count, not hidden identities
- known/revealed hidden cards only
- no future library order

All expert-search code must accept `AIView`, not raw `Game` objects containing hidden zones.

### Stage 5 — information-set search

Search over plausible hidden opponent states instead of assuming perfect information.

For each decision:

1. generate legal actions through Forge
2. sample opponent hands/library states consistent with known information
3. simulate candidate actions across those samples
4. model opponent best responses
5. aggregate expected value and downside risk
6. choose the action with the highest estimated match win probability

This should use information-set MCTS or a related imperfect-information search method rather than ordinary perfect-information minimax.

### Stage 6 — expert value model

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

### Stage 7 — specialist decision modules

Add dedicated search/evaluation for:

- London mulligans
- attack subsets and block assignments
- stack/priority responses
- X-cost choices
- modal spells
- sacrifice/discard choices
- sideboarding
- tutor/search decisions

### Stage 8 — expert validation

Do not call the system expert-human until it passes external validation:

- head-to-head against strong human pilots on known decks
- tactical puzzle suites with known expert lines
- mirror matches where stronger search consistently defeats Forge Default
- no hidden-information leakage audits
- reproducible decision traces

## Balance-testing rule

Until Stage 8 is reached, card-balance reports must name the AI used. A Forge Default result and a Forge Full-Simulation result are separate measurements and should not be treated as equivalent to expert-human balance results.
