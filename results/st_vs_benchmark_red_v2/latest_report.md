# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 139,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.72% | +5.81 pp | 93.81% | 64.99% |
| Eradicator S.T | 93.14% | 88.75% | +4.38 pp | 95.05% | 57.89% |
| Drop Pod S.T | 93.03% | 88.84% | +4.19 pp | 93.29% | 57.44% |
| Terminator S.T | 92.86% | 89.11% | +3.74 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.84% | 89.14% | +3.70 pp | 93.64% | 64.42% |
| Exterminatus S.T | 88.95% | 92.32% | -3.36 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.37% | -2.07 pp | 92.79% | 4.23% |
| Infernus S.T | 92.01% | 90.79% | +1.22 pp | 93.15% | 61.55% |
| Whirlwind S.T | 91.20% | 92.25% | -1.05 pp | 93.51% | 47.86% |
| Servo-Skull | 91.20% | 91.70% | -0.49 pp | 91.25% | 21.94% |
| Demolitionist S.T | 91.66% | 91.45% | +0.20 pp | 93.31% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
