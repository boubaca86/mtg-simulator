# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 62,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.38% | 87.73% | +5.65 pp | 93.63% | 64.92% |
| Eradicator S.T | 93.15% | 88.41% | +4.74 pp | 95.05% | 58.14% |
| Drop Pod S.T | 92.97% | 88.66% | +4.31 pp | 93.28% | 57.55% |
| Pyroclast Squad S.T | 92.83% | 88.87% | +3.96 pp | 93.61% | 64.21% |
| Terminator S.T | 92.69% | 89.13% | +3.56 pp | 93.19% | 65.47% |
| Exterminatus S.T | 89.14% | 92.14% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.27% | -2.06 pp | 92.95% | 4.18% |
| Infernus S.T | 91.82% | 90.85% | +0.97 pp | 92.96% | 61.39% |
| Whirlwind S.T | 91.14% | 92.07% | -0.93 pp | 93.37% | 47.85% |
| Servo-Skull | 91.23% | 91.56% | -0.33 pp | 91.30% | 21.98% |
| Demolitionist S.T | 91.59% | 91.28% | +0.31 pp | 93.27% | 47.33% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
