# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 95,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.58% | 87.62% | +5.96 pp | 93.84% | 64.82% |
| Eradicator S.T | 93.17% | 88.63% | +4.54 pp | 95.10% | 58.02% |
| Drop Pod S.T | 93.05% | 88.76% | +4.29 pp | 93.35% | 57.53% |
| Pyroclast Squad S.T | 92.88% | 89.02% | +3.86 pp | 93.69% | 64.33% |
| Terminator S.T | 92.81% | 89.15% | +3.65 pp | 93.31% | 65.53% |
| Exterminatus S.T | 89.06% | 92.26% | -3.21 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.36% | -2.09 pp | 92.75% | 4.24% |
| Whirlwind S.T | 91.17% | 92.26% | -1.09 pp | 93.42% | 47.90% |
| Infernus S.T | 91.94% | 90.86% | +1.09 pp | 93.10% | 61.41% |
| Servo-Skull | 91.24% | 91.66% | -0.42 pp | 91.31% | 21.97% |
| Demolitionist S.T | 91.64% | 91.43% | +0.21 pp | 93.29% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
