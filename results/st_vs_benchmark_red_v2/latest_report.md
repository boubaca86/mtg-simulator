# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 11,500
**S.T win rate:** 91.96%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.82% | 88.19% | +5.63 pp | 94.04% | 65.46% |
| Eradicator S.T | 93.72% | 88.78% | +4.94 pp | 95.37% | 58.17% |
| Pyroclast Squad S.T | 93.25% | 89.45% | +3.81 pp | 93.99% | 64.09% |
| Drop Pod S.T | 93.17% | 89.64% | +3.53 pp | 93.43% | 57.48% |
| Terminator S.T | 92.93% | 90.06% | +2.87 pp | 93.30% | 65.52% |
| Exterminatus S.T | 89.77% | 92.55% | -2.78 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.87% | 92.60% | -1.74 pp | 94.38% | 4.33% |
| Servo-Skull | 90.96% | 92.25% | -1.29 pp | 91.07% | 22.01% |
| Infernus S.T | 92.31% | 91.28% | +1.03 pp | 93.43% | 61.45% |
| Whirlwind S.T | 91.64% | 92.50% | -0.86 pp | 93.71% | 47.97% |
| Demolitionist S.T | 92.09% | 91.72% | +0.37 pp | 93.90% | 47.35% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
