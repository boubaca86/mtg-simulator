# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 112,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.63% | +5.92 pp | 93.82% | 64.91% |
| Eradicator S.T | 93.16% | 88.65% | +4.51 pp | 95.06% | 57.89% |
| Drop Pod S.T | 93.02% | 88.78% | +4.24 pp | 93.32% | 57.51% |
| Pyroclast Squad S.T | 92.85% | 89.02% | +3.84 pp | 93.66% | 64.45% |
| Terminator S.T | 92.85% | 89.04% | +3.80 pp | 93.33% | 65.55% |
| Exterminatus S.T | 89.04% | 92.26% | -3.22 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.34% | -2.06 pp | 92.72% | 4.25% |
| Infernus S.T | 91.98% | 90.76% | +1.22 pp | 93.13% | 61.51% |
| Whirlwind S.T | 91.13% | 92.30% | -1.17 pp | 93.39% | 47.81% |
| Servo-Skull | 91.20% | 91.66% | -0.46 pp | 91.26% | 22.01% |
| Demolitionist S.T | 91.60% | 91.48% | +0.12 pp | 93.26% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
