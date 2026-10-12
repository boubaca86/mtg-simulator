# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 88,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.57% | +5.94 pp | 93.77% | 64.81% |
| Eradicator S.T | 93.12% | 88.56% | +4.56 pp | 95.08% | 58.05% |
| Drop Pod S.T | 92.99% | 88.69% | +4.30 pp | 93.30% | 57.59% |
| Pyroclast Squad S.T | 92.82% | 88.96% | +3.87 pp | 93.64% | 64.27% |
| Terminator S.T | 92.73% | 89.12% | +3.62 pp | 93.24% | 65.56% |
| Exterminatus S.T | 89.08% | 92.19% | -3.11 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.33% | -2.16 pp | 92.72% | 4.24% |
| Whirlwind S.T | 91.13% | 92.18% | -1.05 pp | 93.41% | 47.89% |
| Infernus S.T | 91.87% | 90.83% | +1.05 pp | 93.03% | 61.37% |
| Servo-Skull | 91.23% | 91.59% | -0.36 pp | 91.32% | 21.94% |
| Demolitionist S.T | 91.58% | 91.37% | +0.21 pp | 93.29% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
