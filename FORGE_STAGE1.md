# Forge Stage 1 — S.T rules-engine integration

Stage 1A proved that Forge 2.0.15 can load S.T custom cards and complete headless games under Forge's Magic rules engine. Stage 1B now validates the complete 60-card S.T deck before work begins on the external expert-search AI bridge.

## Forge target

- Forge release: 2.0.15
- Java: 21
- Workflow: `.github/workflows/forge-smoke.yml`
- Exporter: `forge_export.py`
- Forge simulation mode: `java -jar forge.jar sim ...`

## Stage 1A — passed

The first smoke bundle contained:

1. Drop Pod S.T
2. Servitor
3. Terminator S.T
4. Pyroclast Squad S.T

Forge successfully loaded the custom set and completed 20/20 games. Those historical results remain under `results/forge_smoke/`.

## Stage 1B — full S.T deck

The exporter now contains all eleven custom S.T cards:

1. Drop Pod S.T
2. Servitor
3. Terminator S.T
4. Pyroclast Squad S.T
5. Infernus S.T
6. Demolitionist S.T
7. Whirlwind S.T
8. Eradicator S.T
9. Exterminatus S.T
10. Vindicator S.T
11. Servo-Skull

The seven newly ported cards exercise Forge features including unblocked-attack triggers, combat-damage suppression, conditional land targeting, same-name tap costs, cast restrictions, variable red-mana payments, ward, Fortification attachment, and attachment-related triggers.

## Full 60-card S.T deck

`ST Forge Full.dck` contains:

- 24 Mountain
- 4 Drop Pod S.T
- 4 Servitor
- 4 Terminator S.T
- 4 Pyroclast Squad S.T
- 4 Infernus S.T
- 4 Demolitionist S.T
- 4 Whirlwind S.T
- 4 Eradicator S.T
- 2 Vindicator S.T
- 1 Servo-Skull
- 1 Exterminatus S.T

Total: 60 cards.

The original four-card smoke deck is still generated as `ST Forge Smoke.dck` for regression testing.

## Stage 1B workflow

In GitHub Actions, manually run **Forge Full S.T Rules Test**.

The workflow:

1. Exports all 11 custom card scripts plus the decks into `~/.forge/`.
2. Downloads Forge 2.0.15.
3. Runs 20 headless games: `ST Forge Full.dck` vs `Benchmark Red Forge.dck`.
4. Stores the log and a machine-readable summary under `results/forge_full/`.
5. Verifies that Forge actually printed 20 game-result records; a clean Java exit with fewer completed games is treated as a failure.

`results/forge_full/status.json` records Forge's exit code, completed-game count, S.T wins, Benchmark Red wins, and draws.

## Important validation note

Stage 1B is a rules-engine integration test, not a final balance verdict. The more complex custom scripts are first-pass Forge translations of the current S.T oracle text. A green run proves they parse and survive real games; after that, targeted rules tests should verify each unusual interaction individually before large-scale balance simulations.

## After Stage 1B

Stage 2 will expose Forge game state and legal decisions through a bridge. Forge remains the referee for rules, stack, triggers, state-based actions and priority, while our own fair-information search AI chooses actions. That is the foundation for stronger expert-style play and paired individual-card balance experiments.
