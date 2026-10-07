# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 34,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.53% | +5.95 pp | 93.76% | 65.03% |
| Eradicator S.T | 93.21% | 88.33% | +4.88 pp | 95.00% | 58.32% |
| Drop Pod S.T | 93.05% | 88.51% | +4.54 pp | 93.38% | 57.72% |
| Pyroclast Squad S.T | 92.84% | 88.90% | +3.94 pp | 93.60% | 64.11% |
| Terminator S.T | 92.53% | 89.47% | +3.07 pp | 92.99% | 65.53% |
| Exterminatus S.T | 89.11% | 92.15% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.27% | -2.07 pp | 92.71% | 4.11% |
| Infernus S.T | 91.84% | 90.84% | +1.00 pp | 93.01% | 61.50% |
| Whirlwind S.T | 91.26% | 91.89% | -0.63 pp | 93.55% | 47.76% |
| Demolitionist S.T | 91.68% | 91.16% | +0.51 pp | 93.44% | 47.25% |
| Servo-Skull | 91.18% | 91.58% | -0.41 pp | 91.24% | 21.99% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
