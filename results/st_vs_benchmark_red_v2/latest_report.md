# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 16,500
**S.T win rate:** 91.70%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.73% | 87.65% | +6.08 pp | 93.96% | 65.24% |
| Eradicator S.T | 93.52% | 88.37% | +5.15 pp | 95.20% | 58.37% |
| Drop Pod S.T | 93.04% | 89.15% | +3.89 pp | 93.37% | 57.62% |
| Pyroclast Squad S.T | 92.94% | 89.32% | +3.62 pp | 93.69% | 63.88% |
| Terminator S.T | 92.64% | 89.90% | +2.74 pp | 93.02% | 65.20% |
| Exterminatus S.T | 89.60% | 92.28% | -2.68 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.43% | 92.47% | -2.03 pp | 92.68% | 4.14% |
| Infernus S.T | 92.05% | 91.04% | +1.01 pp | 93.19% | 61.70% |
| Servo-Skull | 90.94% | 91.93% | -0.99 pp | 91.04% | 22.04% |
| Whirlwind S.T | 91.35% | 92.32% | -0.97 pp | 93.58% | 48.13% |
| Demolitionist S.T | 91.88% | 91.39% | +0.48 pp | 93.61% | 47.53% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
