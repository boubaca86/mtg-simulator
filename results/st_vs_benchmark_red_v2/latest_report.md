# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 114,500
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.63% | +5.92 pp | 93.83% | 64.92% |
| Eradicator S.T | 93.17% | 88.63% | +4.55 pp | 95.07% | 57.86% |
| Drop Pod S.T | 93.02% | 88.78% | +4.24 pp | 93.33% | 57.54% |
| Terminator S.T | 92.85% | 89.05% | +3.80 pp | 93.34% | 65.54% |
| Pyroclast Squad S.T | 92.84% | 89.05% | +3.79 pp | 93.65% | 64.43% |
| Exterminatus S.T | 89.04% | 92.26% | -3.22 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.33% | -2.03 pp | 92.74% | 4.25% |
| Infernus S.T | 91.99% | 90.76% | +1.23 pp | 93.13% | 61.51% |
| Whirlwind S.T | 91.15% | 92.27% | -1.12 pp | 93.39% | 47.81% |
| Servo-Skull | 91.20% | 91.67% | -0.46 pp | 91.27% | 22.02% |
| Demolitionist S.T | 91.61% | 91.47% | +0.14 pp | 93.26% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
