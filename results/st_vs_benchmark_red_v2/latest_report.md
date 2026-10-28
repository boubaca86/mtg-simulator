# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 287,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.63% | +5.80 pp | 93.72% | 64.96% |
| Eradicator S.T | 93.04% | 88.64% | +4.40 pp | 94.97% | 57.77% |
| Drop Pod S.T | 92.91% | 88.78% | +4.13 pp | 93.16% | 57.39% |
| Terminator S.T | 92.78% | 88.96% | +3.82 pp | 93.27% | 65.48% |
| Pyroclast Squad S.T | 92.75% | 89.02% | +3.73 pp | 93.55% | 64.30% |
| Exterminatus S.T | 88.89% | 92.20% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.32% | -2.19 pp | 92.34% | 4.24% |
| Infernus S.T | 91.91% | 90.68% | +1.23 pp | 93.03% | 61.35% |
| Whirlwind S.T | 91.12% | 92.11% | -0.99 pp | 93.49% | 47.79% |
| Servo-Skull | 91.07% | 91.61% | -0.54 pp | 91.08% | 22.06% |
| Demolitionist S.T | 91.57% | 91.34% | +0.23 pp | 93.23% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
