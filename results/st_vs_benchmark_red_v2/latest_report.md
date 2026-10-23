# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 220,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.65% | +5.79 pp | 93.74% | 64.92% |
| Eradicator S.T | 93.07% | 88.64% | +4.43 pp | 94.99% | 57.82% |
| Drop Pod S.T | 92.96% | 88.74% | +4.22 pp | 93.21% | 57.48% |
| Terminator S.T | 92.79% | 89.01% | +3.78 pp | 93.28% | 65.42% |
| Pyroclast Squad S.T | 92.77% | 89.04% | +3.72 pp | 93.57% | 64.31% |
| Exterminatus S.T | 88.80% | 92.25% | -3.45 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.32% | -2.16 pp | 92.56% | 4.24% |
| Infernus S.T | 91.94% | 90.68% | +1.26 pp | 93.07% | 61.43% |
| Whirlwind S.T | 91.14% | 92.14% | -1.01 pp | 93.50% | 47.86% |
| Servo-Skull | 91.09% | 91.62% | -0.53 pp | 91.12% | 22.04% |
| Demolitionist S.T | 91.60% | 91.32% | +0.28 pp | 93.25% | 47.17% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
