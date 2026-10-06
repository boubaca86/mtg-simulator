# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 27,000
**S.T win rate:** 91.69%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.70% | 87.67% | +6.03 pp | 93.96% | 65.27% |
| Eradicator S.T | 93.34% | 88.65% | +4.69 pp | 95.07% | 58.40% |
| Drop Pod S.T | 93.10% | 88.99% | +4.11 pp | 93.41% | 57.59% |
| Pyroclast Squad S.T | 92.99% | 89.20% | +3.79 pp | 93.70% | 63.89% |
| Terminator S.T | 92.68% | 89.77% | +2.91 pp | 93.12% | 65.40% |
| Exterminatus S.T | 89.48% | 92.29% | -2.82 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.59% | -2.39 pp | 91.99% | 4.21% |
| Infernus S.T | 91.96% | 91.18% | +0.78 pp | 93.10% | 61.47% |
| Demolitionist S.T | 91.94% | 91.23% | +0.71 pp | 93.68% | 47.38% |
| Servo-Skull | 91.24% | 91.82% | -0.58 pp | 91.27% | 22.05% |
| Whirlwind S.T | 91.51% | 92.00% | -0.48 pp | 93.70% | 47.86% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
