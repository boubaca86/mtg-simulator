# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 14,500
**S.T win rate:** 91.77%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.77% | 87.77% | +6.00 pp | 93.99% | 65.24% |
| Eradicator S.T | 93.63% | 88.41% | +5.22 pp | 95.22% | 58.25% |
| Drop Pod S.T | 93.09% | 89.26% | +3.83 pp | 93.43% | 57.63% |
| Pyroclast Squad S.T | 93.05% | 89.33% | +3.72 pp | 93.83% | 63.69% |
| Terminator S.T | 92.77% | 89.86% | +2.91 pp | 93.17% | 65.26% |
| Exterminatus S.T | 89.69% | 92.34% | -2.64 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.54% | 92.51% | -1.97 pp | 93.22% | 4.17% |
| Servo-Skull | 90.76% | 92.07% | -1.31 pp | 90.93% | 21.97% |
| Infernus S.T | 92.12% | 91.10% | +1.02 pp | 93.22% | 61.73% |
| Whirlwind S.T | 91.53% | 92.20% | -0.68 pp | 93.61% | 48.26% |
| Demolitionist S.T | 91.87% | 91.60% | +0.27 pp | 93.63% | 47.44% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
