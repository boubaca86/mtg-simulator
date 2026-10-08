# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 48,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.79% | +5.66 pp | 93.73% | 65.03% |
| Eradicator S.T | 93.25% | 88.46% | +4.78 pp | 95.06% | 57.97% |
| Drop Pod S.T | 93.09% | 88.62% | +4.47 pp | 93.40% | 57.48% |
| Pyroclast Squad S.T | 92.89% | 88.95% | +3.94 pp | 93.69% | 64.29% |
| Terminator S.T | 92.73% | 89.25% | +3.49 pp | 93.25% | 65.56% |
| Exterminatus S.T | 89.18% | 92.21% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.35% | -2.11 pp | 92.90% | 4.12% |
| Infernus S.T | 91.86% | 90.98% | +0.88 pp | 93.02% | 61.48% |
| Whirlwind S.T | 91.27% | 92.05% | -0.78 pp | 93.51% | 47.79% |
| Servo-Skull | 91.16% | 91.67% | -0.50 pp | 91.24% | 21.78% |
| Demolitionist S.T | 91.73% | 91.23% | +0.50 pp | 93.46% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
