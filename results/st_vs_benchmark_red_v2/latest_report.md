# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 11,000
**S.T win rate:** 91.96%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.82% | 88.20% | +5.62 pp | 94.04% | 65.55% |
| Eradicator S.T | 93.67% | 88.87% | +4.80 pp | 95.35% | 58.24% |
| Pyroclast Squad S.T | 93.28% | 89.39% | +3.89 pp | 94.04% | 64.21% |
| Drop Pod S.T | 93.16% | 89.68% | +3.48 pp | 93.44% | 57.55% |
| Exterminatus S.T | 89.62% | 92.61% | -2.99 pp | 0.00% | 0.00% |
| Terminator S.T | 92.94% | 90.08% | +2.86 pp | 93.32% | 65.50% |
| Vindicator S.T | 90.85% | 92.63% | -1.78 pp | 94.47% | 4.27% |
| Servo-Skull | 90.94% | 92.27% | -1.33 pp | 91.00% | 22.01% |
| Whirlwind S.T | 91.59% | 92.61% | -1.02 pp | 93.70% | 48.03% |
| Infernus S.T | 92.27% | 91.39% | +0.88 pp | 93.39% | 61.45% |
| Demolitionist S.T | 92.11% | 91.70% | +0.41 pp | 93.96% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
