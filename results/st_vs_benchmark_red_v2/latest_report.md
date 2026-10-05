# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 5,000
**S.T win rate:** 92.60%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.52% | 89.05% | +5.47 pp | 95.81% | 58.72% |
| Servitor | 94.18% | 89.39% | +4.79 pp | 94.39% | 65.54% |
| Exterminatus S.T | 89.36% | 93.48% | -4.13 pp | 0.00% | 0.00% |
| Pyroclast Squad S.T | 93.84% | 90.07% | +3.77 pp | 94.51% | 65.16% |
| Terminator S.T | 93.63% | 90.66% | +2.97 pp | 93.94% | 65.04% |
| Servo-Skull | 90.40% | 93.25% | -2.85 pp | 90.44% | 21.96% |
| Drop Pod S.T | 93.49% | 90.88% | +2.61 pp | 93.61% | 57.60% |
| Vindicator S.T | 91.76% | 93.08% | -1.32 pp | 93.18% | 4.40% |
| Demolitionist S.T | 92.97% | 91.93% | +1.04 pp | 94.38% | 47.72% |
| Infernus S.T | 92.96% | 91.92% | +1.04 pp | 93.78% | 61.46% |
| Whirlwind S.T | 92.51% | 92.75% | -0.25 pp | 93.98% | 47.86% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
