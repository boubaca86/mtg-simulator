# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 63,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.75% | +5.65 pp | 93.65% | 64.92% |
| Eradicator S.T | 93.16% | 88.46% | +4.71 pp | 95.06% | 58.16% |
| Drop Pod S.T | 93.00% | 88.67% | +4.33 pp | 93.32% | 57.56% |
| Pyroclast Squad S.T | 92.83% | 88.96% | +3.87 pp | 93.62% | 64.16% |
| Terminator S.T | 92.70% | 89.20% | +3.50 pp | 93.20% | 65.47% |
| Exterminatus S.T | 89.16% | 92.16% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.29% | -2.06 pp | 92.99% | 4.20% |
| Infernus S.T | 91.85% | 90.86% | +0.99 pp | 93.00% | 61.34% |
| Whirlwind S.T | 91.20% | 92.04% | -0.85 pp | 93.44% | 47.85% |
| Demolitionist S.T | 91.61% | 91.32% | +0.29 pp | 93.29% | 47.34% |
| Servo-Skull | 91.28% | 91.57% | -0.29 pp | 91.36% | 21.97% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
