# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 8,500
**S.T win rate:** 92.12%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.04% | 88.62% | +5.42 pp | 95.59% | 58.45% |
| Servitor | 93.90% | 88.57% | +5.33 pp | 94.17% | 65.13% |
| Pyroclast Squad S.T | 93.47% | 89.45% | +4.02 pp | 94.29% | 64.32% |
| Exterminatus S.T | 89.42% | 92.86% | -3.45 pp | 0.00% | 0.00% |
| Drop Pod S.T | 93.26% | 89.95% | +3.32 pp | 93.51% | 57.46% |
| Terminator S.T | 93.09% | 90.25% | +2.84 pp | 93.46% | 65.34% |
| Servo-Skull | 90.61% | 92.57% | -1.96 pp | 90.62% | 22.19% |
| Vindicator S.T | 90.99% | 92.79% | -1.80 pp | 93.70% | 4.29% |
| Infernus S.T | 92.42% | 91.56% | +0.86 pp | 93.46% | 60.99% |
| Whirlwind S.T | 91.92% | 92.45% | -0.53 pp | 93.65% | 47.81% |
| Demolitionist S.T | 92.14% | 92.09% | +0.05 pp | 93.75% | 47.41% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
