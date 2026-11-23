# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 384,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.67% | +5.75 pp | 93.71% | 64.99% |
| Eradicator S.T | 93.02% | 88.72% | +4.31 pp | 94.99% | 57.73% |
| Drop Pod S.T | 92.93% | 88.78% | +4.14 pp | 93.20% | 57.39% |
| Pyroclast Squad S.T | 92.77% | 89.02% | +3.74 pp | 93.58% | 64.27% |
| Terminator S.T | 92.76% | 89.03% | +3.73 pp | 93.26% | 65.51% |
| Exterminatus S.T | 88.93% | 92.21% | -3.28 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.33% | -2.20 pp | 92.36% | 4.25% |
| Infernus S.T | 91.92% | 90.70% | +1.22 pp | 93.07% | 61.38% |
| Whirlwind S.T | 91.14% | 92.12% | -0.97 pp | 93.55% | 47.80% |
| Servo-Skull | 91.04% | 91.63% | -0.60 pp | 91.06% | 22.05% |
| Demolitionist S.T | 91.55% | 91.40% | +0.15 pp | 93.24% | 47.02% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
