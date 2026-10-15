# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 130,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.67% | +5.87 pp | 93.82% | 64.99% |
| Eradicator S.T | 93.15% | 88.71% | +4.45 pp | 95.06% | 57.82% |
| Drop Pod S.T | 93.00% | 88.87% | +4.13 pp | 93.28% | 57.48% |
| Terminator S.T | 92.83% | 89.13% | +3.71 pp | 93.33% | 65.54% |
| Pyroclast Squad S.T | 92.83% | 89.13% | +3.70 pp | 93.63% | 64.44% |
| Exterminatus S.T | 88.98% | 92.30% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.32% | 92.34% | -2.02 pp | 92.71% | 4.23% |
| Infernus S.T | 92.00% | 90.77% | +1.24 pp | 93.13% | 61.57% |
| Whirlwind S.T | 91.20% | 92.24% | -1.04 pp | 93.47% | 47.88% |
| Servo-Skull | 91.24% | 91.68% | -0.44 pp | 91.28% | 21.93% |
| Demolitionist S.T | 91.64% | 91.46% | +0.17 pp | 93.28% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
