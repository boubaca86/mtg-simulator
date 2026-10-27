# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 267,000
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
| Eradicator S.T | 93.05% | 88.64% | +4.40 pp | 94.98% | 57.83% |
| Drop Pod S.T | 92.91% | 88.80% | +4.11 pp | 93.15% | 57.41% |
| Terminator S.T | 92.81% | 88.93% | +3.88 pp | 93.30% | 65.47% |
| Pyroclast Squad S.T | 92.75% | 89.05% | +3.70 pp | 93.55% | 64.28% |
| Exterminatus S.T | 88.87% | 92.22% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.33% | -2.20 pp | 92.37% | 4.24% |
| Infernus S.T | 91.92% | 90.69% | +1.22 pp | 93.04% | 61.36% |
| Whirlwind S.T | 91.11% | 92.15% | -1.04 pp | 93.50% | 47.82% |
| Servo-Skull | 91.06% | 91.62% | -0.55 pp | 91.09% | 22.05% |
| Demolitionist S.T | 91.58% | 91.33% | +0.25 pp | 93.24% | 47.04% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
