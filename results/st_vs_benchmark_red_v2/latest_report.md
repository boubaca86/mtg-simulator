# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 269,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.64% | +5.81 pp | 93.73% | 64.96% |
| Eradicator S.T | 93.04% | 88.67% | +4.37 pp | 94.99% | 57.83% |
| Drop Pod S.T | 92.92% | 88.81% | +4.11 pp | 93.16% | 57.41% |
| Terminator S.T | 92.81% | 88.95% | +3.86 pp | 93.30% | 65.46% |
| Pyroclast Squad S.T | 92.75% | 89.07% | +3.68 pp | 93.55% | 64.29% |
| Exterminatus S.T | 88.90% | 92.22% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.34% | -2.21 pp | 92.39% | 4.24% |
| Infernus S.T | 91.92% | 90.71% | +1.21 pp | 93.04% | 61.36% |
| Whirlwind S.T | 91.12% | 92.15% | -1.03 pp | 93.52% | 47.81% |
| Servo-Skull | 91.07% | 91.62% | -0.56 pp | 91.09% | 22.04% |
| Demolitionist S.T | 91.58% | 91.34% | +0.24 pp | 93.25% | 47.05% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
