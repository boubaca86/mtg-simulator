# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 228,500
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
| Eradicator S.T | 93.05% | 88.65% | +4.40 pp | 94.98% | 57.78% |
| Drop Pod S.T | 92.93% | 88.76% | +4.17 pp | 93.19% | 57.47% |
| Terminator S.T | 92.78% | 88.99% | +3.80 pp | 93.27% | 65.43% |
| Pyroclast Squad S.T | 92.75% | 89.04% | +3.71 pp | 93.56% | 64.31% |
| Exterminatus S.T | 88.81% | 92.23% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.31% | -2.15 pp | 92.58% | 4.23% |
| Infernus S.T | 91.92% | 90.68% | +1.24 pp | 93.05% | 61.43% |
| Whirlwind S.T | 91.12% | 92.14% | -1.02 pp | 93.48% | 47.84% |
| Servo-Skull | 91.07% | 91.62% | -0.55 pp | 91.10% | 22.04% |
| Demolitionist S.T | 91.59% | 91.32% | +0.26 pp | 93.24% | 47.11% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
