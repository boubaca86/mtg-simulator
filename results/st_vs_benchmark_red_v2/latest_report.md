# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 10,500
**S.T win rate:** 92.06%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.90% | 88.30% | +5.60 pp | 94.11% | 65.63% |
| Eradicator S.T | 93.83% | 88.86% | +4.97 pp | 95.47% | 58.21% |
| Pyroclast Squad S.T | 93.34% | 89.53% | +3.81 pp | 94.11% | 64.31% |
| Drop Pod S.T | 93.19% | 89.89% | +3.30 pp | 93.48% | 57.52% |
| Exterminatus S.T | 89.60% | 92.73% | -3.13 pp | 0.00% | 0.00% |
| Terminator S.T | 93.02% | 90.21% | +2.81 pp | 93.39% | 65.39% |
| Vindicator S.T | 91.03% | 92.67% | -1.64 pp | 94.25% | 4.30% |
| Servo-Skull | 91.03% | 92.36% | -1.34 pp | 91.10% | 22.05% |
| Infernus S.T | 92.47% | 91.28% | +1.19 pp | 93.54% | 61.14% |
| Whirlwind S.T | 91.78% | 92.54% | -0.76 pp | 93.74% | 48.04% |
| Demolitionist S.T | 92.20% | 91.79% | +0.41 pp | 93.93% | 47.38% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
