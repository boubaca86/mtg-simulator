# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 3,500
**S.T win rate:** 92.77%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.76% | 89.03% | +5.73 pp | 96.22% | 59.03% |
| Pyroclast Squad S.T | 94.39% | 89.51% | +4.88 pp | 95.02% | 64.89% |
| Servitor | 94.01% | 90.29% | +3.71 pp | 94.17% | 65.23% |
| Exterminatus S.T | 90.05% | 93.52% | -3.46 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.87% | 93.86% | -2.99 pp | 92.99% | 4.49% |
| Terminator S.T | 93.68% | 91.10% | +2.58 pp | 94.01% | 64.37% |
| Demolitionist S.T | 93.65% | 91.15% | +2.50 pp | 94.98% | 48.34% |
| Drop Pod S.T | 93.57% | 91.24% | +2.34 pp | 93.70% | 58.03% |
| Servo-Skull | 91.36% | 93.19% | -1.82 pp | 91.27% | 22.26% |
| Infernus S.T | 93.14% | 92.10% | +1.04 pp | 93.92% | 61.11% |
| Whirlwind S.T | 92.91% | 92.54% | +0.36 pp | 94.37% | 48.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
