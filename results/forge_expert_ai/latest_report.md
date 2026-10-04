# Forge Expert-AI Stage 3 Benchmark

Forge source tag: forge-2.0.15  
Games per arm: 10  
Seed: 20261004

This benchmark exposes Forge's built-in `USE_FULL_SIMULATION` AI to headless CLI games and compares it with the normal heuristic AI.

| Arm | S.T AI | Benchmark AI | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate |
|---|---|---|---:|---:|---:|---:|---:|
| default_default | default | default | 7 | 3 | 0 | 0 | 70.0% |
| full_full | full | full | 7 | 3 | 0 | 0 | 70.0% |
| full_default | full | default | 8 | 2 | 0 | 0 | 80.0% |
| default_full | default | full | 7 | 3 | 0 | 0 | 70.0% |

## Interpretation

The mixed arms are the most informative: `full_default` asks whether full-search S.T gains an edge against normal Forge AI, while `default_full` asks whether a full-search Benchmark Red gains an edge against normal S.T AI.

Passing this benchmark means the stronger search mode is operational in cloud simulations. It does **not** by itself establish expert-human strength. The next architecture step is a hidden-information-safe information-set planner with deeper tree search and a learned value model.
