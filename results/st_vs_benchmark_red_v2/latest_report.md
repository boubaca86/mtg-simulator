# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 40,500
**S.T win rate:** 91.46%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.38% | 87.63% | +5.75 pp | 93.68% | 65.03% |
| Eradicator S.T | 93.13% | 88.39% | +4.74 pp | 94.98% | 58.14% |
| Drop Pod S.T | 93.05% | 88.41% | +4.63 pp | 93.39% | 57.61% |
| Pyroclast Squad S.T | 92.86% | 88.74% | +4.12 pp | 93.66% | 64.23% |
| Exterminatus S.T | 88.98% | 92.14% | -3.15 pp | 0.00% | 0.00% |
| Terminator S.T | 92.51% | 89.41% | +3.09 pp | 93.02% | 65.46% |
| Vindicator S.T | 90.14% | 92.25% | -2.11 pp | 92.73% | 4.11% |
| Infernus S.T | 91.74% | 90.93% | +0.80 pp | 92.93% | 61.35% |
| Whirlwind S.T | 91.17% | 91.95% | -0.78 pp | 93.53% | 47.68% |
| Servo-Skull | 91.07% | 91.57% | -0.51 pp | 91.14% | 21.82% |
| Demolitionist S.T | 91.62% | 91.16% | +0.47 pp | 93.46% | 47.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
