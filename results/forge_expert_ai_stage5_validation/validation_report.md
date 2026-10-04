# Forge Expert AI — Stage 5 Validation

Games per arm: 20  
Seed: 20261005

This run increases the sample size without changing card scripts or gameplay semantics. Confidence intervals are Wilson 95% intervals for the S.T win rate.

| Arm | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate | 95% CI |
|---|---:|---:|---:|---:|---:|---:|
| single_full_default | 12 | 8 | 0 | 0 | 60.0% | 38.7–78.1% |
| ensemble_full_default | 16 | 4 | 0 | 0 | 80.0% | 58.4–91.9% |
| single_default_full | 12 | 8 | 0 | 0 | 60.0% | 38.7–78.1% |
| ensemble_default_full | 17 | 3 | 0 | 0 | 85.0% | 64.0–94.8% |

## Decision rule

Treat this as evidence about stability and direction, not proof of expert-human strength. If three-sample search is stable and competitive, the next engineering stage should improve evaluation/opponent modelling rather than merely increasing search width.
