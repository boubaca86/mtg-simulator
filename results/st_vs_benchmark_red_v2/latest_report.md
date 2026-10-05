# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 13,000
**S.T win rate:** 91.80%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.73% | 87.94% | +5.79 pp | 93.94% | 65.19% |
| Eradicator S.T | 93.65% | 88.44% | +5.21 pp | 95.28% | 58.24% |
| Drop Pod S.T | 93.13% | 89.25% | +3.89 pp | 93.43% | 57.64% |
| Pyroclast Squad S.T | 93.11% | 89.28% | +3.83 pp | 93.91% | 63.87% |
| Terminator S.T | 92.74% | 89.98% | +2.76 pp | 93.15% | 65.37% |
| Exterminatus S.T | 89.71% | 92.37% | -2.66 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.61% | 92.50% | -1.89 pp | 93.61% | 4.22% |
| Servo-Skull | 90.75% | 92.11% | -1.36 pp | 90.85% | 22.02% |
| Infernus S.T | 92.14% | 91.15% | +1.00 pp | 93.22% | 61.68% |
| Whirlwind S.T | 91.51% | 92.30% | -0.78 pp | 93.63% | 48.06% |
| Demolitionist S.T | 91.94% | 91.55% | +0.39 pp | 93.79% | 47.43% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
