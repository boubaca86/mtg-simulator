# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 73,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.69% | +5.80 pp | 93.73% | 64.92% |
| Eradicator S.T | 93.17% | 88.55% | +4.62 pp | 95.08% | 58.13% |
| Drop Pod S.T | 93.03% | 88.71% | +4.33 pp | 93.32% | 57.62% |
| Pyroclast Squad S.T | 92.86% | 89.00% | +3.86 pp | 93.68% | 64.14% |
| Terminator S.T | 92.75% | 89.18% | +3.57 pp | 93.27% | 65.56% |
| Exterminatus S.T | 89.30% | 92.17% | -2.87 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.35% | -2.12 pp | 93.03% | 4.20% |
| Whirlwind S.T | 91.17% | 92.19% | -1.03 pp | 93.44% | 47.81% |
| Infernus S.T | 91.89% | 90.90% | +0.98 pp | 93.05% | 61.38% |
| Demolitionist S.T | 91.67% | 91.32% | +0.35 pp | 93.35% | 47.27% |
| Servo-Skull | 91.30% | 91.62% | -0.31 pp | 91.38% | 21.96% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
