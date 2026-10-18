# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 159,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.75% | +5.73 pp | 93.77% | 65.00% |
| Eradicator S.T | 93.13% | 88.71% | +4.41 pp | 95.03% | 57.84% |
| Drop Pod S.T | 93.00% | 88.85% | +4.15 pp | 93.25% | 57.49% |
| Terminator S.T | 92.85% | 89.07% | +3.78 pp | 93.35% | 65.50% |
| Pyroclast Squad S.T | 92.83% | 89.09% | +3.74 pp | 93.63% | 64.41% |
| Exterminatus S.T | 88.94% | 92.29% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.37% | -2.13 pp | 92.86% | 4.22% |
| Infernus S.T | 92.00% | 90.75% | +1.25 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.16% | 92.28% | -1.12 pp | 93.49% | 47.79% |
| Servo-Skull | 91.17% | 91.68% | -0.51 pp | 91.23% | 22.04% |
| Demolitionist S.T | 91.66% | 91.39% | +0.27 pp | 93.31% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
