# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 111,000
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.60% | +5.94 pp | 93.82% | 64.90% |
| Eradicator S.T | 93.14% | 88.64% | +4.50 pp | 95.04% | 57.89% |
| Drop Pod S.T | 93.01% | 88.78% | +4.23 pp | 93.32% | 57.51% |
| Pyroclast Squad S.T | 92.84% | 89.01% | +3.83 pp | 93.65% | 64.44% |
| Terminator S.T | 92.83% | 89.04% | +3.79 pp | 93.32% | 65.53% |
| Exterminatus S.T | 89.04% | 92.24% | -3.21 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.31% | -2.02 pp | 92.68% | 4.26% |
| Infernus S.T | 91.96% | 90.76% | +1.20 pp | 93.12% | 61.51% |
| Whirlwind S.T | 91.12% | 92.29% | -1.16 pp | 93.38% | 47.82% |
| Servo-Skull | 91.20% | 91.65% | -0.45 pp | 91.26% | 22.01% |
| Demolitionist S.T | 91.59% | 91.46% | +0.13 pp | 93.26% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
