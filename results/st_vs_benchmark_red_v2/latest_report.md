# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 267,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.63% | +5.81 pp | 93.73% | 64.96% |
| Eradicator S.T | 93.05% | 88.65% | +4.40 pp | 94.98% | 57.83% |
| Drop Pod S.T | 92.92% | 88.80% | +4.12 pp | 93.16% | 57.41% |
| Terminator S.T | 92.81% | 88.94% | +3.87 pp | 93.30% | 65.47% |
| Pyroclast Squad S.T | 92.75% | 89.05% | +3.69 pp | 93.55% | 64.29% |
| Exterminatus S.T | 88.88% | 92.22% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.33% | -2.20 pp | 92.36% | 4.24% |
| Infernus S.T | 91.92% | 90.70% | +1.22 pp | 93.04% | 61.36% |
| Whirlwind S.T | 91.12% | 92.15% | -1.03 pp | 93.50% | 47.82% |
| Servo-Skull | 91.06% | 91.62% | -0.56 pp | 91.09% | 22.04% |
| Demolitionist S.T | 91.58% | 91.34% | +0.24 pp | 93.24% | 47.05% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
