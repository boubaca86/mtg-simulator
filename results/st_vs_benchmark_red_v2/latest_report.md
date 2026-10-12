# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 90,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.59% | +5.95 pp | 93.80% | 64.83% |
| Eradicator S.T | 93.14% | 88.60% | +4.54 pp | 95.09% | 58.06% |
| Drop Pod S.T | 93.01% | 88.72% | +4.29 pp | 93.32% | 57.60% |
| Pyroclast Squad S.T | 92.86% | 88.96% | +3.89 pp | 93.67% | 64.26% |
| Terminator S.T | 92.75% | 89.15% | +3.60 pp | 93.26% | 65.57% |
| Exterminatus S.T | 89.09% | 92.22% | -3.13 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.34% | -2.14 pp | 92.74% | 4.24% |
| Whirlwind S.T | 91.14% | 92.21% | -1.07 pp | 93.41% | 47.87% |
| Infernus S.T | 91.90% | 90.84% | +1.06 pp | 93.06% | 61.39% |
| Servo-Skull | 91.28% | 91.61% | -0.33 pp | 91.36% | 21.97% |
| Demolitionist S.T | 91.61% | 91.40% | +0.21 pp | 93.29% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
