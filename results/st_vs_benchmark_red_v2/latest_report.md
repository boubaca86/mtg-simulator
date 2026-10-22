# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 211,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.68% | +5.79 pp | 93.75% | 64.92% |
| Eradicator S.T | 93.07% | 88.70% | +4.38 pp | 94.99% | 57.82% |
| Drop Pod S.T | 92.97% | 88.79% | +4.17 pp | 93.22% | 57.46% |
| Terminator S.T | 92.82% | 89.01% | +3.82 pp | 93.32% | 65.45% |
| Pyroclast Squad S.T | 92.77% | 89.09% | +3.68 pp | 93.59% | 64.31% |
| Exterminatus S.T | 88.87% | 92.26% | -3.39 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.35% | -2.18 pp | 92.59% | 4.23% |
| Infernus S.T | 91.96% | 90.72% | +1.24 pp | 93.08% | 61.42% |
| Whirlwind S.T | 91.17% | 92.14% | -0.97 pp | 93.52% | 47.87% |
| Servo-Skull | 91.09% | 91.66% | -0.57 pp | 91.12% | 21.98% |
| Demolitionist S.T | 91.63% | 91.34% | +0.29 pp | 93.25% | 47.17% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
