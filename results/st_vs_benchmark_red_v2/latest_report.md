# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 2,500
**S.T win rate:** 92.84%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.88% | 88.94% | +5.94 pp | 96.48% | 59.12% |
| Pyroclast Squad S.T | 94.35% | 89.73% | +4.62 pp | 94.87% | 65.52% |
| Exterminatus S.T | 89.75% | 93.65% | -3.90 pp | 0.00% | 0.00% |
| Servitor | 94.09% | 90.31% | +3.77 pp | 94.30% | 65.32% |
| Servo-Skull | 90.18% | 93.63% | -3.45 pp | 90.02% | 22.04% |
| Terminator S.T | 93.69% | 91.24% | +2.44 pp | 93.92% | 65.08% |
| Demolitionist S.T | 93.65% | 91.35% | +2.29 pp | 95.25% | 47.96% |
| Vindicator S.T | 91.40% | 93.66% | -2.26 pp | 92.31% | 4.68% |
| Drop Pod S.T | 93.46% | 91.66% | +1.81 pp | 93.79% | 58.00% |
| Whirlwind S.T | 93.07% | 92.46% | +0.61 pp | 94.68% | 48.08% |
| Infernus S.T | 93.01% | 92.53% | +0.49 pp | 93.84% | 61.00% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
