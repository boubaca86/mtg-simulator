# Forge Expert AI — Stage 5 Multi-Sample Information-Set Search

Forge source tag: forge-2.0.15  
Games per arm: 4  
Seed: 20261004

Stage 5 evaluates a root action across several plausible opponent hidden states instead of trusting a single determinization.

| Arm | Samples | S.T AI | Benchmark AI | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate |
|---|---:|---|---|---:|---:|---:|---:|---:|
| single_full_default | 1 | full | default | 4 | 0 | 0 | 0 | 100.0% |
| ensemble_full_default | 3 | full | default | 3 | 1 | 0 | 0 | 75.0% |
| single_default_full | 1 | default | full | 3 | 1 | 0 | 0 | 75.0% |
| ensemble_default_full | 3 | default | full | 3 | 1 | 0 | 0 | 75.0% |

## Search behavior

For the three-sample arms, every legal root spell/ability candidate is evaluated in three hidden-world determinizations consistent with the information available to the acting player. Scores are averaged, and the candidate with the best mean value is selected.

Recursive lookahead within each sampled world still uses Forge Full Simulation. To avoid strategy fusion, the executable plan retains only the selected root action; subsequent real priority windows perform a fresh information-set search.

This stage is an engineering and strength smoke test. The game count is intentionally small. A later stage must increase samples, add downside/risk handling, improve opponent-response search, and replace the hand-written state evaluator with a learned win-probability model.
