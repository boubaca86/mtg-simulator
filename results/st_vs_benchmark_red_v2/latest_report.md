# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 149,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.72% | +5.79 pp | 93.80% | 64.96% |
| Eradicator S.T | 93.13% | 88.71% | +4.42 pp | 95.04% | 57.87% |
| Drop Pod S.T | 93.02% | 88.81% | +4.22 pp | 93.29% | 57.43% |
| Terminator S.T | 92.84% | 89.09% | +3.75 pp | 93.34% | 65.53% |
| Pyroclast Squad S.T | 92.82% | 89.13% | +3.68 pp | 93.62% | 64.41% |
| Exterminatus S.T | 88.94% | 92.30% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.37% | -2.10 pp | 92.82% | 4.21% |
| Infernus S.T | 92.00% | 90.76% | +1.23 pp | 93.14% | 61.54% |
| Whirlwind S.T | 91.18% | 92.25% | -1.07 pp | 93.50% | 47.86% |
| Servo-Skull | 91.20% | 91.68% | -0.48 pp | 91.25% | 21.99% |
| Demolitionist S.T | 91.66% | 91.40% | +0.26 pp | 93.31% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
