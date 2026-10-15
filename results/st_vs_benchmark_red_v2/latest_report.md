# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 131,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.68% | +5.86 pp | 93.82% | 65.00% |
| Eradicator S.T | 93.14% | 88.73% | +4.41 pp | 95.06% | 57.83% |
| Drop Pod S.T | 93.02% | 88.85% | +4.17 pp | 93.29% | 57.47% |
| Terminator S.T | 92.83% | 89.13% | +3.71 pp | 93.34% | 65.52% |
| Pyroclast Squad S.T | 92.83% | 89.13% | +3.69 pp | 93.63% | 64.43% |
| Exterminatus S.T | 88.99% | 92.30% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.31% | 92.35% | -2.04 pp | 92.68% | 4.24% |
| Infernus S.T | 92.01% | 90.76% | +1.25 pp | 93.15% | 61.57% |
| Whirlwind S.T | 91.19% | 92.25% | -1.05 pp | 93.48% | 47.87% |
| Servo-Skull | 91.23% | 91.68% | -0.45 pp | 91.28% | 21.93% |
| Demolitionist S.T | 91.64% | 91.46% | +0.18 pp | 93.28% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
