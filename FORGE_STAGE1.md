# Forge Stage 1 — S.T rules-engine integration

This repository now contains a manual Forge smoke test. The purpose of Stage 1 is to prove that custom S.T cards can be loaded by Forge and played by Forge's real Magic rules engine before we replace Forge's heuristic AI with our own stronger player.

## Forge target

- Forge release: 2.0.15
- Java: 21
- Workflow: `.github/workflows/forge-smoke.yml`
- Exporter: `forge_export.py`
- Forge simulation mode: `java -jar forge.jar sim ...`

## S.T cards ported in the first smoke-test bundle

1. Drop Pod S.T
   - Defender
   - Red creature spells cost {1} less
   - Deals 1 damage to a creature it blocks
2. Servitor
   - Haste
   - Returns to its owner's hand at the beginning of the next end step after dying
3. Terminator S.T
   - 3/4
   - Trample
4. Pyroclast Squad S.T
   - 3/3
   - Trample
   - Optional artifact destruction after combat damage to a player

The Forge scripts are generated into the Forge user directory by `forge_export.py` using the official custom-card script format.

## Cards deliberately deferred until the smoke test loads cleanly

- Infernus S.T
- Demolitionist S.T
- Whirlwind S.T
- Eradicator S.T
- Exterminatus S.T
- Vindicator S.T
- Servo-Skull

These cards have more complicated targeting, conditional land destruction, combat-damage replacement, X payments, attachment rules, or linked effects. They should be added one at a time after Stage 1 passes so a broken script is easy to isolate.

## Smoke decks

`forge_export.py` creates two 60-card constructed decks in Forge's user directory:

- `ST Forge Smoke.dck`
- `Benchmark Red Forge.dck`

The S.T smoke deck contains the four custom cards above plus simple existing red Magic cards so the first objective is rules-engine loading and legal gameplay, not final balance measurement.

## Running it

In GitHub Actions, manually run **Forge Rules Engine Smoke Test**.

The workflow:

1. Exports the custom set, card scripts, and decks into `~/.forge/`.
2. Downloads Forge 2.0.15.
3. Runs 20 headless Forge AI games.
4. Saves the Forge console log under `results/forge_smoke/`, including the process exit code.

A green run with exit code `0` proves the first custom-card bundle loads and completes games under Forge. A red run is still useful: the Forge error will identify the first card script or setup assumption that needs correction.

## After the smoke test

Stage 1b is to port the remaining seven S.T cards and run the complete 60-card S.T deck in Forge. After that, Stage 2 is to expose Forge legal actions/game state through a bridge so our own fair-information search AI can make every priority-window decision while Forge remains the referee.
