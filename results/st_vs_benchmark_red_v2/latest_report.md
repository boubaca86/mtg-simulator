# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 142,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.69% | +5.82 pp | 93.79% | 64.98% |
| Eradicator S.T | 93.13% | 88.71% | +4.41 pp | 95.04% | 57.89% |
| Drop Pod S.T | 93.02% | 88.82% | +4.20 pp | 93.27% | 57.46% |
| Terminator S.T | 92.84% | 89.09% | +3.75 pp | 93.34% | 65.51% |
| Pyroclast Squad S.T | 92.82% | 89.12% | +3.69 pp | 93.62% | 64.42% |
| Exterminatus S.T | 88.94% | 92.30% | -3.36 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.35% | -2.07 pp | 92.82% | 4.22% |
| Infernus S.T | 91.98% | 90.79% | +1.19 pp | 93.13% | 61.54% |
| Whirlwind S.T | 91.18% | 92.24% | -1.06 pp | 93.49% | 47.84% |
| Servo-Skull | 91.20% | 91.68% | -0.48 pp | 91.24% | 21.96% |
| Demolitionist S.T | 91.65% | 91.42% | +0.23 pp | 93.30% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
