# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 22,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.70% | 87.32% | +6.38 pp | 93.97% | 65.25% |
| Eradicator S.T | 93.27% | 88.46% | +4.81 pp | 95.02% | 58.32% |
| Drop Pod S.T | 93.01% | 88.83% | +4.18 pp | 93.34% | 57.56% |
| Pyroclast Squad S.T | 92.93% | 89.00% | +3.92 pp | 93.68% | 63.62% |
| Terminator S.T | 92.53% | 89.71% | +2.82 pp | 92.95% | 65.51% |
| Exterminatus S.T | 89.43% | 92.15% | -2.73 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.42% | -2.27 pp | 92.04% | 4.13% |
| Infernus S.T | 91.96% | 90.83% | +1.13 pp | 93.15% | 61.50% |
| Whirlwind S.T | 91.29% | 92.05% | -0.76 pp | 93.51% | 48.20% |
| Demolitionist S.T | 91.84% | 91.08% | +0.76 pp | 93.52% | 47.36% |
| Servo-Skull | 91.02% | 91.73% | -0.71 pp | 91.10% | 22.13% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
