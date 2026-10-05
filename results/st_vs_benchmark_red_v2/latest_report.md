# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 13,500
**S.T win rate:** 91.75%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.73% | 87.79% | +5.94 pp | 93.95% | 65.14% |
| Eradicator S.T | 93.61% | 88.36% | +5.25 pp | 95.23% | 58.24% |
| Drop Pod S.T | 93.07% | 89.21% | +3.86 pp | 93.36% | 57.67% |
| Pyroclast Squad S.T | 93.06% | 89.23% | +3.82 pp | 93.82% | 63.86% |
| Terminator S.T | 92.75% | 89.83% | +2.91 pp | 93.15% | 65.30% |
| Exterminatus S.T | 89.62% | 92.33% | -2.71 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.48% | 92.50% | -2.01 pp | 93.30% | 4.20% |
| Servo-Skull | 90.78% | 92.03% | -1.26 pp | 90.92% | 22.04% |
| Infernus S.T | 92.16% | 90.98% | +1.18 pp | 93.21% | 61.62% |
| Whirlwind S.T | 91.43% | 92.30% | -0.87 pp | 93.56% | 48.18% |
| Demolitionist S.T | 91.91% | 91.45% | +0.46 pp | 93.70% | 47.48% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
