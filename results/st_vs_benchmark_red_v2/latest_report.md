# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 29,000
**S.T win rate:** 91.62%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.58% | 87.71% | +5.87 pp | 93.83% | 65.26% |
| Eradicator S.T | 93.32% | 88.51% | +4.81 pp | 95.07% | 58.38% |
| Drop Pod S.T | 93.05% | 88.88% | +4.17 pp | 93.36% | 57.81% |
| Pyroclast Squad S.T | 92.95% | 89.09% | +3.86 pp | 93.68% | 63.91% |
| Terminator S.T | 92.68% | 89.58% | +3.10 pp | 93.12% | 65.34% |
| Exterminatus S.T | 89.43% | 92.22% | -2.79 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.46% | -2.23 pp | 92.31% | 4.17% |
| Infernus S.T | 91.90% | 91.10% | +0.80 pp | 93.04% | 61.57% |
| Demolitionist S.T | 91.84% | 91.23% | +0.61 pp | 93.56% | 47.41% |
| Whirlwind S.T | 91.47% | 91.90% | -0.43 pp | 93.70% | 47.75% |
| Servo-Skull | 91.29% | 91.72% | -0.43 pp | 91.30% | 22.00% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
