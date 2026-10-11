# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 76,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.67% | +5.84 pp | 93.75% | 64.95% |
| Eradicator S.T | 93.15% | 88.61% | +4.54 pp | 95.07% | 58.09% |
| Drop Pod S.T | 93.05% | 88.68% | +4.36 pp | 93.33% | 57.64% |
| Pyroclast Squad S.T | 92.85% | 89.04% | +3.81 pp | 93.66% | 64.16% |
| Terminator S.T | 92.78% | 89.13% | +3.64 pp | 93.29% | 65.65% |
| Exterminatus S.T | 89.25% | 92.19% | -2.93 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.38% | -2.18 pp | 92.79% | 4.20% |
| Whirlwind S.T | 91.16% | 92.21% | -1.05 pp | 93.45% | 47.83% |
| Infernus S.T | 91.89% | 90.90% | +0.99 pp | 93.03% | 61.36% |
| Servo-Skull | 91.28% | 91.63% | -0.35 pp | 91.35% | 21.96% |
| Demolitionist S.T | 91.66% | 91.35% | +0.31 pp | 93.32% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
