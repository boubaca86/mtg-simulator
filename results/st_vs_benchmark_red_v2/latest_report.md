# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 90,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.59% | +5.96 pp | 93.80% | 64.81% |
| Eradicator S.T | 93.14% | 88.61% | +4.53 pp | 95.09% | 58.05% |
| Drop Pod S.T | 93.02% | 88.71% | +4.31 pp | 93.33% | 57.59% |
| Pyroclast Squad S.T | 92.86% | 88.97% | +3.89 pp | 93.68% | 64.27% |
| Terminator S.T | 92.76% | 89.15% | +3.62 pp | 93.27% | 65.56% |
| Exterminatus S.T | 89.07% | 92.22% | -3.15 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.34% | -2.13 pp | 92.77% | 4.23% |
| Whirlwind S.T | 91.14% | 92.23% | -1.10 pp | 93.41% | 47.89% |
| Infernus S.T | 91.91% | 90.84% | +1.07 pp | 93.07% | 61.38% |
| Servo-Skull | 91.25% | 91.62% | -0.38 pp | 91.33% | 21.95% |
| Demolitionist S.T | 91.61% | 91.40% | +0.22 pp | 93.30% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
