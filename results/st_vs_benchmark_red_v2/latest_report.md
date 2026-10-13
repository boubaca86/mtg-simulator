# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 108,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.59% | +5.96 pp | 93.81% | 64.89% |
| Eradicator S.T | 93.14% | 88.61% | +4.53 pp | 95.07% | 57.89% |
| Drop Pod S.T | 93.00% | 88.77% | +4.24 pp | 93.31% | 57.47% |
| Pyroclast Squad S.T | 92.83% | 89.02% | +3.81 pp | 93.64% | 64.39% |
| Terminator S.T | 92.82% | 89.03% | +3.79 pp | 93.32% | 65.55% |
| Exterminatus S.T | 89.05% | 92.23% | -3.19 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.30% | -2.02 pp | 92.63% | 4.25% |
| Infernus S.T | 91.94% | 90.78% | +1.16 pp | 93.10% | 61.50% |
| Whirlwind S.T | 91.12% | 92.28% | -1.16 pp | 93.37% | 47.87% |
| Servo-Skull | 91.19% | 91.64% | -0.45 pp | 91.26% | 22.00% |
| Demolitionist S.T | 91.58% | 91.47% | +0.10 pp | 93.23% | 47.30% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
