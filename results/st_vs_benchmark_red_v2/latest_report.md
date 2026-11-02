# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 348,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.65% | +5.78 pp | 93.71% | 64.95% |
| Eradicator S.T | 93.02% | 88.71% | +4.31 pp | 94.98% | 57.73% |
| Drop Pod S.T | 92.92% | 88.77% | +4.14 pp | 93.19% | 57.41% |
| Pyroclast Squad S.T | 92.76% | 89.01% | +3.75 pp | 93.57% | 64.29% |
| Terminator S.T | 92.76% | 89.01% | +3.75 pp | 93.26% | 65.52% |
| Exterminatus S.T | 88.94% | 92.20% | -3.26 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.31% | -2.17 pp | 92.46% | 4.25% |
| Infernus S.T | 91.91% | 90.70% | +1.21 pp | 93.05% | 61.39% |
| Whirlwind S.T | 91.12% | 92.13% | -1.01 pp | 93.51% | 47.80% |
| Servo-Skull | 90.99% | 91.63% | -0.64 pp | 91.01% | 22.02% |
| Demolitionist S.T | 91.55% | 91.37% | +0.18 pp | 93.25% | 47.02% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
