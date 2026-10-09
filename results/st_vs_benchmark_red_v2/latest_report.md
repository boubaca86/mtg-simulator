# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 58,500
**S.T win rate:** 91.47%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.65% | +5.76 pp | 93.67% | 64.86% |
| Eradicator S.T | 93.16% | 88.38% | +4.78 pp | 95.05% | 58.11% |
| Drop Pod S.T | 92.94% | 88.68% | +4.26 pp | 93.26% | 57.59% |
| Pyroclast Squad S.T | 92.81% | 88.89% | +3.92 pp | 93.59% | 64.21% |
| Terminator S.T | 92.67% | 89.16% | +3.51 pp | 93.16% | 65.49% |
| Exterminatus S.T | 89.12% | 92.13% | -3.01 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.27% | -2.10 pp | 92.93% | 4.18% |
| Whirlwind S.T | 91.14% | 92.04% | -0.90 pp | 93.38% | 47.84% |
| Infernus S.T | 91.78% | 90.90% | +0.89 pp | 92.93% | 61.35% |
| Servo-Skull | 91.17% | 91.56% | -0.40 pp | 91.25% | 21.93% |
| Demolitionist S.T | 91.58% | 91.28% | +0.31 pp | 93.25% | 47.39% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
