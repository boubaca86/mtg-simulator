# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 5,500
**S.T win rate:** 92.44%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.26% | 89.06% | +5.20 pp | 95.75% | 58.56% |
| Servitor | 94.02% | 89.20% | +4.82 pp | 94.27% | 65.67% |
| Exterminatus S.T | 89.19% | 93.32% | -4.13 pp | 0.00% | 0.00% |
| Pyroclast Squad S.T | 93.70% | 89.89% | +3.81 pp | 94.45% | 64.87% |
| Drop Pod S.T | 93.40% | 90.60% | +2.80 pp | 93.60% | 57.36% |
| Terminator S.T | 93.37% | 90.68% | +2.69 pp | 93.68% | 65.00% |
| Servo-Skull | 90.71% | 92.96% | -2.25 pp | 90.81% | 22.35% |
| Vindicator S.T | 91.42% | 93.02% | -1.60 pp | 93.64% | 4.29% |
| Demolitionist S.T | 92.83% | 91.72% | +1.11 pp | 94.34% | 47.51% |
| Infernus S.T | 92.72% | 91.91% | +0.81 pp | 93.56% | 61.53% |
| Whirlwind S.T | 92.32% | 92.64% | -0.32 pp | 93.84% | 47.78% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
