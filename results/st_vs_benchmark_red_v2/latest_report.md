# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 12,500
**S.T win rate:** 91.83%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.79% | 87.88% | +5.91 pp | 93.99% | 65.39% |
| Eradicator S.T | 93.63% | 88.57% | +5.06 pp | 95.25% | 58.27% |
| Pyroclast Squad S.T | 93.17% | 89.27% | +3.90 pp | 93.94% | 63.88% |
| Drop Pod S.T | 93.12% | 89.38% | +3.74 pp | 93.41% | 57.57% |
| Terminator S.T | 92.79% | 89.98% | +2.81 pp | 93.18% | 65.38% |
| Exterminatus S.T | 89.73% | 92.41% | -2.69 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.68% | 92.52% | -1.84 pp | 93.75% | 4.22% |
| Servo-Skull | 90.84% | 92.13% | -1.29 pp | 90.96% | 22.02% |
| Infernus S.T | 92.18% | 91.17% | +1.01 pp | 93.26% | 61.64% |
| Whirlwind S.T | 91.52% | 92.37% | -0.85 pp | 93.69% | 48.06% |
| Demolitionist S.T | 91.93% | 91.67% | +0.26 pp | 93.74% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
