# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 249,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.61% | +5.83 pp | 93.72% | 64.94% |
| Eradicator S.T | 93.03% | 88.65% | +4.38 pp | 94.98% | 57.80% |
| Drop Pod S.T | 92.92% | 88.76% | +4.16 pp | 93.16% | 57.42% |
| Terminator S.T | 92.79% | 88.92% | +3.87 pp | 93.29% | 65.48% |
| Pyroclast Squad S.T | 92.73% | 89.04% | +3.70 pp | 93.54% | 64.27% |
| Exterminatus S.T | 88.82% | 92.22% | -3.39 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.31% | -2.20 pp | 92.36% | 4.24% |
| Infernus S.T | 91.91% | 90.67% | +1.24 pp | 93.03% | 61.38% |
| Whirlwind S.T | 91.10% | 92.14% | -1.04 pp | 93.49% | 47.83% |
| Servo-Skull | 91.05% | 91.61% | -0.56 pp | 91.07% | 22.05% |
| Demolitionist S.T | 91.57% | 91.32% | +0.25 pp | 93.24% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
