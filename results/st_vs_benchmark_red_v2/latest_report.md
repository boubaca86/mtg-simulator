# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 343,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.66% | +5.77 pp | 93.72% | 64.95% |
| Eradicator S.T | 93.02% | 88.70% | +4.32 pp | 94.98% | 57.73% |
| Drop Pod S.T | 92.92% | 88.78% | +4.14 pp | 93.19% | 57.40% |
| Terminator S.T | 92.76% | 89.02% | +3.75 pp | 93.26% | 65.52% |
| Pyroclast Squad S.T | 92.76% | 89.02% | +3.74 pp | 93.57% | 64.29% |
| Exterminatus S.T | 88.93% | 92.20% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.31% | -2.16 pp | 92.51% | 4.25% |
| Infernus S.T | 91.91% | 90.71% | +1.21 pp | 93.05% | 61.37% |
| Whirlwind S.T | 91.12% | 92.14% | -1.02 pp | 93.51% | 47.81% |
| Servo-Skull | 91.01% | 91.63% | -0.63 pp | 91.02% | 22.01% |
| Demolitionist S.T | 91.55% | 91.38% | +0.17 pp | 93.25% | 47.01% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
