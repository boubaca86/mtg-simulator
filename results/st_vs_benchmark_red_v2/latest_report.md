# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 244,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.63% | +5.80 pp | 93.72% | 64.94% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.98% | 57.82% |
| Drop Pod S.T | 92.91% | 88.78% | +4.13 pp | 93.16% | 57.43% |
| Terminator S.T | 92.79% | 88.94% | +3.85 pp | 93.28% | 65.46% |
| Pyroclast Squad S.T | 92.73% | 89.05% | +3.69 pp | 93.55% | 64.28% |
| Exterminatus S.T | 88.82% | 92.22% | -3.40 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.30% | -2.16 pp | 92.48% | 4.23% |
| Infernus S.T | 91.91% | 90.67% | +1.24 pp | 93.04% | 61.40% |
| Whirlwind S.T | 91.11% | 92.13% | -1.02 pp | 93.49% | 47.84% |
| Servo-Skull | 91.07% | 91.61% | -0.54 pp | 91.09% | 22.07% |
| Demolitionist S.T | 91.57% | 91.32% | +0.26 pp | 93.23% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
