# Forge Balance Lab — Drop Pod S.T

Forge: 2.0.15  
Games per arm: 200  
RNG seed: 20261004

This experiment keeps the full 60-card S.T deck unchanged except for the four Drop Pod slots.

| Arm | S.T wins | Benchmark wins | Draws | S.T win rate | 95% Wilson CI |
|---|---:|---:|---:|---:|---:|
| baseline | 133 | 67 | 0 | 66.50% | 59.70%–72.68% |
| no_discount | 144 | 56 | 0 | 72.00% | 65.41%–77.76% |
| no_ping | 133 | 67 | 0 | 66.50% | 59.70%–72.68% |
| blank | 131 | 69 | 0 | 65.50% | 58.68%–71.74% |

## Estimated Drop Pod ability contribution

Positive numbers mean the original Drop Pod won more often than that ablated version.

| Comparison | Baseline − variant | Approx. 95% CI | Interpretation |
|---|---:|---:|---|
| baseline vs no_discount | -5.50 pp | -14.53 pp to +3.53 pp | Removes only the red-creature cost reduction; keeps Defender and the block ping. |
| baseline vs no_ping | +0.00 pp | -9.25 pp to +9.25 pp | Removes only the 1-damage block trigger; keeps Defender and the cost reduction. |
| baseline vs blank | +1.00 pp | -8.28 pp to +10.28 pp | Keeps only the 2-mana 0/3 Artifact Creature — Vehicle body with Defender; removes both benefits. |

## How to read this

This is an ability-ablation experiment, not a final global balance verdict. It tells us how much the Drop Pod text changes S.T performance against this particular Benchmark Red deck under Forge's current AI.

The arms reuse the same Forge RNG seed so the experiment is reproducible. Forge exposes a simulation seed (`-s`), but because the game trees can diverge after the card change, this version is not a strict one-to-one paired trial. A later Forge bridge can give us exact per-game paired seeds and stronger search AI.

Do not rebalance a card from one opponent archetype alone. The next expansion after this test is to repeat the same ablation protocol against multiple archetypes.
