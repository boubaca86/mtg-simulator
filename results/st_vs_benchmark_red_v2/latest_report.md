# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 4,500
**S.T win rate:** 92.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.49% | 88.84% | +5.65 pp | 95.86% | 59.02% |
| Servitor | 94.03% | 89.54% | +4.49 pp | 94.23% | 65.13% |
| Exterminatus S.T | 89.36% | 93.40% | -4.04 pp | 0.00% | 0.00% |
| Pyroclast Squad S.T | 93.81% | 89.93% | +3.88 pp | 94.47% | 65.04% |
| Servo-Skull | 90.10% | 93.25% | -3.15 pp | 90.04% | 22.09% |
| Drop Pod S.T | 93.55% | 90.58% | +2.96 pp | 93.66% | 57.87% |
| Terminator S.T | 93.50% | 90.75% | +2.74 pp | 93.84% | 64.60% |
| Vindicator S.T | 91.46% | 93.15% | -1.69 pp | 93.47% | 4.42% |
| Demolitionist S.T | 93.06% | 91.57% | +1.49 pp | 94.38% | 48.20% |
| Infernus S.T | 93.00% | 91.68% | +1.31 pp | 93.87% | 61.22% |
| Whirlwind S.T | 92.61% | 92.40% | +0.21 pp | 94.04% | 48.11% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
