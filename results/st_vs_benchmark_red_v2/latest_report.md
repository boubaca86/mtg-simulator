# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 170,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.70% | +5.81 pp | 93.80% | 65.00% |
| Eradicator S.T | 93.13% | 88.70% | +4.43 pp | 95.03% | 57.86% |
| Drop Pod S.T | 92.99% | 88.86% | +4.13 pp | 93.24% | 57.50% |
| Terminator S.T | 92.84% | 89.09% | +3.74 pp | 93.32% | 65.48% |
| Pyroclast Squad S.T | 92.81% | 89.12% | +3.69 pp | 93.60% | 64.40% |
| Exterminatus S.T | 88.99% | 92.28% | -3.28 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.34% | -2.05 pp | 92.67% | 4.21% |
| Infernus S.T | 92.01% | 90.73% | +1.28 pp | 93.14% | 61.47% |
| Whirlwind S.T | 91.18% | 92.24% | -1.06 pp | 93.50% | 47.79% |
| Servo-Skull | 91.19% | 91.68% | -0.49 pp | 91.23% | 22.04% |
| Demolitionist S.T | 91.64% | 91.43% | +0.21 pp | 93.28% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
