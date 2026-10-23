# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 227,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.62% | +5.84 pp | 93.74% | 64.94% |
| Eradicator S.T | 93.05% | 88.65% | +4.40 pp | 94.98% | 57.78% |
| Drop Pod S.T | 92.94% | 88.74% | +4.20 pp | 93.20% | 57.47% |
| Terminator S.T | 92.79% | 88.98% | +3.81 pp | 93.28% | 65.42% |
| Pyroclast Squad S.T | 92.75% | 89.04% | +3.72 pp | 93.56% | 64.31% |
| Exterminatus S.T | 88.81% | 92.23% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.31% | -2.16 pp | 92.59% | 4.23% |
| Infernus S.T | 91.92% | 90.68% | +1.24 pp | 93.05% | 61.42% |
| Whirlwind S.T | 91.12% | 92.15% | -1.03 pp | 93.48% | 47.84% |
| Servo-Skull | 91.07% | 91.62% | -0.55 pp | 91.10% | 22.04% |
| Demolitionist S.T | 91.59% | 91.32% | +0.27 pp | 93.24% | 47.12% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
