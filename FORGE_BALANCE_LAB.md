# Forge S.T Balance Lab — Stage 2

Stage 2 measures individual-card contribution inside the validated full 60-card S.T deck using Forge as the rules engine.

## First target: Drop Pod S.T

The first experiment is an ability-ablation test with four otherwise identical S.T decks:

1. **baseline** — original Drop Pod S.T.
2. **no_discount** — same {2} 0/3 Artifact Creature — Vehicle with Defender and the block-damage trigger, but no red-creature cost reduction.
3. **no_ping** — same body with Defender and the cost reduction, but no block-damage trigger.
4. **blank** — same {2} 0/3 Artifact Creature — Vehicle with Defender, with both beneficial abilities removed.

Each arm plays the same Benchmark Red Forge deck.

## Reproducibility

Forge's command-line simulation mode supports `-s <seed>`. The workflow uses the same seed for every arm so the experiment can be reproduced exactly from the same code and inputs.

This is **not yet a strict per-game paired experiment**. Once the card text changes, the AI can make different decisions and consume random numbers differently, so later game states can diverge. A future Forge bridge/fork can assign a fresh deterministic seed to each individual game and expose legal actions to the stronger search AI.

## Statistics

The report records:

- S.T wins, Benchmark wins, and draws for every arm
- S.T win rate
- 95% Wilson interval for each win rate
- baseline-minus-variant win-rate difference
- approximate 95% interval for each difference

Positive baseline-minus-variant values mean the original Drop Pod improved S.T's win rate relative to that ablated version.

## Workflow

Run **Forge S.T Balance Lab — Drop Pod** in GitHub Actions.

Defaults:

- 200 games per arm
- 4 arms = 800 Forge games total
- Forge 2.0.15
- seed 20261004

The workflow accepts different game counts and seeds from the manual Run workflow form.

Results are saved to:

`results/forge_balance_drop_pod/`

including:

- `baseline.log`
- `no_discount.log`
- `no_ping.log`
- `blank.log`
- `status.json`
- `latest_report.md`

## What comes next

After the Drop Pod experiment is validated, the same ablation framework can be generalized to Terminator, Eradicator, Pyroclast, Whirlwind, Servitor, Demolitionist, Infernus, Vindicator, Servo-Skull, and Exterminatus, then repeated against multiple opponent archetypes.
