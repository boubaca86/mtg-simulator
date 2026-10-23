# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 229,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.61% | +5.84 pp | 93.74% | 64.94% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.98% | 57.78% |
| Drop Pod S.T | 92.93% | 88.75% | +4.18 pp | 93.19% | 57.47% |
| Terminator S.T | 92.78% | 88.99% | +3.79 pp | 93.27% | 65.42% |
| Pyroclast Squad S.T | 92.75% | 89.03% | +3.72 pp | 93.56% | 64.31% |
| Exterminatus S.T | 88.80% | 92.23% | -3.43 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.30% | -2.14 pp | 92.59% | 4.23% |
| Infernus S.T | 91.92% | 90.68% | +1.24 pp | 93.05% | 61.43% |
| Whirlwind S.T | 91.11% | 92.14% | -1.03 pp | 93.48% | 47.84% |
| Servo-Skull | 91.06% | 91.62% | -0.56 pp | 91.09% | 22.03% |
| Demolitionist S.T | 91.58% | 91.32% | +0.27 pp | 93.24% | 47.11% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
