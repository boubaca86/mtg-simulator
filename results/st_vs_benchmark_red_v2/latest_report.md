# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 141,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.71% | +5.81 pp | 93.79% | 64.99% |
| Eradicator S.T | 93.11% | 88.75% | +4.37 pp | 95.03% | 57.87% |
| Drop Pod S.T | 93.02% | 88.82% | +4.19 pp | 93.27% | 57.43% |
| Terminator S.T | 92.85% | 89.08% | +3.76 pp | 93.35% | 65.51% |
| Pyroclast Squad S.T | 92.83% | 89.11% | +3.72 pp | 93.63% | 64.44% |
| Exterminatus S.T | 88.93% | 92.30% | -3.37 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.36% | -2.07 pp | 92.79% | 4.23% |
| Infernus S.T | 91.99% | 90.77% | +1.22 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.19% | 92.24% | -1.05 pp | 93.50% | 47.82% |
| Servo-Skull | 91.18% | 91.68% | -0.50 pp | 91.23% | 21.96% |
| Demolitionist S.T | 91.64% | 91.44% | +0.21 pp | 93.30% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
