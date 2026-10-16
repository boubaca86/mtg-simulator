# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 143,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.71% | +5.81 pp | 93.79% | 64.98% |
| Eradicator S.T | 93.13% | 88.72% | +4.41 pp | 95.04% | 57.88% |
| Drop Pod S.T | 93.02% | 88.82% | +4.21 pp | 93.28% | 57.46% |
| Terminator S.T | 92.84% | 89.10% | +3.74 pp | 93.34% | 65.51% |
| Pyroclast Squad S.T | 92.82% | 89.12% | +3.70 pp | 93.63% | 64.41% |
| Exterminatus S.T | 88.95% | 92.30% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.36% | -2.07 pp | 92.83% | 4.22% |
| Infernus S.T | 91.99% | 90.79% | +1.20 pp | 93.13% | 61.53% |
| Whirlwind S.T | 91.19% | 92.24% | -1.06 pp | 93.50% | 47.84% |
| Servo-Skull | 91.21% | 91.68% | -0.47 pp | 91.26% | 21.97% |
| Demolitionist S.T | 91.66% | 91.41% | +0.25 pp | 93.31% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
