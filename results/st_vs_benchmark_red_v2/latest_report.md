# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 313,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.65% | +5.78 pp | 93.71% | 64.96% |
| Eradicator S.T | 93.03% | 88.68% | +4.35 pp | 94.99% | 57.75% |
| Drop Pod S.T | 92.92% | 88.77% | +4.15 pp | 93.18% | 57.42% |
| Terminator S.T | 92.77% | 88.99% | +3.79 pp | 93.28% | 65.48% |
| Pyroclast Squad S.T | 92.76% | 89.01% | +3.75 pp | 93.57% | 64.29% |
| Exterminatus S.T | 88.89% | 92.21% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.31% | -2.17 pp | 92.42% | 4.25% |
| Infernus S.T | 91.90% | 90.72% | +1.18 pp | 93.04% | 61.35% |
| Whirlwind S.T | 91.12% | 92.12% | -1.00 pp | 93.52% | 47.73% |
| Servo-Skull | 91.03% | 91.62% | -0.59 pp | 91.05% | 22.05% |
| Demolitionist S.T | 91.56% | 91.36% | +0.20 pp | 93.25% | 47.03% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
