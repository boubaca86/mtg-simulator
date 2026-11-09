# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 362,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.66% | +5.77 pp | 93.71% | 64.96% |
| Eradicator S.T | 93.02% | 88.71% | +4.30 pp | 94.99% | 57.72% |
| Drop Pod S.T | 92.92% | 88.77% | +4.15 pp | 93.19% | 57.41% |
| Terminator S.T | 92.76% | 89.01% | +3.75 pp | 93.27% | 65.51% |
| Pyroclast Squad S.T | 92.76% | 89.02% | +3.73 pp | 93.57% | 64.29% |
| Exterminatus S.T | 88.93% | 92.20% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.32% | -2.19 pp | 92.42% | 4.24% |
| Infernus S.T | 91.91% | 90.70% | +1.21 pp | 93.06% | 61.40% |
| Whirlwind S.T | 91.13% | 92.11% | -0.98 pp | 93.54% | 47.80% |
| Servo-Skull | 91.03% | 91.63% | -0.60 pp | 91.05% | 22.05% |
| Demolitionist S.T | 91.55% | 91.38% | +0.17 pp | 93.25% | 47.03% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
