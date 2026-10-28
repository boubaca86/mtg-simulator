# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 284,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.63% | +5.80 pp | 93.72% | 64.96% |
| Eradicator S.T | 93.05% | 88.65% | +4.39 pp | 94.98% | 57.78% |
| Drop Pod S.T | 92.92% | 88.79% | +4.12 pp | 93.16% | 57.40% |
| Terminator S.T | 92.79% | 88.96% | +3.83 pp | 93.28% | 65.47% |
| Pyroclast Squad S.T | 92.75% | 89.03% | +3.72 pp | 93.55% | 64.29% |
| Exterminatus S.T | 88.90% | 92.21% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.32% | -2.19 pp | 92.40% | 4.24% |
| Infernus S.T | 91.92% | 90.69% | +1.23 pp | 93.04% | 61.35% |
| Whirlwind S.T | 91.12% | 92.13% | -1.01 pp | 93.50% | 47.80% |
| Servo-Skull | 91.08% | 91.61% | -0.53 pp | 91.09% | 22.06% |
| Demolitionist S.T | 91.57% | 91.34% | +0.23 pp | 93.24% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
