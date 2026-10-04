# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 4,000
**S.T win rate:** 92.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.45% | 89.05% | +5.40 pp | 95.93% | 58.98% |
| Servitor | 93.99% | 89.75% | +4.25 pp | 94.20% | 65.10% |
| Pyroclast Squad S.T | 93.97% | 89.73% | +4.24 pp | 94.63% | 65.12% |
| Exterminatus S.T | 89.47% | 93.43% | -3.96 pp | 0.00% | 0.00% |
| Drop Pod S.T | 93.57% | 90.63% | +2.95 pp | 93.74% | 58.27% |
| Vindicator S.T | 90.98% | 93.48% | -2.50 pp | 93.37% | 4.53% |
| Terminator S.T | 93.44% | 91.01% | +2.43 pp | 93.83% | 64.05% |
| Servo-Skull | 90.96% | 93.05% | -2.09 pp | 90.85% | 22.12% |
| Demolitionist S.T | 93.31% | 91.22% | +2.09 pp | 94.62% | 48.35% |
| Infernus S.T | 92.99% | 91.81% | +1.18 pp | 93.82% | 61.05% |
| Whirlwind S.T | 92.79% | 92.22% | +0.57 pp | 94.42% | 47.95% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
